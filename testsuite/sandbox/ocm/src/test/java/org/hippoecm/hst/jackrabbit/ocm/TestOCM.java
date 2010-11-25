/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.jackrabbit.ocm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import javax.jcr.Session;

import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.query.Filter;
import org.apache.jackrabbit.ocm.query.Query;
import org.apache.jackrabbit.ocm.query.QueryManager;
import org.hippoecm.hst.jackrabbit.ocm.util.OCMUtils;
import org.hippoecm.hst.test.AbstractHstTestCase;
import org.junit.Test;

/**
 * TestOCM
 * 
 * @version $Id$
 */
public class TestOCM extends AbstractHstTestCase{
    
    private String [] fallbackHippoBeans = { "hippo:document" };
    private Class [] annotatedBeans = { TextPage.class };
    
    @Test
    public void testTextPage() throws Exception {
        Session session = getSession();
        
        ObjectContentManager ocm = OCMUtils.createObjectContentManager(session, fallbackHippoBeans, annotatedBeans);
        
        TextPage productsPage = (TextPage) ocm.getObject("/testcontent/documents/testproject/Products/SomeProduct");
        assertNotNull(productsPage);
        assertNotNull(productsPage.getNode());
        assertNotNull(productsPage.getHtml());
        assertNotNull(productsPage.getHtml().getContent());
        
        System.out.println("node: " + productsPage.getNode());
        System.out.println("path: " + productsPage.getPath());
        System.out.println("title: " + productsPage.getTitle());
        System.out.println("stateSummary: " + productsPage.getStateSummary());
        System.out.println("state: " + productsPage.getState());
        System.out.println("html: " + productsPage.getHtml().getContent());

        ocm = OCMUtils.createObjectContentManager(session, fallbackHippoBeans, null);
        
        HippoStdDocument productsPageDoc = (HippoStdDocument) ocm.getObject("/testcontent/documents/testproject/Products/SomeProduct");
        assertNotNull(productsPageDoc);
        assertNotNull(productsPageDoc.getNode());

        System.out.println("productsPageDoc's node: " + productsPageDoc.getNode());
        System.out.println("productsPageDoc's path: " + productsPageDoc.getPath());
        System.out.println("productsPageDoc's stateSummary: " + productsPageDoc.getStateSummary());
        System.out.println("productsPageDoc's state: " + productsPageDoc.getState());
        
        HippoStdFolder parentCollection = productsPageDoc.getFolder();
        System.out.println("parentCollection: " + parentCollection);
        System.out.println("parentCollection.path: " + parentCollection.getPath());
        assertNotNull(parentCollection);
        
        List<HippoStdDocument> childDocs = parentCollection.getDocuments();
        assertNotNull(childDocs);
        assertFalse(childDocs.isEmpty());
        System.out.println("childDocs: " + childDocs);
        for (HippoStdDocument childDoc : childDocs) {
            System.out.println("childDoc: " + childDoc);
        }
        
        session.logout();
    }
    
    @Test
    public void testCollection() throws Exception {
        Session session = getSession();
        
        ObjectContentManager ocm = OCMUtils.createObjectContentManager(session, fallbackHippoBeans, annotatedBeans);
        
        TextPage productsPage = (TextPage) ocm.getObject("/testcontent/documents/testproject/Products/SomeProduct");
        assertNotNull(productsPage);
        assertNotNull(productsPage.getNode());
        
        System.out.println("node: " + productsPage.getNode());
        System.out.println("path: " + productsPage.getPath());
        System.out.println("title: " + productsPage.getTitle());
        System.out.println("stateSummary: " + productsPage.getStateSummary());
        System.out.println("state: " + productsPage.getState());
        
        // Normal JCR Node path
        HippoStdFolder coll = (HippoStdFolder) ocm.getObject("/testcontent/documents/testproject");
        assertNotNull(coll);
        assertNotNull(coll.getNode());
        
        System.out.println("node: " + coll.getNode());
        System.out.println("path: " + coll.getPath());
        
        List<HippoStdFolder> childColl = coll.getFolders();
        assertNotNull(childColl);
        assertFalse(childColl.isEmpty());
        
        System.out.println("childColl: " + childColl);
        
        for (HippoStdFolder childCollItem : childColl) {
            System.out.println("childCollItem: " + childCollItem.getName() + ", " + childCollItem.getPath());
        }
        
        HippoStdFolder productsColl = (HippoStdFolder) ocm.getObject("/testcontent/documents/testproject/Products");
        
        List<HippoStdDocument> childDocs = productsColl.getDocuments();
        assertNotNull(childDocs);
        assertFalse(childDocs.isEmpty());
        
        System.out.println("childDocs: " + childDocs);
        
        for (HippoStdDocument childDoc : childDocs) {
            System.out.println("childDoc: " + childDoc.getName() + ", " + childDoc.getPath() + ", " + childDoc.getState() + ", " + childDoc.getStateSummary());
        }

        session.logout();
    }
    
    @Test
    public void testCollectionWithDigesterMapper() throws Exception {
        Session session = getSession();
        
        ObjectContentManager ocm = OCMUtils.createObjectContentManager(session, new InputStream [] { getClass().getResourceAsStream("jackrabbit-ocm-descriptor.xml") } );
        
        TextPage productsPage = (TextPage) ocm.getObject("/testcontent/documents/testproject/Products/SomeProduct");
        assertNotNull(productsPage);
        assertNotNull(productsPage.getNode());
        
        System.out.println("node: " + productsPage.getNode());
        System.out.println("path: " + productsPage.getPath());
        System.out.println("title: " + productsPage.getTitle());
        System.out.println("stateSummary: " + productsPage.getStateSummary());
        System.out.println("state: " + productsPage.getState());
        
        // Normal JCR Node path
        HippoStdFolder coll = (HippoStdFolder) ocm.getObject("/testcontent/documents/testproject");
        assertNotNull(coll);
        assertNotNull(coll.getNode());
        
        System.out.println("node: " + coll.getNode());
        System.out.println("path: " + coll.getPath());
        
        List<HippoStdFolder> childColl = coll.getFolders();
        assertNotNull(childColl);
        assertFalse(childColl.isEmpty());
        
        System.out.println("childColl: " + childColl);
        
        for (HippoStdFolder childCollItem : childColl) {
            System.out.println("childCollItem: " + childCollItem.getName() + ", " + childCollItem.getPath());
        }
        
        HippoStdFolder productsColl = (HippoStdFolder) ocm.getObject("/testcontent/documents/testproject/Products");
        
        List<HippoStdDocument> childDocs = productsColl.getDocuments();
        assertNotNull(childDocs);
        assertFalse(childDocs.isEmpty());
        
        System.out.println("childDocs: " + childDocs);
        
        for (HippoStdDocument childDoc : childDocs) {
            System.out.println("childDoc: " + childDoc.getName() + ", " + childDoc.getPath() + ", " + childDoc.getState() + ", " + childDoc.getStateSummary());
        }

        session.logout();
    }
    
    @Test
    public void testQueryManager() throws Exception {
        Session session = getSession();
        
        ObjectContentManager ocm = OCMUtils.createObjectContentManager(session, fallbackHippoBeans, annotatedBeans);
        
        // search collection
        QueryManager qm = ocm.getQueryManager();
        Filter filter = qm.createFilter(HippoStdDocument.class);
        filter.setScope("/testcontent/documents/testproject//");
        Query query = qm.createQuery(filter);
        Collection result = ocm.getObjects(query);
        System.out.println("result: " + result);
        assertFalse(result.isEmpty());

        // search document by HippoStdDocument filter
        filter = qm.createFilter(HippoStdDocument.class);
        filter.setScope("/testcontent/documents/testproject/Products/SomeProduct//");
        query = qm.createQuery(filter);
        HippoStdDocument doc = (HippoStdDocument) ocm.getObject(query);
        System.out.println("doc: " + doc);
        assertNotNull(doc);
        assertTrue(doc instanceof TextPage);
        
        // search document by TextPage class filter with title filter.
        // because the class is TextPage, we can use title filter here.
        filter = qm.createFilter(TextPage.class);
        filter.setScope("/testcontent/documents/testproject/Products/SomeProduct//");
        filter.addEqualTo("title", "Products");
        query = qm.createQuery(filter);
        TextPage page = (TextPage) ocm.getObject(query);
        System.out.println("page: " + page);
        assertNotNull(page);
        assertTrue(page instanceof TextPage);
        
        // search document by TextPage class filter with 'contains'.
        filter = qm.createFilter(TextPage.class);
        filter.setScope("/testcontent/documents/testproject/Products//");
        filter.addContains(".", "CMS");
        query = qm.createQuery(filter);
        Collection<TextPage> textPages = (Collection<TextPage>) ocm.getObjects(query);
        assertNotNull(textPages);
        assertFalse(textPages.isEmpty());
        for (TextPage textPage : textPages) {
            System.out.println("text page containing 'CMS': " + textPage.getPath());
        }
        
        session.logout();
    }
    
    @Test
    public void testTextPageUpdate() throws Exception {
        Session session = getSession();
        
        ObjectContentManager ocm = OCMUtils.createObjectContentManager(session, fallbackHippoBeans, annotatedBeans);
        
        TextPage productsPage = (TextPage) ocm.getObject("/testcontent/documents/testproject/Products/SomeProduct");
        assertNotNull(productsPage);
        assertNotNull(productsPage.getNode());
        
        System.out.println("node: " + productsPage.getNode());
        System.out.println("path: " + productsPage.getPath());
        System.out.println("title: " + productsPage.getTitle());
        System.out.println("stateSummary: " + productsPage.getStateSummary());
        System.out.println("state: " + productsPage.getState());
        
        String oldTitle = productsPage.getTitle();
        
        // Now updating...
        productsPage.setTitle("Hey, Dude!");
        ocm.update(productsPage);
        ocm.save();
        
        // Now validating the changes from the repository...
        TextPage productsPageUpdated = (TextPage) ocm.getObject("/testcontent/documents/testproject/Products/SomeProduct");
        assertNotNull(productsPageUpdated);
        assertNotNull(productsPageUpdated.getNode());
        assertEquals("Hey, Dude!", productsPageUpdated.getTitle());
        
        System.out.println("node: " + productsPageUpdated.getNode());
        System.out.println("path: " + productsPageUpdated.getPath());
        System.out.println("title: " + productsPageUpdated.getTitle());
        System.out.println("stateSummary: " + productsPageUpdated.getStateSummary());
        System.out.println("state: " + productsPageUpdated.getState());
        
        // Restores the changes back...
        productsPageUpdated.setTitle(oldTitle);
        ocm.update(productsPageUpdated);
        ocm.save();
        
        session.logout();
    }
}
