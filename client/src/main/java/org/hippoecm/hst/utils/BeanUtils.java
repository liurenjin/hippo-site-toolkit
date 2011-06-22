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
package org.hippoecm.hst.utils;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManagerImpl;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.exceptions.FilterException;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.content.beans.standard.HippoFacetNavigationBean;
import org.hippoecm.hst.content.beans.standard.HippoResultSetBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.search.HstQueryManagerFactory;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.BaseCommandController;

/**
 * Class containing utility methods for Beans
 * 
 */
public class BeanUtils {

    private final static Logger log = LoggerFactory.getLogger(BeanUtils.class);

    /**
     * Returns a HstQuery for incoming beans (incoming beans within scope {@code scope}). You can add filters and ordering to the query before executing it 
     * 
     * You need to add a <code>linkPath</code>: this is that path, that the incoming beans use to link to the HippoDocumentBean {@code bean}. For example, with '/myproject:link/@hippo:docbase' or even 'wildcard/@hippo:docbase' or 
     * 'wildcard/wildcard/@hippo:docbase' where wildcard = *
     * 
     * @see Same as {@link #createIncomingBeansQuery(HippoDocumentBean, HippoBean, String, ObjectConverter, Class, boolean)}
     * @deprecated use {@link #createIncomingBeansQuery(HippoDocumentBean, HippoBean, String, ObjectConverter, Class, boolean)} instead. 
     * The objectConverter can be fetched from {@link BaseHstComponent#getObjectConverter()}
     */
    @Deprecated
    public static HstQuery createIncomingBeansQuery(HippoDocumentBean bean, HippoBean scope, 
            String linkPath, BaseHstComponent component,
            Class<? extends HippoBean> beanMappingClass, boolean includeSubTypes) throws QueryException{

        return createIncomingBeansQuery(bean, scope, linkPath, component.getObjectConverter(), beanMappingClass, includeSubTypes);
    }
    
    /**
     * Returns a HstQuery for incoming beans (incoming beans within scope {@code scope}). You can add filters and ordering to the query before executing it 
     * 
     * You need to add a <code>linkPath</code>: this is that path, that the incoming beans use to link to the HippoDocumentBean {@code bean}. For example, with '/myproject:link/@hippo:docbase' or even 'wildcard/@hippo:docbase' or 
     * 'wildcard/wildcard/@hippo:docbase' where wildcard = *
     * 
     */

    public static HstQuery createIncomingBeansQuery(HippoDocumentBean bean, HippoBean scope, 
            String linkPath, ObjectConverter converter,
            Class<? extends HippoBean> beanMappingClass, boolean includeSubTypes) throws QueryException{

        List<String> linkPaths = new ArrayList<String>();
        linkPaths.add(linkPath);
        return createIncomingBeansQuery(bean, scope, linkPaths, converter, beanMappingClass, includeSubTypes);
    
    }

    /**
     * Returns a HstQuery for incoming beans (incoming beans within scope {@code scope}). You can add filters and ordering to the query before executing it 
     * 
     * The depth indicates how many child nodes deep is searched for a link to the HippoDocumentBean {@code bean}. The depth is allowed to range from 0 (direct hippo:docbase) to 4 (at most 4 levels deep searching is done)
     * 
     * 
     * @see Same as {@link #createIncomingBeansQuery(HippoDocumentBean, HippoBean, int, ObjectConverter, Class, boolean)}
     * @deprecated use {@link #createIncomingBeansQuery(HippoDocumentBean, HippoBean, int, ObjectConverter, Class, boolean)} instead. 
     * The objectConverter can be fetched from {@link BaseHstComponent#getObjectConverter()}
     */
    @Deprecated
    public static HstQuery createIncomingBeansQuery(HippoDocumentBean bean, HippoBean scope, int depth,
            BaseHstComponent component, Class<? extends HippoBean> beanMappingClass,
            boolean includeSubTypes) throws QueryException{
       
        return createIncomingBeansQuery(bean, scope, depth, component.getObjectConverter(), beanMappingClass, includeSubTypes);
    }
    
    /**
     * Returns a HstQuery for incoming beans (incoming beans within scope {@code scope}). You can add filters and ordering to the query before executing it 
     * 
     * The depth indicates how many child nodes deep is searched for a link to the HippoDocumentBean {@code bean}. The depth is allowed to range from 0 (direct hippo:docbase) to 4 (at most 4 levels deep searching is done)
     */

    public static HstQuery createIncomingBeansQuery(HippoDocumentBean bean, HippoBean scope, int depth,
            ObjectConverter converter, Class<? extends HippoBean> beanMappingClass,
            boolean includeSubTypes) throws QueryException{
        if (depth < 0 || depth > 4) {
            throw new FilterException("Depth must be (including) between 0 and 4");
        }
        String path = "@hippo:docbase";
        List<String> linkPaths = new ArrayList<String>();
        linkPaths.add(path);
        for (int i = 1; i <= depth; i++) {
            path = "*/" + path;
            linkPaths.add(path);
        }
        return createIncomingBeansQuery(bean, scope, linkPaths, converter, beanMappingClass, includeSubTypes);
    }

    /**
     * Returns a HstQuery for incoming beans. You can add filters and ordering to the query before executing it 
     * 
     * List<String> linkPaths is the list of paths that are searched that might have a link to the HippoDocumentBean {@code bean}. For example {/myproject:link/@hippo:docbase, /myproject:body/hippostd:content/@hippo:docbase}
     * 
     * @see Same as {@link #createIncomingBeansQuery(HippoDocumentBean, HippoBean, List<String>, ObjectConverter, Class, boolean)}
     * @deprecated use {@link #createIncomingBeansQuery(HippoDocumentBean, HippoBean, List<String>, ObjectConverter, Class, boolean)} instead. 
     * The objectConverter can be fetched from {@link BaseHstComponent#getObjectConverter()}
     */
    @Deprecated
    public static HstQuery createIncomingBeansQuery(HippoDocumentBean bean, HippoBean scope,
            List<String> linkPaths, BaseHstComponent component,
            Class<? extends HippoBean> beanMappingClass, boolean includeSubTypes) throws QueryException{

        return createIncomingBeansQuery(bean, scope, linkPaths, component.getObjectConverter(), beanMappingClass, includeSubTypes);
        
    }
    
    /**
     * Returns a HstQuery for incoming beans. You can add filters and ordering to the query before executing it 
     * 
     * List<String> linkPaths is the list of paths that are searched that might have a link to the HippoDocumentBean {@code bean}. For example {/myproject:link/@hippo:docbase, /myproject:body/hippostd:content/@hippo:docbase}
     */
    public static HstQuery createIncomingBeansQuery(HippoDocumentBean bean, HippoBean scope,
            List<String> linkPaths, ObjectConverter converter,
            Class<? extends HippoBean> beanMappingClass, boolean includeSubTypes) throws QueryException{

        String canonicalHandleUUID = bean.getCanonicalHandleUUID();
        HstQuery query;
        try {
            ComponentManager compMngr = HstServices.getComponentManager();
            HstQueryManagerFactory hstQueryManagerFactory = (HstQueryManagerFactory)compMngr.getComponent(HstQueryManagerFactory.class.getName());
            query = hstQueryManagerFactory.createQueryManager(bean.getNode().getSession(), converter).createQuery(scope, beanMappingClass, includeSubTypes);
            Filter filter = query.createFilter();
            for (String linkPath : linkPaths) {
                Filter orFilter = query.createFilter();
                orFilter.addEqualTo(linkPath, canonicalHandleUUID);
                filter.addOrFilter(orFilter);
            }
            query.setFilter(filter);
            return query;
        } catch (RepositoryException e) {
            throw new QueryException("RepositoryException",e);
        }
        
    }

    
    /**
     * Returns a list of beans of type T (the same type as {@code beanMappingClass}) that have a (facet)link to the HippoDocumentBean {@code bean}. If no incoming beans are found, 
     * an <code>empty</code> list will be returned. 
     * 
     * List<String> linkPaths is the list of paths that are searched that might have a link to the HippoDocumentBean {@code bean}. For example {/myproject:link/@hippo:docbase, /myproject:body/hippostd:content/@hippo:docbase}
     */
    public static <T extends HippoBean> List<T> getIncomingBeans(HstQuery query,
            Class<? extends HippoBean> beanMappingClass) throws QueryException {

        List<T> incomingBeans = new ArrayList<T>();

        HstQueryResult result = query.execute();
        HippoBeanIterator beans = result.getHippoBeans();
        while (beans.hasNext()) {
            T incomingBean = (T) beans.nextHippoBean();
            if(incomingBean == null) {
                continue;
            }
            if (!beanMappingClass.isAssignableFrom(incomingBean.getClass())) {
                // should not be possible
                log.warn("Found a bean not being of type or subtype of '{}'. Skip bean", beanMappingClass.getName());
                continue;
            }
            incomingBeans.add(incomingBean);
        }

        return incomingBeans;
    }
    
    /**
     * <p>
     * Returns the {@link HippoFacetNavigationBean} for this {@link HstRequest} and <code>relPath</code> where it is accounted for the free text <code>query</code>. When <code>query</code> is <code>null</code> or
     * empty, we return the HippoFacetNavigationBean without free text search. Else, we try to return the HippoFacetNavigationBean with free text search. If the 
     * HippoFacetNavigationBean does not exist in the faceted navigation tree in combination with the free text search, we return <code>null</code>. 
     * </p>
     * <p>
     * <b>Note</b> we can only return the HippoFacetNavigationBean if the current {@link ResolvedSiteMapItem} has a {@link ResolvedSiteMapItem#getRelativeContentPath()} that
     * points to points to a {@link HippoFacetNavigationBean} or to some lower descendant {@link HippoBean}, for example to a {@link HippoResultSetBean}. In this latter case,
     * we traverse up until we find a  {@link HippoFacetNavigationBean}, and return that one.
     * </p>
     * <p>
     * The <code>relPath</code> is relative to the site content base path and <b>must not</b> start with a /
     * </p>
     * 
     * If some exception happens, like we cannot get a disposable pooled session, we throw a {@link HstComponentException}
     * 
     * @param hstRequest the hstRequest
     * @param relPath the relative path to the faceted navigation node, which must not start with a / and is relative to the site content base path
     * @param query the free text query that should be accounted for for this <code>facNavBean</code> 
     * @param objectConverter the objectConverter to be used
     * @return the <code>HippoFacetNavigationBean</code> accounted for this <code>query</code> and <code>null</code> if we could not find the HippoFacetNavigationBean when the <code>query</code> is applied
     * @throws HstComponentException
     */
    public static HippoFacetNavigationBean getFacetNavigationBean(HstRequest hstRequest, String relPath, String query, ObjectConverter objectConverter) throws HstComponentException {        
        String base = PathUtils.normalizePath(hstRequest.getRequestContext().getResolvedMount().getMount().getContentPath());
        
        if(relPath == null) {
            log.warn("Cannot return a content bean for relative path null for resolvedSitemapItem belonging to '{}'. Return null", hstRequest.getRequestContext().getResolvedSiteMapItem().getHstSiteMapItem().getId());
            return null;
        }
        
        String absPath = "/"+base;
        if(!"".equals(relPath)) {
            absPath += "/" + relPath;
        }
        
        if(query == null || "".equals(query)) {
           try {
                ObjectBeanManager objectBeanMngr = new ObjectBeanManagerImpl(hstRequest.getRequestContext().getSession(), objectConverter);
                HippoBean bean  = (HippoBean)objectBeanMngr.getObject(absPath);
                if(bean == null) {
                    log.info("Cannot return HippoFacetNavigationBean for path '{}'. Return null", absPath);
                    return null;
                }
                
                while(bean != null && !(bean instanceof HippoFacetNavigationBean)) {
                    log.debug("Bean for '{}' is not instance of 'HippoFacetNavigationBean'. Let's check it's parent. ", bean.getPath());
                    if(bean.getPath().equals("/"+base)) {
                        // we are at the sitebase and did not find a HippoFacetNavigationBean. return null
                        log.info("We did not find a 'HippoFacetNavigationBean' somewhere in the path below '{}'. Return null", absPath);
                        return null;
                    }
                    bean = bean.getParentBean();
                }
                return (HippoFacetNavigationBean)bean;
                
            } catch (ObjectBeanManagerException e) {
                throw new HstComponentException("Could not get the HippoFacetNavigationBean for '"+absPath+"'", e);
            } catch (RepositoryException e) {
                throw new HstComponentException("Could not get the HippoFacetNavigationBean for '"+absPath+"'", e);
            }
        }
        
        // we have free text search. Now, we have to fetch from the root every descendant one-by-one until we hit a FacetedNavigationNode. 
        
        // first, let's get a disposable session:
        Session disposablePoolSession = getDisposablePoolSession(hstRequest, query);
        ObjectBeanManager objectBeanMngr = new ObjectBeanManagerImpl(disposablePoolSession, objectConverter);
        
        HippoFacetNavigationBean facetNavBean = null;
        
        // first, with the original session which is not tied to THIS free text faceted navigation, we need to get the 
        // faceted navigation node. We CANNOT do this with the disposablePoolSession because then we TIE the faceted navigation
        // without free text search already to the disposablePoolSession
        try {
            Node siteBaseNode = (Node)hstRequest.getRequestContext().getSession().getItem("/"+base);
            Node stepInto = siteBaseNode;
            String[] pathElements = relPath.split("/");
            for(int i = 0; i < pathElements.length ; i++) {
                if(facetNavBean == null) {
                    stepInto = stepInto.getNode(pathElements[i]);
                    if(stepInto.isNodeType("hippofacnav:facetnavigation")) {
                        // we found the faceted navigation node! Now, append the free text search
                        // note we get the faceted navigation now with the object bean mngr backed by disposablePoolSession
                        facetNavBean = (HippoFacetNavigationBean)objectBeanMngr.getObject(stepInto.getPath() + "[{"+query+"}]");
                    }
                } else {
                    // if the child path element still returns a faceted navigation element we continue, otherwise we break.
                    String nextPath = facetNavBean.getPath() + "/" + pathElements[i];
                    // note we get the faceted navigation now with the object bean mngr backed by disposablePoolSession
                    Object o = objectBeanMngr.getObject(nextPath);
                    if(o instanceof HippoFacetNavigationBean) {
                        facetNavBean = (HippoFacetNavigationBean)o;
                    } 
                    if(o instanceof HippoResultSetBean) {
                        // we can stop, we are in the resultset
                        break;
                    }
                    if(o == null) {
                        // the path did not resolve to a bean. Thus, the path is incorrect. Return null.
                        facetNavBean = null;
                        break;
                    }
                }
            }
            if(facetNavBean == null) {
                log.info("We did not find a HippoFacetNavigationBean for path '{}' and query '{}'. Return null.",absPath, query);
            }
        } catch (PathNotFoundException e) {
            throw new HstComponentException("Could not get the HippoFacetNavigationBean for '"+absPath+"' and query '"+query+"'", e);
        } catch (RepositoryException e) {
            throw new HstComponentException("Could not get the HippoFacetNavigationBean for '"+absPath+"' and query '"+query+"'", e);
        } catch (ObjectBeanManagerException e) {
            throw new HstComponentException("Could not get the HippoFacetNavigationBean for '"+absPath+"' and query '"+query+"'", e);
        }
        return facetNavBean;
    }
    
    /**
     * <p>
     * Returns the {@link HippoFacetNavigationBean} for this {@link HstRequest} from the {@link ResolvedSiteMapItem} where it is accounted for the free text <code>query</code>. When <code>query</code> is <code>null</code> or
     * empty, we return the HippoFacetNavigationBean without free text search. Else, we try to return the HippoFacetNavigationBean with free text search. If the 
     * HippoFacetNavigationBean does not exist in the faceted navigation tree in combination with the free text search, we return <code>null</code>. 
     * </p>
     * <p>
     * <b>Note</b> we can only return the HippoFacetNavigationBean if the current {@link ResolvedSiteMapItem} has a {@link ResolvedSiteMapItem#getRelativeContentPath()} that
     * points to points to a {@link HippoFacetNavigationBean} or to some lower descendant {@link HippoBean}, for example to a {@link HippoResultSetBean}. In this latter case,
     * we traverse up until we find a  {@link HippoFacetNavigationBean}, and return that one.
     * </p>
     * <p>
     * <b>If</b> you need a {@link HippoFacetNavigationBean} that is not on for the  {@link ResolvedSiteMapItem}, but at some fixed location,
     * you can use {@link #getFacetNavigationBean(HstRequest, String, String, ObjectConverter)}
     * </p>
     * 
     * If some exception happens, like we cannot get a disposable pooled session, we throw a {@link HstComponentException}
     * 
     * @param hstRequest the hstRequest
     * @param query the free text query that should be accounted for for this <code>facNavBean</code> 
     * @param objectConverter the objectConverter to be used
     * @return the <code>HippoFacetNavigationBean</code> accounted for this <code>query</code> and <code>null</code> if we could not find the HippoFacetNavigationBean when the <code>query</code> is applied
     * @throws HstComponentException
     */
    public static HippoFacetNavigationBean getFacetNavigationBean(HstRequest hstRequest, String query, ObjectConverter objectConverter) throws HstComponentException {
        ResolvedSiteMapItem resolvedSiteMapItem = hstRequest.getRequestContext().getResolvedSiteMapItem();
        String relPath = PathUtils.normalizePath(resolvedSiteMapItem.getRelativeContentPath());
        return getFacetNavigationBean(hstRequest, relPath, query, objectConverter);
    }

    /**
     * Same as  {@link #getFacetNavigationBean(HstRequest, String, ObjectConverter)} only now instead of a {@link String} query we 
     * pass in a {@link HstQuery}
     * @see {@link #getFacetNavigationBean(HstRequest, String, ObjectConverter)}
     * @param hstRequest the hstRequest
     * @param query a {@link HstQuery} object. If <code>null</code> the call returns as if there is no query
     * @param objectConverter the objectConverter to be used
     * @return the <code>HippoFacetNavigationBean</code> accounted for this <code>query</code> and <code>null</code> if we could not find the HippoFacetNavigationBean when the <code>query</code> is applied
     * @throws HstComponentException
     */
    public static HippoFacetNavigationBean getFacetNavigationBean(HstRequest hstRequest, HstQuery query, ObjectConverter objectConverter) throws HstComponentException {
        String queryAsString = null;
        if(query == null) {
            return getFacetNavigationBean(hstRequest, queryAsString, objectConverter);
        }
        try {
            queryAsString = "xpath("+query.getQueryAsString(true)+")";
        } catch (QueryException e) {
            throw new HstComponentException("Unable to create a string representation of query", e);
        }
        return getFacetNavigationBean(hstRequest, queryAsString, objectConverter);
    }
    
    /**
     * Same as  {@link #getFacetNavigationBean(HstRequest, HstQuery, ObjectConverter)} only now instead of having the faceted navigation
     * node from the {@link ResolvedSiteMapItem} we add a <code>relPath</code> where it should be found
     * @see {@link #getFacetNavigationBean(HstRequest, String, ObjectConverter)}
     * @param hstRequest the hstRequest
     * @param query a {@link HstQuery} object
     * @param relPath the relative path from site base content to the faceted navigation node, which must not start with a / and is relative to the site content base path  
     * @param objectConverter the objectConverter to be used
     * @return the <code>HippoFacetNavigationBean</code> accounted for this <code>query</code> and <code>relPath</code> and <code>null</code> if we could not find the HippoFacetNavigationBean when the <code>query</code> is applied
     * @throws HstComponentException
     */
    public static HippoFacetNavigationBean getFacetNavigationBean(HstRequest hstRequest, HstQuery query, String relPath, ObjectConverter objectConverter) throws HstComponentException {
        String queryAsString = null;
        try {
            queryAsString = "xpath("+query.getQueryAsString(true)+")";
        } catch (QueryException e) {
            throw new HstComponentException("Unable to create a string representation of query", e);
        }
        return getFacetNavigationBean(hstRequest, relPath, queryAsString, objectConverter);
    }
        
    
    /**
     * Tries to return a bean that is located in a faceted navigation tree below a result set. When it cannot be found,
     * or the bean is not of type <code>beanMappingClass</code>, <code>null</code> will be returned.
     * 
     * @param <T>
     * @param hstRequest the hstRequest
     * @param query the free text search as String that is used for this faceted navigation
     * @param objectConverter
     * @param beanMappingClass the class T must be of 
     * @return The faceted navigation result document of type T and <code>null</code> if it cannot be found or is not of type <code>T</code>
     */
    public static <T extends  HippoBean> T getFacetedNavigationResultDocument(HstRequest hstRequest, String query,
            ObjectConverter objectConverter, Class<T> beanMappingClass)  {
        
        ResolvedSiteMapItem resolvedSiteMapItem = hstRequest.getRequestContext().getResolvedSiteMapItem();
        String relPath = PathUtils.normalizePath(resolvedSiteMapItem.getRelativeContentPath());
        
        return getFacetedNavigationResultDocument(hstRequest, query, relPath, objectConverter, beanMappingClass);
    }

    /**
     * Tries to return a bean that is located in a faceted navigation tree below a result set. When it cannot be found,
     * or the bean is not of type <code>beanMappingClass</code>, <code>null</code> will be returned.
     * 
     * @param <T>
     * @param hstRequest the hstRequest
     * @param query the free text search as String that is used for this faceted navigation
     * @param relPath the relative path from site base content to the faceted navigation node, which must not start with a / and is relative to the site content base path
     * @param objectConverter
     * @param beanMappingClass the class T must be of 
     * @return The faceted navigation result document of type T and <code>null</code> if it cannot be found or is not of type <code>T</code>
     */
    public static <T extends  HippoBean> T getFacetedNavigationResultDocument(HstRequest hstRequest, String query, String relPath,
            ObjectConverter objectConverter, Class<T> beanMappingClass)  {
        
        String base = PathUtils.normalizePath(hstRequest.getRequestContext().getResolvedMount().getMount().getContentPath());
        
        if(relPath == null) {
            log.warn("Cannot return a content bean for relative path null for resolvedSitemapItem belonging to '{}'. Return null", hstRequest.getRequestContext().getResolvedSiteMapItem().getHstSiteMapItem().getId());
            return null;
        }
        
        String absPath = "/"+base;
        if(!"".equals(relPath)) {
            absPath += "/" + relPath;
        }
        
        if(query == null || "".equals(query)) {
           try {
                ObjectBeanManager objectBeanMngr = new ObjectBeanManagerImpl(hstRequest.getRequestContext().getSession(), objectConverter);
                HippoBean bean  = (HippoBean)objectBeanMngr.getObject(absPath);
                if(bean == null) {
                    log.info("Cannot return Document below faceted navigation for path '{}'. Return null", absPath);
                    return null;
                }
                if(!beanMappingClass.isAssignableFrom(bean.getClass())) {
                    log.debug("Expected bean of type '{}' but found of type '{}'. Return null.", beanMappingClass.getName(), bean.getClass().getName());
                    return null;
                }
                return (T)bean;   
            } catch (ObjectBeanManagerException e) {
                throw new HstComponentException("Could not get the HippoFacetNavigationBean for '"+absPath+"'", e);
            } catch (RepositoryException e) {
                throw new HstComponentException("Could not get the HippoFacetNavigationBean for '"+absPath+"'", e);
            }
        }
        
        // we have free text search. Now, we have to fetch from the root every descendant one-by-one until we hit a FacetedNavigationNode. 
        
        // first, let's get a disposable session:
        Session disposablePoolSession = getDisposablePoolSession(hstRequest, query);
        ObjectBeanManager objectBeanMngr = new ObjectBeanManagerImpl(disposablePoolSession, objectConverter);
        
        HippoFacetNavigationBean facetNavBean = null;
        
        // first, with the original session which is not tied to THIS free text faceted navigation, we need to get the 
        // faceted navigation node. We CANNOT do this with the disposablePoolSession because then we TIE the faceted navigation
        // without free text search already to the disposablePoolSession
        try {
            Node siteBaseNode = (Node)hstRequest.getRequestContext().getSession().getItem("/"+base);
            Node stepInto = siteBaseNode;
            String[] pathElements = relPath.split("/");
            
            
            // find the faceted navigation node with free text search first:
            
            String remainderPath = null;
            
            for(int i = 0; i < pathElements.length ; i++) {
                if(facetNavBean == null) {
                    stepInto = stepInto.getNode(pathElements[i]);
                    if(stepInto.isNodeType("hippofacnav:facetnavigation")) {
                        // we found the faceted navigation node! Now, append the free text search
                        // note we get the faceted navigation now with the object bean mngr backed by disposablePoolSession
                        facetNavBean = (HippoFacetNavigationBean)objectBeanMngr.getObject(stepInto.getPath() + "[{"+query+"}]");
                    }
                } else {
                    if(remainderPath == null) {
                        remainderPath = pathElements[i];
                    } else {
                        remainderPath += "/"+pathElements[i];
                    }
                }
            }
            if(facetNavBean == null) {
                log.info("We did not find a Document in the faceted navigation for path '{}' and query '{}'. Return null.",absPath, query);
                return null;
            } else {
                // now we have the faceted navigation bean with search. Let's try to fetch the remainder path
                T bean = facetNavBean.getBean(remainderPath, beanMappingClass);
                if(bean == null) {
                    log.info("We did not find a Document in the faceted navigation for path '{}' and query '{}'. Return null.",absPath, query);
                }
                return bean;
            }
        } catch (PathNotFoundException e) {
            throw new HstComponentException("Could not get the HippoFacetNavigationBean for '"+absPath+"' and query '"+query+"'", e);
        } catch (RepositoryException e) {
            throw new HstComponentException("Could not get the HippoFacetNavigationBean for '"+absPath+"' and query '"+query+"'", e);
        } catch (ObjectBeanManagerException e) {
            throw new HstComponentException("Could not get the HippoFacetNavigationBean for '"+absPath+"' and query '"+query+"'", e);
        }
    }
        
    
    /**
     * Tries to return a bean that is located in a faceted navigation tree below a result set. When it cannot be found,
     * or the bean is not of type <code>beanMappingClass</code>, <code>null</code> will be returned.
     * 
     * @param <T>
     * @param hstRequest the hstRequest
     * @param query the free text search as {@link HstQuery} that is used for this faceted navigation
     * @param objectConverter
     * @param beanMappingClass the class T must be of 
     * @return The faceted navigation result document of type T and <code>null</code> if it cannot be found or is not of type <code>T</code>
     */
    public static <T extends  HippoBean> T getFacetedNavigationResultDocument(HstRequest hstRequest, HstQuery query,
            ObjectConverter objectConverter, Class<T> beanMappingClass)  {
        
        if(query == null) {
            return getFacetedNavigationResultDocument(hstRequest, (String)null, objectConverter, beanMappingClass);
        }
        
        String queryAsString = null;
        try {
            queryAsString = "xpath("+query.getQueryAsString(true)+")";
        } catch (QueryException e) {
            throw new HstComponentException("Unable to create a string representation of query", e);
        }
        return getFacetedNavigationResultDocument(hstRequest, queryAsString, objectConverter, beanMappingClass);
    }
    
    /**
     * Tries to return a bean that is located in a faceted navigation tree below a result set. When it cannot be found,
     * or the bean is not of type <code>beanMappingClass</code>, <code>null</code> will be returned.
     * 
     * @param <T>
     * @param hstRequest the hstRequest
     * @param query the free text search as {@link HstQuery} that is used for this faceted navigation
     * @param relPath the relative path from site base content to the faceted navigation node, which must not start with a / and is relative to the site content base path
     * @param objectConverter
     * @param beanMappingClass the class T must be of 
     * @return The faceted navigation result document of type T and <code>null</code> if it cannot be found or is not of type <code>T</code>
     */
    public static <T extends  HippoBean> T getFacetedNavigationResultDocument(HstRequest hstRequest, HstQuery query, String relPath, 
            ObjectConverter objectConverter, Class<T> beanMappingClass)  {
        
        if(query == null) {
            return getFacetedNavigationResultDocument(hstRequest, (String)null, relPath, objectConverter, beanMappingClass);
        }
        
        String queryAsString = null;
        try {
            queryAsString = "xpath("+query.getQueryAsString(true)+")";
        } catch (QueryException e) {
            throw new HstComponentException("Unable to create a string representation of query", e);
        }
        return getFacetedNavigationResultDocument(hstRequest, queryAsString, relPath, objectConverter, beanMappingClass);
    }
    
    
    /**
     * This method tries to get a {@link Session} from a disposable pool which is identified by <code>disposablePoolIdentifier</code>
     * 
     * If <code>disposablePoolIdentifier</code> is empty or <code>null</code> an HstComponentException will be thrown. If it is not possible to return a 
     * {@link Session} for the <code>disposablePoolIdentifier</code>, for example because there is configured a MultipleRepositoryImpl instead of 
     * LazyMultipleRepositoryImpl, also a {@link HstComponentException} will be thrown.
     * 
     * 
     * @param hstRequest the hstRequest for this HstComponent
     * @param disposablePoolIdentifier the identifier for this disposable pool. It is not allowed to be empty or <code>null</code> 
     * @throws HstComponentException
     */
    public static Session getDisposablePoolSession(HstRequest hstRequest, String disposablePoolIdentifier) throws HstComponentException {
        Credentials cred = hstRequest.getRequestContext().getContextCredentialsProvider().getDefaultCredentials(hstRequest.getRequestContext());
       
        String userID =  ((SimpleCredentials)cred).getUserID();
        char[] passwd = ((SimpleCredentials)cred).getPassword();
        
        String disposablePoolSessionUserId = userID + ";"+disposablePoolIdentifier + ";disposable";
        
        SimpleCredentials disposablePoolSessionCredentials = new SimpleCredentials(disposablePoolSessionUserId, passwd);
        Repository repo = HstServices.getComponentManager().getComponent(Repository.class.getName());
        try {
            Session session = repo.login(disposablePoolSessionCredentials);
            return session;
        } catch (RepositoryException e) {
            throw new HstComponentException(e);
        }
     }

}
