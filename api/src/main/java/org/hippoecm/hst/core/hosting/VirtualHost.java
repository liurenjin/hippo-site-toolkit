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
    
    
}
