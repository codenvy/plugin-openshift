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
package org.eclipse.che.ide.ext.openshift.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.ext.openshift.client.oauth.OpenshiftAuthenticator;
import org.eclipse.che.ide.ext.openshift.client.oauth.OpenshiftAuthorizationHandler;
import org.eclipse.che.security.oauth.OAuthStatus;

import static org.eclipse.che.ide.api.notification.StatusNotification.DisplayMode.EMERGE_MODE;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.SUCCESS;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.FAIL;

/**
 * @author Ann Shumilova
 */
public abstract class ValidateAuthenticationPresenter {

    private final OpenshiftAuthenticator openshiftAuthenticator;
    private final OpenshiftAuthorizationHandler openshiftAuthorizationHandler;
    private final OpenshiftLocalizationConstant locale;
    private final NotificationManager           notificationManager;

    protected ValidateAuthenticationPresenter(OpenshiftAuthenticator openshiftAuthenticator,
                                              OpenshiftAuthorizationHandler openshiftAuthorizationHandler,
                                              OpenshiftLocalizationConstant locale,
                                              NotificationManager notificationManager) {
        this.openshiftAuthenticator = openshiftAuthenticator;
        this.openshiftAuthorizationHandler = openshiftAuthorizationHandler;
        this.locale = locale;
        this.notificationManager = notificationManager;
    }

    public void show() {
        if (openshiftAuthorizationHandler.isLoggedIn()) {
            onSuccessAuthentication();
        } else {
            openshiftAuthenticator.authorize(new AsyncCallback<OAuthStatus>() {
                @Override
                public void onSuccess(OAuthStatus result) {
                    if (result == OAuthStatus.NOT_PERFORMED) {
                        return;
                    }

                    openshiftAuthorizationHandler.registerLogin();
                    notificationManager.notify(locale.loginSuccessful(), SUCCESS, EMERGE_MODE);
                    onSuccessAuthentication();
                }

                @Override
                public void onFailure(Throwable caught) {
                    notificationManager.notify(locale.loginFailed(), FAIL, EMERGE_MODE);
                }
            });
        }
    }

    protected abstract void onSuccessAuthentication();
}
