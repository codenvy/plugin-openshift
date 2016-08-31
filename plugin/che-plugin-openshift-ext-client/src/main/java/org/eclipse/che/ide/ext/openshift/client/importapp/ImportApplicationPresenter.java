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

import org.eclipse.che.api.core.model.project.SourceStorage;
import org.eclipse.che.api.core.rest.shared.dto.ServiceError;
import org.eclipse.che.api.project.shared.Constants;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.project.MutableProjectConfig;
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
import org.eclipse.che.ide.projectimport.wizard.ProjectResolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.eclipse.che.ide.api.notification.StatusNotification.DisplayMode.EMERGE_MODE;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.FAIL;
import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_APPLICATION_VARIABLE_NAME;
import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_NAMESPACE_VARIABLE_NAME;

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
    private final DtoFactory                     dtoFactory;
    private final ProjectNotificationSubscriber  importProjectNotificationSubscriber;
    private final List<String>                   cheProjects;
    private final List<Project>                  projectList;
    private final Map<String, List<BuildConfig>> buildConfigMap;
    private final NotificationManager            notificationManager;
    private final BuildsPresenter                buildsPresenter;
    private final ApplicationManager             applicationManager;
    private       ProjectResolver                projectResolver;
    private final AppContext                     appContext;
    private       BuildConfig                    selectedBuildConfig;


    @Inject
    public ImportApplicationPresenter(OpenshiftLocalizationConstant locale, ImportApplicationView view,
                                      OpenshiftServiceClient openShiftClient,
                                      NotificationManager notificationManager,
                                      OpenshiftAuthenticator openshiftAuthenticator,
                                      OpenshiftAuthorizationHandler openshiftAuthorizationHandler,
                                      ImportProjectNotificationSubscriberFactory importProjectNotificationSubscriberFactory,
                                      DtoFactory dtoFactory,
                                      BuildsPresenter buildsPresenter,
                                      ApplicationManager applicationManager,
                                      AppContext appContext,
                                      ProjectResolver projectResolver) {
        super(openshiftAuthenticator, openshiftAuthorizationHandler, locale, notificationManager);
        this.view = view;
        this.applicationManager = applicationManager;
        this.projectResolver = projectResolver;
        this.view.setDelegate(this);
        this.openShiftClient = openShiftClient;
        this.dtoFactory = dtoFactory;
        this.notificationManager = notificationManager;
        this.importProjectNotificationSubscriber = importProjectNotificationSubscriberFactory.createSubscriber();
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
        cheProjects.clear();
        for (org.eclipse.che.ide.api.resources.Project project : appContext.getProjects()) {
            cheProjects.add(project.getName());
        }

        loadOpenShiftProjects();
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
        final Map<String, String> importOptions = new HashMap<String, String>();
        String branch = selectedBuildConfig.getSpec().getSource().getGit().getRef();
        if (branch != null && !branch.isEmpty()) {
            importOptions.put("branch", selectedBuildConfig.getSpec().getSource().getGit().getRef());
        }

        String contextDir = selectedBuildConfig.getSpec().getSource().getContextDir();
        if (contextDir != null && !contextDir.isEmpty()) {
            importOptions.put("keepDirectory", contextDir);
        }

        Map<String, List<String>> attributes = new HashMap<String, List<String>>();
        attributes.put(OPENSHIFT_APPLICATION_VARIABLE_NAME, singletonList(selectedBuildConfig.getMetadata().getName()));

        attributes.put(OPENSHIFT_NAMESPACE_VARIABLE_NAME, singletonList(selectedBuildConfig.getMetadata().getNamespace()));

        importProjectNotificationSubscriber.subscribe(view.getProjectName());

        MutableProjectConfig importConfig = new MutableProjectConfig();
        importConfig.setName(view.getProjectName());
        importConfig.setMixins(singletonList(OpenshiftProjectTypeConstants.OPENSHIFT_PROJECT_TYPE_ID));
        importConfig.setAttributes(attributes);
        importConfig.setType(Constants.BLANK_ID);
        importConfig.setDescription(view.getProjectDescription());
        importConfig.setSource(new SourceStorage() {
            @Override
            public String getType() {
                return "git";
            }

            @Override
            public String getLocation() {
                return selectedBuildConfig.getSpec().getSource().getGit().getUri();
            }

            @Override
            public Map<String, String> getParameters() {
                return importOptions;
            }
        });

        appContext.getWorkspaceRoot()
                  .importProject()
                  .withBody(importConfig)
                  .send()
                  .thenPromise(
                          new Function<org.eclipse.che.ide.api.resources.Project, Promise<org.eclipse.che.ide.api.resources.Project>>() {
                              @Override
                              public Promise<org.eclipse.che.ide.api.resources.Project> apply(
                                      org.eclipse.che.ide.api.resources.Project project) throws FunctionException {

                                  return projectResolver.resolve(project).then(new Operation<org.eclipse.che.ide.api.resources.Project>() {
                                      @Override
                                      public void apply(org.eclipse.che.ide.api.resources.Project project) throws OperationException {
                                          view.animateImportButton(false);
                                          view.setBlocked(false);
                                          view.closeView();

                                          final String namespace = project.getAttributes().get(OPENSHIFT_NAMESPACE_VARIABLE_NAME).get(0);
                                          Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                                              @Override
                                              public void execute() {
                                                  buildsPresenter.newApplicationCreated(namespace);
                                              }
                                          });

                                          importProjectNotificationSubscriber.onSuccess();

                                          updateApplicationLabel(selectedBuildConfig);
                                      }
                                  }).catchError(new Operation<PromiseError>() {
                                      @Override
                                      public void apply(PromiseError promiseError) throws OperationException {
                                          createProjectFailure(promiseError.getCause());
                                      }
                                  });
                              }
                          })
                  .catchError(new Operation<PromiseError>() {
                      @Override
                      public void apply(PromiseError promiseError) throws OperationException {
                          createProjectFailure(promiseError.getCause());
                      }
                  });
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
