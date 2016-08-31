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
package org.eclipse.che.ide.ext.openshift.client.url;

import com.google.gwt.core.client.JsArrayMixed;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.core.model.project.ProjectConfig;
import org.eclipse.che.api.core.rest.shared.dto.ServiceError;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.notification.StatusNotification;
import org.eclipse.che.ide.api.resources.Project;
import org.eclipse.che.ide.api.resources.Resource;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftLocalizationConstant;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftServiceClient;
import org.eclipse.che.ide.ext.openshift.client.config.ConfigPresenter;
import org.eclipse.che.ide.ext.openshift.shared.dto.Route;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_APPLICATION_VARIABLE_NAME;
import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_NAMESPACE_VARIABLE_NAME;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.FAIL;

/**
 * Presenter for showing application route config.
 *
 * @author Sergii Leschenko
 */
@Singleton
public class RouteConfigPresenter implements ConfigPresenter, RouteConfigView.ActionDelegate {

    private final RouteConfigView               view;
    private final OpenshiftServiceClient        service;
    private final AppContext                    appContext;
    private final NotificationManager           notificationManager;
    private final OpenshiftLocalizationConstant locale;
    private final DtoFactory                    dtoFactory;
    private       List<String>                  routeURLs;
    private       List<Route>                   routes;

    @Inject
    public RouteConfigPresenter(RouteConfigView view,
                                OpenshiftServiceClient service,
                                AppContext appContext,
                                NotificationManager notificationManager,
                                OpenshiftLocalizationConstant locale,
                                DtoFactory dtoFactory) {
        this.appContext = appContext;
        this.notificationManager = notificationManager;
        this.locale = locale;
        this.service = service;
        this.view = view;
        this.dtoFactory = dtoFactory;
        this.view.setDelegate(this);
        this.routes = Collections.emptyList();
    }

    private void resetView() {
        //Route config:
        routes = Collections.emptyList();
        routeURLs = Collections.emptyList();
        displayRouteUrls(routeURLs);
    }

    @Override
    public void onRouteUrlsChanged() {
        boolean haveChanges = false;
        for (String url : view.getRouteURLs()) {
            if (!routeURLs.contains(url)) {
                haveChanges = true;
                break;
            }
        }

        view.enableResetRouteButton(haveChanges);
        view.enableSaveRouteButton(haveChanges);
    }

    @Override
    public void onResetRouteUrls() {
        view.showApplicationURLs(routeURLs);
        view.enableResetRouteButton(false);
        view.enableSaveRouteButton(false);
    }

    @Override
    public void onSaveRouteUrls() {
        List<Promise<?>> promises = new ArrayList<>();
        List<String> routeUrls = view.getRouteURLs();

        for (int i = 0; i < routes.size(); i++) {
            Route route = routes.get(i);
            String newRouteUrl = routeUrls.get(i);
            if (!route.getSpec().getHost().equals(newRouteUrl)) {
                route.getSpec().withHost(newRouteUrl);
                promises.add(service.updateRoute(route));
            }
        }

        Promises.all(promises.toArray(new Promise[promises.size()]))
                .then(new Operation<JsArrayMixed>() {
                    @Override
                    public void apply(JsArrayMixed arg) throws OperationException {
                        loadRouteUrls();
                    }
                })
                .catchError(onFailure(locale.updateApplicationUrlFailed()));
    }

    private void loadRouteUrls() {
        routes = Collections.emptyList();
        routeURLs = Collections.emptyList();
        final Resource resource = appContext.getResource();
        if (resource == null || !resource.getRelatedProject().isPresent()) {
            return;
        }
        final Project currentProject = resource.getRelatedProject().get();
        String namespace = currentProject.getAttribute(OPENSHIFT_NAMESPACE_VARIABLE_NAME);
        String application = currentProject.getAttribute(OPENSHIFT_APPLICATION_VARIABLE_NAME);
        service.getRoutes(namespace, application)
               .then(processRoutesToDisplay())
               .catchError(onFailure(locale.getRoutesError()));
    }

    private Operation<List<Route>> processRoutesToDisplay() {
        return new Operation<List<Route>>() {
            @Override
            public void apply(List<Route> result) throws OperationException {
                routes = result;
                routeURLs = new ArrayList<>();
                for (Route route : result) {
                    routeURLs.add(route.getSpec().getHost());
                }
                displayRouteUrls(routeURLs);
            }
        };
    }

    private void displayRouteUrls(List<String> routeURLs) {
        view.showApplicationURLs(routeURLs);
        view.enableResetRouteButton(false);
        view.enableSaveRouteButton(false);
    }

    private Operation<PromiseError> onFailure(final String errorTitle) {
        return new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                final ServiceError serviceError = dtoFactory.createDtoFromJson(arg.getMessage(), ServiceError.class);
                notificationManager.notify(errorTitle + "." + serviceError.getMessage(), FAIL, StatusNotification.DisplayMode.EMERGE_MODE);
            }
        };
    }

    /** Returns first value of attribute of null if it is absent in project config */
    private String getAttributeValue(ProjectConfig projectConfig, String attibuteValue) {
        final List<String> values = projectConfig.getAttributes().get(attibuteValue);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    @Override
    public String getTitle() {
        return locale.applicationConfigsRouteConfigTitle();
    }

    @Override
    public void go(AcceptsOneWidget container) {
        resetView();
        container.setWidget(view);
        loadRouteUrls();
    }
}
