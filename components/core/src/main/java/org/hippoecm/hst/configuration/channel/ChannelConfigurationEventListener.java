/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.hst.configuration.channel;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

import org.hippoecm.hst.core.jcr.EventListenersContainerListener;
import org.hippoecm.hst.core.jcr.GenericEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelConfigurationEventListener extends GenericEventListener implements EventListenersContainerListener {

    static Logger log = LoggerFactory.getLogger(ChannelConfigurationEventListener.class);

    private ChannelManagerImpl manager;

    public void setManager(final ChannelManagerImpl manager) {
        this.manager = manager;
    }

    public void onEvent(EventIterator events) {
        Event invaliationEvent = null;

        while (events.hasNext()) {
            Event event = events.nextEvent();

            try {
                if (isEventOnSkippedPath(event)) {
                    continue;
                }
            } catch (RepositoryException e) {
                continue;
            }

            invaliationEvent = event;
            break;
        }

        if (invaliationEvent != null) {
            try {
                if (log.isDebugEnabled()) {
                    log.debug("Event received on {} by {}.", invaliationEvent.getPath());
                }
                doInvalidation(invaliationEvent.getPath());
            } catch (RepositoryException e) {
                if (log.isWarnEnabled()) {
                    log.warn("Cannot invalidate: {}", e.getMessage());
                }
            }
        }
    }

    public void onEventListenersContainerStarted() {
        // do nothing
    }

    public void onEventListenersContainerRefreshed() {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Event received: onEventListenersContainerRefreshed");
            }
            doInvalidation(null);
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                log.warn("Cannot invalidate {}", e.getMessage());
            }
        }
    }

    public void onEventListenersContainerStopped() {
        // do nothing
    }

    private void doInvalidation(String path) {
        manager.invalidate();
    }

}
