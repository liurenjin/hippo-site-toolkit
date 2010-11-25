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
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.MethodUtils;
import org.hippoecm.hst.container.HstContainerPortlet;
import org.hippoecm.hst.container.HstContainerPortletContext;

public class HstContainerURLProviderPortletImpl extends AbstractHstContainerURLProvider {
    
    protected boolean portletResourceURLEnabled;
    
    public void setPortletResourceURLEnabled(boolean portletResourceURLEnabled) {
        this.portletResourceURLEnabled = portletResourceURLEnabled;
    }
    
    @Override
    public String toURLString(HstContainerURL containerURL) throws UnsupportedEncodingException, ContainerException {
        StringBuilder path = new StringBuilder(100);
        String pathInfo = buildHstURLPath(containerURL);
        path.append(containerURL.getServletPath());
        path.append(pathInfo);
        
        if (!this.portletResourceURLEnabled && containerURL.getResourceWindowReferenceNamespace() != null) {
            path.insert(0, containerURL.getContextPath());
            return path.toString();
        }
        
        Object portletURL = null;
        Object response = HstContainerPortletContext.getCurrentResponse();
        
        try {
            if (containerURL.getActionWindowReferenceNamespace() != null) {
                portletURL = MethodUtils.invokeMethod(response, "createActionURL", null);
            } else if (containerURL.getResourceWindowReferenceNamespace() != null) {
                portletURL = MethodUtils.invokeMethod(response, "createResourceURL", null);
            } else {
                portletURL = MethodUtils.invokeMethod(response, "createRenderURL", null);
            }
        
            MethodUtils.invokeMethod(portletURL, "setParameter", new Object [] { HstContainerPortlet.HST_PATH_PARAM_NAME, path.toString() });
        } catch (NoSuchMethodException e) {
            throw new ContainerException("Portlet support is not available.", e);
        } catch (IllegalAccessException e) {
            throw new ContainerException("Portlet support is not available.", e);
        } catch (InvocationTargetException e) {
            throw new ContainerException("Portlet support is not available.", e);
        }
        
        return portletURL.toString();
    }
    
}
