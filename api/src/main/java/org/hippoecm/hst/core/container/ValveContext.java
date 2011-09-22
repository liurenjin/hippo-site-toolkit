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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.request.HstRequestContext;


/**
 * Context information during invoking valves in a pipeline.
 * This holds the necessary objects to serve a request.
 * 
 * @version $Id$
 */
public interface ValveContext
{
    
    /**
     * Requests invocation of next possible valve.
     * @throws ContainerException
     */
    void invokeNext() throws ContainerException;
    
    /**
     * Returns the HstComponent container configuration.
     * 
     * @return
     */
    HstContainerConfig getRequestContainerConfig();
    
    /**
     * Returns the current request context.
     * 
     * @return
     */
    HstRequestContext getRequestContext();

    /**
     * Returns the current servlet request.
     * 
     * @return
     */
    HttpServletRequest getServletRequest();

    /**
     * Returns the current servlet response.
     * 
     * @return
     */
    HttpServletResponse getServletResponse();
    
    /**
     * Sets the root {@link HstComponentWindow} instancethat is used to process the current request.
     * 
     * @param rootComponentWindow
     */
    void setRootComponentWindow(HstComponentWindow rootComponentWindow);
    
    /**
     * Returns the root {@link HstComponentWindow} instance that is used to process the current request.
     * 
     * @return
     */
    HstComponentWindow getRootComponentWindow();

    
    /**
     * Sets the root {@link HstComponentWindow} instance to *render* the current request
     * 
     * @param rootComponentRenderingWindow
     */
    void setRootComponentRenderingWindow(HstComponentWindow rootComponentRenderingWindow);
    
    /**
     * <p>
     * Returns the root {@link HstComponentWindow} instance to *render* the current request. By default, this 
     * returns the same {@link HstComponentWindow} as {@link #getRootComponentWindow()}, unless it is explicitly 
     * set to a different {@link HstComponentWindow}. For example in case when you want to render only a single (or subtree)
     * {@link HstComponent} of an entire {@link HstComponent} hierarchy.
     * </p>
     * <p>
     * The rootComponentRenderingWindow must always be a descendant of or the same as {@link #getRootComponentWindow()}
     * </p>
     * @return
     */
    HstComponentWindow getRootComponentRenderingWindow();
    
}
