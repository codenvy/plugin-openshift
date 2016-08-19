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
package org.eclipse.che.ide.ext.openshift.client.delete;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.dialogs.ConfirmCallback;
import org.eclipse.che.ide.api.dialogs.DialogFactory;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.project.MutableProjectConfig;
import org.eclipse.che.ide.api.resources.Project;
import org.eclipse.che.ide.api.resources.Resource;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftLocalizationConstant;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftServiceClient;
import org.eclipse.che.ide.ext.openshift.client.ValidateAuthenticationPresenter;
import org.eclipse.che.ide.ext.openshift.client.oauth.OpenshiftAuthenticator;
import org.eclipse.che.ide.ext.openshift.client.oauth.OpenshiftAuthorizationHandler;
import org.eclipse.che.ide.ext.openshift.shared.dto.BuildConfig;
import org.eclipse.che.ide.ui.loaders.request.LoaderFactory;
import org.eclipse.che.ide.ui.loaders.request.MessageLoader;

import java.util.List;

import static org.eclipse.che.ide.api.notification.StatusNotification.DisplayMode.EMERGE_MODE;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.FAIL;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.SUCCESS;
import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_APPLICATION_VARIABLE_NAME;
import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_NAMESPACE_VARIABLE_NAME;
import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_PROJECT_TYPE_ID;

/**
 * @author Alexander Andrienko
 */
@Singleton
public class DeleteProjectPresenter extends ValidateAuthenticationPresenter {

    private final AppContext    appContext;
    private final DialogFactory dialogFactory;
    private final OpenshiftLocalizationConstant locale;
    private final OpenshiftServiceClient service;
    private final NotificationManager    notificationManager;
    private final MessageLoader          loader;

    @Inject
    protected DeleteProjectPresenter(OpenshiftAuthenticator openshiftAuthenticator,
                                     OpenshiftAuthorizationHandler openshiftAuthorizationHandler,
                                     AppContext appContext,
                                     DialogFactory dialogFactory,
                                     OpenshiftLocalizationConstant locale,
                                     OpenshiftServiceClient service,
                                     NotificationManager notificationManager,
                                     LoaderFactory loaderFactory) {
        super(openshiftAuthenticator, openshiftAuthorizationHandler, locale, notificationManager);
        this.appContext = appContext;
        this.dialogFactory = dialogFactory;
        this.locale = locale;
        this.service = service;
        this.notificationManager = notificationManager;
        this.loader = loaderFactory.newLoader();
    }

    @Override
    protected void onSuccessAuthentication() {
        final Resource resource = appContext.getResource();
        if (resource != null && resource.getRelatedProject().isPresent()) {
            Project project = resource.getRelatedProject().get();
            final String namespace = project.getAttribute(OPENSHIFT_NAMESPACE_VARIABLE_NAME);
            if (!Strings.isNullOrEmpty(namespace)) {
                loader.show(locale.retrievingProjectsData());
                Promise<List<BuildConfig>> buildConfigs = service.getBuildConfigs(namespace);
                buildConfigs.then(showConfirmDialog(project, namespace))
                            .catchError(new Operation<PromiseError>() {
                                @Override
                                public void apply(PromiseError arg) throws OperationException {
                                    loader.hide();
                                }
                            })
                            .catchError(handleError(namespace));
            } else {
                notificationManager.notify(locale.projectIsNotLinkedToOpenShiftError(project.getName()), FAIL, EMERGE_MODE);
            }
        }
    }

    private Operation<List<BuildConfig>> showConfirmDialog(final Project project, final String nameSpace) {
        return new Operation<List<BuildConfig>>() {
            @Override
            public void apply(List<BuildConfig> configs) throws OperationException {
                String dialogLabel;
                if (configs.isEmpty()) {
                    dialogLabel = locale.deleteProjectWithoutAppLabel(nameSpace);
                } else if (configs.size() == 1) {
                    dialogLabel = locale.deleteSingleAppProjectLabel(nameSpace);
                } else {
                    String applications = getBuildConfigNames(configs);
                    dialogLabel = locale.deleteMultipleAppProjectLabel(nameSpace, applications);
                }
                loader.hide();
                ConfirmCallback confirmCallback = new ConfirmCallback() {
                    @Override
                    public void accepted() {
                        service.deleteProject(nameSpace).then(new Operation<Void>() {
                            @Override
                            public void apply(Void arg) throws OperationException {
                                notificationManager.notify(locale.deleteProjectSuccess(nameSpace), SUCCESS, EMERGE_MODE);
                                removeOpenshiftMixin(project, nameSpace);
                            }
                        })
                        .catchError(new Operation<PromiseError>() {
                            @Override
                            public void apply(PromiseError arg) throws OperationException {
                                handleError(nameSpace);
                            }
                        });
                    }
                };
                dialogFactory.createConfirmDialog(locale.deleteProjectDialogTitle(), dialogLabel, confirmCallback, null).show();
            }
        };
    }

    private Operation<PromiseError> handleError(final String nameSpace) {
        return new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError promiseError) throws OperationException {
                notificationManager.notify(locale.deleteProjectFailed(nameSpace) + " " + promiseError.getMessage(), FAIL, EMERGE_MODE);
            }
        };
    }

    private void removeOpenshiftMixin(final Project project, final String nameSpace) {
        project.getMixins().remove(OPENSHIFT_PROJECT_TYPE_ID);
        project.getAttributes().remove(OPENSHIFT_NAMESPACE_VARIABLE_NAME);
        project.getAttributes().remove(OPENSHIFT_APPLICATION_VARIABLE_NAME);

        MutableProjectConfig config = new MutableProjectConfig(project);
        config.getMixins().remove(OPENSHIFT_PROJECT_TYPE_ID);
        config.getAttributes().remove(OPENSHIFT_NAMESPACE_VARIABLE_NAME);
        config.getAttributes().remove(OPENSHIFT_APPLICATION_VARIABLE_NAME);

        project.update().withBody(config).send().then(new Operation<Project>() {
            @Override
            public void apply(Project project) throws OperationException {
                notificationManager.notify(locale.projectSuccessfullyReset(project.getName()), SUCCESS, EMERGE_MODE);
            }
        }).catchError(handleError(nameSpace));
    }

    private String getBuildConfigNames(List<BuildConfig> buildConfigs) {
        String result = "";
        for (BuildConfig buildConfig : buildConfigs) {
            result += buildConfig.getMetadata().getName() + ", ";
        }
        result = result.substring(0, result.length() - 3);//cut last ", "
        return result;
    }
}
