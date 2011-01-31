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
package org.hippoecm.hst.content.rewriter.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.jcr.Node;

import org.apache.commons.lang.StringUtils;
import org.easymock.EasyMock;
import org.hippoecm.hst.content.rewriter.ContentRewriter;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.linking.HstLink;
import org.junit.Before;
import org.junit.Test;

public class TestSimpleContentRewriter {

    private static final String EMPTY_BODY_HTML = 
        "<html>\n" + 
        "<head>\n" + 
        "<title>Hello</title>\n" + 
        "</head>\n" + 
        "<body></body>\n" + 
        "</html>";
    
    private static final String NULL_BODY_HTML = 
        "<html>\n" + 
        "<head>\n" + 
        "<title>Hello</title>\n" + 
        "</head>\n" + 
        "<body/>\n" + 
        "</html>";
    
    private static final String CONTENT_ONLY_HTML = 
        "<div>\n" + 
        "<h1>Hello, World!</h1>\n" + 
        "<p>Test</p>\n" + 
        "</div>";
    
    private static final String CONTENT_WITH_LINKS = 
        "<div>\n" + 
        "<h1>Hello, World!</h1>\n" + 
        "<p>Test</p>\n" + 
        "<a href=\"/foo/bar\">Foo1</a>\n" +
        "<a href=\"/foo/bar?a=b\">Foo2</a>\n" +
        "</div>";
    
    private Node node;
    private HstRequest request;
    private HstResponse response;
    
    @Before
    public void setUp() {
        node = EasyMock.createNiceMock(Node.class);
        request = EasyMock.createNiceMock(HstRequest.class);
        response = EasyMock.createNiceMock(HstResponse.class);
        
        EasyMock.replay(node);
        EasyMock.replay(request);
        EasyMock.replay(response);
    }
    
    @Test
    public void testEmptyBodyHtml() {
        ContentRewriter<String> rewriter = new SimpleContentRewriter();
        String html = rewriter.rewrite(EMPTY_BODY_HTML, node, request, response);
        assertEquals("", html);
    }
    
    @Test
    public void testNullBodyHtml() {
        ContentRewriter<String> rewriter = new SimpleContentRewriter();
        String html = rewriter.rewrite(NULL_BODY_HTML, node, request, response);
        assertNull(html);
    }
    
    @Test
    public void testContentOnlyHtml() {
        ContentRewriter<String> rewriter = new SimpleContentRewriter();
        String html = rewriter.rewrite(CONTENT_ONLY_HTML, node, request, response);
        assertEquals(CONTENT_ONLY_HTML, html);
    }
    
    @Test
    public void testContentWithLinks() {
        ContentRewriter<String> rewriter = new SimpleContentRewriter() {
            // overriding to mimic the hst link creator's behavior here.
            @Override
            protected HstLink getDocumentLink(String path, Node node, HstRequest request, HstResponse response) {
                String docPath = StringUtils.substringBefore(path, "?");
                String queryString = StringUtils.substringAfter(path, "?");
                HstLink link = EasyMock.createNiceMock(HstLink.class);
                EasyMock.expect(link.getPath()).andReturn(docPath).anyTimes();
                String url = null;
                try {
                    url = "/site/preview" + docPath;
                    if (!StringUtils.isEmpty(queryString)) {
                        url += URLEncoder.encode("?" + queryString, "ISO-8859-1");
                    }
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
                EasyMock.expect(link.toUrlForm(request, response, false)).andReturn(url).anyTimes();
                EasyMock.replay(link);
                return link;
            }
        };
        
        String html = rewriter.rewrite(CONTENT_WITH_LINKS, node, request, response);
        System.out.println("html: " + html);
        assertTrue(html.contains("/foo/bar?a=b"));
    }
}
