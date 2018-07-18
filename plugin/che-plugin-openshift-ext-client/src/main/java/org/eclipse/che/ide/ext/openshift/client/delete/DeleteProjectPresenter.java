/**
 * ***************************************************************************** Copyright (c)
 * 2012-2017 Codenvy, S.A. All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: Codenvy, S.A. - initial API and implementation
 * *****************************************************************************
 */
package org.eclipse.che.ide.ext.openshift.client.delete;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.eclipse.che.ide.api.notification.StatusNotification.DisplayMode.EMERGE_MODE;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.SUCCESS;
import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_APPLICATION_VARIABLE_NAME;
import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_NAMESPACE_VARIABLE_NAME;
import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_PROJECT_TYPE_ID;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.dialogs.ConfirmCallback;
import org.eclipse.che.ide.api.dialogs.DialogFactory;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.project.MutableProjectConfig;
import org.eclipse.che.ide.api.resources.Project;
import org.eclipse.che.ide.api.resources.Resource;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftLocalizationConstant;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftServiceClient;
import org.eclipse.che.ide.ext.openshift.client.ValidateAuthenticationPresenter;
import org.eclipse.che.ide.ext.openshift.client.oauth.OpenshiftAuthenticator;
import org.eclipse.che.ide.ext.openshift.client.oauth.OpenshiftAuthorizationHandler;

/** @author Vlad Zhukovskyi */
@Singleton
public class DeleteProjectPresenter extends ValidateAuthenticationPresenter {

  private final AppContext appContext;
  private final DialogFactory dialogFactory;
  private final OpenshiftLocalizationConstant locale;
  private final OpenshiftServiceClient service;
  private final NotificationManager notificationManager;

  @Inject
  protected DeleteProjectPresenter(
      OpenshiftAuthenticator openshiftAuthenticator,
      OpenshiftAuthorizationHandler openshiftAuthorizationHandler,
      AppContext appContext,
      DialogFactory dialogFactory,
      OpenshiftLocalizationConstant locale,
      OpenshiftServiceClient service,
      NotificationManager notificationManager) {
    super(openshiftAuthenticator, openshiftAuthorizationHandler, locale, notificationManager);
    this.appContext = appContext;
    this.dialogFactory = dialogFactory;
    this.locale = locale;
    this.service = service;
    this.notificationManager = notificationManager;
  }

  @Override
  protected void onSuccessAuthentication() {
    final Resource resource = appContext.getResource();

    checkNotNull(resource);

    final Optional<Project> projectOptional = resource.getRelatedProject();

    checkState(projectOptional.isPresent());

    final Project project = projectOptional.get();

    checkState(project.getMixins().contains(OPENSHIFT_PROJECT_TYPE_ID));

    final String namespace = project.getAttribute(OPENSHIFT_NAMESPACE_VARIABLE_NAME);

    dialogFactory
        .createConfirmDialog(
            locale.deleteProjectDialogTitle(),
            locale.deleteSingleAppProjectLabel(namespace),
            new ConfirmCallback() {
              @Override
              public void accepted() {
                service
                    .deleteProject(namespace)
                    .then(
                        new Operation<Void>() {
                          @Override
                          public void apply(Void arg) throws OperationException {
                            notificationManager.notify(
                                locale.deleteProjectSuccess(namespace), SUCCESS, EMERGE_MODE);

                            MutableProjectConfig updateConfig = new MutableProjectConfig(project);
                            updateConfig.getMixins().remove(OPENSHIFT_PROJECT_TYPE_ID);
                            updateConfig.getAttributes().remove(OPENSHIFT_NAMESPACE_VARIABLE_NAME);
                            updateConfig
                                .getAttributes()
                                .remove(OPENSHIFT_APPLICATION_VARIABLE_NAME);

                            project
                                .update()
                                .withBody(updateConfig)
                                .send()
                                .then(
                                    new Operation<Project>() {
                                      @Override
                                      public void apply(Project arg) throws OperationException {
                                        notificationManager.notify(
                                            locale.projectSuccessfullyReset(project.getName()),
                                            SUCCESS,
                                            EMERGE_MODE);
                                      }
                                    });
                          }
                        });
              }
            },
            null)
        .show();
  }
}
