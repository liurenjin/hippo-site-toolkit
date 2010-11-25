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
package org.hippoecm.hst.mock;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.container.HstContainerURLProvider;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.ContextCredentialsProvider;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.request.ResolvedSiteMount;
import org.hippoecm.hst.core.search.HstQueryManagerFactory;
import org.hippoecm.hst.core.sitemenu.HstSiteMenus;

public class MockHstRequestContext implements HstRequestContext {
    
    protected Hashtable<String, Object> attributes = new Hashtable<String, Object>();
    protected Session session;
    protected HstContainerURL baseURL;
    protected String contextNamespace;
    protected HstURLFactory urlFactory;
    protected ResolvedSiteMount resolvedSiteMount;
    protected ResolvedSiteMapItem resolvedSiteMapItem;
    protected HstLinkCreator linkCreator;
    protected HstSiteMapMatcher siteMapMatcher;
    protected HstSiteMenus siteMenus;
    protected HstQueryManagerFactory hstQueryManagerFactory;
    protected Credentials defaultCredentials;
    protected ContainerConfiguration containerConfiguration;
    protected ContextCredentialsProvider contextCredentialsProvider;

    public boolean isPreview() {
    	return this.resolvedSiteMount.getSiteMount().isPreview();
    }
    
    public Object getAttribute(String name) {
        return this.attributes.get(name);
    }
    
    public Enumeration<String> getAttributeNames() {
        return this.attributes.keys();
    }
    
    public HstContainerURL getBaseURL() {
        return this.baseURL;
    }
    
    public String getContextNamespace() {
        return this.contextNamespace;
    }
    
    public void setDefaultCredentials(Credentials defaultCredentials) {
        this.defaultCredentials = defaultCredentials;
    }
    
    public HstSiteMapMatcher getSiteMapMatcher(){
        return this.siteMapMatcher;
    }
    
    public void setSiteMapMatcher(HstSiteMapMatcher siteMapMatcher){
        this.siteMapMatcher = siteMapMatcher;
    }
    
    public HstLinkCreator getHstLinkCreator() {
        return this.linkCreator;
    }
    
    public void setHstLinkCreator(HstLinkCreator linkCreator) {
        this.linkCreator = linkCreator;
    }
    
    public HstQueryManagerFactory getHstQueryManagerFactory() {
        return hstQueryManagerFactory;
    }

    public void setHstQueryManagerFactory(HstQueryManagerFactory hstQueryManagerFactory) {
        this.hstQueryManagerFactory = hstQueryManagerFactory;
    }
    
    public void setHstSiteMenus(HstSiteMenus siteMenus) {
        this.siteMenus = siteMenus;
    }
    
    public HstSiteMenus getHstSiteMenus(){
        return this.siteMenus;
    }
    
    public ResolvedSiteMount getResolvedSiteMount() {
        return this.resolvedSiteMount;
    }
    
    public void setResolvedSiteMount(ResolvedSiteMount resolvedSiteMount) {
        this.resolvedSiteMount = resolvedSiteMount;
    }
    
    public ResolvedSiteMapItem getResolvedSiteMapItem() {
        return this.resolvedSiteMapItem;
    }
    
    public void setResolvedSiteMapItem(ResolvedSiteMapItem resolvedSiteMapItem) {
        this.resolvedSiteMapItem = resolvedSiteMapItem;
    }
    
    public void setSession(Session session) {
        this.session = session;
    }
    
    public Session getSession() throws LoginException, RepositoryException {
        return this.session;
    }
    
    public HstURLFactory getURLFactory() {
        return this.urlFactory;
    }
    
    public void setURLFactory(HstURLFactory urlFactory) {
        this.urlFactory = urlFactory;
    }
    
    public HstContainerURLProvider getContainerURLProvider() {
        return urlFactory != null ? urlFactory.getContainerURLProvider() : null;
    }

    public void removeAttribute(String name) {
        this.attributes.remove(name);
    }
    
    public void setAttribute(String name, Object value) {
        if (value == null) {
            removeAttribute(name);
        } else {
            this.attributes.put(name, value);
        }
    }

    public ContainerConfiguration getContainerConfiguration() {
        return this.containerConfiguration;
    }
    
    public void setContainerConfiguration(ContainerConfiguration containerConfiguration) {
        this.containerConfiguration = containerConfiguration;
    }


    public VirtualHost getVirtualHost() {
     
        return null;
    }

    public boolean isEmbeddedRequest() {
        return false;
    }

    public boolean isPortletContext() {
        return false;
    }
    
    public ContextCredentialsProvider getContextCredentialsProvider() {
        return contextCredentialsProvider;
    }
    
    public void setContextCredentialsProvider(ContextCredentialsProvider contextCredentialsProvider) {
        this.contextCredentialsProvider = contextCredentialsProvider;
    }

    public String getEmbeddingContextPath() {
    	return null;
    }
    
	public ResolvedSiteMount getResolvedEmbeddingSiteMount() {
		return null;
	}

	public String getTargetComponentPath() {
		return null;
	}
}