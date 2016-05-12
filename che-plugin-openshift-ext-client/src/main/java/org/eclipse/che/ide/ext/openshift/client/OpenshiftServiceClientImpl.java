/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.ext.openshift.client;

import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.ide.MimeType;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.ext.openshift.shared.dto.Build;
import org.eclipse.che.ide.ext.openshift.shared.dto.BuildConfig;
import org.eclipse.che.ide.ext.openshift.shared.dto.DeploymentConfig;
import org.eclipse.che.ide.ext.openshift.shared.dto.ImageStream;
import org.eclipse.che.ide.ext.openshift.shared.dto.ImageStreamTag;
import org.eclipse.che.ide.ext.openshift.shared.dto.OpenshiftServerInfo;
import org.eclipse.che.ide.ext.openshift.shared.dto.Project;
import org.eclipse.che.ide.ext.openshift.shared.dto.ProjectRequest;
import org.eclipse.che.ide.ext.openshift.shared.dto.ReplicationController;
import org.eclipse.che.ide.ext.openshift.shared.dto.Route;
import org.eclipse.che.ide.ext.openshift.shared.dto.Service;
import org.eclipse.che.ide.ext.openshift.shared.dto.Template;
import org.eclipse.che.ide.ext.openshift.shared.dto.WebHook;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.ui.loaders.request.LoaderFactory;

import javax.inject.Inject;
import java.util.List;

import static com.google.gwt.http.client.RequestBuilder.PUT;
import static org.eclipse.che.ide.rest.HTTPHeader.ACCEPT;
import static org.eclipse.che.ide.rest.HTTPHeader.CONTENT_TYPE;

/**
 * @author Sergii Leschenko
 * @author Vlad Zhukovskyi
 */
public class OpenshiftServiceClientImpl implements OpenshiftServiceClient {

    private final AsyncRequestFactory    asyncRequestFactory;
    private final LoaderFactory          loaderFactory;
    private final DtoUnmarshallerFactory dtoUnmarshaller;
    private final AppContext             appContext;

    @Inject
    public OpenshiftServiceClientImpl(AsyncRequestFactory asyncRequestFactory,
                                      LoaderFactory loaderFactory,
                                      AppContext appContext,
                                      DtoUnmarshallerFactory dtoUnmarshaller) {
        this.asyncRequestFactory = asyncRequestFactory;
        this.loaderFactory = loaderFactory;
        this.dtoUnmarshaller = dtoUnmarshaller;
        this.appContext = appContext;
    }

    /**
     * Returns OpenShift service base URL
     *
     * @return
     */
    private String baseURL() {
        return appContext.getDevMachine().getWsAgentBaseUrl() + "/openshift/" + appContext.getDevMachine().getWorkspace();
    }

    @Override
    public Promise<OpenshiftServerInfo> getServerInfo() {
        return asyncRequestFactory.createGetRequest(baseURL())
                                  .header(ACCEPT, MimeType.APPLICATION_JSON)
                                  .send(dtoUnmarshaller.newUnmarshaller(OpenshiftServerInfo.class));
    }

    public Promise<List<Template>> getTemplates(String namespace) {
        return asyncRequestFactory.createGetRequest(baseURL() + "/namespace/" + namespace + "/template")
                                  .header(ACCEPT, MimeType.APPLICATION_JSON)
                                  .send(dtoUnmarshaller.newListUnmarshaller(Template.class));
    }

    public Promise<Template> processTemplate(String namespace, Template template) {
        return asyncRequestFactory.createPostRequest(baseURL() + "/namespace/" + namespace + "/template/process", template)
                                  .header(ACCEPT, MimeType.APPLICATION_JSON)
                                  .header(CONTENT_TYPE, MimeType.APPLICATION_JSON)
                                  .send(dtoUnmarshaller.newUnmarshaller(Template.class));
    }

    @Override
    public Promise<List<Project>> getProjects() {
        return asyncRequestFactory.createGetRequest(baseURL() + "/project")
                                  .header(ACCEPT, MimeType.APPLICATION_JSON)
                                  .send(dtoUnmarshaller.newListUnmarshaller(Project.class));
    }

    @Override
    public Promise<Project> createProject(ProjectRequest request) {
        return asyncRequestFactory.createPostRequest(baseURL() + "/project", request)
                                  .header(ACCEPT, MimeType.APPLICATION_JSON)
                                  .send(dtoUnmarshaller.newUnmarshaller(Project.class));
    }

    @Override
    public Promise<Void> deleteProject(String project) {
        return asyncRequestFactory.createDeleteRequest(baseURL() + "/project/" + project)
                                  .loader(loaderFactory.newLoader("Deleting OpenShift project..."))
                                  .send();
    }

    @Override
    public Promise<BuildConfig> createBuildConfig(BuildConfig config) {
        String url = baseURL() + "/namespace/" + config.getMetadata().getNamespace() + "/buildconfig";
        return asyncRequestFactory.createPostRequest(url, config)
                                  .header(CONTENT_TYPE, MimeType.APPLICATION_JSON)
                                  .header(ACCEPT, MimeType.APPLICATION_JSON)
                                  .send(dtoUnmarshaller.newUnmarshaller(BuildConfig.class));
    }

    @Override
    public Promise<BuildConfig> updateBuildConfig(final BuildConfig config) {
        String url = baseURL() + "/namespace/" + config.getMetadata().getNamespace() + "/buildconfig/" + config.getMetadata().getName();
        return asyncRequestFactory.createRequest(PUT, url, config, false)
                                  .header(CONTENT_TYPE, MimeType.APPLICATION_JSON)
                                  .header(ACCEPT, MimeType.APPLICATION_JSON)
                                  .loader(loaderFactory.newLoader("Updating build config..."))
                                  .send(dtoUnmarshaller.newUnmarshaller(BuildConfig.class));
    }

    @Override
    public Promise<List<BuildConfig>> getBuildConfigs(String namespace) {
        return getBuildConfigs(namespace, null);
    }

    @Override
    public Promise<List<BuildConfig>> getBuildConfigs(String namespace, String application) {
        String url = baseURL() + "/namespace/" + namespace + "/buildconfig";
        if (application != null) {
            url += "?application=" + application;
        }
        return asyncRequestFactory.createGetRequest(url)
                                  .header(ACCEPT, MimeType.APPLICATION_JSON)
                                  .send(dtoUnmarshaller.newListUnmarshaller(BuildConfig.class));
    }

    @Override
    public Promise<List<WebHook>> getWebhooks(String namespace, String buildConfig) {
        String url = baseURL() + "/namespace/" + namespace + "/buildconfig/" + buildConfig + "/webhook";
        return asyncRequestFactory.createGetRequest(url)
                                  .header(ACCEPT, MimeType.APPLICATION_JSON)
                                  .loader(loaderFactory.newLoader("Getting webhooks..."))
                                  .send(dtoUnmarshaller.newListUnmarshaller(WebHook.class));
    }

    @Override
    public Promise<ImageStream> createImageStream(ImageStream stream) {
        String url = baseURL() + "/namespace/" + stream.getMetadata().getNamespace() + "/imagestream";
        return asyncRequestFactory.createPostRequest(url, stream)
                                  .header(CONTENT_TYPE, MimeType.APPLICATION_JSON)
                                  .header(ACCEPT, MimeType.APPLICATION_JSON)
                                  .send(dtoUnmarshaller.newUnmarshaller(ImageStream.class));
    }

    @Override
    public Promise<List<ImageStream>> getImageStreams(String namespace, String application) {
        String url = baseURL() + "/namespace/" + namespace + "/imagestream";
        if (application != null) {
            url += "?application=" + application;
        }
        return asyncRequestFactory.createGetRequest(url)
                                  .header(CONTENT_TYPE, MimeType.APPLICATION_JSON)
                                  .header(ACCEPT, MimeType.APPLICATION_JSON)
                                  .loader(loaderFactory.newLoader("Getting image stream..."))
                                  .send(dtoUnmarshaller.newListUnmarshaller(ImageStream.class));
    }

    @Override
    public Promise<ImageStream> getImageStream(String namespace, String imageStream) {
        String url = baseURL() + "/namespace/" + namespace + "/imagestream/" + imageStream;
        return asyncRequestFactory.createGetRequest(url)
                                  .header(CONTENT_TYPE, MimeType.APPLICATION_JSON)
                                  .header(ACCEPT, MimeType.APPLICATION_JSON)
                                  .loader(loaderFactory.newLoader("Getting image stream..."))
                                  .send(dtoUnmarshaller.newUnmarshaller(ImageStream.class));
    }

    @Override
    public Promise<ImageStream> updateImageStream(ImageStream imageStream) {
        String url = baseURL() + "/namespace/" + imageStream.getMetadata().getNamespace() + "/imagestream/" +
                imageStream.getMetadata().getName();
        return asyncRequestFactory.createRequest(PUT, url, imageStream, false)
                                  .header(CONTENT_TYPE, MimeType.APPLICATION_JSON)
                                  .header(ACCEPT, MimeType.APPLICATION_JSON)
                                  .loader(loaderFactory.newLoader("Updating image stream..."))
                                  .send(dtoUnmarshaller.newUnmarshaller(ImageStream.class));
    }

    @Override
    public Promise<ImageStreamTag> getImageStreamTag(String namespace, String imageStream, String tag) {
        String url = baseURL() + "/namespace/" + namespace + "/imagestream/" + imageStream + "/tag/" + tag;
        return asyncRequestFactory.createGetRequest(url)
                                  .header(CONTENT_TYPE, MimeType.APPLICATION_JSON)
                                  .header(ACCEPT, MimeType.APPLICATION_JSON)
                                  .loader(loaderFactory.newLoader("Getting image stream tag..."))
                                  .send(dtoUnmarshaller.newUnmarshaller(ImageStreamTag.class));
    }

    @Override
    public Promise<DeploymentConfig> createDeploymentConfig(final DeploymentConfig config) {
        String url = baseURL() + "/namespace/" + config.getMetadata().getNamespace() + "/deploymentconfig";
        return asyncRequestFactory.createPostRequest(url, config)
                                  .header(CONTENT_TYPE, MimeType.APPLICATION_JSON)
                                  .header(ACCEPT, MimeType.APPLICATION_JSON)
                                  .send(dtoUnmarshaller.newUnmarshaller(DeploymentConfig.class));
    }

    @Override
    public Promise<DeploymentConfig> updateDeploymentConfig(final DeploymentConfig config) {
        String url = baseURL() + "/namespace/" + config.getMetadata().getNamespace() + "/deploymentconfig/" + config.getMetadata().getName();
        return asyncRequestFactory.createRequest(PUT, url, config, false)
                                  .header(CONTENT_TYPE, MimeType.APPLICATION_JSON)
                                  .header(ACCEPT, MimeType.APPLICATION_JSON)
                                  .loader(loaderFactory.newLoader("Updating deployment config..."))
                                  .send(dtoUnmarshaller.newUnmarshaller(DeploymentConfig.class));
    }

    @Override
    public Promise<List<DeploymentConfig>> getDeploymentConfigs(final String namespace, final String application) {
        String url = baseURL() + "/namespace/" + namespace + "/deploymentconfig";
        if (application != null) {
            url += "?application=" + application;
        }
        return asyncRequestFactory.createGetRequest(url)
                                  .header(CONTENT_TYPE, MimeType.APPLICATION_JSON)
                                  .header(ACCEPT, MimeType.APPLICATION_JSON)
                                  .loader(loaderFactory.newLoader("Getting deployment configs..."))
                                  .send(dtoUnmarshaller.newListUnmarshaller(DeploymentConfig.class));
    }

    @Override
    public Promise<Route> createRoute(Route route) {
        return asyncRequestFactory.createPostRequest(baseURL() + "/namespace/" + route.getMetadata().getNamespace() + "/route", route)
                                  .header(CONTENT_TYPE, MimeType.APPLICATION_JSON)
                                  .header(ACCEPT, MimeType.APPLICATION_JSON)
                                  .send(dtoUnmarshaller.newUnmarshaller(Route.class));
    }

    @Override
    public Promise<Route> updateRoute(Route route) {
        String url = baseURL() + "/namespace/" + route.getMetadata().getNamespace() + "/route/" + route.getMetadata().getName();
        return asyncRequestFactory.createRequest(PUT, url, route, false)
                                  .header(CONTENT_TYPE, MimeType.APPLICATION_JSON)
                                  .header(ACCEPT, MimeType.APPLICATION_JSON)
                                  .loader(loaderFactory.newLoader("Updating route..."))
                                  .send(dtoUnmarshaller.newUnmarshaller(Route.class));
    }

    @Override
    public Promise<Service> createService(Service service) {
        String url = baseURL() + "/namespace/" + service.getMetadata().getNamespace() + "/service";
        return asyncRequestFactory.createPostRequest(url, service)
                                  .header(CONTENT_TYPE, MimeType.APPLICATION_JSON)
                                  .header(ACCEPT, MimeType.APPLICATION_JSON)
                                  .send(dtoUnmarshaller.newUnmarshaller(Service.class));
    }

    @Override
    public Promise<Service> updateService(Service service) {
        String url = baseURL() + "/namespace/" + service.getMetadata().getNamespace() + "/service/" + service.getMetadata().getName();
        return asyncRequestFactory
                .createRequest(PUT, url, service, false)
                .header(CONTENT_TYPE, MimeType.APPLICATION_JSON)
                .header(ACCEPT, MimeType.APPLICATION_JSON)
                .loader(loaderFactory.newLoader("Updating service..."))
                .send(dtoUnmarshaller.newUnmarshaller(Service.class));
    }

    @Override
    public Promise<List<Route>> getRoutes(String namespace, String application) {
        String url = baseURL() + "/namespace/" + namespace + "/route";
        if (application != null) {
            url += "?application=" + application;
        }
        return asyncRequestFactory.createGetRequest(url)
                                  .header(ACCEPT, MimeType.APPLICATION_JSON)
                                  .loader(loaderFactory.newLoader("Getting routes..."))
                                  .send(dtoUnmarshaller.newListUnmarshaller(Route.class));
    }

    @Override
    public Promise<List<Build>> getBuilds(String namespace, String application) {
        String url = baseURL() + "/namespace/" + namespace + "/build";
        if (application != null) {
            url += "?application=" + application;
        }
        return asyncRequestFactory.createGetRequest(url)
                                  .header(ACCEPT, MimeType.APPLICATION_JSON)
                                  .loader(loaderFactory.newLoader("Getting builds..."))
                                  .send(dtoUnmarshaller.newListUnmarshaller(Build.class));

    }

    @Override
    public Promise<Build> startBuild(String namespace, String buildConfig) {
        String url = baseURL() + "/namespace/" + namespace + "/build/" + buildConfig;
        return asyncRequestFactory.createPostRequest(url, null)
                                  .header(ACCEPT, MimeType.APPLICATION_JSON)
                                  .loader(loaderFactory.newLoader("Starting build..."))
                                  .send(dtoUnmarshaller.newUnmarshaller(Build.class));

    }

    @Override
    public Promise<List<ReplicationController>> getReplicationControllers(String namespace, String application) {
        String url = baseURL() + "/namespace/" + namespace + "/replicationcontroller";
        if (application != null) {
            url += "?application=" + application;
        }
        return asyncRequestFactory.createGetRequest(url)
                                  .header(ACCEPT, MimeType.APPLICATION_JSON)
                                  .loader(loaderFactory.newLoader("Getting replication controllers..."))
                                  .send(dtoUnmarshaller.newListUnmarshaller(ReplicationController.class));
    }

    @Override
    public Promise<ReplicationController> updateReplicationController(ReplicationController controller) {
        String url = baseURL() + "/namespace/" + controller.getMetadata().getNamespace() + "/replicationcontroller/" +
                           controller.getMetadata().getName();
        return asyncRequestFactory.createRequest(PUT, url, controller, false)
                                  .header(CONTENT_TYPE, MimeType.APPLICATION_JSON)
                                  .header(ACCEPT, MimeType.APPLICATION_JSON)
                                  .loader(loaderFactory.newLoader("Updating replication controller..."))
                                  .send(dtoUnmarshaller.newUnmarshaller(ReplicationController.class));
    }

    @Override
    public Promise<List<Service>> getServices(String namespace, String application) {
        String url = baseURL() + "/namespace/" + namespace + "/service";
        if (application != null) {
            url += "?application=" + application;
        }
        return asyncRequestFactory.createGetRequest(url)
                                  .header(CONTENT_TYPE, MimeType.APPLICATION_JSON)
                                  .header(ACCEPT, MimeType.APPLICATION_JSON)
                                  .loader(loaderFactory.newLoader("Getting services..."))
                                  .send(dtoUnmarshaller.newListUnmarshaller(Service.class));
    }

}
