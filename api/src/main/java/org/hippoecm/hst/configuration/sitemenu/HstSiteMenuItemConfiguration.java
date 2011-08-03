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
package org.hippoecm.hst.configuration.sitemenu;

import java.util.List;
import java.util.Map;

/**
 * Implementations should return an unmodifiable map for {@link #getSiteMenuItemConfigurations()} because clients should not
 * be able to modify the configuration
 *
 */
public interface HstSiteMenuItemConfiguration {
    /**
     * 
     * @return the name of this SiteMenuItem
     */
    String getName();
    
    /**
     * The sitemapitem path can point to a sitemap item that contains wildcards. The sitemapitem path can be for example 'news/2009/may', and
     * the sitemap item which is resolved as the link to this sitemenu item might be 'news/'*'/'*'' 
     * @return the sitemap path that should be able to resolve the link for this sitemenu configuration item
     */
    String getSiteMapItemPath();
    
    /**
     * When a sitemenu item has an external link (http://...) configured, it is retrieved by this method. When no external link is configured, <code>null</code> is 
     * returned. When an external link is configured, the {@ #getSiteMapItemPath()} is ignored
     * @return the configured external link or <code>null</code> if non is configured
     */
    String getExternalLink();
    
    /**
     * @return the container <code>HstSiteMenuConfiguration</code> of this <code>HstSiteMenuItemConfiguration</code>
     */
    HstSiteMenuConfiguration getHstSiteMenuConfiguration();
    
    /**
     * 
     * @return all direct child <code>SiteMenuItemConfiguration</code>'s of this item
     */
    List<HstSiteMenuItemConfiguration> getChildItemConfigurations();
    
    
    /**
     * @return the parent <code>HstSiteMenuItemConfiguration</code> and <code>null</code> is none exists (ie, it is a root)
     */
    HstSiteMenuItemConfiguration getParentItemConfiguration();
    
    /**
     * When developers have customized SiteMenuItem configuration with extra properties, these properties can be 
     * accessed through this Map
     * 
     * @return a Map containing the value for every property in the backing content provider for this SiteMenuItem
     */
     Map<String, Object> getProperties(); 
    
    /**
     * @return <code>true</code> when below this sitemenu item repository based navigation is expected
     */
    boolean isRepositoryBased();
    
    /**
     * 
     * @return the depth of repository based items in case of repository based navigation
     */
    int getDepth();
    
    /**
     * A HstSiteMenuItemConfiguration can contain a Map of parameters. A parameter from this Map can be accessed through this method. If it is not present, <code>null</code>
     * will be returned.
     * 
     * Parameters are inherited from ancestor HstSiteMenuItemConfiguration's. When this HstSiteMenuItemConfiguration configures the same parameter as an ancestor, the
     * value from the ancestor is overwritten. 
     * 
     * Implementations should return an unmodifiable map, for example {@link java.util.Collections$UnmodifiableMap} to avoid 
     * client code changing configurationn
     * 
     * @param name the name of the parameter
     * @return the value of the parameter or <code>null</code> when not present
     */
    String getParameter(String name);
    
    /**
     * The value of the local parameter, where there are no parameters inherited from ancestor items
     * @see {@link #getParameter(String)}, only this method returns parameters without inheritance
     * @param name the name of the parameter
     * @return the value of the parameter or <code>null</code> when not present
     */
    String getLocalParameter(String name);
    
    /**
     * Parameters are inherited from ancestor HstSiteMenuItemConfiguration's. When this HstSiteMenuItemConfiguration configures the same parameter as an ancestor, the
     * value from the ancestor is overwritten. 
     * 
     * @see {@link #getParameter(String)}, only now the entire parameters map is returned.
     * @return the Map of parameters contained in this <code>HstSiteMenu</code>. If no parameters present, and empty map is returned
     */
    Map<String, String> getParameters();
    
    /**
     * @see {@link #getParameters()}, only this method returns parameters (unmodifiable map) without inheritance
     * @return the Map of parameters contained in this <code>HstSiteMenuItemConfiguration</code>. If no parameters present, and empty map is returned
     */
    Map<String, String> getLocalParameters();
}
