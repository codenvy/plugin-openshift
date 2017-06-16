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
package org.eclipse.che.ide.ext.openshift.client.build;

import com.google.gwt.dom.client.PreElement;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.api.parts.base.BaseView;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftLocalizationConstant;
import org.eclipse.che.ide.ext.openshift.shared.dto.Build;
import static org.eclipse.che.ide.ext.openshift.shared.dto.BuildStatus.Phase.Complete;
import static org.eclipse.che.ide.ext.openshift.shared.dto.BuildStatus.Phase.Failed;
import static org.eclipse.che.ide.ext.openshift.shared.dto.BuildStatus.Phase.New;
import static org.eclipse.che.ide.ext.openshift.shared.dto.BuildStatus.Phase.Pending;
import static org.eclipse.che.ide.ext.openshift.shared.dto.BuildStatus.Phase.Running;
import org.vectomatic.dom.svg.ui.SVGImage;
import org.vectomatic.dom.svg.ui.SVGResource;

import java.util.HashMap;
import java.util.Map;

/**
 * The view of {@link BuildsPresenter}.
 *
 * @author Vitaliy Guliy
 */
@Singleton
public class BuildsViewImpl extends BaseView<BuildsView.ActionDelegate> implements BuildsView {

    interface BuildsViewImplUiBinder extends UiBinder<Widget, BuildsViewImpl> {
    }

    interface BuildsStyles extends CssResource {

        /** Project item */

        String project();

        String projectTitle();

        String projectButton();

        String projectIcon();

        String projectLabel();

        String projectBuilds();

        /** Build item */

        String build();

        String buildIcon();

        String buildLabel();

        /** Build Logger */

        String logger();

        String loggerHeader();

        String loggerBody();

        String loggerLines();

        /** Progress loading icons */
        String progress();

        String success();

        String error();
    }

    private Resources ideResources;

    @UiField
    BuildsStyles style;

    @UiField(provided = true)
    SplitLayoutPanel mainPanel;

    @UiField
    FlowPanel projectsPanel;

    @UiField
    FlowPanel logsPanel;

    /** A set of project items */
    private HashMap<String, ProjectItem> projectItems = new HashMap<>();

    /** Currently selected build item in the tree */
    private BuildItem selectedBuildItem;

    /** A set of build loggers */
    private HashMap<String, BuildLogger> buildLoggers = new HashMap<>();

    /** Currently displayed build logger in logs area */
    private BuildLogger selectedBuildLogger;

    @Inject
    public BuildsViewImpl(BuildsViewImplUiBinder uiBinder,
                          Resources ideResources,
                          OpenshiftLocalizationConstant locale) {
        this.ideResources = ideResources;
        mainPanel = new SplitLayoutPanel(3);
        setContentWidget(uiBinder.createAndBindUi(this));
        setTitle(locale.buildsPartTooltip());
    }

    @Override
    public void showProject(String namespace) {
        if (projectItems.containsKey(namespace)) {
            return;
        }

        ProjectItem projectItem = new ProjectItem(namespace);
        projectsPanel.add(projectItem);
        projectItems.put(namespace, projectItem);
    }

    @Override
    public void updateProject(String namespace, boolean connected) {
        if (projectItems.containsKey(namespace)) {
            projectItems.get(namespace).setConnected(connected);
        }
    }

    @Override
    public void showBuild(Build build) {
        showProject(build.getMetadata().getNamespace());

        ProjectItem projectItem = projectItems.get(build.getMetadata().getNamespace());
        projectItem.showBuild(build);
    }

    @Override
    public void selectBuild(Build build) {
        ProjectItem projectItem = projectItems.get(build.getMetadata().getNamespace());
        if (projectItem != null) {
            projectItem.selectBuild(build);
        }
    }

    @Override
    public void showLog(Build build) {
        String buildId = build.getMetadata().getNamespace() + "/" + build.getMetadata().getName();

        BuildLogger logger = buildLoggers.get(buildId);
        if (logger == null) {
            logger = new BuildLogger(build);
            logger.setVisible(false);
            logsPanel.add(logger);
            buildLoggers.put(buildId, logger);
        }

        if (selectedBuildLogger != null) {
            selectedBuildLogger.setVisible(false);
        }

        selectedBuildLogger = logger;
        selectedBuildLogger.setVisible(true);
    }

    @Override
    public void writeLog(Build build, String text) {
        String buildId = build.getMetadata().getNamespace() + "/" + build.getMetadata().getName();

        BuildLogger logger = buildLoggers.get(buildId);
        if (logger == null) {
            logger = new BuildLogger(build);
            logger.setVisible(false);
            logsPanel.add(logger);
            buildLoggers.put(buildId, logger);
        }

        logger.write(text);
    }

    @Override
    public void clear() {
        projectItems.clear();
        buildLoggers.clear();

        selectedBuildItem = null;
        selectedBuildLogger = null;

        projectsPanel.clear();
        logsPanel.clear();
    }

    /******************************************************************************************************************
     *
     * Project Item
     * Represents an OpenShift application with build items as children.
     *
     ******************************************************************************************************************/
    private class ProjectItem extends FlowPanel implements MouseDownHandler {

        private FlowPanel title;
        private FlowPanel button;
        private FlowPanel icon;
        private FlowPanel label;
        private FlowPanel builds;

        private Map<String, BuildItem> buildItems = new HashMap<>();

        private boolean connected = false;

        public ProjectItem(final String namespace) {
            setStyleName(style.project());

            // Title row
            title = new FlowPanel();
            title.setStyleName(style.projectTitle());
            add(title);

            // Expand / Collapse button
            button = new FlowPanel();
            button.setStyleName(style.projectButton());
            title.add(button);

            // Title icon
            icon = new FlowPanel();
            icon.setStyleName(style.projectIcon());
            title.add(icon);

            // Icon image
            SVGImage svgImage = new SVGImage(ideResources.projectFolder());
            icon.add(svgImage);

            // Title label
            label = new FlowPanel();
            label.setStyleName(style.projectLabel());
            label.getElement().setInnerHTML(namespace);
            title.add(label);

            // Builds panel
            builds = new FlowPanel();
            builds.setStyleName(style.projectBuilds());
            builds.setVisible(false);
            add(builds);

            button.addDomHandler(this, MouseDownEvent.getType());

            update();
        }

        /**
         * Appends new or updates existed build item.
         *
         * @param build
         *          buid
         */
        public void showBuild(Build build) {
            BuildItem buildItem = buildItems.get(build.getMetadata().getName());
            if (buildItem != null) {
                buildItem.update(build);
                return;
            }

            buildItem = new BuildItem(build);
            builds.add(buildItem);
            buildItems.put(build.getMetadata().getName(), buildItem);
        }

        /**
         * Updates tree item state.
         */
        private void update() {
            if (!connected) {
                button.getElement().setInnerHTML("");

                icon.getElement().getStyle().setProperty("opacity", "0.4");
                label.getElement().getStyle().setProperty("opacity", "0.3");

                builds.setVisible(false);
            } else {
                if (builds.isVisible()) {
                    button.getElement().setInnerHTML("-");
                } else {
                    button.getElement().setInnerHTML("+");
                }

                icon.getElement().getStyle().clearProperty("opacity");
                label.getElement().getStyle().clearProperty("opacity");
            }
        }

        @Override
        public void onMouseDown(MouseDownEvent event) {
            if (!connected) {
                return;
            }

            builds.setVisible(!builds.isVisible());
            update();
        }

        /**
         * Selects specified build in the project tree.
         *
         * @param build
         *          build to select
         */
        public void selectBuild(Build build) {
            if (!connected) {
                return;
            }

            BuildItem buildItem = buildItems.get(build.getMetadata().getName());
            if (buildItem != null) {
                if (!builds.isVisible()) {
                    builds.setVisible(true);
                    button.getElement().setInnerHTML("+");
                }

                buildItem.select();
            }
        }

        /**
         * Sets new connection state for the project.
         *
         * @param connected
         *          is connected or not
         */
        public void setConnected(boolean connected) {
            this.connected = connected;

            if (connected) {
                title.getElement().setAttribute("connected", "");
            } else {
                title.getElement().removeAttribute("connected");
            }

            update();
        }
    }

    /******************************************************************************************************************
     *
     * Build Item
     * Represents an OpenShift application build as project children in the project tree.
     *
     ******************************************************************************************************************/
    private class BuildItem extends FlowPanel implements MouseDownHandler {

        private Build build;

        private FlowPanel icon;
        private FlowPanel label;

        public BuildItem(Build build) {
            setStyleName(style.build());

            // Build Icon
            icon = new FlowPanel();
            icon.setStyleName(style.buildIcon());
            add(icon);

            // Build Label
            label = new FlowPanel();
            label.setStyleName(style.buildLabel());
            add(label);

            addDomHandler(this, MouseDownEvent.getType());

            update(build);
        }

        /**
         * Sets new SVG icon for the build item.
         *
         * @param resource
         *          SVG resource
         * @param styleName
         *          style name for the icon
         */
        private void setIcon(final SVGResource resource, final String styleName) {
            icon.clear();
            SVGImage svgImage = new SVGImage(resource);
            icon.add(svgImage);
            svgImage.getElement().setAttribute("class", styleName);
        }

        /**
         * Updates the item.
         *
         * @param build
         *          new build info
         */
        public void update(Build build) {
            this.build = build;

            label.getElement().setInnerHTML(build.getMetadata().getName());

            if (New.equals(build.getStatus().getPhase()) ||
                    Pending.equals(build.getStatus().getPhase()) ||
                    Running.equals(build.getStatus().getPhase())) {
                setIcon(ideResources.progress(), style.progress());//todo need check style
            } else if (Complete.equals(build.getStatus().getPhase())) {
                setIcon(ideResources.success(), style.success());
            } else if (Failed.equals(build.getStatus().getPhase())) {
                setIcon(ideResources.fail(), style.error());
            }
        }

        @Override
        public void onMouseDown(MouseDownEvent event) {
            select();
        }

        /**
         * Makes the build selected in the tree.
         */
        public void select() {
            if (selectedBuildItem != null) {
                selectedBuildItem.getElement().removeAttribute("selected");
            }

            selectedBuildItem = this;
            getElement().setAttribute("selected", "");

            delegate.buildSelected(build);
        }
    }

    /******************************************************************************************************************
     *
     * Build Logger
     * Represents a logs area for a build.
     *
     ******************************************************************************************************************/
    private class BuildLogger extends AbsolutePanel {

        private FlowPanel header;
        private ScrollPanel scrollPanel;
        private FlowPanel logsPanel;

        public BuildLogger(Build build) {
            setStyleName(style.logger());

            header = new FlowPanel();
            header.setStyleName(style.loggerHeader());
            header.getElement().setInnerHTML(build.getMetadata().getNamespace() + " / " + build.getMetadata().getName());
            add(header);

            FlowPanel body = new FlowPanel();
            body.setStyleName(style.loggerBody());
            add(body);

            scrollPanel = new ScrollPanel();
            body.add(scrollPanel);

            logsPanel = new FlowPanel();
            logsPanel.setStyleName(style.loggerLines());
            scrollPanel.add(logsPanel);
        }

        /**
         * Writes a text to the scrollable area.
         * Scrolls the area to the bottom if logger is visible.
         *
         * @param text
         *          text to write
         */
        public void write(String text) {
            PreElement pre = DOM.createElement("pre").cast();
            pre.setInnerHTML(text);
            logsPanel.getElement().appendChild(pre);

            if (isVisible()) {
                scrollPanel.scrollToBottom();
            }
        }
    }

}
