/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ExtResponseRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.SiteMapResource;
import org.hippoecm.repository.util.NodeIterable;
import org.junit.Test;

import static org.hippoecm.hst.configuration.HstNodeTypes.COMPONENT_PROPERTY_REFERECENCECOMPONENT;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_ABSTRACTPAGES;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AbstractifyTest extends AbstractSiteMapResourceTest {

    private void initContext() throws Exception {
        // call below will init request context
        getSiteMapItemRepresentation(session, "home");
    }

    @Test
    public void abstractify_created_page() throws Exception {

        final SiteMapResource siteMapResource = createResource();
        final Node newSiteMapItem = createNewPage(siteMapResource, "foo");
        initContext();
        SiteMapResource.ToDoGetRidOfThisClass prototypeName = new SiteMapResource.ToDoGetRidOfThisClass();
        prototypeName.setData("foo");
        Response response = siteMapResource.abstractify(newSiteMapItem.getIdentifier(), prototypeName);
        assertEquals(((ExtResponseRepresentation) response.getEntity()).getMessage(),
                Response.Status.OK.getStatusCode(), response.getStatus());

        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:prototypepages/foo"));
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:abstractpages/foo"));

        Node prototype = session.getNode("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:prototypepages/foo");
        Node abstractPage = session.getNode("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:abstractpages/foo");

        assertEquals(NODENAME_HST_ABSTRACTPAGES + "/" + abstractPage.getName(), prototype.getProperty(COMPONENT_PROPERTY_REFERECENCECOMPONENT).getString());
        assertEquals(NODENAME_HST_ABSTRACTPAGES + "/basepage", abstractPage.getProperty(COMPONENT_PROPERTY_REFERECENCECOMPONENT).getString());

        assertPrototypeContainsOnlyEmptyContainers(prototype);
        assertAbstractPageContainsOnlyNonEmptyContainers(abstractPage);

        // TODO assert locks are present (also if the new page in #createNewPage did *NOT* have lock nodes!)

        // TODO publish and assert published
    }

    private void assertPrototypeContainsOnlyEmptyContainers(final Node node) throws RepositoryException {
        if (node.isNodeType(NODETYPE_HST_CONTAINERCOMPONENT)) {
            if (node.hasNodes()) {
                fail("Prototypes should only have the empty containers");
            }
        }
        for (Node child : new NodeIterable(node.getNodes())) {
            assertPrototypeContainsOnlyEmptyContainers(child);
        }
    }

    private void assertAbstractPageContainsOnlyNonEmptyContainers(final Node node) throws RepositoryException {
        if (node.isNodeType(NODETYPE_HST_CONTAINERCOMPONENT)) {
            if (!node.hasNodes()) {
                fail("Abstract pages should only have the non empty containers");
            }
        }
        for (Node child : new NodeIterable(node.getNodes())) {
            assertPrototypeContainsOnlyEmptyContainers(child);
        }
    }

    private Node createNewPage(final SiteMapResource siteMapResource, final String newPage) throws Exception {
        initContext();
        final SiteMapItemRepresentation newFoo = createSiteMapItemRepresentation(newPage, getPrototypePageUUID());
        siteMapResource.create(newFoo);
        String newPageNodeName = newPage + "-" + session.getNodeByIdentifier(getPrototypePageUUID()).getName();
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:sitemap/"+newPage));
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:pages/"+newPageNodeName));
        return session.getNode("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:sitemap/"+newPage);
    }
}

