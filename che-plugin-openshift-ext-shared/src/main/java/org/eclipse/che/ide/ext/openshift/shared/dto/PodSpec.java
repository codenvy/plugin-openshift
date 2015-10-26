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
package org.eclipse.che.ide.ext.openshift.shared.dto;

import org.eclipse.che.dto.shared.DTO;

import java.util.List;
import java.util.Map;

@DTO
public interface PodSpec {
    String getDnsPolicy();

    void setDnsPolicy(String dnsPolicy);

    PodSpec withDnsPolicy(String dnsPolicy);

    Integer getTerminationGracePeriodSeconds();

    void setTerminationGracePeriodSeconds(Integer terminationGracePeriodSeconds);

    PodSpec withTerminationGracePeriodSeconds(Integer terminationGracePeriodSeconds);

    boolean getHostNetwork();

    void setHostNetwork(boolean hostNetwork);

    PodSpec withHostNetwork(boolean hostNetwork);

    List<LocalObjectReference> getImagePullSecrets();

    void setImagePullSecrets(List<LocalObjectReference> imagePullSecrets);

    PodSpec withImagePullSecrets(List<LocalObjectReference> imagePullSecrets);

    List<Volume> getVolumes();

    void setVolumes(List<Volume> volumes);

    PodSpec withVolumes(List<Volume> volumes);

    String getHost();

    void setHost(String host);

    PodSpec withHost(String host);

    List<Container> getContainers();

    void setContainers(List<Container> containers);

    PodSpec withContainers(List<Container> containers);

    String getServiceAccount();

    void setServiceAccount(String serviceAccount);

    PodSpec withServiceAccount(String serviceAccount);

    String getRestartPolicy();

    void setRestartPolicy(String restartPolicy);

    PodSpec withRestartPolicy(String restartPolicy);

    Integer getActiveDeadlineSeconds();

    void setActiveDeadlineSeconds(Integer activeDeadlineSeconds);

    PodSpec withActiveDeadlineSeconds(Integer activeDeadlineSeconds);

    Map<String, String> getNodeSelector();

    void setNodeSelector(Map<String, String> nodeSelector);

    PodSpec withNodeSelector(Map<String, String> nodeSelector);

}
