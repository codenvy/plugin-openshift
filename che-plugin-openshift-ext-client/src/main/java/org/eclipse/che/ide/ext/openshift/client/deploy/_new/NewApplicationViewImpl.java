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
package org.eclipse.che.ide.ext.openshift.client.deploy._new;

import elemental.dom.Element;
import elemental.html.SpanElement;
import elemental.html.TableElement;

import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.TextInputCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.view.client.ListDataProvider;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftLocalizationConstant;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftResources;
import org.eclipse.che.ide.ext.openshift.shared.dto.Project;
import org.eclipse.che.ide.ui.cellview.CellTableResources;
import org.eclipse.che.ide.ui.list.SimpleList;
import org.eclipse.che.ide.ui.listbox.CustomListBox;
import org.eclipse.che.ide.ui.window.Window;
import org.eclipse.che.ide.util.dom.Elements;

import java.util.Collections;
import java.util.List;

import static org.eclipse.che.ide.ext.openshift.client.deploy._new.NewApplicationView.Mode.CREATE_NEW_PROJECT;
import static org.eclipse.che.ide.ext.openshift.client.deploy._new.NewApplicationView.Mode.SELECT_EXISTING_PROJECT;

/**
 * View implementation for {@NewApplicationView}.
 *
 * @author Vlad Zhukovskiy
 */
@Singleton
public class NewApplicationViewImpl extends Window implements NewApplicationView {

    interface NewApplicationViewImplUiBinder extends UiBinder<DockLayoutPanel, NewApplicationViewImpl> {
    }

    private static NewApplicationViewImplUiBinder uiBinder = GWT.create(NewApplicationViewImplUiBinder.class);

    @UiField
    TextBox projectName;

    @UiField
    TextBox displayName;

    @UiField
    TextArea description;

    @UiField
    TextBox applicationName;

    @UiField
    RadioButton createNewProject;

    @UiField
    RadioButton choseExistProject;

    @UiField
    CustomListBox images;

    @UiField
    Button addLabelButton;

    @UiField
    Button addVariableButton;

    @UiField
    ScrollPanel osProjectListPanel;

    @UiField
    Label emptyProjectsMessage;

    @UiField(provided = true)
    CellTable<KeyValue> environmentVariables;

    private ListDataProvider<KeyValue> environmentVariablesProvider;

    @UiField(provided = true)
    CellTable<KeyValue> environmentLabels;

    private ListDataProvider<KeyValue> environmentLabelsProvider;

    private SimpleList<Project> projectsList;

    private Button cancelBtn;

    private Button deployBtn;

    @UiField(provided = true)
    OpenshiftResources resources;

    @UiField(provided = true)
    OpenshiftLocalizationConstant locale;

    private ActionDelegate delegate;

    @Inject
    public NewApplicationViewImpl(org.eclipse.che.ide.Resources coreResources, OpenshiftResources resources,
                                  CellTableResources cellTableResources,
                                  OpenshiftLocalizationConstant locale,
                                  CoreLocalizationConstant constants) {
        this.resources = resources;
        this.locale = locale;

        ensureDebugId("deployCheProject");
        setTitle(locale.deployProjectWindowTitle());
        getWidget().getElement().getStyle().setPadding(0, Style.Unit.PX);

        environmentVariablesProvider = new ListDataProvider<KeyValue>();
        environmentVariables = createCellTable(cellTableResources, environmentVariablesProvider);

        environmentLabelsProvider = new ListDataProvider<KeyValue>();
        environmentLabels = createCellTable(cellTableResources, environmentLabelsProvider);

        setWidget(uiBinder.createAndBindUi(this));

        projectsList = createProjectList(coreResources);
        osProjectListPanel.add(projectsList);

        deployBtn = createPrimaryButton(locale.deployProjectWindowDeploy(),
                                        "deployCheProject-deploy-button", new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        delegate.onDeployClicked();
                    }
                });
        deployBtn.addStyleName(coreResources.Css().buttonLoader());
        addButtonToFooter(deployBtn);

        cancelBtn = createButton(constants.cancel(), "deployCheProject-cancel-button", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                delegate.onCancelClicked();
            }
        });
        addButtonToFooter(cancelBtn);

        createNewProject.setValue(true);
        projectName.setEnabled(true);
        displayName.setEnabled(true);
        description.setEnabled(true);
    }

    private SimpleList<Project> createProjectList(org.eclipse.che.ide.Resources coreResources) {
        TableElement tableElement = Elements.createTableElement();
        tableElement.setWidth("100%");

        return SimpleList.create((SimpleList.View)tableElement, coreResources.defaultSimpleListCss(),
                                 new SimpleList.ListItemRenderer<Project>() {
                                     @Override
                                     public void render(Element listItemBase, Project itemData) {
                                         SpanElement container = Elements.createSpanElement();
                                         container.setInnerText(itemData.getMetadata().getName());
                                         listItemBase.appendChild(container);
                                     }
                                 },
                                 new SimpleList.ListEventDelegate<Project>() {
                                     @Override
                                     public void onListItemClicked(Element listItemBase, Project itemData) {
                                         if (choseExistProject.getValue()) {
                                             projectsList.getSelectionModel().setSelectedItem(itemData);
                                             delegate.onActiveProjectChanged(itemData);
                                         }
                                     }

                                     @Override
                                     public void onListItemDoubleClicked(Element listItemBase, Project itemData) {

                                     }
                                 });
    }

    private CellTable createCellTable(CellTableResources cellTableResources, final ListDataProvider<KeyValue> dataProvider) {
        CellTable<KeyValue> table = new CellTable<KeyValue>(50, cellTableResources);
        table.setKeyboardSelectionPolicy(HasKeyboardSelectionPolicy.KeyboardSelectionPolicy.DISABLED);
        dataProvider.addDataDisplay(table);

        final Column<KeyValue, String> nameColumn = new Column<KeyValue, String>(new TextInputCell()) {
            @Override
            public String getValue(KeyValue object) {
                return object.getKey();
            }
        };

        nameColumn.setFieldUpdater(new FieldUpdater<KeyValue, String>() {
            @Override
            public void update(int index, KeyValue object, String value) {
                object.setKey(value);
            }
        });


        Column<KeyValue, String> valueColumn = new Column<KeyValue, String>(new TextInputCell()) {
            @Override
            public String getValue(KeyValue object) {
                return object.getValue();
            }
        };

        valueColumn.setFieldUpdater(new FieldUpdater<KeyValue, String>() {
            @Override
            public void update(int index, KeyValue object, String value) {
                object.setValue(value);
            }
        });

        Column<KeyValue, String> removeColumn = new Column<KeyValue, String>(new ButtonCell()) {
            @Override
            public String getValue(KeyValue object) {
                return "-";
            }

            @Override
            public void render(Cell.Context context, KeyValue object, SafeHtmlBuilder sb) {
                Button removeButton = new Button();
                super.render(context, object, sb.appendHtmlConstant(removeButton.getHTML()));
            }
        };

        removeColumn.setFieldUpdater(new FieldUpdater<KeyValue, String>() {
            @Override
            public void update(int index, KeyValue object, String value) {
                dataProvider.getList().remove(object);
            }
        });


        table.addColumn(nameColumn);
        table.setColumnWidth(nameColumn, 35, Style.Unit.PCT);
        table.addColumn(valueColumn);
        table.setColumnWidth(valueColumn, 55, Style.Unit.PCT);
        table.addColumn(removeColumn);
        table.setColumnWidth(removeColumn, 10, Style.Unit.PCT);
        removeColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        return table;
    }

    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getApplicationName() {
        return applicationName.getValue();
    }

    @Override
    public void setApplicationName(String name) {
        applicationName.setValue(name);
        delegate.onApplicationNameChanged(name);
    }

    @UiHandler("addVariableButton")
    public void onAddVariable(ClickEvent event) {
        environmentVariablesProvider.getList().add(0, new KeyValue("", ""));
        environmentVariablesProvider.refresh();
    }

    @UiHandler("addLabelButton")
    public void onAddLabel(ClickEvent event) {
        environmentLabelsProvider.getList().add(0, new KeyValue("", ""));
        environmentLabelsProvider.refresh();
    }

    @UiHandler("applicationName")
    public void onApplicationNameChanged(KeyUpEvent event) {
        delegate.onApplicationNameChanged(applicationName.getValue());
    }

    @Override
    public String getOpenShiftProjectName() {
        return projectName.getValue();
    }

    @Override
    public void setOpenShiftProjectName(String name) {
        projectName.setValue(name);
        delegate.onProjectNameChanged(name);
    }

    @UiHandler("projectName")
    public void onProjectNameChanged(KeyUpEvent event) {
        delegate.onProjectNameChanged(projectName.getValue());
    }

    @Override
    public String getOpenShiftProjectDisplayName() {
        return displayName.getValue();
    }

    @Override
    public void setOpenShiftProjectDisplayName(String name) {
        displayName.setValue(name);
    }

    @Override
    public String getOpenShiftProjectDescription() {
        return description.getValue();
    }

    @Override
    public void setOpenShiftProjectDescription(String description) {
        this.description.setValue(description);
    }

    @Override
    public Project getOpenShiftSelectedProject() {
        return projectsList.getSelectionModel().getSelectedItem();
    }

    @Override
    public void setProjects(List<Project> projects) {
        boolean projectsAvailable = projects != null && !projects.isEmpty();
        emptyProjectsMessage.setVisible(!projectsAvailable);
        osProjectListPanel.setVisible(projectsAvailable);

        if (projectsAvailable) {
            projectsList.render(projects);
        }
    }


    @Override
    public void setImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            return;
        }
        this.images.clear();

        for (String image : images) {
            this.images.addItem(image, image);
        }
        this.images.setSelectedIndex(0);
        delegate.onImageStreamChanged(images.get(0));
    }

    @UiHandler({"createNewProject", "choseExistProject"})
    public void onCreateModeChanged(ValueChangeEvent<Boolean> event) {
        changeMode();
    }

    /**
     * Makes all the UI changes which rely on mode state.
     */
    private void changeMode() {
        projectName.setEnabled(createNewProject.getValue());
        displayName.setEnabled(createNewProject.getValue());
        description.setEnabled(createNewProject.getValue());

        if (createNewProject.getValue()) {
            projectsList.getSelectionModel().clearSelection();
        } else if (projectsList.size() > 0) {
            projectsList.getSelectionModel().setSelectedItem(0);
        }

        delegate.onModeChanged(createNewProject.getValue() ? CREATE_NEW_PROJECT : SELECT_EXISTING_PROJECT);
        delegate.onActiveProjectChanged(projectsList.getSelectionModel().getSelectedItem());
    }

    @UiHandler("images")
    public void onImagesChanged(ChangeEvent event) {
        delegate.onImageStreamChanged(images.getValue(images.getSelectedIndex()));
    }

    @Override
    public void setEnvironmentVariables(List<KeyValue> variables) {
        environmentVariablesProvider.getList().clear();
        environmentVariablesProvider.getList().addAll(variables);
        environmentVariablesProvider.refresh();
    }

    @Override
    public List<KeyValue> getEnvironmentVariables() {
        return environmentVariablesProvider.getList();
    }

    @Override
    public void setLabels(List<KeyValue> labels) {
        environmentLabelsProvider.getList().clear();
        environmentLabelsProvider.getList().addAll(labels);
        environmentLabelsProvider.refresh();
    }

    @Override
    public List<KeyValue> getLabels() {
        return environmentLabelsProvider.getList();
    }

    @Override
    public String getActiveImage() {
        return (images.getSelectedIndex() < 0) ? null : images.getValue(images.getSelectedIndex());
    }

    @Override
    public void setDeployButtonEnabled(boolean enabled) {
        deployBtn.setEnabled(enabled);
    }

    @Override
    public void showLoader(boolean show) {
        setBlocked(show);
        deployBtn.setEnabled(!show);
        if (show) {
            deployBtn.setHTML("<i></i>");
        } else {
            deployBtn.setText(locale.deployProjectWindowDeploy());
        }
    }

    @Override
    public void showError(String error) {
        //TODO display error on window
    }

    @Override
    public Mode getMode() {
        return createNewProject.getValue() ? CREATE_NEW_PROJECT : SELECT_EXISTING_PROJECT;
    }

    @Override
    public void setMode(Mode mode) {
        if (mode == CREATE_NEW_PROJECT) {
            createNewProject.setValue(true);
        }

        changeMode();
    }

    @Override
    public void show() {
        super.show();
        images.clear();
        projectsList.render(Collections.<Project>emptyList());
    }
}
