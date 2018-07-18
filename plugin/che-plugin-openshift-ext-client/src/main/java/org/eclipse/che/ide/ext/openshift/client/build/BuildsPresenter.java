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
package org.eclipse.che.ide.ext.openshift.client.build;

import static org.eclipse.che.ide.api.notification.StatusNotification.DisplayMode.FLOAT_MODE;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.FAIL;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.PROGRESS;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.SUCCESS;
import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_NAMESPACE_VARIABLE_NAME;
import static org.eclipse.che.ide.ext.openshift.shared.dto.BuildStatus.Phase.Complete;
import static org.eclipse.che.ide.ext.openshift.shared.dto.BuildStatus.Phase.Failed;
import static org.eclipse.che.ide.ext.openshift.shared.dto.BuildStatus.Phase.New;
import static org.eclipse.che.ide.ext.openshift.shared.dto.BuildStatus.Phase.Pending;
import static org.eclipse.che.ide.ext.openshift.shared.dto.BuildStatus.Phase.Running;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;
import java.util.HashMap;
import java.util.List;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.notification.StatusNotification;
import org.eclipse.che.ide.api.parts.PartStackType;
import org.eclipse.che.ide.api.parts.WorkspaceAgent;
import org.eclipse.che.ide.api.parts.base.BasePresenter;
import org.eclipse.che.ide.api.resources.Project;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftLocalizationConstant;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftWebSocketPathProvider;
import org.eclipse.che.ide.ext.openshift.client.dto.BuildChangeEvent;
import org.eclipse.che.ide.ext.openshift.client.oauth.OAuthTokenChangedEvent;
import org.eclipse.che.ide.ext.openshift.client.oauth.OAuthTokenChangedHandler;
import org.eclipse.che.ide.ext.openshift.shared.dto.Build;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.ide.websocket.WebSocket;
import org.eclipse.che.ide.websocket.events.ConnectionErrorHandler;
import org.eclipse.che.ide.websocket.events.ConnectionOpenedHandler;
import org.eclipse.che.ide.websocket.events.MessageReceivedEvent;
import org.eclipse.che.ide.websocket.events.MessageReceivedHandler;

/**
 * Manages OpenShift builds.
 *
 * <p>Presenter automatically describes on build channel for each OpenShift project, shows builds
 * changes and live logs.
 *
 * @author Vitaliy Guliy
 */
@Singleton
public class BuildsPresenter extends BasePresenter
    implements BuildsView.ActionDelegate, OAuthTokenChangedHandler {

  private final BuildsView view;
  private final WorkspaceAgent workspaceAgent;
  private final DtoFactory dtoFactory;
  private final NotificationManager notificationManager;
  private final OpenshiftLocalizationConstant locale;
  private final OpenshiftWebSocketPathProvider wsPathProvider;
  private AppContext appContext;

  private String webSocketBasePath = null;

  private String oauthToken = null;

  /** A set of build watchers. Use `namespace` for key */
  private HashMap<String, BuildsWatcher> buildWatchers = new HashMap<>();

  /** A set of logs watchers. Use `namespace` + `/` + `name` for key */
  private HashMap<String, LogsWatcher> logsWatchers = new HashMap<>();

  /** A set of notifications */
  private HashMap<String, StatusNotification> notifications = new HashMap<>();

  @Inject
  public BuildsPresenter(
      final BuildsView view,
      final WorkspaceAgent workspaceAgent,
      final OpenshiftWebSocketPathProvider wsPathProvider,
      final EventBus eventBus,
      final DtoFactory dtoFactory,
      final NotificationManager notificationManager,
      final OpenshiftLocalizationConstant locale,
      AppContext appContext) {
    this.view = view;
    this.workspaceAgent = workspaceAgent;
    this.dtoFactory = dtoFactory;
    this.notificationManager = notificationManager;
    this.locale = locale;
    this.wsPathProvider = wsPathProvider;
    this.appContext = appContext;

    view.setDelegate(this);

    eventBus.addHandler(OAuthTokenChangedEvent.TYPE, this);
  }

  @Override
  public void onOAuthTokenChanged(OAuthTokenChangedEvent event) {
    oauthToken = event.getToken();

    if (oauthToken == null) {
      stopWatching();
    } else {
      if (WebSocket.isSupported()) {
        getWebSocketBasePath();
      }
    }
  }

  /** Retrieves a base path for OpenShift WebSocket connections. */
  private void getWebSocketBasePath() {
    wsPathProvider.get(
        new AsyncCallback<String>() {
          @Override
          public void onSuccess(String webSocketPath) {
            webSocketBasePath = webSocketPath;
            checkWorkspaceProjects();
          }

          @Override
          public void onFailure(Throwable caught) {
            Log.error(getClass(), caught.getMessage());
          }
        });
  }

  /**
   * Checks workspace projects for being deployed on OpenShift and starts observation for builds for
   * that projects.
   */
  private void checkWorkspaceProjects() {
    for (Project project : appContext.getProjects()) {
      if (!project.getMixins().contains("openshift")) {
        continue;
      }

      List<String> namespaces = project.getAttributes().get(OPENSHIFT_NAMESPACE_VARIABLE_NAME);
      if (namespaces == null || namespaces.isEmpty()) {
        continue;
      }

      openView();

      String namespace = namespaces.get(0);
      view.showProject(namespace);
      if (!buildWatchers.containsKey(namespace)) {
        BuildsWatcher buildsWatcher = new BuildsWatcher(namespace);
        buildsWatcher.startWatching();
        buildWatchers.put(namespace, buildsWatcher);
      }
    }
  }

  /** Stop watching for builds and logs, closes all websocket connections to openshift origin. */
  private void stopWatching() {
    for (BuildsWatcher buildsWatcher : buildWatchers.values()) {
      buildsWatcher.stopWatching();
    }
    buildWatchers.clear();

    for (LogsWatcher logsWatcher : logsWatchers.values()) {
      logsWatcher.stopWatching();
    }
    logsWatchers.clear();

    view.clear();
  }

  /**
   * Opens the view and displays a notification when the user started a new build.
   *
   * @param build build
   */
  public void newBuildStarted(final Build build) {
    onBuildPending(build);

    openView();

    String namespace = build.getMetadata().getNamespace();
    if (!buildWatchers.containsKey(namespace)) {
      BuildsWatcher buildsWatcher = new BuildsWatcher(namespace);
      buildsWatcher.startWatching();
      buildWatchers.put(namespace, buildsWatcher);
    }

    view.showBuild(build);
    view.selectBuild(build);
  }

  /**
   * Opens the view and displays a notification when the user created or imported an OpenShift
   * application.
   *
   * @param namespace application namespace
   */
  public void newApplicationCreated(String namespace) {
    openView();
    view.showProject(namespace);

    if (!buildWatchers.containsKey(namespace)) {
      BuildsWatcher buildsWatcher = new BuildsWatcher(namespace);
      buildsWatcher.startWatching();
      buildWatchers.put(namespace, buildsWatcher);
    }
  }

  /**
   * Displays a notification when changing the build status on Pending.
   *
   * @param build build
   */
  private void onBuildPending(final Build build) {
    String buildId = build.getMetadata().getNamespace() + "/" + build.getMetadata().getName();

    if (!notifications.containsKey(buildId)) {
      StatusNotification notification =
          new StatusNotification(locale.buildStatusRunning(buildId), PROGRESS, FLOAT_MODE);
      notificationManager.notify(notification);
      notifications.put(buildId, notification);
    }
  }

  /**
   * Displays a notification when changing the build status on Running. Starts new logs watcher for
   * the build.
   *
   * @param build build
   */
  private void onBuildRunning(final Build build) {
    String buildId = build.getMetadata().getNamespace() + "/" + build.getMetadata().getName();

    if (!logsWatchers.containsKey(buildId)) {
      LogsWatcher logsWatcher = new LogsWatcher(build);
      logsWatchers.put(buildId, logsWatcher);
      logsWatcher.startWatching();
    }

    if (notifications.containsKey(buildId)) {
      StatusNotification notification = notifications.get(buildId);
      notification.setTitle(locale.buildStatusRunning(buildId));
      notification.setStatus(PROGRESS);
    } else {
      StatusNotification notification =
          new StatusNotification(locale.buildStatusRunning(buildId), PROGRESS, FLOAT_MODE);
      notificationManager.notify(notification);
      notifications.put(buildId, notification);
    }
  }

  /**
   * Displays a notification when successfully finishing the build.
   *
   * @param build build
   */
  private void onBuildComplete(final Build build) {
    String buildId = build.getMetadata().getNamespace() + "/" + build.getMetadata().getName();

    if (notifications.containsKey(buildId)) {
      StatusNotification notification = notifications.get(buildId);
      notification.setTitle(locale.buildStatusCompleted(buildId));
      notification.setStatus(SUCCESS);
      notificationManager.notify(notification);
      notifications.remove(buildId);
    }
  }

  /**
   * Displays a notification when failing the build.
   *
   * @param build build
   */
  private void onBuildFailed(final Build build) {
    String buildId = build.getMetadata().getNamespace() + "/" + build.getMetadata().getName();

    if (notifications.containsKey(buildId)) {
      StatusNotification notification = notifications.get(buildId);
      notification.setTitle(locale.buildStatusFailed(buildId));
      notification.setStatus(FAIL);

      notificationManager.notify(notification);
      notifications.remove(buildId);
    }
  }

  @Override
  public String getTitle() {
    return locale.buildsPartTitle();
  }

  /** Ensures view is opened and is active. */
  private void openView() {
    if (!workspaceAgent.getPartStack(PartStackType.INFORMATION).containsPart(this)) {
      workspaceAgent.openPart(this, PartStackType.INFORMATION);
      workspaceAgent.setActivePart(this);
    }
  }

  @Override
  public IsWidget getView() {
    return view;
  }

  @Override
  public String getTitleToolTip() {
    return locale.buildsPartTooltip();
  }

  @Override
  public void go(AcceptsOneWidget container) {
    container.setWidget(view);
  }

  /**
   * ****************************************************************************************************************
   *
   * <p>Builds Watcher
   *
   * <p>Manages connection to build channel, parses received messages as Build object, reacts on
   * changing the build status.
   *
   * <p>****************************************************************************************************************
   */
  private class BuildsWatcher
      implements MessageReceivedHandler, ConnectionOpenedHandler, ConnectionErrorHandler {

    /** WebSocket connection */
    private WebSocket webSocket;

    /** Application namespace */
    private String namespace;

    public BuildsWatcher(String namespace) {
      this.namespace = namespace;
    }

    /** Opens a websocket connection to the build channel. */
    public void startWatching() {
      if (oauthToken == null) {
        return;
      }

      try {
        webSocket =
            WebSocket.create(
                webSocketBasePath
                    + "/watch/namespaces/"
                    + namespace
                    + "/builds?access_token="
                    + oauthToken);
        webSocket.setOnMessageHandler(BuildsWatcher.this);
        webSocket.setOnOpenHandler(BuildsWatcher.this);
        webSocket.setOnErrorHandler(BuildsWatcher.this);
      } catch (Exception e) {
        Log.error(getClass(), e.getMessage());
      }
    }

    /** Closes opened websocket connection ignoring errors. */
    public void stopWatching() {
      try {
        if (webSocket != null) {
          webSocket.close();
        }
      } catch (Exception e) {
        Log.error(getClass(), e.getMessage());
      }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
      BuildChangeEvent buildChangeEvent =
          dtoFactory.createDtoFromJson(event.getMessage(), BuildChangeEvent.class);
      Build build = buildChangeEvent.getObject();

      view.showBuild(build);

      if (Pending.equals(build.getStatus().getPhase())) {
        onBuildPending(build);

      } else if (Running.equals(build.getStatus().getPhase())) {
        onBuildRunning(build);

      } else if (Complete.equals(build.getStatus().getPhase())) {
        onBuildComplete(build);

      } else if (Failed.equals(build.getStatus().getPhase())) {
        onBuildFailed(build);
      }
    }

    @Override
    public void onOpen() {
      view.updateProject(namespace, true);
    }

    @Override
    public void onError() {
      view.updateProject(namespace, false);
    }
  }

  /**
   * ****************************************************************************************************************
   *
   * <p>Logs Watcher
   *
   * <p>Manages connection to build logs channel, receives and displays build logs.
   *
   * <p>****************************************************************************************************************
   */
  private class LogsWatcher implements MessageReceivedHandler {

    /** Build */
    private Build build;

    /** WebSocket connection */
    private WebSocket webSocket;

    public LogsWatcher(Build build) {
      this.build = build;
    }

    /** Opens a websocket connection to the build logs channel. */
    public void startWatching() {
      if (oauthToken == null) {
        return;
      }

      String namespace = build.getMetadata().getNamespace();
      String buildName = build.getMetadata().getName();
      webSocket =
          WebSocket.create(
              webSocketBasePath
                  + "/namespaces/"
                  + namespace
                  + "/builds/"
                  + buildName
                  + "/log?follow=true&tailLines=1000&limitBytes=10485760&access_token="
                  + oauthToken);
      webSocket.setOnMessageHandler(LogsWatcher.this);
    }

    /** Closes opened websocket connection ignoring errors. */
    public void stopWatching() {
      try {
        if (webSocket != null) {
          webSocket.close();
        }
      } catch (Exception e) {
        Log.error(getClass(), e.getMessage());
      }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
      view.writeLog(build, event.getMessage());
    }
  }

  @Override
  public void buildSelected(Build build) {
    String buildId = build.getMetadata().getNamespace() + "/" + build.getMetadata().getName();

    view.showLog(build);

    if (New.equals(build.getStatus().getPhase()) || Pending.equals(build.getStatus().getPhase())) {
      return;
    }

    if (!logsWatchers.containsKey(buildId)) {
      LogsWatcher logsWatcher = new LogsWatcher(build);
      logsWatchers.put(buildId, logsWatcher);
      logsWatcher.startWatching();
    }
  }
}
