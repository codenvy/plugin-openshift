/**
 * ***************************************************************************** Copyright (c)
 * 2012-2017 Codenvy, S.A. All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: Codenvy, S.A. - initial API and implementation
 * *****************************************************************************
 */
package org.eclipse.che.ide.ext.openshift.client;

import org.eclipse.che.ide.ext.openshift.client.dto.NewApplicationRequest;
import org.eclipse.che.ide.ext.openshift.client.dto.NewServiceRequest;
import org.eclipse.che.ide.ext.openshift.client.project.wizard.CreateProjectWizard;
import org.eclipse.che.ide.ext.openshift.client.service.add.wizard.CreateServiceWizard;

/**
 * Wizard factory. Creates new wizard based on data object.
 *
 * @author Vlad Zhukovskiy
 * @author Alexander Andrienko
 */
public interface WizardFactory {
  CreateProjectWizard newProjectWizard(NewApplicationRequest newApplicationRequest);

  CreateServiceWizard newServiceWizard(NewServiceRequest newServiceRequest);
}
