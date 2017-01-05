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
package org.eclipse.che.ide.ext.openshift.client.config;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.api.action.AbstractPerspectiveAction;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.resources.Project;
import org.eclipse.che.ide.api.resources.Resource;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftLocalizationConstant;
import org.eclipse.che.ide.ext.openshift.client.oauth.OpenshiftAuthorizationHandler;

import javax.validation.constraints.NotNull;
import java.util.Collections;

import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_PROJECT_TYPE_ID;
import static org.eclipse.che.ide.workspace.perspectives.project.ProjectPerspective.PROJECT_PERSPECTIVE_ID;

/**
 * Action for showing OpenShift application configs with ability
 * to edit them.
 *
 * @author Anna Shumilova
 */
@Singleton
public class ConfigureApplicationAction extends AbstractPerspectiveAction {
    private final AppContext                    appContext;
    private final ConfigureApplicationPresenter presenter;
    private final OpenshiftAuthorizationHandler authorizationHandler;

    @Inject
    public ConfigureApplicationAction(AppContext appContext,
                                      ConfigureApplicationPresenter presenter,
                                      OpenshiftLocalizationConstant locale,
                                      OpenshiftAuthorizationHandler authorizationHandler) {
        super(Collections.singletonList(PROJECT_PERSPECTIVE_ID), locale.applicationConfigAction(), null, null, null);
        this.appContext = appContext;
        this.presenter = presenter;
        this.authorizationHandler = authorizationHandler;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        presenter.show();
    }

    @Override
    public void updateInPerspective(@NotNull ActionEvent event) {
        event.getPresentation().setVisible(true);

        final Resource resource = appContext.getResource();
        if (resource != null) {
            final Optional<Project> relatedProject = resource.getRelatedProject();
            if (relatedProject.isPresent()) {
                event.getPresentation().setEnabled(relatedProject.get().getMixins().contains(OPENSHIFT_PROJECT_TYPE_ID)
                                                   && authorizationHandler.isLoggedIn());
            }
        } else {
            event.getPresentation().setEnabledAndVisible(false);
        }
    }
}
