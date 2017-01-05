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
package org.eclipse.che.ide.ext.openshift.client.service.add.wizard.page.configure;

import javax.inject.Inject;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
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
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.inject.Singleton;

import org.eclipse.che.ide.ext.openshift.client.OpenshiftLocalizationConstant;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftResources;
import org.eclipse.che.ide.ext.openshift.client.deploy._new.KeyValue;
import org.eclipse.che.ide.ext.openshift.client.util.OpenshiftValidator;
import org.eclipse.che.ide.ext.openshift.shared.dto.Parameter;
import org.eclipse.che.ide.ui.Tooltip;
import org.eclipse.che.ide.ui.cellview.CellTableResources;
import org.eclipse.che.ide.ui.menu.PositionController;
import org.eclipse.che.ide.ui.window.Window;

import com.google.gwt.dom.client.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link ConfigureServiceView}
 */
@Singleton
public class ConfigureServiceViewImpl extends Window implements ConfigureServiceView {

    interface Template extends SafeHtmlTemplates {
        @Template("<input type=\"text\" value=\"{0}\" tabindex=\"-1\" ></input>")
        SafeHtml input(final String value);
    }

    interface ConfigureServiceViewUiBinder extends UiBinder<DockLayoutPanel, ConfigureServiceViewImpl> {
    }

    @UiField(provided = true)
    final OpenshiftResources resources;

    @UiField(provided = true)
    final OpenshiftLocalizationConstant locale;

    @UiField
    SimplePanel envVariablesPanel;

    @UiField
    SimplePanel labelsPanel;

    @UiField
    Button addLabelButton;

    @UiField
    Label labelsErrorLabel;

    @UiField
    Label environmentsErrorLabel;

    private ListDataProvider<Parameter> envVariablesProvider;
    private CellTable<Parameter>        envVariablesTable;

    private ListDataProvider<KeyValue> labelsProvider;
    private CellTable<KeyValue>        labelsTable;

    private ActionDelegate delegate;
    private Template       template;
    private Tooltip        labelsErrorTooltip;

    @Inject
    public ConfigureServiceViewImpl(CellTableResources cellTableResources,
                                    OpenshiftLocalizationConstant locale,
                                    OpenshiftResources resources,
                                    ConfigureServiceViewUiBinder uiBinder) {
        this.resources = resources;
        this.locale = locale;

        ensureDebugId("configure-service");
        setWidget(uiBinder.createAndBindUi(this));

        envVariablesProvider = new ListDataProvider<>();
        envVariablesTable = createVariablesTable(cellTableResources);
        envVariablesPanel.add(envVariablesTable);

        labelsProvider = new ListDataProvider<>();

        labelsTable = createLabelsTable(cellTableResources, new Predicate<String>() {
            @Override
            public boolean apply(String input) {
                return OpenshiftValidator.isLabelValueValid(input);
            }
        });
        labelsPanel.add(labelsTable);
        template = GWT.create(Template.class);
    }

    private CellTable<Parameter> createVariablesTable(final CellTableResources tableResources) {
        envVariablesTable = new CellTable<>(50, tableResources);
        envVariablesTable.setTableLayoutFixed(true);
        envVariablesTable.setKeyboardSelectionPolicy(HasKeyboardSelectionPolicy.KeyboardSelectionPolicy.DISABLED);
        envVariablesProvider.addDataDisplay(envVariablesTable);

        Column<Parameter, String> nameColumn = new Column<Parameter, String>(new TextCell()) {
            @Override
            public String getValue(Parameter parameter) {
                return parameter.getName();
            }
        };

        TextInputCellWithPlaceHolder variablesCell = new TextInputCellWithPlaceHolder(locale.environmentVarialbesValuePlaceholder());
        Column<Parameter, String> valueColumn = new Column<Parameter, String>(variablesCell) {
            @Override
            public void onBrowserEvent(Cell.Context context, Element elem, Parameter parameter, NativeEvent event) {
                super.onBrowserEvent(context, elem, parameter, event);


                if (event.getType().equals(BrowserEvents.KEYUP)) {
                    TextInputCellWithPlaceHolder cell = (TextInputCellWithPlaceHolder)envVariablesTable.getColumn(context.getColumn())
                                                                                                       .getCell();
                    String newValue = cell.getViewData(context.getKey()).getCurrentValue();

                    if (parameter.getGenerate() == null && Strings.isNullOrEmpty(newValue)) {
                        elem.getParentElement().addClassName(resources.css().applicationTableError());
                    } else {
                        elem.getParentElement().removeClassName(resources.css().applicationTableError());
                    }
                    getFieldUpdater().update(context.getIndex(), parameter, newValue);
                    delegate.updateControls();
                }
            }

            @Override
            public String getCellStyleNames(Cell.Context context, Parameter parameter) {
                if (parameter != null && parameter.getGenerate() == null && Strings.isNullOrEmpty(parameter.getValue())) {
                    return resources.css().applicationTableError();
                }
                return null;
            }

            @Override
            public void render(Cell.Context context, Parameter parameter, SafeHtmlBuilder sb) {
                if (parameter.getGenerate() == null) {
                    String value = parameter.getValue() == null ? "" : parameter.getValue();
                    sb.append(template.input(value));
                } else {
                    super.render(context, parameter, sb);
                }
            }

            @Override
            public String getValue(Parameter parameter) {
                return parameter.getValue();
            }
        };
        valueColumn.setFieldUpdater(new FieldUpdater<Parameter, String>() {
            @Override
            public void update(int index, Parameter parameter, String value) {
                parameter.setValue(value);
                delegate.updateControls();
            }
        });

        envVariablesTable.addColumn(nameColumn);
        envVariablesTable.setColumnWidth(nameColumn, 15, Style.Unit.PCT);
        envVariablesTable.addColumn(valueColumn);
        envVariablesTable.setColumnWidth(valueColumn, 20, Style.Unit.PCT);
        return envVariablesTable;
    }

    private CellTable<KeyValue> createLabelsTable(CellTableResources tableResources,
                                                  final Predicate<String> labelValidator) {
        labelsTable = new CellTable<>(50, tableResources);
        labelsTable.setTableLayoutFixed(true);
        labelsTable.setKeyboardSelectionPolicy(HasKeyboardSelectionPolicy.KeyboardSelectionPolicy.DISABLED);
        labelsProvider.addDataDisplay(labelsTable);

        final ValidatedInputCell keyTextInputCell = new ValidatedInputCell(labelValidator, locale.labelNamePlaceholder());
        final Column<KeyValue, String> keyColumn = new Column<KeyValue, String>(keyTextInputCell) {
            @Override
            public String getCellStyleNames(Cell.Context context, KeyValue keyValue) {
                if (!labelValidator.apply(keyValue.getKey())) {
                    return resources.css().applicationTableError();
                }
                return null;
            }

            @Override
            public String getValue(KeyValue keyValue) {
                return keyValue.getKey();
            }
        };
        keyColumn.setFieldUpdater(new FieldUpdater<KeyValue, String>() {
            @Override
            public void update(int index, KeyValue keyValue, String value) {
                keyValue.setKey(value);
                delegate.updateControls();
            }
        });

        ValidatedInputCell valueTextInputCell = new ValidatedInputCell(labelValidator, locale.labelValuePlaceholder());
        Column<KeyValue, String> valueColumn = new Column<KeyValue, String>(valueTextInputCell) {
            @Override
            public String getCellStyleNames(Cell.Context context, KeyValue keyValue) {
                if (!labelValidator.apply(keyValue.getValue())) {
                    return resources.css().applicationTableError();
                }
                return null;
            }

            @Override
            public String getValue(KeyValue keyValue) {
                return keyValue.getValue();
            }
        };
        valueColumn.setFieldUpdater(new FieldUpdater<KeyValue, String>() {
            @Override
            public void update(int index, KeyValue keyValue, String value) {
                keyValue.setValue(value);
                delegate.updateControls();
            }
        });

        Column<KeyValue, String> removeColumn = new Column<KeyValue, String>(new ButtonCell()) {
            @Override
            public String getValue(KeyValue value) {
                return "-";
            }

            @Override
            public void render(Cell.Context context, KeyValue keyValue, SafeHtmlBuilder sb) {
                Button removeButton = new Button();
                super.render(context, keyValue, sb.appendHtmlConstant(removeButton.getHTML()));
            }
        };
        removeColumn.setFieldUpdater(new FieldUpdater<KeyValue, String>() {
            @Override
            public void update(int index, KeyValue keyValue, String value) {
                labelsProvider.getList().remove(keyValue);
                labelsProvider.refresh();
                delegate.updateControls();
            }
        });

        labelsTable.addColumn(keyColumn);
        labelsTable.setColumnWidth(keyColumn, 20, Style.Unit.PCT);
        labelsTable.addColumn(valueColumn);
        labelsTable.setColumnWidth(valueColumn, 20, Style.Unit.PCT);
        labelsTable.addColumn(removeColumn);
        labelsTable.setColumnWidth(removeColumn, 1, Style.Unit.PCT);
        removeColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        return labelsTable;
    }

    private class ValidatedInputCell extends TextInputCellWithPlaceHolder {
        private final Predicate<String> validator;

        private ValidatedInputCell(Predicate<String> validator, String placeHolder) {
            super(placeHolder);
            this.validator = validator;
        }

        @Override
        public void onBrowserEvent(Cell.Context context, Element parent, String value, NativeEvent event,
                                   ValueUpdater<String> valueUpdater) {
            super.onBrowserEvent(context, parent, value, event, valueUpdater);
            if (event.getType().equals(BrowserEvents.KEYUP)) {
                String newValue = getInputElement(parent).getValue();
                if (!validator.apply(newValue)) {
                    parent.getParentElement().addClassName(resources.css().applicationTableError());
                } else {
                    parent.getParentElement().removeClassName(resources.css().applicationTableError());
                }
                valueUpdater.update(newValue);
                delegate.updateControls();
            }
        }
    }

    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public Widget asWidget() {
        return getWidget();
    }

    @Override
    public void setEnvironmentVariables(List<Parameter> parameters) {
        if (parameters != null) {
            envVariablesProvider.setList(parameters);
            envVariablesProvider.refresh();
        }
    }

    @Override
    public List<Parameter> getEnvironmentVariables() {
        return envVariablesProvider.getList();
    }

    @Override
    public void setEnvironmentLabels(Map<String, String> labels) {
        List<KeyValue> list = new ArrayList<>();
        for (Map.Entry<String, String> map : labels.entrySet()) {
            list.add(new KeyValue(map.getKey(), map.getValue()));
        }
        labelsProvider.setList(list);
        labelsProvider.refresh();
    }

    @Override
    public List<KeyValue> getEnvironmentLabels() {
        return labelsProvider.getList();
    }

    @UiHandler("addLabelButton")
    public void onAddLabelButtonClicked(ClickEvent clickEvent) {
        labelsProvider.getList().add(0, new KeyValue("", ""));
        labelsProvider.refresh();
        delegate.updateControls();
    }

    @Override
    public void setEnabled(boolean enabled) {
        setBlocked(enabled);
    }

    @Override
    public void showLabelsError(String labelMessage, String tooltipMessage) {
        labelsErrorLabel.setText(labelMessage);

        if (labelsErrorTooltip != null) {
            labelsErrorTooltip.destroy();
        }

        if (!Strings.isNullOrEmpty(tooltipMessage)) {
            labelsErrorTooltip = Tooltip.create((elemental.dom.Element)labelsErrorLabel.getElement(),
                                                PositionController.VerticalAlign.TOP,
                                                PositionController.HorizontalAlign.MIDDLE,
                                                tooltipMessage);
            labelsErrorTooltip.setShowDelayDisabled(false);
        }
    }

    @Override
    public void hideLabelsError() {
        labelsErrorLabel.setText("");
    }

    @Override
    public void showEnvironmentError(String labelMessage) {
        environmentsErrorLabel.setText(labelMessage);
    }

    @Override
    public void hideEnvironmentError() {
        environmentsErrorLabel.setText("");
    }
}
