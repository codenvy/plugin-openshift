/*******************************************************************************
 * Copyright (c) 2012-2017 Codenvy, S.A.
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

@DTO
public interface AWSElasticBlockStoreVolumeSource {
    Integer getPartition();

    void setPartition(Integer partition);

    AWSElasticBlockStoreVolumeSource withPartition(Integer partition);

    String getVolumeID();

    void setVolumeID(String volumeID);

    AWSElasticBlockStoreVolumeSource withVolumeID(String volumeID);

    boolean getReadOnly();

    void setReadOnly(boolean readOnly);

    AWSElasticBlockStoreVolumeSource withReadOnly(boolean readOnly);

    String getFsType();

    void setFsType(String fsType);

    AWSElasticBlockStoreVolumeSource withFsType(String fsType);

}
