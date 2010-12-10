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
package org.hippoecm.hst.mock.configuration.components;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;


/**
 * Mock implementation of {@link org.hippoecm.hst.configuration.components.HstComponentConfiguration}.
 *
 */
public class MockHstComponentConfiguration implements HstComponentConfiguration {

    private String id;
    private String name;
    private SortedMap<String, HstComponentConfiguration> componentConfigs =
            new TreeMap<String, HstComponentConfiguration>();
    private Map<String,String> parameters = new HashMap<String,String>();
    private Map<String,String> localParameters = new HashMap<String,String>();
    private String canonicalStoredLocation;
    private HstComponentConfiguration parent;
    private String referenceName;
    private String renderPath;
    private String serveResourcePath;
    private String componentClassName;
    private String canonicalIdentifier;
    private Type componentType;
    private String namedRenderer;
    private String namedResourceServer;
    private String pageErrorHandlerClassName;
    private String xType;

    public MockHstComponentConfiguration(String id) {
        this.id = id;
    }

    public HstComponentConfiguration getChildByName(String name) {
        return componentConfigs.get(name);
    }

    public SortedMap<String, HstComponentConfiguration> getChildren() {
        return componentConfigs;
    }
    
    public HstComponentConfiguration addChild(HstComponentConfiguration config){
        componentConfigs.put(config.getId(), config);
        return config;
    }
    
    public void addChildren(MockHstComponentConfiguration ... config){
        for (MockHstComponentConfiguration mockHstComponentConfiguration : config) {
            addChild(mockHstComponentConfiguration);
        }
    }

    public String getCanonicalStoredLocation() {
        return canonicalStoredLocation;
    }
    
    public void setCanonicalStoredLocation(String canonicalStoredLocation) {
        this.canonicalStoredLocation = canonicalStoredLocation;
    }

    public String getLocalParameter(String name) {
        return localParameters.get(name);
    }
    
    public void setLocalParameter(String name, String value) {
        localParameters.put(name, value);
    }

    public Map<String, String> getLocalParameters() {
        return localParameters;
    }

    public String getParameter(String name) {
        return parameters.get(name);
    }

    public void setParameter(String name, String value) {
        parameters.put(name,value);
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public HstComponentConfiguration getParent() {
        return parent;
    }
    
    public void setParent(HstComponentConfiguration parent) {
        this.parent = parent;
    }

    public String getReferenceName() {
        return referenceName;
    }
    
    public void setReferenceName(String referenceName) {
        this.referenceName = referenceName;
    }

    public String getRenderPath() {
        return renderPath;
    }
    
    public void setRenderPath(String renderPath) {
        this.renderPath = renderPath;
    }

    public String getServeResourcePath() {
        return serveResourcePath;
    }
    
    public void setServeResourcePath(String serveResourcePath) {
        this.serveResourcePath = serveResourcePath;
    }

    public String getComponentClassName() {
        return componentClassName;
    }
    
    public void setComponentClassName(String componentClassName) {
        this.componentClassName = componentClassName;
    }

    public String getId() {
        return id;
    }
    
    public void setId(String id){
        this.id = id;
    }

    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public String getCanonicalIdentifier() {
        return canonicalIdentifier;
    }

    public void setCanonicalIdentifier(String canonicalIdentifier) {
        this.canonicalIdentifier = canonicalIdentifier;
    }

    public Type getComponentType() {
        return componentType;
    }
    
    public void setComponentType(Type componentType) {
        this.componentType = componentType;
    }

    public String getNamedRenderer() {
        return namedRenderer;
    }
    
    public void setNamedRenderer(String namedRenderer) {
        this.namedRenderer = namedRenderer;
    }

    public String getNamedResourceServer() {
        return namedResourceServer;
    }

    public void setNamedResourceServer(String namedResourceServer) {
        this.namedResourceServer = namedResourceServer;
    }

    public String getPageErrorHandlerClassName() {
        return pageErrorHandlerClassName;
    }

    public void setPageErrorHandlerClassName(String pageErrorHandlerClassName) {
        this.pageErrorHandlerClassName = pageErrorHandlerClassName;
    }

    public String getXType() {
        return xType;
    }
    
    public void setXType(String xType) {
        this.xType = xType;
    }
}
