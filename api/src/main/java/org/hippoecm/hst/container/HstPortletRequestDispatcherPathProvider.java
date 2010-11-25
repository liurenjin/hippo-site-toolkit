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
package org.hippoecm.hst.container;

import javax.portlet.PortletConfig;
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;

/**
 * Path provider interface for <CODE>HstContainerPortlet</CODE>.
 * <P>
 * <CODE>HstContainerPortlet</CODE> should dispatch to some url paths of <CODE>HstContainerServlet</CODE>.
 * The url paths consist of servlet path and path info. Because the servlet path could be used to decide
 * whether the request is preview site or live site, <CODE>HstContainerPortlet</CODE> should invoke {@link #getServletPath(PortletRequest)}
 * method to find the servlet path. 
 * </P>
 * 
 * @version $Id$
 */
public interface HstPortletRequestDispatcherPathProvider {
    
    void init(PortletConfig config) throws PortletException;
    
    String getServletPath(PortletRequest request) throws PortletException;
    
    void destroy();
    
}
