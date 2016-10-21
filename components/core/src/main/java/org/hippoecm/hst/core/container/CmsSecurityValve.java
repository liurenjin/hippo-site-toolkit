/*
 *  Copyright 2011-2016 Hippo B.V. (http://www.onehippo.com)
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
import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.jcr.SessionSecurityDelegation;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.HstRequestUtils;
import org.onehippo.cms7.services.cmscontext.CmsContextService;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;
import org.onehippo.cms7.services.HippoServiceRegistry;

import static org.hippoecm.hst.core.container.ContainerConstants.CMS_REQUEST_REPO_CREDS_ATTR;
import static org.hippoecm.hst.core.container.ContainerConstants.CMS_REQUEST_USER_ID_ATTR;
import static org.hippoecm.hst.core.container.ContainerConstants.CMS_SSO_AUTHENTICATED;
import static org.hippoecm.hst.core.container.ContainerConstants.CMS_SSO_REPO_CREDS_ATTR_NAME;
import static org.hippoecm.hst.core.container.ContainerConstants.CMS_USER_ID_ATTR;

/**
 * <p>
 * CmsSecurityValve responsible for authenticating the user using CMS.
 * </p>
 * <p> This valve check if the CMS has provided encrypted credentials or not if and only if the
 * page request is done from the CMS context. This valve checks if the CMS has provided encrypted credentials or not.
 * If
 * the credentials are _not_ available
 * with the URL, this valve will redirect to the CMS auth URL with a secret. If the credentials are  available with the
 * URL, this valve will try to get the session for the credentials and continue. </p>
 *
 * <p>
 * The check whether the page request originates from a CMS context is done by checking whether the {@link
 * HstRequestContext#getRenderHost()}
 * is not <code>null</code> : A non-null render host implies that the CMS requested the page.
 * </p>
 */
public class CmsSecurityValve extends AbstractBaseOrderableValve {

    private static final String HSTSESSIONID_COOKIE_NAME = "HSTSESSIONID";

    private SessionSecurityDelegation sessionSecurityDelegation;

    public void setSessionSecurityDelegation(SessionSecurityDelegation sessionSecurityDelegation) {
        this.sessionSecurityDelegation = sessionSecurityDelegation;
    }

    @Override
    public void invoke(ValveContext context) throws ContainerException {
        HttpServletRequest servletRequest = context.getServletRequest();
        HttpServletResponse servletResponse = context.getServletResponse();
        HstRequestContext requestContext = context.getRequestContext();

        if (!requestContext.isCmsRequest()) {
            String ignoredPrefix = requestContext.getResolvedMount().getMatchingIgnoredPrefix();
            if (!StringUtils.isEmpty(ignoredPrefix) && ignoredPrefix.equals(requestContext.getResolvedMount()
                    .getMount().getVirtualHost().getVirtualHosts().getCmsPreviewPrefix())) {
                // When the ignoredPrefix is not equal cmsPreviewPrefix the request is only allowed in the CMS CONTEXT
                sendError(servletResponse, HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            context.invokeNext();
            return;
        }

        log.debug("Request '{}' is invoked from CMS context. Check whether the SSO handshake is done.", servletRequest.getRequestURL());

        HttpSession httpSession = servletRequest.getSession(false);
        CmsSessionContext cmsSessionContext = httpSession != null ? CmsSessionContext.getContext(httpSession) : null;

        if (httpSession == null || cmsSessionContext == null)
        {
            CmsContextService cmsContextService = HippoServiceRegistry.getService(CmsContextService.class);
            if (cmsContextService == null) {
                log.debug("No CmsContextService available");
                sendError(servletResponse, HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            final String cmsContextServiceId = servletRequest.getParameter("cmsCSID");
            final String cmsSessionContextId = servletRequest.getParameter("cmsSCID");
            if (cmsContextServiceId == null || cmsSessionContextId == null) {
                // no CmsSessionContext and/or CmsContextService IDs provided:  if possible, request these by redirecting back to CMS
                final String method = servletRequest.getMethod();
                if (!"GET".equals(method) && !"HEAD".equals(method)) {
                    log.warn("Invalid request to redirect for authentication because request method is '{}' and only" +
                            " 'GET' or 'HEAD' are allowed", method);
                    sendError(servletResponse, HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
                log.debug("No CmsSessionContext and/or CmsContextService IDs found. Redirect to the CMS");
                redirectToCms(servletRequest, servletResponse, requestContext, cmsContextService.getId());
                return;
            }

            if (!cmsContextServiceId.equals(cmsContextService.getId())) {
                log.warn("Cannot authorize request: not coming from this CMS HOST. Redirecting to cms authentication URL to retry.");
                redirectToCms(servletRequest, servletResponse, requestContext, cmsContextService.getId());
                return;
            }

            if (httpSession == null) {
                httpSession = servletRequest.getSession(true);
            }
            cmsSessionContext = cmsContextService.attachSessionContext(cmsSessionContextId, httpSession);
            if (cmsSessionContext == null) {
                httpSession.invalidate();
                log.warn("Cannot authorize request: CmsSessionContext not found");
                sendError(servletResponse, HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }

        updateHstSessionCookie(servletRequest, servletResponse, httpSession);

        servletRequest.setAttribute(CMS_REQUEST_USER_ID_ATTR, cmsSessionContext.getRepositoryCredentials().getUserID());
        servletRequest.setAttribute(CMS_REQUEST_REPO_CREDS_ATTR, cmsSessionContext.getRepositoryCredentials());

        // TODO: remove this with 5.0 / CMS 12.0
        {
            httpSession.setAttribute(CMS_USER_ID_ATTR, cmsSessionContext.getRepositoryCredentials().getUserID());
            httpSession.setAttribute(CMS_SSO_REPO_CREDS_ATTR_NAME, cmsSessionContext.getRepositoryCredentials());
            httpSession.setAttribute(CMS_SSO_AUTHENTICATED, Boolean.TRUE);
        }

        // we need to synchronize on a http session as a jcr session which is tied to it is not thread safe. Also, virtual states will be lost
        // if another thread flushes this session.
        synchronized (httpSession) {
            Session jcrSession = null;
            try {
                if (isCmsRestOrPageComposerRequest(servletRequest)) {
                    jcrSession = createCmsChannelManagerRestSession(servletRequest);
                } else {
                    // request preview website, for example in channel manager. The request is not
                    // a REST call
                    if (sessionSecurityDelegation.sessionSecurityDelegationEnabled()) {
                        jcrSession = createCmsPreviewSession(servletRequest);
                    } else {
                        // do not yet create a session. just use the one that the HST container will create later
                    }
                }

                if (jcrSession != null) {
                    ((HstMutableRequestContext)requestContext).setSession(jcrSession);
                }
                context.invokeNext();

                if (jcrSession != null && jcrSession.hasPendingChanges()) {
                    log.warn("Request to {} triggered changes in JCR session that were not saved - they will be lost", servletRequest.getPathInfo());
                }
            } catch (LoginException e) {
                // the credentials of the current CMS user have changed, so reset the current authentication
                log.info("Credentials of CMS user '{}' are no longer valid, resetting its HTTP session and starting the SSO handshake again.",
                        cmsSessionContext.getRepositoryCredentials().getUserID());
                httpSession.invalidate();
                redirectToCms(servletRequest, servletResponse, requestContext, null);
                return;
            } catch (RepositoryException e) {
                log.warn("RepositoryException : {}", e.toString());
                throw new ContainerException(e);
            } finally {
                if (jcrSession != null) {
                    jcrSession.logout();
                }
            }
        }
    }

    private static void sendError(final HttpServletResponse servletResponse, final int errorCode) throws ContainerException {
        try {
            servletResponse.sendError(errorCode);
        } catch (IOException e) {
            throw new ContainerException(String.format("Unable to send unauthorized (%s) response to client", errorCode) , e);
        }
    }

    private static void redirectToCms(final HttpServletRequest servletRequest, final HttpServletResponse servletResponse,
                                      final HstRequestContext requestContext, final String cmsContextServiceId) throws ContainerException {

        if (servletRequest.getParameterMap().containsKey("retry")) {
            // endless redirect loop protection
            // in case the loadbalancer keeps skewing the CMS and HST application from different container instances
            sendError(servletResponse, HttpServletResponse.SC_CONFLICT);
            return;
        }

        try {
            final String cmsAuthUrl = createCmsAuthenticationUrl(servletRequest, requestContext, cmsContextServiceId);
            servletResponse.sendRedirect(cmsAuthUrl);
        } catch (UnsupportedEncodingException e) {
            log.error("Unable to encode the destination url with UTF8 encoding " + e.getMessage(), e);
            sendError(servletResponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (IOException e) {
            log.error("Something gone wrong so stopping valve invocation fall through: " + e.getMessage(), e);
            sendError(servletResponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private static String createCmsAuthenticationUrl(final HttpServletRequest servletRequest, final HstRequestContext requestContext, final String cmsContextServiceId) throws ContainerException {
        final String farthestRequestUrlPrefix = getFarthestUrlPrefix(servletRequest);
        final String cmsLocation = getCmsLocationByPrefix(requestContext, farthestRequestUrlPrefix);
        final String destinationURL = createDestinationUrl(servletRequest, requestContext, farthestRequestUrlPrefix);

        final StringBuilder authUrl = new StringBuilder(cmsLocation);
        if (!cmsLocation.endsWith("/")) {
            authUrl.append("/");
        }
        authUrl.append("auth?destinationUrl=").append(destinationURL);
        if (cmsContextServiceId != null) {
            authUrl.append("&cmsCSID=").append(cmsContextServiceId);
        }
        return authUrl.toString();
    }

    private static String getFarthestUrlPrefix(final HttpServletRequest servletRequest) {
        final String farthestRequestScheme = HstRequestUtils.getFarthestRequestScheme(servletRequest);
        final String farthestRequestHost = HstRequestUtils.getFarthestRequestHost(servletRequest, false);
        return farthestRequestScheme + "://" + farthestRequestHost;
    }

    private static String getCmsLocationByPrefix(final HstRequestContext requestContext, final String prefix) throws ContainerException {
        final Mount mount = requestContext.getResolvedMount().getMount();
        final List<String> cmsLocations = mount.getCmsLocations();
        for (String cmsLocation : cmsLocations) {
            if (cmsLocation.startsWith(prefix)) {
                return cmsLocation;
            }
        }
        throw new ContainerException("Could not establish a SSO between CMS & site application because no CMS location could be found that starts with '" + prefix + "'");
    }

    private static String createDestinationUrl(final HttpServletRequest servletRequest, final HstRequestContext requestContext, final String prefix) {
        final StringBuilder destinationURL = new StringBuilder(prefix);

        // we append the request uri including the context path (normally this is /site/...)
        destinationURL.append(servletRequest.getRequestURI());

        if (requestContext.getPathSuffix() != null) {
            final HstManager hstManager = HstServices.getComponentManager().getComponent(HstManager.class.getName());
            final String subPathDelimiter = hstManager.getPathSuffixDelimiter();
            destinationURL.append(subPathDelimiter).append(requestContext.getPathSuffix());
        }

        final String queryString = servletRequest.getQueryString();
        if (queryString != null) {
            destinationURL.append("?").append(queryString);
        }
        return destinationURL.toString();
    }

    private static void updateHstSessionCookie(final HttpServletRequest servletRequest, final HttpServletResponse servletResponse, final HttpSession session) {
        final Cookie[] cookies = servletRequest.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (HSTSESSIONID_COOKIE_NAME.equals(cookie.getName()) && session.getId().equals(cookie.getValue())) {
                    // HSTSESSIONID_COOKIE_NAME cookie already present and correct
                    return;
                }
            }
        }
        // (java) session cookie may not be available to the client-side javascript code,
        // as the cookie may be secured by the container (useHttpOnly=true).
        Cookie sessionIdCookie = new Cookie(HSTSESSIONID_COOKIE_NAME, session.getId());
        sessionIdCookie.setMaxAge(-1);
        servletResponse.addCookie(sessionIdCookie);
    }

    private static boolean isCmsRestOrPageComposerRequest(final HttpServletRequest servletRequest) {
        return Boolean.TRUE.equals(servletRequest.getAttribute(ContainerConstants.CMS_REST_REQUEST_CONTEXT));
    }

    private Session createCmsChannelManagerRestSession(final HttpServletRequest request) throws LoginException, ContainerException {
        long start = System.currentTimeMillis();
        try {
            final Credentials credentials = (Credentials)request.getAttribute(ContainerConstants.CMS_REQUEST_REPO_CREDS_ATTR);
            // This returns a plain session for credentials where access is not merged with for example preview user session
            // For cms rest calls to page composer or cms-rest we must *NEVER* combine the security with other sessions
            Session session = sessionSecurityDelegation.getDelegatedSession(credentials);
            log.debug("Acquiring cms rest session took '{}' ms.", (System.currentTimeMillis() - start));
            return session;
        } catch (LoginException e) {
            throw e;
        } catch (Exception e) {
            throw new ContainerException("Failed to create session based on SSO.", e);
        }
    }

    private Session createCmsPreviewSession(final HttpServletRequest request) throws LoginException, ContainerException {
        long start = System.currentTimeMillis();
        Credentials cmsUserCred = (Credentials)request.getAttribute(ContainerConstants.CMS_REQUEST_REPO_CREDS_ATTR);
        try {
            Session session = sessionSecurityDelegation.createPreviewSecurityDelegate(cmsUserCred, false);
            log.debug("Acquiring security delegate session took '{}' ms.", (System.currentTimeMillis() - start));
            return session;
        } catch (LoginException e) {
            throw e;
        } catch (Exception e) {
            throw new ContainerException("Failed to create Session based on SSO.", e);
        }
    }
}
