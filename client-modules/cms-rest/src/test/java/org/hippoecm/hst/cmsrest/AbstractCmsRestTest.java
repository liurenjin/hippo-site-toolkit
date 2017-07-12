/*
 *  Copyright 2012-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.cmsrest;


import java.util.List;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.container.HstContainerRequestImpl;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.internal.HstRequestContextComponent;
import org.hippoecm.hst.core.internal.MountDecorator;
import org.hippoecm.hst.core.internal.MutableResolvedMount;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.site.addon.module.model.ModuleDefinition;
import org.hippoecm.hst.site.container.ModuleDescriptorUtils;
import org.hippoecm.hst.site.container.SpringComponentManager;
import org.hippoecm.hst.util.GenericHttpServletRequestWrapper;
import org.hippoecm.hst.util.HstRequestUtils;
import org.junit.After;
import org.junit.Before;
import org.onehippo.cms7.services.ServletContextRegistry;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.ServletContextAware;

public abstract class AbstractCmsRestTest {

    protected SpringComponentManager componentManager;
    protected HstManager hstManager;
    private HstSiteMapMatcher siteMapMatcher;
    private HstURLFactory hstURLFactory;
    protected MountDecorator mountDecorator;

    protected MockServletContext servletContext;
    protected MockServletContext servletContext2;

    @Before
    public void setUp() throws Exception {
        componentManager = new SpringComponentManager(getContainerConfiguration());
        componentManager.setConfigurationResources(getConfigurations());
        servletContext = new MockServletContext();
        servletContext.setContextPath("/site");
        ServletContextRegistry.register(servletContext, ServletContextRegistry.WebAppType.HST);

        servletContext2 = new MockServletContext();
        servletContext2.setContextPath("/site2");
        ServletContextRegistry.register(servletContext2, ServletContextRegistry.WebAppType.HST);
        componentManager.setServletContext(servletContext2);

        final List<ModuleDefinition> addonModuleDefinitions = ModuleDescriptorUtils.collectAllModuleDefinitions();
        if (addonModuleDefinitions != null && !addonModuleDefinitions.isEmpty()) {
            componentManager.setAddonModuleDefinitions(addonModuleDefinitions);
        }

        componentManager.initialize();
        componentManager.start();
        HstServices.setComponentManager(getComponentManager());
        hstManager = getComponentManager().getComponent(HstManager.class.getName());

        siteMapMatcher = getComponentManager().getComponent(HstSiteMapMatcher.class.getName());
        hstURLFactory = getComponentManager().getComponent(HstURLFactory.class.getName());
        this.mountDecorator = HstServices.getComponentManager().getComponent(MountDecorator.class.getName());
    }

    @After
    public void tearDown() throws Exception {
        this.componentManager.stop();
        this.componentManager.close();
        ServletContextRegistry.unregister(servletContext);
        ServletContextRegistry.unregister(servletContext2);
        HstServices.setComponentManager(null);
        // always clear HstRequestContext in case it is set on a thread local
        ModifiableRequestContextProvider.clear();
    }

    protected String[] getConfigurations() {
        String classXmlFileName = AbstractCmsRestTest.class.getName().replace(".", "/") + ".xml";
        String classXmlFileName2 = AbstractCmsRestTest.class.getName().replace(".", "/") + "-*.xml";
        return new String[] { classXmlFileName, classXmlFileName2 };
    }

    protected ComponentManager getComponentManager() {
        return this.componentManager;
    }


    protected Session createSession() throws RepositoryException {
        Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName() + ".delegating");
        return repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
    }

    protected Configuration getContainerConfiguration() {
        return new PropertiesConfiguration();
    }


    protected HstRequestContext getRequestFromCms(final String hostAndPort,
                                                  final String pathInfo) throws Exception {
        HstRequestContextComponent rcc = getComponentManager().getComponent(HstRequestContextComponent.class.getName());
        HstMutableRequestContext requestContext = rcc.create();
        HstContainerURL containerUrl = createContainerUrlForCmsRequest(requestContext, hostAndPort, pathInfo);
        requestContext.setBaseURL(containerUrl);
        requestContext.setResolvedMount(getResolvedMount(containerUrl, requestContext));
        requestContext.matchingFinished();
        HstURLFactory hstURLFactory = getComponentManager().getComponent(HstURLFactory.class.getName());
        requestContext.setURLFactory(hstURLFactory);
        requestContext.setSiteMapMatcher(siteMapMatcher);

        return requestContext;
    }

    protected HstContainerURL createContainerUrlForCmsRequest(final HstMutableRequestContext requestContext,
                                                              final String hostAndPort,
                                                              final String pathInfo) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        GenericHttpServletRequestWrapper containerRequest;
        {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setScheme("http");
            String host = hostAndPort.split(":")[0];
            if (hostAndPort.split(":").length > 1) {
                int port = Integer.parseInt(hostAndPort.split(":")[1]);
                request.setLocalPort(port);
                request.setServerPort(port);
            }
            request.setServerName(host);
            request.addHeader("Host", hostAndPort);
            request.setPathInfo(pathInfo);
            request.setContextPath("/site");
            request.setRequestURI("/site" + pathInfo);
            containerRequest = new HstContainerRequestImpl(request, hstManager.getPathSuffixDelimiter());
        }
        requestContext.setServletRequest(containerRequest);
        requestContext.setCmsRequest(true);

        VirtualHosts vhosts = hstManager.getVirtualHosts();
        ResolvedMount mount = vhosts.matchMount(HstRequestUtils.getFarthestRequestHost(containerRequest),
                containerRequest.getContextPath(), HstRequestUtils.getRequestPath(containerRequest));

        // set hst servlet path correct
        if (mount.getMatchingIgnoredPrefix() != null) {
            containerRequest.setServletPath("/" + mount.getMatchingIgnoredPrefix() + mount.getResolvedMountPath());
        } else {
            containerRequest.setServletPath(mount.getResolvedMountPath());
        }
        return hstURLFactory.getContainerURLProvider().parseURL(containerRequest, response, mount);
    }

    protected ResolvedMount getResolvedMount(HstContainerURL url, final HstMutableRequestContext requestContext) throws ContainerException {
        VirtualHosts vhosts = hstManager.getVirtualHosts();
        final ResolvedMount resolvedMount = vhosts.matchMount(url.getHostName(), url.getContextPath(), url.getRequestPath());
        requestContext.setAttribute(ContainerConstants.UNDECORATED_MOUNT, resolvedMount.getMount());
        final Mount decorated = mountDecorator.decorateMountAsPreview(resolvedMount.getMount());
        ((MutableResolvedMount) resolvedMount).setMount(decorated);
        return resolvedMount;
    }

}
