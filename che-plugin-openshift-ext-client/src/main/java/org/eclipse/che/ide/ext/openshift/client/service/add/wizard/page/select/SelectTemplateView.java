/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.ext.openshift.client.service.add.wizard.page.select;

import java.util.List;

import org.eclipse.che.ide.ext.openshift.shared.dto.Template;
import org.eclipse.che.ide.api.mvp.View;

import com.google.inject.ImplementedBy;

/**
 * The view of {@link SelectTemplatePresenter}
 *
 */
@ImplementedBy(SelectTemplateViewImpl.class)
public interface SelectTemplateView extends View<SelectTemplateView.ActionDelegate> {
    /**
     * Hides template list and shows template loader.
     */
    void showLoadingTemplates();

    /**
     * Hides template loader
     */
    void hideLoadingTemplates();

    /**
     * Sets available template list.
     */
    void setTemplates(List<Template> templates);

    /**
     * Handles operations from the view.
     */
    interface ActionDelegate {

        /**
         * Process operations when user selects template.
         */
        void onTemplateSelected(Template template);
    }
}
