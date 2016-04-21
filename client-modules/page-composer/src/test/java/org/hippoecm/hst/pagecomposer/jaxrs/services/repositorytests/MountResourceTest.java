/*
 * Copyright 2013-2016 Hippo B.V. (http://www.onehippo.com)
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
 */
package org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.Response;

import com.google.common.eventbus.Subscribe;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.api.ChannelEvent;
import org.hippoecm.hst.pagecomposer.jaxrs.cxf.CXFJaxrsHstConfigService;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ExtResponseRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.ContainerComponentResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.ContainerComponentService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.ContainerComponentServiceImpl;
import org.hippoecm.hst.pagecomposer.jaxrs.services.MountResourceAccessor;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.ContainerHelper;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.repository.util.NodeIterable;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MountResourceTest extends AbstractMountResourceTest {

    @Test
    public void testEditAndPublishMount() throws Exception {

        movePagesFromCommonToUnitTestProject();
        createWorkspaceWithTestContainer();
        addReferencedContainerForHomePage();
        String catalogItemUUID = addCatalogItem();
        session.save();
        // give time for jcr events to evict model
        Thread.sleep(200);

        mockNewRequest(session, "localhost", "");

        final PageComposerContextService pccs = mountResource.getPageComposerContextService();
        final HstRequestContext ctx = pccs.getRequestContext();

        final String previewConfigurationPath = ctx.getResolvedMount().getMount().getHstSite().getConfigurationPath() + "-preview";
        assertFalse("Preview config node should not exist yet.",
                session.nodeExists(previewConfigurationPath));


        mountResource.startEdit();

        assertTrue("Live config node should exist",
                session.nodeExists(ctx.getResolvedMount().getMount().getHstSite().getConfigurationPath()));
        assertTrue("Preview config node should exist",
                session.nodeExists(previewConfigurationPath));


        assertTrue("Live channel path node should exist",
                session.nodeExists(ctx.getResolvedMount().getMount().getChannelPath()));
        assertTrue("Preview channel path node should exist",
                session.nodeExists(ctx.getResolvedMount().getMount().getChannelPath() + "-preview"));

        // reload model through new request, and then modify a container
        // give time for jcr events to evict model
        Thread.sleep(200);

        mockNewRequest(session, "localhost", "");

        assertTrue(pccs.getEditingPreviewSite().getConfigurationPath().equals(pccs.getEditingLiveConfigurationPath() + "-preview"));
        assertTrue(pccs.getEditingPreviewConfigurationPath().equals(pccs.getEditingLiveConfigurationPath() + "-preview"));
        assertTrue(pccs.getEditingPreviewChannel().getHstConfigPath().equals(pccs.getEditingPreviewSite().getConfigurationPath()));
        assertEquals(0, pccs.getEditingPreviewChannel().getChangedBySet().size());

        assertTrue(pccs.getEditingPreviewChannel().getId().endsWith("-preview"));
        assertTrue(pccs.getEditingPreviewChannel().getId().equals(pccs.getEditingMount().getChannel().getId()));

        final String previewContainerNodeUUID = session.getNode(previewConfigurationPath)
                .getNode("hst:workspace/hst:containers/testcontainer").getIdentifier();
        pccs.getRequestContext().setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, previewContainerNodeUUID);

        // there should be not yet any locks
        Set<String> changedBySet = mountResource.getPageComposerContextService().getEditingPreviewChannel().getChangedBySet();
        assertTrue(changedBySet.isEmpty());

        final ContainerComponentResource containerComponentResource = createContainerResource();
        final Response response = containerComponentResource.createContainerItem(catalogItemUUID, 0);
        assertEquals("New container item should be created", Response.Status.CREATED.getStatusCode(), response.getStatus());

        // reload model through new request, and then modify a container
        // give time for jcr events to evict model
        Thread.sleep(200);

        mockNewRequest(session, "localhost", "/home");

        changedBySet = pccs.getEditingPreviewChannel().getChangedBySet();
        assertTrue(changedBySet.contains("admin"));

        mountResource.publish();

        // reload model through new request, and then modify a container
        // give time for jcr events to evict model
        Thread.sleep(200);

        mockNewRequest(session, "localhost", "/home");

        // there should be no locks
        changedBySet = pccs.getEditingPreviewChannel().getChangedBySet();
        assertTrue(changedBySet.isEmpty());

    }

    protected ContainerComponentResource createContainerResource() {
        final ContainerComponentResource containerComponentResource = new ContainerComponentResource();
        final PageComposerContextService pageComposerContextService = mountResource.getPageComposerContextService();

        final ContainerHelper helper = new ContainerHelper();
        helper.setPageComposerContextService(pageComposerContextService);

        final ContainerComponentService containerComponentService = new ContainerComponentServiceImpl(pageComposerContextService, helper);
        containerComponentResource.setContainerComponentService(containerComponentService);
        containerComponentResource.setPageComposerContextService(pageComposerContextService);
        return containerComponentResource;
    }

    @Test
    public void testXpathQueries() {

        assertEquals("/jcr:root/hst:hst/hst:configurations/myproject-preview//element(*,hst:containercomponent)[@hst:lockedby = 'admin' or @hst:lockedby = 'editor']",
                MountResourceAccessor.buildXPathQueryToFindContainersForUsers("/hst:hst/hst:configurations/myproject-preview", Arrays.asList(new String[]{"admin", "editor"})));
        assertEquals("/jcr:root/hst:hst/hst:configurations/_x0037__8-preview//element(*,hst:containercomponent)[@hst:lockedby = 'admin' or @hst:lockedby = 'editor']",
                MountResourceAccessor.buildXPathQueryToFindContainersForUsers("/hst:hst/hst:configurations/7_8-preview", Arrays.asList(new String[]{"admin", "editor"})));

        assertEquals("/jcr:root/hst:hst/hst:configurations/myproject-preview/*[@hst:lockedby = 'admin' or @hst:lockedby = 'editor']",
                MountResourceAccessor.buildXPathQueryToFindMainfConfigNodesForUsers("/hst:hst/hst:configurations/myproject-preview", Arrays.asList(new String[]{"admin", "editor"})));
        assertEquals("/jcr:root/hst:hst/hst:configurations/_x0037__8-preview/*[@hst:lockedby = 'admin' or @hst:lockedby = 'editor']",
                MountResourceAccessor.buildXPathQueryToFindMainfConfigNodesForUsers("/hst:hst/hst:configurations/7_8-preview", Arrays.asList(new String[]{"admin", "editor"})));
    }


    @Test
    public void testEditAndPublishProjectThatStartsWithNumber() throws Exception {

        movePagesFromCommonToUnitTestProject();
        createWorkspaceWithTestContainer();
        addReferencedContainerForHomePage();
        String catalogItemUUID = addCatalogItem();

        session.move("/hst:hst/hst:configurations/unittestproject", "/hst:hst/hst:configurations/7_8");
        // change default unittestproject site to map to /hst:hst/hst:configurations/7_8
        Node testSideNode = session.getNode("/hst:hst/hst:sites/unittestproject");
        testSideNode.setProperty("hst:configurationpath", "/hst:hst/hst:configurations/7_8");

        session.save();
        // give time for jcr events to evict model
        Thread.sleep(200);

        final PageComposerContextService pccs = mountResource.getPageComposerContextService();
        mockNewRequest(session, "localhost", "");

        assertEquals("/hst:hst/hst:configurations/7_8", pccs.getRequestContext().getResolvedMount().getMount().getHstSite().getConfigurationPath());

        mountResource.startEdit();

        // reload model through new request, and then modify a container
        // give time for jcr events to evict model
        Thread.sleep(200);

        mockNewRequest(session, "localhost", "");

        final String previewContainerNodeUUID = session.getNode(pccs.getEditingPreviewSite().getConfigurationPath())
                .getNode("hst:workspace/hst:containers/testcontainer").getIdentifier();

        pccs.getRequestContext().setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, previewContainerNodeUUID);

        // there should be not yet any locks
        Set<String> usersWithLockedContainers = pccs.getEditingPreviewChannel().getChangedBySet();
        assertTrue(usersWithLockedContainers.isEmpty());

        final ContainerComponentResource containerComponentResource = createContainerResource();
        final Response response = containerComponentResource.createContainerItem(catalogItemUUID, 0);
        assertEquals("New container item should be created", Response.Status.CREATED.getStatusCode(), response.getStatus());

        // reload model through new request, and then modify a container
        // give time for jcr events to evict model
        Thread.sleep(200);
        mockNewRequest(session, "localhost", "/home");

        usersWithLockedContainers = pccs.getEditingPreviewChannel().getChangedBySet();
        assertTrue(usersWithLockedContainers.contains("admin"));

        mountResource.publish();

        // reload model through new request, and then modify a container
        // give time for jcr events to evict model
        Thread.sleep(200);

        mockNewRequest(session, "localhost", "/home");

        usersWithLockedContainers = pccs.getEditingPreviewChannel().getChangedBySet();
        assertTrue(usersWithLockedContainers.isEmpty());

    }


    public static class ChannelEventListener {

        private List<ChannelEvent> processed = new ArrayList<>();
        private boolean locksOnConfigurationPresentDuringEventDispatching;
        @Subscribe
        public void onChannelEvent(ChannelEvent event) throws RepositoryException {
            final Session session = event.getRequestContext().getSession();

            // to assure that the event publishSynchronousEvent(event); in MountResource happens *AFTER* all locks are
            // removed without yet saving by the calls 1-5 below, we need to validate that the locks in preview are all gone!

            // 1 : copyChangedMainConfigNodes(session, previewConfigurationPath, liveConfigurationPath, mainConfigNodeNamesToPublish);
            // 2 : publishChannelChanges(session, userIds);

            // 3 : siteMapHelper.publishChanges(userIds);
            // 4 : pagesHelper.publishChanges(userIds);
            // 5 : siteMenuHelper.publishChanges(userIds);

            locksOnConfigurationPresentDuringEventDispatching = lockForPresentBelow(session, event.getEditingPreviewSite().getConfigurationPath());
            processed.add(event);
        }

        public List<ChannelEvent> getProcessed() {
            return processed;
        }
    }

    private static boolean lockForPresentBelow(final Session session, final String rootPath) throws RepositoryException {
        final Node start = session.getNode(rootPath);
        return checkRecursiveForLock(start);

    }

    private static boolean checkRecursiveForLock(final Node current) throws RepositoryException {
        if (current.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY)) {
            return true;
        }
        for (Node child : new NodeIterable(current.getNodes())) {
            boolean hasLock = checkRecursiveForLock(child);
            if (hasLock) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void publish_mount_with_ChannelEventListener() throws Exception {
        final ChannelEventListener listener = new ChannelEventListener();
        try {
            HstServices.getComponentManager().registerEventSubscriber(listener);
            createSomePreviewChanges();

            assertTrue(lockForPresentBelow(session, mountResource.getPageComposerContextService().getEditingPreviewSite().getConfigurationPath()));

            mountResource.publish();

            assertEquals(1, listener.getProcessed().size());
            // during the event dispatching, the locks should already have been removed from preview!
            assertFalse("during event dispatching locks should be already removed!",
                    listener.locksOnConfigurationPresentDuringEventDispatching);
        } finally {
            HstServices.getComponentManager().unregisterEventSubscriber(listener);
        }
    }

    @Test
     public void discard_mount_with_ChannelEventListener() throws Exception {
        final ChannelEventListener listener = new ChannelEventListener();
        try {
            HstServices.getComponentManager().registerEventSubscriber(listener);
            createSomePreviewChanges();
            assertTrue(lockForPresentBelow(session, mountResource.getPageComposerContextService().getEditingPreviewSite().getConfigurationPath()));
            mountResource.discardChanges();
            assertEquals(1, listener.getProcessed().size());

            // during the event dispatching, the locks should already have been removed from preview!
            assertFalse("during event dispatching locks should be already removed!",
                    listener.locksOnConfigurationPresentDuringEventDispatching);
        } finally {
            HstServices.getComponentManager().unregisterEventSubscriber(listener);
        }

    }

    public static class ChannelEventListenerSettingClientException {
        private ChannelEvent handledEvent;
        @Subscribe
        public void onChannelEvent(ChannelEvent event) throws RepositoryException {
            this.handledEvent = event;
            event.setException(new ClientException("ClientException message", ClientError.UNKNOWN));
        }
    }

    @Test
    public void publish_mount_with_ChannelEventListener_that_sets_ClientException_does_not_result_in_publication_but_bad_request() throws Exception {
        final ChannelEventListenerSettingClientException listener = new ChannelEventListenerSettingClientException();
        try {
            HstServices.getComponentManager().registerEventSubscriber(listener);
            createSomePreviewChanges();

            assertTrue(lockForPresentBelow(session, mountResource.getPageComposerContextService().getEditingPreviewSite().getConfigurationPath()));

            Response response = mountResource.publish();

            assertNotNull(listener.handledEvent.getException());
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            assertEquals(listener.handledEvent.getException().toString(), ((ExtResponseRepresentation) response.getEntity()).getMessage());

            // session contains not more changes as should be reset
            assertFalse(session.hasPendingChanges());

            // locks should still be present since publication failed
            assertTrue(lockForPresentBelow(session, mountResource.getPageComposerContextService().getEditingPreviewSite().getConfigurationPath()));

        } finally {
            HstServices.getComponentManager().unregisterEventSubscriber(listener);
        }
    }


    @Test
    public void discard_mount_with_ChannelEventListener_that_sets_ClientException_does_not_result_in_discard_but_bad_request() throws Exception {
        final ChannelEventListenerSettingClientException listener = new ChannelEventListenerSettingClientException();
        try {
            HstServices.getComponentManager().registerEventSubscriber(listener);
            createSomePreviewChanges();

            assertTrue(lockForPresentBelow(session, mountResource.getPageComposerContextService().getEditingPreviewSite().getConfigurationPath()));

            Response response = mountResource.discardChanges();

            assertNotNull(listener.handledEvent.getException());
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            assertEquals(listener.handledEvent.getException().toString(), ((ExtResponseRepresentation) response.getEntity()).getMessage());

            // session contains not more changes as should be reset
            assertFalse(session.hasPendingChanges());

            // locks should still be present since discard failed
            assertTrue(lockForPresentBelow(session, mountResource.getPageComposerContextService().getEditingPreviewSite().getConfigurationPath()));

        } finally {
            HstServices.getComponentManager().unregisterEventSubscriber(listener);
        }
    }

    public static class ChannelEventListenerSettingIllegalStateException {
        @Subscribe
        public void onChannelEvent(ChannelEvent event) throws RepositoryException {
            event.setException(new IllegalStateException("IllegalStateException message"));
        }
    }

    @Test
    public void publish_mount_with_ChannelEventListener_that_sets_IllegalStateException_does_not_result_in_publication_but_server_error() throws Exception {
        final ChannelEventListenerSettingIllegalStateException listener = new ChannelEventListenerSettingIllegalStateException();
        try {
            HstServices.getComponentManager().registerEventSubscriber(listener);
            createSomePreviewChanges();

            assertTrue(lockForPresentBelow(session, mountResource.getPageComposerContextService().getEditingPreviewSite().getConfigurationPath()));

            Response response = mountResource.publish();

            assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());

            assertEquals("IllegalStateException message", ((ExtResponseRepresentation)response.getEntity()).getMessage());
            // session contains not more changes as should be reset
            assertFalse(session.hasPendingChanges());

            // locks should still be present since publication failed
            assertTrue(lockForPresentBelow(session, mountResource.getPageComposerContextService().getEditingPreviewSite().getConfigurationPath()));

        } finally {
            HstServices.getComponentManager().unregisterEventSubscriber(listener);
        }
    }


    @Test
    public void discard_mount_with_ChannelEventListener_that_sets_IllegalStateException_does_not_result_in_discard_but_server_error() throws Exception {
        final ChannelEventListenerSettingIllegalStateException listener = new ChannelEventListenerSettingIllegalStateException();
        try {
            HstServices.getComponentManager().registerEventSubscriber(listener);
            createSomePreviewChanges();

            assertTrue(lockForPresentBelow(session, mountResource.getPageComposerContextService().getEditingPreviewSite().getConfigurationPath()));

            Response response = mountResource.discardChanges();

            assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
            assertEquals("IllegalStateException message", ((ExtResponseRepresentation) response.getEntity()).getMessage());

            // session contains not more changes as should be reset
            assertFalse(session.hasPendingChanges());

            // locks should still be present since discard failed
            assertTrue(lockForPresentBelow(session, mountResource.getPageComposerContextService().getEditingPreviewSite().getConfigurationPath()));

        } finally {
            HstServices.getComponentManager().unregisterEventSubscriber(listener);
        }
    }

    private void createSomePreviewChanges() throws Exception {
        movePagesFromCommonToUnitTestProject();
        createWorkspaceWithTestContainer();
        addReferencedContainerForHomePage();
        String catalogItemUUID = addCatalogItem();
        session.save();
        // give time for jcr events to evict model
        Thread.sleep(200);

        mockNewRequest(session, "localhost", "");
        mountResource.startEdit();

        // reload model through new request, and then modify a container
        // give time for jcr events to evict model
        Thread.sleep(200);

        mockNewRequest(session, "localhost", "");

        final PageComposerContextService pccs = mountResource.getPageComposerContextService();
        final String previewConfigurationPath = pccs.getRequestContext().getResolvedMount().getMount().getHstSite().getConfigurationPath();

        final String previewContainerNodeUUID = session.getNode(previewConfigurationPath)
                .getNode("hst:workspace/hst:containers/testcontainer").getIdentifier();
        pccs.getRequestContext().setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, previewContainerNodeUUID);

        final ContainerComponentResource containerComponentResource = createContainerResource();
        final Response response = containerComponentResource.createContainerItem(catalogItemUUID, 0);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        // reload model through new request, and then modify a container
        // give time for jcr events to evict model
        Thread.sleep(200);

        mockNewRequest(session, "localhost", "/home");

    }

}
