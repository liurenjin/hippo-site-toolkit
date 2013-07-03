/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.util.JcrSessionUtils;
import org.hippoecm.repository.api.HippoSession;
import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HstConfigurationUtils {

    private static final Logger log = LoggerFactory.getLogger(HstConfigurationUtils.class);

    private HstConfigurationUtils() {
    }

    /**
     * Persists pending changes. logs events to the HippoEventBus and if <code>hstManager</code> is not <code>null</code>
     * also invalidates the hstMananger
     * @param session
     * @param hstManager
     * @throws RepositoryException
     */
    public synchronized static void persistChanges(final Session session, HstManager hstManager) throws RepositoryException {
        if (!session.hasPendingChanges()) {
            return;
        }
        String[] pathsToBeChanged = null;
        if (hstManager != null) {
            pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, true);
        }
        StringBuilder buffer = new StringBuilder("User made changes at (and possibly below): ");
        appendPendingChangesFromNodeToBuffer(session, buffer,",");
        session.save();
        if (pathsToBeChanged != null) {
            hstManager.invalidate(pathsToBeChanged);
        }
        //only log when the save is successful
        logEvent("write-changes",session.getUserID(),buffer.toString());
    }

    public static void appendPendingChangesFromNodeToBuffer(final Session session, final StringBuilder buf,
                                                        final String delimiter) throws RepositoryException {
        HippoSession hippoSession = getNonProxiedSession(session);
        if (hippoSession == null) {
            throw new IllegalStateException("Session cannot be un-proxied to HippoSession");
        }
        // we prune the pending changes as for hst configuration it can be very large in case of
        // coarse grained publication as then an entire hst configuration gets copied
        final NodeIterator it = hippoSession.pendingChanges(session.getRootNode(), "nt:base", false);
        while (it.hasNext()) {
            Node node = it.nextNode();
            buf.append(node.getPath());
            if(it.hasNext()) {
                buf.append(delimiter);
            }
        }
    }

    public static void logEvent(String action, String user, String message) {
        final HippoEventBus eventBus = HippoServiceRegistry.getService(HippoEventBus.class);
        if (eventBus != null) {
            final HippoEvent event = new HippoEvent("channel-manager");
            event.category("channel-manager").user(user).action(action);
            event.message(message);
            eventBus.post(event);
        }
    }

    public static HippoSession getNonProxiedSession(Session session) throws RepositoryException {
        HippoSession hippoSession;
        if (!(session instanceof HippoSession)) {
            // a jcr session from request context cannot be directly cast to a HippoSession...hence this workaround:
            Session nonProxiedSession = session.getRootNode().getSession();
            if (!(nonProxiedSession instanceof HippoSession)) {
                throw new IllegalStateException("Session not instance of HippoSession.");
            }
            hippoSession = (HippoSession) nonProxiedSession;
        } else {
            hippoSession = (HippoSession) session;
        }
        return hippoSession;
    }

    /**
     * if needed (finegrained locking mode), this call tries to set a lock. If there is not yet a lock, then also
     * a timestamp validation is done whether the configuration node that needs to be locked has not been modified
     * by someone else
     *
     */
    public static void tryLockIfNeeded(final Node configNode, final long validateLastModifiedTimestamp, final boolean finegrainedLocking) throws RepositoryException, IllegalStateException {
        Session session = configNode.getSession();
        Node rootHstConfigNode = findRootConfigurationNode(configNode);
        if (rootHstConfigNode == null) {
            throw new IllegalStateException("Exception during creating new container item : Could not find a root hst:configuration for node for '"+configNode.getPath()+"'");
        }

        if (finegrainedLocking) {
            Node containerNode = findContainerNode(configNode);
            if (containerNode == null) {
                log.info("No Container found for '{}'.", configNode.getPath());
                throw new IllegalStateException("No Container found for '"+configNode.getPath()+"'.");
            }
            if (isLockedBySomeoneElse(containerNode)) {
                log.info("Container '{}' is already locked by someone else.", containerNode.getPath());
                throw new IllegalStateException("Container '"+containerNode.getPath()+"' is already locked by someone else.");
            }
            if (isLockedBySession(containerNode)) {
                log.debug("Container '{}' already has a lock for user '{}'.", containerNode.getPath(), session.getUserID());
                return;
            }

            if (containerNode.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED)) {
                long existingTimeStamp = containerNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED).getDate().getTimeInMillis();
                if (existingTimeStamp != validateLastModifiedTimestamp) {
                    Calendar existing = Calendar.getInstance();
                    existing.setTimeInMillis(existingTimeStamp);
                    Calendar validate = Calendar.getInstance();
                    validate.setTimeInMillis(validateLastModifiedTimestamp);
                    DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss:SSS zzz", Locale.US);
                    log.info("Container '{}' has been modified at '{}' but validation timestamp was '{}'. Cannot acquire lock now for user '{}'.",
                            new String[]{containerNode.getPath(), dateFormat.format(existing.getTime()),
                                    dateFormat.format(validate.getTime()) , session.getUserID()});
                    throw new IllegalStateException("Container '"+containerNode.getPath()+"' cannot be changed because timestamp validation did not pass.");
                }
            }
            log.info("Container '{}' gets a lock for user '{}'.", containerNode.getPath(), session.getUserID());
            containerNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY, session.getUserID());
            Calendar now = Calendar.getInstance();
            if (!containerNode.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON)) {
                containerNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON, now);
            }
            containerNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED_BY, session.getUserID());
        } else {
            if (isLockedBySomeoneElse(rootHstConfigNode)) {
                throw new IllegalStateException("Container '"+rootHstConfigNode.getPath()+"' is already locked by someone else.");
            }
        }
    }

    /**
     * @return the set timestamp
     */
    public static long setLastModifiedTimestampForContainer(final Node configNode) throws RepositoryException {
        Node containerNode = findContainerNode(configNode);
        if (containerNode == null) {
            throw new IllegalStateException("No Container found for '"+configNode.getPath()+"'.");
        }
        final Calendar cal = Calendar.getInstance();
        containerNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED, cal);
        return cal.getTimeInMillis();
    }


    /**
     * @return Returns the ancestor node (or itself) of type <code>hst:componentcontainer</code> for <code>configNode</code> and <code>null</code> if
     * <code>configNode</code> does not have an ancestor of type <code>hst:componentcontainer</code>
     */
    public static Node findContainerNode(final Node configNode) throws RepositoryException {
        return findAncestorOrSelfOfType(configNode, HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT);

    }

    /**
     * @return Returns the ancestor node (or itself) of type <code>hst:configuration</code> for <code>configNode</code> and <code>null</code> if
     * <code>configNode</code> does not have an ancestor of type <code>hst:configuration</code>
     */
    public static Node findRootConfigurationNode(final Node configNode) throws RepositoryException {
        return findAncestorOrSelfOfType(configNode, HstNodeTypes.NODENAME_HST_CONFIGURATION);
    }


    /**
     * @return Returns the ancestor node (or itself) of type <code>requiredNodeType</code> for <code>configNode</code> and <code>null</code> if
     * <code>configNode</code> does not have an ancestor of type <code>requiredNodeType</code>
     */
    public static Node findAncestorOrSelfOfType(final Node configNode, final String requiredNodeType) throws RepositoryException {
        Node rootNode = configNode.getSession().getRootNode();
        Node current = configNode;
        while (true) {
            if (current.equals(rootNode)) {
                return null;
            }
            if (current.isNodeType(requiredNodeType)) {
                return current;
            }
            current = current.getParent();
        }
    }



    public static  boolean isLockedBySomeoneElse(Node configurationNode) throws RepositoryException {
        final String holder = getLockedBy(configurationNode);
        if (StringUtils.isEmpty(holder)) {
            return false;
        }
        return !configurationNode.getSession().getUserID().equals(holder);
    }

    public static  boolean isLockedBySession(Node configurationNode) throws RepositoryException {
        final String holder = getLockedBy(configurationNode);
        if (StringUtils.isEmpty(holder)) {
            return false;
        }
        return configurationNode.getSession().getUserID().equals(holder);
    }

    public static  String getLockedBy(Node configurationNode) throws RepositoryException {
        if (!configurationNode.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY)) {
            return null;
        }
        return configurationNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString();
    }

}
