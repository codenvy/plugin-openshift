/**
 * ***************************************************************************** Copyright (c)
 * 2012-2017 Codenvy, S.A. All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: Codenvy, S.A. - initial API and implementation
 * *****************************************************************************
 */
package org.eclipse.che.ide.ext.openshift.client.build;

import static org.eclipse.che.ide.api.notification.StatusNotification.DisplayMode.EMERGE_MODE;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.FAIL;
import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_APPLICATION_VARIABLE_NAME;
import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_NAMESPACE_VARIABLE_NAME;
import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_PROJECT_TYPE_ID;
import static org.eclipse.che.ide.workspace.perspectives.project.ProjectPerspective.PROJECT_PERSPECTIVE_ID;

import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.ide.api.action.AbstractPerspectiveAction;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.resources.Project;
import org.eclipse.che.ide.api.resources.Resource;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftLocalizationConstant;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftServiceClient;
import org.eclipse.che.ide.ext.openshift.client.oauth.OpenshiftAuthorizationHandler;
import org.eclipse.che.ide.ext.openshift.shared.dto.Build;
import org.eclipse.che.ide.ext.openshift.shared.dto.BuildConfig;

/** @author Sergii Leschenko */
public class StartBuildAction extends AbstractPerspectiveAction {
  private final OpenshiftAuthorizationHandler authorizationHandler;
  private final AppContext appContext;
  private final OpenshiftServiceClient openshiftService;
  private final NotificationManager notificationManager;
  private final OpenshiftLocalizationConstant locale;
  private final BuildsPresenter buildsPresenter;

  @Inject
  public StartBuildAction(
      OpenshiftAuthorizationHandler authorizationHandler,
      AppContext appContext,
      OpenshiftServiceClient openshiftService,
      NotificationManager notificationManager,
      OpenshiftLocalizationConstant locale,
      BuildsPresenter buildsPresenter) {
    super(
        Collections.singletonList(PROJECT_PERSPECTIVE_ID),
        locale.startBuildTitle(),
        null,
        null,
        null);
    this.authorizationHandler = authorizationHandler;
    this.appContext = appContext;
    this.openshiftService = openshiftService;
    this.notificationManager = notificationManager;
    this.locale = locale;
    this.buildsPresenter = buildsPresenter;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    final Resource resource = appContext.getResource();
    if (resource != null && resource.getRelatedProject().isPresent()) {
      final Project relatedProject = resource.getRelatedProject().get();
      final String namespace = relatedProject.getAttribute(OPENSHIFT_NAMESPACE_VARIABLE_NAME);
      final String application = relatedProject.getAttribute(OPENSHIFT_APPLICATION_VARIABLE_NAME);
      openshiftService
          .getBuildConfigs(namespace, application)
          .thenPromise(
              new Function<List<BuildConfig>, Promise<BuildConfig>>() {
                @Override
                public Promise<BuildConfig> apply(List<BuildConfig> arg) throws FunctionException {
                  if (arg.isEmpty() || arg.size() > 1) {
                    throw new FunctionException(locale.noBuildConfigError());
                  }

                  return Promises.resolve(arg.get(0));
                }
              })
          .thenPromise(
              new Function<BuildConfig, Promise<Build>>() {
                @Override
                public Promise<Build> apply(BuildConfig buildConfig) throws FunctionException {
                  return openshiftService.startBuild(
                      namespace, buildConfig.getMetadata().getName());
                }
              })
          .then(
              new Operation<Build>() {
                @Override
                public void apply(Build startedBuild) throws OperationException {
                  buildsPresenter.newBuildStarted(startedBuild);
                }
              })
          .catchError(
              new Operation<PromiseError>() {
                @Override
                public void apply(PromiseError arg) throws OperationException {
                  notificationManager.notify(
                      locale.startBuildError() + " " + arg.getMessage(), FAIL, EMERGE_MODE);
                }
              });
    }
  }

  @Override
  public void updateInPerspective(@NotNull ActionEvent event) {
    final Resource resource = appContext.getResource();
    if (resource != null && resource.getRelatedProject().isPresent()) {
      final Project currentProject = resource.getRelatedProject().get();
      event.getPresentation().setVisible(true);
      event
          .getPresentation()
          .setEnabled(
              authorizationHandler.isLoggedIn()
                  && currentProject != null
                  && currentProject.getMixins().contains(OPENSHIFT_PROJECT_TYPE_ID));
    } else {
      event.getPresentation().setEnabledAndVisible(false);
    }
  }
}
