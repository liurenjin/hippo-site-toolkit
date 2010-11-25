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

import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.portlet.BaseURL;
import javax.portlet.MimeResponse;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.portlet.ResourceURL;

import org.hippoecm.hst.container.HstContainerPortlet;
import org.hippoecm.hst.core.request.HstPortletRequestContext;
import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * HstContainerURLProviderPortletImpl
 * 
 * @version $Id$
 */
public class HstContainerURLProviderPortletImpl extends AbstractHstContainerURLProvider {
    
    protected boolean portletResourceURLEnabled;
    
    public void setPortletResourceURLEnabled(boolean portletResourceURLEnabled) {
        this.portletResourceURLEnabled = portletResourceURLEnabled;
    }
    
    public String toURLString(HstContainerURL containerURL, HstRequestContext requestContext) throws UnsupportedEncodingException, ContainerException {
        return toURLString(containerURL, requestContext, null);
    }
    
    public String toURLString(HstContainerURL containerURL, HstRequestContext requestContext, String contextPath) throws UnsupportedEncodingException, ContainerException {
        String urlString = "";
        
        HstPortletRequestContext prc = (HstPortletRequestContext)requestContext;
        String lifecycle = (String)prc.getPortletRequest().getAttribute(PortletRequest.LIFECYCLE_PHASE);
        
        if (PortletRequest.RESOURCE_PHASE.equals(lifecycle) || PortletRequest.RENDER_PHASE.equals(lifecycle)) {
            String resourceWindowReferenceNamespace = containerURL.getResourceWindowReferenceNamespace();
            boolean hstContainerResource = !portletResourceURLEnabled && ContainerConstants.CONTAINER_REFERENCE_NAMESPACE.equals(resourceWindowReferenceNamespace);
            
            StringBuilder path = new StringBuilder(100);
            
            if (hstContainerResource) {
                String pathInfo = null;
                String oldPathInfo = containerURL.getPathInfo();
                String resourcePath = containerURL.getResourceId();
                Map<String, String[]> oldParamMap = containerURL.getParameterMap();
                
                try {
                    containerURL.setResourceWindowReferenceNamespace(null);
                    ((HstContainerURLImpl) containerURL).setPathInfo(resourcePath);
                    ((HstContainerURLImpl) containerURL).setParameters(null);
                    pathInfo = buildHstURLPath(containerURL);
                } finally {
                    containerURL.setResourceWindowReferenceNamespace(resourceWindowReferenceNamespace);
                    ((HstContainerURLImpl) containerURL).setPathInfo(oldPathInfo);
                    ((HstContainerURLImpl) containerURL).setParameters(oldParamMap);
                }
                
                path.append(contextPath != null ? contextPath : getVirtualizedContextPath(containerURL, requestContext, pathInfo));
                path.append(getVirtualizedServletPath(containerURL, requestContext, pathInfo));
                path.append(pathInfo);
                urlString = path.toString();
                
            } else {
                path.append(containerURL.getServletPath());
                path.append(buildHstURLPath(containerURL));
                
                BaseURL url = null;
                PortletResponse response = prc.getPortletResponse();
                
                MimeResponse mimeResponse = (MimeResponse) response;
                
                if (containerURL.getActionWindowReferenceNamespace() != null) {
                    url = mimeResponse.createActionURL();
                    url.setParameter(HstContainerPortlet.HST_PATH_PARAM_NAME, path.toString());
                } else if (resourceWindowReferenceNamespace != null) {
                    url = mimeResponse.createResourceURL();
                    ((ResourceURL) url).setResourceID(path.toString());
                } else {
                    url = mimeResponse.createRenderURL();
                    url.setParameter(HstContainerPortlet.HST_PATH_PARAM_NAME, path.toString());
                }
                urlString = url.toString();
            }
        } else {
            // should not be allowed to come here: throw IllegalStateException?
        } 
        
        return urlString;
    }
}
