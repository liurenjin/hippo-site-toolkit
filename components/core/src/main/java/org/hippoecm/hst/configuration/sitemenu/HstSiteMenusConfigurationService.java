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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstSiteMenusConfigurationService implements HstSiteMenusConfiguration {
    
    private static final Logger log = LoggerFactory.getLogger(HstSiteMenusConfigurationService.class);
    
    private HstSite hstSite;
    private Map<String, HstSiteMenuConfiguration> hstSiteMenuConfigurations = new HashMap<String, HstSiteMenuConfiguration>();
    private Map<String, List<HstSiteMenuItemConfiguration>> hstSiteMenuItemsBySiteMapId = new HashMap<String, List<HstSiteMenuItemConfiguration>>();
    
    public HstSiteMenusConfigurationService(HstSite hstSite, HstNode siteMenusNode) throws ServiceException{
        this.hstSite = hstSite;
        if (HstNodeTypes.NODETYPE_HST_SITEMENUS.equals(siteMenusNode.getNodeTypeName())) {
            for(HstNode siteMenu: siteMenusNode.getNodes()) {
                if(HstNodeTypes.NODETYPE_HST_SITEMENU.equals(siteMenu.getNodeTypeName())) {
                    try {
                        HstSiteMenuConfiguration hstSiteMenuConfiguration = new HstSiteMenuConfigurationService(this, siteMenu);
                        HstSiteMenuConfiguration old = hstSiteMenuConfigurations.put(hstSiteMenuConfiguration.getName(), hstSiteMenuConfiguration);
                        if(old != null) {
                            log.error("Duplicate name for HstSiteMenuConfiguration found. The first one is replaced");
                        }
                    } catch(ServiceException e) {
                        log.warn("Skipping HstSiteMenuConfiguration for '{}' because '{}'", siteMenu.getValueProvider().getPath(), e.toString());
                    }
                } else {
                    log.error("Skipping siteMenu '{}' because not of type '{}'", siteMenu.getValueProvider().getPath(), HstNodeTypes.NODETYPE_HST_SITEMENU);
                }
            }
        }
    }

    public void addHstSiteMenuItem(String hstSiteMapItemId, HstSiteMenuItemConfiguration hstSiteMenuItemConfiguration) {
        List<HstSiteMenuItemConfiguration> itemsForSiteMapItemId = hstSiteMenuItemsBySiteMapId.get(hstSiteMapItemId);
        if(itemsForSiteMapItemId == null) {
            itemsForSiteMapItemId = new ArrayList<HstSiteMenuItemConfiguration>();
            itemsForSiteMapItemId.add(hstSiteMenuItemConfiguration);
            hstSiteMenuItemsBySiteMapId.put(hstSiteMapItemId, itemsForSiteMapItemId);
        } else {
            itemsForSiteMapItemId.add(hstSiteMenuItemConfiguration);
        }
    }
    
    public HstSite getSite() {
        return this.hstSite;
    }

    public Map<String, HstSiteMenuConfiguration> getSiteMenuConfigurations() {
        return Collections.unmodifiableMap(hstSiteMenuConfigurations);
    }

    public HstSiteMenuConfiguration getSiteMenuConfiguration(String name) {
        return this.hstSiteMenuConfigurations.get(name);
    }


}
