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
package org.hippoecm.hst.core.jcr.pool;

import java.util.HashSet;
import java.util.Set;

import javax.jcr.Session;

import org.hippoecm.hst.core.ResourceLifecycleManagement;

public class PooledSessionResourceManagement implements ResourceLifecycleManagement, PoolingRepositoryAware {
    
    private ThreadLocal<Boolean> tlActiveState = new ThreadLocal<Boolean>();
    private ThreadLocal<Set<Session>> tlPooledSessions = new ThreadLocal<Set<Session>>();

    protected PoolingRepository poolingRepository;
    
    public void setPoolingRepository(PoolingRepository poolingRepository) {
        this.poolingRepository = poolingRepository;
    }
    
    public boolean isActive() {
        Boolean activeState = tlActiveState.get();
        return (activeState != null && activeState.booleanValue());
    }
    
    public void setActive(boolean active) {
        Boolean activeState = tlActiveState.get();
        
        if (activeState == null || activeState.booleanValue() != active) {
            activeState = new Boolean(active);
            tlActiveState.set(activeState);
        }
    }

    public void registerResource(Object session) {
        Set<Session> sessions = tlPooledSessions.get();
        
        if (sessions == null) {
            sessions = new HashSet<Session>();
            sessions.add((Session) session);
            tlPooledSessions.set(sessions);
        } else {
            sessions.add((Session) session);
        }
    }
    
    public void unregisterResource(Object session) {
        Set<Session> sessions = tlPooledSessions.get();
        
        if (sessions != null) {
            sessions.remove((Session) session);
        }
    }
    
    public void disposeResource(Object session) {
        this.poolingRepository.returnSession((Session) session);
        unregisterResource(session);
    }
    
    public void disposeAllResources() {
        Set<Session> sessions = tlPooledSessions.get();
        
        if (sessions != null) {
            for (Session session : sessions) {
                this.poolingRepository.returnSession((Session) session);
            }
            
            sessions.clear();
        }
    }

}
