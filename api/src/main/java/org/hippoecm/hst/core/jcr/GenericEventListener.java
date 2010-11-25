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

import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

/**
 * The <CODE>GenericEventListener</CODE> class provides a default implementation for
 * the {@link EventListener} interface.
 * This receives an event and dispatches each event to a specialized method.
 * The child class of this class can override some methods which are related to
 * its own interests.
 */
public class GenericEventListener implements EventListener {

    public final void onEvent(EventIterator events) {
        while (events.hasNext()) {
            Event event = events.nextEvent();
            int type = event.getType();
            
            switch (type) {
            case Event.NODE_ADDED:
                onNodeAdded(event);
                break;
            case Event.NODE_REMOVED:
                onNodeRemoved(event);
                break;
            case Event.PROPERTY_ADDED:
                onPropertyAdded(event);
                break;
            case Event.PROPERTY_CHANGED:
                onPropertyChanged(event);
                break;
            case Event.PROPERTY_REMOVED:
                onPropertyRemoved(event);
                break;
            }
        }
    }
    
    protected void onNodeAdded(Event event) {
    }

    protected void onNodeRemoved(Event event) {
    }
    
    protected void onPropertyAdded(Event event) {
    }
    
    protected void onPropertyChanged(Event event) {
    }
    
    protected void onPropertyRemoved(Event event) {
    }
    
}
