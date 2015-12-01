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
package org.eclipse.che.ide.ext.openshift.client.url;

import com.google.inject.ImplementedBy;

import org.eclipse.che.ide.api.mvp.View;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * The view of {@link RouteConfigPresenter}.
 *
 * @author Sergii Leschenko
 */
@ImplementedBy(RouteConfigViewImpl.class)
public interface RouteConfigView extends View<RouteConfigView.ActionDelegate> {
    /** Needs for delegate some function into application url view. */
    interface ActionDelegate {

        void onRouteUrlsChanged();

        void onResetRouteUrls();

        void onSaveRouteUrls();
    }

    void showApplicationURLs(@NotNull List<String> URLs);

    void enableSaveRouteButton(boolean enabled);

    void enableResetRouteButton(boolean enabled);

    List<String> getRouteURLs();
}
