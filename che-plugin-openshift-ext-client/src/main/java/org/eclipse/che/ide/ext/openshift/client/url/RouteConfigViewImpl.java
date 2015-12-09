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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.ext.openshift.client.OpenshiftLocalizationConstant;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftResources;
import org.eclipse.che.ide.ui.zeroclipboard.ClipboardButtonBuilder;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The implementation of View.
 *
 * @author Sergii Leschenko
 */
@Singleton
public class RouteConfigViewImpl implements RouteConfigView {
    @Override
    public Widget asWidget() {
        return content;
    }

    interface RouteConfigViewImplUiBinder extends UiBinder<FlowPanel, RouteConfigViewImpl> {
    }

    private static RouteConfigViewImplUiBinder ourUiBinder = GWT.create(RouteConfigViewImplUiBinder.class);

    @UiField
    FlowPanel urlsPanel;

    @UiField
    Button saveRouteUrlsButton;

    @UiField
    Button resetRouteUrlsButton;

    @UiField(provided = true)
    OpenshiftLocalizationConstant locale;

    @UiField(provided = true)
    OpenshiftResources resources;

    FlowPanel     content;
    List<TextBox> routeUrlTextBoxes;


    private final ClipboardButtonBuilder buttonBuilder;
    private       ActionDelegate         delegate;

    @Inject
    protected RouteConfigViewImpl(OpenshiftLocalizationConstant locale, OpenshiftResources resources,
                                  ClipboardButtonBuilder buttonBuilder) {
        this.locale = locale;
        this.resources = resources;
        this.buttonBuilder = buttonBuilder;
        this.routeUrlTextBoxes = new ArrayList<>();
        content = ourUiBinder.createAndBindUi(this);
    }

    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void showApplicationURLs(@NotNull List<String> urLs) {
        urlsPanel.clear();
        routeUrlTextBoxes.clear();

        if (urLs.size() == 0) {
            urlsPanel.add(new Label(locale.noApplicationUrlLabel()));
            return;
        }

        for (final String url : urLs) {
            if (url == null) {
                continue;
            }

            DockLayoutPanel panel = new DockLayoutPanel(Style.Unit.PX);
            panel.setSize("100%", "22px");
            panel.getElement().getStyle().setMarginBottom(10, Style.Unit.PX);

            Label label = new Label(locale.applicationConfigsRouteHost());
            label.getElement().getStyle().setLineHeight(22, Style.Unit.PX);
            panel.addWest(label, 40);

            Button openButton = new Button("Go", new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    com.google.gwt.user.client.Window.open("http://" + url, "_blank", "");
                }
            });
            openButton.addStyleName(resources.css().goButton());
            panel.addEast(openButton, 50);

            FlowPanel inputPanel = new FlowPanel();

            final TextBox urlInput = new TextBox();
            urlInput.addStyleName(resources.css().textInput());
            urlInput.setWidth("88%");
            urlInput.getElement().getStyle().setFloat(Style.Float.LEFT);
            urlInput.setText(url);

            urlInput.addKeyUpHandler(new KeyUpHandler() {
                @Override
                public void onKeyUp(KeyUpEvent event) {
                    delegate.onRouteUrlsChanged();
                }
            });

            routeUrlTextBoxes.add(urlInput);

            inputPanel.add(urlInput);

            panel.add(inputPanel);
            buttonBuilder.withResourceWidget(urlInput).withParentWidget(inputPanel).build().getStyle().setMarginLeft(5, Style.Unit.PX);

            urlsPanel.add(panel);
        }
    }

    @Override
    public List<String> getRouteURLs() {
        if (routeUrlTextBoxes.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> values = new ArrayList<>();
        for (TextBox textBox : routeUrlTextBoxes) {
            values.add(textBox.getValue());
        }
        return values;
    }

    @UiHandler("resetRouteUrlsButton")
    public void onResetRouteUrlsClicked(ClickEvent event) {
        delegate.onResetRouteUrls();
    }

    @UiHandler("saveRouteUrlsButton")
    public void onSaveRouteUrlsClicked(ClickEvent event) {
        delegate.onSaveRouteUrls();
    }


    @Override
    public void enableSaveRouteButton(boolean enabled) {
        saveRouteUrlsButton.setEnabled(enabled);
    }

    @Override
    public void enableResetRouteButton(boolean enabled) {
        resetRouteUrlsButton.setEnabled(enabled);
    }
}
