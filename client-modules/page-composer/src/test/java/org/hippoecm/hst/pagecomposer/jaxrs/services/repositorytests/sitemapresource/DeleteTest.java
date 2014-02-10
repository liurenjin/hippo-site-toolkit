/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests.sitemapresource;

import java.util.UUID;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ExtResponseRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.SiteMapResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.SiteMapHelper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DeleteTest extends AbstractSiteMapResourceTest {



    private void initContext() throws Exception {
        // call below will init request context
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
    }

    @Test
    public void test_delete() throws Exception {
        final SiteMapResource siteMapResource = new SiteMapResource();
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");

        final Response delete = siteMapResource.delete(home.getId());
        assertEquals(Response.Status.OK.getStatusCode(), delete.getStatus());
        assertTrue(((ExtResponseRepresentation) delete.getEntity()).getMessage().contains("deleted"));
    }

    @Test
    public void test_delete_non_existing() throws Exception {
        initContext();
        final SiteMapResource siteMapResource = new SiteMapResource();
        final Response delete = siteMapResource.delete(UUID.randomUUID().toString());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), delete.getStatus());
    }

    @Test
    public void test_delete_invalid_uuid() throws Exception {
        initContext();
        final SiteMapResource siteMapResource = new SiteMapResource();
        final Response delete = siteMapResource.delete("invalid-uuid");
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), delete.getStatus());
    }

    @Test
    public void test_delete_non_workspace_item_fails() throws Exception {
        final SiteMapItemRepresentation nonWorkspaceItem = getSiteMapItemRepresentation(session, "about-us");
        final SiteMapResource siteMapResource = new SiteMapResource();
        final Response delete = siteMapResource.delete(nonWorkspaceItem.getId());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), delete.getStatus());
        assertTrue((((ExtResponseRepresentation) delete.getEntity()).getMessage().contains("not part of hst:workspace")));
    }

    @Test
    public void test_deleted_item_not_part_of_model() throws Exception {
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
        final SiteMapResource siteMapResource = new SiteMapResource();
        final Response delete = siteMapResource.delete(home.getId());
        assertEquals(Response.Status.OK.getStatusCode(), delete.getStatus());
        assertTrue(((ExtResponseRepresentation) delete.getEntity()).getMessage().contains("deleted"));

        // a refetch for deleted item should return null as not present in hst model any more
        assertNull(getSiteMapItemRepresentation(session, "home"));

    }

    @Test
    public void test_deleted_item_jcr_node_still_present_and_locked() throws Exception {
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
        final SiteMapResource siteMapResource = new SiteMapResource();
        final Response delete = siteMapResource.delete(home.getId());
        assertEquals(Response.Status.OK.getStatusCode(), delete.getStatus());
        assertTrue(((ExtResponseRepresentation) delete.getEntity()).getMessage().contains("deleted"));

        try {
            final Node deletedNode = session.getNodeByIdentifier(home.getId());
            assertEquals("deleted",deletedNode.getProperty(HstNodeTypes.EDITABLE_PROPERTY_STATE).getString());

            final Session bob = createSession("bob", "bob");
            Node deleteHomeNodeByBob = bob.getNodeByIdentifier(home.getId());
            SiteMapHelper helper = new SiteMapHelper();
            try {
                helper.acquireLock(deleteHomeNodeByBob);
                fail("Bob should 'see' locked deleted home node");
            } catch (IllegalStateException e) {

            }
            bob.logout();

        } catch (ItemNotFoundException e) {
            fail("Node should still exist but marked as deleted");
        }
    }

}
