/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.hst.pagecomposer.jaxrs.services;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.channel.ChannelException;
import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.configuration.channel.ChannelManager;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.jcr.RuntimeRepositoryException;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ChannelInfoDescription;
import org.hippoecm.hst.rest.beans.ChannelInfoClassInfo;
import org.hippoecm.hst.rest.beans.InformationObjectsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelServiceImpl implements ChannelService {
    private static final Logger log = LoggerFactory.getLogger(ChannelServiceImpl.class);

    private ChannelManager channelManager;

    @Override
    public ChannelInfoDescription getChannelInfoDescription(final String channelId, final String locale) throws ChannelException {
        try {
            Class<? extends ChannelInfo> channelInfoClass = getAllVirtualHosts().getChannelInfoClass(getCurrentVirtualHost().getHostGroupName(), channelId);

            if (channelInfoClass == null) {
                throw new ChannelException("Cannot find ChannelInfo class of the channel with id '" + channelId + "'");
            }
            final ChannelInfoClassInfo channelInfoClassInfo = InformationObjectsBuilder.buildChannelInfoClassInfo(channelInfoClass);
            return new ChannelInfoDescription(channelInfoClassInfo.getFieldGroups(), getLocalizedResources(channelId, locale));
        } catch (ChannelException e) {
            if (log.isDebugEnabled()) {
                log.info("Failed to retrieve channel info class for channel with id '{}'", channelId, e);
            } else {
                log.info("Failed to retrieve channel info class for channel with id '{}'", channelId, e.toString());
            }
            throw e;
        }
    }

    private Map<String, String> getLocalizedResources(final String channelId, final String language) {
        final ResourceBundle resourceBundle = getAllVirtualHosts().getResourceBundle(getChannel(channelId), new Locale(language));
        if (resourceBundle == null) {
            return Collections.EMPTY_MAP;
        }

        return resourceBundle.keySet().stream()
                .collect(Collectors.toMap(Function.identity(), resourceBundle::getString));
    }

    @Override
    public Channel getChannel(final String channelId) {
        final VirtualHost virtualHost = getCurrentVirtualHost();
        return getAllVirtualHosts().getChannelById(virtualHost.getHostGroupName(), channelId);
    }

    @Override
    public void saveChannel(Session session, final String channelId, Channel channel) throws RepositoryException, IllegalStateException, ChannelException {
        String currentHostGroupName = getCurrentVirtualHost().getHostGroupName();

        this.channelManager.save(currentHostGroupName, channel);
    }

    @Override
    public List<Channel> getChannels(final boolean previewConfigRequired, final boolean workspaceRequired) {
        final VirtualHost virtualHost = getCurrentVirtualHost();
        return virtualHost.getVirtualHosts().getChannels(virtualHost.getHostGroupName())
                .values()
                .stream()
                .filter(channel -> previewConfigRequiredFiltered(channel, previewConfigRequired))
                .filter(channel -> workspaceFiltered(channel, workspaceRequired))
                .collect(Collectors.toList());
    }

    private Node getOrAddChannelPropsNode(final Node channelNode) throws RepositoryException {
        if (!channelNode.hasNode(HstNodeTypes.NODENAME_HST_CHANNELINFO)) {
            return channelNode.addNode(HstNodeTypes.NODENAME_HST_CHANNELINFO, HstNodeTypes.NODETYPE_HST_CHANNELINFO);
        } else {
            return channelNode.getNode(HstNodeTypes.NODENAME_HST_CHANNELINFO);
        }
    }

    private VirtualHost getCurrentVirtualHost() {
        return RequestContextProvider.get().getResolvedMount().getMount().getVirtualHost();
    }

    private VirtualHosts getAllVirtualHosts() {
        return RequestContextProvider.get().getVirtualHost().getVirtualHosts();
    }

    private boolean previewConfigRequiredFiltered(final Channel channel, final boolean previewConfigRequired) {
        return !previewConfigRequired || channel.isPreview();
    }

    private boolean workspaceFiltered(final Channel channel, final boolean required) throws RuntimeRepositoryException {
        if (!required) {
            return true;
        }
        final Mount mount = getAllVirtualHosts().getMountByIdentifier(channel.getMountId());
        final String workspacePath = mount.getHstSite().getConfigurationPath() + "/" + HstNodeTypes.NODENAME_HST_WORKSPACE;
        try {
            return RequestContextProvider.get().getSession().nodeExists(workspacePath);
        } catch (RepositoryException e) {
            throw new RuntimeRepositoryException(e);
        }
    }

    public void setChannelManager(final ChannelManager channelManager) {
        this.channelManager = channelManager;
    }
}
