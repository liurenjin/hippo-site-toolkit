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
package org.hippoecm.hst.services.support.jaxrs.content.workflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.math.NumberUtils;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstContainerConfig;
import org.hippoecm.hst.core.container.Pipeline;
import org.hippoecm.hst.core.container.Pipelines;
import org.hippoecm.hst.services.support.jaxrs.content.AbstractJaxrsSpringTestCase;
import org.hippoecm.hst.services.support.jaxrs.content.BaseHstContentService;
import org.hippoecm.hst.site.HstServices;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/**
 * TestWorkflowService
 * 
 * @version $Id$
 */
public class TestWorkflowService extends AbstractJaxrsSpringTestCase {
    
    private static final String PREVIEW_SITE_CONTENT_PATH = "/testpreview/testproject/hst:content";
    
    protected Pipelines pipelines;
    protected Pipeline jaxrsPipeline;
    protected ServletConfig servletConfig;
    protected ServletContext servletContext;
    protected HstContainerConfig hstContainerConfig;
    
    @Before
    public void setUp() throws Exception {
        super.setUp();

        HstServices.setComponentManager(getComponentManager());
        
        pipelines = (Pipelines) getComponent(Pipelines.class.getName());
        jaxrsPipeline = this.pipelines.getPipeline("JaxrsPipeline");
        
        servletConfig = getComponent("jaxrsServiceServletConfig");
        servletContext = servletConfig.getServletContext();
        
        hstContainerConfig = new HstContainerConfig() {
            public ClassLoader getContextClassLoader() {
                return TestWorkflowService.class.getClassLoader();
            }
            public ServletConfig getServletConfig() {
                return servletConfig;
            }
        };
    }
    
    @Test
    public void testGetWorkflow() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setAttribute(BaseHstContentService.SITE_CONTENT_PATH, PREVIEW_SITE_CONTENT_PATH);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("GET");
        request.setRequestURI("/testapp/preview/services/workflowservice/Products/HippoCMS");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/workflowservice/Products/HippoCMS");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(response.getContentAsByteArray()));
        
        XPath xpath = XPathFactory.newInstance().newXPath();
        XPathExpression expr = xpath.compile("count(/workflow/hints/entry)");
        String value = expr.evaluate(document);
        assertTrue(NumberUtils.isNumber(value));
        assertTrue(NumberUtils.toInt(value) > 0);
        
        xpath = XPathFactory.newInstance().newXPath();
        expr = xpath.compile("count(/workflow/interfaces/interface)");
        value = expr.evaluate(document);
        assertTrue(NumberUtils.isNumber(value));
        assertTrue(NumberUtils.toInt(value) > 0);
    }
    
    @Test
    public void testProcessAction() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setAttribute(BaseHstContentService.SITE_CONTENT_PATH, PREVIEW_SITE_CONTENT_PATH);
        request.setProtocol("HTTP/1.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setMethod("POST");
        request.setRequestURI("/testapp/preview/services/workflowservice/Products/HippoCMS");
        request.setContextPath("/testapp");
        request.setServletPath("/preview/services");
        request.setPathInfo("/workflowservice/Products/HippoCMS");
        request.setQueryString("wfclass=org.hippoecm.repository.reviewedactions.FullReviewedActionsWorkflow&action=requestPublication");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        invokeJaxrsPipeline(request, response);
        
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }
    
    private void invokeJaxrsPipeline(HttpServletRequest request, HttpServletResponse response) throws ContainerException {
        jaxrsPipeline.beforeInvoke(hstContainerConfig, request, response);
        
        try {
            jaxrsPipeline.invoke(hstContainerConfig, request, response);
        } catch (Exception e) {
            throw new ContainerException(e);
        } finally {
            jaxrsPipeline.afterInvoke(hstContainerConfig, request, response);
        }
    }
    
}
