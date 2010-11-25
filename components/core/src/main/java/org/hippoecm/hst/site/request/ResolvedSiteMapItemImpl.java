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
package org.hippoecm.hst.site.request;

import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.util.PropertyParser;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ResolvedSiteMapItemImpl
 * 
 * @version $Id$
 */
public class ResolvedSiteMapItemImpl implements ResolvedSiteMapItem {

    private final static Logger log = LoggerFactory.getLogger(ResolvedSiteMapItemImpl.class);
    private HstSiteMapItem hstSiteMapItem;
    private Properties resolvedParameters;
    private Properties localResolvedParameters;
    private ResolvedMount resolvedMount;
    private String relativeContentPath;
    private HstComponentConfiguration hstComponentConfiguration;
    private HstComponentConfiguration portletHstComponentConfiguration;
    private String pathInfo;
    
    public ResolvedSiteMapItemImpl(HstSiteMapItem hstSiteMapItem , Properties params, String pathInfo, ResolvedMount resolvedMount) {
      
       this.pathInfo = PathUtils.normalizePath(pathInfo);
       this.hstSiteMapItem = hstSiteMapItem;
       this.resolvedMount = resolvedMount;
       
       HstSite hstSite = hstSiteMapItem.getHstSiteMap().getSite();
       if (hstSiteMapItem.getComponentConfigurationId() == null && hstSiteMapItem.getPortletComponentConfigurationId() == null) {
           log.debug("The ResolvedSiteMapItemImpl does not have a component configuration id because the sitemap item '{}' does not have one", hstSiteMapItem.getId());
       } else {
           String componentConfigurationId = hstSiteMapItem.getComponentConfigurationId();
           
           if (componentConfigurationId != null) {
               this.hstComponentConfiguration = hstSite.getComponentsConfiguration().getComponentConfiguration(componentConfigurationId);
           }
           
           String portletComponentConfigurationId = hstSiteMapItem.getPortletComponentConfigurationId();
           
           if (portletComponentConfigurationId != null) {
               this.portletHstComponentConfiguration = hstSite.getComponentsConfiguration().getComponentConfiguration(portletComponentConfigurationId);
           }
           
           if (this.hstComponentConfiguration == null && this.portletHstComponentConfiguration == null) {
               log.warn("ResolvedSiteMapItemImpl cannot be created correctly, because the component configuration id cannot be found. {} or {}", 
                       componentConfigurationId, portletComponentConfigurationId);
           }
       }
       
       /*
        * We take the properties form the hstSiteMapItem getParameters and replace params (like ${1}) with the params[] array 
        */
       
       this.resolvedParameters = new Properties();
       this.localResolvedParameters = new Properties();
       
       resolvedParameters.putAll(params);
       localResolvedParameters.putAll(params);
       
       PropertyParser pp = new PropertyParser(params);
       
       for(Entry<String, String> entry : hstSiteMapItem.getParameters().entrySet()) {
           Object o = pp.resolveProperty(entry.getKey(), entry.getValue());
           resolvedParameters.put(entry.getKey(), o);
       }
       
       for(Entry<String, String> entry : hstSiteMapItem.getLocalParameters().entrySet()) {
           Object o = pp.resolveProperty(entry.getKey(), entry.getValue());
           localResolvedParameters.put(entry.getKey(), o);
       }
       
       relativeContentPath = (String)pp.resolveProperty("relativeContentPath", hstSiteMapItem.getRelativeContentPath());

    }

    public int getStatusCode(){
        return this.hstSiteMapItem.getStatusCode();
    }

    public int getErrorCode(){
        return this.hstSiteMapItem.getErrorCode();
    }
    
    public HstSiteMapItem getHstSiteMapItem() {
        return this.hstSiteMapItem;
    }
    
    public HstComponentConfiguration getHstComponentConfiguration() {
        return this.hstComponentConfiguration;
    }

    public HstComponentConfiguration getPortletHstComponentConfiguration() {
        return this.portletHstComponentConfiguration;
    }
    
    public String getParameter(String name) {
        return (String)resolvedParameters.get(name);
    }
    
    public Properties getParameters(){
        return this.resolvedParameters;
    }
    

	public String getLocalParameter(String name) {
		return (String)localResolvedParameters.get(name);
	}

	public Properties getLocalParameters() {
		return this.localResolvedParameters;
	}

    public String getRelativeContentPath() {
        return relativeContentPath;
    }

    public String getPathInfo() {
        return this.pathInfo;
    }
    
    public ResolvedMount getResolvedMount() {
       return resolvedMount;
    }

    public String getNamedPipeline() {
        return hstSiteMapItem.getNamedPipeline();
    }

    public boolean isSecured() {
        return hstSiteMapItem.isSecured();
    }

    public Set<String> getRoles() {
        return hstSiteMapItem.getRoles();
    }

    public Set<String> getUsers() {
        return hstSiteMapItem.getUsers();
    }
}
