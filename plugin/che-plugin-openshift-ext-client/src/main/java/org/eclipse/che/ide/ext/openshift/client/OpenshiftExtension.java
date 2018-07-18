/**
 * ***************************************************************************** Copyright (c)
 * 2012-2017 Codenvy, S.A. All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: Codenvy, S.A. - initial API and implementation
 * *****************************************************************************
 */
package org.eclipse.che.ide.ext.openshift.client;

import static org.eclipse.che.ide.api.action.IdeActions.GROUP_MAIN_MENU;
import static org.eclipse.che.ide.api.action.IdeActions.GROUP_WINDOW;
import static org.eclipse.che.ide.api.constraints.Anchor.BEFORE;

import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.DefaultActionGroup;
import org.eclipse.che.ide.api.constraints.Constraints;
import org.eclipse.che.ide.api.extension.Extension;
import org.eclipse.che.ide.ext.openshift.client.build.StartBuildAction;
import org.eclipse.che.ide.ext.openshift.client.config.ConfigureApplicationAction;
import org.eclipse.che.ide.ext.openshift.client.delete.DeleteProjectAction;
import org.eclipse.che.ide.ext.openshift.client.deploy.LinkProjectWithExistingApplicationAction;
import org.eclipse.che.ide.ext.openshift.client.deploy._new.NewApplicationAction;
import org.eclipse.che.ide.ext.openshift.client.importapp.ImportApplicationAction;
import org.eclipse.che.ide.ext.openshift.client.oauth.ConnectAccountAction;
import org.eclipse.che.ide.ext.openshift.client.oauth.DisconnectAccountAction;
import org.eclipse.che.ide.ext.openshift.client.project.CreateApplicationFromTemplateAction;
import org.eclipse.che.ide.ext.openshift.client.project.UnlinkProjectAction;
import org.eclipse.che.ide.ext.openshift.client.service.add.AddServiceAction;

/**
 * Extension add OpenShift support to the IDE Application.
 *
 * @author Sergii Leschenko
 */
@Singleton
@Extension(title = "Openshift", version = "3.0.0")
public class OpenshiftExtension {
  public static final String OPENSHIFT_GROUP_MAIN_MENU = "OpenShift";

  @Inject
  public OpenshiftExtension(
      ActionManager actionManager,
      OpenshiftResources openshiftResources,
      OpenshiftLocalizationConstant constant,
      ConnectAccountAction connectAccountAction,
      DisconnectAccountAction disconnectAccountAction,
      CreateApplicationFromTemplateAction createApplicationFromTemplateAction,
      LinkProjectWithExistingApplicationAction deployToExistingApplicationAction,
      NewApplicationAction newApplicationAction,
      AddServiceAction addServiceAction,
      DeleteProjectAction deleteProjectAction,
      StartBuildAction startBuildAction,
      ImportApplicationAction importApplicationAction,
      UnlinkProjectAction unlinkProjectAction,
      ConfigureApplicationAction configureApplicationAction) {
    openshiftResources.css().ensureInjected();
    DefaultActionGroup mainMenu = (DefaultActionGroup) actionManager.getAction(GROUP_MAIN_MENU);

    DefaultActionGroup openshift =
        new DefaultActionGroup(OPENSHIFT_GROUP_MAIN_MENU, true, actionManager);
    actionManager.registerAction("openshift", openshift);
    mainMenu.add(openshift, new Constraints(BEFORE, GROUP_WINDOW));

    actionManager.registerAction("connectOpenshiftAccount", connectAccountAction);
    openshift.add(connectAccountAction);

    actionManager.registerAction("disconnectOpenshiftAccount", disconnectAccountAction);
    openshift.add(disconnectAccountAction);

    openshift.addSeparator();
    DefaultActionGroup deployGroup = new DefaultActionGroup("Deploy", true, actionManager);
    deployGroup.getTemplatePresentation().setDescription("Deploy application...");
    deployGroup
        .getTemplatePresentation()
        .setSVGResource(null); // TODO replace with icon in nearest future

    actionManager.registerAction("newOpenshiftApplication", newApplicationAction);
    deployGroup.add(newApplicationAction);

    actionManager.registerAction("deployToExistingApplication", deployToExistingApplicationAction);
    deployGroup.add(deployToExistingApplicationAction);

    deployGroup.getTemplatePresentation().setSVGResource(openshiftResources.deployGroup());
    openshift.add(deployGroup);

    actionManager.registerAction("unlinkOpenshiftProject", unlinkProjectAction);
    openshift.add(unlinkProjectAction);

    openshift.addSeparator();
    DefaultActionGroup serviceGroup = new DefaultActionGroup("Service", true, actionManager);
    serviceGroup.getTemplatePresentation().setDescription(constant.addServiceGroupDescription());
    serviceGroup
        .getTemplatePresentation()
        .setSVGResource(null); // TODO replace with icon in nearest future

    actionManager.registerAction("addService", addServiceAction);
    serviceGroup.add(addServiceAction);

    openshift.add(serviceGroup);

    openshift.addSeparator();
    actionManager.registerAction(
        "createOpenshiftApplicationFromTemplate", createApplicationFromTemplateAction);
    openshift.add(createApplicationFromTemplateAction);

    actionManager.registerAction("importApplication", importApplicationAction);
    openshift.add(importApplicationAction);

    actionManager.registerAction("startOpenshiftBuild", startBuildAction);
    openshift.add(startBuildAction);

    actionManager.registerAction("deleteOpenshiftProject", deleteProjectAction);
    openshift.add(deleteProjectAction);

    actionManager.registerAction("applicationConfig", configureApplicationAction);
    openshift.add(configureApplicationAction);
  }
}
