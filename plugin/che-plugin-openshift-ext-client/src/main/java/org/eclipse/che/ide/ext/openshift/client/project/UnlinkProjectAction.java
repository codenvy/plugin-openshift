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
package org.eclipse.che.ide.ext.openshift.client.project;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.api.project.ProjectServiceClient;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.ide.api.action.AbstractPerspectiveAction;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftLocalizationConstant;
import org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_PROJECT_TYPE_ID;
import static org.eclipse.che.ide.workspace.perspectives.project.ProjectPerspective.PROJECT_PERSPECTIVE_ID;
import static org.eclipse.che.ide.api.notification.StatusNotification.DisplayMode.EMERGE_MODE;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.FAIL;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.SUCCESS;

/**
 * Action for unlink Che project from OpenShift.
 *
 * @author Sergii Leschenko
 */
@Singleton
public class UnlinkProjectAction extends AbstractPerspectiveAction {

    private final AppContext                    appContext;
    private final ProjectServiceClient          projectServiceClient;
    private final NotificationManager           notificationManager;
    private final OpenshiftLocalizationConstant locale;

    @Inject
    public UnlinkProjectAction(AppContext appContext,
                               ProjectServiceClient projectServiceClient,
                               NotificationManager notificationManager,
                               OpenshiftLocalizationConstant locale) {
        super(Collections.singletonList(PROJECT_PERSPECTIVE_ID), locale.unlinkProjectActionTitle(), null, null, null);
        this.appContext = appContext;
        this.projectServiceClient = projectServiceClient;
        this.notificationManager = notificationManager;
        this.locale = locale;
    }

    @Override
    public void updateInPerspective(@NotNull ActionEvent event) {
        final CurrentProject currentProject = appContext.getCurrentProject();
        event.getPresentation().setVisible(currentProject != null);
        event.getPresentation().setEnabled(currentProject != null
                                           && currentProject.getRootProject().getMixins().contains(OPENSHIFT_PROJECT_TYPE_ID));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final ProjectConfigDto projectConfig = appContext.getCurrentProject().getRootProject();
        List<String> mixins = projectConfig.getMixins();
        if (mixins.contains(OpenshiftProjectTypeConstants.OPENSHIFT_PROJECT_TYPE_ID)) {
            mixins.remove(OpenshiftProjectTypeConstants.OPENSHIFT_PROJECT_TYPE_ID);

            Map<String, List<String>> attributes = projectConfig.getAttributes();
            attributes.remove(OpenshiftProjectTypeConstants.OPENSHIFT_APPLICATION_VARIABLE_NAME);
            attributes.remove(OpenshiftProjectTypeConstants.OPENSHIFT_NAMESPACE_VARIABLE_NAME);

            projectServiceClient.updateProject(appContext.getDevMachine(), projectConfig.getPath(), projectConfig)
                                .then(new Operation<ProjectConfigDto>() {
                                    @Override
                                    public void apply(ProjectConfigDto result) throws OperationException {
                                        appContext.getCurrentProject().setRootProject(result);
                                        notificationManager.notify(locale.unlinkProjectSuccessful(result.getName()),
                                                                   SUCCESS,
                                                                   EMERGE_MODE);
                                    }
                                })
                                .catchError(new Operation<PromiseError>() {
                                    @Override
                                    public void apply(PromiseError promiseError) throws OperationException {
                                        notificationManager.notify(locale.unlinkProjectFailed() + " " + promiseError.getMessage(),
                                                                   FAIL,
                                                                   EMERGE_MODE);
                                    }
                                });
        }
    }
}
