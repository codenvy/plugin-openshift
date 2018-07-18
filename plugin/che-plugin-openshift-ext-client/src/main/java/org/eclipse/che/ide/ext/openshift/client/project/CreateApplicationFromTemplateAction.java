/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.ide.ext.openshift.client.project;

import static org.eclipse.che.ide.workspace.perspectives.project.ProjectPerspective.PROJECT_PERSPECTIVE_ID;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
import javax.validation.constraints.NotNull;
import org.eclipse.che.ide.api.action.AbstractPerspectiveAction;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftResources;
import org.eclipse.che.ide.ext.openshift.client.oauth.OpenshiftAuthorizationHandler;
import org.eclipse.che.ide.ext.openshift.client.project.wizard.CreateProjectPresenter;

/**
 * Action to handle create new application request.
 *
 * @author Vlad Zhukovskiy
 */
@Singleton
public class CreateApplicationFromTemplateAction extends AbstractPerspectiveAction {
  private final CreateProjectPresenter wizard;
  private final OpenshiftAuthorizationHandler authorizationHandler;

  @Inject
  public CreateApplicationFromTemplateAction(
      CreateProjectPresenter wizard,
      OpenshiftAuthorizationHandler authorizationHandler,
      OpenshiftResources resources) {
    super(
        Collections.singletonList(PROJECT_PERSPECTIVE_ID),
        "Create Application From Template",
        null,
        null,
        resources.createFromTemplate());
    this.wizard = wizard;
    this.authorizationHandler = authorizationHandler;
  }

  /** {@inheritDoc} */
  @Override
  public void updateInPerspective(@NotNull ActionEvent event) {
    event.getPresentation().setEnabled(authorizationHandler.isLoggedIn());
  }

  /** {@inheritDoc} */
  @Override
  public void actionPerformed(ActionEvent e) {
    wizard.createWizardAndShow();
  }
}
