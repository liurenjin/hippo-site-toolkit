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

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.core.request.MatchedMapping;
import org.hippoecm.hst.site.request.HstRequestContextImpl;

public class SiteResolvingValve extends AbstractValve {
    
    @Override
    public void invoke(ValveContext context) throws ContainerException {
        HstRequestContextImpl requestContext = (HstRequestContextImpl) context.getServletRequest().getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
        HstContainerURL baseURL = requestContext.getBaseURL();

        MatchedMapping matchedMapping = requestContext.getMatchedMapping();
        
        if (matchedMapping == null) {
            String hostName = context.getServletRequest().getServerName();
            
            if (this.virtualHostsManager.getVirtualHosts() == null) {
                throw new ContainerException("Hosts are not properly initialized");
            }
            
            matchedMapping = this.virtualHostsManager.getVirtualHosts().findMapping(hostName, baseURL.getServletPath() + baseURL.getPathInfo());
            
            if (matchedMapping == null) {
                throw new ContainerException("No proper configuration found for host : " + hostName);
            }
            
            requestContext.setMatchedMapping(matchedMapping);
        }
        
        if (StringUtils.isEmpty(matchedMapping.getSiteName())) {
            throw new ContainerException("No siteName found for matchedMapping. Configure one in your virtual hosting.");
        }
        
        // continue
        context.invokeNext();
    }
    
}
