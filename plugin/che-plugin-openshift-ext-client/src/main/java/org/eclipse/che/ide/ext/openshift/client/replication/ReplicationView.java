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
package org.eclipse.che.ide.ext.openshift.client.replication;

import com.google.inject.ImplementedBy;
import org.eclipse.che.ide.api.mvp.View;

/**
 * View for editing replication controller.
 *
 * @author Anna Shumilova
 */
@ImplementedBy(ReplicationViewImpl.class)
public interface ReplicationView extends View<ReplicationView.ActionDelegate> {
  /** Needs for delegate some function into application url view. */
  interface ActionDelegate {
    /** Handle event, when user clicked Add button */
    void onAddClicked();

    /** Handle event, when user clicked Minus button */
    void onMinusClicked();
  }

  /**
   * Set the number of replicas on the view.
   *
   * @param number of replicas
   */
  void setReplicas(int number);

  /**
   * Set the no replicas state.
   *
   * @param visible visible
   */
  void setNoReplicaState(boolean visible);

  /**
   * Set the enabled state of Add button.
   *
   * @param enabled enabled
   */
  void enableAddButton(boolean enabled);

  /**
   * Set the enabled state of Minus button.
   *
   * @param enabled enabled
   */
  void enableMinusButton(boolean enabled);
}
