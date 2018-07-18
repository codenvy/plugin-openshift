/**
 * ***************************************************************************** Copyright (c)
 * 2012-2017 Codenvy, S.A. All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: Codenvy, S.A. - initial API and implementation
 * *****************************************************************************
 */
package org.eclipse.che.ide.ext.openshift.client.oauth;

import com.google.gwt.event.shared.GwtEvent;

/** @author Sergii Leschenko */
public class OAuthTokenChangedEvent extends GwtEvent<OAuthTokenChangedHandler> {

  public static Type<OAuthTokenChangedHandler> TYPE = new Type<>();

  /** Oauth token */
  private String token;

  public OAuthTokenChangedEvent(String token) {
    this.token = token;
  }

  @Override
  public Type<OAuthTokenChangedHandler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(OAuthTokenChangedHandler handler) {
    handler.onOAuthTokenChanged(this);
  }

  /**
   * Returns oauth token
   *
   * @return token
   */
  public String getToken() {
    return token;
  }
}
