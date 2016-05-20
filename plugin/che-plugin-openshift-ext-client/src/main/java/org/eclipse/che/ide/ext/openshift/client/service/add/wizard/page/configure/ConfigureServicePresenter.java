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
package org.eclipse.che.ide.ext.openshift.client.service.add.wizard.page.configure;

import javax.inject.Inject;

import org.eclipse.che.ide.api.wizard.AbstractWizardPage;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftLocalizationConstant;
import org.eclipse.che.ide.ext.openshift.client.deploy._new.KeyValue;
import org.eclipse.che.ide.ext.openshift.client.dto.NewServiceRequest;
import org.eclipse.che.ide.ext.openshift.client.util.OpenshiftValidator;
import org.eclipse.che.ide.ext.openshift.shared.dto.Parameter;
import org.eclipse.che.ide.ext.openshift.shared.dto.Template;

import com.google.common.base.Strings;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Singleton;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Presenter for configure datasource service
 *
 * @author Andrienko Alexander
 */
@Singleton
public class ConfigureServicePresenter extends AbstractWizardPage<NewServiceRequest> implements ConfigureServiceView.ActionDelegate {

    private final ConfigureServiceView          view;
    private final OpenshiftLocalizationConstant locale;

    @Inject
    public ConfigureServicePresenter(ConfigureServiceView view,
                                     OpenshiftLocalizationConstant locale) {
        this.view = view;
        this.locale = locale;

        view.setDelegate(this);
    }

    @Override
    public void go(AcceptsOneWidget container) {
        container.setWidget(view);
    }

    @Override
    public void init(NewServiceRequest dataObject) {
        super.init(dataObject);

        Template template = dataObject.getTemplate();
        List<Parameter> parameterList = template.getParameters();
        Map<String, String> labels = template.getLabels();
        view.setEnvironmentVariables(parameterList);
        view.setEnvironmentLabels(labels);
        updateControls();
    }

    /**
     * Update labels and parameters in data object
     */
    public void updateData() {
        Template template = dataObject.getTemplate();

        Map<String, String> mapLabels = new HashMap<>();
        for (KeyValue keyValue : view.getEnvironmentLabels()) {
            if (!keyValue.getKey().isEmpty() && !keyValue.getValue().isEmpty()) {
                mapLabels.put(keyValue.getKey(), keyValue.getValue());
            }
        }
        template.setLabels(mapLabels);

        template.setParameters(view.getEnvironmentVariables());
    }

    @Override
    public boolean isCompleted() {
        return isValidLabels() & isValidEnvironmentVariables();
    }

    private boolean isValidLabels() {
        for (KeyValue label : view.getEnvironmentLabels()) {
            if (!OpenshiftValidator.isLabelValueValid(label.getKey()) || !OpenshiftValidator.isLabelValueValid(label.getValue())) {
                view.showLabelsError(locale.invalidLabelsError(), locale.invalidLabelsDetailError());
                return false;
            }
        }
        view.hideLabelsError();
        return true;
    }

    private boolean isValidEnvironmentVariables() {
        List<Parameter> parameters = view.getEnvironmentVariables();
        for (Parameter parameter : parameters) {
            if (parameter.getGenerate() == null && Strings.isNullOrEmpty(parameter.getValue())) {
                view.showEnvironmentError(locale.invalidVariablesError());
                return false;
            }
        }
        view.hideEnvironmentError();
        return true;
    }

    @Override
    public void updateControls() {
        updateDelegate.updateControls();
    }
}
