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

import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * HstSitePipeline
 * 
 * @version $Id$
 */
public class HstSitePipeline implements Pipeline
{

    protected Valve [] preInvokingValves;
    protected Valve [] invokingValves;
    protected Valve [] postInvokingValves;
    
    public HstSitePipeline() throws Exception
    {
    }

    public void setPreInvokingValves(Valve [] preInvokingValves) {
        if (preInvokingValves == null) {
            this.preInvokingValves = null;
        } else {
            this.preInvokingValves = new Valve[preInvokingValves.length];
            System.arraycopy(preInvokingValves, 0, this.preInvokingValves, 0, preInvokingValves.length);
        }
    }
    
    public void addPreInvokingValve(Valve preInvokingValve) {
        preInvokingValves = add(preInvokingValves, preInvokingValve);
    }
    
    public void setInvokingValves(Valve [] invokingValves) {
        if (invokingValves == null) {
            this.invokingValves = null;
        } else {
            this.invokingValves = new Valve[invokingValves.length];
            System.arraycopy(invokingValves, 0, this.invokingValves, 0, invokingValves.length);
        }
    }
    
    public void addInvokingValve(Valve invokingValve) {
        invokingValves = add(invokingValves, invokingValve);
    }
    
    public void setPostInvokingValves(Valve [] postInvokingValves) {
        if (postInvokingValves == null) {
            this.postInvokingValves = null;
        } else {
            this.postInvokingValves = new Valve[postInvokingValves.length];
            System.arraycopy(postInvokingValves, 0, this.postInvokingValves, 0, postInvokingValves.length);
        }
    }
    
    public void addPostInvokingValve(Valve postInvokingValve) {
        postInvokingValves = add(postInvokingValves, postInvokingValve);
    }

    private Valve[] add(Valve[] valves, Valve valve) {
        if(valve == null) {
            return valves;
        }
        Valve[] newValves;
        if (valves == null) {
            newValves = new Valve[1];
            newValves[0] = valve;
        } else {
            newValves =  new Valve[valves.length +1];
            System.arraycopy(valves, 0, newValves, 0, valves.length);
            newValves[invokingValves.length] = valve;
        }
        return newValves;
    }
    
    public void initialize() throws ContainerException {
    }
    
    public void beforeInvoke(HstContainerConfig requestContainerConfig, HstRequestContext requestContext, HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws ContainerException {
        invokeValves(requestContainerConfig, requestContext, servletRequest, servletResponse, preInvokingValves);
    }

    public void invoke(HstContainerConfig requestContainerConfig, HstRequestContext requestContext, HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws ContainerException {
        invokeValves(requestContainerConfig, requestContext, servletRequest, servletResponse, invokingValves);
    }
    
    public void afterInvoke(HstContainerConfig requestContainerConfig, HstRequestContext requestContext, HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws ContainerException {
        invokeValves(requestContainerConfig, requestContext, servletRequest, servletResponse, postInvokingValves);
    }
    
    private void invokeValves(HstContainerConfig requestContainerConfig, HstRequestContext requestContext, HttpServletRequest servletRequest, HttpServletResponse servletResponse, Valve [] valves) throws ContainerException {
        if (valves != null && valves.length > 0) {
            new Invocation(requestContainerConfig, requestContext, servletRequest, servletResponse, valves).invokeNext();
        }
    }

    public void destroy() throws ContainerException {
    }    
    
    private static final class Invocation implements ValveContext
    {

        private final Valve[] valves;

        private final HstContainerConfig requestContainerConfig;
        private final HttpServletRequest servletRequest;
        private final HttpServletResponse servletResponse;
        private HstComponentWindow rootComponentWindow;
        private final HstRequestContext requestContext;

        private int at = 0;

        public Invocation(HstContainerConfig requestContainerConfig, HstRequestContext requestContext, HttpServletRequest servletRequest, HttpServletResponse servletResponse, Valve[] valves) {
            this.requestContainerConfig = requestContainerConfig;
            this.requestContext = requestContext;
            this.servletRequest = servletRequest;
            this.servletResponse = servletResponse;
            this.valves = valves;
        }

        public void invokeNext() throws ContainerException {
            if (at < valves.length)
            {
                Valve next = valves[at];
                at++;
                next.invoke(this);
            }
        }

        public HstContainerConfig getRequestContainerConfig() {
            return this.requestContainerConfig;
        }
        
        public HstRequestContext getRequestContext() {
        	return this.requestContext;
        }
        public HttpServletRequest getServletRequest() {
            return this.servletRequest;
        }

        public HttpServletResponse getServletResponse() {
            return this.servletResponse;
        }

        public void setRootComponentWindow(HstComponentWindow rootComponentWindow) {
            this.rootComponentWindow = rootComponentWindow;
        }
        
        public HstComponentWindow getRootComponentWindow() {
            return this.rootComponentWindow;
        }
    }
}
