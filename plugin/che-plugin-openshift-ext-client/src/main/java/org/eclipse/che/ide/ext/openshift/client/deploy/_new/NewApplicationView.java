/*******************************************************************************
 * Copyright (c) 2012-2017 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.ext.openshift.client.deploy._new;

import com.google.inject.ImplementedBy;

import org.eclipse.che.ide.api.mvp.View;
import org.eclipse.che.ide.ext.openshift.shared.dto.Project;

import java.util.List;

/**
 * View for deploying Che project to new OpenShift application.
 *
 * @author Vlad Zhukovskiy
 */
@ImplementedBy(NewApplicationViewImpl.class)
public interface NewApplicationView extends View<NewApplicationView.ActionDelegate> {

    enum Mode {
        CREATE_NEW_PROJECT,
        SELECT_EXISTING_PROJECT;
    }

    /**
     * Show view.
     */
    void show();

    /**
     * Hide view.
     */
    void hide();

    /**
     * Set create new OpenShift project mode or use existing.
     *
     * @param mode
     *         the project's mode
     */
    void setMode(Mode mode);

    /**
     * Get new OpenShift project's name.
     *
     * @return String returns OpenShift project name
     */
    String getOpenShiftProjectName();

    /**
     * Set name for OpenShift new project.
     *
     * @param name
     */
    void setOpenShiftProjectName(String name);

    /**
     * Get new OpenShift project's display name.
     *
     * @return String returns OpenShift project display name
     */
    String getOpenShiftProjectDisplayName();

    /**
     * Set display name for OpenShift new project.
     *
     * @param name
     *         display name
     */
    void setOpenShiftProjectDisplayName(String name);

    /**
     * Get new OpenShift project's description.
     *
     * @return String returns OpenShift project description
     */
    String getOpenShiftProjectDescription();

    /**
     * Set description for new OpenShift project.
     *
     * @param description
     */
    void setOpenShiftProjectDescription(String description);

    /**
     * Get selected existing OpenShift project.
     *
     * @return OpenShift selected project
     */
    Project getOpenShiftSelectedProject();

    /**
     * Set name for new OpenShift application.
     *
     * @param name
     */
    void setApplicationName(String name);

    /**
     * Set the list of OpenShift projects to display.
     *
     * @param projects
     */
    void setProjects(List<Project> projects);

    /**
     * Set the list of OpenShift images to display.
     *
     * @param images
     */
    void setImages(List<String> images);

    /**
     * Get selected OpenShift image.
     *
     * @return selected image
     */
    String getActiveImage();

    /**
     * Get selected OpenShift mode: new or existing.
     *
     * @return mode
     */
    Mode getMode();

    /**
     * Set the list of OpenShift environment variables to display.
     *
     * @param variables
     *         environment variables
     */
    void setEnvironmentVariables(List<KeyValue> variables);

    /**
     * Get the list of OpenShift environment variables.
     *
     * @return environment variables
     */
    List<KeyValue> getEnvironmentVariables();

    /**
     * Set the list of OpenShift labels to display.
     *
     * @param labels
     */
    void setLabels(List<KeyValue> labels);

    /**
     * Get the list of OpenShift application labels.
     *
     * @return labels
     */
    List<KeyValue> getLabels();

    /**
     * Set the enabled state of Deploy button.
     *
     * @param enabled
     */
    void setDeployButtonEnabled(boolean enabled);

    /**
     * Set the animation state of Deploy button.
     *
     * @param show
     */
    void showLoader(boolean show);

    /**
     * Set error message to display.
     *
     * @param error error message
     */
    void showError(String error);

    /**
     * Show invalid OpenShift project name error message. Attach tooltip
     * with {@code tooltipMessage}, if it is not null or empty, otherwise remove it.
     *
     * @param labelMessage
     *         message to display on label
     * @param tooltipMessage
     *         message to display in tooltip
     */
    void showProjectNameError(String labelMessage, String tooltipMessage);

    /** Hide invalid OpenShift project name error message. */
    void hideProjectNameError();

    /**
     * Show invalid OpenShift application name error message. Attach tooltip
     * with {@code tooltipMessage}, if it is not null or empty, otherwise remove it.
     *
     * @param labelMessage
     *         message to display on label
     * @param tooltipMessage
     *         message to display in tooltip
     */
    void showApplicationNameError(String labelMessage, String tooltipMessage);

    /** Hide invalid application name error message. */
    void hideApplicationNameError();

    /**
     * Show invalid OpenShift environment variable error message.
     *
     * @param message
     *         message to display
     */
    void showVariablesError(String message);

    /** Hide invalid OpenShift environment variable error message. */
    void hideVariablesError();

    /**
     * Show invalid OpenShift label name error message. Attach tooltip
     * with {@code tooltipMessage}, if it is not null or empty, otherwise remove it.
     *
     * @param labelMessage
     *         message to display on label
     * @param tooltipMessage
     *         message to display in tooltip
     */
    void showLabelsError(String labelMessage, String tooltipMessage);

    /** Hide invalid OpenShift label name error message. */
    void hideLabelsError();

    interface ActionDelegate {

        /**
         * Handle event, when cancel button is clicked.
         */
        void onCancelClicked();

        /**
         * Handler event, when deploy button is clicked.
         */
        void onDeployClicked();

        /**
         * Handler event, when application name is changed.
         *
         * @param name
         *         new application name
         */
        void onApplicationNameChanged(String name);

        /**
         * Handler event, when image stream is changed.
         *
         * @param stream
         *         new image stream
         */
        void onImageStreamChanged(String stream);

        /** Validate names on the form and enable buttons, depending on result */
        void updateControls();
    }
}
