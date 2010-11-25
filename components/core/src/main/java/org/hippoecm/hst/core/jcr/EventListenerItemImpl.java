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

import javax.jcr.observation.EventListener;

import org.apache.jackrabbit.spi.Event;

public class EventListenerItemImpl implements EventListenerItem {
    
    protected int eventTypes;
    protected String absolutePath;
    protected boolean deep;
    protected String [] uuids;
    protected String [] nodeTypeNames;
    protected boolean noLocal;
    protected EventListener eventListener;
    
    public int getEventTypes() {
        return eventTypes;
    }
    
    public void setEventTypes(int eventTypes) {
        this.eventTypes = eventTypes;
    }
    
    public boolean isNodeAddedEnabled() {
        return (Event.NODE_ADDED == (Event.NODE_ADDED & this.eventTypes));
    }

    public void setNodeAddedEnabled(boolean nodeAddedEnabled) {
        this.eventTypes |= Event.NODE_ADDED;
    }

    public boolean isNodeRemovedEnabled() {
        return (Event.NODE_REMOVED == (Event.NODE_REMOVED & this.eventTypes));
    }

    public void setNodeRemovedEnabled(boolean nodeRemovedEnabled) {
        this.eventTypes |= Event.NODE_REMOVED;
    }

    public boolean isPropertyAddedEnabled() {
        return (Event.PROPERTY_ADDED == (Event.PROPERTY_ADDED & this.eventTypes));
    }

    public void setPropertyAddedEnabled(boolean propertyAddedEnabled) {
        this.eventTypes |= Event.PROPERTY_ADDED;
    }

    public boolean isPropertyChangedEnabled() {
        return (Event.PROPERTY_CHANGED == (Event.PROPERTY_CHANGED & this.eventTypes));
    }

    public void setPropertyChangedEnabled(boolean propertyChangedEnabled) {
        this.eventTypes |= Event.PROPERTY_CHANGED;
    }

    public boolean isPropertyRemovedEnabled() {
        return (Event.PROPERTY_REMOVED == (Event.PROPERTY_REMOVED & this.eventTypes));
    }

    public void setPropertyRemovedEnabled(boolean propertyRemovedEnabled) {
        this.eventTypes |= Event.PROPERTY_REMOVED;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }
    
    public void setAbsolutePath(String absolutePath) {
        this.absolutePath = absolutePath;
    }
    
    public boolean isDeep() {
        return deep;
    }
    
    public void setDeep(boolean deep) {
        this.deep = deep;
    }
    
    public String[] getUuids() {
        return uuids;
    }
    
    public void setUuids(String[] uuids) {
        this.uuids = uuids;
    }
    
    public String[] getNodeTypeNames() {
        return nodeTypeNames;
    }
    
    public void setNodeTypeNames(String[] nodeTypeNames) {
        this.nodeTypeNames = nodeTypeNames;
    }
    
    public boolean isNoLocal() {
        return noLocal;
    }
    
    public void setNoLocal(boolean noLocal) {
        this.noLocal = noLocal;
    }
    
    public EventListener getEventListener() {
        return eventListener;
    }
    
    public void setEventListener(EventListener eventListener) {
        this.eventListener = eventListener;
    }

}
