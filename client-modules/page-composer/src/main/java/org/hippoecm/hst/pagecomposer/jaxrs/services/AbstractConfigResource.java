/*
 *  Copyright 2010-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.util.concurrent.Callable;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.ArrayUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ComponentManagerAware;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.api.RuntimeExceptionEvent;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ExtResponseRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validators.Validator;
import org.hippoecm.hst.pagecomposer.jaxrs.util.HstConfigurationUtils;
import org.hippoecm.repository.api.HippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractConfigResource implements ComponentManagerAware {

    private static Logger log = LoggerFactory.getLogger(AbstractConfigResource.class);

    private PageComposerContextService pageComposerContextService;
    private ComponentManager componentManager;

    public void setPageComposerContextService(PageComposerContextService pageComposerContextService) {
        this.pageComposerContextService = pageComposerContextService;
    }

    @Override
    public void setComponentManager(ComponentManager componentManager) {
        this.componentManager = componentManager;
    }

    public PageComposerContextService getPageComposerContextService() {
        return pageComposerContextService;
    }

    protected Response ok(String msg) {
        return ok(msg, ArrayUtils.EMPTY_STRING_ARRAY);
    }

    protected Response ok(String msg, Object data) {
        ExtResponseRepresentation entity = new ExtResponseRepresentation(data);
        entity.setMessage(msg);
        entity.setSuccess(true);
        return Response.ok().entity(entity).build();
    }

    protected Response error(String msg) {
        return error(msg, ArrayUtils.EMPTY_STRING_ARRAY);
    }

    protected Response error(String msg, Object data) {
        ExtResponseRepresentation entity = new ExtResponseRepresentation(data);
        entity.setMessage(msg);
        entity.setSuccess(false);
        return Response.serverError().entity(entity).build();
    }

    protected Response clientError(String msg, Object data) {
        ExtResponseRepresentation entity = new ExtResponseRepresentation(data);
        entity.setMessage(msg);
        entity.setSuccess(false);
        return Response.status(Response.Status.BAD_REQUEST).entity(entity).build();
    }

    protected Response created(String msg) {
        ExtResponseRepresentation entity = new ExtResponseRepresentation();
        entity.setMessage(msg);
        entity.setSuccess(true);
        return Response.status(Response.Status.CREATED).entity(entity).build();
    }

    protected Response conflict(String msg) {
        ExtResponseRepresentation entity = new ExtResponseRepresentation();
        entity.setMessage(msg);
        entity.setSuccess(false);
        return Response.status(Response.Status.CONFLICT).entity(entity).build();
    }

    protected ObjectConverter getObjectConverter(HstRequestContext requestContext) {
        return requestContext.getContentBeansTool().getObjectConverter();
    }


    protected Response tryGet(final Callable<Response> callable) {
        try {
            return callable.call();
        } catch (ClientException e) {
            resetSession();
            return logAndReturnClientError(e);
        } catch (Exception e) {
            resetSession();
            return logAndReturnServerError(e);
        }
    }

    protected Response tryExecute(final Callable<Response> callable,
                                  final Validator preValidator) {
        try {

            createMandatoryWorkspaceNodesIfMissing();

            final HstRequestContext requestContext = getPageComposerContextService().getRequestContext();
            preValidator.validate(requestContext);

            final Response response = callable.call();

            final Session session = requestContext.getSession();
            if (session.hasPendingChanges()) {
                HstConfigurationUtils.persistChanges(session);
            }
            return response;
        } catch (ClientException e) {
            resetSession();
            return logAndReturnClientError(e);
        } catch (Exception e) {
            resetSession();
            return logAndReturnServerError(e);
        }
    }

    protected void publishSynchronousEvent(final RuntimeExceptionEvent event) throws ClientException {
        componentManager.publishEvent(event);
        if (event.getException() != null) {
            throw event.getException();
        }
    }


    private void createMandatoryWorkspaceNodesIfMissing() throws RepositoryException {
        final String liveConfigPath = getPageComposerContextService().getEditingLiveConfigurationPath();
        final String previewConfigPath = getPageComposerContextService().getEditingPreviewConfigurationPath();
        HstConfigurationUtils.createMandatoryWorkspaceNodesIfMissing(liveConfigPath, getPageComposerContextService().getRequestContext().getSession());
        HstConfigurationUtils.createMandatoryWorkspaceNodesIfMissing(previewConfigPath, getPageComposerContextService().getRequestContext().getSession());
    }

    protected void resetSession() {
        final HstRequestContext requestContext = getPageComposerContextService().getRequestContext();
        if (requestContext != null) {
            try {
                final Session session = requestContext.getSession();
                if (session.hasPendingChanges()) {
                    if (session instanceof HippoSession) {
                        ((HippoSession) session).localRefresh();
                    } else {
                        session.refresh(false);
                    }
                }
            } catch (RepositoryException e) {
                log.error("RepositoryException while resetting session", e);
            }
        }
    }

    protected Response logAndReturnServerError(Exception e) {
        if (log.isDebugEnabled()) {
            log.warn(e.toString(), e);
        } else {
            log.warn(e.toString());
        }
        return error(e.getMessage());
    }

    protected Response logAndReturnClientError(ClientException e) {
        final String formattedMessage = e.getMessage();
        if (log.isDebugEnabled()) {
            log.info(formattedMessage, e);
        } else {
            log.info(formattedMessage);
        }
        final ExtResponseRepresentation entity = new ExtResponseRepresentation();
        entity.setSuccess(false);
        entity.setMessage(e.toString());
        entity.setErrorCode(e.getError().name());
        entity.setData(e.getParameterMap());
        return Response.status(Response.Status.BAD_REQUEST).entity(entity).build();
    }

    protected CanonicalInfo getCanonicalInfo(final Object o) throws IllegalStateException {
        if (o instanceof CanonicalInfo) {
            return (CanonicalInfo) o;
        }
        throw new IllegalStateException("HstSiteMenuItemConfiguration not instanceof CanonicalInfo");
    }

    protected String getPreviewConfigurationPath() {
        return getPageComposerContextService().getEditingPreviewConfigurationPath();
    }

    protected String getPreviewConfigurationWorkspacePath() {
        return getPreviewConfigurationPath() + "/" + HstNodeTypes.NODENAME_HST_WORKSPACE;
    }

    protected String getPreviewConfigurationWorkspacePath(final String mountId) {
        final Mount mount = pageComposerContextService.getRequestContext().getVirtualHost().getVirtualHosts().getMountByIdentifier(mountId);
        if (mount == null || !mount.getHstSite().hasPreviewConfiguration()) {
            final String msg = String.format("Cannot find for id '%s' or the mount exists but does not have a preview configuration.", mountId);
            throw new IllegalArgumentException(msg);
        }
        final String configurationPath = mount.getHstSite().getConfigurationPath();
        return configurationPath + "/" + HstNodeTypes.NODENAME_HST_WORKSPACE;
    }

}
