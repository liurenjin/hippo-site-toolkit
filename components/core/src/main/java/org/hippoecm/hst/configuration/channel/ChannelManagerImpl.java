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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelManagerImpl implements ChannelManager {

    static final Logger log = LoggerFactory.getLogger(ChannelManagerImpl.class.getName());

    private String rootPath = "/hst:hst";

    private int lastChannelId;
    private Map<String, BlueprintService> blueprints;
    private Map<String, Channel> channels;
    private Credentials credentials;
    private Repository repository;

    private String hostGroup = "dev-localhost";

    private String sites = "hst:sites";

    public ChannelManagerImpl() {
    }

    public void setCredentials(final Credentials credentials) {
        this.credentials = credentials;
    }

    public void setRepository(final Repository repository) {
        this.repository = repository;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public void setHostGroup(String hostGroup) {
        this.hostGroup = hostGroup;
    }

    public void setSites(final String sites) {
        this.sites = sites;
    }

    private void loadBlueprints(final Node configNode) throws RepositoryException {
        if (configNode.hasNode(HstNodeTypes.NODENAME_HST_BLUEPRINTS)) {
            Node blueprintsNode = configNode.getNode(HstNodeTypes.NODENAME_HST_BLUEPRINTS);
            NodeIterator blueprintIterator = blueprintsNode.getNodes();
            while (blueprintIterator.hasNext()) {
                Node blueprint = blueprintIterator.nextNode();
                blueprints.put(blueprint.getName(), new BlueprintService(blueprint));
            }
        }
    }

    private void loadChannels(final Node configNode) throws RepositoryException {
        Node virtualHosts = configNode.getNode("hst:hosts/" + hostGroup);
        NodeIterator rootChannelNodes = virtualHosts.getNodes();
        while (rootChannelNodes.hasNext()) {
            Node hgNode = rootChannelNodes.nextNode();
            populateChannels(hgNode);
        }
    }

    /**
     * Recursively gets the list of "channels" configured under a virtual host node.
     * <p/>
     * Ignores the mounts which are configured to be "rest" or "composer" either in hst:type or hst:types.
     *
     * @param node - the inital node to start with, must be a virtual host node.
     * @throws javax.jcr.RepositoryException - In case cannot read required node/property from the repository.
     */
    private void populateChannels(Node node) throws RepositoryException {
        NodeIterator nodes = node.getNodes();
        while (nodes.hasNext()) {
            Channel channel = null;
            Node currNode = nodes.nextNode();

            //Get the channels from the child node.
            populateChannels(currNode);

            if (!currNode.isNodeType(HstNodeTypes.NODETYPE_HST_MOUNT)) {
                continue;
            }

            if (!currNode.hasProperty(HstNodeTypes.MOUNT_PROPERTY_CHANNELID)) {
                continue;
            }

            String id = currNode.getProperty(HstNodeTypes.MOUNT_PROPERTY_CHANNELID).getString();
            String bluePrintId = null;
            if (currNode.hasProperty(HstNodeTypes.MOUNT_PROPERTY_BLUEPRINTID)) {
                bluePrintId = currNode.getProperty(HstNodeTypes.MOUNT_PROPERTY_BLUEPRINTID).getString();
            }
            if (channels.containsKey(id)) {
                channel = channels.get(id);
                if (!channel.getBlueprintId().equals(bluePrintId)) {
                    log.warn("Channel found with id " + id + " that has a different blueprint id; " + "expected " + channel.getBlueprintId() + ", found " + bluePrintId + ".  Ignoring mount");
                }
            } else {
                channel = new Channel(bluePrintId, id);
                channels.put(id, channel);
            }

            if (currNode.hasProperty(HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT)) {
                String mountPoint = currNode.getProperty(HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT).getString();
                Node siteNode = currNode.getSession().getNode(mountPoint);
                if (siteNode.hasProperty(HstNodeTypes.SITE_CONFIGURATIONPATH)) {
                    channel.setHstConfigPath(siteNode.getProperty(HstNodeTypes.SITE_CONFIGURATIONPATH).getString());
                }
                Node contentNode = siteNode.getNode(HstNodeTypes.NODENAME_HST_CONTENTNODE);
                if (contentNode.hasProperty(HippoNodeType.HIPPO_DOCBASE)) {
                    String siteDocbase = contentNode.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
                    String contentRoot = contentNode.getSession().getNodeByIdentifier(siteDocbase).getPath();
                    channel.setContentRoot(contentRoot);
                }
            }
        }
    }

    private void load() throws ChannelException {
        if (channels == null) {
            Session session = null;
            try {
                session = getSession();
                Node configNode = null;
                configNode = session.getNode(rootPath);

                channels = new HashMap<String, Channel>();
                blueprints = new HashMap<String, BlueprintService>();
                loadChannels(configNode);
                loadBlueprints(configNode);
            } catch (RepositoryException e) {
                throw new ChannelException("Could not load channels and/or blueprints", e);
            } finally {
                if (session != null) {
                    session.logout();
                }
            }
        }
    }

    protected String nextChannelId() {
        while (channels.containsKey("channel-" + lastChannelId)) {
            lastChannelId++;
        }
        return "channel-" + lastChannelId;
    }

    protected Session getSession() throws RepositoryException {
        javax.jcr.Session session = null;

        if (this.credentials == null) {
            session = this.repository.login();
        } else {
            session = this.repository.login(this.credentials);
        }

        // session can come from a pooled event based pool so always refresh before building configuration:
        session.refresh(false);

        return session;
    }

    // PUBLIC interface; all synchronised to guarantee consistent state

    @Override
    public synchronized Map<String, Channel> getChannels() throws ChannelException {
        load();
        return Collections.unmodifiableMap(channels);
    }

    @Override
    public synchronized Channel createChannel(final String blueprintId) throws ChannelException {
        load();
        if (!blueprints.containsKey(blueprintId)) {
            throw new ChannelException("Blue print id " + blueprintId + " is not valid");
        }
        return new Channel(blueprintId, nextChannelId());
    }

    @Override
    public synchronized void save(final Channel channel) throws ChannelException {
        Session session = null;
        try {
            session = getSession();
            Node configNode = session.getNode(rootPath);
            if (channels.containsKey(channel.getId())) {
                channels.clear();
                loadChannels(configNode);

                if (channels.containsKey(channel.getId())) {
                    Channel previous = channels.get(channel.getId());
                    if (!previous.getBlueprintId().equals(channel.getBlueprintId())) {
                        throw new ChannelException("Cannot change channel to new blue print");
                    }

                    // TODO: validate that mandatory properties (URL and such) have not changed
                } else {
                    throw new ChannelException("Channel was removed since it's retrieval");
                }
            } else {
                BlueprintService bps = blueprints.get(channel.getBlueprintId());
                if (bps == null) {
                    throw new ChannelException("Invalid blueprint ID " + channel.getBlueprintId());
                }

                Node blueprintNode = bps.getNode(session);
                createChannelFromBlueprint(configNode, blueprintNode, channel);
            }
            session.save();
        } catch (RepositoryException e) {
            throw new ChannelException("Unable to save channel to the repository", e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    void validateChannel(Channel channel) {
        /*
        - check URL is valid
        - check ID is valid
         */
    }

    private void createChannelFromBlueprint(Node configRoot, final Node blueprintNode, final Channel channel) throws ChannelException, RepositoryException {
        String tmp = channel.getUrl();
        tmp = tmp.substring("http://".length());
        String mountPath = tmp.substring(tmp.indexOf('/') + 1);
        while (mountPath.lastIndexOf('/') == mountPath.length() - 1) {
            mountPath = mountPath.substring(0, mountPath.lastIndexOf('/'));
        }
        String domainEls = tmp.substring(0, tmp.indexOf('/'));

        // create virtual host
        Node parent = configRoot.getNode("hst:hosts/" + hostGroup);
        String[] elements = domainEls.split("[.]");
        for (int i = elements.length - 1; i >= 0; i--) {
            if (parent.hasNode(elements[i])) {
                parent = parent.getNode(elements[i]);
            } else {
                parent = parent.addNode(elements[i], "hst:virtualhost");
            }
        }

        // create mounts
        String[] mountPathEls = mountPath.split("/");
        if (mountPathEls.length > 0) {
            if (parent.hasNode("hst:root")) {
                parent = parent.getNode("hst:root");
            } else {
                parent = parent.addNode("hst:root", "hst:mount");
            }
            for (int i = 0; i < mountPathEls.length - 1; i++) {
                if (parent.hasNode(mountPathEls[i])) {
                    parent = parent.getNode(mountPathEls[i]);
                } else {
                    parent = parent.addNode(mountPathEls[i], "hst:mount");
                }
            }
            copyNodes(blueprintNode.getNode("hst:mount"), parent, mountPathEls[mountPathEls.length - 1]);
        } else {
            copyNodes(blueprintNode.getNode("hst:mount"), parent, "hst:root");
        }

        copyNodes(blueprintNode.getNode("hst:site"), configRoot.getNode(sites), channel.getId());
        copyNodes(blueprintNode.getNode("hst:configuration"), configRoot.getNode("hst:configurations"), channel.getId());
    }

    static void copyNodes(Node source, Node parent, String name) throws RepositoryException {
        Node clone = parent.addNode(name, source.getPrimaryNodeType().getName());
        for (PropertyIterator pi = source.getProperties(); pi.hasNext(); ) {
            Property prop = pi.nextProperty();
            if (prop.getDefinition().isProtected()) {
                continue;
            }
            if (prop.isMultiple()) {
                clone.setProperty(prop.getName(), prop.getValues());
            } else {
                clone.setProperty(prop.getName(), prop.getValue());
            }
        }
        for (NodeIterator ni = source.getNodes(); ni.hasNext(); ) {
            Node node = ni.nextNode();
            copyNodes(node, clone, node.getName());
        }
    }

    @Override
    public synchronized List<Blueprint> getBlueprints() throws ChannelException {
        load();
        return new ArrayList<Blueprint>(blueprints.values());
    }

    @Override
    public synchronized Blueprint getBlueprint(final String id) throws ChannelException {
        load();
        if (!blueprints.containsKey(id)) {
            throw new ChannelException("Blueprint " + id + " does not exist");
        }
        return blueprints.get(id);
    }

    public synchronized void invalidate() {
        channels = null;
        blueprints = null;
    }
}
