/*******************************************************************************
 * Copyright (c) 2012-2017 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.ext.openshift.client.deploy;

import com.google.gwt.core.client.JsArrayMixed;

import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.promises.client.js.Executor;
import org.eclipse.che.api.promises.client.js.JsPromiseError;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.api.promises.client.js.RejectFunction;
import org.eclipse.che.api.promises.client.js.ResolveFunction;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftLocalizationConstant;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftServiceClient;
import org.eclipse.che.ide.ext.openshift.shared.dto.BuildConfig;
import org.eclipse.che.ide.ext.openshift.shared.dto.BuildSource;
import org.eclipse.che.ide.ext.openshift.shared.dto.Container;
import org.eclipse.che.ide.ext.openshift.shared.dto.DeploymentConfig;
import org.eclipse.che.ide.ext.openshift.shared.dto.ImageStream;
import org.eclipse.che.ide.ext.openshift.shared.dto.NamedTagEventList;
import org.eclipse.che.ide.ext.openshift.shared.dto.ObjectReference;
import org.eclipse.che.ide.ext.openshift.shared.dto.Route;
import org.eclipse.che.ide.ext.openshift.shared.dto.Service;
import org.eclipse.che.ide.ext.openshift.shared.dto.Project;
import org.eclipse.che.ide.util.Pair;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Sergii Leschenko
 * @author Michail Kuznyetsov
 */
public class ApplicationManager {
    private static final String APPLICATION_LABEL_NAME = "application";

    private final OpenshiftServiceClient        openShiftClient;
    private final OpenshiftLocalizationConstant locale;

    @Inject
    public ApplicationManager(OpenshiftServiceClient openShiftClient,
                              OpenshiftLocalizationConstant locale) {
        this.openShiftClient = openShiftClient;
        this.locale = locale;
    }

    /**
     * @return a list of pairs, where first is namespace,
     * second - application name
     */
    public Promise<List<Pair<String, String>>> getApplicationNamesByNamespaces() {
        final Executor.ExecutorBody<List<Pair<String, String>>> body = new Executor.ExecutorBody<List<Pair<String, String>>>() {
            @Override
            public void apply(final ResolveFunction<List<Pair<String, String>>> resolve, final RejectFunction reject) {
                openShiftClient.getProjects().then(new Operation<List<Project>>() {
                    @Override
                    public void apply(List<Project> projects) throws OperationException {
                        List<String> namespaces = new ArrayList<>();
                        for (Project project : projects) {
                            namespaces.add(project.getMetadata().getName());
                        }

                        if (namespaces.isEmpty()) {
                            resolve.apply(Collections.<Pair<String, String>>emptyList());
                            return;
                        }

                        final List<Pair<String, String>> result = new ArrayList<>();
                        List<Promise> promises = new ArrayList<>();
                        for (final String namespace : namespaces) {
                            promises.add(openShiftClient.getBuildConfigs(namespace).then(new Operation<List<BuildConfig>>() {
                                @Override
                                public void apply(List<BuildConfig> buildConfigs) throws OperationException {
                                    for (BuildConfig buildConfig : buildConfigs) {
                                        result.add(new Pair<>(namespace, buildConfig.getMetadata().getName()));
                                    }
                                }
                            }));
                        }
                        Promises.all(promises.toArray(new Promise[promises.size()])).then(new Operation() {
                            @Override
                            public void apply(Object arg) throws OperationException {
                                resolve.apply(result);
                            }
                        }).catchError(new Operation<PromiseError>() {
                            @Override
                            public void apply(PromiseError arg) throws OperationException {
                                reject.apply(arg);
                            }
                        });
                    }
                }).catchError(new Operation<PromiseError>() {
                    @Override
                    public void apply(PromiseError arg) throws OperationException {
                        reject.apply(arg);
                    }
                });
            }
        };
        final Executor<List<Pair<String, String>>> executor = Executor.create(body);
        return Promises.create(executor);
    }

    /**
     * @param buildConfig
     *         build config for searching other application objects
     * @return {@link Application} object with all configs that are related to the given build config
     */
    public Promise<Application> findApplication(final BuildConfig buildConfig) {
        final Executor.ExecutorBody<Application> body = new Executor.ExecutorBody<Application>() {
            @Override
            public void apply(final ResolveFunction<Application> resolve, final RejectFunction reject) {
                final String namespace = buildConfig.getMetadata().getNamespace();

                final ObjectReference outputObject = buildConfig.getSpec().getOutput().getTo();
                if (!"ImageStreamTag".equals(outputObject.getKind())) {
                    reject.apply(JsPromiseError.create(locale.buildConfigHasInvalidOutputError()));
                    return;
                }

                // Parse image stream tag name that looks like 'myImageSteam:tagName'
                String imageTagName = outputObject.getName();
                final String[] split = imageTagName.split(":");
                if (split.length != 2) {
                    reject.apply(JsPromiseError.create(locale.buildConfigHasInvalidTagName(imageTagName)));
                    return;
                }
                final String imageStreamName = split[0];
                final String imageStreamTag = split[1];

                final Application application = new Application();
                application.setBuildConfig(buildConfig);
                openShiftClient.getImageStream(namespace, imageStreamName)
                               .thenPromise(new Function<ImageStream, Promise<String>>() {
                                   @Override
                                   public Promise<String> apply(ImageStream imageStream) throws FunctionException {
                                       application.setOutputImageStream(imageStream);
                                       return getDockerImageReference(imageStreamName, imageStreamTag, imageStream);
                                   }
                               })
                               .thenPromise(new Function<String, Promise<List<DeploymentConfig>>>() {
                                   @Override
                                   public Promise<List<DeploymentConfig>> apply(String imageStreamTagReference) throws FunctionException {
                                       application.setImageStreamTagReference(imageStreamTagReference);
                                       return openShiftClient.getDeploymentConfigs(namespace, null);
                                   }
                               })
                               .thenPromise(new Function<List<DeploymentConfig>, Promise<List<Service>>>() {
                                   @Override
                                   public Promise<List<Service>> apply(List<DeploymentConfig> deploymentConfigs) throws FunctionException {
                                       application.setDeploymentConfigs(
                                               filterDeploymentConfigsByImageTag(deploymentConfigs,
                                                                                 application.getImageStreamTagReference()));
                                       return openShiftClient.getServices(namespace, null);
                                   }
                               })
                               .thenPromise(new Function<List<Service>, Promise<List<Route>>>() {
                                   @Override
                                   public Promise<List<Route>> apply(List<Service> services) throws FunctionException {
                                       application.setServices(filterServicesByDeploymentConfigs(services,
                                                                                                 application.getDeploymentConfigs()));
                                       return openShiftClient.getRoutes(namespace, null);
                                   }
                               })
                               .then(new Operation<List<Route>>() {
                                   @Override
                                   public void apply(List<Route> routes) {
                                       application.setRoutes(filterRoutesByServices(routes, application.getServices()));
                                       resolve.apply(application);
                                   }
                               })
                               .catchError(new Operation<PromiseError>() {
                                   @Override
                                   public void apply(PromiseError arg) throws OperationException {
                                       reject.apply(arg);
                                   }
                               });
            }
        };
        final Executor<Application> executor = Executor.create(body);
        return Promises.create(executor);
    }

    /**
     * Returns new list that contains only those {@link Route} instances from given list of routes <br/>
     * that are linked to any of given services.
     *
     * <p/> About link between Route and Service see {@link Application#routes}
     */
    private List<Route> filterRoutesByServices(List<Route> routes, List<Service> services) {
        Set<String> servicesNames = new HashSet<>();
        for (Service service : services) {
            servicesNames.add(service.getMetadata().getName());
        }

        List<Route> resultRoutes = new ArrayList<>();
        for (Route route : routes) {
            if ("Service".equals(route.getSpec().getTo().getKind()) &&
                servicesNames.contains(route.getSpec().getTo().getName())) {
                resultRoutes.add(route);
            }
        }
        return resultRoutes;
    }

    /**
     * Returns new list that contains only those {@link Service} instances from given list of services <br/>
     * that are linked to any of given deployment configs.
     *
     * <p/> About link between DeploymentConfig and Service see {@link Application#services}
     */
    private List<Service> filterServicesByDeploymentConfigs(List<Service> services, List<DeploymentConfig> deploymentConfigs) {
        List<Service> resultServices = new ArrayList<>();

        Set<Pair<String, String>> labels = new HashSet<>();
        for (DeploymentConfig deploymentConfig : deploymentConfigs) {
            for (Map.Entry<String, String> stringStringEntry : deploymentConfig.getSpec().getTemplate()
                                                                               .getMetadata()
                                                                               .getLabels()
                                                                               .entrySet()) {
                labels.add(Pair.of(stringStringEntry.getKey(), stringStringEntry.getValue()));
            }
        }
        for (Service service : services) {
            for (Map.Entry<String, String> stringStringEntry : service.getSpec().getSelector().entrySet()) {
                if (labels.contains(Pair.of(stringStringEntry.getKey(), stringStringEntry.getValue()))) {
                    resultServices.add(service);
                    break;
                }
            }
        }
        return resultServices;
    }

    /**
     * Returns new list that contains only those {@link DeploymentConfig} instances from given list of deployment configs <br/>
     * that are linked to given imageStreamTagReference.
     *
     * <p/> About link between DeploymentConfig and imageStreamTagReference <br/>
     * see {@link Application#imageStreamTagReference} and {@link Application#imageStreamTagReference}
     */
    private List<DeploymentConfig> filterDeploymentConfigsByImageTag(List<DeploymentConfig> deploymentConfigs,
                                                                     String imageStreamTagReference) {
        List<DeploymentConfig> filtered = new ArrayList<>();
        for (DeploymentConfig deploymentConfig : deploymentConfigs) {
            for (Container container : deploymentConfig.getSpec().getTemplate().getSpec().getContainers()) {
                String image = container.getImage();
                if (!image.contains(":")) {
                    image += ":latest";
                }
                if (imageStreamTagReference.equals(image)) {
                    filtered.add(deploymentConfig);
                    break;
                }
            }
        }
        return filtered;
    }

    /**
     * Returns ImageStreamTagReference.
     * See {@link Application#imageStreamTagReference}
     */
    private Promise<String> getDockerImageReference(String imageStreamName, String imageStreamTag, ImageStream imageStream) {
        if (imageStream.getStatus().getTags().isEmpty()) {
            return Promises.resolve(imageStreamName + ":" + imageStreamTag);
        } else {
            for (NamedTagEventList namedTagEventList : imageStream.getStatus().getTags()) {
                if (imageStreamTag.equals(namedTagEventList.getTag())) {
                    if (namedTagEventList.getItems().isEmpty()) {
                        return Promises.reject(JsPromiseError.create(locale.imageSteamHasInvalidTagError(imageStreamName, imageStreamTag)));
                    } else {
                        return Promises
                                .resolve(namedTagEventList.getItems().get(0).getDockerImageReference());
                    }
                }
            }

            return Promises.reject(JsPromiseError.create(locale.imageStreamDoesNotHaveAnyTag(imageStreamName, imageStreamTag)));
        }
    }

    /**
     * Updates objects from given {@link Application} if there are changes after adding label 'application' <br/>
     * with given value to all objects from {@link Application}.
     *
     * @param application
     *         instance of {@link Application} for updating of its configs
     * @param applicationName
     *         value of label application that will be adding to all objects from {@link Application}
     */
    public Promise<Application> updateOpenshiftApplication(final Application application, final String applicationName) {
        return updateOpenshiftApplication(application, applicationName, application.getBuildConfig().getSpec().getSource());
    }
    /**
     * Updates objects from given {@link Application} if there are changes after setting new given {@link BuildSource} <br/>
     * for {@link Application#buildConfig} and adding label 'application' with given value to all objects from {@link Application}.
     *
     * @param application
     *         instance of {@link Application} for updating of its configs
     * @param applicationName
     *         value of label application that will be adding to all objects from {@link Application}
     * @param newBuildSources
     *         instance of {@link BuildSource} that will be set to {@link BuildConfig} from {@link Application#getBuildConfig()}
     */
    public Promise<Application> updateOpenshiftApplication(final Application application, final String applicationName,
                                                           final BuildSource newBuildSources) {
        final Executor.ExecutorBody<Application> body = new Executor.ExecutorBody<Application>() {
            @Override
            public void apply(final ResolveFunction<Application> resolve, final RejectFunction reject) {
                List<Promise<?>> promises = new ArrayList<>();

                String oldApplicationName = application.getBuildConfig().getMetadata().getLabels()
                                                       .put(APPLICATION_LABEL_NAME, applicationName);

                if (!newBuildSources.equals(application.getBuildConfig().getSpec().getSource())
                    || !applicationName.equals(oldApplicationName)) {

                    application.getBuildConfig().getSpec().setSource(newBuildSources);
                    promises.add(openShiftClient.updateBuildConfig(application.getBuildConfig()));
                }

                oldApplicationName = application.getOutputImageStream().getMetadata().getLabels()
                                                .put(APPLICATION_LABEL_NAME, applicationName);
                if (!applicationName.equals(oldApplicationName)) {
                    promises.add(openShiftClient.updateImageStream(application.getOutputImageStream()));
                }

                for (DeploymentConfig deploymentConfig : application.getDeploymentConfigs()) {
                    oldApplicationName = deploymentConfig.getMetadata().getLabels().put(APPLICATION_LABEL_NAME, applicationName);
                    if (!applicationName.equals(oldApplicationName)) {
                        promises.add(openShiftClient.updateDeploymentConfig(deploymentConfig));
                    }
                }

                for (Service service : application.getServices()) {
                    oldApplicationName = service.getMetadata().getLabels().put(APPLICATION_LABEL_NAME, applicationName);
                    if (!applicationName.equals(oldApplicationName)) {
                        promises.add(openShiftClient.updateService(service));
                    }
                }

                for (Route route : application.getRoutes()) {
                    oldApplicationName = route.getMetadata().getLabels().put(APPLICATION_LABEL_NAME, applicationName);
                    if (!applicationName.equals(oldApplicationName)) {
                        promises.add(openShiftClient.updateRoute(route));
                    }
                }

                Promises.all(promises.toArray(new Promise[promises.size()]))
                        .then(new Operation<JsArrayMixed>() {
                            @Override
                            public void apply(JsArrayMixed jsArrayMixed) throws OperationException {
                                resolve.apply(application);
                            }
                        })
                        .catchError(new Operation<PromiseError>() {
                            @Override
                            public void apply(PromiseError arg) throws OperationException {
                                reject.apply(arg);
                            }
                        });
            }
        };
        final Executor<Application> executor = Executor.create(body);
        return Promises.create(executor);
    }
}
