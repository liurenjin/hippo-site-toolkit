/*
 *  Copyright 2012 Hippo.
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


import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManagerImpl;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.beans.AbstractBeanTestCase;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.container.RepositoryNotAvailableException;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.internal.HstRequestContextComponent;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.util.HstRequestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static junit.framework.Assert.assertEquals;

/**
 * <p>
 *  These unit tests are there to make sure that links that are created for requests that originate from the cms (cms
 *  rest calls or website in channel manager) are always over the HOST of the cms and always include the SITE
 *  contextpath. If there are cross-channel links in the site that are normally cross-domain, that should in cms context
 *  result in a querystring containing a rendering_host
 * <p/>
 * <p>
 *  When doing a request from the cms over, say some REST mount, then through the {@link
 *  org.hippoecm.hst.core.request.HstRequestContext#isCmsRequest()} you get <code>true</code>. This value
 *  <code>true</code> comes from the backing http servlet request by HttpServletRequest#setAttribute(ContainerConstants.REQUEST_COMES_FROM_CMS,
 *  Boolean.TRUE);
 * </p>
 * <p>
 *  Apart from this boolean, a CMS request MAY or MAY NOT also set a 'renderingHost' on the
 *  backing http servletrequest Whe a request is done from the CMS through cms rest api, typically this 'renderingHost'
 *  is <code>null</code>. However, when the request is for loading the website in the channel manager, this renderhost is
 *  typically available : Because, requests will be done over the HOST OF THE CMS, the renderHost contains the value of
 *  the host that needs to be 'faked' to render the page.
 * </p>
 */
public class TestHstLinkForCmsRequest extends AbstractBeanTestCase {

    private Repository repository;
    private Credentials credentials;
    private HstManager hstManager;
    private HstURLFactory hstURLFactory;
    private ObjectConverter objectConverter;
    private HstLinkCreator linkCreator;
    private HstSiteMapMatcher siteMapMatcher;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        this.repository = getComponent(Repository.class.getName());
        this.credentials= getComponent(Credentials.class.getName()+".hstconfigreader");
        this.hstManager = getComponent(HstManager.class.getName());
        this.siteMapMatcher = getComponent(HstSiteMapMatcher.class.getName());
        this.hstURLFactory = getComponent(HstURLFactory.class.getName());
        this.objectConverter = getObjectConverter();
        this.linkCreator = getComponent(HstLinkCreator.class.getName());
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();

    }

    @Test
    public void testLinksCMSRequestNoRenderingHost() throws Exception {
        {
            HstRequestContext requestContext = getRequestFromCms("cms.example.com", "/home", null, null, false);
            ObjectBeanManager obm = new ObjectBeanManagerImpl(requestContext.getSession(), objectConverter);
            Object homeBean = obm.getObject("/unittestcontent/documents/unittestproject/common/homepage");
            HstLink homePageLink = linkCreator.create((HippoBean) homeBean, requestContext);
            assertEquals("link.getPath for homepage node should be '", "", homePageLink.getPath());
            // A link in CMS request context for the HOMEPAGE should NOT be /site like for normal site requests,
            // but should be /site/ to work well with PROXIES using /site/ to match on. Hence, /site/ is expected
            assertEquals("wrong absolute link for homepage for CMS context", "/site/", (homePageLink.toUrlForm(requestContext, false)));
    
            // A fully qualified link for CMS request context for should NOT be fully qualified, even for toUrlForm(requestContext, TRUE))
            // CMS links must always be relative to the CMS host! Thus no http://localhost involved
            assertEquals("wrong fully qualified url for homepage for CMS context", "/site/", (homePageLink.toUrlForm(requestContext, true)));
    
            Node newsNode = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject/News/News1");
            HstLink newsLink = linkCreator.create(newsNode, requestContext);
            assertEquals("wrong link.getPath for News/News1 for CMS context","news/News1.html", newsLink.getPath());
            assertEquals("wrong absolute link for News/News1 for CMS context" ,"/site/news/News1.html", (newsLink.toUrlForm(requestContext, false)));
            assertEquals("wrong fully qualified url for News/News1 for CMS context" ,"/site/news/News1.html", (newsLink.toUrlForm(requestContext, true)));
    
            // link for an image in cms context should also start with context path
            HstLink imageLink = linkCreator.create("/images/mythumbnail.gif", requestContext.getResolvedMount().getMount());
            assertEquals("wrong absolute link for images/mythumbnail.gif for CMS context" ,"/site/images/mythumbnail.gif", (imageLink.toUrlForm(requestContext, false)));
            assertEquals("wrong fully qualified url for images/mythumbnail.gif for CMS context" ,"/site/images/mythumbnail.gif", (imageLink.toUrlForm(requestContext, true)));
        }
        // NOW we do the same tests as above, but we first SET the showcontextpath on hst:hst/hst:hosts to FALSE : 
        // EVEN when contextpath is set to FALSE, for the URLs in cms context, still the contextpath should be included.
        // When acessing the site over the host of the cms, ALWAYS the context path needs to be included
        Session session = repository.login(credentials);
        Node hstHostsNode = session.getNode("/hst:hst/hst:hosts");
        boolean before = hstHostsNode.getProperty("hst:showcontextpath").getBoolean();
        hstHostsNode.setProperty("hst:showcontextpath", false);
        session.save();
        // wait to be sure async jcr event arrived
        Thread.sleep(50);
        // NOW below, even when show contextpath is FALSE, the /site contextpath should be included because the links are
        // for cms host
        {
            HstRequestContext requestContext = getRequestFromCms("cms.example.com", "/home", null, null, false);
            ObjectBeanManager obm = new ObjectBeanManagerImpl(requestContext.getSession(), objectConverter);
            Object homeBean = obm.getObject("/unittestcontent/documents/unittestproject/common/homepage");
            HstLink homePageLink = linkCreator.create((HippoBean) homeBean, requestContext);
            assertEquals("link.getPath for homepage node should be '", "", homePageLink.getPath());
            // A link in CMS request context for the HOMEPAGE should NOT be /site like for normal site requests,
            // but should be /site/ to work well with PROXIES using /site/ to match on. Hence, /site/ is expected
            assertEquals("wrong absolute link for homepage for CMS context", "/site/", (homePageLink.toUrlForm(requestContext, false)));

            // A fully qualified link for CMS request context for should NOT be fully qualified, even for toUrlForm(requestContext, TRUE))
            // CMS links must always be relative to the CMS host! Thus no http://localhost involved
            assertEquals("wrong fully qualified url for homepage for CMS context", "/site/", (homePageLink.toUrlForm(requestContext, true)));

            Node newsNode = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject/News/News1");
            HstLink newsLink = linkCreator.create(newsNode, requestContext);
            assertEquals("wrong link.getPath for News/News1 for CMS context","news/News1.html", newsLink.getPath());
            assertEquals("wrong absolute link for News/News1 for CMS context" ,"/site/news/News1.html", (newsLink.toUrlForm(requestContext, false)));
            assertEquals("wrong fully qualified url for News/News1 for CMS context" ,"/site/news/News1.html", (newsLink.toUrlForm(requestContext, true)));

            // link for an image in cms context should also start with context path
            HstLink imageLink = linkCreator.create("/images/mythumbnail.gif", requestContext.getResolvedMount().getMount());
            assertEquals("wrong absolute link for images/mythumbnail.gif for CMS context" ,"/site/images/mythumbnail.gif", (imageLink.toUrlForm(requestContext, false)));
            assertEquals("wrong fully qualified url for images/mythumbnail.gif for CMS context" ,"/site/images/mythumbnail.gif", (imageLink.toUrlForm(requestContext, true)));
        }
        // set the value again to original.
        hstHostsNode.setProperty("hst:showcontextpath", before);
        session.save();
        session.logout();
        
    }


    @Test
    public void testLinksCMSRequestWITHRenderingHost() throws Exception {
        // the rendering host is www.unit.test
        HstRequestContext requestContext = getRequestFromCms("cms.example.com", "/home", null, "www.unit.test", false);
        // assert that the match Mount is www.unit.test
        assertEquals("Matched mount should be the renderHost mount", "www.unit.test", requestContext.getResolvedMount().getResolvedVirtualHost().getResolvedHostName());

        ObjectBeanManager obm = new ObjectBeanManagerImpl(requestContext.getSession(), objectConverter);
        Object homeBean = obm.getObject("/unittestcontent/documents/unittestproject/common/homepage");
        {
            // link will be for www.unit.test Mount because can be created for current renderHost
            HstLink homePageLink = linkCreator.create((HippoBean) homeBean, requestContext);
            assertEquals("www.unit.test", homePageLink.getMount().getVirtualHost().getHostName());
    
            assertEquals("link.getPath for homepage node should be '", "", homePageLink.getPath());
            // A link in CMS request context for the HOMEPAGE should NOT be /site like for normal site requests,
            // but should be /site/ to work well with PROXIES using /site/ to match on. Hence, /site/ is expected
            // SINCE RENDER HOST stays the same, no render host is included in query string
            assertEquals("wrong absolute link for homepage for CMS context", "/site/", (homePageLink.toUrlForm(requestContext, false)));
    
            // A fully qualified link for CMS request context for should NOT be fully qualified, even for toUrlForm(requestContext, TRUE))
            // CMS links must always be relative to the CMS host! Thus no http://localhost involved
            // SINCE RENDER HOST stays the same, no render host is included in query string
            assertEquals("wrong fully qualified url for homepage for CMS context", "/site/", (homePageLink.toUrlForm(requestContext, true)));
        }

        {
            // NOW, ew create homepage link for another MOUNT. This should include a renderhost in the queryString
    
            HstLink homePageLinkForMobile = linkCreator.create(((HippoBean) homeBean).getNode(), requestContext, "mobile");
            String hostName = homePageLinkForMobile.getMount().getVirtualHost().getHostName();
            assertEquals("m.unit.test", hostName);
            assertEquals("link.getPath for homepage node should be '", "", homePageLinkForMobile.getPath());
          
            // renderhost should be included
            assertEquals("wrong absolute link for homepage for CMS context", "/site/?"+ContainerConstants.RENDERING_HOST+"="+hostName, (homePageLinkForMobile.toUrlForm(requestContext, false)));

            // renderhost should be included
            assertEquals("wrong fully qualified url for homepage for CMS context", "/site/?"+ContainerConstants.RENDERING_HOST+"="+hostName, (homePageLinkForMobile.toUrlForm(requestContext, true)));
        }
    }

    /**
     * Even when a render host is set, when there is also an indication on the request that the client host should be forced, then
     * the render host should be skipped. This is the case where for example on the http session the renderhost is stored, but
     * the request should not used this stored renderhost. Then, with the parameter FORCE_CLIENT_HOST = true this can be indicated
     * @throws Exception
     */
    @Test
    public void testLinksCMSRequestWITHRenderingHostAndForceClientHost() throws Exception {
        // the rendering host is www.unit.test but we also indicate FORCE_CLIENT_HOST = true
        HstRequestContext requestContext = getRequestFromCms("cms.example.com", "/home", null, "www.unit.test", true);
        // even though the renderingHost www.unit.test is set
        
        // Since hst:defaulthostname is localhost, with 'force client host' and client host 'cms.example.com' which does
        // not have a configured mount, a fallback to localhost should be seen:
        assertEquals("Matched mount should be the renderHost mount", "localhost",
                requestContext.getResolvedMount().getResolvedVirtualHost().getResolvedHostName());


        // when not forcing client host, we should get www.unit.test as the matched mount its hostname

        requestContext = getRequestFromCms("cms.example.com", "/home", null, "www.unit.test", false);
        assertEquals("Matched mount should be the renderHost mount", "www.unit.test",
                requestContext.getResolvedMount().getResolvedVirtualHost().getResolvedHostName());

    }

    public HstRequestContext getRequestFromCms(final String hostAndPort,
                                               final String requestURI,
                                               final String queryString,
                                               final String renderingHost,
                                               final boolean forceClientHost) throws Exception {
        HstRequestContextComponent rcc = getComponent(HstRequestContextComponent.class.getName());
        HstMutableRequestContext requestContext = rcc.create(false);
        HstContainerURL containerUrl = createContainerUrlForCmsRequest(requestContext, hostAndPort, requestURI, queryString, renderingHost, forceClientHost);
        requestContext.setBaseURL(containerUrl);
        ResolvedSiteMapItem resolvedSiteMapItem = getResolvedSiteMapItem(containerUrl);
        requestContext.setResolvedSiteMapItem(resolvedSiteMapItem);
        requestContext.setResolvedMount(resolvedSiteMapItem.getResolvedMount());
        HstURLFactory hstURLFactory = getComponent(HstURLFactory.class.getName());
        requestContext.setURLFactory(hstURLFactory);
        requestContext.setSiteMapMatcher(siteMapMatcher);

        requestContext.setRenderHost(renderingHost);

        return requestContext;
    }

    public HstContainerURL createContainerUrlForCmsRequest(final HstMutableRequestContext requestContext,
                                                           final String hostAndPort,
                                                           final String requestURI,
                                                           final String queryString,
                                                           final String renderingHost,
                                                           final boolean forceClientHost) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();
        requestContext.setServletRequest(request);
        String host = hostAndPort.split(":")[0];
        if (hostAndPort.split(":").length > 1) {
            int port = Integer.parseInt(hostAndPort.split(":")[1]);
            request.setLocalPort(port);
            request.setServerPort(port);
        }
        request.setScheme("http");
        request.setServerName(host);
        request.addHeader("Host", hostAndPort);
        request.setContextPath("/site");
        request.setQueryString(queryString);
        request.setRequestURI("/site" + requestURI);

        // set the magic attributes that are used to indicate that some request is triggered by cms (channel manager)
        request.setAttribute(ContainerConstants.REQUEST_COMES_FROM_CMS, Boolean.TRUE);
        if (renderingHost != null) {
            request.setParameter(ContainerConstants.RENDERING_HOST, renderingHost);
        }
        if (forceClientHost) {
            request.setParameter("FORCE_CLIENT_HOST", "true");
        }
        VirtualHosts vhosts = hstManager.getVirtualHosts();
        System.out.println(HstRequestUtils.getFarthestRequestHost(request));
        ResolvedMount mount = vhosts.matchMount(HstRequestUtils.getFarthestRequestHost(request), request.getContextPath(), HstRequestUtils.getRequestPath(request));
        return hstURLFactory.getContainerURLProvider().parseURL(request, response, mount);
    }

    public ResolvedSiteMapItem getResolvedSiteMapItem(HstContainerURL url) throws RepositoryNotAvailableException {
        VirtualHosts vhosts = hstManager.getVirtualHosts();
        return vhosts.matchSiteMapItem(url);
    }


}
