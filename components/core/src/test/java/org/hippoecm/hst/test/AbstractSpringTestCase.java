/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.internal.HstRequestContextComponent;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.site.container.SpringComponentManager;
import org.hippoecm.hst.util.GenericHttpServletRequestWrapper;
import org.hippoecm.hst.util.HstRequestUtils;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.onehippo.cms7.services.ServletContextRegistry;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

/**
 * <p>
 * AbstractSpringTestCase
 * </p>
 * <p>
 *
 * </p>
 *
 *
 */
public abstract class AbstractSpringTestCase
{

    protected final static Logger log = LoggerFactory.getLogger(AbstractSpringTestCase.class);
    protected ComponentManager componentManager;

    protected final MockServletContext servletContext = new MockServletContext();

    @BeforeClass
    public static void clearRepository() {
        // when run together with other RepositoryTestCase based tests *and*
        // -Dorg.onehippo.repository.test.keepserver=true
        // then an existing RepositoryImpl may already be (kept) running, which can interfere with this test
        RepositoryTestCase.clearRepository();
    }

    @AfterClass
    public static void shutDownRepository() throws RepositoryException {
        // after all methods of the class have finished we need to close the repository to unregister all services in the
        // HippoServiceRegistry. Otherwise we get issues with IT tests that extend from RepositoryTestCase vs the IT tests
        // that use spring wiring
        HippoRepository hippoRepository = HippoRepositoryFactory.getHippoRepository();
        if (hippoRepository != null) {
            hippoRepository.close();
        }
    }

    @Before
    public void setUp() throws Exception {
        componentManager = new SpringComponentManager(getContainerConfiguration());
        componentManager.setConfigurationResources(getConfigurations());

        servletContext.setContextPath("/site");
        ServletContextRegistry.register(servletContext, ServletContextRegistry.WebAppType.HST);

        componentManager.setServletContext(servletContext);
        componentManager.initialize();
        componentManager.start();
        HstServices.setComponentManager(getComponentManager());
    }

    @After
    public void tearDown() throws Exception {
        this.componentManager.stop();
        this.componentManager.close();
        ServletContextRegistry.unregister(servletContext);
        HstServices.setComponentManager(null);
        // always clear HstRequestContext in case it is set on a thread local
        ModifiableRequestContextProvider.clear();
    }

    /**
     * required specification of spring configurations
     * the derived class can override this.
     */
    protected String[] getConfigurations() {
        String classXmlFileName = getClass().getName().replace(".", "/") + ".xml";
        String classXmlFileName2 = getClass().getName().replace(".", "/") + "-*.xml";
        return new String[] { classXmlFileName, classXmlFileName2 };
    }
    
    protected ComponentManager getComponentManager() {
        return this.componentManager;
    }

    protected <T> T getComponent(String name) {
        return getComponentManager().getComponent(name);
    }
    
    protected Configuration getContainerConfiguration() {
        return new PropertiesConfiguration();
    }


    protected void setRequestInfo(final MockHttpServletRequest request,
                                  final String contextPath,
                                  final String pathInfo) {
        request.setPathInfo(pathInfo);
        request.setContextPath(contextPath);
        request.setRequestURI(contextPath + request.getServletPath() + pathInfo);
    }


    protected void setHstServletPath(final GenericHttpServletRequestWrapper request, final ResolvedMount resolvedMount) {
        if (resolvedMount.getMatchingIgnoredPrefix() != null) {
            request.setServletPath("/" + resolvedMount.getMatchingIgnoredPrefix() + resolvedMount.getResolvedMountPath());
        } else {
            request.setServletPath(resolvedMount.getResolvedMountPath());
        }
    }


    protected void setResolvedMount(HstMutableRequestContext requestContext) {
        
        ResolvedMount resolvedMount = createNiceMock(ResolvedMount.class);
        Mount mount = createNiceMock(Mount.class);
        VirtualHost virtualHost = createNiceMock(VirtualHost.class);
        VirtualHosts virtualHosts = createNiceMock(VirtualHosts.class);
        HstManager hstManager = createNiceMock(HstManager.class);
        
        expect(resolvedMount.getResolvedMountPath()).andReturn("").anyTimes();
        expect(resolvedMount.getMount()).andReturn(mount).anyTimes();
        expect(mount.isContextPathInUrl()).andReturn(true).anyTimes();
        expect(mount.getVirtualHost()).andReturn(virtualHost).anyTimes();
        expect(virtualHost.isContextPathInUrl()).andReturn(true).anyTimes();

        expect(virtualHost.getVirtualHosts()).andReturn(virtualHosts).anyTimes();
        expect(virtualHosts.getHstManager()).andReturn(hstManager).anyTimes();
        expect(hstManager.getPathSuffixDelimiter()).andReturn("./").anyTimes();
        
        replay(resolvedMount);
        replay(mount);
        replay(virtualHost);
        replay(virtualHosts);
        replay(hstManager);
        
        // to parse a url, there must be a ResolvedMount on the HstRequestContext
        requestContext.setResolvedMount(resolvedMount);
    }
    

    protected HstRequestContext resolveRequest(HttpServletRequest request, HttpServletResponse response) throws ContainerException {
        HstManager hstSitesManager = HstServices.getComponentManager().getComponent(HstManager.class.getName());
        VirtualHosts vHosts = hstSitesManager.getVirtualHosts();
        HstMutableRequestContext requestContext = ((HstRequestContextComponent)HstServices.getComponentManager().getComponent(HstRequestContextComponent.class.getName())).create();
        requestContext.setServletRequest(request);
        requestContext.setServletResponse(response);
        request.setAttribute(ContainerConstants.HST_REQUEST_CONTEXT, requestContext);
        ResolvedMount mount = vHosts.matchMount(HstRequestUtils.getFarthestRequestHost(request), request.getContextPath() , HstRequestUtils.getRequestPath(request));     
        requestContext.setResolvedMount(mount);
        // now we can parse the url *with* a RESOLVED_MOUNT which is needed!        
        HstURLFactory factory = HstServices.getComponentManager().getComponent(HstURLFactory.class.getName());
        HstContainerURL hstContainerURL = factory.getContainerURLProvider().parseURL(request, response, mount);
        ResolvedSiteMapItem resolvedSiteMapItem = mount.matchSiteMapItem(hstContainerURL.getPathInfo());
        requestContext.setBaseURL(hstContainerURL);
        requestContext.setResolvedSiteMapItem(resolvedSiteMapItem);
        requestContext.matchingFinished();
        return requestContext;
    }
}
