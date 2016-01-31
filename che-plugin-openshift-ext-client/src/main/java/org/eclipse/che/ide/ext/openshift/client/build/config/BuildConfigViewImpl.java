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
package org.eclipse.che.ide.ext.openshift.client.build.config;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.ext.openshift.client.OpenshiftLocalizationConstant;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftResources;
import org.eclipse.che.ide.ext.openshift.shared.dto.WebHook;
import org.eclipse.che.ide.ui.zeroclipboard.ClipboardButtonBuilder;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Implementation of {@BuildConfigView}.
 *
 * @author Anna Shumilova
 */
@Singleton
public class BuildConfigViewImpl implements BuildConfigView {

    interface BuildConfigViewImplUiBinder extends UiBinder<FlowPanel, BuildConfigViewImpl> {
    }

    private static BuildConfigViewImplUiBinder uiBinder = GWT.create(BuildConfigViewImplUiBinder.class);


    @UiField(provided = true)
    OpenshiftLocalizationConstant locale;

    @UiField(provided = true)
    OpenshiftResources resources;

    @UiField
    TextBox sourceUrl;

    @UiField
    TextBox sourceReference;

    @UiField
    TextBox sourceContextDir;

    @UiField
    FlowPanel sourcePanel;

    @UiField
    Button saveButton;

    @UiField
    Button restoreButton;

    @UiField
    FlowPanel webhooksPanel;

    @UiField
    Label noWebhooksMessage;

    @UiField
    Label noBuildConfigsMessage;

    FlowPanel content;

    private ActionDelegate delegate;

    private final ClipboardButtonBuilder buttonBuilder;

    @Inject
    protected BuildConfigViewImpl(OpenshiftLocalizationConstant locale, OpenshiftResources resources,
                                  ClipboardButtonBuilder buttonBuilder) {
        this.locale = locale;
        this.resources = resources;
        this.buttonBuilder = buttonBuilder;
        content = uiBinder.createAndBindUi(this);
    }

    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public Widget asWidget() {
        return content;
    }

    @Override
    public void setSourceUrl(String url) {
        sourceUrl.setValue(url);
    }

    @Override
    public void setSourceReference(String branch) {
        sourceReference.setValue(branch);
    }

    @Override
    public void setSourceContextDir(String contextDir) {
        sourceContextDir.setValue(contextDir);
    }

    @Override
    public void setNoBuildConfigs(boolean isVisible) {
        noBuildConfigsMessage.setVisible(isVisible);
        sourcePanel.setVisible(!isVisible);
    }

    @Override
    public String getSourceUrl() {
        return sourceUrl.getValue();
    }

    @Override
    public String getSourceReference() {
        return sourceReference.getValue();
    }

    @Override
    public String getSourceContextDir() {
        return sourceContextDir.getValue();
    }

    @Override
    public void enableRestoreButton(boolean enabled) {
        restoreButton.setEnabled(enabled);
    }

    @Override
    public void enableSaveButton(boolean enabled) {
        saveButton.setEnabled(enabled);
    }

    @UiHandler("restoreButton")
    public void onResetRouteUrlsClicked(ClickEvent event) {
        delegate.onRestoreClicked();
    }

    @UiHandler("saveButton")
    public void onSaveRouteUrlsClicked(ClickEvent event) {
        delegate.onSaveClicked();
    }

    @UiHandler({"sourceUrl", "sourceContextDir", "sourceReference"})
    public void onSourceDataChanged(KeyUpEvent keyUpEvent) {
        delegate.onSourceDataChanged();
    }

    @Override
    public void setWebhooks(@NotNull List<WebHook> webHooks) {
        webhooksPanel.clear();

        boolean noWebhooks = webHooks.isEmpty();
        noWebhooksMessage.setVisible(noWebhooks);
        webhooksPanel.setVisible(!noWebhooks);

        if (noWebhooks) {
            return;
        }

        for (WebHook webHook : webHooks) {
            final Label label = new Label(webHook.getType());
            label.getElement().getStyle().setFontWeight(Style.FontWeight.BOLD);
            webhooksPanel.add(label);

            displayReadOnlyBox(webhooksPanel, locale.webhookURLLabelTitle(), webHook.getUrl());
            displayReadOnlyBox(webhooksPanel, locale.webhookSecretLabelTitle(), webHook.getSecret());
        }
    }

    private void displayReadOnlyBox(Panel parent, String label, String content) {
        final FlowPanel container = new FlowPanel();
        container.setWidth("100%");
        container.getElement().getStyle().setFloat(Style.Float.LEFT);
        parent.add(container);
        Label title = new Label(label);
        title.addStyleName(resources.css().textInputTitle());
        title.setWidth("15%");
        container.add(title);

        final TextBox readOnlyBox = new TextBox();
        readOnlyBox.addStyleName(resources.css().textInput());
        readOnlyBox.setReadOnly(true);
        readOnlyBox.setWidth("78%");
        readOnlyBox.setText(content);
        readOnlyBox.getElement().getStyle().setMarginBottom(10, Style.Unit.PX);
        container.add(readOnlyBox);

        buttonBuilder.withResourceWidget(readOnlyBox).build().getStyle().setFloat(Style.Float.RIGHT);
    }
}
