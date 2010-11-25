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
package org.hippoecm.hst.core.jcr;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpSessionBindingListener;

import org.apache.commons.proxy.Invoker;
import org.hippoecm.hst.proxy.ProxyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LazySessionDelegatingRepository
 * @version $Id$
 */
public class LazySessionDelegatingRepository extends DelegatingRepository {
    
    private static Logger log = LoggerFactory.getLogger(LazySessionDelegatingRepository.class);
    
    private boolean logoutOnSessionUnbound;
    private SessionsRefreshCounter sessionsRefreshCounter = new SessionsRefreshCounter();
    
    public LazySessionDelegatingRepository(Repository delegatee) {
        super(delegatee);
    }
    
    public void setLogoutOnSessionUnbound(boolean logoutOnSessionUnbound) {
        this.logoutOnSessionUnbound = logoutOnSessionUnbound;
    }
    
    public Session login() throws LoginException, RepositoryException {
        return createLazySession(null, null);
    }

    public Session login(Credentials credentials) throws LoginException, RepositoryException {
        return createLazySession(credentials, null);
    }

    public Session login(String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return createLazySession(null, workspaceName);
    }

    public Session login(Credentials credentials, String workspaceName) throws LoginException,
            NoSuchWorkspaceException, RepositoryException {
        return createLazySession(credentials, workspaceName);
    }
    
    public void setSessionsRefreshPendingAfter(long sessionsRefreshPendingAfter) {
        sessionsRefreshCounter.setSessionsRefreshPendingAfter(sessionsRefreshPendingAfter);
    }
    
    public long getSessionsRefreshPendingAfter() {
        return sessionsRefreshCounter.getSessionsRefreshPendingAfter();
    }
    
    protected Session createLazySession(Credentials credentials, String workspaceName) {
        Session lazySession = null;
        
        ProxyFactory factory = new ProxyFactory();
        LazySessionInvoker invoker = new LazySessionInvoker(getDelegatee(), credentials, workspaceName, logoutOnSessionUnbound);
        invoker.setSessionsRefreshCounter(sessionsRefreshCounter);
        
        ClassLoader sessionClassloader = getDelegatee().getClass().getClassLoader();
        ClassLoader currentClassloader = Thread.currentThread().getContextClassLoader();
        
        try {
            if (sessionClassloader != currentClassloader) {
                Thread.currentThread().setContextClassLoader(sessionClassloader);
            }
            
            lazySession = (Session) factory.createInvokerProxy(sessionClassloader, invoker, new Class [] { LazySession.class, Serializable.class, HttpSessionBindingListener.class });
        } finally {
            if (sessionClassloader != currentClassloader) {
                Thread.currentThread().setContextClassLoader(currentClassloader);
            }
        }

        return lazySession;
    }
    
    protected static class SessionsRefreshCounter {
        
        private long sessionsRefreshPendingTimeMillis;
        
        public void setSessionsRefreshPendingAfter(long sessionsRefreshPendingTimeMillis) {
            this.sessionsRefreshPendingTimeMillis = sessionsRefreshPendingTimeMillis;
        }
        
        public long getSessionsRefreshPendingAfter() {
            return sessionsRefreshPendingTimeMillis;
        }
    }
    
    protected static class LazySessionInvoker implements Invoker, Serializable {
        
        private static final long serialVersionUID = 1L;
        
        private transient Repository repository;
        private transient WeakReference<Session> sessionWeakRef;
        private Credentials credentials;
        private String workspaceName;
        private boolean logoutOnSessionUnbound;
        private long lastLoggedIn;
        private long lastRefreshed;
        private SessionsRefreshCounter sessionsRefreshCounter;
        
        public LazySessionInvoker() {
        }
        
        public LazySessionInvoker(Repository repository, Credentials credentials, String workspaceName) {
            this(repository, credentials, workspaceName, false);
        }
        
        public LazySessionInvoker(Repository repository, Credentials credentials, String workspaceName, boolean logoutOnSessionUnbound) {
            this.repository = repository;
            this.credentials = credentials;
            this.workspaceName = workspaceName;
            this.logoutOnSessionUnbound = logoutOnSessionUnbound;
        }
        
        public void setSessionsRefreshCounter(SessionsRefreshCounter sessionsRefreshCounter) {
            this.sessionsRefreshCounter = sessionsRefreshCounter;
        }
        
        public SessionsRefreshCounter getSessionsRefreshCounter() {
            return sessionsRefreshCounter;
        }
        
        public Object invoke(Object proxy, Method method, Object [] args) throws Throwable {
            Class<?> declaringClass = method.getDeclaringClass();
            String methodName = method.getName();
            
            if (LazySession.class.isAssignableFrom(declaringClass)) {
                if ("getRefreshPendingAfter".equals(methodName)) {
                    return sessionsRefreshCounter.getSessionsRefreshPendingAfter();
                } else if ("logoutSession".equals(methodName)) {
                    clearSession();
                } else if ("lastLoggedIn".equals(methodName)) {
                    return lastLoggedIn;
                } else if ("lastRefreshed".equals(methodName)) {
                    return lastRefreshed;
                }
                
                return null;
            }
            
            if (Session.class.isAssignableFrom(declaringClass)) {
                Session session = (sessionWeakRef != null ? sessionWeakRef.get() : null);
                
                if (session == null) {
                    if ("logout".equals(methodName)) {
                        return null;
                    }
                    
                    if ("refresh".equals(methodName)) {
                        lastRefreshed = System.currentTimeMillis();
                        return null;
                    }
                    
                    // Because this session is lazy, it should mimic a live session
                    // even though the session is not initialized yet.
                    if ("isLive".equals(methodName)) {
                        return true;
                    }
                    
                    if (credentials == null && workspaceName == null) {
                        session = repository.login();
                    } else if (credentials != null && workspaceName == null) {
                        session = repository.login(credentials);
                    } else if (credentials == null && workspaceName != null) {
                        session = repository.login(workspaceName);
                    } else {
                        session = repository.login(credentials, workspaceName);
                    }
                    
                    lastLoggedIn = System.currentTimeMillis();
                    lastRefreshed = 0L;
                    sessionWeakRef = new WeakReference<Session>(session);
                }
                
                Object ret = method.invoke(session, args);
                
                if ("refresh".equals(methodName)) {
                    lastRefreshed = System.currentTimeMillis();
                }
                
                return ret;
            }
            
            if (HttpSessionBindingListener.class.isAssignableFrom(declaringClass)) {
                if ("valueUnbound".equals(methodName)) {
                    if (log.isDebugEnabled()) {
                        log.debug("LazySession session value is being unbound.");
                    }
                    
                    clearSession();
                }
                
                return null;
            }
            
            // to override default toString() implemented in AbstractInvocationHandler.
            if ("toString".equals(methodName)) {
                Session session = (sessionWeakRef != null ? sessionWeakRef.get() : null);
                return super.toString() + " (" + session + ")";
            }
            
            return null;
        }
        
        protected void clearSession() {
            if (!logoutOnSessionUnbound) {
                return;
            }
            
            Session session = null;
            
            if (sessionWeakRef != null) {
                session = sessionWeakRef.get();
                sessionWeakRef.clear();
            }
            
            lastLoggedIn = 0L;
            lastRefreshed = 0L;
            
            if (session == null) {
                return;
            }
            
            try {
                session.logout();
                
                if (log.isDebugEnabled()) {
                    log.debug("LazySession's session is logged out.");
                }
            } catch (Throwable th) {
                if (log.isDebugEnabled()) {
                    log.warn("Failed to logout stateful session.", th);
                } else {
                    log.warn("Failed to logout stateful session: " + th);
                }
            }
        }
        
        @Override
        protected void finalize() {
            //System.out.println("LazySession object is being finalized.");
            if (log.isDebugEnabled()) {
                log.debug("LazySession object is being finalized.");
            }
            
            clearSession();
        }
    }

}
