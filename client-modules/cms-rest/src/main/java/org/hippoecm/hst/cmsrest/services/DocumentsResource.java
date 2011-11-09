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
package org.hippoecm.hst.cmsrest.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.hippoecm.hst.cmsrest.Implements;
import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.channel.ChannelException;
import org.hippoecm.hst.configuration.channel.ChannelManager;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.rest.DocumentService;
import org.hippoecm.hst.rest.beans.ChannelDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/documents/")
@Implements(DocumentService.class)
public class DocumentsResource {

    private static final Logger log = LoggerFactory.getLogger(DocumentsResource.class);

    private ChannelManager channelManager;
    private HstLinkCreator hstLinkCreator;

    public void setChannelManager(final ChannelManager channelManager) {
        this.channelManager = channelManager;
    }

    public void setHstLinkCreator(HstLinkCreator hstLinkCreator) {
        this.hstLinkCreator = hstLinkCreator;
    }

    @GET
    @Path("/{uuid}/channels/")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ChannelDocument> getChannels(@PathParam("uuid") String uuid, @Context HttpServletRequest servletRequest) {
        if (channelManager == null) {
            log.warn("Cannot look up channels for document '{}' because the channel manager is null", uuid);
            return Collections.emptyList();
        }
        if (hstLinkCreator == null) {
            log.warn("Cannot look up channels for document '{}' because hstLinkCreator is null", uuid);
            return Collections.emptyList();
        }

        HstRequestContext requestContext = ResourceUtil.getRequestContext(servletRequest);

        Node handle = ResourceUtil.getNode(requestContext, uuid);
        if (handle == null) {
            return Collections.emptyList();
        }

        List<HstLink> canonicalLinks = hstLinkCreator.createAllAvailableCanonicals(handle, requestContext, null, channelManager.getHostGroup());
        List<ChannelDocument> channelDocuments = new ArrayList<ChannelDocument>(canonicalLinks.size());

        for (HstLink link : canonicalLinks) {
            final Mount linkMount = link.getMount();
            final String channelPath = linkMount.getChannelPath();
            if (channelPath == null) {
                log.debug("Skipping link for mount '{}' since it does not have a channel path", linkMount.getName());
                continue;
            }

            try {
                final Channel channel = channelManager.getChannel(channelPath);
                if (channel == null) {
                    log.warn("Skipping link for mount '{}' since its channel path '{}' does not point to a channel",
                            linkMount.getName(), channelPath);
                    continue;
                }

                ChannelDocument document = new ChannelDocument();
                document.setChannelId(channel.getId());
                document.setChannelName(channel.getName());
                document.setPathInfo("/"+link.getPath());
                document.setMountPath(link.getMount().getMountPath());
                document.setHostName(link.getMount().getVirtualHost().getHostName());
                // The preview in the cms always accesses the hst site through the hostname of the cms, but 
                // adds the contextpath of the website. By default it it site, but, if a different contextpath is 
                // available for the mount that belons to  the Hstlink, we take that one.
                if (link.getMount().onlyForContextPath() != null) {
                    document.setContextPath(link.getMount().onlyForContextPath());
                } else {
                    // if there is no contextpath configured on the Mount belonging to the HstLink, then we use the contextpath 
                    // from the current HttpServletRequest
                    document.setContextPath(servletRequest.getContextPath());
                }
                
                // and set the contextpath through which the temlate composer is available
                if(link.getMount().getVirtualHost().getVirtualHosts().getDefaultContextPath() != null) {
                    document.setTemplateComposerContextPath(link.getMount().getVirtualHost().getVirtualHosts().getDefaultContextPath());
                } else {
                    document.setTemplateComposerContextPath(servletRequest.getContextPath());
                }

                // set the cmsPreviewPrefix through which prefix after the contextPath the channels can be accessed
                
                document.setCmsPreviewPrefix(link.getMount().getVirtualHost().getVirtualHosts().getCmsPreviewPrefix());
                
                channelDocuments.add(document);
            } catch (ChannelException e) {
                log.warn("Error getting channel with path '" + channelPath + "'", e);
            }
        }

        return channelDocuments;
    }

}
