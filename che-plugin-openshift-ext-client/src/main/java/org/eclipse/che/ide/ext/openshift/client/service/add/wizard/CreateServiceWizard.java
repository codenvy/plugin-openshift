/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.ext.openshift.client.service.add.wizard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.JsArrayMixed;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.wizard.AbstractWizard;
import org.eclipse.che.ide.api.wizard.WizardPage;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftServiceClient;
import org.eclipse.che.ide.ext.openshift.client.dto.NewServiceRequest;
import org.eclipse.che.ide.ext.openshift.shared.dto.Container;
import org.eclipse.che.ide.ext.openshift.shared.dto.DeploymentConfig;
import org.eclipse.che.ide.ext.openshift.shared.dto.EnvVar;
import org.eclipse.che.ide.ext.openshift.shared.dto.Service;
import org.eclipse.che.ide.ext.openshift.shared.dto.Template;

import javax.validation.constraints.NotNull;

import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_NAMESPACE_VARIABLE_NAME;

/**
 * Create datasource service wizard
 *
 * @author Alexander Andrienko
 */
public class CreateServiceWizard extends AbstractWizard<NewServiceRequest> {

    private final OpenshiftServiceClient client;
    private final AppContext             appContext;
    private final DtoFactory             dtoFactory;

    private String nameSpace;

    @Inject
    public CreateServiceWizard(@Assisted NewServiceRequest newServiceRequest,
                               OpenshiftServiceClient client,
                               AppContext appContext,
                               DtoFactory dtoFactory) {
        super(newServiceRequest);
        this.client = client;
        this.appContext = appContext;
        this.dtoFactory = dtoFactory;
    }

    @Override
    public void complete(final @NotNull CompleteCallback callback) {
        ProjectConfigDto projectConfig = appContext.getCurrentProject().getRootProject();
        nameSpace = getAttributeValue(projectConfig, OPENSHIFT_NAMESPACE_VARIABLE_NAME);

        Template template = dataObject.getTemplate();

        client.processTemplate(nameSpace, template).then(processTemplate())
              .then(onSuccess(callback))
              .catchError(new Operation<PromiseError>() {
                  @Override
                  public void apply(PromiseError promiseError) throws OperationException {
                      callback.onFailure(promiseError.getCause());
                  }
              });
    }

    private String getAttributeValue(ProjectConfigDto projectConfig, String value) {
        List<String> attributes = projectConfig.getAttributes().get(value);
        if (attributes == null || attributes.isEmpty()) {
            return null;
        }
        return projectConfig.getAttributes().get(value).get(0);
    }

    private Function<Template, Promise<JsArrayMixed>> processTemplate() {
        return new Function<Template, Promise<JsArrayMixed>>() {
            @Override
            public Promise<JsArrayMixed> apply(Template template) throws FunctionException {
                List<Promise<?>> promiseList = new ArrayList<>();
                for (Object object : template.getObjects()) {
                    JSONObject json = (JSONObject)object;
                    String kind = ((JSONString)json.get("kind")).stringValue();
                    switch (kind) {
                        case "Service":
                            Service service = dtoFactory.createDtoFromJson(json.toString(), Service.class);
                            service.getMetadata().setNamespace(nameSpace);
                            promiseList.add(client.createService(service));
                            break;
                        case "DeploymentConfig":
                            final DeploymentConfig deploymentConfig = dtoFactory.createDtoFromJson(json.toString(), DeploymentConfig.class);
                            deploymentConfig.getMetadata().setNamespace(nameSpace);
                            Promise<JsArrayMixed> p = client.createDeploymentConfig(deploymentConfig)
                                                            .thenPromise(linkApplicationDeloymentConfigsWithCreatedDeploymentConfig());
                            promiseList.add(p);
                            break;
                    }
                }
                Promise<?>[] promises = promiseList.toArray(new Promise<?>[promiseList.size()]);
                return Promises.all(promises);
            }
        };
    }

    private Function<DeploymentConfig, Promise<JsArrayMixed>> linkApplicationDeloymentConfigsWithCreatedDeploymentConfig() {
        return new Function<DeploymentConfig, Promise<JsArrayMixed>>() {
            @Override
            public Promise<JsArrayMixed> apply(final DeploymentConfig serviceConfig) throws FunctionException {
                String applicationName = appContext.getCurrentProject().getRootProject().getName();
                return client.getDeploymentConfigs(nameSpace, applicationName)
                             .then(new Function<List<DeploymentConfig>, List<DeploymentConfig>>() {
                                 @Override
                                 public List<DeploymentConfig> apply(List<DeploymentConfig> configs) throws FunctionException {
                                     final String deploymentConfigName = serviceConfig.getMetadata().getName();
                                     final List<Container> containersForUpdate = serviceConfig.getSpec().getTemplate().getSpec()
                                                                                              .getContainers();
                                     for (DeploymentConfig config : configs) {
                                         List<Container> containers = config.getSpec().getTemplate().getSpec().getContainers();

                                         if (containers == null) {
                                             config.getSpec().getTemplate().getSpec().setContainers(containersForUpdate);
                                             continue;
                                         }

                                         for (Container containerForUpdate : containersForUpdate) {
                                             for (Container container : containers) {
                                                 updateContainerEnv(container, containerForUpdate.getEnv());
                                             }
                                         }
                                         config.getMetadata().getLabels().put("database", deploymentConfigName);
                                     }
                                     return configs;
                                 }
                             }).thenPromise(updateApplicationDeploymentConfigs());
            }
        };
    }

    private Function<List<DeploymentConfig>, Promise<JsArrayMixed>> updateApplicationDeploymentConfigs() {
        return new Function<List<DeploymentConfig>, Promise<JsArrayMixed>>() {
            @Override
            public Promise<JsArrayMixed> apply(List<DeploymentConfig> configs) throws FunctionException {
                List<Promise<?>> promiseList = new ArrayList<>();
                for (DeploymentConfig config : configs) {
                    Promise<DeploymentConfig> promise = client.updateDeploymentConfig(config);
                    promiseList.add(promise);
                }
                Promise<?>[] promises = promiseList.toArray(new Promise<?>[promiseList.size()]);
                return Promises.all(promises);
            }
        };
    }

    private void updateContainerEnv(Container container, List<EnvVar> envVariables) {
        List<EnvVar> containerEnvs = container.getEnv();

        if (containerEnvs == null) {
            container.setEnv(envVariables);
            return;
        }

        Map<String, EnvVar> envNamesToEnv = new HashMap<>();
        for (EnvVar containerEnv : containerEnvs) {
            envNamesToEnv.put(containerEnv.getName(), containerEnv);
        }

        for (final EnvVar envVarForEdit : envVariables) {
            String name = envVarForEdit.getName();
            if (envNamesToEnv.containsKey(name)) {
                containerEnvs.remove(envNamesToEnv.get(name));
            }
            containerEnvs.add(envVarForEdit);
        }
    }

    private Operation<Promise<JsArrayMixed>> onSuccess(final CompleteCallback callback) {
        return new Operation<Promise<JsArrayMixed>>() {
            @Override
            public void apply(Promise<JsArrayMixed> arg) throws OperationException {
                callback.onCompleted();
            }
        };
    }

    /**
     * Get first page from the wizard
     * @return first wizard page
     */
    public WizardPage<NewServiceRequest> getFirstPage() {
        return wizardPages.get(0);
    }
}
