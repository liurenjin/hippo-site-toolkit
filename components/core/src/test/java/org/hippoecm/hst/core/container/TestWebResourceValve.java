/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.container;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.easymock.EasyMock;
import org.hippoecm.hst.cache.CacheElement;
import org.hippoecm.hst.cache.HstCache;
import org.hippoecm.hst.cache.webresources.CacheableWebResource;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.mock.core.component.MockHstRequest;
import org.hippoecm.hst.mock.core.component.MockHstResponse;
import org.hippoecm.hst.mock.core.component.MockValveContext;
import org.hippoecm.hst.mock.core.request.MockHstRequestContext;
import org.hippoecm.hst.mock.core.request.MockResolvedSiteMapItem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.webresources.Binary;
import org.onehippo.cms7.services.webresources.WebResource;
import org.onehippo.cms7.services.webresources.WebResourceBundle;
import org.onehippo.cms7.services.webresources.WebResourceException;
import org.onehippo.cms7.services.webresources.WebResourcesService;
import org.onehippo.repository.mock.MockNode;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestWebResourceValve {

    public static final String STYLE_CSS_CONTENTS = "/* example css */";
    private MockHstRequest request;
    private MockHstRequestContext requestContext;
    private MockHstResponse response;
    private MockValveContext valveContext;
    private WebResourceValve valve;
    private HstCache cache;
    private WebResourcesService webResourcesService;
    private WebResourceBundle webResourceBundle;
    private WebResource webResource;

    @Before
    public void setUp() throws Exception {
        request = new MockHstRequest();

        requestContext = new MockHstRequestContext();
        final Session session = MockNode.root().getSession();
        requestContext.setSession(session);
        mockContextPath("site", requestContext);
        mockResolvedSiteMapItem("css/style.css", "bundleVersion", requestContext);
        request.setRequestContext(requestContext);

        response = new MockHstResponse();
        valveContext = new MockValveContext(request, response);
        valve = new WebResourceValve();

        cache = EasyMock.createNiceMock(HstCache.class);
        valve.setWebResourceCache(cache);

        webResourcesService = EasyMock.createMock(WebResourcesService.class);
        webResourceBundle = EasyMock.createMock(WebResourceBundle.class);
        webResource = EasyMock.createMock(WebResource.class);
        expect(webResourcesService.getJcrWebResourceBundle(eq(session), eq("site"))).andReturn(webResourceBundle);
        HippoServiceRegistry.registerService(webResourcesService, WebResourcesService.class);
    }

    private void replayMocks() {
        replay(cache, webResourcesService, webResourceBundle, webResource);
    }

    private static void mockContextPath(final String contextPath, final MockHstRequestContext requestContext) throws RepositoryException {
        final ResolvedMount resolvedMount = EasyMock.createMock(ResolvedMount.class);
        final Mount mount = EasyMock.createMock(Mount.class);
        expect(resolvedMount.getMount()).andReturn(mount);
        expect(mount.getContextPath()).andReturn(contextPath);
        replay(resolvedMount, mount);
        requestContext.setResolvedMount(resolvedMount);
    }

    private static void mockResolvedSiteMapItem(final String relativeContentPath, final String version, final MockHstRequestContext requestContext) throws RepositoryException {
        final MockResolvedSiteMapItem resolvedSiteMapItem = new MockResolvedSiteMapItem();
        resolvedSiteMapItem.setRelativeContentPath(relativeContentPath);
        resolvedSiteMapItem.addParameter("version", version);
        requestContext.setResolvedSiteMapItem(resolvedSiteMapItem);
    }

    private static WebResource styleCss() {
        final WebResource styleCss = EasyMock.createMock(WebResource.class);
        expect(styleCss.getPath()).andReturn("/css/style.css");
        expect(styleCss.getName()).andReturn("style.css");
        expect(styleCss.getMimeType()).andReturn("text/css");
        expect(styleCss.getEncoding()).andReturn("UTF-8");
        expect(styleCss.getLastModified()).andReturn(Calendar.getInstance());

        final Binary binary = EasyMock.createNiceMock(Binary.class);
        byte[] data = STYLE_CSS_CONTENTS.getBytes();
        expect(binary.getSize()).andReturn((long)data.length);
        expect(binary.getStream()).andReturn(new ByteArrayInputStream(data));
        expect(styleCss.getBinary()).andReturn(binary);

        replay(styleCss, binary);

        return styleCss;
    }

    @After
    public void tearDown() throws Exception {
        HippoServiceRegistry.unregisterService(webResourcesService, WebResourcesService.class);
    }

    @Test
    public void uncached_web_resource_from_workspace_is_cached() throws ContainerException, UnsupportedEncodingException {
        expect(webResourceBundle.getAntiCacheValue()).andReturn("bundleVersion");
        expect(webResourceBundle.get("/css/style.css")).andReturn(styleCss());
        expect(cache.createElement(anyObject(), anyObject())).andReturn(EasyMock.createMock(CacheElement.class));
        replayMocks();

        valve.invoke(valveContext);

        verify(cache);
        assertStyleCssIsWritten();
        assertTrue("Next valve should have been invoked", valveContext.isNextValveInvoked());
    }

    @Test
    public void uncached_web_resource_from_history_is_cached() throws ContainerException, UnsupportedEncodingException {
        expect(webResourceBundle.getAntiCacheValue()).andReturn("antiCacheValueThatIsNotEqualToTheBundleVersion");
        expect(webResourceBundle.get("/css/style.css", "bundleVersion")).andReturn(styleCss());
        expect(cache.createElement(anyObject(), anyObject())).andReturn(EasyMock.createMock(CacheElement.class));
        replayMocks();

        valve.invoke(valveContext);

        verify(cache);
        assertStyleCssIsWritten();
        assertTrue("Next valve should have been invoked", valveContext.isNextValveInvoked());
    }

    @Test
    public void cached_web_resource_is_served_from_cache() throws ContainerException, IOException {
        final CacheElement cacheElement = EasyMock.createMock(CacheElement.class);
        expect(cacheElement.getContent()).andReturn(new CacheableWebResource(styleCss()));
        replay(cacheElement);

        expect(cache.get(anyObject())).andReturn(cacheElement);
        replayMocks();

        valve.invoke(valveContext);

        verify(cache);
        assertStyleCssIsWritten();
        assertTrue("Next valve should have been invoked", valveContext.isNextValveInvoked());
    }

    @Test
    public void error_while_caching_clears_lock_and_stops_valve_invocation() throws UnsupportedEncodingException {
        expect(webResourceBundle.getAntiCacheValue()).andReturn("bundleVersion");
        expect(webResourceBundle.get("/css/style.css")).andReturn(styleCss());
        expect(cache.createElement(anyObject(), anyObject())).andThrow(new RuntimeException("simulate error while caching"));
        expect(cache.createElement(anyObject(), eq(null))).andReturn(null);
        replayMocks();

        try {
            valve.invoke(valveContext);
        } catch (ContainerException expected) {
            verify(cache);
            assertEquals("nothing should be written to the response", "", response.getContentAsString());
            assertFalse("Next valve should not have been invoked", valveContext.isNextValveInvoked());
            return;
        }
        fail("expected a ContainerException");
    }

    @Test
    public void unknown_web_resource_clears_lock_and_sets_not_found_status() throws UnsupportedEncodingException, ContainerException {
        expect(webResourceBundle.getAntiCacheValue()).andReturn("bundleVersion");
        expect(webResourceBundle.get("/css/style.css")).andThrow(new WebResourceException("simulate unknown web resource"));
        expect(cache.createElement(anyObject(), eq(null))).andReturn(null);
        replayMocks();

        valve.invoke(valveContext);

        verify(cache);
        assertEquals("nothing should be written to the response", "", response.getContentAsString());
        assertFalse("Next valve should not have been invoked", valveContext.isNextValveInvoked());
        assertEquals("response code", 404, response.getStatusCode());
    }

    private void assertStyleCssIsWritten() throws UnsupportedEncodingException {
        final WebResource styleCss = styleCss();

        final Map<String, List<Object>> headers = response.getHeaders();
        assertEquals("Content-Length header", String.valueOf(styleCss.getBinary().getSize()), headers.get("Content-Length").get(0));
        assertEquals("Content type", styleCss.getMimeType(), response.getContentType());
        assertTrue("Expires in the future", ((Date) headers.get("Expires").get(0)).after(Calendar.getInstance().getTime()));
        assertEquals("Cache-Control header", "max-age=31536000", headers.get("Cache-Control").get(0));

        assertEquals("written web resource", STYLE_CSS_CONTENTS, response.getContentAsString());
    }

}