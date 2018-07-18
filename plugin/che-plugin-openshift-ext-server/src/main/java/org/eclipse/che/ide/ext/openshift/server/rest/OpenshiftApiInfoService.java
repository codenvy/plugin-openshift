/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.ide.ext.openshift.server.rest;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.eclipse.che.ide.ext.openshift.server.ClientFactory;
import org.eclipse.che.ide.ext.openshift.shared.dto.OpenshiftServerInfo;

/** @author Sergii Leschenko */
@Path("/openshift")
public class OpenshiftApiInfoService {
  private final ClientFactory clientFactory;

  @Inject
  public OpenshiftApiInfoService(ClientFactory clientFactory) {
    this.clientFactory = clientFactory;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public OpenshiftServerInfo getServerInfo() {
    return clientFactory.getClientInfo();
  }
}
