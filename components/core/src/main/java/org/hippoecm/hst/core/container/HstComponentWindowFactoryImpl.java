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

import java.util.Map;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.request.HstRequestContext;

public class HstComponentWindowFactoryImpl implements HstComponentWindowFactory {
    
    protected String referenceNameSeparator = "_";
    
    public void setReferenceNameSeparator(String referenceNameSeparator) {
        this.referenceNameSeparator = referenceNameSeparator;
    }
    
    public String getReferenceNameSeparator() {
        return this.referenceNameSeparator;
    }

    public HstComponentWindow create(HstContainerConfig requestContainerConfig, HstRequestContext requestContext, HstComponentConfiguration compConfig, HstComponentFactory compFactory) throws HstComponentException {
        return create(requestContainerConfig, requestContext, compConfig, compFactory, null);
    }
    
    public HstComponentWindow create(HstContainerConfig requestContainerConfig, HstRequestContext requestContext, HstComponentConfiguration compConfig, HstComponentFactory compFactory, HstComponentWindow parentWindow) throws HstComponentException {
        
        String name = compConfig.getName();
        String referenceName = compConfig.getReferenceName();
        StringBuilder referenceNamespaceBuilder = new StringBuilder();

        if (parentWindow == null) {
            String contextNamespace = requestContext.getContextNamespace();
            
            if (contextNamespace != null) {
                referenceNamespaceBuilder.append(contextNamespace);
            }            
        } else {
            String parentReferenceNamespace = parentWindow.getReferenceNamespace();
            
            if ("".equals(parentReferenceNamespace)) {
                referenceNamespaceBuilder.append(referenceName);
            } else {
                referenceNamespaceBuilder.append(parentReferenceNamespace).append(this.referenceNameSeparator).append(referenceName);
            }
        }

        String referenceNamespace = referenceNamespaceBuilder.toString();
        
        String renderPath = compConfig.getRenderPath();
        
        if (renderPath != null && !renderPath.startsWith("/")) {
            renderPath = new StringBuilder(renderPath.length() + 1).append('/').append(renderPath).toString();
        }
        
        String serveResourcePath = compConfig.getServeResourcePath();
        
        if (serveResourcePath != null && !serveResourcePath.startsWith("/")) {
            serveResourcePath = new StringBuilder(serveResourcePath.length() + 1).append('/').append(serveResourcePath).toString();
        }
        
        HstComponent component = null;
        HstComponentException componentFactoryException = null;
        
        try {
            component = compFactory.getComponentInstance(requestContainerConfig, compConfig);
        } catch (Throwable th) {
            componentFactoryException = new HstComponentException(th.getMessage());
        }
        
        HstComponentWindowImpl window = 
            new HstComponentWindowImpl(
                name,
                referenceName, 
                referenceNamespace, 
                component, 
                renderPath, 
                serveResourcePath,
                parentWindow);
        
        if (componentFactoryException != null) {
            window.addComponentExcpetion(componentFactoryException);
        }
        
        Map<String, HstComponentConfiguration> childCompConfigMap = compConfig.getChildren();
        
        if (childCompConfigMap != null && !childCompConfigMap.isEmpty()) {
            for (Map.Entry<String, HstComponentConfiguration> entry : childCompConfigMap.entrySet()) {
                HstComponentConfiguration childCompConfig = entry.getValue();
                HstComponentWindow childCompWindow = create(requestContainerConfig, requestContext, childCompConfig, compFactory, window);
                window.addChildWindow(childCompWindow);
            }
        }
        
        return window;
    }

}
