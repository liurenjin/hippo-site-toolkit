/*
 *  Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.test;

import java.io.IOException;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.Filter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.site.container.SpringComponentManager;
import org.junit.After;
import org.junit.Before;
import org.onehippo.cms7.services.ServletContextRegistry;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;

import static org.junit.Assert.assertTrue;

public class AbstractFullRequestCycleTest {

    private static final Logger log = LoggerFactory.getLogger(AbstractFullRequestCycleTest.class);

    protected SpringComponentManager componentManager;
    protected final MockServletContext servletContext = new MockServletContext();

    protected Filter filter;

    @Before
    public void setUp() throws Exception {

        String hstTestPropertiesFileName = StringUtils.substringBeforeLast(AbstractFullRequestCycleTest.class.getName().replace(".", "/"), "/") + "/hst-test.properties";
        PropertiesConfiguration configuration = new PropertiesConfiguration(hstTestPropertiesFileName);

        Configuration custom = null;
        try {
            custom = loadCustomProperties();
        } catch (ConfigurationException e) {
            log.warn("Failed to load custom properties.", e);
        }

        CompositeConfiguration composite = new CompositeConfiguration();
        composite.addConfiguration(configuration);
        if (custom != null) {
            composite.addConfiguration(custom);
        }

        componentManager = new SpringComponentManager(composite);
        componentManager.setConfigurationResources(getResourceConfigurations());

        servletContext.setContextPath("/site");
        ServletContextRegistry.register(servletContext, ServletContextRegistry.WebAppType.HST);

        componentManager.setServletContext(servletContext);

        componentManager.initialize();
        componentManager.start();

        Thread.sleep(10000);

        HstServices.setComponentManager(getComponentManager());
        filter = HstServices.getComponentManager().getComponent("org.hippoecm.hst.container.HstFilter");


        // assert admin has hippo:admin privilege
        Session admin = createSession("admin", "admin");
        assertTrue(admin.hasPermission("/hst:hst", "jcr:write"));
        assertTrue(admin.hasPermission("/hst:hst/hst:channels", "hippo:admin"));
        admin.logout();

        // assert editor is part of webmaster group
        final Session editor = createSession("editor", "editor");
        assertTrue(editor.hasPermission("/hst:hst", "jcr:write"));
        editor.logout();
    }

    protected Configuration loadCustomProperties() throws ConfigurationException {
       return null;
    }

    @After
    public void tearDown() throws Exception {
        this.componentManager.stop();
        this.componentManager.close();
        ServletContextRegistry.unregister(servletContext);
        HstServices.setComponentManager(null);
        ModifiableRequestContextProvider.clear();

    }

    protected String[] getResourceConfigurations() {
        String classXmlFileName = AbstractFullRequestCycleTest.class.getName().replace(".", "/") + ".xml";
        String classXmlFileName2 = AbstractFullRequestCycleTest.class.getName().replace(".", "/") + "-*.xml";
        return new String[]{classXmlFileName, classXmlFileName2};
    }

    protected ComponentManager getComponentManager() {
        return this.componentManager;
    }

    protected Session createSession(final String userName, final String password) throws RepositoryException {
        Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName() + ".delegating");
        return repository.login(new SimpleCredentials(userName, password.toCharArray()));
    }

    public MockHttpServletResponse process(final RequestResponseMock requestResponse) throws IOException, ServletException {
        final MockHttpServletRequest request = requestResponse.getRequest();
        final MockHttpServletResponse response = requestResponse.getResponse();

        filter.doFilter(request, response, new MockFilterChain(new HttpServlet() {
            @Override
            protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
                super.doGet(req, resp);
            }
        }, filter));
        return response;
    }

    public MockHttpServletResponse processCMSRequest(final RequestResponseMock requestResponse, final Credentials authenticatedCmsUser) throws IOException, ServletException {
        final MockHttpServletRequest request = requestResponse.getRequest();

        final MockHttpSession mockHttpSession;
        if (request.getSession(false) == null) {
            mockHttpSession = new MockHttpSession();
            request.setSession(mockHttpSession);
        } else {
            mockHttpSession = (MockHttpSession)request.getSession();
        }

        mockHttpSession.setAttribute(CmsSessionContext.SESSION_KEY, new CmsSessionContextMock(authenticatedCmsUser));

        final MockHttpServletResponse response = requestResponse.getResponse();

        filter.doFilter(request, response, new MockFilterChain(new HttpServlet() {
            @Override
            protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
                super.doGet(req, resp);
            }
        }, filter));
        return response;
    }

    // TODO replace with fluent api MockRequestResponseBuilder
    public RequestResponseMock mockRequestResponse(final String hostAndPort,
                                                   final String pathInfo) {
        return mockRequestResponse(hostAndPort, pathInfo, null);
    }

    public RequestResponseMock mockRequestResponse(final String hostAndPort,
                                                   final String pathInfo,
                                                   final String queryString) {
        return mockRequestResponse(hostAndPort, pathInfo, queryString, "GET");
    }

    public RequestResponseMock mockRequestResponse(final String hostAndPort,
                                                   final String pathInfo,
                                                   final String queryString,
                                                   final String method) {
        return mockRequestResponse(hostAndPort, pathInfo, queryString, method, "http");
    }

    public RequestResponseMock mockRequestResponse(final String hostAndPort,
                                                   final String pathInfo,
                                                   final String queryString,
                                                   final String method,
                                                   final String scheme) {
        return mockRequestResponse(hostAndPort, pathInfo, queryString, method, scheme, "/site");
    }

    /**
     * @param scheme      http or https
     * @param hostAndPort eg localhost:8080 or www.example.com
     * @param pathInfo    the request pathInfo, starting with a slash
     * @param queryString optional query string
     * @return RequestResponseMock containing {@link MockHttpServletRequest} and {@link MockHttpServletResponse}
     * @throws Exception
     */
    public RequestResponseMock mockRequestResponse(final String hostAndPort,
                                                   final String pathInfo,
                                                   final String queryString,
                                                   final String method,
                                                   final String scheme,
                                                   final String contextPath) {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();

        String host = hostAndPort.split(":")[0];
        if (hostAndPort.split(":").length > 1) {
            int port = Integer.parseInt(hostAndPort.split(":")[1]);
            request.setLocalPort(port);
            request.setServerPort(port);
        }
        if (scheme == null) {
            request.setScheme("http");
        } else {
            request.setScheme(scheme);
        }
        request.setServerName(host);
        request.addHeader("Host", hostAndPort);
        request.setPathInfo(pathInfo);
        request.setContextPath(contextPath);
        request.setRequestURI(contextPath + pathInfo);
        request.setMethod(method);
        if (queryString != null) {
            request.setQueryString(queryString);
        }

        return new RequestResponseMock(request, response);
    }

    public static class RequestResponseMock {
        MockHttpServletRequest request;
        MockHttpServletResponse response;

        public RequestResponseMock(final MockHttpServletRequest request, final MockHttpServletResponse response) {
            this.request = request;
            this.response = response;

        }

        public MockHttpServletRequest getRequest() {
            return request;
        }

        public MockHttpServletResponse getResponse() {
            return response;
        }

    }

    public static class CmsSessionContextMock implements CmsSessionContext {

        private SimpleCredentials credentials;

        public CmsSessionContextMock(Credentials credentials) {
            this.credentials = (SimpleCredentials)credentials;
        }

        @Override
        public String getId() {
            return null;
        }

        @Override
        public String getCmsContextServiceId() {
            return null;
        }

        @Override
        public Object get(final String key) {
            return CmsSessionContext.REPOSITORY_CREDENTIALS.equals(key) ? credentials : null;
        }
    }
}


