/*
 *  Copyright 2011 Hippo.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.pagecomposer.jaxrs.model.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.hippoecm.hst.core.parameters.Color;
import org.hippoecm.hst.core.parameters.DocumentLink;
import org.hippoecm.hst.core.parameters.DropDownList;
import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ComponentWrapper;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ComponentWrapper.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParametersInfoProcessor {

    private static Logger log = LoggerFactory.getLogger(ParametersInfoProcessor.class);

    public static List<Property> getProperties(ParametersInfo parameterInfo) {
        return getProperties(parameterInfo, null, null);
    }

    public static List<Property> getProperties(ParametersInfo parameterInfo, Locale locale, String currentMountCanonicalContentPath) {
        final List<Property> properties = new ArrayList<Property>();

        final Class classType = parameterInfo.type();
        if (classType == null) {
            return properties;
        }

        ResourceBundle resourceBundle = null;
        final String typeName = parameterInfo.type().getName();
        try {
            if (locale != null) {
                resourceBundle = ResourceBundle.getBundle(typeName, locale);
            } else {
                resourceBundle = ResourceBundle.getBundle(typeName);
            }
        } catch (MissingResourceException missingResourceException) {
            log.debug("Could not find a resource bundle for class '{}', locale '{}'. The template composer properties panel will show displayName values instead of internationalised labels.", new Object[]{typeName, locale});
        }

        for (Method method : classType.getMethods()) {
            if (method.isAnnotationPresent(Parameter.class)) {
                final Parameter propAnnotation = method.getAnnotation(Parameter.class);
                final Property prop = new ComponentWrapper.Property();
                prop.setName(propAnnotation.name());
                prop.setDefaultValue(propAnnotation.defaultValue());
                prop.setDescription(propAnnotation.description());
                prop.setRequired(propAnnotation.required());
                if (resourceBundle != null && resourceBundle.containsKey(propAnnotation.name())) {
                    prop.setLabel(resourceBundle.getString(propAnnotation.name()));
                } else {
                    if (propAnnotation.displayName().equals("")) {
                        prop.setLabel(propAnnotation.name());
                    } else {
                        prop.setLabel(propAnnotation.displayName());
                    }
                }

                String type = null;

                for (Annotation annotation : method.getAnnotations()) {
                    if (annotation == propAnnotation) {
                        continue;
                    }
                    if (annotation.annotationType() == DocumentLink.class) {
                        type = ComponentWrapper.ParameterType.DOCUMENT;
                        DocumentLink documentLink = (DocumentLink) annotation;
                        prop.setDocType(documentLink.docType());
                        prop.setDocLocation(documentLink.docLocation());
                        prop.setAllowCreation(documentLink.allowCreation());
                    } else if (annotation instanceof JcrPath) {
                        // for JcrPath we need some extra processing too
                            final JcrPath jcrPath = (JcrPath) annotation;
                        type = ComponentWrapper.ParameterType.DOCUMENTPICKER;
                        prop.setPickerConfiguration(jcrPath.pickerConfiguration());
                        prop.setPickerInitialPath(jcrPath.pickerInitialPath());
                        prop.setPickerRootPath(currentMountCanonicalContentPath);
                        prop.setPickerPathIsRelative(jcrPath.isRelative());
                        prop.setPickerRemembersLastVisited(jcrPath.pickerRemembersLastVisited());
                        prop.setPickerSelectableNodeTypes(jcrPath.pickerSelectableNodeTypes());
                    } else if (annotation.annotationType() == Color.class) {
                        type = ComponentWrapper.ParameterType.COLOR;
                    } else if (annotation instanceof DropDownList) {
                        final DropDownList dropDownList = (DropDownList)annotation;
                        type = ComponentWrapper.ParameterType.DROPDOWNLIST;
                        final String values[] = dropDownList.value();
                        final String[] displayValues = new String[values.length];

                        for (int i=0; i<values.length; i++) {
                            final String resourceKey = propAnnotation.name() + "/" + values[i];
                            if (resourceBundle != null && resourceBundle.containsKey(resourceKey)) {
                                displayValues[i] = resourceBundle.getString(resourceKey);
                            } else {
                                displayValues[i] = values[i];
                            }
                        }

                        prop.setDropDownListValues(values);
                        prop.setDropDownListDisplayValues(displayValues);
                    }
                }

                if (type == null) {
                    type = getReturnType(method);
                }
                prop.setType(type);

                // Set the value to be default value before setting it with original value
                prop.setValue(propAnnotation.defaultValue());
                properties.add(prop);
            }
        }

        return properties;
    }

    private static String getReturnType(final Method method) {
        Class<?> returnType = method.getReturnType();
        if (returnType == Date.class) {
            return ComponentWrapper.ParameterType.DATE;
        } else if (returnType == Long.class || returnType == long.class 
                || returnType == Integer.class || returnType == int.class 
                || returnType == Short.class || returnType == short.class) {
            return ComponentWrapper.ParameterType.NUMBER;
        } else if (returnType == Boolean.class || returnType == boolean.class) {
            return ComponentWrapper.ParameterType.BOOLEAN;
        } else if (returnType == String.class) {
            return ComponentWrapper.ParameterType.STRING;
        } else {
            return "UNKNOWN";
        }
    }

}