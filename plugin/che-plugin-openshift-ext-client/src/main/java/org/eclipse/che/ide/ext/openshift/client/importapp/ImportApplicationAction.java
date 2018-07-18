/**
 * ***************************************************************************** Copyright (c)
 * 2012-2017 Codenvy, S.A. All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: Codenvy, S.A. - initial API and implementation
 * *****************************************************************************
 */
package org.eclipse.che.ide.ext.openshift.client.importapp;

import static org.eclipse.che.ide.workspace.perspectives.project.ProjectPerspective.PROJECT_PERSPECTIVE_ID;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
import javax.validation.constraints.NotNull;
import org.eclipse.che.ide.api.action.AbstractPerspectiveAction;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftLocalizationConstant;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftResources;
import org.eclipse.che.ide.ext.openshift.client.oauth.OpenshiftAuthorizationHandler;

/**
 * Action for importing existing OpenShift application to Che.
 *
 * @author Anna Shumilova
 */
@Singleton
public class ImportApplicationAction extends AbstractPerspectiveAction {

  private final ImportApplicationPresenter presenter;
  private final OpenshiftAuthorizationHandler authorizationHandler;

  @Inject
  public ImportApplicationAction(
      OpenshiftLocalizationConstant locale,
      OpenshiftResources resources,
      ImportApplicationPresenter presenter,
      OpenshiftAuthorizationHandler authorizationHandler) {
    super(
        Collections.singletonList(PROJECT_PERSPECTIVE_ID),
        locale.importApplicationAction(),
        locale.linkProjectWithExistingApplicationAction(),
        null,
        resources.importApplication());
    this.presenter = presenter;
    this.authorizationHandler = authorizationHandler;
  }

  @Override
  public void updateInPerspective(@NotNull ActionEvent event) {
    event.getPresentation().setEnabled(authorizationHandler.isLoggedIn());
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    presenter.show();
  }
}
