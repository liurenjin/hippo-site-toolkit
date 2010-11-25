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

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;

/**
 * ContextResolvingValve
 * 
 * @version $Id$
 */
public class ContextResolvingValve extends AbstractValve
{
    
    @Override
    public void invoke(ValveContext context) throws ContainerException
    {
        HstMutableRequestContext requestContext = (HstMutableRequestContext) context.getRequestContext();

        ResolvedSiteMapItem resolvedSiteMapItem = requestContext.getResolvedSiteMapItem();
        
        if (resolvedSiteMapItem == null) {
            // if there is no ResolvedSiteMapItem on the request we cannot continue
            throw new ContainerException("No resolvedSiteMapItem found for this request. Cannot continue request processing");
        }
        
        HstComponentConfiguration rootComponentConfig = resolvedSiteMapItem.getHstComponentConfiguration();
        
        if (!requestContext.isEmbeddedRequest() && requestContext.isPortletContext() && resolvedSiteMapItem.getPortletHstComponentConfiguration() != null) {
        	rootComponentConfig = resolvedSiteMapItem.getPortletHstComponentConfiguration();
        }
        
        if (rootComponentConfig == null) {
        	// TODO: log ResolvedSiteMapItem.getQualifiedId() as reference)
            throw new ContainerNotFoundException("Resolved siteMapItem does not contain a ComponentConfiguration that can be resolved.");
        }
        
        String targetComponentPath = requestContext.getTargetComponentPath();
        if (targetComponentPath != null) {
        	HstComponentConfiguration hcc = rootComponentConfig;
            for ( String pathName : targetComponentPath.split("/"))
            {
                hcc = hcc.getChildByName(pathName);
                if (hcc == null)
                {
                    break;
                }
            }
            if (hcc == null) {
            	// TODO: log ResolvedSiteMapItem.getQualifiedId() as reference or better rootComponentConfig.getQualifiedId())
            	throw new ContainerNotFoundException("Cannot find target child component configuration '"+targetComponentPath+"'");
            }
            rootComponentConfig = hcc;
            
            if (requestContext.isEmbeddedRequest()) {
                // build and set the embedded component reference contextName needed proper parameter encoding and resolving
                StringBuilder contextNamespaceBuilder = new StringBuilder();
                String referenceNameSeparator = getComponentWindowFactory().getReferenceNameSeparator();
                if (hcc != resolvedSiteMapItem.getHstComponentConfiguration()) {
                    do {
                        contextNamespaceBuilder.insert(0, hcc.getReferenceName());
                        hcc = hcc.getParent();
                        if (hcc == resolvedSiteMapItem.getHstComponentConfiguration()) {
                            break;
                        }
                        contextNamespaceBuilder.insert(0, referenceNameSeparator);
                    } while (true);
                }
                requestContext.setContextNamespace(contextNamespaceBuilder.toString());
            }            
        }
        
        try {
            HstComponentWindow rootComponentWindow = getComponentWindowFactory().create(context.getRequestContainerConfig(), requestContext, rootComponentConfig, getComponentFactory());
            context.setRootComponentWindow(rootComponentWindow);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to create component windows: {}", e.toString(), e);
            } else if (log.isWarnEnabled()) {
                log.warn("Failed to create component windows: {}", e.toString());
            }
            
            throw new ContainerException("Failed to create component window for the configuration: " + rootComponentConfig.getId(), e);
        }
        
        // continue
        context.invokeNext();
    }
}
