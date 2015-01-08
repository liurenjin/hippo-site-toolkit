/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.linking;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.component.GenericHstComponent;
import org.hippoecm.hst.core.parameters.DocumentLink;
import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class HstLinkRewritingComponentPickedIT extends AbstractHstLinkRewritingIT {

    public static final String TMPDOC_LOC = "/unittestcontent/documents/unittestproject/tmpdoc";
    private Session session;



    @Before
    public void setUp() throws Exception {
        super.setUp();
        session = createAdminSession();
        createHstConfigBackup(session);

        // enable hst:componentlinkrewritingsupported on mount
        session.getNode("/hst:hst/hst:sites/unittestproject").setProperty("hst:componentlinkrewritingsupported", true);

        // create tmpdoc that is not mapped via sitemap
        //session.getNode("/unittestcontent/documents/unittestproject/News/News1")
        JcrUtils.copy(session, "/unittestcontent/documents/unittestproject/News/News1", TMPDOC_LOC);
        // rename document to handle name
        JcrUtils.copy(session, "/unittestcontent/documents/unittestproject/tmpdoc/News1", "/unittestcontent/documents/unittestproject/tmpdoc/tmpdoc");

        session.save();
    }
    @After
    public void tearDown() throws Exception {
        restoreHstConfigBackup(session);
        session.removeItem(TMPDOC_LOC);
        session.save();
        session.logout();
        super.tearDown();
    }

    @Test
    public void assert_document_not_linked_at_all_results_in_not_found() throws Exception {
        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        Node tmpDoc = requestContext.getSession().getNode(TMPDOC_LOC);
        final HstLink hstLink = linkCreator.create(tmpDoc, requestContext);
        assertTrue(hstLink.isNotFound());
    }

    @SuppressWarnings("ALL")
    public static interface TestJcrPathAbsoluteI {
        @Parameter(name = "myproject-picked-news-jcrpath-absolute", displayName = "Picked News")
        @JcrPath(isRelative = false, pickerInitialPath = "tmpdoc")
        String getPickedNews();
    }

    @ParametersInfo(type = TestJcrPathAbsoluteI.class)
    public static class TestJcrPathAbsolute extends GenericHstComponent {

    }

    @Test
    public void document_linked_via_component_jcrPath_absolute_and_not_with_sitemap_content_path() throws Exception {
        // add hst component class to the contactpage:
        Node contactPage = session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:pages/contactpage");
        contactPage.setProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME, TestJcrPathAbsolute.class.getName());
        contactPage.setProperty("hst:parameternames", new String[]{"myproject-picked-news-jcrpath-absolute"});
        contactPage.setProperty("hst:parametervalues", new String[]{TMPDOC_LOC});
        session.save();

        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        Node tmpDoc = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject/tmpdoc");

        final HstLink hstLink = linkCreator.create(tmpDoc, requestContext);
        assertEquals("contact", hstLink.getPath());
    }

    @SuppressWarnings("ALL")
    public static interface TestJcrPathRelativeI {
        @Parameter(name = "myproject-picked-news-jcrpath-relative", displayName = "Picked News")
        @JcrPath(isRelative = true, pickerInitialPath = "tmpdoc")
        String getPickedNews();
    }

    @ParametersInfo(type = TestJcrPathRelativeI.class)
    public static class TestJcrPathRelative extends GenericHstComponent { }


    @Test
    public void document_linked_via_component_jcrPath_relative_and_not_with_sitemap_content_path() throws Exception {
        // add hst component class to the contactpage:
        Node contactPage = session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:pages/contactpage");
        contactPage.setProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME, TestJcrPathRelative.class.getName());
        contactPage.setProperty("hst:parameternames", new String[]{"myproject-picked-news-jcrpath-relative"});
        contactPage.setProperty("hst:parametervalues", new String[]{"tmpdoc"});
        session.save();

        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        Node tmpDoc = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject/tmpdoc");

        final HstLink hstLink = linkCreator.create(tmpDoc, requestContext);
        assertEquals("contact", hstLink.getPath());
    }


    @SuppressWarnings("ALL")
    public static interface TestDocumentLinkAbsoluteI {
        @Parameter(name = "myproject-picked-news-documentlink-absolute", displayName = "Document Location")
        @DocumentLink(docLocation = "/content", docType = "unittestproject:newspage")
        String getDocumentLocation();
    }

    @ParametersInfo(type = TestDocumentLinkAbsoluteI.class)
    public static class TestDocumentLinkAbsolute extends GenericHstComponent { }


    @Test
    public void document_linked_via_component_documentLink_absolute_and_not_with_sitemap_content_path() throws Exception {
        // add hst component class to the contactpage:
        Node contactPage = session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:pages/contactpage");
        contactPage.setProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME, TestDocumentLinkAbsolute.class.getName());
        contactPage.setProperty("hst:parameternames", new String[]{"myproject-picked-news-documentlink-absolute"});
        contactPage.setProperty("hst:parametervalues", new String[]{TMPDOC_LOC});
        session.save();

        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        Node tmpDoc = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject/tmpdoc");

        final HstLink hstLink = linkCreator.create(tmpDoc, requestContext);
        assertEquals("contact", hstLink.getPath());
    }



    @SuppressWarnings("ALL")
    public static interface TestDocumentLinkRelativeI {
        @Parameter(name = "myproject-picked-news-documentlink-relative", displayName = "Document Location")
        @DocumentLink(docLocation = "/content", docType = "unittestproject:newspage")
        String getDocumentLocation();
    }

    @ParametersInfo(type = TestDocumentLinkRelativeI.class)
    public static class TestDocumentLinkRelative extends GenericHstComponent { }


    @Test
    public void document_linked_via_component_documentLink_relative_and_not_with_sitemap_content_path() throws Exception {
        // add hst component class to the contactpage:
        Node contactPage = session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:pages/contactpage");
        contactPage.setProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME, TestDocumentLinkRelative.class.getName());
        contactPage.setProperty("hst:parameternames", new String[]{"myproject-picked-news-documentlink-relative"});
        contactPage.setProperty("hst:parametervalues", new String[]{"tmpdoc"});
        session.save();

        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        Node tmpDoc = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject/tmpdoc");

        final HstLink hstLink = linkCreator.create(tmpDoc, requestContext);
        assertEquals("contact", hstLink.getPath());
    }

    @Test
    public void document_linked_via_component_try_preferred_sitemap_item() throws Exception {
        // add hst component class to the contactpage:
        Node contactPage = session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:pages/contactpage");
        contactPage.setProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME, TestJcrPathRelative.class.getName());
        contactPage.setProperty("hst:parameternames", new String[]{"myproject-picked-news-jcrpath-relative"});
        contactPage.setProperty("hst:parametervalues", new String[]{"tmpdoc"});
        session.save();

        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        Node tmpDoc = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject/tmpdoc");

        HstSiteMapItem contactPreferred = requestContext.getResolvedSiteMapItem().getHstSiteMapItem().getHstSiteMap().getSiteMapItem("contact");
        HstSiteMapItem homePreferred = requestContext.getResolvedSiteMapItem().getHstSiteMapItem().getHstSiteMap().getSiteMapItem("home");
        final boolean fallback = true;
        final HstLink hstLink1 = linkCreator.create(tmpDoc, requestContext, contactPreferred, fallback);
        final HstLink hstLink2 = linkCreator.create(tmpDoc, requestContext, homePreferred, fallback);
        final HstLink hstLink3 = linkCreator.create(tmpDoc, requestContext, contactPreferred, !fallback);
        final HstLink hstLink4 = linkCreator.create(tmpDoc, requestContext, homePreferred, !fallback);
        assertEquals("contact", hstLink1.getPath());
        assertEquals("contact", hstLink2.getPath());
        assertEquals("contact", hstLink3.getPath());
        // preferred sitemap item home and fallback false can not create link
        assertTrue(hstLink4.isNotFound());
    }

    @Test
    public void test_document_linked_via_component_AND_via_sitemap_content_path() throws Exception {
        // add hst component class to the contactpage:
        Node contactPage = session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:pages/contactpage");
        contactPage.setProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME, TestJcrPathRelative.class.getName());
        contactPage.setProperty("hst:parameternames", new String[]{"myproject-picked-news-jcrpath-relative"});
        contactPage.setProperty("hst:parametervalues", new String[]{"tmpdoc"});

        // add relative hst:relativecontentpath = tmpdoc to existing sitemap item for '/home'
        session.getNode("/hst:hst/hst:configurations/unittestproject/hst:sitemap/home")
                .setProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH, "tmpdoc");

        session.save();

        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        Node tmpDoc = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject/tmpdoc");

        final HstLink hstLink = linkCreator.create(tmpDoc, requestContext);
        // home page has preference since shortest
        assertEquals("", hstLink.getPath());

        HstSiteMapItem contactPreferred = requestContext.getResolvedSiteMapItem().getHstSiteMapItem().getHstSiteMap().getSiteMapItem("contact");
        final boolean fallback = true;
        final HstLink hstLinkPreferredContact1 = linkCreator.create(tmpDoc, requestContext, contactPreferred, fallback);
        final HstLink hstLinkPreferredContact2 = linkCreator.create(tmpDoc, requestContext, contactPreferred, !fallback);

        assertEquals("contact", hstLinkPreferredContact1.getPath());
        assertEquals("contact", hstLinkPreferredContact2.getPath());

        HstSiteMapItem newsPreferred = requestContext.getResolvedSiteMapItem().getHstSiteMapItem().getHstSiteMap().getSiteMapItem("news");
        final HstLink hstLinkPreferredNews1 = linkCreator.create(tmpDoc, requestContext, newsPreferred, fallback);
        final HstLink hstLinkPreferredNews2 = linkCreator.create(tmpDoc, requestContext, newsPreferred, !fallback);

        // home page has preference since shortest so should be the fallback
        assertEquals("", hstLinkPreferredNews1.getPath());
        // preferred sitemap item news and fallback false can not create link
        assertTrue(hstLinkPreferredNews2.isNotFound());


        final List<HstLink> hstLinks = linkCreator.createAll(tmpDoc, requestContext, false);
        // home page has preference since shortest
        assertEquals("", hstLinks.get(0).getPath());
        assertEquals("contact", hstLinks.get(1).getPath());
    }


}