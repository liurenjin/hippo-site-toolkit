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

import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import javax.security.auth.Subject;

import org.easymock.IAnswer;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.MutableMount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.RepositoryNotAvailableException;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.security.HstSubject;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.test.AbstractHstTestCase;
import org.hippoecm.repository.api.HippoNodeType;
import org.junit.After;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

public class ChannelManagerImplTest extends AbstractHstTestCase {

    private List<Mount> mounts;
    private VirtualHost testHost;
    private MutableMount testMount;

    public void setComponentManager(ComponentManager componentManager) {
        HstServices.setComponentManager(componentManager);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        HstServices.setComponentManager(null);

        for (NodeIterator ni = getSession().getNode("/hst:hst/hst:hosts/dev-localhost").getNodes("cmit-*"); ni.hasNext(); ) {
            ni.nextNode().remove();
        }
        for (NodeIterator ni = getSession().getNode("/hst:hst/hst:sites").getNodes("channel-*"); ni.hasNext(); ) {
            ni.nextNode().remove();
        }
        for (NodeIterator ni = getSession().getNode("/hst:hst/hst:sites").getNodes("cmit-*"); ni.hasNext(); ) {
            ni.nextNode().remove();
        }
        for (NodeIterator ni = getSession().getNode("/hst:hst/hst:configurations").getNodes("channel-*"); ni.hasNext(); ) {
            ni.nextNode().remove();
        }
        for (NodeIterator ni = getSession().getNode("/hst:hst/hst:configurations").getNodes("cmit-*"); ni.hasNext(); ) {
            ni.nextNode().remove();
        }
        for (NodeIterator ni = getSession().getNode("/hst:hst/hst:channels").getNodes("cmit-*"); ni.hasNext(); ) {
            ni.nextNode().remove();
        }
        for (NodeIterator ni = getSession().getNode("/hst:hst/hst:blueprints").getNodes("cmit-*"); ni.hasNext(); ) {
            ni.nextNode().remove();
        }
        getSession().save();

        super.tearDown();
    }

    @Test
    public void createUniqueChannelId() throws RepositoryException, ChannelException {
        ChannelManagerImpl manager = createManager();

        assertEquals("test", manager.createUniqueChannelId("test"));
        assertEquals("name-with-spaces", manager.createUniqueChannelId("Name with Spaces"));
        assertEquals("special-characters--and---and", manager.createUniqueChannelId("Special Characters: % and / and []"));
        assertEquals("'testchannel' already exists in the default unit test content, so the new channel ID should get a suffix",
                "testchannel-1", manager.createUniqueChannelId("testchannel"));

    }

    @Test
    public void channelsAreReadCorrectly() throws ChannelException, RepositoryException {
        ChannelManagerImpl manager = createManager();

        Map<String, Channel> channels = manager.getChannels();
        assertEquals(1, channels.size());
        assertEquals("testchannel", channels.keySet().iterator().next());

        Channel channel = channels.values().iterator().next();
        assertEquals("testchannel", channel.getId());
        assertEquals("Test Channel", channel.getName());
        assertEquals("en_EN", channel.getLocale());
    }

    @Test
    public void channelsAreClonedWhenRetrieved() throws ChannelException, RepositoryException {
        ChannelManagerImpl manager = createManager();

        Map<String, Channel> channels = manager.getChannels();
        Channel channel = channels.values().iterator().next();
        channel.setName("aap");

        Map<String, Channel> moreChannels = manager.getChannels();
        assertFalse(moreChannels == channels);
        assertEquals("Test Channel", moreChannels.values().iterator().next().getName());
    }

    @Test
    public void channelIsCreatedFromBlueprint() throws ChannelException, RepositoryException, PrivilegedActionException {
        final ChannelManagerImpl manager = createManager();

        List<Blueprint> bluePrints = manager.getBlueprints();
        assertEquals(1, bluePrints.size());
        final Blueprint blueprint = bluePrints.get(0);

        final Channel channel = blueprint.createChannel();
        channel.setName("CMIT Test Channel: with special and/or specific characters");
        channel.setUrl("http://cmit-myhost");
        channel.setContentRoot("/unittestcontent/documents");
        channel.setLocale("nl_NL");

        String channelId = HstSubject.doAsPrivileged(createSubject(), new PrivilegedExceptionAction<String>() {
            @Override
            public String run() throws ChannelException {
                return manager.persist(blueprint.getId(), channel);
            }
        }, null);
        final String encodedChannelName = "cmit-test-channel-with-special-and-or-specific-characters";
        assertEquals(encodedChannelName, channelId);

        Node channelNode = getSession().getNode("/hst:hst/hst:channels/" + channelId);
        assertEquals("CMIT Test Channel: with special and/or specific characters", channelNode.getProperty("hst:name").getString());

        Node hostNode = getSession().getNode("/hst:hst/hst:hosts/dev-localhost");
        assertTrue(hostNode.hasNode("cmit-myhost/hst:root"));

        Node mountNode = hostNode.getNode("cmit-myhost/hst:root");
        assertEquals("nl_NL", mountNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCALE).getString());
        String sitePath = "/hst:hst/hst:sites/" + channelId;
        assertEquals(sitePath, mountNode.getProperty(HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT).getString());
        assertTrue(getSession().itemExists(sitePath));

        Node siteNode = getSession().getNode(sitePath);
        assertEquals("/hst:hst/hst:configurations/" + channelId, siteNode.getProperty(HstNodeTypes.SITE_CONFIGURATIONPATH).getString());
        assertEquals(getSession().getNode("/unittestcontent/documents").getUUID(),
                siteNode.getNode(HstNodeTypes.NODENAME_HST_CONTENTNODE).getProperty(HippoNodeType.HIPPO_DOCBASE).getString());
    }

    @Test
    public void channelsAreReloaded() throws ChannelException, RepositoryException, PrivilegedActionException, RepositoryNotAvailableException {
        final ChannelManagerImpl manager = createManager();
        int numberOfChannels = manager.getChannels().size();

        Node channelsNode = getSession().getNode("/hst:hst/hst:channels");
        Node channel = channelsNode.addNode("cmit-test-channel", "hst:channel");
        channel.setProperty("hst:name", "CMIT Test Channel");
        getSession().save();

        manager.invalidate();

        // manager should reload

        reset(testHost, testMount);
        

        VirtualHosts vhosts = HstServices.getComponentManager().getComponent(VirtualHosts.class.getName());
        expectMountLoad(testHost, testMount, vhosts);

        MutableMount newMount = createNiceMock(MutableMount.class);
        mounts.add(newMount);
        
        VirtualHost newHost = createNiceMock(VirtualHost.class);
        
        expect(newMount.getChannelPath()).andReturn("/hst:hst/hst:channels/cmit-test-channel").anyTimes();
        expect(newMount.getMountPoint()).andReturn("mountpoint").anyTimes();
        expect(newMount.getHstSite()).andReturn(createNiceMock(HstSite.class)).anyTimes();
        expect(newMount.getCanonicalContentPath()).andReturn("/unittestcontent/documents");
        expect(newMount.getVirtualHost()).andReturn(newHost).anyTimes();
        expect(newHost.getHostName()).andReturn("myhost").anyTimes();
        expect(newHost.getVirtualHosts()).andReturn(vhosts).anyTimes();
        expect(newMount.getLocale()).andReturn("nl_NL").anyTimes();
        expect(newMount.getScheme()).andReturn("http").anyTimes();
        expect(newMount.getMountPath()).andReturn("").anyTimes();

        replay(testHost, testMount, newHost, newMount);
      
        Map<String, Channel> channels = manager.getChannels();

        // verify reload
        verify(testHost, testMount, newHost, newMount);

        assertEquals(numberOfChannels + 1, channels.size());
        assertTrue(channels.containsKey("cmit-test-channel"));

        Channel created = channels.get("cmit-test-channel");
        assertNotNull(created);
        assertEquals("cmit-test-channel", created.getId());
        assertEquals("CMIT Test Channel", created.getName());
        assertEquals("http://myhost", created.getUrl());
        assertEquals("/unittestcontent/documents", created.getContentRoot());
        assertEquals("nl_NL", created.getLocale());
    }

    @Test
    public void ancestorMountsMustExist() throws ChannelException, RepositoryException, PrivilegedActionException {
        final ChannelManagerImpl manager = createManager();

        List<Blueprint> bluePrints = manager.getBlueprints();
        assertEquals(1, bluePrints.size());
        final Blueprint blueprint = bluePrints.get(0);

        final Channel channel = blueprint.createChannel();
        channel.setName("cmit-channel");
        channel.setUrl("http://cmit-myhost/newmount");

        try {
            HstSubject.doAsPrivileged(createSubject(), new PrivilegedExceptionAction<String>() {
                @Override
                public String run() throws ChannelException {
                    return manager.persist(blueprint.getId(), channel);
                }
            }, null);
            fail("Expected a " + MountNotFoundException.class.getName());
        } catch (PrivilegedActionException e) {
            assertEquals(MountNotFoundException.class, e.getCause().getClass());
        }
    }

    public static interface TestInfoClass extends ChannelInfo {
        @Parameter(name="getme", defaultValue = "aap")
        String getGetme();
    }

    @Test
    public void blueprintDefaultValuesAreCopied() throws RepositoryException, ChannelException, PrivilegedActionException {
        Node configNode = getSession().getRootNode().getNode("hst:hst");
        Node bpFolder = configNode.getNode(HstNodeTypes.NODENAME_HST_BLUEPRINTS);

        Node bp = bpFolder.addNode("cmit-test-bp", HstNodeTypes.NODETYPE_HST_BLUEPRINT);
        bp.addNode(HstNodeTypes.NODENAME_HST_CONFIGURATION, HstNodeTypes.NODETYPE_HST_CONFIGURATION);
        Node channelBlueprint = bp.addNode(HstNodeTypes.NODENAME_HST_CHANNEL, HstNodeTypes.NODETYPE_HST_CHANNEL);
        channelBlueprint.setProperty(HstNodeTypes.CHANNEL_PROPERTY_CHANNELINFO_CLASS, TestInfoClass.class.getName());
        Node defaultChannelInfo = channelBlueprint.addNode(HstNodeTypes.NODENAME_HST_CHANNELINFO, HstNodeTypes.NODETYPE_HST_CHANNELINFO);
        defaultChannelInfo.setProperty("getme", "noot");
        getSession().save();

        final ChannelManagerImpl manager = createManager();

        final Channel channel = manager.getBlueprint("cmit-test-bp").createChannel();
        channel.setName("cmit-channel");
        channel.setUrl("http://cmit-myhost");
        Map<String, Object> properties = channel.getProperties();
        assertTrue(properties.containsKey("getme"));
        assertEquals("noot", properties.get("getme"));

        HstSubject.doAsPrivileged(createSubject(), new PrivilegedExceptionAction<String>() {
            @Override
            public String run() throws ChannelException {
                return manager.persist("cmit-test-bp", channel);
            }
        }, null);
        TestInfoClass channelInfo = manager.getChannelInfo(channel);
        assertEquals("noot", channelInfo.getGetme());
    }

    private Subject createSubject() {
        Subject subject = new Subject();
        subject.getPrivateCredentials().add(new SimpleCredentials("admin", "admin".toCharArray()));
        subject.setReadOnly();
        return subject;
    }

    private ChannelManagerImpl createManager() throws ChannelException {

        final ChannelManagerImpl manager = new ChannelManagerImpl();
        manager.setRepository(getRepository());
        // FIXME: use readonly credentials
        manager.setCredentials(new SimpleCredentials("admin", "admin".toCharArray()));
        manager.setHostGroup("dev-localhost");
        manager.setSites("hst:sites");

        ComponentManager cm = createMock(ComponentManager.class);
        setComponentManager(cm);

        final VirtualHosts vhosts = createNiceMock(VirtualHosts.class);
        expect(vhosts.getCmsPreviewPrefix()).andReturn("_cmsinternal").anyTimes();
        HstManager hstMgr = createNiceMock(HstManager.class);
        try {
            expect(hstMgr.getVirtualHosts()).andAnswer(new IAnswer<VirtualHosts>() {
                @Override
                public VirtualHosts answer() throws Throwable {
                    manager.load(vhosts);
                    return vhosts;
                }
            }).anyTimes();
        } catch (RepositoryNotAvailableException e) {
            // mock impl doesn't throw
        }
        expect(cm.getComponent(HstManager.class.getName())).andReturn(hstMgr).anyTimes();
        expect(cm.getComponent(VirtualHosts.class.getName())).andReturn(vhosts).anyTimes();

        mounts = new LinkedList<Mount>();
        expect(vhosts.getMountsByHostGroup("dev-localhost")).andReturn(mounts).anyTimes();
        expect(vhosts.getHostGroupNames()).andReturn(Arrays.asList("dev-localhost")).anyTimes();
        
        testHost = createNiceMock(VirtualHost.class);
        
        testMount = createNiceMock(MutableMount.class);
        mounts.add(testMount);

        expectMountLoad(testHost, testMount, vhosts);

        replay(vhosts, cm, hstMgr, testHost, testMount);

        manager.load();

        verify(testHost, testMount);

        return manager;
    }

    private static void expectMountLoad(final VirtualHost host, final MutableMount mount, VirtualHosts vhosts) {
        expect(host.getHostName()).andReturn("localhost").anyTimes();
        expect(host.getVirtualHosts()).andReturn(vhosts).anyTimes();
        
        expect(mount.getMountPoint()).andReturn("mountpoint");
        expect(mount.getHstSite()).andReturn(createNiceMock(HstSite.class));
        expect(mount.getCanonicalContentPath()).andReturn("/content/documents");

        expect(mount.getChannelPath()).andReturn("/hst:hst/hst:channels/testchannel");
        expect(mount.getVirtualHost()).andReturn(host).anyTimes();
        expect(mount.getLocale()).andReturn("en_EN");
        expect(mount.getScheme()).andReturn("http");
        expect(mount.getMountPath()).andReturn("");
    }

}
