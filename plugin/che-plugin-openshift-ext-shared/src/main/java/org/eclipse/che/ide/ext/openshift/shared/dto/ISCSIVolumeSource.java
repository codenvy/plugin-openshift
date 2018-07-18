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
public interface ISCSIVolumeSource {
  Integer getLun();

  void setLun(Integer lun);

  ISCSIVolumeSource withLun(Integer lun);

  String getIqn();

  void setIqn(String iqn);

  ISCSIVolumeSource withIqn(String iqn);

  boolean getReadOnly();

  void setReadOnly(boolean readOnly);

  ISCSIVolumeSource withReadOnly(boolean readOnly);

  String getFsType();

  void setFsType(String fsType);

  ISCSIVolumeSource withFsType(String fsType);

  String getTargetPortal();

  void setTargetPortal(String targetPortal);

  ISCSIVolumeSource withTargetPortal(String targetPortal);
}
