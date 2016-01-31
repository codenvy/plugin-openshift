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
package org.eclipse.che.ide.ext.openshift.client.replication;

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.core.model.workspace.ProjectConfig;
import org.eclipse.che.api.core.rest.shared.dto.ServiceError;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftLocalizationConstant;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftServiceClient;
import org.eclipse.che.ide.ext.openshift.client.config.ConfigPresenter;
import org.eclipse.che.ide.ext.openshift.shared.dto.ReplicationController;

import java.util.List;

import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_APPLICATION_VARIABLE_NAME;
import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_NAMESPACE_VARIABLE_NAME;

/**
 * Presenter for scaling application.
 *
 * @author Anna Shumilova
 */
@Singleton
public class ReplicationPresenter implements ConfigPresenter, ReplicationView.ActionDelegate {

    private final ReplicationView               view;
    private final OpenshiftServiceClient        service;
    private final AppContext                    appContext;
    private final NotificationManager           notificationManager;
    private final OpenshiftLocalizationConstant locale;
    private final DtoFactory                    dtoFactory;
    private       int                           replicasNumber;
    private       ReplicationController         replicationController;

    @Inject
    public ReplicationPresenter(ReplicationView view,
                                OpenshiftServiceClient service,
                                AppContext appContext,
                                NotificationManager notificationManager,
                                OpenshiftLocalizationConstant locale,
                                DtoFactory dtoFactory) {
        this.appContext = appContext;
        this.notificationManager = notificationManager;
        this.locale = locale;
        this.service = service;
        this.view = view;
        this.dtoFactory = dtoFactory;
        this.view.setDelegate(this);
    }

    private void resetView() {
        replicasNumber = 0;
        view.setNoReplicaState(true);
        view.setReplicas(replicasNumber);
    }

    private void loadReplicationData() {
        final CurrentProject currentProject = appContext.getCurrentProject();
        if (currentProject == null) {
            return;
        }
        final ProjectConfig projectDescription = currentProject.getRootProject();

        String namespace = getAttributeValue(projectDescription, OPENSHIFT_NAMESPACE_VARIABLE_NAME);
        String application = getAttributeValue(projectDescription, OPENSHIFT_APPLICATION_VARIABLE_NAME);

        service.getReplicationControllers(namespace, application)
               .then(showReplicas())
               .catchError(onFail());
    }

    private Operation<List<ReplicationController>> showReplicas() {
        return new Operation<List<ReplicationController>>() {
            @Override
            public void apply(List<ReplicationController> result) throws OperationException {
                boolean noReplicationCtrl = result == null || result.isEmpty();
                view.setNoReplicaState(noReplicationCtrl);
                if (noReplicationCtrl) {
                    return;
                }
                replicationController = result.get(result.size() - 1);//take the last replication controller
                replicasNumber = replicationController.getSpec().getReplicas();
                view.setReplicas(replicasNumber);
                view.enableAddButton(true);
                view.enableMinusButton(replicasNumber > 1);
            }
        };
    }

    private Operation<PromiseError> onFail() {
        return new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                final ServiceError serviceError = dtoFactory.createDtoFromJson(arg.getMessage(), ServiceError.class);
                notificationManager.showError(serviceError.getMessage());
            }
        };
    }

    /** Returns first value of attribute of null if it is absent in project descriptor */
    private String getAttributeValue(ProjectConfig projectDescriptor, String attibuteValue) {
        final List<String> values = projectDescriptor.getAttributes().get(attibuteValue);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    @Override
    public String getTitle() {
        return locale.applicationConfigsReplicationConfigTitle();
    }

    @Override
    public void go(AcceptsOneWidget container) {
        resetView();
        container.setWidget(view);
        loadReplicationData();
    }

    @Override
    public void onAddClicked() {
        updateReplicas(replicasNumber + 1);
    }

    @Override
    public void onMinusClicked() {
        updateReplicas(replicasNumber - 1);
    }

    private void updateReplicas(final int replicas) {
        replicationController.getSpec().setReplicas(replicas);
        replicationController.getMetadata().setResourceVersion(null);

        service.updateReplicationController(replicationController).then(new Operation<ReplicationController>() {
            @Override
            public void apply(ReplicationController arg) throws OperationException {
                replicationController = arg;
                replicasNumber = arg.getSpec().getReplicas();
                view.setReplicas(replicasNumber);
                view.enableMinusButton(replicasNumber > 1);
                notificationManager.showInfo(locale.applicationConfigsScaledSuccess(replicasNumber));
            }
        }).catchError(onFail());
    }
}
