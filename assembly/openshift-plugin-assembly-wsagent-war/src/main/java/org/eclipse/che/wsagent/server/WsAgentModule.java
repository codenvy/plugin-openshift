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
package org.eclipse.che.wsagent.server;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;

import org.eclipse.che.ApiEndpointAccessibilityChecker;
import org.eclipse.che.EventBusURLProvider;
import org.eclipse.che.UriApiEndpointProvider;
import org.eclipse.che.UserTokenProvider;
import org.eclipse.che.api.auth.oauth.OAuthTokenProvider;
import org.eclipse.che.api.core.jsonrpc.JsonRpcRequestReceiver;
import org.eclipse.che.api.core.jsonrpc.JsonRpcRequestTransmitter;
import org.eclipse.che.api.core.jsonrpc.JsonRpcResponseReceiver;
import org.eclipse.che.api.core.jsonrpc.JsonRpcResponseTransmitter;
import org.eclipse.che.api.core.jsonrpc.impl.BasicJsonRpcObjectValidator;
import org.eclipse.che.api.core.jsonrpc.impl.JsonRpcDispatcher;
import org.eclipse.che.api.core.jsonrpc.impl.JsonRpcObjectValidator;
import org.eclipse.che.api.core.jsonrpc.impl.WebSocketJsonRpcDispatcher;
import org.eclipse.che.api.core.jsonrpc.impl.WebSocketJsonRpcRequestDispatcher;
import org.eclipse.che.api.core.jsonrpc.impl.WebSocketJsonRpcRequestTransmitter;
import org.eclipse.che.api.core.jsonrpc.impl.WebSocketJsonRpcResponseDispatcher;
import org.eclipse.che.api.core.jsonrpc.impl.WebSocketJsonRpcResponseTransmitter;
import org.eclipse.che.api.core.notification.WSocketEventBusClient;
import org.eclipse.che.api.core.rest.ApiInfoService;
import org.eclipse.che.api.core.rest.CoreRestModule;
import org.eclipse.che.api.core.util.FileCleaner.FileCleanerModule;
import org.eclipse.che.api.core.websocket.WebSocketMessageReceiver;
import org.eclipse.che.api.core.websocket.WebSocketMessageTransmitter;
import org.eclipse.che.api.core.websocket.impl.BasicWebSocketMessageTransmitter;
import org.eclipse.che.api.core.websocket.impl.BasicWebSocketTransmissionValidator;
import org.eclipse.che.api.core.websocket.impl.GuiceInjectorEndpointConfigurator;
import org.eclipse.che.api.core.websocket.impl.WebSocketTransmissionValidator;
import org.eclipse.che.api.git.GitConnectionFactory;
import org.eclipse.che.api.git.GitUserResolver;
import org.eclipse.che.api.git.LocalGitUserResolver;
import org.eclipse.che.api.project.server.ProjectApiModule;
import org.eclipse.che.api.ssh.server.HttpSshServiceClient;
import org.eclipse.che.api.ssh.server.SshServiceClient;
import org.eclipse.che.api.user.server.spi.PreferenceDao;
import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.git.impl.jgit.JGitConnectionFactory;
import org.eclipse.che.ide.ext.openshift.server.inject.OpenshiftModule;
import org.eclipse.che.inject.DynaModule;
import org.eclipse.che.plugin.java.server.rest.WsAgentURLProvider;
import org.eclipse.che.security.oauth.RemoteOAuthTokenProvider;

import javax.inject.Named;
import java.net.URI;

/**
 * @author Evgen Vidolob
 */
@DynaModule
public class WsAgentModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ApiInfoService.class);

        bind(PreferenceDao.class).to(org.eclipse.che.RemotePreferenceDao.class);

        bind(OAuthTokenProvider.class).to(RemoteOAuthTokenProvider.class);
        bind(SshServiceClient.class).to(HttpSshServiceClient.class);

        bind(org.eclipse.che.plugin.ssh.key.script.SshKeyProvider.class)
                .to(org.eclipse.che.plugin.ssh.key.script.SshKeyProviderImpl.class);

        install(new CoreRestModule());
        install(new FileCleanerModule());
        install(new ProjectApiModule());
        install(new org.eclipse.che.swagger.deploy.DocsModule());
        install(new org.eclipse.che.api.debugger.server.DebuggerModule());
        install(new org.eclipse.che.commons.schedule.executor.ScheduleModule());
        install(new OpenshiftModule());

        bind(GitUserResolver.class).to(LocalGitUserResolver.class);
        bind(GitConnectionFactory.class).to(JGitConnectionFactory.class);

        bind(URI.class).annotatedWith(Names.named("api.endpoint")).toProvider(UriApiEndpointProvider.class);
        bind(String.class).annotatedWith(Names.named("user.token")).toProvider(UserTokenProvider.class);
        bind(WSocketEventBusClient.class).asEagerSingleton();

        bind(String.class).annotatedWith(Names.named("event.bus.url")).toProvider(EventBusURLProvider.class);
        bind(ApiEndpointAccessibilityChecker.class);
        bind(WsAgentAnalyticsAddresser.class);

        bind(String.class).annotatedWith(Names.named("wsagent.endpoint"))
                          .toProvider(WsAgentURLProvider.class);

        configureJsonRpc();
        configureWebSocket();
    }

    //it's need for WSocketEventBusClient and in the future will be replaced with the property
    @Named("notification.client.event_subscriptions")
    @Provides
    @SuppressWarnings("unchecked")
    Pair<String, String>[] eventSubscriptionsProvider(@Named("event.bus.url") String eventBusURL) {
        return new Pair[]{Pair.of(eventBusURL, "")};
    }

    //it's need for EventOriginClientPropagationPolicy and in the future will be replaced with the property
    @Named("notification.client.propagate_events")
    @Provides
    @SuppressWarnings("unchecked")
    Pair<String, String>[] propagateEventsProvider(@Named("event.bus.url") String eventBusURL) {
        return new Pair[]{Pair.of(eventBusURL, "")};
    }

    private void configureWebSocket() {
        requestStaticInjection(GuiceInjectorEndpointConfigurator.class);

        bind(WebSocketTransmissionValidator.class).to(BasicWebSocketTransmissionValidator.class);

        bind(WebSocketMessageTransmitter.class).to(BasicWebSocketMessageTransmitter.class);

        MapBinder<String, WebSocketMessageReceiver> receivers =
                MapBinder.newMapBinder(binder(), String.class, WebSocketMessageReceiver.class);

        receivers.addBinding("jsonrpc-2.0").to(WebSocketJsonRpcDispatcher.class);
    }

    private void configureJsonRpc() {
        bind(JsonRpcObjectValidator.class).to(BasicJsonRpcObjectValidator.class);

        bind(JsonRpcResponseTransmitter.class).to(WebSocketJsonRpcResponseTransmitter.class);
        bind(JsonRpcRequestTransmitter.class).to(WebSocketJsonRpcRequestTransmitter.class);

        MapBinder<String, JsonRpcDispatcher> dispatchers =
                MapBinder.newMapBinder(binder(), String.class, JsonRpcDispatcher.class);

        dispatchers.addBinding("request").to(WebSocketJsonRpcRequestDispatcher.class);
        dispatchers.addBinding("response").to(WebSocketJsonRpcResponseDispatcher.class);

        MapBinder<String, JsonRpcRequestReceiver> requestReceivers =
                MapBinder.newMapBinder(binder(), String.class, JsonRpcRequestReceiver.class);
        MapBinder<String, JsonRpcResponseReceiver> responseReceivers =
                MapBinder.newMapBinder(binder(), String.class, JsonRpcResponseReceiver.class);
    }
}
