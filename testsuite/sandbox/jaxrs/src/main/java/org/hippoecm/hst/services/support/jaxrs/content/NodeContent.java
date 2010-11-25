/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.services.support.jaxrs.content;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * NodeContent
 * 
 * @version $Id$
 */
@XmlRootElement(name = "node")
public class NodeContent extends ItemContent {
    
    private String primaryNodeTypeName;
    private String uuid;
    private Collection<PropertyContent> propertyContents;
    private Collection<NodeContent> children;
    
    public NodeContent() {
        super();
    }
    
    public NodeContent(String name) {
        super(name);
    }
    
    public NodeContent(String name, String path) {
        super(name, path);
    }
    
    public NodeContent(Node node) throws RepositoryException {
        this(node, null);
    }
    
    public NodeContent(Node node, final Set<String> propertyNamesFilledWithValues) throws RepositoryException {
        super(node);
        
        primaryNodeTypeName = node.getPrimaryNodeType().getName(); 
        
        if (node.isNodeType("mix:referenceable")) {
            this.uuid = node.getUUID();
        }
        
        propertyContents = new ArrayList<PropertyContent>();
        
        for (PropertyIterator it = node.getProperties(); it.hasNext(); ) {
            Property prop = it.nextProperty();
            
            if (propertyNamesFilledWithValues == null || propertyNamesFilledWithValues.contains(prop.getName())) {
                propertyContents.add(new PropertyContent(prop));
            } else {
                propertyContents.add(new PropertyContent(prop.getName(), prop.getPath()));
            }
        }
        
        children = new ArrayList<NodeContent>();
        
        for (NodeIterator it = node.getNodes(); it.hasNext(); ) {
            Node childNode = it.nextNode();
            
            if (childNode != null) {
                children.add(new NodeContent(childNode.getName(), childNode.getPath()));
            }
        }
    }
    
    public String getPrimaryNodeTypeName() {
        return primaryNodeTypeName;
    }
    
    public void setPrimaryNodeTypeName(String primaryNodeTypeName) {
        this.primaryNodeTypeName = primaryNodeTypeName;
    }
    
    public String getUuid() {
        return uuid;
    }
    
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    
    @XmlElementWrapper(name="properties")
    @XmlElements(@XmlElement(name="property"))
    public Collection<PropertyContent> getPropertyContents() {
        return propertyContents;
    }
    
    public void setPropertyContents(Collection<PropertyContent> propertyContents) {
        this.propertyContents = propertyContents;
    }
    
    public PropertyContent getPropertyContent(String propertyName) {
        if (propertyContents != null) {
            for (PropertyContent propertyContent : propertyContents) {
                if (propertyContent.getName().equals(propertyName)) {
                    return propertyContent;
                }
            }
        }
        
        return null;
    }
    
    @XmlElementWrapper(name="children")
    @XmlElements(@XmlElement(name="node"))
    public Collection<NodeContent> getChildren() {
        return children;
    }
    
    public void setChildren(List<NodeContent> children) {
        this.children = children;
    }
    
    public void buildChildUris(String urlBase, String siteContentPath, String encoding) throws UnsupportedEncodingException {
        if (propertyContents != null) {
            for (PropertyContent propertyContent : propertyContents) {
                propertyContent.buildUri(urlBase, siteContentPath, encoding);
            }
        }
        
        if (children != null) {
            for (NodeContent nodeContent : children) {
                nodeContent.buildUri(urlBase, siteContentPath, encoding);
            }
        }
    }
    
}
