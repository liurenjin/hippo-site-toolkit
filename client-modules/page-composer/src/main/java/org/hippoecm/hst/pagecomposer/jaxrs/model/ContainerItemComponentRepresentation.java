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

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component node wrapper that will be mapped automatically by the JAXRS (Jettison or JAXB) for generating the JSON/XML
 */
@XmlRootElement(name = "component")
public class ContainerItemComponentRepresentation {

    private List<ContainerItemComponentPropertyRepresentation> properties;

    public List<ContainerItemComponentPropertyRepresentation> getProperties() {
        return properties;
    }

    public void setProperties(List<ContainerItemComponentPropertyRepresentation> properties) {
        this.properties = properties;
    }

}
