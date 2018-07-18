/**
 * ***************************************************************************** Copyright (c)
 * 2012-2017 Codenvy, S.A. All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: Codenvy, S.A. - initial API and implementation
 * *****************************************************************************
 */
package org.eclipse.che.ide.ext.openshift.client.config;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftLocalizationConstant;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftResources;
import org.eclipse.che.ide.ui.list.CategoriesList;
import org.eclipse.che.ide.ui.list.Category;
import org.eclipse.che.ide.ui.list.CategoryRenderer;
import org.eclipse.che.ide.ui.window.Window;
import org.eclipse.che.ide.ui.zeroclipboard.ClipboardButtonBuilder;

/**
 * The implementation of {@link ConfigureApplicationView}.
 *
 * @author Anna Shumilova
 */
@Singleton
public class ConfigureConfigureApplicationViewImpl extends Window
    implements ConfigureApplicationView {

  interface ConfigureApplicationViewImplUiBinder
      extends UiBinder<Widget, ConfigureConfigureApplicationViewImpl> {}

  private static ConfigureApplicationViewImplUiBinder uiBinder =
      GWT.create(ConfigureApplicationViewImplUiBinder.class);

  @UiField(provided = true)
  OpenshiftLocalizationConstant locale;

  @UiField(provided = true)
  OpenshiftResources resources;

  @UiField SimplePanel configsPanel;

  @UiField SimplePanel contentPanel;

  private final org.eclipse.che.ide.Resources coreResources;

  private ActionDelegate delegate;

  private CategoriesList configsList;

  private final Category.CategoryEventDelegate<ConfigPresenter> configDelegate =
      new Category.CategoryEventDelegate<ConfigPresenter>() {
        @Override
        public void onListItemClicked(
            com.google.gwt.dom.client.Element listItemBase, ConfigPresenter itemData) {
          delegate.onConfigSelected(itemData);
        }
      };

  private final CategoryRenderer<ConfigPresenter> configViewRenderer =
      new CategoryRenderer<ConfigPresenter>() {
        @Override
        public void renderElement(
            com.google.gwt.dom.client.Element element, ConfigPresenter configView) {
          element.setInnerText(configView.getTitle());
        }

        @Override
        public com.google.gwt.dom.client.SpanElement renderCategory(
            Category<ConfigPresenter> category) {
          SpanElement spanElement = Document.get().createSpanElement();
          spanElement.setClassName(coreResources.defaultCategoriesListCss().headerText());
          spanElement.setInnerText(category.getTitle().toUpperCase());
          return spanElement;
        }
      };

  @Inject
  protected ConfigureConfigureApplicationViewImpl(
      OpenshiftLocalizationConstant locale,
      OpenshiftResources openshiftResources,
      org.eclipse.che.ide.Resources coreResources,
      ClipboardButtonBuilder buttonBuilder) {
    this.locale = locale;
    this.resources = openshiftResources;
    this.coreResources = coreResources;
    setTitle(locale.applicationConfigViewTitle());

    this.ensureDebugId("openshiftConfigureApplication-window");

    Widget widget = uiBinder.createAndBindUi(this);

    this.setWidget(widget);

    configsList = new CategoriesList(coreResources);
    configsPanel.add(configsList);

    Button btnClose =
        createButton(
            locale.buttonClose(),
            "openshiftApplicationConfig-btnClose",
            new ClickHandler() {
              @Override
              public void onClick(ClickEvent event) {
                delegate.onCloseClicked();
              }
            });
    addButtonToFooter(btnClose);
  }

  @Override
  public AcceptsOneWidget getContentPanel() {
    return contentPanel;
  }

  @Override
  public void setConfigs(List<ConfigPresenter> configViews) {
    List<Category<?>> categoriesList = new ArrayList<>();
    categoriesList.add(
        new Category<ConfigPresenter>("Configs", configViewRenderer, configViews, configDelegate));
    configsList.render(categoriesList, true);
  }

  @Override
  public void selectConfig(ConfigPresenter config) {
    configsList.selectElement(config);
  }

  @Override
  public void close() {
    this.hide();
  }

  @Override
  public void showDialog() {
    this.show();
  }

  @Override
  public void setDelegate(ActionDelegate delegate) {
    this.delegate = delegate;
  }
}
