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
package org.eclipse.che.ide.ext.openshift.client.importapp;

import com.google.gwt.core.client.Scheduler;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.core.rest.shared.dto.ServiceError;
import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.gwt.client.ProjectTypeServiceClient;
import org.eclipse.che.api.project.shared.dto.ProjectTypeDto;
import org.eclipse.che.api.project.shared.dto.SourceEstimation;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.api.workspace.shared.dto.SourceStorageDto;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.event.ConfigureProjectEvent;
import org.eclipse.che.ide.api.event.project.CreateProjectEvent;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.project.wizard.ImportProjectNotificationSubscriberFactory;
import org.eclipse.che.ide.api.project.wizard.ProjectNotificationSubscriber;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftLocalizationConstant;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftServiceClient;
import org.eclipse.che.ide.ext.openshift.client.ValidateAuthenticationPresenter;
import org.eclipse.che.ide.ext.openshift.client.build.BuildsPresenter;
import org.eclipse.che.ide.ext.openshift.client.deploy.Application;
import org.eclipse.che.ide.ext.openshift.client.deploy.ApplicationManager;
import org.eclipse.che.ide.ext.openshift.client.oauth.OpenshiftAuthenticator;
import org.eclipse.che.ide.ext.openshift.client.oauth.OpenshiftAuthorizationHandler;
import org.eclipse.che.ide.ext.openshift.client.util.OpenshiftValidator;
import org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants;
import org.eclipse.che.ide.ext.openshift.shared.dto.BuildConfig;
import org.eclipse.che.ide.ext.openshift.shared.dto.Project;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_NAMESPACE_VARIABLE_NAME;
import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_APPLICATION_VARIABLE_NAME;
import static org.eclipse.che.ide.api.notification.StatusNotification.DisplayMode.EMERGE_MODE;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.FAIL;
import org.eclipse.che.ide.resource.Path;

/**
 * Presenter, which handles logic for importing OpenShift application to Che.
 *
 * @author Anna Shumilova
 * @author Vitaliy Guliy
 */
public class ImportApplicationPresenter extends ValidateAuthenticationPresenter implements ImportApplicationView.ActionDelegate {

    private final ImportApplicationView          view;
    private final OpenshiftLocalizationConstant  locale;
    private final OpenshiftServiceClient         openShiftClient;
    private final ProjectServiceClient           projectServiceClient;
    private final DtoFactory                     dtoFactory;
    private final ProjectNotificationSubscriber  importProjectNotificationSubscriber;
    private final EventBus                       eventBus;
    private final List<String>                   cheProjects;
    private final List<Project>                  projectList;
    private final Map<String, List<BuildConfig>> buildConfigMap;
    private final NotificationManager            notificationManager;
    private final BuildsPresenter                buildsPresenter;
    private final ApplicationManager             applicationManager;
    private final AppContext                     appContext;
    private       BuildConfig                    selectedBuildConfig;
    private       ProjectTypeServiceClient       projectTypeServiceClient;


    @Inject
    public ImportApplicationPresenter(OpenshiftLocalizationConstant locale, ImportApplicationView view,
                                      OpenshiftServiceClient openShiftClient,
                                      ProjectServiceClient projectServiceClient,
                                      ProjectTypeServiceClient projectTypeServiceClient,
                                      NotificationManager notificationManager,
                                      OpenshiftAuthenticator openshiftAuthenticator,
                                      OpenshiftAuthorizationHandler openshiftAuthorizationHandler,
                                      ImportProjectNotificationSubscriberFactory importProjectNotificationSubscriberFactory,
                                      EventBus eventBus,
                                      DtoFactory dtoFactory,
                                      BuildsPresenter buildsPresenter,
                                      ApplicationManager applicationManager,
                                      AppContext appContext) {
        super(openshiftAuthenticator, openshiftAuthorizationHandler, locale, notificationManager);
        this.view = view;
        this.applicationManager = applicationManager;
        this.view.setDelegate(this);
        this.openShiftClient = openShiftClient;
        this.projectServiceClient = projectServiceClient;
        this.projectTypeServiceClient = projectTypeServiceClient;
        this.dtoFactory = dtoFactory;
        this.notificationManager = notificationManager;
        this.importProjectNotificationSubscriber = importProjectNotificationSubscriberFactory.createSubscriber();
        this.eventBus = eventBus;
        this.locale = locale;
        this.buildsPresenter = buildsPresenter;
        this.appContext = appContext;
        projectList = new ArrayList<>();
        buildConfigMap = new HashMap<>();
        cheProjects = new ArrayList<>();
    }

    @Override
    protected void onSuccessAuthentication() {
        projectList.clear();
        buildConfigMap.clear();
        selectedBuildConfig = null;

        view.setErrorMessage("");
        view.setProjectName("", false);
        view.setProjectDescription("");
        view.setApplicationInfo(null);

        view.enableImportButton(false);
        view.animateImportButton(false);
        view.enableCancelButton(true);

        view.setBuildConfigs(buildConfigMap);
        view.enableBuildConfigs(true);

        view.enableNameField(false);
        view.enableDescriptionField(false);

        view.showView();

        view.showLoadingBuildConfigs("Loading projects...");

        loadCheProjects();
    }

    /**
     * Load che projects for following verifications.
     */
    private void loadCheProjects() {
        projectServiceClient.getProjects(appContext.getDevMachine()).then(new Operation<List<ProjectConfigDto>>() {
            @Override
            public void apply(List<ProjectConfigDto> result) throws OperationException {
                cheProjects.clear();
                for (ProjectConfigDto project : result) {
                    cheProjects.add(project.getName());
                }

                loadOpenShiftProjects();
            }
        }).catchError(handleError());
    }

    private Operation<PromiseError> handleError() {
        return new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                final ServiceError serviceError = dtoFactory.createDtoFromJson(arg.getMessage(), ServiceError.class);
                notificationManager.notify(serviceError.getMessage(), FAIL, EMERGE_MODE);
            }
        };
    }

    /**
     * Load OpenShift Project and Application data.
     */
    private void loadOpenShiftProjects() {
        openShiftClient.getProjects().then(new Operation<List<Project>>() {
            @Override
            public void apply(List<Project> result) throws OperationException {
                if (result.isEmpty()) {
                    view.setBuildConfigs(buildConfigMap);
                    return;
                }

                projectList.addAll(result);
                for (Project project : result) {
                    getBuildConfigs(project.getMetadata().getName());
                }
            }
        }).catchError(handleError());
    }

    /**
     * Get OpenShift Build Configs by namespace.
     *
     * @param namespace
     *         namespace
     */
    private void getBuildConfigs(final String namespace) {
        openShiftClient.getBuildConfigs(namespace).then(new Operation<List<BuildConfig>>() {
            @Override
            public void apply(List<BuildConfig> result) throws OperationException {
                buildConfigMap.put(namespace, result);

                if (buildConfigMap.size() == projectList.size()) {
                    view.setBuildConfigs(buildConfigMap);
                }
            }
        }).catchError(handleError());
    }

    @Override
    public void onImportApplicationClicked() {
        view.enableBuildConfigs(false);
        view.enableNameField(false);
        view.enableDescriptionField(false);

        view.enableImportButton(false);
        view.animateImportButton(true);

        view.enableCancelButton(false);
        view.setBlocked(true);

        importProject();
    }

    /**
     * Fills project data and imports project.
     */
    private void importProject() {
        final ProjectConfigDto projectConfig = dtoFactory.createDto(ProjectConfigDto.class)
                                                         .withSource(dtoFactory.createDto(SourceStorageDto.class));

        projectConfig.setName(view.getProjectName());

        Map<String, String> importOptions = new HashMap<String, String>();
        String branch = selectedBuildConfig.getSpec().getSource().getGit().getRef();
        if (branch != null && !branch.isEmpty()) {
            importOptions.put("branch", selectedBuildConfig.getSpec().getSource().getGit().getRef());
        }

        String contextDir = selectedBuildConfig.getSpec().getSource().getContextDir();
        if (contextDir != null && !contextDir.isEmpty()) {
            importOptions.put("keepDirectory", contextDir);
        }

        Map<String, List<String>> attributes = new HashMap<String, List<String>>();
        attributes.put(OPENSHIFT_APPLICATION_VARIABLE_NAME, Arrays.asList(selectedBuildConfig.getMetadata().getName()));

        attributes.put(OPENSHIFT_NAMESPACE_VARIABLE_NAME, Arrays.asList(selectedBuildConfig.getMetadata().getNamespace()));

        projectConfig.withMixins(Arrays.asList(OpenshiftProjectTypeConstants.OPENSHIFT_PROJECT_TYPE_ID))
                     .withAttributes(attributes);

        projectConfig.getSource().withType("git").withParameters(importOptions)
                     .withLocation(selectedBuildConfig.getSpec().getSource().getGit().getUri());

        projectConfig.withType("blank").withDescription(view.getProjectDescription());

        importProjectNotificationSubscriber.subscribe(view.getProjectName());

        try {
            projectServiceClient.importProject(appContext.getDevMachine(), view.getProjectName(), projectConfig.getSource())
                                .then(new Operation<Void>() {
                                    @Override
                                    public void apply(Void arg) throws OperationException {
                                        resolveProject(projectConfig);
                                    }
                                })
                                .catchError(new Operation<PromiseError>() {
                                    @Override
                                    public void apply(PromiseError promiseError) throws OperationException {
                                        createProjectFailure(promiseError.getCause());
                                    }
                                });
        } catch (Exception e) {
            createProjectFailure(e);
        }
    }

    /**
     * Resolves project's type.
     *
     * @param projectConfig
     *         project's config
     */
    private void resolveProject(final ProjectConfigDto projectConfig) {
        final String projectName = projectConfig.getName();
        final String workspaceId = appContext.getWorkspaceId();
        projectServiceClient.resolveSources(appContext.getDevMachine(), Path.valueOf(projectConfig.getPath()))
                            .then(new Operation<List<SourceEstimation>>() {
                                @Override
                                public void apply(List<SourceEstimation> result) throws OperationException {
                                    for (SourceEstimation estimation : result) {
                                        final Promise<ProjectTypeDto> projectTypePromise =
                                                projectTypeServiceClient.getProjectType(appContext.getDevMachine(), estimation.getType());
                                        projectTypePromise.then(new Operation<ProjectTypeDto>() {
                                            @Override
                                            public void apply(ProjectTypeDto arg) throws OperationException {
                                                if (arg.isPrimaryable()) {
                                                    updateProject(projectConfig.withType(arg.getId()));
                                                }
                                            }
                                        });
                                    }
                                }
                            })
                            .catchError(new Operation<PromiseError>() {
                                @Override
                                public void apply(PromiseError promiseError) throws OperationException {
                                    importProjectNotificationSubscriber.onFailure(promiseError.getMessage());
                                    createProjectFailure(promiseError.getCause());
                                }
                            });
    }

    /**
     * Updates Che project's type.
     *
     * @param projectConfig
     *         project config
     */
    private void updateProject(final ProjectConfigDto projectConfig) {
        try {
            projectServiceClient.updateProject(appContext.getDevMachine(), Path.valueOf(projectConfig.getPath()), projectConfig)
                                .then(new Operation<ProjectConfigDto>() {
                                    @Override
                                    public void apply(ProjectConfigDto result) throws OperationException {
                                        view.animateImportButton(false);
                                        view.setBlocked(false);
                                        view.closeView();

                                        final String namespace = result.getAttributes().get(OPENSHIFT_NAMESPACE_VARIABLE_NAME).get(0);
                                        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                                            @Override
                                            public void execute() {
                                                buildsPresenter.newApplicationCreated(namespace);
                                            }
                                        });

                                        eventBus.fireEvent(new CreateProjectEvent(result));

                                        if (!result.getProblems().isEmpty()) {
                                            eventBus.fireEvent(new ConfigureProjectEvent(result));
                                        }

                                        importProjectNotificationSubscriber.onSuccess();

                                        updateApplicationLabel(selectedBuildConfig);
                                    }
                                })
                                .catchError(new Operation<PromiseError>() {
                                    @Override
                                    public void apply(PromiseError promiseError) throws OperationException {
                                        createProjectFailure(promiseError.getCause());
                                    }
                                });
        } catch (Exception e) {
            createProjectFailure(e);
        }
    }

    /**
     * Handles errors when creating a project.
     *
     * @param exception
     *         cause
     */
    private void createProjectFailure(Throwable exception) {
        view.animateImportButton(false);
        view.enableImportButton(true);
        view.enableCancelButton(true);
        view.setBlocked(false);

        view.enableNameField(true);
        view.enableDescriptionField(true);

        view.setErrorMessage(exception.getMessage());
        importProjectNotificationSubscriber.onFailure(exception.getMessage());
    }

    @Override
    public void onCancelClicked() {
        view.closeView();
    }

    @Override
    public void onBuildConfigSelected(BuildConfig buildConfig) {
        selectedBuildConfig = buildConfig;

        if (buildConfig != null) {
            view.enableNameField(true);
            view.enableDescriptionField(true);

            view.setProjectName(buildConfig.getMetadata().getName(), true);
            view.setApplicationInfo(buildConfig);
        }

        view.enableImportButton((buildConfig != null && view.getProjectName() != null && !view.getProjectName().isEmpty()));
    }

    @Override
    public void onProjectNameChanged(String name) {
        view.enableImportButton(selectedBuildConfig != null & isCheProjectNameValid(view.getProjectName()));
    }

    private boolean isCheProjectNameValid(String projectName) {
        if (cheProjects.contains(projectName)) {
            view.showCheProjectNameError(locale.existingProjectNameError(), null);
            return false;
        }
        if (!OpenshiftValidator.isProjectNameValid(projectName)) {
            view.showCheProjectNameError(locale.invalidProjectNameError(), locale.invalidProjectNameDetailError());
            return false;
        }
        view.hideCheProjectNameError();
        return true;
    }

    private void updateApplicationLabel(final BuildConfig buildConfig) {
        applicationManager.findApplication(buildConfig)
                          .thenPromise(new Function<Application, Promise<Application>>() {
                              @Override
                              public Promise<Application> apply(Application application) {
                                  return applicationManager.updateOpenshiftApplication(application, buildConfig.getMetadata().getName());
                              }
                          });
    }
}
