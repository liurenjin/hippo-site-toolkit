/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.hst.cmsrest.AbstractCmsRestTest;
import org.hippoecm.hst.configuration.model.HstManagerImpl;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.Test;
import org.onehippo.cms7.services.hst.Channel;

import static org.hippoecm.hst.cmsrest.container.CmsRestSecurityValve.HOST_GROUP_NAME_FOR_CMS_HOST;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_INHERITS_FROM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ChannelResourceTest extends AbstractCmsRestTest {

    protected void initRequest() throws Exception {
        HstRequestContext requestContext = getRequestFromCms("127.0.0.1:8080", "/_cmsrest");
        ModifiableRequestContextProvider.set(requestContext);
        RequestContextProvider.get().setAttribute(HOST_GROUP_NAME_FOR_CMS_HOST, "dev-localhost");
    }

    @Test
    public void get_channels_from_channels_resource_only_returns_channels_for_current_context_path() throws Exception {
        // by default 'servletContext2' for /site2 is set, see AbstractCmsRestTest. If we set it
        // now below before the HST model is loaded we can load the channels for /site
        ((HstManagerImpl)hstManager).setServletContext(servletContext);
        initRequest();
        final ChannelsResource channelsResource = new ChannelsResource();
        List<Channel> channels = channelsResource.getChannels();
        assertEquals(2, channels.size());
        for (Channel channel : channels) {
            assertFalse(channel.isPreview());
            assertFalse(channel.isPreviewHstConfigExists());
            assertTrue(channel.getName().equals("Test Channel") || channel.getName().equals("Test Sub Channel"));
        }
    }

    @Test
    public void get_channels_from_channels_resource_only_returns_channels_for_current_context_path_2() throws Exception {
        // by default 'servletContext2' for /site2 is set, see AbstractCmsRestTest which means contextpath /site2
        initRequest();
        final ChannelsResource channelsResource = new ChannelsResource();
        List<Channel> channels = channelsResource.getChannels();
        assertEquals(1, channels.size());
        Channel channel = channels.get(0);
        assertFalse(channel.isPreview());
        assertFalse(channel.isPreviewHstConfigExists());
        assertEquals("Intranet Test Channel", channel.getName());
    }

    /**
     * This test confirms that if there is a live and preview configuration, always the 'preview' channel is returned.
     * If not, the branch select dropdown in channel manager does not behave correctly (at least as of 12.0.0)
     */
    @Test
    public void get_channels_from_channels_resource_returns_preview_channel_if_preview_exists() throws Exception {
        Session session = createSession();
        try {
            JcrUtils.copy(session, "/hst:hst/hst:configurations/unittestproject", "/hst:hst/hst:configurations/unittestproject-preview");
            final Node previewConfig = session.getNode("/hst:hst/hst:configurations/unittestproject-preview");
            previewConfig.setProperty(GENERAL_PROPERTY_INHERITS_FROM, new String[]{"../unittestproject"});
            session.save();

            // by default 'servletContext2' for /site2 is set, see AbstractCmsRestTest. If we set it
            // now below before the HST model is loaded we can load the channels for /site
            ((HstManagerImpl)hstManager).setServletContext(servletContext);
            initRequest();
            final ChannelsResource channelsResource = new ChannelsResource();
            List<Channel> channels = channelsResource.getChannels();
            assertEquals(2, channels.size());
            for (Channel channel : channels) {
                if (channel.getId().equals("unittestproject-preview")) {
                    assertTrue(channel.isPreview());
                    assertTrue(channel.isPreviewHstConfigExists());
                    assertEquals("Test Channel", channel.getName());
                } else {
                    assertFalse(channel.isPreview());
                    assertFalse(channel.isPreviewHstConfigExists());
                    assertEquals("Test Sub Channel", channel.getName());
                }
            }

        } finally {
            session.getNode("/hst:hst/hst:configurations/unittestproject-preview").remove();
            session.save();
            session.logout();
        }
    }
}
