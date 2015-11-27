/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.ext.openshift.client.oauth.authenticator;

import com.google.gwt.user.client.rpc.AsyncCallback;

import org.eclipse.che.security.oauth.OAuthStatus;

/**
 * @author Sergii Leschenko
 */
public interface OpenshiftAuthenticator {
    void authorize(AsyncCallback<OAuthStatus> callback);
}
