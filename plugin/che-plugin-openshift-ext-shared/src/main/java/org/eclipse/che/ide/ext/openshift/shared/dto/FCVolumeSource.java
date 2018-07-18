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

import java.util.List;
import org.eclipse.che.dto.shared.DTO;

@DTO
public interface FCVolumeSource {
  List<String> getTargetWWNs();

  void setTargetWWNs(List<String> targetWWNs);

  FCVolumeSource withTargetWWNs(List<String> targetWWNs);

  Integer getLun();

  void setLun(Integer lun);

  FCVolumeSource withLun(Integer lun);

  boolean getReadOnly();

  void setReadOnly(boolean readOnly);

  FCVolumeSource withReadOnly(boolean readOnly);

  String getFsType();

  void setFsType(String fsType);

  FCVolumeSource withFsType(String fsType);
}
