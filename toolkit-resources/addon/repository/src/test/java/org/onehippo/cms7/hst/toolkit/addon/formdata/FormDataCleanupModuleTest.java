/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.hst.toolkit.addon.formdata;


import java.util.Calendar;
import java.util.Random;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.test.AbstractHstTestCase;
import org.hippoecm.repository.util.NodeIterable;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class FormDataCleanupModuleTest extends AbstractHstTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Session session = getSession();
        removeFormData();
        Node rootNode = session.getRootNode();
        rootNode.addNode("formdata", "hst:formdatacontainer");
        session.save();

    }

    private void removeFormData() throws RepositoryException {
        if (getSession().nodeExists("/formdata")) {
            for (Node node : new NodeIterable(getSession().getNode("/formdata").getNodes())) {
                node.remove();
            }
        }
    }

    @After
    public void tearDown() throws Exception {
        removeFormData();
        super.tearDown();
    }

    private void createFormDataNode(String subPath, long creationTimeMillis) throws Exception {
        Session session = getSession();
        Node rootNode = session.getRootNode();
        Node formData = rootNode.getNode("formdata");
        if (subPath != null) {
            if (!formData.hasNode(subPath)) {
                formData = formData.addNode(subPath, "hst:formdatacontainer");
            } else {
                formData = formData.getNode(subPath);
            }
        }
        Node postedFormDataNode = formData.addNode("tick_"+System.currentTimeMillis(), "hst:formdata");
        Thread.sleep(2l);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(creationTimeMillis);
        postedFormDataNode.setProperty("hst:creationtime", calendar);
        session.save();
    }

    private int countFormDataNodes(Node formDataFolder) throws RepositoryException {
        int count = 0;
        for (Node node : new NodeIterable(formDataFolder.getNodes())) {
            if (node.isNodeType("hst:formdata")) {
                count++;
            } else if (node.isNodeType("hst:formdatacontainer")) {
                count+= countFormDataNodes(node);
            }
        }
        return count;
    }

    @Test
    public void testFormDataCleanup() throws Exception {
        long now = System.currentTimeMillis();
        createFormDataNode(null, now - 45 * 1000);
        createFormDataNode(null, now - 65 * 1000);
        assertEquals(2l, countFormDataNodes(getSession().getNode("/formdata")));

        FormDataCleanupModule formDataCleanupModule = new FormDataCleanupModule(
                getSession(),
                "/hippo:configuration/hippo:modules/formdatacleanup/hippo:moduleconfig",
                "0/2 * * * * ?", 1l, "");

        Thread.sleep(2000);
        assertEquals(1l, countFormDataNodes(getSession().getNode("/formdata")));
        // quartz scheduler does not really allow running every second, so need to wait about a minute to see the second removal
        //Thread.sleep(60000);
        //assertEquals(0l, getFormdataNodes().getTotalSize());

        formDataCleanupModule.unscheduleJob();
        formDataCleanupModule.shutdown();
    }

    @Test
    public void testExcludeFormDataCleanup() throws Exception {
        long now = System.currentTimeMillis();
        createFormDataNode(null, now - 45 * 1000);
        createFormDataNode("permanent", now - 45 * 1000);
        createFormDataNode("abcd", now - 45 * 1000);
        createFormDataNode(null, now - 65 * 1000);
        createFormDataNode("permanent", now - 65 * 1000);
        createFormDataNode("abcd", now - 65 * 1000);
        assertEquals(6l, countFormDataNodes(getSession().getNode("/formdata")));

        FormDataCleanupModule formDataCleanupModule = new FormDataCleanupModule(
                getSession(),
                "/hippo:configuration/hippo:modules/formdatacleanup/hippo:moduleconfig",
                "0/2 * * * * ?", 1l, "/formdata/permanent/|/formdata/abcd|");

        Thread.sleep(2000);
        assertEquals(5l, countFormDataNodes(getSession().getNode("/formdata")));
        //quartz scheduler does not really allow running every second, so need to wait about a minute to see the second removal
        //Thread.sleep(60000);
        //assertEquals(4l, getFormdataNodes().getTotalSize());
        formDataCleanupModule.unscheduleJob();
        formDataCleanupModule.shutdown();
    }

    @Ignore
    @Test
    public void testFormDataBulkCleanup() throws Exception {
        Random random = new Random();
        String alphabet = "abcdefghijklmnopqrstuvwxyz";
        long time = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            createFormDataNode(String.valueOf(alphabet.charAt(random.nextInt(alphabet.length()))), time - (i % 2 == 0 ? 45 * 1000 : 65 * 1000));
        }
        FormDataCleanupModule formDataCleanupModule = new FormDataCleanupModule(
                getSession(),
                "/hippo:configuration/hippo:modules/formdatacleanup/hippo:moduleconfig",
                "0/2 * * * * ?", 1l, "/formdata/permanent/|/formdata/abcd|");

        Thread.sleep(5000);
        assertEquals(500l, countFormDataNodes(getSession().getNode("/formdata")));
        //quartz scheduler does not really allow running every second, so need to wait about a minute to see the second removal
        //Thread.sleep(60000);
        //assertEquals(4l, getFormdataNodes().getTotalSize());
        formDataCleanupModule.unscheduleJob();
        formDataCleanupModule.shutdown();
    }

}
