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
package org.eclipse.che.ide.ext.openshift.client.service.add.wizard;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.mvp.Presenter;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftLocalizationConstant;
import org.eclipse.che.ide.ui.window.Window;

/**
 * Implementation of {@link CreateServiceWizardView}
 *
 */
@Singleton
public class CreateServiceWizardViewImpl extends Window implements CreateServiceWizardView {

    interface CreateServiceWizardViewUiBinder extends UiBinder<FlowPanel, CreateServiceWizardViewImpl> {//todo Window
    }

    @UiField
    SimplePanel wizardPanel;

    Button nextBtn;

    Button prevBtn;

    Button createBtn;

    @UiField(provided = true)
    final org.eclipse.che.ide.Resources resources;

    @UiField(provided = true)
    final CoreLocalizationConstant constants;

    private ActionDelegate delegate;

    @Inject
    public CreateServiceWizardViewImpl(org.eclipse.che.ide.Resources resources,
                                       CoreLocalizationConstant constants,
                                       OpenshiftLocalizationConstant openshiftConstant,
                                       CreateServiceWizardViewUiBinder uiBinder) {
        ensureDebugId("openshift-create-service-from-template");

        this.resources = resources;
        this.constants = constants;

        setTitle(openshiftConstant.createServiceFromTemplate());
        setWidget(uiBinder.createAndBindUi(this));

        createBtn = createPrimaryButton("Create", "openshift-create-from-template-create-button", new ClickHandler() {//TODO const
            @Override
            public void onClick(ClickEvent event) {
                delegate.onCreateClicked();
            }
        });
        addButtonToFooter(createBtn);
        createBtn.addStyleName(resources.Css().buttonLoader());

        nextBtn = createButton(constants.next(), "openshift-create-from-template-next-button", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                delegate.onNextClicked();
            }
        });
        addButtonToFooter(nextBtn);

        prevBtn = createButton(constants.back(), "openshift-create-from-template-prev-button", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                delegate.onPreviousClicked();
            }
        });
        addButtonToFooter(prevBtn);

        getWidget().getElement().getStyle().setPadding(0, Style.Unit.PX);
    }

    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void showWizard() {
        show();
    }

    @Override
    public void closeWizard() {
        hide();
    }

    @Override
    public void setNextButtonEnabled(boolean enabled) {
        nextBtn.setEnabled(enabled);
    }

    @Override
    public void setPreviousButtonEnabled(boolean enabled) {
        prevBtn.setEnabled(enabled);
    }

    @Override
    public void setCreateButtonEnabled(boolean enabled) {
        createBtn.setEnabled(enabled);
    }

    @Override
    public void animateCreateButton(boolean animate) {
        if (animate && !createBtn.getElement().hasAttribute("animated")) {
            // save state and start animation
            createBtn.getElement().setAttribute("originText", createBtn.getText());
            createBtn.getElement().getStyle().setProperty("minWidth", createBtn.getOffsetWidth() + "px");
            createBtn.setHTML("<i></i>");
            createBtn.getElement().setAttribute("animated", "true");
        } else if (!animate && createBtn.getElement().hasAttribute("animated")) {
            // stop animation and restore state
            createBtn.setText(createBtn.getElement().getAttribute("originText"));
            createBtn.getElement().removeAttribute("originText");
            createBtn.getElement().getStyle().clearProperty("minWidth");
            createBtn.getElement().removeAttribute("animated");
        }
    }

    @Override
    public void showPage(Presenter presenter) {
        wizardPanel.clear();
        presenter.go(wizardPanel);
    }
}
