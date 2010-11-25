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

import java.util.ArrayList;
import java.util.List;

import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuConfiguration;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuItemConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;

public class HstSiteMenuImpl implements HstSiteMenu {

    private static final long serialVersionUID = 1L;

    private String name;
    private HstSiteMenus hstSiteMenus;
    private List<HstSiteMenuItem> hstSiteMenuItems = new ArrayList<HstSiteMenuItem>();
    private List<HstSiteMenuItem> expandedHstSiteMenuItems = new ArrayList<HstSiteMenuItem>();
    private boolean expanded;
    

    public HstSiteMenuImpl(HstSiteMenus hstSiteMenus, HstSiteMenuConfiguration siteMenuConfiguration, HstRequestContext hstRequestContext) {
        this.hstSiteMenus = hstSiteMenus;
        this.name = siteMenuConfiguration.getName();
        for(HstSiteMenuItemConfiguration hstSiteMenuItemConfiguration : siteMenuConfiguration.getSiteMenuConfigurationItems()) {
            hstSiteMenuItems.add(new HstSiteMenuItemImpl(this, null, hstSiteMenuItemConfiguration , hstRequestContext));
        }
    }

    public String getName() {
        return this.name;
    }

    public List<HstSiteMenuItem> getSiteMenuItems() {
        return hstSiteMenuItems;
    }

    public boolean isExpanded() {
        return expanded;
    }
    
    public HstSiteMenus getHstSiteMenus() {
        return this.hstSiteMenus;
    }

    public List<HstSiteMenuItem> getExpandedSiteMenuItems() {
        return expandedHstSiteMenuItems;
    }
    
    
    public void addExpandedSiteMenuItem(HstSiteMenuItem siteMenuItem){
        this.expanded = true;
        if(!expandedHstSiteMenuItems.contains(siteMenuItem)) {
            expandedHstSiteMenuItems.add(siteMenuItem);
        }
    }

}
