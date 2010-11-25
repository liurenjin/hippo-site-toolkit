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
package org.hippoecm.hst.core.sitemenu;

import java.io.Serializable;
import java.util.List;

/**
 * The interface for a SiteMenu implementation, containing possibly a tree of {@link HstSiteMenuItem}'s
 *
 */
public interface HstSiteMenu extends Serializable{
    /**
     * Returns the name of this SiteMenu. For example, you could have a "topmenu", "leftmenu" and "footermenu" on your site/portal,
     * where these names might be appropriate 
     * @return the name of this SiteMenu
     */
    String getName();
    
    /**
     * Based on the request, the implementation should be able to indicate whether this HstSiteMenu is expanded
     * 
     * @return <code>true</code> when any HstSiteMenuItem in this HstSiteMenu container is selected
     */
    boolean isExpanded();
    
    /**
     * Returns the currently expanded sitemenu items.
     * 
     * @return the currently expanded sitemenu items. 
     */
    List<HstSiteMenuItem> getExpandedSiteMenuItems();
    
    /**
     * @return returns all direct child {@link HstSiteMenuItem}'s of this SiteMenu
     */
    List<HstSiteMenuItem> getSiteMenuItems();
    
    /**
     * 
     * @return the <code>HstSiteMenus</code> container for this HstSiteMenu
     */
    HstSiteMenus getHstSiteMenus();
}
