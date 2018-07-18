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
package org.eclipse.che.ide.ext.openshift.client.service.add.wizard.page.select;

import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import elemental.dom.Element;
import elemental.html.DivElement;
import elemental.html.SpanElement;
import elemental.html.TableElement;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftResources;
import org.eclipse.che.ide.ext.openshift.shared.dto.Template;
import org.eclipse.che.ide.ui.list.SimpleList;
import org.eclipse.che.ide.util.dom.Elements;

/**
 * Implementation of {@link SelectTemplateView}
 *
 * @author Alexander Andrienko
 */
@Singleton
public class SelectTemplateViewImpl implements SelectTemplateView {

  interface SelectServiceViewUiBinder extends UiBinder<DockPanel, SelectTemplateViewImpl> {}

  @UiField Label templatesLabel;

  @UiField Label loadingCategoriesLabel;

  @UiField ScrollPanel templatePanel;

  private SimpleList<Template> templateList;
  private ActionDelegate delegate;
  private DockPanel widget;

  @Inject
  public SelectTemplateViewImpl(
      SelectServiceViewUiBinder uiBinder,
      Resources resources,
      final OpenshiftResources openshiftResources) {
    widget = uiBinder.createAndBindUi(this);

    TableElement breakPointsElement = Elements.createTableElement();
    breakPointsElement.setAttribute("style", "width: 100%");

    templateList =
        SimpleList.create(
            (SimpleList.View) breakPointsElement,
            resources.defaultSimpleListCss(),
            new SimpleList.ListItemRenderer<Template>() {
              @Override
              public void render(Element element, Template template) {
                String tags = template.getMetadata().getAnnotations().get("tags");
                tags = (tags != null) ? tags.replace(",", " ") : "";

                DivElement title =
                    Elements.createDivElement(openshiftResources.css().templateSectionTitle());
                title.setTextContent(template.getMetadata().getName());
                element.appendChild(title);

                DivElement description =
                    Elements.createDivElement(
                        openshiftResources.css().templateSectionDescription());
                description.setTextContent(
                    template.getMetadata().getAnnotations().get("description"));
                element.appendChild(description);

                DivElement namespace = Elements.createDivElement();
                SpanElement namespaceTitle =
                    Elements.createSpanElement(openshiftResources.css().templateSectionSecondary());
                namespaceTitle.setTextContent("Namespace:");
                namespaceTitle.getStyle().setMarginRight(10, "px");
                namespace.appendChild(namespaceTitle);
                SpanElement namespaceContent = Elements.createSpanElement();
                namespaceContent.setTextContent(template.getMetadata().getNamespace());
                namespace.appendChild(namespaceContent);
                element.appendChild(namespace);

                DivElement tag =
                    Elements.createDivElement(
                        openshiftResources.css().templateSectionTags(),
                        openshiftResources.css().templateSectionSecondary());
                tag.setTextContent(tags);
                element.appendChild(tag);

                element.getClassList().add(openshiftResources.css().templateSection());
              }
            },
            new SimpleList.ListEventDelegate<Template>() {

              @Override
              public void onListItemClicked(Element listItemBase, Template itemData) {
                templateList.getSelectionModel().setSelectedItem(itemData);
                delegate.onTemplateSelected(itemData);
              }

              @Override
              public void onListItemDoubleClicked(Element listItemBase, Template itemData) {
                // do nothing
              }
            });

    templatePanel.add(templateList);
  }

  @Override
  public void setDelegate(ActionDelegate delegate) {
    this.delegate = delegate;
  }

  @Override
  public Widget asWidget() {
    return widget;
  }

  @Override
  public void showLoadingTemplates() {
    // hide templates list
    templateList.asWidget().setVisible(false);
    templatesLabel.setVisible(false);

    // show loading label
    loadingCategoriesLabel.setVisible(true);
  }

  @Override
  public void hideLoadingTemplates() {
    // hide loading label
    loadingCategoriesLabel.setVisible(false);

    // show templates list
    templatesLabel.setVisible(true);
    templateList.asWidget().setVisible(true);
  }

  @Override
  public void setTemplates(List<Template> templates) {
    hideLoadingTemplates();
    templateList.render(templates);
  }
}
