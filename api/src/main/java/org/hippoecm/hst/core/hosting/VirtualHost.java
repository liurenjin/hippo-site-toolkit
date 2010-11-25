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
package org.hippoecm.hst.core.hosting;

import javax.servlet.http.HttpServletRequest;

/**
 * VirtualHost which holds the mapping between host (server name) and site name.
 * 
 */
public interface VirtualHost {

    /**
     * Returns the site name for the host if one is configured. If there is no siteName configured, the siteName must be part of the requestUri.
     * 
     * @return the siteName belonging to this virtual host, or <code>null</code>
     */
    String getSiteName();
    
    /**
     * 
     * @param name the name segment of the hostname
     * @return the child <code>VirtualHost</code> or <code>null</code> if none found
     */
    VirtualHost getChildHost(String name);
    
    
    /**
     * 
     * @param pathInfo
     * @return return the best <code>{@link Mapping}</code> within this VirtualHost for the pathInfo
     */
    Mapping getMapping(String pathInfo);
    
    /**
     * 
     * @return the <code>VirtualHosts</code> container of this <code>VirtualHost</code>
     */
    VirtualHosts getVirtualHosts();
    
    /**
     * 
     * @return <code>true</code> when the created url should have the contextpath in it
     */
    boolean isContextPathInUrl();
    
    /**
     * 
     * @return <code>true</code> when an externalized (starting with http/https) should contain a port number
     */
    boolean isPortVisible();

    /**
     * 
     * @return when {@link #isPortVisible()} returns <code>true</code> this method returns the port number of the externalized url
     */
    int getPortNumber();
    
    /**
     * @return the protocol to use for creating external urls, for example http / https
     */
    String getProtocol();
    
    
    /**
     * Returns the base of the <code>URL</code> as seen by for example a browser. The base URL is consists of <code>protocol + hostname + portnumber</code>
     * for example 'http://www.hippoecm.org:8081' 
     * 
     * The protocol is 'http' by default, unless {@link # getProtocol()} returns something else 
     * The hostname is the HttpServeltRequest request.getServerName() (proxies must have <code>ProxyPreserveHost On</code>)
     * The portnumber is as follows: 
     * <ul>
     *   <li>when {@link #isPortVisible()} is <code>false</code>, there is no portnumber</li>
     *   <li>otherwise: 
     *       <ul>
     *          <li><code>port = {@link #getPortNumber()}</code></li>
     *          <li><code>if (port == 0) {port = request.getServerPort()}</code></li>
     *          <li>if(port == 80 && "http".equals(protocol)) || (port == 443 && "https".equals(protocol)): no portnumber will be in baseUrl
     *       </ul>
     *   </li>
     * </ul>  
     * 
     * @param request the HttpServletRequest
     * @return the <code>URL</code> until the context path, thus <code>protocol + hostname + portnumber</code>, for example 'http://www.hippoecm.org:8081' 
     */
    
    String getBaseURL(HttpServletRequest request);

}
