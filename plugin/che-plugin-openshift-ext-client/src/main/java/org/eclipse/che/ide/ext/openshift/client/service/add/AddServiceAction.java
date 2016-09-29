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
package org.eclipse.che.ide.ext.openshift.client.service.add;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.api.action.AbstractPerspectiveAction;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;

import org.eclipse.che.ide.api.resources.Project;
import org.eclipse.che.ide.api.resources.Resource;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftLocalizationConstant;
import org.eclipse.che.ide.ext.openshift.client.oauth.OpenshiftAuthorizationHandler;
import org.eclipse.che.ide.ext.openshift.client.service.add.wizard.CreateServicePresenter;

import javax.validation.constraints.NotNull;
import java.util.Collections;

import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_PROJECT_TYPE_ID;
import static org.eclipse.che.ide.workspace.perspectives.project.ProjectPerspective.PROJECT_PERSPECTIVE_ID;

/**
 * Action for adding datasource service and link this service with che application.
 *
 * @author Alexander Andrienko
 */
@Singleton
public class AddServiceAction extends AbstractPerspectiveAction {

    private final AppContext                    appContext;
    private final CreateServicePresenter        createServiceWizard;
    private final OpenshiftAuthorizationHandler authorizationHandler;

    @Inject
    public AddServiceAction(OpenshiftLocalizationConstant locale,
                            AppContext appContext,
                            CreateServicePresenter createServiceWizard,
                            OpenshiftAuthorizationHandler authorizationHandler) {
        super(Collections.singletonList(PROJECT_PERSPECTIVE_ID),
              locale.addServiceAction(),
              locale.addServiceActionDescription(), null, null);

        this.appContext = appContext;
        this.createServiceWizard = createServiceWizard;
        this.authorizationHandler = authorizationHandler;
    }

    @Override
    public void updateInPerspective(@NotNull ActionEvent event) {
        final Resource resource = appContext.getResource();
        if (resource != null && resource.getRelatedProject().isPresent()) {
            final Project currentProject = resource.getRelatedProject().get();
            event.getPresentation().setVisible(true);
            event.getPresentation().setEnabled(currentProject != null
                                               && currentProject.getMixins().contains(OPENSHIFT_PROJECT_TYPE_ID)
                                               && authorizationHandler.isLoggedIn());
        } else {
            event.getPresentation().setEnabledAndVisible(false);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        this.createServiceWizard.createWizardAndShow();
    }
}
