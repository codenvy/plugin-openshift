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
package org.eclipse.che.ide.ext.openshift.client.service.add.wizard.page.select;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Singleton;
import javax.validation.constraints.NotNull;

import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.wizard.AbstractWizardPage;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftLocalizationConstant;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftServiceClient;
import org.eclipse.che.ide.ext.openshift.client.dto.NewServiceRequest;
import org.eclipse.che.ide.ext.openshift.shared.dto.Template;

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;

import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_NAMESPACE_VARIABLE_NAME;

/**
 * Presenter which handles logic for selection templates
 *
 * @author Alexander Andrienko
 */
@Singleton
public class SelectTemplatePresenter extends AbstractWizardPage<NewServiceRequest> implements SelectTemplateView.ActionDelegate {

    private final SelectTemplateView            view;
    private final OpenshiftServiceClient        client;
    private final NotificationManager           notificationManager;
    private final AppContext                    appContext;
    private final OpenshiftLocalizationConstant locale;

    public static final  String DEFAULT_NAMESPACE = "openshift";
    private static final String DATABASE_TAG      = "database";
    
    private Template selectedTemplate;

    @Inject
    public SelectTemplatePresenter(SelectTemplateView view,
                                   OpenshiftServiceClient client,
                                   NotificationManager notificationManager,
                                   AppContext appContext,
                                   OpenshiftLocalizationConstant locale) {
        this.view = view;
        this.client = client;
        this.notificationManager = notificationManager;
        this.appContext = appContext;
        this.locale = locale;
        
        view.setDelegate(this);
    }
    
    @Override
    public void init(NewServiceRequest dataObject) {
        super.init(dataObject);
        selectedTemplate = null;
        
        ProjectConfigDto projectConfig = appContext.getCurrentProject().getRootProject();
        final String namespace = getAttributeValue(projectConfig, OPENSHIFT_NAMESPACE_VARIABLE_NAME);

        view.showLoadingTemplates();

        List<Template> allTemplates = new ArrayList<>();
        getTemplates(DEFAULT_NAMESPACE, allTemplates).then(getTemplates(namespace, allTemplates))
                                                     .then(filterByCategory(DATABASE_TAG))
                                                     .then(addTemplates())
                                                     .catchError(handleError());
    }

    private Promise<List<Template>> getTemplates(String nameSpace, final List<Template> allTemplates) {
        return client.getTemplates(nameSpace).then(new Function<List<Template>, List<Template>>() {
            @Override
            public List<Template> apply(List<Template> templates) throws FunctionException {
                allTemplates.addAll(templates);
                return allTemplates;
            }
        });
    }
    
    private Function<List<Template>, List<Template>> filterByCategory(@NotNull final String category) {
        return new Function<List<Template>, List<Template>>() {
            @Override
            public List<Template> apply(List<Template> templates) throws FunctionException {
                List<Template> filteredTemplates = new ArrayList<>();
                for (final Template template : templates) {
                    final String tags = template.getMetadata().getAnnotations().get("tags");
                    if (tags == null) {
                        continue;
                    }
                    for (String tag : tags.split(",")) {
                        if (category.equals(tag.trim())) {
                            filteredTemplates.add(template);
                        }
                    }
                }
                return filteredTemplates;
            }
        };
    }

    private Operation<List<Template>> addTemplates() {
        return new Operation<List<Template>>() {
            @Override
            public void apply(List<Template> templates) {
                view.setTemplates(templates);
            }
        };
    }
    
     private Operation<PromiseError> handleError() {
        return new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError promiseError) throws OperationException {
                notificationManager.showError(locale.getListTemplatesFailed() + " " + promiseError.getMessage());
                view.hideLoadingTemplates();
            }
        };
    }
    
    private String getAttributeValue(ProjectConfigDto projectConfig, String value) {
        List<String> attributes = projectConfig.getAttributes().get(value);
        if (attributes == null || attributes.isEmpty()) {
            return null;
        }
        return projectConfig.getAttributes().get(value).get(0);
    }
    
    @Override
    public boolean isCompleted() {
        return selectedTemplate != null;
    }

    @Override
    public void onTemplateSelected(Template template) {
        this.selectedTemplate = template;
        dataObject.setTemplate(template);
        updateDelegate.updateControls();
    }

    @Override
    public void go(AcceptsOneWidget container) {
        container.setWidget(view);
    }
}
