/*******************************************************************************
 * Copyright (c) 2012-2017 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.ext.openshift.client.deploy._new;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.api.action.AbstractPerspectiveAction;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.resources.Project;
import org.eclipse.che.ide.api.resources.Resource;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftLocalizationConstant;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftResources;
import org.eclipse.che.ide.ext.openshift.client.oauth.OpenshiftAuthorizationHandler;

import javax.validation.constraints.NotNull;

import java.util.Collections;

import static org.eclipse.che.ide.workspace.perspectives.project.ProjectPerspective.PROJECT_PERSPECTIVE_ID;
import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_PROJECT_TYPE_ID;

/**
 * Action for deploying Che project to OpenShift new application.
 *
 * @author Vlad Zhukovskiy
 */
@Singleton
public class NewApplicationAction extends AbstractPerspectiveAction {

    private final NewApplicationPresenter       presenter;
    private final AppContext                    appContext;
    private final OpenshiftAuthorizationHandler authorizationHandler;

    @Inject
    public NewApplicationAction(final NewApplicationPresenter presenter,
                                final AppContext appContext,
                                OpenshiftLocalizationConstant locale,
                                OpenshiftResources resources,
                                OpenshiftAuthorizationHandler authorizationHandler) {
        super(Collections.singletonList(PROJECT_PERSPECTIVE_ID),
              locale.newApplicationAction(),
              null,
              null,
              resources.deployNewApplication());
        this.presenter = presenter;
        this.appContext = appContext;
        this.authorizationHandler = authorizationHandler;
    }

    @Override
    public void updateInPerspective(@NotNull ActionEvent event) {
        final Resource resource = appContext.getResource();
        if (resource != null) {
            final Optional<Project> relatedProject = resource.getRelatedProject();
            if (relatedProject.isPresent()) {
                event.getPresentation().setVisible(true);
                event.getPresentation().setEnabled(!relatedProject.get().getMixins().contains(OPENSHIFT_PROJECT_TYPE_ID)
                                                   && authorizationHandler.isLoggedIn());
            }
        } else {
            event.getPresentation().setEnabledAndVisible(false);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        presenter.show();
    }
}
