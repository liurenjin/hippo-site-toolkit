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

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * This class can probably be removed once new items are added through the in-memory model instead of created
 * through jcr.
 * 
 */

public class ContainerItemRepresentation extends ComponentRepresentation {

    public ContainerItemRepresentation represent(Node node) throws RepositoryException {
        setId(node.getUUID());
        setName(node.getName());
        setPath(node.getPath());
        setParentId(node.getParent().getUUID());
        setTemplate(node.getProperty(HstNodeTypes.COMPONENT_PROPERTY_TEMPLATE_).getString());
        setType(HstComponentConfiguration.Type.CONTAINER_ITEM_COMPONENT.toString());
        setXtype(node.getProperty(HstNodeTypes.COMPONENT_PROPERTY_XTYPE).getString());

        setComponentClassName(node.hasProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME) ?
                node.getProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME).getString() : "");

        return this;
    }
    

}
