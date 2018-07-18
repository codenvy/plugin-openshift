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
package org.eclipse.che.ide.ext.openshift.client.project.wizard;

import static org.eclipse.che.ide.api.notification.StatusNotification.DisplayMode.EMERGE_MODE;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.FAIL;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.SUCCESS;

import com.google.gwt.core.client.Scheduler;
import com.google.inject.Inject;
import javax.validation.constraints.NotNull;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.wizard.Wizard;
import org.eclipse.che.ide.api.wizard.WizardPage;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftLocalizationConstant;
import org.eclipse.che.ide.ext.openshift.client.WizardFactory;
import org.eclipse.che.ide.ext.openshift.client.build.BuildsPresenter;
import org.eclipse.che.ide.ext.openshift.client.dto.NewApplicationRequest;
import org.eclipse.che.ide.ext.openshift.client.project.wizard.page.configure.ConfigureProjectPresenter;
import org.eclipse.che.ide.ext.openshift.client.project.wizard.page.template.SelectTemplatePresenter;

/**
 * Presenter for new application request.
 *
 * @author Vlad Zhukovskiy
 */
public class CreateProjectPresenter
    implements Wizard.UpdateDelegate, CreateProjectView.ActionDelegate {

  private CreateProjectWizard wizard;
  private final CreateProjectView view;
  private final WizardFactory wizardFactory;
  private final ConfigureProjectPresenter configProjectPage;
  private final SelectTemplatePresenter selectTemplatePage;
  private final DtoFactory dtoFactory;
  private final NotificationManager notificationManager;
  private final OpenshiftLocalizationConstant locale;
  private final BuildsPresenter buildsPresenter;

  private WizardPage currentPage;

  @Inject
  public CreateProjectPresenter(
      CreateProjectView view,
      WizardFactory wizardFactory,
      ConfigureProjectPresenter configProjectPage,
      SelectTemplatePresenter selectTemplatePage,
      NotificationManager notificationManager,
      OpenshiftLocalizationConstant locale,
      DtoFactory dtoFactory,
      BuildsPresenter buildsPresenter) {
    this.view = view;
    this.wizardFactory = wizardFactory;
    this.configProjectPage = configProjectPage;
    this.selectTemplatePage = selectTemplatePage;
    this.dtoFactory = dtoFactory;
    this.notificationManager = notificationManager;
    this.locale = locale;
    this.buildsPresenter = buildsPresenter;

    view.setDelegate(this);
  }

  /** {@inheritDoc} */
  @Override
  public void onNextClicked() {
    final WizardPage nextPage = wizard.navigateToNext();
    if (nextPage != null) {
      showWizardPage(nextPage);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void onPreviousClicked() {
    final WizardPage prevPage = wizard.navigateToPrevious();
    if (prevPage != null) {
      showWizardPage(prevPage);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void onCreateClicked() {
    configProjectPage.setEnabled(false);
    view.setPreviousButtonEnabled(false);
    view.setNextButtonEnabled(false);
    view.setCreateButtonEnabled(false);
    view.animateCreateButton(true);
    view.setBlocked(true);

    wizard.complete(
        new Wizard.CompleteCallback() {
          @Override
          public void onCompleted() {
            final String namespace = wizard.getDataObject().getProject().getMetadata().getName();

            configProjectPage.setEnabled(true);
            updateControls();
            view.animateCreateButton(false);
            view.setBlocked(false);

            notificationManager.notify(locale.createFromTemplateSuccess(), SUCCESS, EMERGE_MODE);
            view.closeWizard();

            Scheduler.get()
                .scheduleDeferred(
                    new Scheduler.ScheduledCommand() {
                      @Override
                      public void execute() {
                        buildsPresenter.newApplicationCreated(namespace);
                      }
                    });
          }

          @Override
          public void onFailure(Throwable e) {
            configProjectPage.setEnabled(true);
            updateControls();
            view.animateCreateButton(false);
            view.setBlocked(false);

            String message =
                e.getMessage() != null ? e.getMessage() : locale.createFromTemplateFailed();
            notificationManager.notify(message, FAIL, EMERGE_MODE);
          }
        });
  }

  /** {@inheritDoc} */
  @Override
  public void updateControls() {
    view.setPreviousButtonEnabled(wizard.hasPrevious());
    view.setNextButtonEnabled(wizard.hasNext() && currentPage.isCompleted());
    view.setCreateButtonEnabled(wizard.canComplete());
  }

  private void showWizardPage(@NotNull WizardPage wizardPage) {
    currentPage = wizardPage;
    updateControls();
    view.showPage(currentPage);
  }

  /** Displays the wizard. */
  public void createWizardAndShow() {
    wizard =
        wizardFactory.newProjectWizard(
            dtoFactory
                .createDto(NewApplicationRequest.class)
                .withProjectConfigDto(dtoFactory.createDto(ProjectConfigDto.class)));

    wizard.setUpdateDelegate(this);
    wizard.addPage(selectTemplatePage);
    wizard.addPage(configProjectPage);

    final WizardPage<NewApplicationRequest> firstPage = wizard.navigateToFirst();
    if (firstPage != null) {
      showWizardPage(firstPage);
      view.showWizard();
    }
  }
}
