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

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.jcr.LazySession;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;

/**
 * StatefulSessionValve
 * 
 * @version $Id$
 */
public class SubjectBasedSessionValve extends AbstractValve {
    
    public static final String SUBJECT_BASED_SESSION_ATTR_NAME = SubjectBasedSessionValve.class.getName() + ".session";
    
    protected Repository subjectBasedRepository;
    
    public void setSubjectBasedRepository(Repository subjectBasedRepository) {
        this.subjectBasedRepository = subjectBasedRepository;
    }
    
    @Override
    public void invoke(ValveContext context) throws ContainerException {
        HttpServletRequest servletRequest = (HttpServletRequest) context.getServletRequest();
        HstRequestContext requestContext = (HstRequestContext) servletRequest.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
        ResolvedMount resolvedMount = requestContext.getResolvedMount();
        boolean subjectBasedSession = resolvedMount.isSubjectBasedSession();
        boolean sessionStateful = resolvedMount.isSessionStateful();
        
        if (subjectBasedSession) {
            if (requestContext.getSubject() == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Subject based session cannot be set because no subject is found.");
                }
            } else {
                setSubjectSession(context, requestContext, sessionStateful);
            }
        }
        
        context.invokeNext();
    }
    
    protected void setSubjectSession(ValveContext valveContext, HstRequestContext requestContext, boolean sessionStateful) throws ContainerException {
        LazySession lazySession = null;
        
        if (sessionStateful) {
            HttpSession httpSession = valveContext.getServletRequest().getSession(false);
            lazySession = (httpSession != null ? (LazySession) httpSession.getAttribute(SUBJECT_BASED_SESSION_ATTR_NAME) : (LazySession) null);
            
            if (lazySession != null) {
                boolean isLive = false;
                
                try {
                    isLive = lazySession.isLive();
                } catch (Exception e) {
                    log.error("Error during checking lazy session", e);
                }
                
                if (!isLive) {
                    try {
                        lazySession.logout();
                    } catch (Exception ignore) {
                        ;
                    } finally {
                        lazySession = null;
                    }
                }
            }
        } else {
            lazySession = (LazySession) requestContext.getAttribute(SUBJECT_BASED_SESSION_ATTR_NAME);
        }
        
        if (lazySession == null) {
            try {
                lazySession = (LazySession) subjectBasedRepository.login();
            } catch (Exception e) {
                throw new ContainerException("Failed to create session based on subject.", e);
            }
        }
        
        if (sessionStateful) {
            valveContext.getServletRequest().getSession(true).setAttribute(SUBJECT_BASED_SESSION_ATTR_NAME, lazySession);
            
            long refreshPendingTimeMillis = lazySession.getRefreshPendingAfter();
            
            if (refreshPendingTimeMillis > 0L && lazySession.lastRefreshed() < refreshPendingTimeMillis) {
                try {
                    lazySession.refresh(false);
                } catch (RepositoryException e) {
                    throw new ContainerException("Failed to refresh session.", e);
                }
            }
        } else {
            requestContext.setAttribute(SUBJECT_BASED_SESSION_ATTR_NAME, lazySession);
        }
        
        ((HstMutableRequestContext) requestContext).setSession(lazySession);
    }
}
