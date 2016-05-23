/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.ext.openshift.client.service.add.wizard.page.configure;

import com.google.inject.ImplementedBy;

import org.eclipse.che.ide.api.mvp.View;
import org.eclipse.che.ide.ext.openshift.client.deploy._new.KeyValue;
import org.eclipse.che.ide.ext.openshift.shared.dto.Parameter;

import java.util.List;
import java.util.Map;

/**
 * View representation of configurable page for datasource service
 *
 * @author Alexander Andrienko
 */
@ImplementedBy(ConfigureServiceViewImpl.class)
public interface ConfigureServiceView extends View<ConfigureServiceView.ActionDelegate> {

    /**
     * Set environment variables
     * @param parameters list parameters with environment variables
     */
    void setEnvironmentVariables(List<Parameter> parameters);

    /**
     * Get list environment variables
     * @return list environment variables
     */
    List<Parameter> getEnvironmentVariables();

    /**
     * Set environment labels
     * @param labels map with environment labels
     */
    void setEnvironmentLabels(Map<String, String> labels);

    /**
     * Get list environment labels
     * @return
     */
    List<KeyValue> getEnvironmentLabels();

    /**
     * Blocks the window, prevents it closing.
     *
     * @param enabled
     */
    void setEnabled(boolean enabled);

    /**
     * Show error message about wrong label and set tooltip detail error message
     * @param labelMessage label error message
     * @param tooltipMessage tooltip error message
     */
    void showLabelsError(String labelMessage, String tooltipMessage);

    /**
     * Hide error message about wrong label
     */
    void hideLabelsError();

    /**
     * Show error message about invalid environment value in the view
     */
    void showEnvironmentError(String labelMessage);

    /**
     * Hide error message about invalid environment value from the view
     */
    void hideEnvironmentError();

    interface ActionDelegate {
        /**
         * Update bottom panel with buttons Next, Previous, Create
         */
        void updateControls();
    }
}
