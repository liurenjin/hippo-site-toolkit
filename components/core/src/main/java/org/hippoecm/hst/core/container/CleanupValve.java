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

import java.util.List;

import javax.servlet.ServletRequest;

import org.hippoecm.hst.core.ResourceLifecycleManagement;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * CleanupValve
 * 
 * @version $Id$
 */
public class CleanupValve extends AbstractValve
{
    protected List<ResourceLifecycleManagement> resourceLifecycleManagements;
    
    public void setResourceLifecycleManagements(List<ResourceLifecycleManagement> resourceLifecycleManagements) {
        this.resourceLifecycleManagements = resourceLifecycleManagements;
    }
    
    @Override
    public void invoke(ValveContext context) throws ContainerException
    {
        if (this.resourceLifecycleManagements != null) {
            for (ResourceLifecycleManagement resourceLifecycleManagement : this.resourceLifecycleManagements) {
                resourceLifecycleManagement.disposeAllResources();
            }
        }
        
        ServletRequest servletRequest = context.getServletRequest();
        HstRequestContext requestContext = context.getRequestContext();
        
        if (requestContext != null) {
        	// ensure Session isn't tried to reuse again (its closed anyway)
            ((HstMutableRequestContext) requestContext).setSession(null);
            
            if (servletRequest.getAttribute(ContainerConstants.HST_FORWARD_PATH_INFO) == null) {
                getRequestContextComponent().release(requestContext);
            }
        }
        
        // continue
        context.invokeNext();
    }
}
