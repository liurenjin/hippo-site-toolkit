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

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstRequestImpl;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.component.HstResponseImpl;
import org.hippoecm.hst.core.component.HstResponseState;
import org.hippoecm.hst.core.component.HstServletResponseState;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.site.HstServices;

/**
 * AggregationValve
 * 
 * @version $Id$
 */
public class AggregationValve extends AbstractValve {
    
    @Override
    public void invoke(ValveContext context) throws ContainerException {

        HttpServletRequest servletRequest = (HttpServletRequest) context.getServletRequest();
        HttpServletResponse servletResponse = (HttpServletResponse) context.getServletResponse();
        HstRequestContext requestContext = (HstRequestContext) servletRequest.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
        boolean isDevelopmentMode = getContainerConfiguration().isDevelopmentMode();
        
        if (!context.getServletResponse().isCommitted() && requestContext.getBaseURL().getResourceWindowReferenceNamespace() == null) {
            HstComponentWindow rootWindow = context.getRootComponentWindow();
            
            if (rootWindow != null) {
                
                Map<HstComponentWindow, HstRequest> requestMap = new HashMap<HstComponentWindow, HstRequest>();
                Map<HstComponentWindow, HstResponse> responseMap = new HashMap<HstComponentWindow, HstResponse>();

                ServletRequest parentRequest = servletRequest;
                ServletResponse parentResponse = servletResponse;
                
                HstResponse topParentResponse = null;
                
                // Check if it is invoked from portlet.
                HstResponseState portletHstResponseState = (HstResponseState) servletRequest.getAttribute(HstResponseState.class.getName());
                
                if (portletHstResponseState != null) {
                    parentResponse = new HstResponseImpl((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse, requestContext, null, portletHstResponseState, null);
                    topParentResponse = (HstResponse) parentResponse;
                }
                
                // make hstRequest and hstResponse for each component window.
                // note that hstResponse is hierarchically created.
                createHstRequestResponseForWindows(rootWindow, requestContext, parentRequest, parentResponse,
                        requestMap, responseMap, topParentResponse);
                
                // to avoid recursive invocation from now, just make a list by hierarchical order.
                List<HstComponentWindow> sortedComponentWindowList = new LinkedList<HstComponentWindow>();
                sortComponentWindowsByHierarchy(rootWindow, sortedComponentWindowList);
                
                // create and add a window for trace tool
                HstComponentWindow traceToolWindow = null;
                if (isDevelopmentMode && !sortedComponentWindowList.isEmpty()) {
                    HstComponentWindow lastChildWindow = sortedComponentWindowList.get(sortedComponentWindowList.size() - 1);
                    traceToolWindow = createTraceToolComponent(context, requestContext, lastChildWindow);
                    
                    if (traceToolWindow != null) {
                        ((HstComponentWindowImpl) lastChildWindow).addChildWindow(traceToolWindow);
                        createHstRequestResponseForWindows(traceToolWindow, requestContext, 
                                requestMap.get(lastChildWindow), responseMap.get(lastChildWindow), 
                                requestMap, responseMap, topParentResponse);
                        sortedComponentWindowList.add(traceToolWindow);
                    }
                }
                
                HstComponentWindow [] sortedComponentWindows = sortedComponentWindowList.toArray(new HstComponentWindow[sortedComponentWindowList.size()]);
                
                HstContainerConfig requestContainerConfig = context.getRequestContainerConfig();
                // process doBeforeRender() of each component as sorted order, parent first.
                processWindowsBeforeRender(requestContainerConfig, rootWindow, sortedComponentWindows, requestMap, responseMap);
                
                // add the hst-version as a response header if we are in preview:
                if(Boolean.TRUE == requestContext.getAttribute(ContainerConstants.IS_PREVIEW)) {
                    rootWindow.getResponseState().addHeader("HST-VERSION", HstServices.getImplementationVersion());
                }
                
                // check if it's requested to forward.
                String forwardPathInfo = rootWindow.getResponseState().getForwardPathInfo();
                
                // page error handling...
                PageErrors pageErrors = getPageErrors(sortedComponentWindows, true);
                if (pageErrors != null) {
                    PageErrorHandler.Status handled = handleComponentExceptions(pageErrors, requestContainerConfig, rootWindow, requestMap.get(rootWindow), responseMap.get(rootWindow));
                    forwardPathInfo = rootWindow.getResponseState().getForwardPathInfo();
                    if (handled == PageErrorHandler.Status.HANDLED_TO_STOP && forwardPathInfo == null) {
                        context.invokeNext();
                        return;
                    }
                }
                
                // check if any invocation on sendError() exists...
                HstComponentWindow errorCodeSendingWindow = findErrorCodeSendingWindow(sortedComponentWindows);
                
                if (errorCodeSendingWindow != null) {
                    
                    try {
                        int errorCode = errorCodeSendingWindow.getResponseState().getErrorCode();
                        String errorMessage = errorCodeSendingWindow.getResponseState().getErrorMessage();
                        String componentClassName = errorCodeSendingWindow.getComponentInfo().getComponentClassName();
                        
                        if (log.isDebugEnabled()) {
                            log.debug("The component window has error status code, {} from {}.", errorCode, componentClassName);
                        }
                        
                        if (errorMessage != null) {
                            servletResponse.sendError(errorCode, errorMessage);
                        } else {
                            servletResponse.sendError(errorCode);
                        }
                    } catch (IOException e) {
                        if (log.isDebugEnabled()) {
                            log.warn("Exception invocation on sendError().", e);
                        } else if (log.isWarnEnabled()) {
                            log.warn("Exception invocation on sendError(). {}", e.toString());
                        }
                    }
                    
                } else if (forwardPathInfo != null) {
                    
                    servletRequest.setAttribute(ContainerConstants.HST_FORWARD_PATH_INFO, forwardPathInfo);
                    
                } else {
                    
                    // process doRender() of each component as reversed sort order, child first.
                    processWindowsRender(requestContainerConfig, sortedComponentWindows, requestMap, responseMap, isDevelopmentMode, traceToolWindow);
                    
                    // page error handling...
                    pageErrors = getPageErrors(sortedComponentWindows, true);
                    if (pageErrors != null) {
                        PageErrorHandler.Status handled = handleComponentExceptions(pageErrors, requestContainerConfig, rootWindow, requestMap.get(rootWindow), responseMap.get(rootWindow));
                        if (handled == PageErrorHandler.Status.HANDLED_TO_STOP) {
                            // just ignore because we don't support forward or redirect during rendering...
                        }
                    }
                    
                    try {
                        // flush root component window content.
                        // note that the child component's contents are already flushed into the root component's response state.
                        rootWindow.getResponseState().flush();
                    } catch (Exception e) {
                        if (log.isDebugEnabled()) {
                            log.warn("Exception during flushing the response state.", e);
                        } else if (log.isWarnEnabled()) {
                            log.warn("Exception during flushing the response state.");
                        }
                    }
                    
                }
            }
        }
        
        // continue
        context.invokeNext();
    }

    protected void createHstRequestResponseForWindows(
            final HstComponentWindow window,
            final HstRequestContext requestContext, 
            final ServletRequest servletRequest,
            final ServletResponse servletResponse, 
            final Map<HstComponentWindow, HstRequest> requestMap,
            final Map<HstComponentWindow, HstResponse> responseMap,
            HstResponse topComponentHstResponse) {

        HstRequest request = new HstRequestImpl((HttpServletRequest) servletRequest, requestContext, window, HstRequest.RENDER_PHASE);
        HstResponseState responseState = new HstServletResponseState((HttpServletRequest) servletRequest,
                (HttpServletResponse) servletResponse);
        HstResponse response = new HstResponseImpl((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse, requestContext, window,
                responseState, topComponentHstResponse);
        
        if (topComponentHstResponse == null) {
            topComponentHstResponse = response;
        }

        requestMap.put(window, request);
        responseMap.put(window, response);

        ((HstComponentWindowImpl) window).setResponseState(responseState);

        Map<String, HstComponentWindow> childWindowMap = window.getChildWindowMap();

        if (childWindowMap != null) {
            for (Map.Entry<String, HstComponentWindow> entry : childWindowMap.entrySet()) {
                createHstRequestResponseForWindows(entry.getValue(), requestContext, servletRequest, response,
                        requestMap, responseMap, topComponentHstResponse);
            }
        }
    }

    protected void sortComponentWindowsByHierarchy(
            final HstComponentWindow window, 
            final List<HstComponentWindow> sortedWindowList) {
        
        sortedWindowList.add(window);

        Map<String, HstComponentWindow> childWindowMap = window.getChildWindowMap();

        if (childWindowMap != null) {
            for (Map.Entry<String, HstComponentWindow> entry : childWindowMap.entrySet()) {
                sortComponentWindowsByHierarchy(entry.getValue(), sortedWindowList);
            }
        }
    }
    
    protected void processWindowsBeforeRender(
            final HstContainerConfig requestContainerConfig, 
            final HstComponentWindow rootWindow,
            final HstComponentWindow [] sortedComponentWindows,
            final Map<HstComponentWindow, HstRequest> requestMap, 
            final Map<HstComponentWindow, HstResponse> responseMap)
            throws ContainerException {

        for (int i = 0; i < sortedComponentWindows.length; i++) {
            HstComponentWindow window = sortedComponentWindows[i];
            HstRequest request = requestMap.get(window);
            HstResponse response = responseMap.get(window);
            getComponentInvoker().invokeBeforeRender(requestContainerConfig, request, response);
            
            if (rootWindow.getResponseState().getForwardPathInfo() != null) {
                break;
            }
        }

    }
    
    protected void processWindowsRender(
            final HstContainerConfig requestContainerConfig, 
            final HstComponentWindow [] sortedComponentWindows,
            final Map<HstComponentWindow, HstRequest> requestMap, 
            final Map<HstComponentWindow, HstResponse> responseMap,
            final boolean isDevelopmentMode,
            final HstComponentWindow traceToolWindow)
            throws ContainerException {

        if (isDevelopmentMode) {
            boolean traceToolWindowFlushed = false;
    
            for (int i = sortedComponentWindows.length - 1; i >= 0; i--) {
                HstComponentWindow window = sortedComponentWindows[i];
                HstRequest request = requestMap.get(window);
                HstResponse response = responseMap.get(window);

                if (window == traceToolWindow) {
                    try {
                        getComponentInvoker().invokeRender(requestContainerConfig, request, response);
                    } catch (Exception e) {
                        if (log.isDebugEnabled()) {
                            log.warn("Failed to render tracetool.", e);
                        } else if (log.isWarnEnabled()) {
                            log.warn("Failed to render tracetool. {}", e.toString());
                        }
                    }
                } else {
                    getComponentInvoker().invokeRender(requestContainerConfig, request, response);
                    
                    if (isDevelopmentMode && !traceToolWindowFlushed) {
                        try {
                            traceToolWindowFlushed = true;
                            if (traceToolWindow != null) {
                                traceToolWindow.getResponseState().flush();
                            }
                        } catch (Exception e) {
                            if (log.isDebugEnabled()) {
                                log.warn("Exception during flushing the traceToolWindow's response state.", e);
                            } else if (log.isWarnEnabled()) {
                                log.warn("Exception during flushing the traceToolWindow's response state. {}", e.toString());
                            }                    
                        }
                    }
                }
            }
        } else {
            for (int i = sortedComponentWindows.length - 1; i >= 0; i--) {
                HstComponentWindow window = sortedComponentWindows[i];
                HstRequest request = requestMap.get(window);
                HstResponse response = responseMap.get(window);
                getComponentInvoker().invokeRender(requestContainerConfig, request, response);
            }
        }
    }
    
}
