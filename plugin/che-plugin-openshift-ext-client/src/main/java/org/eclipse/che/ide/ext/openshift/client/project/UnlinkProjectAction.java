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

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.ide.api.action.AbstractPerspectiveAction;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.project.MutableProjectConfig;
import org.eclipse.che.ide.api.resources.Project;
import org.eclipse.che.ide.api.resources.Resource;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftLocalizationConstant;

import javax.validation.constraints.NotNull;
import java.util.Collections;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.eclipse.che.ide.api.notification.StatusNotification.DisplayMode.EMERGE_MODE;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.FAIL;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.SUCCESS;
import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_APPLICATION_VARIABLE_NAME;
import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_NAMESPACE_VARIABLE_NAME;
import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_PROJECT_TYPE_ID;
import static org.eclipse.che.ide.workspace.perspectives.project.ProjectPerspective.PROJECT_PERSPECTIVE_ID;

/**
 * Action for unlink Che project from OpenShift.
 *
 * @author Sergii Leschenko
 * @author Vlad Zhukovskyi
 */
@Singleton
public class UnlinkProjectAction extends AbstractPerspectiveAction {

    private final AppContext           appContext;
    private final NotificationManager  notificationManager;
    private final OpenshiftLocalizationConstant locale;

    @Inject
    public UnlinkProjectAction(AppContext appContext,
                               NotificationManager notificationManager,
                               OpenshiftLocalizationConstant locale) {
        super(Collections.singletonList(PROJECT_PERSPECTIVE_ID), locale.unlinkProjectActionTitle(), null, null, null);
        this.appContext = appContext;
        this.notificationManager = notificationManager;
        this.locale = locale;
    }

    @Override
    public void updateInPerspective(@NotNull ActionEvent event) {
        event.getPresentation().setVisible(true);

        final Resource resource = appContext.getResource();

        if (resource != null) {
            final Optional<Project> project = resource.getRelatedProject();
            event.getPresentation().setEnabled(project.isPresent() && project.get().getMixins().contains(OPENSHIFT_PROJECT_TYPE_ID));
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final Resource resource = appContext.getResource();
        checkNotNull(resource);

        final Optional<Project> projectOptional = resource.getRelatedProject();
        checkState(projectOptional.isPresent());

        final Project project = projectOptional.get();

        checkNotNull(project);
        checkState(project.getMixins().contains(OPENSHIFT_PROJECT_TYPE_ID));

        MutableProjectConfig updateConfig = new MutableProjectConfig(project);
        updateConfig.getAttributes().remove(OPENSHIFT_APPLICATION_VARIABLE_NAME);
        updateConfig.getAttributes().remove(OPENSHIFT_NAMESPACE_VARIABLE_NAME);
        updateConfig.getMixins().remove(OPENSHIFT_PROJECT_TYPE_ID);

        project.update()
               .withBody(updateConfig)
               .send()
               .then(new Operation<Project>() {
                   @Override
                   public void apply(Project project) throws OperationException {
                       notificationManager.notify(locale.unlinkProjectSuccessful(project.getName()), SUCCESS, EMERGE_MODE);
                   }
               })
               .catchError(new Operation<PromiseError>() {
                   @Override
                   public void apply(PromiseError error) throws OperationException {
                       notificationManager.notify(locale.unlinkProjectFailed() + " " + error.getMessage(), FAIL, EMERGE_MODE);
                   }
               });
    }
}
