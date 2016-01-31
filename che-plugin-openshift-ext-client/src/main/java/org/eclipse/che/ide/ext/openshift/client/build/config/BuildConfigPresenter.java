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
package org.eclipse.che.ide.ext.openshift.client.build.config;

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
import org.eclipse.che.ide.ext.openshift.shared.dto.BuildConfig;
import org.eclipse.che.ide.ext.openshift.shared.dto.BuildSource;
import org.eclipse.che.ide.ext.openshift.shared.dto.WebHook;

import java.util.List;

import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_APPLICATION_VARIABLE_NAME;
import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_NAMESPACE_VARIABLE_NAME;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.FAIL;

/**
 * Presenter for viewing and editing build config (webhooks, source).
 *
 * @author Anna Shumilova
 */
@Singleton
public class BuildConfigPresenter implements ConfigPresenter, BuildConfigView.ActionDelegate {

    private final BuildConfigView               view;
    private final OpenshiftServiceClient        service;
    private final AppContext                    appContext;
    private final NotificationManager           notificationManager;
    private final OpenshiftLocalizationConstant locale;
    private final DtoFactory                    dtoFactory;
    private       BuildConfig                   buildConfig;

    @Inject
    public BuildConfigPresenter(BuildConfigView view,
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
        this.buildConfig = null;
        view.setNoBuildConfigs(true);
        view.setSourceUrl(null);
        view.setSourceReference(null);
        view.setSourceContextDir(null);
        view.enableSaveButton(false);
        view.enableRestoreButton(false);
    }

    private void loadBuildConfig() {
        final CurrentProject currentProject = appContext.getCurrentProject();
        if (currentProject == null) {
            return;
        }
        final ProjectConfig projectDescription = currentProject.getRootProject();

        String namespace = getAttributeValue(projectDescription, OPENSHIFT_NAMESPACE_VARIABLE_NAME);
        String application = getAttributeValue(projectDescription, OPENSHIFT_APPLICATION_VARIABLE_NAME);

        service.getBuildConfigs(namespace, application)
               .then(showBuildConfigs())
               .catchError(onFail());
    }

    private Operation<List<BuildConfig>> showBuildConfigs() {
        return new Operation<List<BuildConfig>>() {
            @Override
            public void apply(List<BuildConfig> result) throws OperationException {
                boolean noBuildConfigs = result == null || result.isEmpty();
                view.setNoBuildConfigs(noBuildConfigs);

                if (noBuildConfigs) {
                    return;
                }

                buildConfig = result.get(0);
                setBuildSource(buildConfig.getSpec().getSource());
                getWebHooks(buildConfig);
            }
        };
    }

    private void getWebHooks(BuildConfig buildConfig) {
        service.getWebhooks(buildConfig.getMetadata().getNamespace(), buildConfig.getMetadata().getName())
               .then(showWebhooks())
               .catchError(onFail());
    }

    private Operation<List<WebHook>> showWebhooks() {
        return new Operation<List<WebHook>>() {
            @Override
            public void apply(List<WebHook> webHooks) throws OperationException {
                view.setWebhooks(webHooks);
            }
        };
    }

    /**
     * Display build source info (url, reference and context dir).
     *
     * @param buildSource
     */
    private void setBuildSource(BuildSource buildSource) {
        view.setSourceUrl(buildSource.getGit().getUri());
        view.setSourceReference(buildSource.getGit().getRef());
        view.setSourceContextDir(buildSource.getContextDir());

        view.enableSaveButton(false);
        view.enableRestoreButton(false);
    }

    /**
     * Process request fail and display message.
     */
    private Operation<PromiseError> onFail() {
        return new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                final ServiceError serviceError = dtoFactory.createDtoFromJson(arg.getMessage(), ServiceError.class);
                notificationManager.notify(serviceError.getMessage(), FAIL, true);
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
        return locale.applicationConfigsBuildConfigTitle();
    }

    @Override
    public void go(AcceptsOneWidget container) {
        resetView();
        container.setWidget(view);
        loadBuildConfig();
    }


    @Override
    public void onSaveClicked() {
        buildConfig.getSpec().getSource().setContextDir(view.getSourceContextDir());
        buildConfig.getSpec().getSource().getGit().setRef(view.getSourceReference());
        buildConfig.getSpec().getSource().getGit().setUri(view.getSourceUrl());

        service.updateBuildConfig(buildConfig).then(new Operation<BuildConfig>() {
            @Override
            public void apply(BuildConfig arg) throws OperationException {
                buildConfig = arg;
                setBuildSource(buildConfig.getSpec().getSource());
            }
        }).catchError(onFail());
    }

    @Override
    public void onRestoreClicked() {
        setBuildSource(buildConfig.getSpec().getSource());
        view.enableRestoreButton(false);
        view.enableSaveButton(false);
    }

    @Override
    public void onSourceDataChanged() {
        BuildSource buildSource = buildConfig.getSpec().getSource();
        boolean changed = !(buildSource.getContextDir().equals(view.getSourceContextDir()) &&
                            buildSource.getGit().getRef().equals(view.getSourceReference()) &&
                            buildSource.getGit().getUri().equals(view.getSourceUrl()));
        view.enableSaveButton(changed);
        view.enableRestoreButton(changed);
    }
}
