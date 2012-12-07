package org.hippoecm.hst.pagecomposer.container;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.SignatureException;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.container.AbstractValve;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.ValveContext;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.jcr.LazySession;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.security.AuthenticationProvider;
import org.onehippo.sso.CredentialCipher;

/**
 * CmsSecurityValve responsible for authenticating the user using CMS. <p> When CMS invokes the mount configured with
 * this valve, HST checks if the CMS has provided encrypted credentials or not. If the credentials are _not_ available
 * with the URL, this valve will redirect to the CMS auth URL with a secret. If the credentials are  available with the
 * URL, this valve will try to get the session for the credentials and continue. </p>
 */
public class CmsSecurityValve extends AbstractValve {
    protected AuthenticationProvider authProvider;
    private Repository repository;
    private static final String SSO_BASED_SESSION_ATTR_NAME = CmsSecurityValve.class.getName() + ".jcrSession";

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setAuthenticationProvider(AuthenticationProvider authProvider) {
        this.authProvider = authProvider;
    }

    @Override
    public void invoke(ValveContext context) throws ContainerException {
        HttpServletRequest servletRequest = context.getServletRequest();
        HttpServletResponse servletResponse = context.getServletResponse();
        HstRequestContext requestContext = context.getRequestContext();
        ResolvedMount resolvedMount = requestContext.getResolvedMount();

        HttpSession session = servletRequest.getSession(true);

        //First check if the user has access to the requested mount
        if (session.getAttribute(ContainerConstants.SUBJECT_REPO_CREDS_ATTR_NAME) == null) {
            String key = servletRequest.getSession(true).getId();
            String credentialParam = servletRequest.getParameter("cred");
            //If there is no secret or credentialParam, add the secret and request for credentialParam by redirecting back to CMS.
            if (credentialParam == null) {
                HstLink destinationLink = null;
                try {
                    Mount destLinkMount = resolvedMount.getMount();

                    if (!destLinkMount.isSite()) {
                        Mount siteMount = requestContext.getMount(ContainerConstants.MOUNT_ALIAS_SITE);

                        if (siteMount != null) {
                            destLinkMount = siteMount;
                        }
                    }

                    ResolvedSiteMapItem resolvedSiteMapItem = requestContext.getResolvedSiteMapItem();
                    String pathInfo = (resolvedSiteMapItem == null ? "" : resolvedSiteMapItem.getPathInfo());
                    destinationLink = requestContext.getHstLinkCreator().create(pathInfo, destLinkMount);

                } catch (Exception linkEx) {
                    if (log.isDebugEnabled()) {
                        log.warn("Failed to create destination link.", linkEx);
                    } else if (log.isWarnEnabled()) {
                        log.warn("Failed to create destination link. {}", linkEx.toString());
                    }
                }

                // generate key; redirect to cms
                try {
                    String cmsAuthUrl = null;
                    if (destinationLink != null) {
                        String cmsBaseUrl = requestContext.getContainerConfiguration().getString(ContainerConstants.CMS_LOCATION);
                        if (!cmsBaseUrl.endsWith("/")) {
                            cmsBaseUrl += "/";
                        }
                        cmsAuthUrl = cmsBaseUrl + "auth?destinationUrl=" + URLEncoder.encode(destinationLink.toUrlForm(requestContext, true), "UTF8") + "&key=" + key;
                    } else {
                        log.error("No destinationUrl specified");
                    }
                    
                    if (cmsAuthUrl != null) {
                        //Everything seems to be fine, redirect to destination url and return
                        servletResponse.sendRedirect(cmsAuthUrl);
                    } else {
                        log.error("No cmsAuthUrl specified");
                    }
                } catch (UnsupportedEncodingException e) {
                    log.error("Unable to encode the destination url with utf8 encoding" + e.getMessage(), e);
                } catch (IOException e) {
                    log.error("Something gone wrong so stopping valve invocation fall through:" + e.getMessage(), e);

                }

                return;
            } else {
                CredentialCipher credentialCipher = CredentialCipher.getInstance();
                try {
                    Credentials cred = credentialCipher.decryptFromString(key, credentialParam);
                    session.setAttribute(ContainerConstants.SUBJECT_REPO_CREDS_ATTR_NAME, cred);
                    setSSOSession(session, cred);
                } catch (SignatureException se) {
                    throw new ContainerException(se);
                }
            }
        }


        LazySession lazySession = (LazySession) session.getAttribute(SSO_BASED_SESSION_ATTR_NAME);
        ((HstMutableRequestContext) requestContext).setSession(lazySession);

        context.invokeNext();
    }

    protected void setSSOSession(HttpSession httpSession, Credentials credentials) throws ContainerException {
        try {
            LazySession lazySession = (LazySession) repository.login(credentials);
            httpSession.setAttribute(SSO_BASED_SESSION_ATTR_NAME, lazySession);
            httpSession.setAttribute(ContainerConstants.CMS_SSO_AUTHENTICATED, true);
        } catch (Exception e) {
            throw new ContainerException("Failed to create session based on subject.", e);
        }
    }

}
