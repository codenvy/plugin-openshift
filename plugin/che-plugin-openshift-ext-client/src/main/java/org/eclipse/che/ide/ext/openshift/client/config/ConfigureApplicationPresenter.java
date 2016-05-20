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
package org.eclipse.che.ide.ext.openshift.client.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftLocalizationConstant;
import org.eclipse.che.ide.ext.openshift.client.ValidateAuthenticationPresenter;
import org.eclipse.che.ide.ext.openshift.client.build.config.BuildConfigPresenter;
import org.eclipse.che.ide.ext.openshift.client.oauth.OpenshiftAuthenticator;
import org.eclipse.che.ide.ext.openshift.client.oauth.OpenshiftAuthorizationHandler;
import org.eclipse.che.ide.ext.openshift.client.replication.ReplicationPresenter;
import org.eclipse.che.ide.ext.openshift.client.url.RouteConfigPresenter;

import java.util.ArrayList;
import java.util.List;

/**
 * Presenter for managing OpenShift application configs.
 *
 * @author Anna Shumilova
 */
@Singleton
public class ConfigureApplicationPresenter extends ValidateAuthenticationPresenter implements ConfigureApplicationView.ActionDelegate {

    private final ConfigureApplicationView view;
    private       List<ConfigPresenter>    configPresenters;

    @Inject
    public ConfigureApplicationPresenter(OpenshiftAuthenticator openshiftAuthenticator,
                                         OpenshiftAuthorizationHandler openshiftAuthorizationHandler,
                                         ConfigureApplicationView view,
                                         NotificationManager notificationManager,
                                         OpenshiftLocalizationConstant locale,
                                         BuildConfigPresenter buildConfigPresenter,
                                         RouteConfigPresenter routeConfigPresenter,
                                         ReplicationPresenter replicationPresenter) {
        super(openshiftAuthenticator, openshiftAuthorizationHandler, locale, notificationManager);
        this.view = view;
        this.view.setDelegate(this);

        configPresenters = new ArrayList<>();
        configPresenters.add(buildConfigPresenter);
        configPresenters.add(routeConfigPresenter);
        configPresenters.add(replicationPresenter);
        view.setConfigs(configPresenters);
    }

    @Override
    public void onCloseClicked() {
        view.close();
    }

    @Override
    public void onConfigSelected(ConfigPresenter configPresenter) {
        configPresenter.go(view.getContentPanel());
    }

    @Override
    protected void onSuccessAuthentication() {
        if (configPresenters.size() > 0) {
            view.selectConfig(configPresenters.get(0));
        }
        view.showDialog();
    }
}
