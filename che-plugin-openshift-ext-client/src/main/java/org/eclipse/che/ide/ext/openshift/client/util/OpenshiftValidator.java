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
package org.eclipse.che.ide.ext.openshift.client.util;

import com.google.common.base.Strings;

/**
 * Utility class, that contains validation methods for various
 * OpenShift entities
 *
 * @author Michail Kuznyetsov
 */
public class OpenshiftValidator {

    private static final String ENV_NAME     = "[_A-Za-z0-9]*";
    private static final String LABEL_NAME   = "(([A-Za-z0-9][-A-Za-z0-9_.]*)?[A-Za-z0-9])?";
    private static final String LABEL_NAME_PREFIX = "[a-z0-9]([-a-z0-9]*[a-z0-9])?(\\.[a-z0-9]([-a-z0-9]*[a-z0-9])?)*";
    private static final String LABEL_VALUE = "(([A-Za-z0-9][-A-Za-z0-9_.]*)?[A-Za-z0-9])?";
    private static final String PROJECT_NAME = "[a-z]([-a-z0-9]*[a-z0-9])?";

    private OpenshiftValidator() {
    }

    /**
     * Checks whether name matches OpenShift project naming rules.
     *
     * @param name
     *         OpenShift project name
     * @return true if project name is valid, false otherwise
     */
    public static boolean isProjectNameValid(String name) {
        return !(Strings.isNullOrEmpty(name)
                 || name.length() > 63
                 || name.length() < 2
                 || !name.matches(PROJECT_NAME));
    }

    /**
     *
     * @param name
     *         OpenShift application name
     * @return true if application name is valid, false otherwise
     */
    public static boolean isApplicationNameValid(String name) {
        return !(Strings.isNullOrEmpty(name)
                 || name.length() > 24
                 || name.length() < 2
                 || !name.matches(PROJECT_NAME));
    }

    /**
     *
     * @param variable
     *         OpenShift application variable name
     * @return true if variable name is valid, false otherwise
     */
    public static boolean isEnvironmentVariableNameValid(String variable) {
        return !(Strings.isNullOrEmpty(variable)
                 || variable.length() > 63
                 || !variable.matches(ENV_NAME));
    }

    /**
     *
     * @param labelName
     *         OpenShift application label name
     * @return true if label name is valid, false otherwise
     */
    public static boolean isLabelNameValid(String labelName) {
        if (Strings.isNullOrEmpty(labelName)) {
            return false;
        }
        String name;
        String[] splitted = labelName.split("/");
        if (splitted.length == 2) {
            String prefix = splitted[0];
            name = splitted[1];
            return !(Strings.isNullOrEmpty(prefix) || prefix.length() > 253 || !prefix.matches(LABEL_NAME_PREFIX)
                     || Strings.isNullOrEmpty(name) || name.length() > 63 || !name.matches(LABEL_NAME));
        } else if (splitted.length == 1) {
            name = splitted[0];
            return !(Strings.isNullOrEmpty(name) || name.length() > 63 || !name.matches(LABEL_NAME));
        } else {
            return false;
        }
    }

    /**
     *
     * @param labelValue
     *         OpenShift application label name
     * @return true if label name is valid, false otherwise
     */
    public static boolean isLabelValueValid(String labelValue) {
        return !(Strings.isNullOrEmpty(labelValue)
                || labelValue.length() > 63
                || !labelValue.matches(LABEL_VALUE));
    }
}
