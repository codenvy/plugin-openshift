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

import java.util.Map;
import org.eclipse.che.dto.shared.DTO;

@DTO
public interface NamedTagReference {
  boolean getReference();

  void setReference(boolean reference);

  NamedTagReference withReference(boolean reference);

  String getName();

  void setName(String name);

  NamedTagReference withName(String name);

  Map<String, String> getAnnotations();

  void setAnnotations(Map<String, String> annotations);

  NamedTagReference withAnnotations(Map<String, String> annotations);

  ObjectReference getFrom();

  void setFrom(ObjectReference from);

  NamedTagReference withFrom(ObjectReference from);
}
