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
package org.eclipse.che.ide.ext.openshift.shared.dto;

import org.eclipse.che.dto.shared.DTO;

@DTO
public interface Probe {
  TCPSocketAction getTcpSocket();

  void setTcpSocket(TCPSocketAction tcpSocket);

  Probe withTcpSocket(TCPSocketAction tcpSocket);

  Integer getTimeoutSeconds();

  void setTimeoutSeconds(Integer timeoutSeconds);

  Probe withTimeoutSeconds(Integer timeoutSeconds);

  Integer getInitialDelaySeconds();

  void setInitialDelaySeconds(Integer initialDelaySeconds);

  Probe withInitialDelaySeconds(Integer initialDelaySeconds);

  ExecAction getExec();

  void setExec(ExecAction exec);

  Probe withExec(ExecAction exec);

  HTTPGetAction getHttpGet();

  void setHttpGet(HTTPGetAction httpGet);

  Probe withHttpGet(HTTPGetAction httpGet);
}
