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
package org.eclipse.che.ide.ext.openshift.client;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

import org.eclipse.che.ide.ui.Styles;
import org.vectomatic.dom.svg.ui.SVGResource;

/**
 * The resource interface for the Openshift extension.
 *
 * @author Ann Shumilova
 */
public interface OpenshiftResources extends ClientBundle {

    @Source("actions/connect.svg")
    SVGResource connect();
    
    @Source("actions/create-from-template.svg")
    SVGResource createFromTemplate();
    
    @Source("actions/deploy-group.svg")
    SVGResource deployGroup();
    
    @Source("actions/deploy-new-app.svg")
    SVGResource deployNewApplication();

    @Source("actions/disconnect.svg")
    SVGResource disconnect();
    
    @Source("actions/import-app.svg")
    SVGResource importApplication();
    
    @Source("actions/link-to-app.svg")
    SVGResource linkToExistingApplication();
    
    @Source("actions/show-app-url.svg")
    SVGResource showApplicationUrl();
    
    @Source("actions/show-app-webhook.svg")
    SVGResource showApplicationWebhooks();
    
    /** Returns the CSS resource for the Openshift extension. */
    @Source({"openshift.css", "org/eclipse/che/ide/api/ui/style.css", "org/eclipse/che/ide/ui/Styles.css"})
    Css css();

    /** The CssResource interface for the Machine extension. */
    interface Css extends CssResource, Styles {

        String floatRight();

        String floatLeft();

        String marginV();

        String marginH();

        String sectionTitle();

        String sectionSeparator();

        String configList();

        String configDescription();

        String choiceTitle();

        String textInput();

        String textInputTitle();

        String projectApplicationBox();

        String warningLabel();

        String goButton();

        String smallButton();

        String applicationTable();
        
        String tableWithEmptyBorder();

        String deployApplicationTableError();

        String templateSection();

        String templateSectionTitle();

        String templateSectionDescription();

        String templateSectionSecondary();

        String templateSectionTags();

        String labelErrorPosition();

        String labelErrorPositionTable();

        String loadingCategoriesLabel();

        String flashingLabel();

        String applicationTableError();
    }
}
