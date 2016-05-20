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
package org.eclipse.che.ide.ext.openshift.client.deploy._new;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.JsArrayMixed;
import com.google.gwt.json.client.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.core.rest.shared.dto.ServiceError;
import org.eclipse.che.ide.api.git.GitServiceClient;
import org.eclipse.che.api.git.shared.Remote;
import org.eclipse.che.ide.api.project.ProjectServiceClient;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.event.project.ProjectUpdatedEvent;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.collections.Jso;
import org.eclipse.che.ide.collections.js.JsoArray;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.openshift.client.deploy.ApplicationManager;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftLocalizationConstant;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftServiceClient;
import org.eclipse.che.ide.ext.openshift.client.ValidateAuthenticationPresenter;
import org.eclipse.che.ide.ext.openshift.client.oauth.OpenshiftAuthenticator;
import org.eclipse.che.ide.ext.openshift.client.oauth.OpenshiftAuthorizationHandler;
import org.eclipse.che.ide.ext.openshift.client.util.OpenshiftValidator;
import org.eclipse.che.ide.ext.openshift.shared.dto.BuildConfig;
import org.eclipse.che.ide.ext.openshift.shared.dto.BuildConfigSpec;
import org.eclipse.che.ide.ext.openshift.shared.dto.BuildOutput;
import org.eclipse.che.ide.ext.openshift.shared.dto.BuildSource;
import org.eclipse.che.ide.ext.openshift.shared.dto.BuildStrategy;
import org.eclipse.che.ide.ext.openshift.shared.dto.BuildTriggerPolicy;
import org.eclipse.che.ide.ext.openshift.shared.dto.Container;
import org.eclipse.che.ide.ext.openshift.shared.dto.ContainerPort;
import org.eclipse.che.ide.ext.openshift.shared.dto.DeploymentConfig;
import org.eclipse.che.ide.ext.openshift.shared.dto.DeploymentConfigSpec;
import org.eclipse.che.ide.ext.openshift.shared.dto.DeploymentTriggerImageChangeParams;
import org.eclipse.che.ide.ext.openshift.shared.dto.DeploymentTriggerPolicy;
import org.eclipse.che.ide.ext.openshift.shared.dto.DockerImageMetadata;
import org.eclipse.che.ide.ext.openshift.shared.dto.EnvVar;
import org.eclipse.che.ide.ext.openshift.shared.dto.GitBuildSource;
import org.eclipse.che.ide.ext.openshift.shared.dto.ImageChangeTrigger;
import org.eclipse.che.ide.ext.openshift.shared.dto.ImageStream;
import org.eclipse.che.ide.ext.openshift.shared.dto.ImageStreamTag;
import org.eclipse.che.ide.ext.openshift.shared.dto.ObjectMeta;
import org.eclipse.che.ide.ext.openshift.shared.dto.ObjectReference;
import org.eclipse.che.ide.ext.openshift.shared.dto.PodSpec;
import org.eclipse.che.ide.ext.openshift.shared.dto.PodTemplateSpec;
import org.eclipse.che.ide.ext.openshift.shared.dto.Project;
import org.eclipse.che.ide.ext.openshift.shared.dto.ProjectRequest;
import org.eclipse.che.ide.ext.openshift.shared.dto.Route;
import org.eclipse.che.ide.ext.openshift.shared.dto.RouteSpec;
import org.eclipse.che.ide.ext.openshift.shared.dto.Service;
import org.eclipse.che.ide.ext.openshift.shared.dto.ServicePort;
import org.eclipse.che.ide.ext.openshift.shared.dto.ServiceSpec;
import org.eclipse.che.ide.ext.openshift.shared.dto.SourceBuildStrategy;
import org.eclipse.che.ide.ext.openshift.shared.dto.WebHookTrigger;
import org.eclipse.che.ide.api.dialogs.DialogFactory;
import org.eclipse.che.ide.util.Pair;

import javax.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static java.util.Collections.unmodifiableList;
import static org.eclipse.che.ide.ext.openshift.client.deploy._new.NewApplicationView.Mode.CREATE_NEW_PROJECT;
import static org.eclipse.che.ide.ext.openshift.client.deploy._new.NewApplicationView.Mode.SELECT_EXISTING_PROJECT;
import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_APPLICATION_VARIABLE_NAME;
import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_NAMESPACE_VARIABLE_NAME;
import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_PROJECT_TYPE_ID;
import static org.eclipse.che.ide.api.notification.StatusNotification.DisplayMode.EMERGE_MODE;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.FAIL;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.SUCCESS;

/**
 * Presenter for deploying Che project to new OpenShift application.
 *
 * @author Vlad Zhukovskiy
 */
@Singleton
public class NewApplicationPresenter extends ValidateAuthenticationPresenter implements NewApplicationView.ActionDelegate {
    private final NewApplicationView            view;
    private final AppContext                    appContext;
    private final DialogFactory                 dialogFactory;
    private final OpenshiftLocalizationConstant locale;
    private final GitServiceClient              gitService;
    private final OpenshiftServiceClient        osService;
    private final DtoFactory                    dtoFactory;
    private final ProjectServiceClient          projectService;
    private final EventBus                      eventBus;
    private final NotificationManager           notificationManager;
    private final ApplicationManager            applicationManager;
    private       List<Project>                 osProjects;
    private       List<Pair<String, String>>    osApplications;
    private       List<Remote>                  projectRemotes;
    private       List<ImageStream>             osImageStreams;
    private       ImageStreamTag                osActiveStreamTag;
    private       String                        osAppName;

    public static final String API_VERSION = "v1";

    @Inject
    public NewApplicationPresenter(NewApplicationView view,
                                   AppContext appContext,
                                   DialogFactory dialogFactory,
                                   OpenshiftLocalizationConstant locale,
                                   GitServiceClient gitService,
                                   OpenshiftServiceClient osService,
                                   DtoFactory dtoFactory,
                                   ProjectServiceClient projectService,
                                   EventBus eventBus,
                                   NotificationManager notificationManager,
                                   ApplicationManager applicationManager,
                                   OpenshiftAuthenticator openshiftAuthenticator,
                                   OpenshiftAuthorizationHandler openshiftAuthorizationHandler) {
        super(openshiftAuthenticator, openshiftAuthorizationHandler, locale, notificationManager);
        this.view = view;
        this.appContext = appContext;
        this.dialogFactory = dialogFactory;
        this.locale = locale;
        this.gitService = gitService;
        this.osService = osService;
        this.dtoFactory = dtoFactory;
        this.projectService = projectService;
        this.eventBus = eventBus;
        this.notificationManager = notificationManager;
        this.applicationManager = applicationManager;
        view.setDelegate(this);
        osProjects = new ArrayList<>();
        osApplications = new ArrayList<>();
    }

    private void reset() {
        osProjects.clear();
        osApplications.clear();
        osImageStreams = null;
        osAppName = null;
        osActiveStreamTag = null;
        projectRemotes = null;

        view.setDeployButtonEnabled(false);
        view.setLabels(Collections.<KeyValue>emptyList());
        view.setEnvironmentVariables(Collections.<KeyValue>emptyList());
        view.setApplicationName(null);
        view.hideApplicationNameError();
        view.hideProjectNameError();
        view.hideLabelsError();
        view.hideVariablesError();
        view.setOpenShiftProjectName(null);
        view.setOpenShiftProjectDisplayName(null);
        view.setOpenShiftProjectDescription(null);
        view.setMode(CREATE_NEW_PROJECT);
        view.setImages(Collections.<String>emptyList());
        view.setProjects(Collections.<Project>emptyList());
    }

    @Override
    protected void onSuccessAuthentication() {
        reset();

        if (appContext.getCurrentProject() != null) {
            //Check is Git repository:
            ProjectConfigDto projectConfig = appContext.getCurrentProject().getRootProject();
            List<String> listVcsProvider = projectConfig.getAttributes().get("vcs.provider.name");
            if (listVcsProvider != null && listVcsProvider.contains("git")) {
                getGitRemoteRepositories(projectConfig);
            } else {
                dialogFactory.createMessageDialog(locale.notGitRepositoryWarningTitle(),
                                                  locale.notGitRepositoryWarning(projectConfig.getName()),
                                                  null).show();
            }
        }
    }

    private void getGitRemoteRepositories(final ProjectConfigDto projectConfig) {
        gitService.remoteList(appContext.getDevMachine(), projectConfig, null, true)
                  .then(new Operation<List<Remote>>() {
                      @Override
                      public void apply(List<Remote> result) throws OperationException {
                          if (!result.isEmpty()) {
                              projectRemotes = unmodifiableList(result);
                              loadOpenShiftData();
                          } else {
                              dialogFactory.createMessageDialog(locale.noGitRemoteRepositoryWarningTitle(),
                                                                locale.noGitRemoteRepositoryWarning(projectConfig.getName()),
                                                                null).show();
                          }
                      }
                  })
                  .catchError(new Operation<PromiseError>() {
                      @Override
                      public void apply(PromiseError arg) throws OperationException {
                          notificationManager.notify(locale.getGitRemoteRepositoryError(projectConfig.getName()), FAIL, EMERGE_MODE);
                      }
                  });
    }

    private void loadOpenShiftData() {
        final ProjectConfigDto projectConfig = appContext.getCurrentProject().getRootProject();
        view.setApplicationName(projectConfig.getName());
        view.show();

        osService.getProjects().then(new Operation<List<Project>>() {
            @Override
            public void apply(List<Project> projects) throws OperationException {
                if (projects == null || projects.isEmpty()) {
                    return;
                }
                osProjects.clear();
                osProjects.addAll(unmodifiableList(projects));
                view.setProjects(osProjects);
            }
        }).then(osService.getImageStreams("openshift", null).then(new Operation<List<ImageStream>>() {
            @Override
            public void apply(List<ImageStream> streams) throws OperationException {
                if (streams == null || streams.isEmpty()) {
                    return;
                }

                osImageStreams = unmodifiableList(streams);

                final List<String> imageNames = Lists.transform(osImageStreams, new com.google.common.base.Function<ImageStream, String>() {
                    @Override
                    public String apply(ImageStream input) {
                        return input.getMetadata().getName();
                    }
                });

                view.setImages(imageNames);
                view.setLabels(Collections.<KeyValue>emptyList());
            }
        }));
        applicationManager.getApplicationNamesByNamespaces().then(new Operation<List<Pair<String, String>>>() {
            @Override
            public void apply(List<Pair<String, String>> arg) throws OperationException {
                osApplications.clear();
                osApplications.addAll(arg);
            }
        });
    }

    @Override
    public void onCancelClicked() {
        view.hide();
    }

    @Override
    public void onDeployClicked() {
        Promise<Project> projectPromise;
        view.showLoader(true);
        switch (view.getMode()) {
            case CREATE_NEW_PROJECT:
                String osProjectName = view.getOpenShiftProjectName();
                String osProjectDisplayName = view.getOpenShiftProjectDisplayName();
                String osProjectDescription = view.getOpenShiftProjectDescription();
                //create new project
                ProjectRequest request = newDto(ProjectRequest.class).withApiVersion(API_VERSION)
                                                                     .withDisplayName(osProjectDisplayName)
                                                                     .withDescription(osProjectDescription)
                                                                     .withMetadata(newDto(ObjectMeta.class).withName(osProjectName));
                projectPromise = osService.createProject(request);
                break;
            case SELECT_EXISTING_PROJECT:
            default:
                Project osSelectedProject = view.getOpenShiftSelectedProject();
                projectPromise = Promises.resolve(osSelectedProject);
        }

        projectPromise.then(new Operation<Project>() {
            @Override
            public void apply(final Project project) throws OperationException {
                final Map<String, String> labels = new HashMap<>();
                labels.put("generatedby", "Che");
                labels.put("application", osAppName);

                for (KeyValue label : view.getLabels()) {
                    labels.put(label.getKey(), label.getValue());
                }

                final DockerImageMetadata imageMetadata = osActiveStreamTag.getImage().getDockerImageMetadata();
                Object exposedPorts = (imageMetadata.getConfig() != null) ? imageMetadata.getConfig().getExposedPorts()
                                                                          : imageMetadata.getContainerConfig().getExposedPorts();
                List<ContainerPort> ports = parsePorts(exposedPorts);

                String namespace = project.getMetadata().getName();
                List<Promise<?>> promises = new ArrayList<>();
                promises.add(osService.createImageStream(generateImageStream(namespace, labels)));
                promises.add(osService.createBuildConfig(generateBuildConfig(namespace, labels)));
                promises.add(osService.createDeploymentConfig(generateDeploymentConfig(namespace, labels)));
                promises.add(osService.createRoute(generateRoute(namespace, labels)));

                if (!ports.isEmpty()) {
                    promises.add(osService.createService(generateService(namespace, getFirstPort(ports), labels)));
                }

                Promises.all(promises.toArray(new Promise[promises.size()]))
                        .then(new Operation<JsArrayMixed>() {
                            @Override
                            public void apply(JsArrayMixed arg) throws OperationException {
                                view.showLoader(false);
                                view.hide();
                                notificationManager
                                        .notify(locale.deployProjectSuccess(appContext.getCurrentProject().getRootProject().getName()),
                                                SUCCESS,
                                                EMERGE_MODE);
                                setupMixin(project);
                            }
                        })
                        .catchError(new Operation<PromiseError>() {
                            @Override
                            public void apply(PromiseError arg) throws OperationException {
                                handleError(arg);
                            }
                        });
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                handleError(arg);
            }
        });
    }

    private void handleError(PromiseError error) {
        view.showLoader(false);
        final ServiceError serviceError = dtoFactory.createDtoFromJson(error.getMessage(), ServiceError.class);
        notificationManager.notify(serviceError.getMessage(), FAIL, EMERGE_MODE);
        view.showError(serviceError.getMessage());
    }

    private void setupMixin(Project project) {
        final ProjectConfigDto projectConfig = appContext.getCurrentProject().getRootProject();

        List<String> mixins = projectConfig.getMixins();
        if (!mixins.contains(OPENSHIFT_PROJECT_TYPE_ID)) {
            mixins.add(OPENSHIFT_PROJECT_TYPE_ID);
        }

        Map<String, List<String>> attributes = projectConfig.getAttributes();
        attributes.put(OPENSHIFT_APPLICATION_VARIABLE_NAME, newArrayList(osAppName));
        attributes.put(OPENSHIFT_NAMESPACE_VARIABLE_NAME, newArrayList(project.getMetadata().getName()));

        projectService.updateProject(appContext.getDevMachine(), projectConfig.getPath(), projectConfig)
                      .then(new Operation<ProjectConfigDto>() {
                          @Override
                          public void apply(ProjectConfigDto project) throws OperationException {
                              eventBus.fireEvent(new ProjectUpdatedEvent(projectConfig.getPath(), project));
                              notificationManager.notify(locale.linkProjectWithExistingSuccess(projectConfig.getName(), osAppName),
                                                         SUCCESS,
                                                         EMERGE_MODE);
                          }
                      })
                      .catchError(new Operation<PromiseError>() {
                          @Override
                          public void apply(PromiseError arg) throws OperationException {
                              final ServiceError serviceError = dtoFactory.createDtoFromJson(arg.getMessage(), ServiceError.class);
                              notificationManager.notify(serviceError.getMessage(), FAIL, EMERGE_MODE);
                          }
                      });
    }

    private Promise<ImageStreamTag> setActiveImageTag(final ImageStream stream) {
        return osService.getImageStreamTag("openshift", stream.getMetadata().getName(), "latest").thenPromise(
                new Function<ImageStreamTag, Promise<ImageStreamTag>>() {
                    @Override
                    public Promise<ImageStreamTag> apply(ImageStreamTag streamTag) throws FunctionException {
                        osActiveStreamTag = streamTag;
                        final DockerImageMetadata dockerImageMetadata = streamTag.getImage().getDockerImageMetadata();
                        List<String> envs = (dockerImageMetadata.getConfig() != null) ? dockerImageMetadata.getConfig().getEnv()
                                                                                      : dockerImageMetadata.getContainerConfig().getEnv();
                        List<KeyValue> variables = new ArrayList<>();
                        for (String env : envs) {
                            String[] keyValuePair = env.split("=");
                            if (keyValuePair.length != 2) {
                                continue;
                            }
                            variables.add(new KeyValue(keyValuePair[0], keyValuePair[1]));
                        }
                        view.setEnvironmentVariables(variables);
                        return Promises.resolve(streamTag);
                    }
                });
    }

    @Override
    public void onApplicationNameChanged(String name) {
        osAppName = name;
        updateControls();
    }

    @Override
    public void onImageStreamChanged(String stream) {
        for (ImageStream osStream : osImageStreams) {
            if (stream.equals(osStream.getMetadata().getName())) {
                setActiveImageTag(osStream);
                break;
            }
        }
        updateControls();
    }

    private boolean isApplicationNameValid() {
        if (!OpenshiftValidator.isApplicationNameValid(osAppName)) {
            view.showApplicationNameError(locale.invalidApplicationNameError(), locale.invalidApplicationNameDetailError());
            return false;
        }
        if (view.getMode() == CREATE_NEW_PROJECT) {
            for (Pair<String, String> pair : osApplications) {
                if (pair.getFirst().equals(view.getOpenShiftProjectName()) && pair.getSecond().equals(osAppName)) {
                    view.showApplicationNameError(locale.existingApplicationNameError(), null);
                    return false;
                }
            }
        } else if (view.getMode() == SELECT_EXISTING_PROJECT
                   && view.getOpenShiftSelectedProject() != null) {
            for (Pair<String, String> pair : osApplications) {
                if (pair.getFirst().equals(view.getOpenShiftSelectedProject().getMetadata().getName())
                    && pair.getSecond().equals(osAppName)) {
                    view.showApplicationNameError(locale.existingApplicationNameError(), null);
                    return false;
                }
            }
        }
        view.hideApplicationNameError();
        return true;
    }

    private boolean isProjectNameValid() {
        String osProjectName = view.getOpenShiftProjectName();
        if (view.getMode() == CREATE_NEW_PROJECT) {
            if (!OpenshiftValidator.isProjectNameValid(osProjectName)) {
                view.showProjectNameError(locale.invalidProjectNameError(), locale.invalidProjectNameDetailError());
                return false;
            }
            for (Project project : osProjects) {
                if (project.getMetadata().getName().equals(osProjectName)) {
                    view.showProjectNameError(locale.existingProjectNameError(), null);
                    return false;
                }
            }
        } else if (view.getMode() == SELECT_EXISTING_PROJECT) {
            if (view.getOpenShiftSelectedProject() == null) {
                return false;
            }
        }
        view.hideProjectNameError();
        return true;
    }

    private boolean isVariablesListValid() {
        List<KeyValue> variables = view.getEnvironmentVariables();
        if (variables.isEmpty()) {
            view.hideVariablesError();
            return true;
        }
        for (KeyValue keyValue : variables) {
            if (!OpenshiftValidator.isEnvironmentVariableNameValid(keyValue.getKey())) {
                view.showVariablesError(locale.invalidVariablesError());
                return false;
            }
        }
        view.hideVariablesError();
        return true;
    }

    private boolean isLabelListValid() {
        List<KeyValue> labels = view.getLabels();
        if (labels.isEmpty()) {
            view.hideLabelsError();
            return true;
        }
        for (KeyValue keyValue : labels) {
            if (!OpenshiftValidator.isLabelNameValid(keyValue.getKey())
                || !OpenshiftValidator.isLabelValueValid(keyValue.getValue())) {
                view.showLabelsError(locale.invalidLabelsError(), locale.invalidLabelsDetailError());
                return false;
            }
        }
        view.hideLabelsError();
        return true;
    }

    @Override
    public void updateControls() {
        view.setDeployButtonEnabled(isApplicationNameValid()
                                    & isProjectNameValid()
                                    & isVariablesListValid()
                                    & isLabelListValid()
                                    & osImageStreams != null
                                    & view.getActiveImage() != null);
    }

    private ImageStream generateImageStream(String namespace, Map<String, String> labels) {
        return newDto(ImageStream.class).withApiVersion(API_VERSION)
                                        .withKind("ImageStream")
                                        .withMetadata(newDto(ObjectMeta.class).withName(osAppName)
                                                                              .withLabels(labels)
                                                                              .withNamespace(namespace));
    }


    private BuildConfig generateBuildConfig(String namespace, Map<String, String> labels) {
        BuildSource source = newDto(BuildSource.class).withType("Git")
                                                      .withGit(newDto(GitBuildSource.class)
                                                                       .withRef("master") //load branch
                                                                       .withUri(projectRemotes.get(0).getUrl()));

        SourceBuildStrategy sourceStrategy = newDto(SourceBuildStrategy.class).withFrom(newDto(ObjectReference.class)
                                                                                                .withKind("ImageStreamTag")
                                                                                                .withName(osActiveStreamTag.getMetadata()
                                                                                                                           .getName())
                                                                                                .withNamespace("openshift"));

        BuildStrategy strategy = newDto(BuildStrategy.class).withType("Source")
                                                            .withSourceStrategy(sourceStrategy);

        BuildTriggerPolicy generic = newDto(BuildTriggerPolicy.class).withType("generic")
                                                                     .withGeneric(newDto(WebHookTrigger.class)
                                                                                          .withSecret(generateSecret()));

        BuildTriggerPolicy github = newDto(BuildTriggerPolicy.class).withType("github")
                                                                    .withGithub(newDto(WebHookTrigger.class)
                                                                                        .withSecret(generateSecret()));

        BuildTriggerPolicy imageChange = newDto(BuildTriggerPolicy.class).withType("imageChange")
                                                                         .withImageChange(newDto(ImageChangeTrigger.class));

        BuildTriggerPolicy configChange = newDto(BuildTriggerPolicy.class).withType("ConfigChange");

        BuildOutput output = newDto(BuildOutput.class).withTo(newDto(ObjectReference.class).withName(osAppName + ":latest")
                                                                                           .withKind("ImageStreamTag"));

        final BuildConfigSpec spec = newDto(BuildConfigSpec.class).withSource(source)
                                                                  .withStrategy(strategy)
                                                                  .withTriggers(newArrayList(generic, github, imageChange, configChange))
                                                                  .withOutput(output);

        return newDto(BuildConfig.class).withApiVersion(API_VERSION)
                                        .withKind("BuildConfig")
                                        .withMetadata(newDto(ObjectMeta.class).withName(osAppName)
                                                                              .withLabels(labels)
                                                                              .withNamespace(namespace))
                                        .withSpec(spec);
    }

    private DeploymentConfig generateDeploymentConfig(String namespace, Map<String, String> labels) {
        Object exposedPorts = osActiveStreamTag.getImage().getDockerImageMetadata().getContainerConfig().getExposedPorts();
        List<ContainerPort> ports = parsePorts(exposedPorts);

        Map<String, String> templateLabels = new HashMap<>(labels);
        templateLabels.put("deploymentconfig", osAppName);

        List<EnvVar> env = newArrayList();

        for (KeyValue variable : view.getEnvironmentVariables()) {
            env.add(newDto(EnvVar.class)
                            .withName(variable.getKey())
                            .withValue(variable.getValue()));
        }

        final String steamTagName = osAppName + ":latest";
        PodSpec podSpec = newDto(PodSpec.class)
                .withContainers(newArrayList(newDto(Container.class)
                                                     .withImage(steamTagName)
                                                     .withName(osAppName)
                                                     .withPorts(ports)
                                                     .withEnv(env)));

        PodTemplateSpec template = newDto(PodTemplateSpec.class)
                .withMetadata(newDto(ObjectMeta.class)
                                      .withLabels(templateLabels))
                .withSpec(podSpec);

        DeploymentTriggerPolicy imageChange = newDto(DeploymentTriggerPolicy.class)
                .withType("ImageChange")
                .withImageChangeParams(newDto(DeploymentTriggerImageChangeParams.class).withAutomatic(true)
                                                                                       .withContainerNames(newArrayList(osAppName))
                                                                                       .withFrom(newDto(ObjectReference.class)
                                                                                                         .withKind("ImageStreamTag")
                                                                                                         .withName(steamTagName)));

        DeploymentTriggerPolicy configChange = newDto(DeploymentTriggerPolicy.class).withType("ConfigChange");

        return newDto(DeploymentConfig.class)
                .withApiVersion(API_VERSION)
                .withKind("DeploymentConfig")
                .withMetadata(newDto(ObjectMeta.class)
                                      .withName(osAppName)
                                      .withLabels(labels)
                                      .withNamespace(namespace))
                .withSpec(newDto(DeploymentConfigSpec.class).withReplicas(1)
                                                            .withSelector(Collections.singletonMap("deploymentconfig", osAppName))
                                                            .withTemplate(template)
                                                            .withTriggers(newArrayList(imageChange, configChange)));
    }

    private Service generateService(String namespace, ContainerPort port, Map<String, String> labels) {
        final ServicePort servicePort = newDto(ServicePort.class).withPort(port.getContainerPort())
                                                                 .withTargetPort(port.getContainerPort())
                                                                 .withProtocol(port.getProtocol());
        return newDto(Service.class).withApiVersion(API_VERSION)
                                    .withKind("Service")
                                    .withMetadata(newDto(ObjectMeta.class).withName(osAppName)
                                                                          .withLabels(labels)
                                                                          .withNamespace(namespace))
                                    .withSpec(newDto(ServiceSpec.class).withPorts(newArrayList(servicePort))
                                                                       .withSelector(singletonMap("deploymentconfig", osAppName)));
    }

    private Route generateRoute(@NotNull String namespace, Map<String, String> labels) {
        return newDto(Route.class).withKind("Route")
                                  .withApiVersion(API_VERSION)
                                  .withMetadata(newDto(ObjectMeta.class).withName(osAppName)
                                                                        .withLabels(labels)
                                                                        .withNamespace(namespace))
                                  .withSpec(newDto(RouteSpec.class).withTo(newDto(ObjectReference.class).withKind("Service")
                                                                                                        .withName(osAppName)));
    }

    private List<ContainerPort> parsePorts(Object exposedPorts) {
        if (!(exposedPorts instanceof JSONObject)) {
            return emptyList();
        }

        JSONObject ports = (JSONObject)exposedPorts;
        Jso jso = ports.getJavaScriptObject().cast();

        JsoArray<String> keys = jso.getKeys();

        List<ContainerPort> containerPorts = new ArrayList<>();

        for (int i = 0; i < keys.size(); i++) {
            String[] split = keys.get(i).split("/");
            if (split.length != 2) {
                continue;
            }
            containerPorts.add(newDto(ContainerPort.class).withContainerPort(Integer.valueOf(split[0]))
                                                          .withProtocol(split[1].toUpperCase()));
        }

        return containerPorts;
    }

    private <T> T newDto(Class<T> clazz) {
        return dtoFactory.createDto(clazz);
    }

    /**
     * @return the lowest container port
     */
    private ContainerPort getFirstPort(List<ContainerPort> ports) {
        if (ports.isEmpty()) {
            return null;
        }

        final Iterator<ContainerPort> portsIterator = ports.iterator();

        ContainerPort firstPort = portsIterator.next();
        while (portsIterator.hasNext()) {
            final ContainerPort port = portsIterator.next();
            if (port.getContainerPort() < firstPort.getContainerPort()) {
                firstPort = port;
            }
        }

        return firstPort;
    }

    /**
     * Generates secret key.
     *
     * @return secret key
     */
    private native String generateSecret() /*-{
        function s4() {
            return Math.floor((1 + Math.random()) * 0x10000)
                .toString(16)
                .substring(1);
        }

        return s4() + s4() + s4() + s4();
    }-*/;
}
