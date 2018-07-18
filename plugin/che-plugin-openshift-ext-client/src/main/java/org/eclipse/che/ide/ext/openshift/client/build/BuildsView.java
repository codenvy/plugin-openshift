/**
 * ***************************************************************************** Copyright (c)
 * 2012-2017 Codenvy, S.A. All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: Codenvy, S.A. - initial API and implementation
 * *****************************************************************************
 */
package org.eclipse.che.ide.ext.openshift.client.build;

import com.google.inject.ImplementedBy;
import org.eclipse.che.ide.api.mvp.View;
import org.eclipse.che.ide.api.parts.base.BaseActionDelegate;
import org.eclipse.che.ide.ext.openshift.shared.dto.Build;

/**
 * The view of {@link BuildsPresenter}. View contains projects and builds represented as tree and
 * logs area.
 *
 * @author Vitaliy Guliy
 */
@ImplementedBy(BuildsViewImpl.class)
public interface BuildsView extends View<BuildsView.ActionDelegate> {
  /** Needs for delegate UI actions to the present. */
  interface ActionDelegate extends BaseActionDelegate {

    /**
     * Called when user selected a build in the tree.
     *
     * @param build selected build
     */
    void buildSelected(Build build);
  }

  /**
   * Displays a project in the project tree.
   *
   * @param namespace project namespace
   */
  void showProject(String namespace);

  /**
   * Updates the connection state of existed project item in the tree.
   *
   * @param namespace project namespace
   * @param connected is connected or not
   */
  void updateProject(String namespace, boolean connected);

  /**
   * Displays a build as child node in the project tree.
   *
   * @param build build to display
   */
  void showBuild(Build build);

  /**
   * Selects a build in the project tree.
   *
   * @param build build to select
   */
  void selectBuild(Build build);

  /**
   * Shows logs area for the build.
   *
   * @param build build
   */
  void showLog(Build build);

  /**
   * Writes a text to build logs area.
   *
   * @param build build
   * @param text log text
   */
  void writeLog(Build build, String text);

  /** Removes all projects from the tree and clears logs area. */
  void clear();
}
