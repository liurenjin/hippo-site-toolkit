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
package org.hippoecm.hst.services.support.jaxrs.content;

import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.configuration.HstSitesManager;
import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.manager.ObjectBeanPersistenceManager;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.manager.ObjectConverterImpl;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManager;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManagerImpl;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.standard.HippoAsset;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDirectory;
import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.content.beans.standard.HippoFacetSelect;
import org.hippoecm.hst.content.beans.standard.HippoFixedDirectory;
import org.hippoecm.hst.content.beans.standard.HippoFolder;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.content.beans.standard.HippoHtml;
import org.hippoecm.hst.content.beans.standard.HippoImage;
import org.hippoecm.hst.content.beans.standard.HippoMirror;
import org.hippoecm.hst.content.beans.standard.HippoResource;
import org.hippoecm.hst.content.beans.standard.HippoStdPubWfRequest;
import org.hippoecm.hst.content.beans.standard.HippoTranslation;
import org.hippoecm.hst.content.beans.standard.facetnavigation.HippoFacetSearch;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstComponentFatalException;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.hosting.VirtualHost;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.MatchedMapping;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.search.HstQueryManagerFactory;
import org.hippoecm.hst.site.HstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BaseHstContentService
 * 
 * @version $Id$
 */
public class BaseHstContentService {
    
    public static final String SITE_CONTENT_PATH = "org.hippoecm.hst.services.support.site.content.path";
    
    private static final String IMPERSONATED_SESSION_ATTRIBUTE = BaseHstContentService.class.getName() + ".impersonatedSession";

    private static Logger log = LoggerFactory.getLogger(BaseHstContentService.class);

    private ObjectConverter objectConverter;
    private HstQueryManager hstQueryManager;
    
    private List<Class<? extends HippoBean>> annotatedClasses;
    
    private boolean impersonatingJcrSession;
    
    private String impersonatingJcrCredentialAttributeName; 
    
    public BaseHstContentService() {
    }
    
    protected HstRequestContext getHstRequestContext(HttpServletRequest servletRequest) {
        return (HstRequestContext) servletRequest.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
    }
    
    protected Session getJcrSession(HttpServletRequest servletRequest) throws LoginException, RepositoryException {
        HstRequestContext requestContext = getHstRequestContext(servletRequest);
        Session jcrSession = requestContext.getSession();
        
        if (impersonatingJcrSession && impersonatingJcrCredentialAttributeName != null) {
            Session impersonatedJcrSession = (Session) servletRequest.getAttribute(IMPERSONATED_SESSION_ATTRIBUTE);
            
            if (impersonatedJcrSession == null) {
                Credentials impersonatingCreds = (Credentials) servletRequest.getAttribute(impersonatingJcrCredentialAttributeName);
                
                if (impersonatingCreds == null) {
                    javax.servlet.http.HttpSession httpSession = servletRequest.getSession(false);
                    
                    if (httpSession != null) {
                        impersonatingCreds = (Credentials) httpSession.getAttribute(impersonatingJcrCredentialAttributeName);
                    }
                }
                
                if (impersonatingCreds != null) {
                    impersonatedJcrSession = jcrSession.impersonate(impersonatingCreds);
                    servletRequest.setAttribute(IMPERSONATED_SESSION_ATTRIBUTE, impersonatedJcrSession);
                }
            }
            
            if (impersonatedJcrSession != null) {
                return impersonatedJcrSession;
            }
        }
        
        return jcrSession;
    }
    
    protected ObjectBeanPersistenceManager getContentPersistenceManager(HttpServletRequest servletRequest) throws LoginException, RepositoryException {
        return new WorkflowPersistenceManagerImpl(getJcrSession(servletRequest), getObjectConverter());
    }
    
    protected WorkflowPersistenceManager createWorkflowPersistenceManager(HttpServletRequest servletRequest) throws LoginException, RepositoryException {
        return new WorkflowPersistenceManagerImpl(getJcrSession(servletRequest), getObjectConverter());
    }
    
    protected ObjectConverter getObjectConverter() {
        if (objectConverter == null) {
            Map<String, Class<? extends HippoBean>> jcrPrimaryNodeTypeClassPairs = new HashMap<String, Class<? extends HippoBean>>();
            List<Class<? extends HippoBean>> annoClasses = getAnnotatedClasses();
            
            for (Class<? extends HippoBean> c : annoClasses) {
                addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, c, false) ;
            }
            
            // below the default present mapped mappings
            addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoDocument.class, true);
            addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoFolder.class, true);
            addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoFacetSearch.class, true);
            addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoMirror.class, true);
            addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoFacetSelect.class, true);
            addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoDirectory.class, true);
            addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoFixedDirectory.class, true);
            addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoHtml.class, true);
            addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoResource.class, true);
            addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoStdPubWfRequest.class, true);
            addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoAsset.class, true);
            addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoImage.class, true);
            addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoTranslation.class, true);
            
            // builds a fallback jcrPrimaryNodeType array.
            String[] fallBackJcrNodeTypes = getFallBackJcrNodeTypes();
            objectConverter = new ObjectConverterImpl(jcrPrimaryNodeTypeClassPairs, fallBackJcrNodeTypes);
        }
        
        return objectConverter;
    }
    
    protected HstQueryManager getHstQueryManager() {
        if (hstQueryManager == null) {
            HstQueryManagerFactory hstQueryManagerFactory = (HstQueryManagerFactory) HstServices.getComponentManager().getComponent(HstQueryManagerFactory.class.getName());
            hstQueryManager = hstQueryManagerFactory.createQueryManager(getObjectConverter());
        }
        
        return hstQueryManager;
    }
    
    protected HippoBeanContent createHippoBeanContent(HippoBean bean, final Set<String> propertyNamesFilledWithValues) throws RepositoryException {
        HippoBeanContent beanContent = null;
        
        if (bean instanceof HippoFolderBean) {
            beanContent = new HippoFolderBeanContent((HippoFolderBean) bean, propertyNamesFilledWithValues);
        } else if (bean instanceof HippoDocumentBean) {
            beanContent = new HippoDocumentBeanContent((HippoDocumentBean) bean, propertyNamesFilledWithValues);
        } else {
            beanContent = new HippoBeanContent(bean, propertyNamesFilledWithValues);
        }
        
        return beanContent;
    }
    
    protected String getContentItemPath(final HttpServletRequest servletRequest, final List<PathSegment> pathSegments) {
        StringBuilder pathBuilder = new StringBuilder(80);
        
        if (pathSegments.size() > 0 && "jcr:root".equals(pathSegments.get(0).getPath())) {
            for (int i = 1; i < pathSegments.size(); i++) {
                pathBuilder.append('/').append(pathSegments.get(i).getPath());
            }
        } else {
            pathBuilder.append(getSiteContentPath(servletRequest));
            
            for (PathSegment pathSegment : pathSegments) {
                pathBuilder.append('/').append(pathSegment.getPath());
            }
        }
        
        String path = pathBuilder.toString();
        
        String encoding = servletRequest.getCharacterEncoding();
        
        if (encoding == null) {
            encoding = "ISO-8859-1";
        }
        
        try {
            path = URLDecoder.decode(path, encoding);
        } catch (Exception e) {
            log.warn("Failed to decode. {}", path);
        }
        
        return path;
    }
    
    protected String getSiteContentPath(HttpServletRequest servletRequest) {
        return (String) servletRequest.getAttribute(BaseHstContentService.SITE_CONTENT_PATH);
    }
    
    protected String getRelativeItemContentPath(HttpServletRequest servletRequest, final ItemContent itemContent) {
        String itemContentPath = itemContent.getPath();
        String siteContentPath = getSiteContentPath(servletRequest);
        
        if (itemContentPath != null && itemContentPath.startsWith(siteContentPath)) {
            itemContentPath = itemContentPath.substring(siteContentPath.length());
        }
        
        return itemContentPath;
    }
    
    protected String getRequestURIBase(final UriInfo uriInfo, final HttpServletRequest servletRequest) {
        String base = uriInfo.getBaseUri().toString();
        
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        
        if (base.startsWith("http://") || base.startsWith("https://")) {
            int offset = base.indexOf('/', 8);
            
            if (offset != -1) {
                String hostAddress = base.substring(0, offset);
                String path = base.substring(offset);
                String defaultContextServletPath = servletRequest.getContextPath() + servletRequest.getServletPath();
                
                if (path.startsWith(defaultContextServletPath)) {
                    path = getVirtualizedContextPath(servletRequest) + getVirtualizedServletPath(servletRequest) + path.substring(defaultContextServletPath.length());
                    base = hostAddress + path;
                }
            }
        }
        
        return base;
    }
    
    protected String getVirtualizedContextPath(HttpServletRequest servletRequest) {
        String virtualizedContextPath = servletRequest.getContextPath();
        HstRequestContext requestContext = getHstRequestContext(servletRequest);
        
        if (requestContext != null) {
            VirtualHost virtualHost = requestContext.getVirtualHost();
            
            if (virtualHost != null && !virtualHost.isContextPathInUrl()) {
                virtualizedContextPath = "";
            }
        }
        
        return virtualizedContextPath;
    }
    
    protected String getVirtualizedServletPath(HttpServletRequest servletRequest) {
        String virtualizedServletPath = servletRequest.getServletPath();
        HstRequestContext requestContext = getHstRequestContext(servletRequest);
        
        if (requestContext != null) {
            MatchedMapping matchedMapping = requestContext.getMatchedMapping();
            
            if (matchedMapping != null) {
                virtualizedServletPath = matchedMapping.getMapping().getUriPrefix();
                
                if (virtualizedServletPath == null) {
                    virtualizedServletPath = "";
                } else if (virtualizedServletPath.endsWith("/")) {
                    virtualizedServletPath = virtualizedServletPath.substring(0, virtualizedServletPath.length() - 1);
                }
            }
        }
        
        return virtualizedServletPath;
    }
    
    public List<Class<? extends HippoBean>> getAnnotatedClasses() {
        if (annotatedClasses == null) {
            return Collections.emptyList();
        }
        
        return annotatedClasses;
    }
    
    public void setAnnotatedClasses(List<Class<? extends HippoBean>> annotatedClasses) {
        this.annotatedClasses = annotatedClasses;
    }
    
    public boolean isImpersonatingJcrSession() {
        return impersonatingJcrSession;
    }
    
    public void setImpersonatingJcrSession(boolean impersonatingJcrSession) {
        this.impersonatingJcrSession = impersonatingJcrSession;
    }
    
    public String getImpersonatingJcrCredentialAttributeName() {
        return impersonatingJcrCredentialAttributeName;
    }
    
    public void setImpersonatingJcrCredentialAttributeName(String impersonatingJcrCredentialAttributeName) {
        this.impersonatingJcrCredentialAttributeName = impersonatingJcrCredentialAttributeName;
    }
    
    protected String[] getFallBackJcrNodeTypes(){
        return new String[] { "hippo:facetselect", "hippo:mirror", "hippostd:directory", "hippostd:folder", "hippo:resource", "hippo:request", "hippostd:html", "hippo:document" };
    }
    
    protected boolean isPreview(final HttpServletRequest servletRequest) {
        HstRequestContext requestContext = getHstRequestContext(servletRequest);
        return Boolean.TRUE == requestContext.getAttribute(ContainerConstants.IS_PREVIEW);
    }
    
    protected String getPageUriByCanonicalUuid(final HttpServletRequest servletRequest, final HttpServletResponse sevletResponse, final String canonicalUuid) {
        if (StringUtils.isBlank(canonicalUuid)) {
            return null;
        }
        
        HstRequestContext requestContext = getHstRequestContext(servletRequest);
        
        if (requestContext != null) {
            try {
                String sitesManagerComponentBaseName = HstSitesManager.class.getName();
                boolean previewMode = isPreview(servletRequest);
                HstSitesManager sitesManager = HstServices.getComponentManager().getComponent(sitesManagerComponentBaseName + (previewMode ? ".preview" : ".live"));
                HstSite hstSite = sitesManager.getSites().getSite(requestContext.getMatchedMapping().getSiteName());
                HstSiteMapMatcher siteMapMatcher = HstServices.getComponentManager().getComponent(HstSiteMapMatcher.class.getName());
                ResolvedSiteMapItem resolvedSiteMapItem = siteMapMatcher.match("/", hstSite);
                if (resolvedSiteMapItem == null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Cannot resolve sitemap item on /.");
                    }
                    return null;
                }
                HstLink hstLink = requestContext.getHstLinkCreator().create(canonicalUuid, getJcrSession(servletRequest), resolvedSiteMapItem);
                String previewServletPath = HstServices.getComponentManager().getContainerConfiguration().getString("preview.servlet.path");
                String liveServletPath = HstServices.getComponentManager().getContainerConfiguration().getString("live.servlet.path");
                HstContainerURL navURL = requestContext.getContainerURLProvider().parseURL(servletRequest, sevletResponse, requestContext, hstLink.getPath());
                navURL.setParameters(null);
                HstURL hstUrl = requestContext.getURLFactory().createURL(HstURL.RENDER_TYPE, null, navURL, requestContext);
                String pageUri = hstUrl.toString();
                String baseServletPath = StringUtils.removeStart(servletRequest.getServletPath(), liveServletPath);
                String baseUri = servletRequest.getContextPath() + baseServletPath;
                if (pageUri.startsWith(baseUri)) {
                    if (previewMode) {
                        pageUri = servletRequest.getContextPath() + previewServletPath + pageUri.substring(baseUri.length());
                    } else {
                        pageUri = servletRequest.getContextPath() + pageUri.substring(baseUri.length());
                    }
                } else if (pageUri.startsWith(baseServletPath)) {
                    if (previewMode) {
                        pageUri = previewServletPath + pageUri.substring(baseServletPath.length());
                    } else {
                        pageUri = pageUri.substring(baseServletPath.length());
                    }
                }
                return pageUri;
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.debug("Page link is not available. ", e);
                }
            }
        }
        
        return null;
    }
    
    private static void addJcrPrimaryNodeTypeClassPair(Map<String, Class<? extends HippoBean>> jcrPrimaryNodeTypeClassPairs,
            Class clazz, boolean builtinType) throws HstComponentException {
        String jcrPrimaryNodeType = null;

        if (clazz.isAnnotationPresent(Node.class)) {
            Node anno = (Node) clazz.getAnnotation(Node.class);
            jcrPrimaryNodeType = anno.jcrType();
        }

        if (jcrPrimaryNodeTypeClassPairs.get(jcrPrimaryNodeType) != null) {
            if (!builtinType) {
                throw new HstComponentFatalException("Annotated class for primarytype '"+jcrPrimaryNodeType+"' is duplicate. " +
                        "You might have configured a bean in 'beans-annotated-classes.xml' that does not have a annotation for the jcrType and" +
                        "inherits the jcrType from the bean it extends, resulting in 2 beans with the same jcrType. Correct your beans.");
            }
            return;
        }
        
        if (jcrPrimaryNodeType == null) {
            throw new IllegalArgumentException("There's no annotation for jcrType in the class: " + clazz);
        }

        jcrPrimaryNodeTypeClassPairs.put(jcrPrimaryNodeType,clazz);
    }
    
}
