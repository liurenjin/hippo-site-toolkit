/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.hosting;

import java.util.List;

import org.hippoecm.hst.configuration.channel.ChannelManager;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.request.ResolvedVirtualHost;


/**
 * The container interface for {@link VirtualHost}
 * 
 */
public interface VirtualHosts {

    String DEFAULT_SCHEME = "http";

    /**
     * @return the {@link HstManager} for this VirtualHosts object
     */
    HstManager getHstManager();
    
    /**
     * 
     * Typically, some paths we do not want to be handle by the hst framework request processing. Typically, this would
     * be for example paths starting with /binaries/, or paths ending with some extension, like .pdf
     * 
     * When a path must be excluded, this method return true.
     *
     * @param pathInfo
     * @return true when the path must be excluded for matching to a host.
     */
    boolean isExcluded(String pathInfo);
    
    /**
     * <p>This method tries to match a hstContainerURL to a flyweight {@link ResolvedSiteMapItem}. It does so, by first trying to match the 
     * correct {@link ResolvedVirtualHost}. If it does find a {@link ResolvedVirtualHost}, the match is delegated to
     * {@link ResolvedVirtualHost#matchSiteMount(HstContainerURL)}, which returns the {@link ResolvedMount}. This object
     * delegates to {@link ResolvedMount#matchSiteMapItem(String)} which in the end returns the {@link ResolvedSiteMapItem}. If somewhere
     * in the chain a match cannot be made a MatchException exception is thrown
     * </p>
     * 
     * @param request the HttpServletRequest
     * @return the resolvedSiteMapItem for this request
     * @throws MatchException when the matching cannot be done, for example because no valid virtual hosts are configured or when the request path does not match 
     * a sitemap item
     */
    ResolvedSiteMapItem matchSiteMapItem(HstContainerURL hstContainerURL) throws MatchException;

    
    /**
     * <p>This method tries to match a hostName, contextPath and requestPath to a flyweight {@link ResolvedMount}. It does so, by first trying to match the 
     * correct {@link ResolvedVirtualHost}. If it does find a {@link ResolvedVirtualHost}, the match is delegated to
     * {@link ResolvedVirtualHost#matchMount(String, String)}, which returns the {@link ResolvedMount}. If somewhere
     * in the chain a match cannot be made, <code>null</code> will be returned. The contextPath will only be of influence in the matching
     * when the SiteMount has a non-empty value for {@link Mount#onlyForContextPath()}. If {@link Mount#onlyForContextPath()} is <code>null</code> or empty,
     * the <code>contextPath</code> is ignored for matching.
     * </p>
     * @param hostName
     * @param contextPath the contextPath of the request
     * @param requestPath
     * @return the {@link ResolvedMount} for this hstContainerUrl or <code>null</code> when it can not be matched to a {@link Mount}
     * @throws MatchException
     */
    ResolvedMount matchMount(String hostName, String contextPath,  String requestPath) throws MatchException;
    
    /**
     * <p>
     *  This method tries to match a request to a flyweight {@link ResolvedVirtualHost}
     * </p>
     * @param hostName 
     * @return the resolvedVirtualHost for this hostName or <code>null</code> when it can not be matched to a virtualHost
     * @throws MatchException
     */
    ResolvedVirtualHost matchVirtualHost(String hostName) throws MatchException;
 
    /**
     * @return the hostname that is configured as default, or <code>null</code> if none is configured as default.
     */
    String getDefaultHostName();
    
    /**
     * This is the global setting for every {@link VirtualHost} / {@link Mount} whether contextPath should be in the URL or not
     * @return <code>true</code> when the created url should have the contextPath in it
     */
    boolean isContextPathInUrl();
    
    /**
     * For external calls like the CMS REST api, an external app needs to know what context path to use. Through this getter,
     * the default context path can be retrieved. If not configured, <code>null</code> is returned and the external app must know
     * the context path. If configured, the contextPath is either an empty string, or it has to start with a "/" and is not allowed to have any other "/".
     * @return the default context path for the webapps or <code>null</code> when not configured
     */
    String getDefaultContextPath();
    
    /**
     * This is the global setting for every {@link VirtualHost} / {@link Mount} whether the port number should be in the URL or not
     * @return <code>true</code> when the created url should have the port number in it
     */
    boolean isPortInUrl();
    
    /**
     * @return the locale of this VirtualHosts object or <code>null</code> if no locale is configured
     */
    String getLocale();
    
    /**
     * Returns the {@link Mount} for this <code>hostGroupName</code>, <code>alias<code> and <code>type<code> having {@link Mount#getType()} equal to <code>type</code>. Returns <code>null</code> when no match
     * 
     * @param hostGroupName
     * @param alias the alias the mount must have
     * @param type  the type (for example preview, live, composer) the siteMount must have. 
     * @return the {@link Mount} for this <code>hostGroupName</code>, <code>alias<code> and <code>type<code> having {@link Mount#getType()} equal to <code>type</code>. Returns <code>null</code> when no match
     */
    Mount getMountByGroupAliasAndType(String hostGroupName, String alias, String type);
    
    /**
     * @param hostGroupName
     * @return the List<{@link Mount}> belonging to <code>hostGroupName</code> or <code>null</code> when there are no {@link Mount} for <code>hostGroupName</code>
     */
    List<Mount> getMountsByHostGroup(String hostGroupName);
    
    /**
     * @return return the list of all hostGroupNames
     */
    List<String> getHostGroupNames();
    
    /**
     * 
     * @param uuid
     * @return
     */
    Mount getMountByIdentifier(String uuid);
    
    /**
     * The cmsPreviewPrefix will never start or end with a slash and will never be <code>null</code>
     * @return the configured cmsPreviewPrefix with leading and trailing slashes removed. It will never be <code>null</code>. If configured
     * to be empty, it will be ""
     */
     String getCmsPreviewPrefix();
     
     /**
      * Returns the virtual host group node name in use for the current environment. The {@link ChannelManager} will only be able to create links 
      * for channels that are reflected in the hostGroup 
      *
      * @return the virtual host group node name used for this {@link VirtualHosts}. If not configured it returns <code>null</code>
      */
     String getChannelManagerHostGroupName();
     
     /**
      * @return the node name of the hst:sites that will be managed by the {@link ChannelManager}. If not configured it returns <code>hst:sites</code>
      */
     String getChannelManagerSitesName();

    /**
     * @return <code>true</code> when diagnostics about request processing is enabled for the client IP address.
     * If <code>ip</code> is <code>null</code>, then the <code>ip</code> address of the request won't be taken into account
     * to determine whether or not the diagnostics is enabled.
     */
    boolean isDiagnosticsEnabled(String ip);

    /**
     * @return default resource bundle for all sites to use, for example org.example.resources.MyResources, or <code>null</code>
     * when not configured
     */
    String getDefaultResourceBundleId();
}
