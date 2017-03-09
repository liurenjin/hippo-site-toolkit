/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.site;

import java.util.HashMap;
import java.util.Map;

import org.hippoecm.hst.configuration.cache.HstNodeLoadingCache;
import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.configuration.model.ModelLoadingException;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemapitemhandlers.HstSiteMapItemHandlersConfiguration;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenusConfiguration;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.linking.LocationMapTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstSiteFactory {

    private static final Logger log = LoggerFactory.getLogger(HstSiteFactory.class);

    public HstSite createLiveSiteService(final HstNode site,
                                                final MountSiteMapConfiguration mountSiteMapConfiguration,
                                                final HstNodeLoadingCache hstNodeLoadingCache) throws ModelLoadingException {
        return createSiteService(site, mountSiteMapConfiguration, hstNodeLoadingCache, false);
    }


    public HstSite createPreviewSiteService(final HstNode site,
                                                   final MountSiteMapConfiguration mountSiteMapConfiguration,
                                                   final HstNodeLoadingCache hstNodeLoadingCache) throws ModelLoadingException {
        return createSiteService(site, mountSiteMapConfiguration, hstNodeLoadingCache, true);
    }


    private HstSite createSiteService(final HstNode site,
                                      final MountSiteMapConfiguration mountSiteMapConfiguration,
                                      final HstNodeLoadingCache hstNodeLoadingCache,
                                      final boolean isPreviewSite) {
        HstSiteService defaultHstSite = new HstSiteService(site, mountSiteMapConfiguration, hstNodeLoadingCache, isPreviewSite);
        if (mountSiteMapConfiguration.getCampaigns() == null || mountSiteMapConfiguration.getCampaigns().length == 0) {
            return defaultHstSite;
        }

        Map<String, HstSite> campaignsMapping = new HashMap<>();
        for (String campaign : mountSiteMapConfiguration.getCampaigns()) {
            // TODO catch model loading exception for separate campaigns?
            campaignsMapping.put(campaign, new HstSiteService(site, mountSiteMapConfiguration, hstNodeLoadingCache, campaign, isPreviewSite));
        }

        return new CompositeHstSite(defaultHstSite, campaignsMapping, mountSiteMapConfiguration.getActiveCampaign());
    }

    public class CompositeHstSite implements HstSite {

        private final HstSiteService defaultHstSite;
        private final Map<String, HstSite> campaignsMapping;
        private String activeCampaign;

        public CompositeHstSite(final HstSiteService defaultHstSite, final Map<String, HstSite> campaignsMapping,
                                final String activeCampaign) {
            this.defaultHstSite = defaultHstSite;
            this.campaignsMapping = campaignsMapping;
            this.activeCampaign = activeCampaign;
        }

        public HstSite getActiveHstSite() {
            if (activeCampaign == null) {
                return defaultHstSite;
            }
            HstSite activeHstSite = campaignsMapping.get(activeCampaign);
            if (activeHstSite == null) {
                log.warn("Active campaign '{}' does not exist. Fall back to non-campaign site");
                return defaultHstSite;
            }
            log.debug("Using active campaign '{}'", activeHstSite.getName());
            return activeHstSite;
        }

        @Override
        public String getName() {
            return getActiveHstSite().getName();
        }

        @Override
        public String getCanonicalIdentifier() {
            return getActiveHstSite().getCanonicalIdentifier();
        }

        @Override
        public HstSiteMapItemHandlersConfiguration getSiteMapItemHandlersConfiguration() {
            return getActiveHstSite().getSiteMapItemHandlersConfiguration();
        }

        @Override
        public HstComponentsConfiguration getComponentsConfiguration() {
            return getActiveHstSite().getComponentsConfiguration();
        }

        @Override
        public HstSiteMap getSiteMap() {
            return getActiveHstSite().getSiteMap();
        }

        @Override
        public LocationMapTree getLocationMapTree() {
            return getActiveHstSite().getLocationMapTree();
        }

        @Override
        public LocationMapTree getLocationMapTreeComponentDocuments() {
            return getActiveHstSite().getLocationMapTreeComponentDocuments();
        }

        @Override
        public HstSiteMenusConfiguration getSiteMenusConfiguration() {
            return getActiveHstSite().getSiteMenusConfiguration();
        }

        @Override
        public String getConfigurationPath() {
            return getActiveHstSite().getConfigurationPath();
        }

        @Override
        @Deprecated
        public long getVersion() {
            return getActiveHstSite().getVersion();
        }

        @Override
        public boolean hasPreviewConfiguration() {
            return getActiveHstSite().hasPreviewConfiguration();
        }
    }
}
