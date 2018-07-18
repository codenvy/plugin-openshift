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
package org.eclipse.che.ide.ext.openshift.client;

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.inject.client.assistedinject.GinFactoryModuleBuilder;
import javax.inject.Singleton;
import org.eclipse.che.ide.api.auth.OAuthServiceClient;
import org.eclipse.che.ide.api.auth.OAuthServiceClientImpl;
import org.eclipse.che.ide.api.extension.ExtensionGinModule;

/** @author Sergii Leschenko */
@ExtensionGinModule
public class OpenshiftGinModule extends AbstractGinModule {
  @Override
  protected void configure() {
    bind(OAuthServiceClient.class).to(OAuthServiceClientImpl.class).in(Singleton.class);
    bind(OpenshiftServiceClient.class).to(OpenshiftServiceClientImpl.class);
    install(new GinFactoryModuleBuilder().build(WizardFactory.class));
  }
}
