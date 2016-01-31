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
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftLocalizationConstant;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftServiceClient;
import org.eclipse.che.ide.ext.openshift.client.ValidateAuthenticationPresenter;
import org.eclipse.che.ide.ext.openshift.client.oauth.OpenshiftAuthenticator;
import org.eclipse.che.ide.ext.openshift.client.oauth.OpenshiftAuthorizationHandler;
import org.eclipse.che.ide.ext.openshift.shared.dto.BuildConfig;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.ui.dialogs.ConfirmCallback;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import org.eclipse.che.ide.ui.loaders.requestLoader.IdeLoader;

import java.util.List;

import static org.eclipse.che.api.promises.client.callback.PromiseHelper.newCallback;
import static org.eclipse.che.api.promises.client.callback.PromiseHelper.newPromise;
import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_APPLICATION_VARIABLE_NAME;
import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_NAMESPACE_VARIABLE_NAME;
import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_PROJECT_TYPE_ID;

/**
 * @author Alexander Andrienko
 */
@Singleton
public class DeleteProjectPresenter extends ValidateAuthenticationPresenter {

    private final AppContext                    appContext;
    private final DialogFactory                 dialogFactory;
    private final OpenshiftLocalizationConstant locale;
    private final OpenshiftServiceClient        service;
    private final NotificationManager           notificationManager;
    private final ProjectServiceClient          projectService;
    private final DtoUnmarshallerFactory        dtoUnmarshaller;
    private final IdeLoader                     loader;

    @Inject
    protected DeleteProjectPresenter(OpenshiftAuthenticator openshiftAuthenticator,
                                     OpenshiftAuthorizationHandler openshiftAuthorizationHandler,
                                     AppContext appContext,
                                     DialogFactory dialogFactory,
                                     OpenshiftLocalizationConstant locale,
                                     OpenshiftServiceClient service,
                                     ProjectServiceClient projectService,
                                     NotificationManager notificationManager,
                                     DtoUnmarshallerFactory dtoUnmarshaller,
                                     IdeLoader loader) {
        super(openshiftAuthenticator, openshiftAuthorizationHandler, locale, notificationManager);

        this.appContext = appContext;
        this.dialogFactory = dialogFactory;
        this.locale = locale;
        this.service = service;
        this.notificationManager = notificationManager;
        this.projectService = projectService;
        this.dtoUnmarshaller = dtoUnmarshaller;
        this.loader = loader;
    }

    @Override
    protected void onSuccessAuthentication() {
        ProjectConfigDto projectConfig = appContext.getCurrentProject().getRootProject();
        final String namespace = getAttributeValue(projectConfig, OPENSHIFT_NAMESPACE_VARIABLE_NAME);

        if (!Strings.isNullOrEmpty(namespace)) {
            loader.show(locale.retrievingProjectsData());
            Promise<List<BuildConfig>> buildConfigs = service.getBuildConfigs(namespace);
            buildConfigs.then(showConfirmDialog(projectConfig, namespace))
                        .catchError(new Operation<PromiseError>() {
                            @Override
                            public void apply(PromiseError arg) throws OperationException {
                                loader.hide();
                            }
                        })
                        .catchError(handleError(namespace));
        } else {
            notificationManager.showError(locale.projectIsNotLinkedToOpenShiftError(projectConfig.getName()));
        }
    }

    private String getAttributeValue(ProjectConfigDto projectConfig, String value) {
        List<String> attributes = projectConfig.getAttributes().get(value);
        if (attributes == null || attributes.isEmpty()) {
            return null;
        }
        return projectConfig.getAttributes().get(value).get(0);
    }

    private Operation<List<BuildConfig>> showConfirmDialog(final ProjectConfigDto projectConfig, final String nameSpace) {
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
                dialogFactory.createConfirmDialog(locale.deleteProjectDialogTitle(),
                                                  dialogLabel,
                                                  new ConfirmCallback() {
                                                      @Override
                                                      public void accepted() {
                                                          service.deleteProject(nameSpace).then(new Operation<Void>() {
                                                              @Override
                                                              public void apply(Void arg) throws OperationException {
                                                                  notificationManager.showInfo(locale.deleteProjectSuccess(nameSpace));
                                                                  removeOpenshiftMixin(projectConfig, nameSpace);
                                                              }
                                                          }).catchError(new Operation<PromiseError>() {
                                                              @Override
                                                              public void apply(PromiseError arg) throws OperationException {
                                                                  handleError(nameSpace);
                                                              }
                                                          });
                                                      }
                                                  },
                                                  null).show();
            }
        };
    }

    private Operation<PromiseError> handleError(final String nameSpace) {
        return new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError promiseError) throws OperationException {
                notificationManager.showError(locale.deleteProjectFailed(nameSpace) + " " + promiseError.getMessage());
            }
        };
    }

    private void removeOpenshiftMixin(final ProjectConfigDto projectConfig, final String nameSpace) {
        projectConfig.getMixins().remove(OPENSHIFT_PROJECT_TYPE_ID);
        projectConfig.getAttributes().remove(OPENSHIFT_NAMESPACE_VARIABLE_NAME);
        projectConfig.getAttributes().remove(OPENSHIFT_APPLICATION_VARIABLE_NAME);

        newPromise(new AsyncPromiseHelper.RequestCall<ProjectConfigDto>() {
            @Override
            public void makeCall(AsyncCallback<ProjectConfigDto> callback) {
                projectService.updateProject(projectConfig.getPath(),
                                             projectConfig,
                                             newCallback(callback, dtoUnmarshaller.newUnmarshaller(ProjectConfigDto.class)));
            }
        }).then(new Operation<ProjectConfigDto>() {
            @Override
            public void apply(ProjectConfigDto configDto) throws OperationException {
                appContext.getCurrentProject().setRootProject(configDto);
                notificationManager.showInfo(locale.projectSuccessfullyReset(configDto.getName()));
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
