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
package org.eclipse.che.ide.ext.openshift.client.service.add.wizard;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.wizard.Wizard;
import org.eclipse.che.ide.api.wizard.WizardPage;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftLocalizationConstant;
import org.eclipse.che.ide.ext.openshift.client.WizardFactory;
import org.eclipse.che.ide.ext.openshift.client.dto.NewServiceRequest;
import org.eclipse.che.ide.ext.openshift.client.service.add.wizard.page.configure.ConfigureServicePresenter;
import org.eclipse.che.ide.ext.openshift.client.service.add.wizard.page.configure.ConfigureServiceView;
import org.eclipse.che.ide.ext.openshift.client.service.add.wizard.page.select.SelectTemplatePresenter;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.openshift.shared.dto.Template;

import javax.validation.constraints.NotNull;
import static org.eclipse.che.ide.api.notification.StatusNotification.DisplayMode.EMERGE_MODE;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.FAIL;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.SUCCESS;

/**
 * Presenter, which controls datasource service creation
 *
 * @author Alexander Andrienko
 */
@Singleton
public class CreateServicePresenter implements Wizard.UpdateDelegate, CreateServiceWizardView.ActionDelegate {

    private final NotificationManager           notificationManager;
    private final OpenshiftLocalizationConstant locale;
    private final WizardFactory                 wizardFactory;
    private final DtoFactory                    dtoFactory;
    private final CreateServiceWizardView       view;
    private final SelectTemplatePresenter       selectPage;
    private final ConfigureServicePresenter     configureServicePage;
    private final ConfigureServiceView          configureServiceView;

    private CreateServiceWizard           wizard;
    private WizardPage<NewServiceRequest> currentPage;

    @Inject
    public CreateServicePresenter(NotificationManager notificationManager,
                                  OpenshiftLocalizationConstant locale,
                                  WizardFactory wizardFactory,
                                  DtoFactory dtoFactory,
                                  CreateServiceWizardView view,
                                  SelectTemplatePresenter selectPage,
                                  ConfigureServicePresenter configurePage,
                                  ConfigureServiceView configureServiceView) {

        this.notificationManager = notificationManager;
        this.locale = locale;
        this.wizardFactory = wizardFactory;
        this.dtoFactory = dtoFactory;
        this.view = view;
        this.selectPage = selectPage;
        this.configureServicePage = configurePage;
        this.configureServiceView = configureServiceView;

        view.setDelegate(this);
    }

    @Override
    public void onNextClicked() {
        WizardPage<NewServiceRequest> nextPage = wizard.navigateToNext();
        if (nextPage != null) {
            showWizardPage(nextPage);
            nextPage.init(wizard.getDataObject());
        }
    }

    @Override
    public void onPreviousClicked() {
        WizardPage<NewServiceRequest> previousPage = wizard.navigateToPrevious();
        if (previousPage != null) {
            showWizardPage(previousPage);
        }
    }

    @Override
    public void onCreateClicked() {
        configureServiceView.setEnabled(false);
        view.setPreviousButtonEnabled(false);
        view.setNextButtonEnabled(false);
        view.setCreateButtonEnabled(false);
        view.animateCreateButton(true);
        view.setBlocked(true);
        configureServicePage.updateData();

        wizard.complete(new Wizard.CompleteCallback() {
            @Override
            public void onCompleted() {
                updateControls();
                view.animateCreateButton(false);
                view.setBlocked(false);

                notificationManager.notify(locale.createServiceFromTemplateSuccess(), SUCCESS, EMERGE_MODE);
                view.closeWizard();
            }

            @Override
            public void onFailure(Throwable e) {
                updateControls();
                view.animateCreateButton(false);
                view.setBlocked(false);

                String message = e.getMessage() != null ? e.getMessage() : locale.createFromTemplateFailed();
                notificationManager.notify(locale.createServiceFromTemplateFailed() + " " + message, FAIL, EMERGE_MODE);
            }
        });
    }

    @Override
    public void updateControls() {
        view.setPreviousButtonEnabled(wizard.hasPrevious());
        view.setNextButtonEnabled(wizard.hasNext() && currentPage.isCompleted());
        view.setCreateButtonEnabled(wizard.canComplete() && !currentPage.equals(wizard.getFirstPage()));
    }

    /**
     * Show wizard page
     * @param wizardPage current wizard page
     */
    private void showWizardPage(@NotNull WizardPage<NewServiceRequest> wizardPage) {
        currentPage = wizardPage;
        updateControls();
        view.showPage(currentPage);
    }

    /**
     * Create wizard and show
     */
    public void createWizardAndShow() {
        NewServiceRequest newServiceRequest = dtoFactory.createDto(NewServiceRequest.class)
                                                        .withTemplate(dtoFactory.createDto(Template.class));
        wizard = wizardFactory.newServiceWizard(newServiceRequest);

        wizard.setUpdateDelegate(this);
        wizard.addPage(selectPage);
        wizard.addPage(configureServicePage);

        final WizardPage<NewServiceRequest> firstPage = wizard.navigateToFirst();
        if (firstPage != null) {
            showWizardPage(firstPage);
            view.showWizard();
        }
    }
}
