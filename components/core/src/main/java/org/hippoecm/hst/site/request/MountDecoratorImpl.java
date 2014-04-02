/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.configuration.channel.Blueprint;
import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.channel.ChannelException;
import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.configuration.channel.HstPropertyDefinition;
import org.hippoecm.hst.configuration.hosting.MatchException;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.PortMount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.internal.ContextualizableMount;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.internal.MountDecorator;
import org.hippoecm.hst.core.internal.MutableResolvedMount;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.request.ResolvedVirtualHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MountDecoratorImpl implements MountDecorator {

    protected final static Logger log = LoggerFactory.getLogger(MountDecoratorImpl.class);
    @Override
    public Mount decorateMountAsPreview(Mount mount) {
        if (mount instanceof MountAsPreviewDecorator) {
            log.debug("Already preview decorated mount '{}'. Return", mount.toString());
            return mount;
        }
        if(mount.isPreview()) {
            log.debug("Mount {} is already a preview mount. Still decorate the backing virtualhosts to preview", mount.toString());
        }
        return new MountAsPreviewDecorator(mount);
    }

    private class MountAsPreviewDecorator implements Mount {

        private Mount delegatee;
        private Mount parentAsPreview;
        private Map<String, Mount> childAsPreview = new HashMap<>();

        public MountAsPreviewDecorator(Mount delegatee) {
            this.delegatee = delegatee;
        }

        @Override
        public HstSite getHstSite() {
            if (delegatee.isPreview() || !(delegatee instanceof ContextualizableMount)) {
                return delegatee.getHstSite();
            }
            return ((ContextualizableMount)delegatee).getPreviewHstSite();
        }

        @Override
        public String getMountPoint() {
            return delegatee.getMountPoint();
        }

        @Override
        @Deprecated
        public String getCanonicalContentPath() {
            return delegatee.getContentPath();
        }

        @Override
        public String getContentPath() {
            return delegatee.getContentPath();
        }

        @Override
        public boolean isPreview() {
            return true;
        }

        @Override
        public Mount getParent() {
            if (delegatee.isPreview()) {
                return delegatee.getParent();
            }
            if (delegatee.getParent() == null) {
                return null;
            }
            parentAsPreview = new MountAsPreviewDecorator(delegatee.getParent());
            return parentAsPreview;
        }

        @Override
        public Mount getChildMount(String name) {
            if (delegatee.isPreview()) {
                return delegatee.getChildMount(name);
            }
            Mount child = childAsPreview.get(name);
            if (child != null) {
                return child;
            }
            if (delegatee.getChildMount(name) == null) {
                return null;
            }
            child = new MountAsPreviewDecorator(delegatee.getChildMount(name));
            childAsPreview.put(name, child);
            return child;
        }

        @Override
        public List<Mount> getChildMounts() {
            List<Mount> childMounts = delegatee.getChildMounts();
            List<Mount> previewChilds = new ArrayList<>();
            for(Mount child : childMounts) {
                previewChilds.add(getChildMount(child.getName()));
            }
            return Collections.unmodifiableList(previewChilds);
        }

        @Override
        public String getType() {
            if (delegatee.isPreview()) {
                return delegatee.getType();
            }
            if (Mount.LIVE_NAME.equals(delegatee.getType())) {
                return Mount.PREVIEW_NAME;
            } else {
                // not 'live' : just return same type
                return delegatee.getType();
            }
        }

        @Override
        public List<String> getTypes() {
            if (delegatee.isPreview()) {
                return delegatee.getTypes();
            }
            // immutable list
            List<String> types = delegatee.getTypes();
            String decoratedType = getType();
            String unDecoratedType = delegatee.getType();
            // postfix all delegate types with '-preview'
            List<String> decoratedTypes = new ArrayList<>();
            for (String type : types) {
                if (type.equals(unDecoratedType)) {
                    decoratedTypes.add(decoratedType);
                } else {
                    decoratedTypes.add(type + "-preview");
                }
            }
            return Collections.unmodifiableList(decoratedTypes);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends ChannelInfo> T getChannelInfo() {
            if(delegatee instanceof ContextualizableMount) {
                return (T) ((ContextualizableMount)delegatee).getPreviewChannelInfo();
            }
            return delegatee.getChannelInfo();
        }

        @Override
        public String getChannelPath() {
            HstSite site = getHstSite();
            if (site.hasPreviewConfiguration()) {
                return delegatee.getChannelPath() +"-preview";
            }
            return delegatee.getChannelPath();
        }

        /**
         * @return the repository path to the channel configuration node and <code>null</code> if not configured
         */
        @Override
        public Channel getChannel() {
            if(delegatee instanceof ContextualizableMount) {
                return  ((ContextualizableMount)delegatee).getPreviewChannel();
            }
            return delegatee.getChannel();
        }

        @Override
        public String getAlias() {
            return delegatee.getAlias();
        }

        @Override
        public String[] getDefaultSiteMapItemHandlerIds() {
            return delegatee.getDefaultSiteMapItemHandlerIds();
        }

        @Override
        public boolean isCacheable() {
            return false;
        }


        @Deprecated
        @Override
        public String getDefaultResourceBundleId() {
            return delegatee.getDefaultResourceBundleId();
        }

        @Override
        public String [] getDefaultResourceBundleIds() {
            return delegatee.getDefaultResourceBundleIds();
        }

        @Override
        public String getFormLoginPage() {
            return delegatee.getFormLoginPage();
        }

        @Override
        public String getHomePage() {
            return delegatee.getHomePage();
        }

        @Override
        public HstSiteMapMatcher getHstSiteMapMatcher() {
            return delegatee.getHstSiteMapMatcher();
        }

        @Override
        public String getIdentifier() {
            return delegatee.getIdentifier();
        }

        @Override
        public String getLocale() {
            return delegatee.getLocale();
        }

        @Override
        public String getMountPath() {
            return delegatee.getMountPath();
        }

        @Override
        public Map<String, String> getMountProperties() {
            return delegatee.getMountProperties();
        }

        @Override
        public String getName() {
            return delegatee.getName();
        }

        @Override
        public String getNamedPipeline() {
            return delegatee.getNamedPipeline();
        }

        @Override
        public String getPageNotFound() {
            return delegatee.getPageNotFound();
        }

        @Override
        public int getPort() {
            return delegatee.getPort();
        }

        @Override
        public String getProperty(String name) {
            return delegatee.getProperty(name);
        }

        @Override
        public String getParameter(String name) {
            return delegatee.getParameter(name);
        }

        @Override
        public Map<String, String> getParameters() {
            return delegatee.getParameters();
        }

        @Override
        public Set<String> getRoles() {
            return delegatee.getRoles();
        }

        @Override
        public String getScheme() {
            return delegatee.getScheme();
        }

        @Override
        public boolean isSchemeAgnostic() {
            return true;
        }

        @Override
        public boolean containsMultipleSchemes() {
            return false;
        }

        @Override
        public int getSchemeNotMatchingResponseCode() {
            // TODO Test this, I think preview just return ok
            return HttpServletResponse.SC_OK;
        }

        @Override
        public Set<String> getUsers() {
            return delegatee.getUsers();
        }

        @Override
        public VirtualHost getVirtualHost() {
            return new VirtualHostAsPreviewDecorator(delegatee.getVirtualHost());
        }

        @Override
        public boolean isAuthenticated() {
            return delegatee.isAuthenticated();
        }

        @Override
        public boolean isContextPathInUrl() {
            return delegatee.isContextPathInUrl();
        }

        @Override
        public boolean isMapped() {
            return delegatee.isMapped();
        }

        @Override
        public boolean isOfType(String type) {
            return delegatee.isOfType(type);
        }

        @Override
        public boolean isPortInUrl() {
            return delegatee.isPortInUrl();
        }

        @Override
        public boolean isSessionStateful() {
            return delegatee.isSessionStateful();
        }

        @Override
        public boolean isSite() {
            return delegatee.isSite();
        }

        @Override
        public boolean isSubjectBasedSession() {
            return delegatee.isSubjectBasedSession();
        }

        @Override
        public boolean isVersionInPreviewHeader() {
            return delegatee.isVersionInPreviewHeader();
        }

        @Override
        public String onlyForContextPath() {
            return delegatee.onlyForContextPath();
        }

        @Override
        public String getCmsLocation() {
            return delegatee.getCmsLocation();
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("MountAsPreviewDecorator for Mount [");
            builder.append(delegatee.toString());
            builder.append("]");
            return  builder.toString();
        }

    }

    private class VirtualHostAsPreviewDecorator implements VirtualHost {

        private VirtualHost delegatee;

        private VirtualHostAsPreviewDecorator(VirtualHost delegatee) {
            this.delegatee = delegatee;
        }

        @Override
        public String getHostName() {
            return delegatee.getHostName();
        }

        @Override
        public String getName() {
            return delegatee.getName();
        }

        @Override
        public String getHostGroupName() {
            return delegatee.getHostGroupName();
        }

        @Override
        public String getLocale() {
            return delegatee.getLocale();
        }

        @Override
        public VirtualHost getChildHost(final String name) {
            VirtualHost child = delegatee.getChildHost(name);
            if (child == null) {
                return null;
            }
            return new VirtualHostAsPreviewDecorator(child);
        }

        @Override
        public List<VirtualHost> getChildHosts() {
            final List<VirtualHost> childHosts = delegatee.getChildHosts();
            if (childHosts == null) {
                return null;
            }
            List<VirtualHost> decoratedChildren = new ArrayList<>();
            for (VirtualHost childHost : childHosts) {
                decoratedChildren.add(new VirtualHostAsPreviewDecorator(childHost));
            }
            return decoratedChildren;
        }

        @Override
        public PortMount getPortMount(final int portNumber) {
            PortMount portMount = delegatee.getPortMount(portNumber);
            if (portMount == null) {
                return null;
            }
            return new PortMountAsPreviewDecorator(portMount);
        }

        @Override
        public VirtualHosts getVirtualHosts() {
            return new VirtualHostsAsPreviewDecorator(delegatee.getVirtualHosts());
        }

        @Override
        public boolean isContextPathInUrl() {
            return delegatee.isContextPathInUrl();
        }

        @Override
        public String onlyForContextPath() {
            return delegatee.onlyForContextPath();
        }

        @Override
        public boolean isPortInUrl() {
            return delegatee.isPortInUrl();
        }

        @Override
        public String getScheme() {
            return delegatee.getScheme();
        }

        @Override
        public boolean isSchemeAgnostic() {
            return delegatee.isSchemeAgnostic();
        }

        @Override
        public int getSchemeNotMatchingResponseCode() {
            return delegatee.getSchemeNotMatchingResponseCode();
        }

        @Override
        public String getHomePage() {
            return delegatee.getHomePage();
        }

        @Override
        public String getBaseURL(final HttpServletRequest request) {
            return delegatee.getBaseURL(request);
        }

        @Override
        public String getPageNotFound() {
            return delegatee.getPageNotFound();
        }

        @Override
        public boolean isVersionInPreviewHeader() {
            return delegatee.isVersionInPreviewHeader();
        }

        @Override
        public boolean isCacheable() {
            return delegatee.isCacheable();
        }

        @Override
        @Deprecated
        public String getDefaultResourceBundleId() {
            return delegatee.getDefaultResourceBundleId();
        }

        @Override
        public String[] getDefaultResourceBundleIds() {
            return delegatee.getDefaultResourceBundleIds();
        }

        @Override
        public boolean isCustomHttpsSupported() {
            return delegatee.isCustomHttpsSupported();
        }
    }

    private class PortMountAsPreviewDecorator implements PortMount {

        private PortMount delegatee;

        private PortMountAsPreviewDecorator(PortMount delegatee) {
            this.delegatee = delegatee;
        }
        @Override
        public int getPortNumber() {
            return delegatee.getPortNumber();
        }

        @Override
        public Mount getRootMount() {
            final Mount rootMount = delegatee.getRootMount();
            return new MountAsPreviewDecorator(rootMount);
        }
    }

    private class VirtualHostsAsPreviewDecorator implements VirtualHosts {
        private VirtualHosts delegatee;

        private VirtualHostsAsPreviewDecorator(VirtualHosts delegatee) {
            this.delegatee = delegatee;
        }

        @Override
        @Deprecated
        public HstManager getHstManager() {
            return delegatee.getHstManager();
        }

        @Override
        public boolean isExcluded(final String pathInfo) {
            return delegatee.isExcluded(pathInfo);
        }

        @Deprecated
        @Override
        public ResolvedSiteMapItem matchSiteMapItem(final HstContainerURL hstContainerURL) throws MatchException {
            // don't delegate the matching of mount since we need a decorated!
            ResolvedMount decoratedMount = matchMount(hstContainerURL.getHostName(), hstContainerURL.getContextPath(), hstContainerURL.getRequestPath());
            ResolvedSiteMapItem resolvedSiteMapItem =  decoratedMount.matchSiteMapItem(hstContainerURL.getPathInfo());
            return resolvedSiteMapItem;
        }

        @Override
        public ResolvedMount matchMount(final String hostName, final String contextPath, final String requestPath) throws MatchException {
            final ResolvedMount resolvedMount = delegatee.matchMount(hostName, contextPath, requestPath);
            if (!(resolvedMount instanceof MutableResolvedMount)) {
                String msg = String.format("Resolved mount '%s' expected to be a MutableResolvedMount but was not.",
                        resolvedMount.getMount().toString());
                throw new IllegalStateException(msg);
            }
            ((MutableResolvedMount)resolvedMount).setMount(new MountAsPreviewDecorator(resolvedMount.getMount()));
            return resolvedMount;
        }

        @Override
        public ResolvedVirtualHost matchVirtualHost(final String hostName) throws MatchException {
            // TODO should be decorated? FOR NOW RETURN not suppored
            throw new UnsupportedOperationException("matchVirtualHost is not allowed for decorated virtualhosts");
        }

        @Override
        public String getDefaultHostName() {
            return delegatee.getDefaultHostName();
        }

        @Override
        public boolean isContextPathInUrl() {
            return delegatee.isContextPathInUrl();
        }

        @Override
        public String getDefaultContextPath() {
            return delegatee.getDefaultContextPath();
        }

        @Override
        public boolean isPortInUrl() {
            return delegatee.isPortInUrl();
        }

        @Override
        public String getLocale() {
            return delegatee.getLocale();
        }

        @Override
        public Mount getMountByGroupAliasAndType(final String hostGroupName, final String alias, final String type) {
            if (Mount.PREVIEW_NAME.equals(type)) {
                final Mount mountByGroupAliasAndType = delegatee.getMountByGroupAliasAndType(hostGroupName, alias, type);
                if (mountByGroupAliasAndType != null) {
                    // explicit preview found
                    return mountByGroupAliasAndType;
                }
                // check whether there is a 'live' variant. If so, return that one decorated as preview mount
                final Mount liveMount = delegatee.getMountByGroupAliasAndType(hostGroupName, alias, Mount.LIVE_NAME);
                return new MountAsPreviewDecorator(liveMount);
            }
            return null;  
        }

        @Override
        public List<Mount> getMountsByHostGroup(final String hostGroupName) {
            final List<Mount> mountsByHostGroup = delegatee.getMountsByHostGroup(hostGroupName);
            List<Mount> previewMounts = new ArrayList<>();
            for (Mount mount : mountsByHostGroup) {
                 previewMounts.add(new MountAsPreviewDecorator(mount));
            }
            return previewMounts;
        }

        @Override
        public List<String> getHostGroupNames() {
            return delegatee.getHostGroupNames();
        }

        @Override
        public Mount getMountByIdentifier(final String uuid) {
            final Mount mountByIdentifier = delegatee.getMountByIdentifier(uuid);
            return new MountAsPreviewDecorator(mountByIdentifier);
        }

        @Override
        public String getCmsPreviewPrefix() {
            return delegatee.getCmsPreviewPrefix();
        }

        @Override
        public String getChannelManagerHostGroupName() {
            return delegatee.getChannelManagerHostGroupName();
        }

        @Override
        public String getChannelManagerSitesName() {
            return delegatee.getChannelManagerSitesName();
        }

        @Override
        public boolean isDiagnosticsEnabled(final String ip) {
            return delegatee.isDiagnosticsEnabled(ip);
        }

        @Override
        public String getDefaultResourceBundleId() {
            return delegatee.getDefaultResourceBundleId();
        }

        @Override
        public String[] getDefaultResourceBundleIds() {
            return delegatee.getDefaultResourceBundleIds();
        }

        @Override
        public boolean isChannelMngrSiteAuthenticationSkipped() {
            return delegatee.isChannelMngrSiteAuthenticationSkipped();
        }

        @Override
        public Map<String, Channel> getChannels() {
            // TODO Should channel be decorated?
            return delegatee.getChannels();
        }

        @Override
        public Channel getChannelByJcrPath(final String channelPath) {
            // TODO Should channel be decorated?
            return delegatee.getChannelByJcrPath(channelPath);
        }

        @Override
        public Channel getChannelById(final String id) {
            // TODO Should channel be decorated?
            return delegatee.getChannelById(id);
        }

        @Override
        public List<Blueprint> getBlueprints() {
            return delegatee.getBlueprints();
        }

        @Override
        public Blueprint getBlueprint(final String id) {
            return delegatee.getBlueprint(id);
        }

        @Override
        public Class<? extends ChannelInfo> getChannelInfoClass(final Channel channel) throws ChannelException {
            return delegatee.getChannelInfoClass(channel);
        }

        @Override
        public Class<? extends ChannelInfo> getChannelInfoClass(final String id) throws ChannelException {
            return delegatee.getChannelInfoClass(id);
        }

        @Override
        public <T extends ChannelInfo> T getChannelInfo(final Channel channel) throws ChannelException {
            return delegatee.getChannelInfo(channel);
        }

        @Override
        public ResourceBundle getResourceBundle(final Channel channel, final Locale locale) {
            return delegatee.getResourceBundle(channel, locale);
        }

        @Override
        public List<HstPropertyDefinition> getPropertyDefinitions(final Channel channel) {
            return delegatee.getPropertyDefinitions(channel);
        }

        @Override
        public List<HstPropertyDefinition> getPropertyDefinitions(final String channelId) {
            return delegatee.getPropertyDefinitions(channelId);
        }
    }
}
