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
package org.hippoecm.hst.jcr;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.SimpleCredentials;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.core.HSTConfiguration;
import org.hippoecm.hst.core.filters.base.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrSessionPoolManager {
    
    private final Logger log = LoggerFactory.getLogger(JcrSessionPoolManager.class);
    private final Map<String,JcrSessionPool> jcrSessionPools = new HashMap<String,JcrSessionPool>();
    
    private static long lastEventTick = System.currentTimeMillis();
    private int refreshInterval = 0;
    private JcrSessionPoolManagerChecker jcrSessionPoolManagerChecker;
    
    private static final JcrSessionPoolManager jcrSessionPoolManager = new JcrSessionPoolManager();
    
    public static long getLastEventTick() {
        return lastEventTick;
    }

    private JcrSessionPoolManager(){
        this.jcrSessionPoolManagerChecker = new JcrSessionPoolManagerChecker();
        startChecker();
    }
    
    public void startChecker() {
        jcrSessionPoolManagerChecker.start();
    }
    public static JcrSessionPoolManager getInstance(){
        return jcrSessionPoolManager;
    }
    
    public static void setLastEventTick(long lastEventTick) {
        JcrSessionPoolManager.lastEventTick = lastEventTick;
    }

    public ReadOnlyPooledSession getSession(HttpServletRequest request, HstRequestContext hstRequestContext, String entryPath){
        
        // TODO improve speed of getting a session from the pool. 
        ServletContext sc = request.getSession().getServletContext();
        String repositoryLocation = HSTConfiguration.get(sc, HSTConfiguration.KEY_REPOSITORY_ADRESS);
        String username = HSTConfiguration.get(sc, HSTConfiguration.KEY_REPOSITORY_USERNAME);
        String password = HSTConfiguration.get(sc, HSTConfiguration.KEY_REPOSITORY_PASSWORD);
        SimpleCredentials simpleCredentials = new SimpleCredentials(username, (password != null ? password.toCharArray() : null));
        
        // TODO a less blocking synronization
        String poolKey = "";
        if(entryPath != null) {
            poolKey += entryPath+":";  
        }
        if(simpleCredentials.getUserID() != null) {
        poolKey += simpleCredentials.getUserID();
        }
        else{
            poolKey += "anonymous";
        }
        boolean isSessionInterValRefresh = true;
        
        synchronized(jcrSessionPools) {
            if(hstRequestContext != null) {
                poolKey += ":preview="+hstRequestContext.isPreview();
                // the preview does not do interval refresh
                isSessionInterValRefresh = !hstRequestContext.isPreview();
            }
            JcrSessionPool jcrSessionPool = jcrSessionPools.get(poolKey);
            if(jcrSessionPool == null) {
                log.debug("No session pool present for key '" +poolKey+ "'. Create one" );
                jcrSessionPool = new JcrSessionPool(simpleCredentials, repositoryLocation, isSessionInterValRefresh, refreshInterval, poolKey);
                jcrSessionPools.put(poolKey,jcrSessionPool);
                return jcrSessionPool.getSession(request.getSession());
            }
            log.debug("Return session from pool if an idle valid one is present, otherwise add a new one to the session");
            return jcrSessionPool.getSession(request.getSession());
        }
    }
    
    public ReadOnlyPooledSession getSessionForBinaries(HttpServletRequest request){
        return getSession(request, null, null);
    }
    
    public void dispose() {
        if (this.jcrSessionPoolManagerChecker != null) {
            try {
                this.jcrSessionPoolManagerChecker.interrupt();
            } catch (Exception e) {
                 log.warn("Exception occurred during interrupting jcrSessionPoolManagerChecker thread. {}", e.toString());
            }
            this.jcrSessionPoolManagerChecker = null;
        }
        
       // logout *all* jcr sessions. 
        synchronized(jcrSessionPools) {
            for(Iterator<JcrSessionPool> si = jcrSessionPools.values().iterator(); si.hasNext(); ) {
                JcrSessionPool jcrSessionPool = si.next();
                jcrSessionPool.dispose();
            }
        }
        
        
    }

    public void setRefreshInterval(int refreshInterval) {
        this.refreshInterval = refreshInterval;
    }
    
    private class JcrSessionPoolManagerChecker extends Thread {

        private JcrSessionPoolManagerChecker() {
            super("JcrSessionPoolChecker");
        }

        public void run() {
            while (true) {
                try {
                    Thread.sleep(60000);
                    synchronized (jcrSessionPools) {
                        for (JcrSessionPool pool : jcrSessionPools.values()) {
                            pool.cleanupPool();
                        }

                    }
                } catch (InterruptedException e) {
                    log.error("JcrSessionPoolManagerChecker thread died");
                }
            }
        }
    }

    public Map<String, JcrSessionPool> getJcrSessionPools() {
        return jcrSessionPools;
    }

    public void markAllDirty() {
        synchronized (jcrSessionPools) {
           for(JcrSessionPool pool: jcrSessionPools.values()) {
               pool.markDirty();
           }
       }
    }

    public void markDirty(String dirty) {
        synchronized (jcrSessionPools) {
            JcrSessionPool pool = jcrSessionPools.get(dirty); 
            if(pool != null) {
                pool.markDirty();
            }
        }
    }
    
    
}
