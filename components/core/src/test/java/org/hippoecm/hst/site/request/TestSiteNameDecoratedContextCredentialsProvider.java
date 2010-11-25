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
package org.hippoecm.hst.site.request;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;

import org.hippoecm.hst.configuration.hosting.SiteMount;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.core.request.ContextCredentialsProvider;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.request.ResolvedSiteMount;
import org.junit.Before;
import org.junit.Test;


public class TestSiteNameDecoratedContextCredentialsProvider {
    
    private Credentials defaultCredentials;
    private Credentials defaultCredentialsForPreviewMode;
    private Credentials writableCredentials;
    private ContextCredentialsProvider ccp;
    private HstRequestContext requestContext;
    private ResolvedSiteMapItem resolvedSiteMapItem;
    private ResolvedSiteMount resolvedSiteMount;
    private SiteMount siteMount;
    private HstSite hstSite;
    
    @Before
    public void setUp() throws Exception {
        defaultCredentials = new SimpleCredentials("admin@default", "password@default".toCharArray());
        defaultCredentialsForPreviewMode = new SimpleCredentials("admin@preview", "password@preview".toCharArray());
        writableCredentials = new SimpleCredentials("admin@writable", "password@writable".toCharArray());
        ccp = new SiteNameDecoratedContextCredentialsProvider(defaultCredentials, defaultCredentialsForPreviewMode, writableCredentials);
        requestContext = createMock(HstRequestContext.class);
        resolvedSiteMapItem = createMock(ResolvedSiteMapItem.class);
        resolvedSiteMount = createMock(ResolvedSiteMount.class);
        siteMount = createMock(SiteMount.class);
        hstSite = createMock(HstSite.class);
    }
    
    @Test
    public void testDefaultCredentials() throws Exception {

        expect(requestContext.isPreview()).andReturn(Boolean.FALSE).anyTimes();
        expect(requestContext.getResolvedSiteMapItem()).andReturn(resolvedSiteMapItem);
        expect(resolvedSiteMapItem.getResolvedSiteMount()).andReturn(resolvedSiteMount);
        expect(resolvedSiteMount.getSiteMount()).andReturn(siteMount);
        expect(siteMount.getHstSite()).andReturn(hstSite);
        expect(hstSite.getName()).andReturn("site1");
        
        replay(requestContext);
        replay(resolvedSiteMapItem);
        replay(resolvedSiteMount);
        replay(siteMount);
        replay(hstSite);
        
        Credentials creds = ccp.getDefaultCredentials(requestContext);
        
        assertTrue(creds instanceof SimpleCredentials);
        assertEquals("admin@default@site1", ((SimpleCredentials) creds).getUserID());
        assertEquals("password@default", new String(((SimpleCredentials) creds).getPassword()));

        reset(requestContext);
        reset(resolvedSiteMapItem);
        reset(resolvedSiteMount);
        reset(siteMount);
        reset(hstSite);
    
        expect(requestContext.isPreview()).andReturn(Boolean.FALSE).anyTimes();
        expect(requestContext.getResolvedSiteMapItem()).andReturn(resolvedSiteMapItem);
        expect(resolvedSiteMapItem.getResolvedSiteMount()).andReturn(resolvedSiteMount);
        expect(resolvedSiteMount.getSiteMount()).andReturn(siteMount);
        expect(siteMount.getHstSite()).andReturn(hstSite);
        expect(hstSite.getName()).andReturn("site1");
        
        replay(requestContext);
        replay(resolvedSiteMapItem);
        replay(resolvedSiteMount);
        replay(siteMount);
        replay(hstSite);
        
        Credentials creds2 = ccp.getDefaultCredentials(requestContext);
        
        assertTrue(creds == creds2);
        
        reset(requestContext);
        reset(resolvedSiteMapItem);
        reset(resolvedSiteMount);
        reset(siteMount);
        reset(hstSite);
    
        expect(requestContext.isPreview()).andReturn(Boolean.FALSE).anyTimes();
        expect(requestContext.getResolvedSiteMapItem()).andReturn(resolvedSiteMapItem);
        expect(resolvedSiteMapItem.getResolvedSiteMount()).andReturn(resolvedSiteMount);
        expect(resolvedSiteMount.getSiteMount()).andReturn(siteMount);
        expect(siteMount.getHstSite()).andReturn(hstSite);
        expect(hstSite.getName()).andReturn("site2");
        
        replay(requestContext);
        replay(resolvedSiteMapItem);
        replay(resolvedSiteMount);
        replay(siteMount);
        replay(hstSite);
        
        Credentials creds3 = ccp.getDefaultCredentials(requestContext);
        assertTrue(creds3 instanceof SimpleCredentials);
        assertFalse(creds == creds3);
        assertFalse(creds.equals(creds3));
        assertEquals("admin@default@site2", ((SimpleCredentials) creds3).getUserID());
    }

    @Test
    public void testPreviewCredentials() throws Exception {
        
        expect(requestContext.isPreview()).andReturn(Boolean.TRUE).anyTimes();
        expect(requestContext.getResolvedSiteMapItem()).andReturn(resolvedSiteMapItem);
        expect(resolvedSiteMapItem.getResolvedSiteMount()).andReturn(resolvedSiteMount);
        expect(resolvedSiteMount.getSiteMount()).andReturn(siteMount);
        expect(siteMount.getHstSite()).andReturn(hstSite);
        expect(hstSite.getName()).andReturn("site1");
        
        replay(requestContext);
        replay(resolvedSiteMapItem);
        replay(resolvedSiteMount);
        replay(siteMount);
        replay(hstSite);
        
        Credentials creds = ccp.getDefaultCredentials(requestContext);
        
        assertTrue(creds instanceof SimpleCredentials);
        assertEquals("admin@preview@site1", ((SimpleCredentials) creds).getUserID());
        assertEquals("password@preview", new String(((SimpleCredentials) creds).getPassword()));
        
        reset(requestContext);
        reset(resolvedSiteMapItem);
        reset(resolvedSiteMount);
        reset(siteMount);
        reset(hstSite);
    
        expect(requestContext.isPreview()).andReturn(Boolean.TRUE).anyTimes();
        expect(requestContext.getResolvedSiteMapItem()).andReturn(resolvedSiteMapItem);
        expect(resolvedSiteMapItem.getResolvedSiteMount()).andReturn(resolvedSiteMount);
        expect(resolvedSiteMount.getSiteMount()).andReturn(siteMount);
        expect(siteMount.getHstSite()).andReturn(hstSite);
        expect(hstSite.getName()).andReturn("site1");
        
        replay(requestContext);
        replay(resolvedSiteMapItem);
        replay(resolvedSiteMount);
        replay(siteMount);
        replay(hstSite);

        Credentials creds2 = ccp.getDefaultCredentials(requestContext);
        
        assertTrue(creds == creds2);
        
        reset(requestContext);
        reset(resolvedSiteMapItem);
        reset(resolvedSiteMount);
        reset(siteMount);
        reset(hstSite);
    
        expect(requestContext.isPreview()).andReturn(Boolean.TRUE).anyTimes();
        expect(requestContext.getResolvedSiteMapItem()).andReturn(resolvedSiteMapItem);
        expect(resolvedSiteMapItem.getResolvedSiteMount()).andReturn(resolvedSiteMount);
        expect(resolvedSiteMount.getSiteMount()).andReturn(siteMount);
        expect(siteMount.getHstSite()).andReturn(hstSite);
        expect(hstSite.getName()).andReturn("site2");
        
        replay(requestContext);
        replay(resolvedSiteMapItem);
        replay(resolvedSiteMount);
        replay(siteMount);
        replay(hstSite);
      
        Credentials creds3 = ccp.getDefaultCredentials(requestContext);
        assertTrue(creds3 instanceof SimpleCredentials);
        assertFalse(creds == creds3);
        assertFalse(creds.equals(creds3));
        assertEquals("admin@preview@site2", ((SimpleCredentials) creds3).getUserID());
    }
    
    @Test
    public void testWritableCredentials() throws Exception {
       
        expect(requestContext.getResolvedSiteMapItem()).andReturn(resolvedSiteMapItem);
        expect(resolvedSiteMapItem.getResolvedSiteMount()).andReturn(resolvedSiteMount);
        expect(resolvedSiteMount.getSiteMount()).andReturn(siteMount);
        expect(siteMount.getHstSite()).andReturn(hstSite);
        expect(hstSite.getName()).andReturn("site1");
        
        replay(requestContext);
        replay(resolvedSiteMapItem);
        replay(resolvedSiteMount);
        replay(siteMount);
        replay(hstSite);
        
        Credentials creds = ccp.getWritableCredentials(requestContext);
        
        assertTrue(creds instanceof SimpleCredentials);
        assertEquals("admin@writable@site1", ((SimpleCredentials) creds).getUserID());
        assertEquals("password@writable", new String(((SimpleCredentials) creds).getPassword()));
        
        reset(requestContext);
        reset(resolvedSiteMapItem);
        reset(resolvedSiteMount);
        reset(siteMount);
        reset(hstSite);
      
        expect(requestContext.getResolvedSiteMapItem()).andReturn(resolvedSiteMapItem);
        expect(resolvedSiteMapItem.getResolvedSiteMount()).andReturn(resolvedSiteMount);
        expect(resolvedSiteMount.getSiteMount()).andReturn(siteMount);
        expect(siteMount.getHstSite()).andReturn(hstSite);
        expect(hstSite.getName()).andReturn("site1");
        
        replay(requestContext);
        replay(resolvedSiteMapItem);
        replay(resolvedSiteMount);
        replay(siteMount);
        replay(hstSite);
     
        Credentials creds2 = ccp.getWritableCredentials(requestContext);
        
        assertTrue(creds == creds2);
        
        reset(requestContext);
        reset(resolvedSiteMapItem);
        reset(resolvedSiteMount);
        reset(siteMount);
        reset(hstSite);
      
        expect(requestContext.getResolvedSiteMapItem()).andReturn(resolvedSiteMapItem);
        expect(resolvedSiteMapItem.getResolvedSiteMount()).andReturn(resolvedSiteMount);
        expect(resolvedSiteMount.getSiteMount()).andReturn(siteMount);
        expect(siteMount.getHstSite()).andReturn(hstSite);
        expect(hstSite.getName()).andReturn("site2");
        
        replay(requestContext);
        replay(resolvedSiteMapItem);
        replay(resolvedSiteMount);
        replay(siteMount);
        replay(hstSite);
     
        Credentials creds3 = ccp.getWritableCredentials(requestContext);
        assertTrue(creds3 instanceof SimpleCredentials);
        assertFalse(creds == creds3);
        assertFalse(creds.equals(creds3));
        assertEquals("admin@writable@site2", ((SimpleCredentials) creds3).getUserID());

    }

}
