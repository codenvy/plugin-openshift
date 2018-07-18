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
package org.eclipse.che.ide.ext.openshift.client.config;

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.ImplementedBy;
import java.util.List;
import org.eclipse.che.ide.api.mvp.View;

/**
 * The view of {@link ConfigureApplicationPresenter} for managing OpenShift application configs.
 *
 * @author Anna Shumilova
 */
@ImplementedBy(ConfigureConfigureApplicationViewImpl.class)
public interface ConfigureApplicationView extends View<ConfigureApplicationView.ActionDelegate> {
  /** Needs for delegate some function into application config view. */
  interface ActionDelegate {
    /** Performs any actions appropriate in response to the user having pressed the Close button. */
    void onCloseClicked();

    void onConfigSelected(ConfigPresenter view);
  }

  AcceptsOneWidget getContentPanel();

  void setConfigs(List<ConfigPresenter> configs);

  void selectConfig(ConfigPresenter config);

  void setTitle(String title);

  /** Close dialog. */
  void close();

  /** Show dialog. */
  void showDialog();
}
