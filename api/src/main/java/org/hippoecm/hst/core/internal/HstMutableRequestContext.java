/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.hst.core.internal;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.jcr.Session;
import javax.security.auth.Subject;
import javax.servlet.ServletContext;

import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.search.HstQueryManagerFactory;
import org.hippoecm.hst.core.sitemenu.HstSiteMenus;


/**
 * This is an INTERNAL USAGE ONLY API. Clients should not cast to these interfaces as they should never be used from client code
 * 
 * @version $Id$
 *
 */
public interface HstMutableRequestContext extends HstRequestContext {
    
	public void setServletContext(ServletContext servletContext);
	
	public void setContextNamespace(String contextNamespace);

	public void setSession(Session session);

	public void setResolvedMount(ResolvedMount resolvedMount);

	public void setResolvedSiteMapItem(ResolvedSiteMapItem resolvedSiteMapItem);
	
	public void setTargetComponentPath(String targetComponentPath);

	public void setBaseURL(HstContainerURL baseURL);

	public void setURLFactory(HstURLFactory urlFactory);

	public void setSiteMapMatcher(HstSiteMapMatcher siteMapMatcher);

	public void setLinkCreator(HstLinkCreator linkCreator);

	public void setHstSiteMenus(HstSiteMenus siteMenus);

	public void setHstQueryManagerFactory(HstQueryManagerFactory hstQueryManagerFactory);

	public void setContainerConfiguration(ContainerConfiguration containerConfiguration);
	
	public void setEmbeddingContextPath(String embeddingContextPath);

	public void setResolvedEmbeddingMount(ResolvedMount resolvedEmbeddingMount);
	
    public void setSubject(Subject subject);
    
    /**
     * Sets the preferred locale associated with this request.
     * @param locale The preferred locale associated with this request.
     */
    public void setPreferredLocale(Locale locale);
    
    /**
     * Sets the locales assocaited with this request.
     * @param locales
     */
    public void setLocales(List<Locale> locales);
    
    /**
     * Sets the path suffix
     * @param pathSuffix
     */
    public void setPathSuffix(String pathSuffix);


    /**
     * set the conditions that will trigger a component to be added to the component window hierarchy.
     */
    void setComponentFilterTags(Set<String> conditions);
    
    /**
     * @param fullyQualifiedURLs sets whether created URLs will be fully qualified
     */
    public void setFullyQualifiedURLs(boolean fullyQualifiedURLs);
    
    /**
     * Sets a specific render host. This can be used to render the request as if host <code>renderHost</code> was the actual 
     * used host in the request. 
     * @param renderHost the host to be used for rendering
     */
    public void setRenderHost(String renderHost);
}
