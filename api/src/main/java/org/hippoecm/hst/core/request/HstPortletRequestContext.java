/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.hst.core.request;

import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * HstPortletRequestContext extends HstRequestContext to support porlet specific configuration and context.
 * 
 * @version $Id$
 */
public interface HstPortletRequestContext extends HstRequestContext {
    
    /**
     * Initializes the Portlet specific context
     * @param request
     * @param response
     */
    void initPortletContext(HttpServletRequest request, HttpServletResponse response);
    
    /**
     * Returns the PortletRequest serving the HstRequest.
     */
    PortletRequest getPortletRequest();

    /**
     * Returns the PortletResponse serving the HstResponse.
     */
    PortletResponse getPortletResponse();
}
