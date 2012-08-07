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
package org.hippoecm.hst.pagecomposer.jaxrs.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.hippoecm.hst.core.parameters.*;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ComponentWrapper.Property;
import org.hippoecm.hst.pagecomposer.jaxrs.model.utils.ParametersInfoProcessor;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class ParametersInfoProcessorTest {

    @ParametersInfo(type=NewstyleInterface.class)
    static class NewstyleContainer {
    }

    @Test
    public void additionalAnnotationBasedProcessing() {
        ParametersInfo parameterInfo = NewstyleContainer.class.getAnnotation(ParametersInfo.class);
        List<Property> properties = ParametersInfoProcessor.getProperties(parameterInfo);
        assertEquals(14, properties.size());

        // sort properties alphabetically by name to ensure a deterministic order
        Collections.sort(properties, new PropertyComparator());

        Property colorProperty = properties.get(0);
        assertEquals("colorfield", colorProperty.getType());
        assertEquals("blue", colorProperty.getDefaultValue());
        assertEquals("blue", colorProperty.getValue());

        Property docLocProperty = properties.get(1);
        assertEquals("/content", docLocProperty.getDocLocation());
        assertEquals("documentcombobox", docLocProperty.getType());
        assertEquals("hst:testdocument", docLocProperty.getDocType());

        Property imageProperty = properties.get(2);
        assertEquals("textfield", imageProperty.getType());
        assertEquals("/content/gallery/default.png", imageProperty.getDefaultValue());
        assertEquals("/content/gallery/default.png", imageProperty.getValue());

        Property dateProperty = properties.get(3);
        assertEquals("datefield", dateProperty.getType());

        Property booleanProperty = properties.get(4);
        assertEquals("checkbox", booleanProperty.getType());

        Property booleanClassProperty = properties.get(5);
        assertEquals("checkbox", booleanClassProperty.getType());

        Property intProperty = properties.get(6);
        assertEquals("numberfield", intProperty.getType());

        Property integerClassProperty = properties.get(7);
        assertEquals("numberfield", integerClassProperty.getType());

        Property longProperty = properties.get(8);
        assertEquals("numberfield", longProperty.getType());

        Property longClassProperty = properties.get(9);
        assertEquals("numberfield", longClassProperty.getType());

        Property shortProperty = properties.get(10);
        assertEquals("numberfield", shortProperty.getType());

        Property shortClassProperty = properties.get(11);
        assertEquals("numberfield", shortClassProperty.getType());

        final Property comboBox = properties.get(12);
        assertEquals("combo", comboBox.getType());
        final String values[] = comboBox.getDropDownListValues();
        assertEquals(values.length, 3);
        assertEquals("value1", values[0]);
        assertEquals("value2", values[1]);
        assertEquals("value3", values[2]);

        String[] displayValues = comboBox.getDropDownListDisplayValues();
        assertEquals(3, displayValues.length);

        final Property linkPicker = properties.get(13);
        assertEquals("linkpicker", linkPicker.getType());
    }

    private static class PropertyComparator implements Comparator<Property> {

        @Override
        public int compare(final Property p1, final Property p2) {
            return p1.getName().compareTo(p2.getName());
        }

    }

}
