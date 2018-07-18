/**
 * ***************************************************************************** Copyright (c)
 * 2012-2017 Codenvy, S.A. All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: Codenvy, S.A. - initial API and implementation
 * *****************************************************************************
 */
package org.eclipse.che.ide.ext.openshift.client.deploy;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Collections.singletonList;
import static org.eclipse.che.api.project.shared.Constants.VCS_PROVIDER_NAME;
import static org.eclipse.che.ide.api.notification.StatusNotification.DisplayMode.EMERGE_MODE;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.FAIL;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.SUCCESS;
import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_APPLICATION_VARIABLE_NAME;
import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_NAMESPACE_VARIABLE_NAME;
import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_PROJECT_TYPE_ID;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.che.api.core.rest.shared.dto.ServiceError;
import org.eclipse.che.api.git.shared.Remote;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.dialogs.DialogFactory;
import org.eclipse.che.ide.api.git.GitServiceClient;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.project.MutableProjectConfig;
import org.eclipse.che.ide.api.resources.Resource;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftLocalizationConstant;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftServiceClient;
import org.eclipse.che.ide.ext.openshift.client.ValidateAuthenticationPresenter;
import org.eclipse.che.ide.ext.openshift.client.oauth.OpenshiftAuthenticator;
import org.eclipse.che.ide.ext.openshift.client.oauth.OpenshiftAuthorizationHandler;
import org.eclipse.che.ide.ext.openshift.shared.dto.BuildConfig;
import org.eclipse.che.ide.ext.openshift.shared.dto.BuildSource;
import org.eclipse.che.ide.ext.openshift.shared.dto.GitBuildSource;
import org.eclipse.che.ide.ext.openshift.shared.dto.Project;

/**
 * Presenter, which handles logic for linking current project with OpenShift application.
 *
 * @author Anna Shumilova
 * @author Sergii Leschenko
 * @author Vlad Zhukovskyi
 */
public class LinkProjectWithExistingApplicationPresenter extends ValidateAuthenticationPresenter
    implements LinkProjectWithExistingApplicationView.ActionDelegate {

  private final LinkProjectWithExistingApplicationView view;
  private final AppContext appContext;
  private final OpenshiftLocalizationConstant locale;
  private final NotificationManager notificationManager;
  private final DialogFactory dialogFactory;
  private final OpenshiftServiceClient openShiftClient;
  private final GitServiceClient gitService;
  private final DtoFactory dtoFactory;
  private final Map<String, List<BuildConfig>> buildConfigMap;
  private final ApplicationManager applicationManager;
  private BuildConfig selectedBuildConfig;
  private org.eclipse.che.ide.api.resources.Project cdProject;

  @Inject
  public LinkProjectWithExistingApplicationPresenter(
      OpenshiftLocalizationConstant locale,
      LinkProjectWithExistingApplicationView view,
      OpenshiftServiceClient openShiftClient,
      GitServiceClient gitService,
      NotificationManager notificationManager,
      DialogFactory dialogFactory,
      AppContext appContext,
      DtoFactory dtoFactory,
      OpenshiftAuthenticator openshiftAuthenticator,
      OpenshiftAuthorizationHandler openshiftAuthorizationHandler,
      ApplicationManager applicationManager) {
    super(openshiftAuthenticator, openshiftAuthorizationHandler, locale, notificationManager);
    this.view = view;
    this.applicationManager = applicationManager;
    this.view.setDelegate(this);

    this.appContext = appContext;
    this.locale = locale;
    this.notificationManager = notificationManager;
    this.dialogFactory = dialogFactory;
    this.dtoFactory = dtoFactory;
    this.openShiftClient = openShiftClient;
    this.gitService = gitService;
    buildConfigMap = new HashMap<>();
  }

  /** Show dialog box for managing linking project to OpenShift application. */
  @Override
  protected void onSuccessAuthentication() {
    final Resource resource = appContext.getResource();

    checkNotNull(resource);

    final Optional<org.eclipse.che.ide.api.resources.Project> projectOptional =
        resource.getRelatedProject();

    checkState(projectOptional.isPresent());

    cdProject = projectOptional.get();

    if (isNullOrEmpty(cdProject.getAttribute(VCS_PROVIDER_NAME))
        || !cdProject.getAttribute(VCS_PROVIDER_NAME).equals("git")) {
      dialogFactory
          .createMessageDialog(
              locale.notGitRepositoryWarningTitle(),
              locale.notGitRepositoryWarning(cdProject.getName()),
              null)
          .show();

      return;
    }

    gitService
        .remoteList(cdProject.getLocation(), null, true)
        .then(
            new Operation<List<Remote>>() {
              @Override
              public void apply(List<Remote> result) throws OperationException {
                if (result.isEmpty()) {
                  dialogFactory
                      .createMessageDialog(
                          locale.noGitRemoteRepositoryWarningTitle(),
                          locale.noGitRemoteRepositoryWarning(cdProject.getName()),
                          null)
                      .show();

                  return;
                }

                view.setGitRemotes(result);

                buildConfigMap.clear();
                selectedBuildConfig = null;
                view.enableLinkButton(false);
                view.setBuildConfigGitUrl("");

                loadOpenShiftData();
              }
            })
        .catchError(
            new Operation<PromiseError>() {
              @Override
              public void apply(PromiseError arg) throws OperationException {
                notificationManager.notify(
                    locale.getGitRemoteRepositoryError(cdProject.getName()), FAIL, EMERGE_MODE);
              }
            });
  }

  @Override
  public void onLinkApplicationClicked() {
    final String applicationName = selectedBuildConfig.getMetadata().getName();
    applicationManager
        .findApplication(selectedBuildConfig)
        .thenPromise(
            new Function<Application, Promise<Application>>() {
              @Override
              public Promise<Application> apply(Application application) {
                BuildSource buildSource =
                    dtoFactory
                        .createDto(BuildSource.class)
                        .withType("Git")
                        .withGit(
                            dtoFactory
                                .createDto(GitBuildSource.class)
                                .withUri(view.getGitRemoteUrl()))
                        .withContextDir(cdProject.getPath());

                return applicationManager.updateOpenshiftApplication(
                    application, applicationName, buildSource);
              }
            })
        .then(
            new Operation<Application>() {
              @Override
              public void apply(Application arg) throws OperationException {
                view.closeView();
                notificationManager.notify(
                    locale.linkProjectWithExistingUpdateBuildConfigSuccess(applicationName),
                    SUCCESS,
                    EMERGE_MODE);
                markAsOpenshiftProject(
                    selectedBuildConfig.getMetadata().getNamespace(), applicationName);
              }
            })
        .catchError(handleError());
  }

  private void markAsOpenshiftProject(String namespace, final String application) {
    MutableProjectConfig updateConfig = new MutableProjectConfig(cdProject);

    if (!updateConfig.getMixins().contains(OPENSHIFT_PROJECT_TYPE_ID)) {
      updateConfig.getMixins().add(OPENSHIFT_PROJECT_TYPE_ID);
    }

    updateConfig.getAttributes().put(OPENSHIFT_NAMESPACE_VARIABLE_NAME, singletonList(namespace));
    updateConfig
        .getAttributes()
        .put(OPENSHIFT_APPLICATION_VARIABLE_NAME, singletonList(application));

    cdProject
        .update()
        .withBody(updateConfig)
        .send()
        .then(
            new Operation<org.eclipse.che.ide.api.resources.Project>() {
              @Override
              public void apply(org.eclipse.che.ide.api.resources.Project project)
                  throws OperationException {
                notificationManager.notify(
                    locale.linkProjectWithExistingSuccess(project.getName(), application),
                    SUCCESS,
                    EMERGE_MODE);
              }
            })
        .catchError(
            new Operation<PromiseError>() {
              @Override
              public void apply(PromiseError promiseError) throws OperationException {
                notificationManager.notify(promiseError.getMessage(), FAIL, EMERGE_MODE);
              }
            });
  }

  @Override
  public void onCancelClicked() {
    view.closeView();
  }

  @Override
  public void onBuildConfigSelected(BuildConfig buildConfig) {
    selectedBuildConfig = buildConfig;

    if (buildConfig != null) {
      view.setBuildConfigGitUrl(buildConfig.getSpec().getSource().getGit().getUri());
      view.setReplaceWarningMessage(
          locale.linkProjectWithExistingReplaceWarning(
              buildConfig.getMetadata().getName(), cdProject.getName()));
    }
    view.enableLinkButton((buildConfig != null));
  }

  /** Load OpenShift Project and Application data. */
  private void loadOpenShiftData() {
    openShiftClient
        .getProjects()
        .then(
            new Operation<List<Project>>() {
              @Override
              public void apply(List<Project> result) throws OperationException {
                if (result.isEmpty()) {
                  dialogFactory
                      .createMessageDialog(
                          locale.deployProjectWindowNoProjectsTitle(),
                          locale.deployProjectWindowNoProjects(),
                          null)
                      .show();
                  return;
                }
                view.showView();
                for (Project project : result) {
                  loadBuildConfigs(project.getMetadata().getName());
                }
              }
            })
        .catchError(handleError());
  }

  /**
   * Load OpenShift Build Configs by specified namespace.
   *
   * @param namespace namespace for loading build configs
   */
  private void loadBuildConfigs(final String namespace) {
    openShiftClient
        .getBuildConfigs(namespace)
        .then(
            new Operation<List<BuildConfig>>() {
              @Override
              public void apply(List<BuildConfig> result) throws OperationException {
                buildConfigMap.put(namespace, result);
                view.setBuildConfigs(buildConfigMap);
              }
            })
        .catchError(handleError());
  }

  private Operation<PromiseError> handleError() {
    return new Operation<PromiseError>() {
      @Override
      public void apply(PromiseError arg) throws OperationException {
        final ServiceError serviceError =
            dtoFactory.createDtoFromJson(arg.getMessage(), ServiceError.class);
        notificationManager.notify(serviceError.getMessage(), FAIL, EMERGE_MODE);
      }
    };
  }
}
