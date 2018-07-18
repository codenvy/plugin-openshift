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

import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_PROJECT_TYPE_ID;
import static org.eclipse.che.ide.workspace.perspectives.project.ProjectPerspective.PROJECT_PERSPECTIVE_ID;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
import javax.validation.constraints.NotNull;
import org.eclipse.che.ide.api.action.AbstractPerspectiveAction;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.resources.Project;
import org.eclipse.che.ide.api.resources.Resource;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftLocalizationConstant;
import org.eclipse.che.ide.ext.openshift.client.oauth.OpenshiftAuthorizationHandler;

/**
 * Action for deletion project from OpenShift
 *
 * @author Alexander Andrienko
 */
@Singleton
public class DeleteProjectAction extends AbstractPerspectiveAction {

  private final AppContext appContext;
  private final DeleteProjectPresenter deleteProjectPresenter;
  private final OpenshiftAuthorizationHandler authorizationHandler;

  @Inject
  public DeleteProjectAction(
      OpenshiftLocalizationConstant locale,
      AppContext appContext,
      DeleteProjectPresenter deleteProjectPresenter,
      OpenshiftAuthorizationHandler authorizationHandler) {
    super(
        Collections.singletonList(PROJECT_PERSPECTIVE_ID),
        locale.deleteProjectAction(),
        locale.deleteProjectActionDescription(),
        null,
        null);

    this.appContext = appContext;
    this.deleteProjectPresenter = deleteProjectPresenter;
    this.authorizationHandler = authorizationHandler;
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
              currentProject != null
                  && currentProject.getMixins().contains(OPENSHIFT_PROJECT_TYPE_ID)
                  && authorizationHandler.isLoggedIn());
    } else {
      event.getPresentation().setEnabledAndVisible(false);
    }
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    this.deleteProjectPresenter.show();
  }
}
