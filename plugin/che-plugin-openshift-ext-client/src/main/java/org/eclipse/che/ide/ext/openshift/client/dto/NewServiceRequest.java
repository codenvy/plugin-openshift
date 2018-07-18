/**
 * ***************************************************************************** Copyright (c)
 * 2012-2017 Codenvy, S.A. All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: Codenvy, S.A. - initial API and implementation
 * *****************************************************************************
 */
package org.eclipse.che.ide.ext.openshift.client.dto;

import org.eclipse.che.dto.shared.DTO;
import org.eclipse.che.ide.ext.openshift.shared.dto.Service;
import org.eclipse.che.ide.ext.openshift.shared.dto.Template;

@DTO
public interface NewServiceRequest {
  Service getService();

  void setService(Service service);

  NewServiceRequest withService(Service service);

  void setTemplate(Template template);

  Template getTemplate();

  NewServiceRequest withTemplate(Template template);
}
