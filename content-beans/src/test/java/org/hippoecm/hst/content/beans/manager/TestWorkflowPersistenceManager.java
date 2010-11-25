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
package org.hippoecm.hst.content.beans.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.hst.AbstractBeanTestCase;
import org.hippoecm.hst.content.beans.ContentNodeBinder;
import org.hippoecm.hst.content.beans.ContentNodeBindingException;
import org.hippoecm.hst.content.beans.PersistableTextPage;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowCallbackHandler;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManager;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManagerImpl;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.repository.reviewedactions.FullReviewedActionsWorkflow;
import org.junit.Before;
import org.junit.Test;

public class TestWorkflowPersistenceManager extends AbstractBeanTestCase {
    
    private static final String HIPPOSTD_FOLDER_NODE_TYPE = "hippostd:folder";
    private static final String TEST_DOCUMENT_NODE_TYPE = "unittestproject:textpage";
    
    private static final String TEST_CONTENTS_PATH = "/unittestpreview/unittestproject/hst:content";
    private static final String TEST_FOLDER_NODE_PATH = TEST_CONTENTS_PATH + "/common";

    private static final String TEST_EXISTING_DOCUMENT_NODE_PATH = TEST_FOLDER_NODE_PATH + "/homepage";
    
    private static final String TEST_NEW_DOCUMENT_NODE_NAME = "about";
    private static final String TEST_NEW_DOCUMENT_NODE_PATH = TEST_FOLDER_NODE_PATH + "/" + TEST_NEW_DOCUMENT_NODE_NAME;
    
    private static final String TEST_NEW_FOLDER_NODE_NAME = "subcommon";
    private static final String TEST_NEW_FOLDER_NODE_PATH = TEST_FOLDER_NODE_PATH + "/" + TEST_NEW_FOLDER_NODE_NAME;
    
    private static final String TEST_AUTO_NEW_FOLDER_NODE_NAME = "comments/tests";
    private static final String TEST_AUTO_NEW_FOLDER_NODE_PATH = TEST_CONTENTS_PATH + "/" + TEST_AUTO_NEW_FOLDER_NODE_NAME;
    
    private WorkflowPersistenceManager wpm;
    private Map<String, ContentNodeBinder> persistBinders;
    
    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.persistBinders = new HashMap<String, ContentNodeBinder>();
        this.persistBinders.put("unittestproject:textpage", new PersistableTextPageBinder());
    }
    
 
    @Test
    public void testDocumentManipulation() throws Exception {
        Session session = null;
        
        try {
            ObjectConverter objectConverter = getObjectConverter();
            
            session = this.getSession();
            
            wpm = new WorkflowPersistenceManagerImpl(session, objectConverter, persistBinders);
            wpm.setWorkflowCallbackHandler(new WorkflowCallbackHandler<FullReviewedActionsWorkflow>() {
                public void processWorkflow(FullReviewedActionsWorkflow wf) throws Exception {
                    FullReviewedActionsWorkflow fraw = (FullReviewedActionsWorkflow) wf;
                    fraw.requestPublication();
                }
            });
            
            // basic object retrieval from the content
            PersistableTextPage page = (PersistableTextPage) wpm.getObject(TEST_EXISTING_DOCUMENT_NODE_PATH);
            assertNotNull(page);
            
            try {
                // create a document with type and name 
                wpm.create(TEST_FOLDER_NODE_PATH, TEST_DOCUMENT_NODE_TYPE, TEST_NEW_DOCUMENT_NODE_NAME);
            
                // retrieves the document created just before
                PersistableTextPage newPage = (PersistableTextPage) wpm.getObject(TEST_NEW_DOCUMENT_NODE_PATH);
                assertNotNull(newPage);
                
                newPage.setTitle("Title of the about page");
                newPage.setBodyContent("<h1>Welcome to the about page!</h1>");

                // custom mapping binder is already provided during WPM instantiation.
                // but you can also provide your custom binder as the second parameter.
                // if any binder is not found and the first parameter (newPage) is instanceof ContentPersistenceBinder,
                // then the POJO object in the first parameter will be used as a binder. 
                wpm.update(newPage);
                
                // retrieves the document created just before
                newPage = (PersistableTextPage) wpm.getObject(TEST_NEW_DOCUMENT_NODE_PATH);
                assertEquals("Title of the about page", newPage.getTitle());
                assertEquals("<h1>Welcome to the about page!</h1>", newPage.getBodyContent());
                
            } finally {
                PersistableTextPage newPage = null;
                
                try {
                    newPage = (PersistableTextPage) wpm.getObject(TEST_NEW_DOCUMENT_NODE_PATH);
                } catch (Exception e) {
                }
                
                if (newPage != null) {
                    wpm.remove(newPage);
                }
            }
        } finally {
            if (session != null) session.logout();
        }
        
    }
    
    @Test
    public void testFolderCreateRemove() throws Exception {
        Session session = null;
        
        try {
            ObjectConverter objectConverter = getObjectConverter();
            
            session = this.getSession();
            
            wpm = new WorkflowPersistenceManagerImpl(session, objectConverter, persistBinders);
            
            HippoFolderBean newFolder = null;
            
            try {
                // create a document with type and name 
                wpm.create(TEST_FOLDER_NODE_PATH, HIPPOSTD_FOLDER_NODE_TYPE, TEST_NEW_FOLDER_NODE_NAME);
            
                // retrieves the document created just before
                newFolder = (HippoFolderBean) wpm.getObject(TEST_NEW_FOLDER_NODE_PATH);
                assertNotNull(newFolder);
                
            } finally {
                if (newFolder != null) {
                    wpm.remove(newFolder);
                }
            }
            
            wpm.save();
        } finally {
            if (session != null) session.logout();
        }
        
    }
    
    @Test
    public void testFolderAutoCreateRemove() throws Exception {
        Session session = null;
        
        try {
            ObjectConverter objectConverter = getObjectConverter();
            
            session = this.getSession();
            
            wpm = new WorkflowPersistenceManagerImpl(session, objectConverter, persistBinders);
            
            HippoFolderBean newFolder = null;
            
            try {
                // create a document with type and name 
                wpm.create(TEST_AUTO_NEW_FOLDER_NODE_PATH, HIPPOSTD_FOLDER_NODE_TYPE, "testfolder", true);
            
                // retrieves the document created just before
                newFolder = (HippoFolderBean) wpm.getObject(TEST_AUTO_NEW_FOLDER_NODE_PATH + "/testfolder");
                assertNotNull(newFolder);
                
            } finally {
                if (newFolder != null) {
                    wpm.remove(newFolder);
                }
            }
            
            wpm.save();
        } finally {
            if (session != null) session.logout();
        }
        
    }
    
    private class PersistableTextPageBinder implements ContentNodeBinder {
        
        public boolean bind(Object content, Node node) throws ContentNodeBindingException {
            PersistableTextPage page = (PersistableTextPage) content;
            
            try {
                node.setProperty("unittestproject:title", page.getTitle());
                Node htmlNode = node.getNode("unittestproject:body");
                htmlNode.setProperty("hippostd:content", page.getBodyContent());
            } catch (Exception e) {
                throw new ContentNodeBindingException(e);
            }
            
            // FIXME: return true only if actual changes happen.
            return true;
        }
        
    }
    
}
