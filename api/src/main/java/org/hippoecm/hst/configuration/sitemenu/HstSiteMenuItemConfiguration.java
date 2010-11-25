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

import java.io.Serializable;
import java.util.List;

/**
 * Implementations should return an unmodifiable map for {@link #getSiteMenuItemConfigurations()} because clients should not
 * be able to modify the configuration
 *
 */
public interface HstSiteMenuItemConfiguration extends Serializable{
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
     * @return the container <code>HstSiteMenuConfiguration</code> of this <code>HstSiteMenuItemConfiguration</code>
     */
    HstSiteMenuConfiguration getHstSiteMenuConfiguration();
    
    /**
     * 
     * @return all direct child <code>SiteMenuItemConfiguration</code>'s of this item
     */
    List<HstSiteMenuItemConfiguration> getChildItemConfigurations();
    
    
    /**
     * 
     * @return the parent <code>HstSiteMenuItemConfiguration</code> and <code>null</code> is none exists (ie, it is a root)
     */
    HstSiteMenuItemConfiguration getParentItemConfiguration();
    
   
}
