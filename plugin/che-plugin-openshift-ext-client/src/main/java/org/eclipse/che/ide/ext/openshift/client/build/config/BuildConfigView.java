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
package org.eclipse.che.ide.ext.openshift.client.build.config;

import com.google.inject.ImplementedBy;

import org.eclipse.che.ide.api.mvp.View;
import org.eclipse.che.ide.ext.openshift.shared.dto.WebHook;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * View for displaying and editing build configs.
 *
 * @author Anna Shumilova
 */
@ImplementedBy(BuildConfigViewImpl.class)
public interface BuildConfigView extends View<BuildConfigView.ActionDelegate> {
    /** Needs for delegate some function into build config view. */
    interface ActionDelegate {
        /** Handle event,when user clicks Save button. */
        void onSaveClicked();

        /** Handle event,when user clicks Restore button. */
        void onRestoreClicked();

        /** Handle event,when any of source data is changed. */
        void onSourceDataChanged();
    }

    /**
     * Set source URL to display.
     *
     * @param url source url
     */
    void setSourceUrl(String url);

    /**
     * Set source reference to display.
     *
     * @param reference source reference (branch, tag)
     */
    void setSourceReference(String reference);

    /**
     * Set source context directory to display.
     *
     * @param contextDir source context directory
     */
    void setSourceContextDir(String contextDir);

    /**
     * Display or hide state, when there are no build configs,
     *
     * @param isVisible
     */
    void setNoBuildConfigs(boolean isVisible);

    /**
     * Return source url from input,
     *
     * @return source url
     */
    String getSourceUrl();

    /**
     * Return source reference from input,
     *
     * @return source reference
     */
    String getSourceReference();

    /**
     * Return source context directory from input,
     *
     * @return source context directory
     */
    String getSourceContextDir();

    /**
     * Set the enabled state of Restore button.
     *
     * @param enabled enabled
     */
    void enableRestoreButton(boolean enabled);

    /**
     * Set the enabled state of Save button.
     *
     * @param enabled enabled
     */
    void enableSaveButton(boolean enabled);

    /**
     * Set application webhooks into field on the view.
     *
     * @param webhooks
     *         application webhooks what will be shown on view
     */
    void setWebhooks(@NotNull List<WebHook> webhooks);
}