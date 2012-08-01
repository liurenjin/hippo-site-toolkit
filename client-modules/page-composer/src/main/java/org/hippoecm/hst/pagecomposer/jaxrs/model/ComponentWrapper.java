/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.hst.pagecomposer.jaxrs.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.xml.bind.annotation.XmlRootElement;

import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.pagecomposer.jaxrs.model.utils.OldParametersInfoProcessor;
import org.hippoecm.hst.pagecomposer.jaxrs.model.utils.ParametersInfoProcessor;

/**
 * Component node wrapper that will be mapped automatically by the JAXRS (Jettison or JAXB) for generating the JSON/XML
 */
@XmlRootElement(name = "component")
public class ComponentWrapper {

    private static final String HST_PARAMETERNAMES = "hst:parameternames";
    private static final String HST_COMPONENTCLASSNAME = "hst:componentclassname";
    private static final String HST_PARAMETERVALUES = "hst:parametervalues";

    private List<Property> properties;
    private Boolean success = false;
    private Map<String, String> hstParameters;

    /**
     * Constructs a component node wrapper
     *
     * @param node JcrNode for a component.
     * @param locale the locale to get localized names, can be null
     * @throws RepositoryException    Thrown if the reposiotry exception occurred during reading of the properties.
     * @throws ClassNotFoundException thrown when this class can't instantiate the component class.
     */
    public ComponentWrapper(Node node, Locale locale, String currentMountCanonicalContentPath) throws RepositoryException, ClassNotFoundException {
        properties = new ArrayList<Property>();

        //Get the parameter names and values from the component node.
        if (node.hasProperty(HST_PARAMETERNAMES) && node.hasProperty(HST_PARAMETERVALUES)) {
            hstParameters = new HashMap<String, String>();
            Value[] paramNames = node.getProperty(HST_PARAMETERNAMES).getValues();
            Value[] paramValues = node.getProperty(HST_PARAMETERVALUES).getValues();
            for (int i = 0; i < paramNames.length; i++) {
                hstParameters.put(paramNames[i].getString(), paramValues[i].getString());
            }
        }

        //Get the properties via annotation on the component class
        String componentClassName = null;
        if (node.hasProperty(HST_COMPONENTCLASSNAME)) {
            componentClassName = node.getProperty(HST_COMPONENTCLASSNAME).getString();
        }

        if (componentClassName != null) {
            Class componentClass = Thread.currentThread().getContextClassLoader().loadClass(componentClassName);
            if (componentClass.isAnnotationPresent(ParametersInfo.class)) {
                // parse new style ParametersInfo
                ParametersInfo parametersInfo = (ParametersInfo) componentClass.getAnnotation(ParametersInfo.class);
                properties = ParametersInfoProcessor.getProperties(parametersInfo, locale, currentMountCanonicalContentPath);
            } else if (componentClass.isAnnotationPresent(org.hippoecm.hst.configuration.components.ParametersInfo.class)) {
                // parse deprecated old style ParametersInfo
                org.hippoecm.hst.configuration.components.ParametersInfo parameterInfo = (org.hippoecm.hst.configuration.components.ParametersInfo) componentClass.getAnnotation(org.hippoecm.hst.configuration.components.ParametersInfo.class);
                properties = OldParametersInfoProcessor.getProperties(parameterInfo, locale);
            }
            if (hstParameters != null) {
                for (Property prop : properties) {
                    if (hstParameters.get(prop.getName()) != null) {
                        prop.setValue(hstParameters.get(prop.getName()));
                    }
                }
            }
        }
        this.success = true;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public void setProperties(List<Property> properties) {
        this.properties = properties;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    /**
     * ParameterType used to provide a hint to the template composer about the type of the parameter.
     * This is just a convenience interface that provides some constants for the field types.
     *
     * @deprecated From HST version 2.23.01, the type of a parameter is determined from the return type of the
     * annotated getter method and (possibly) additional annotations in the package {@link org.hippoecm.hst.core.parameters}
     */
    @Deprecated
    public static interface ParameterType {
        String STRING = "STRING";
        String NUMBER = "NUMBER";
        String BOOLEAN = "BOOLEAN";
        String DATE = "DATE";
        String COLOR = "COLOR";
        String DOCUMENT = "DOCUMENT";
        String DROPDOWNLIST = "DROPDOWNLIST";
        String DOCUMENTPICKER = "DOCUMENTPICKER";
    }
    
    /**
     * @deprecated will be replaced soon 
     */
    @Deprecated
    public static class Property {
        private String name;
        private String value;
        private String type;
        private String label;
        private String defaultValue;
        private String description;
        private boolean required;

        private String docType;
        private boolean allowCreation;
        private String docLocation;

        private String pickerConfiguration;
        private String pickerInitialPath;
        private String pickerRootPath;

        private boolean pickerPathIsRelative;
        private boolean pickerRemembersLastVisited;
        private String[] pickerSelectableNodeTypes;

        private String[] dropDownListValues;
        private String[] dropDownListDisplayValues;

        public Property() {
            name = "";
            value = "";
            type = ComponentWrapper.ParameterType.STRING;
            label = "";
            defaultValue = "";
            description = "";
            required = false;
            docType = "";
            allowCreation = false;
            docLocation = "";
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            if (ComponentWrapper.ParameterType.DATE.equals(type)) {
                this.type = "datefield";
            } else if (ParameterType.BOOLEAN.equals(type)) {
                this.type = "checkbox";
            } else if (ParameterType.NUMBER.equals(type)) {
                this.type = "numberfield";
            } else if (ParameterType.DOCUMENT.equals(type)) {
                this.type = "documentcombobox";
            } else if (ParameterType.COLOR.equals(type)) {
                this.type = "colorfield";
            } else if (ParameterType.DROPDOWNLIST.equals(type)) {
                this.type = "combo";
            } else if (ParameterType.DOCUMENTPICKER.equals(type)) {
                this.type = "linkpicker";
            } else {
                this.type = "textfield";
            }
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }

        public void setDocType(final String docType) {
            this.docType = docType;
        }

        public String getDocType() {
            return this.docType;
        }

        public boolean isAllowCreation() {
            return allowCreation;
        }

        public void setAllowCreation(boolean allowCreation) {
            this.allowCreation = allowCreation;
        }

        public String getDocLocation() {
            return docLocation;
        }

        public void setDocLocation(String docLocation) {
            this.docLocation = docLocation;
        }

        public String[] getDropDownListValues() {
            return dropDownListValues;
        }

        public void setDropDownListValues(String[] dropDownValues) {
            this.dropDownListValues = dropDownValues;
        }

        public String[] getDropDownListDisplayValues() {
            return dropDownListDisplayValues;
        }

        public void setDropDownListDisplayValues(String[] dropDownListDisplayValues) {
            this.dropDownListDisplayValues = dropDownListDisplayValues;
        }

        public String getPickerConfiguration() {
            return pickerConfiguration;
        }

        public void setPickerConfiguration(final String pickerConfiguration) {
            this.pickerConfiguration = pickerConfiguration;
        }

        public String getPickerInitialPath() {
            return pickerInitialPath;
        }

        public void setPickerInitialPath(final String pickerInitialPath) {
            this.pickerInitialPath = pickerInitialPath;
        }

        public String getPickerRootPath() {
            return pickerRootPath;
        }

        public void setPickerRootPath(final String pickerRootPath) {
            this.pickerRootPath = pickerRootPath;
        }

        public boolean isPickerPathIsRelative() {
            return pickerPathIsRelative;
        }

        public void setPickerPathIsRelative(final boolean pickerPathIsRelative) {
            this.pickerPathIsRelative = pickerPathIsRelative;
        }

        public boolean isPickerRemembersLastVisited() {
            return pickerRemembersLastVisited;
        }

        public void setPickerRemembersLastVisited(final boolean pickerRemembersLastVisited) {
            this.pickerRemembersLastVisited = pickerRemembersLastVisited;
        }

        public String[] getPickerSelectableNodeTypes() {
            return pickerSelectableNodeTypes;
        }

        public void setPickerSelectableNodeTypes(final String[] pickerSelectableNodeTypes) {
            this.pickerSelectableNodeTypes = pickerSelectableNodeTypes;
        }

    }
   
}
