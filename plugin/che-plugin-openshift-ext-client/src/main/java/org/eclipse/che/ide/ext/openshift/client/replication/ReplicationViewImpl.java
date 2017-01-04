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
package org.eclipse.che.ide.ext.openshift.client.replication;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.ext.openshift.client.OpenshiftLocalizationConstant;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftResources;

/**
 * Implementation for {@ReplicationView}.
 *
 * @author Anna Shumilova
 */
@Singleton
public class ReplicationViewImpl implements ReplicationView {
    @Override
    public Widget asWidget() {
        return content;
    }

    interface ReplicationViewImplUiBinder extends UiBinder<FlowPanel, ReplicationViewImpl> {
    }

    private static ReplicationViewImplUiBinder uiBinder = GWT.create(ReplicationViewImplUiBinder.class);

    @UiField
    TextBox replicas;

    @UiField
    Button addButton;

    @UiField
    Button minusButton;

    @UiField
    FlowPanel replicasPanel;

    @UiField
    Label noReplicasMessage;

    @UiField(provided = true)
    OpenshiftLocalizationConstant locale;

    @UiField(provided = true)
    OpenshiftResources resources;

    FlowPanel content;

    private ActionDelegate delegate;

    @Inject
    protected ReplicationViewImpl(OpenshiftLocalizationConstant locale, OpenshiftResources resources) {
        this.locale = locale;
        this.resources = resources;
        content = uiBinder.createAndBindUi(this);
    }

    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    @UiHandler("addButton")
    public void onResetClicked(ClickEvent event) {
        delegate.onAddClicked();
    }

    @UiHandler("minusButton")
    public void onSaveClicked(ClickEvent event) {
        delegate.onMinusClicked();
    }

    @Override
    public void setReplicas(int number) {
        replicas.setValue(String.valueOf(number));
    }

    @Override
    public void setNoReplicaState(boolean visible) {
        replicasPanel.setVisible(!visible);
        noReplicasMessage.setVisible(visible);
    }

    @Override
    public void enableAddButton(boolean enabled) {
        addButton.setEnabled(enabled);
    }

    @Override
    public void enableMinusButton(boolean enabled) {
        minusButton.setEnabled(enabled);
    }
}
