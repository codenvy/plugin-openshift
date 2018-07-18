/**
 * ***************************************************************************** Copyright (c)
 * 2012-2017 Codenvy, S.A. All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: Codenvy, S.A. - initial API and implementation
 * *****************************************************************************
 */
package org.eclipse.che.ide.ext.openshift.shared.dto;

import java.util.List;
import java.util.Map;
import org.eclipse.che.dto.shared.DTO;

@DTO
public interface ServiceSpec {
  String getPortalIP();

  void setPortalIP(String portalIP);

  ServiceSpec withPortalIP(String portalIP);

  List<String> getExternalIPs();

  void setExternalIPs(List<String> externalIPs);

  ServiceSpec withExternalIPs(List<String> externalIPs);

  String getLoadBalancerIP();

  void setLoadBalancerIP(String loadBalancerIP);

  ServiceSpec withLoadBalancerIP(String loadBalancerIP);

  List<String> getDeprecatedPublicIPs();

  void setDeprecatedPublicIPs(List<String> deprecatedPublicIPs);

  ServiceSpec withDeprecatedPublicIPs(List<String> deprecatedPublicIPs);

  String getSessionAffinity();

  void setSessionAffinity(String sessionAffinity);

  ServiceSpec withSessionAffinity(String sessionAffinity);

  Map<String, String> getSelector();

  void setSelector(Map<String, String> selector);

  ServiceSpec withSelector(Map<String, String> selector);

  List<ServicePort> getPorts();

  void setPorts(List<ServicePort> ports);

  ServiceSpec withPorts(List<ServicePort> ports);

  String getType();

  void setType(String type);

  ServiceSpec withType(String type);

  String getClusterIP();

  void setClusterIP(String clusterIP);

  ServiceSpec withClusterIP(String clusterIP);
}
