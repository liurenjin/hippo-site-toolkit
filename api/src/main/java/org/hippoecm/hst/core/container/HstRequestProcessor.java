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
package org.hippoecm.hst.core.container;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Request processor. This request processor is called by HstComponent container servlet.
 * This request processor can be initialized and run by another web application and its own classloader.
 * So, the HstComponent container servlet or other components should not assume that this
 * request processor is loaded by the same classloader.
 */
public interface HstRequestProcessor {
    
    /**
     * processes request
     * 
     * @param requestContainerConfig the holder for the servletConfig and classloader of the HST container
     * @param servletRequest the servletRequest of the HST request
     * @param servletResponse the servletResponse of the HST response
     * @throws ContainerException
     */
    void processRequest(HstContainerConfig requestContainerConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException;
    
    /**
     * processes request
     * 
     * @param requestContainerConfig the holder for the servletConfig and classloader of the HST container
     * @param servletRequest the servletRequest of the HST request
     * @param servletResponse the servletResponse of the HST response
     * @param pathInfo the forced path info for the HST request processing
     * @throws ContainerException
     */
    void processRequest(HstContainerConfig requestContainerConfig, ServletRequest servletRequest, ServletResponse servletResponse, String pathInfo) throws ContainerException;
    
}
