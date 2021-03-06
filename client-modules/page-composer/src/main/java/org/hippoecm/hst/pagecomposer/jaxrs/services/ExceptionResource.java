/*
 *  Copyright 2010-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.services;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.transport.servlet.ServletController;
import org.hippoecm.hst.jaxrs.cxf.CXFJaxrsService;
import org.hippoecm.hst.pagecomposer.jaxrs.cxf.CXFJaxrsHstConfigService;

/**
 * The hst:exception is a 'fake' resource which gets invoked by the {@link CXFJaxrsHstConfigService} in case an exception happens
 * before it can create a new wrapper {@link HttpServletRequest} to be invoked by the {@link ServletController#invoke(HttpServletRequest, HttpServletResponse)} in 
 * {@link CXFJaxrsService#invoke(org.hippoecm.hst.core.request.HstRequestContext, HttpServletRequest, HttpServletResponse)}
 */

@Path("/hst:exception/")
public class ExceptionResource extends AbstractConfigResource {

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response exceptionGet(@Context HttpServletRequest servletRequest) {
        return error(servletRequest.getAttribute(CXFJaxrsHstConfigService.REQUEST_ERROR_MESSAGE_ATTRIBUTE).toString());
    }

    @HEAD
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response exceptionHead(@Context HttpServletRequest servletRequest) {
        return error(servletRequest.getAttribute(CXFJaxrsHstConfigService.REQUEST_ERROR_MESSAGE_ATTRIBUTE).toString());
    }

    @POST
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response exceptionPost(@Context HttpServletRequest servletRequest) {
        return error(servletRequest.getAttribute(CXFJaxrsHstConfigService.REQUEST_ERROR_MESSAGE_ATTRIBUTE).toString());
    }

    @PUT
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response exceptionPut(@Context HttpServletRequest servletRequest) {
        return error(servletRequest.getAttribute(CXFJaxrsHstConfigService.REQUEST_ERROR_MESSAGE_ATTRIBUTE).toString());
    }

    @DELETE
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response exceptionDelete(@Context HttpServletRequest servletRequest) {
        return error(servletRequest.getAttribute(CXFJaxrsHstConfigService.REQUEST_ERROR_MESSAGE_ATTRIBUTE).toString());
    }

    
}
