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
package org.hippoecm.hst.content.beans.query;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.query.filter.IsNodeTypeFilter;
import org.hippoecm.hst.content.beans.query.filter.NodeTypeFilter;
import org.hippoecm.hst.content.beans.query.filter.PrimaryNodeTypeFilterImpl;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HstQueryManagerImpl implements HstQueryManager{

    private static final Logger log = LoggerFactory.getLogger(HstQueryManagerImpl.class);

    private ObjectConverter objectConverter;
    private HstCtxWhereClauseComputer hstCtxWhereClauseComputer;
    private Session session;
    private final Filter.Resolution defaultResolution;

    /**
     * 
     * @param objectConverter
     * @param hstCtxWhereClauseComputer
     * @deprecated use {@link #HstQueryManagerImpl(Session, ObjectConverter, HstCtxWhereClauseComputer)} instead
     */
    @Deprecated
    public HstQueryManagerImpl(ObjectConverter objectConverter, HstCtxWhereClauseComputer hstCtxWhereClauseComputer) {
        this(null, objectConverter, hstCtxWhereClauseComputer, Filter.Resolution.EXPENSIVE_PRECISE);
        log.warn("Using deprecated HstQueryManagerImpl constructor. No Filter.Resolution is specified. Use default" +
                " Filter.Resolution.EXPENSIVE_PRECISE");
    }

    /**
     * @deprecated since 2.24.13  Use {@link #HstQueryManagerImpl(Session, ObjectConverter, HstCtxWhereClauseComputer, Filter.Resolution)}
     * instead
     */
    @Deprecated
    public HstQueryManagerImpl(Session session, ObjectConverter objectConverter, HstCtxWhereClauseComputer hstCtxWhereClauseComputer) {
        this(session, objectConverter, hstCtxWhereClauseComputer, Filter.Resolution.EXPENSIVE_PRECISE);
        log.warn("Using deprecated HstQueryManagerImpl constructor. No Filter.Resolution is specified. Use default" +
                " Filter.Resolution.EXPENSIVE_PRECISE");
    }

    public HstQueryManagerImpl(final Session session,
                               final ObjectConverter objectConverter,
                               final HstCtxWhereClauseComputer hstCtxWhereClauseComputer,
                               final Filter.Resolution resolution) {
        this.session = session;
        this.objectConverter = objectConverter;
        this.hstCtxWhereClauseComputer = hstCtxWhereClauseComputer;
        defaultResolution = resolution;
    }
   
    public HstQuery createQuery(Node scope) throws QueryException {
        return createQuery(scope, (NodeTypeFilter)null);
    }
    
    /**
     * Creates a query, with scope  and Filter for types of filterBean. If includeSubTypes is <code>true</code>,
     * the result may also contain HippoBean's whose primarytype is a subtype of the filterBean type. 
     * 
     */
    @SuppressWarnings("unchecked")
    public HstQuery createQuery(Node scope, Class<? extends HippoBean> filterBean, boolean includeSubTypes) throws QueryException {
        if(!includeSubTypes) {
           return createQuery(scope, filterBean);
        }
        String primaryNodeTypeNameForBean = objectConverter.getPrimaryNodeTypeNameFor(filterBean);
        IsNodeTypeFilter isNodeTypeFilter = null;
        if(primaryNodeTypeNameForBean == null) {
          throw new QueryException("Cannot find primaryNodeType for '"+filterBean.getName()+"'.");
        } else {
            isNodeTypeFilter = new IsNodeTypeFilter(primaryNodeTypeNameForBean);
        }
        return new HstQueryImpl(session, this.hstCtxWhereClauseComputer, this.objectConverter, scope, isNodeTypeFilter);
    }
    
   
    /**
     * Creates a empty query, with the scope HippoBean
     */
    public HstQuery createQuery(HippoBean scope) throws QueryException{
        if(scope == null || scope.getNode() == null) {
            return createQuery((Node)null);
        }
        return createQuery(scope.getNode());
    }
    
 
    
    /**
     * Creates a query, with scope HippoBean and Filter for types of filterBean. If includeSubTypes is <code>true</code>,
     * the result may also contain HippoBean's whose primarytype is a subtype of the filterBean type. 
     * 
     */
    public HstQuery createQuery(HippoBean scope, Class<? extends HippoBean> filterBean, boolean includeSubTypes) throws QueryException {
        if(scope.getNode() == null) {
            return createQuery((Node)null, filterBean, includeSubTypes);
        }
        return createQuery(scope.getNode(), filterBean, includeSubTypes);
    }
    
    public HstQuery createQuery(HippoBean scope, Class<? extends HippoBean>... filterBeans) throws QueryException{
        if(scope.getNode() == null) {
            return createQuery((Node)null, filterBeans);
        }
        return createQuery(scope.getNode(), filterBeans);
    }

    /**
     * Creates a query, with the scope HippoBean and with a Filter that filters to only return HippoBeans of the types that are 
     * added as variable arguments. It is not possible to retrieve subtypes of the applied filterBeans.
     */
    public HstQuery createQuery(Node scope, Class<? extends HippoBean>... filterBeans) throws QueryException{
        List<String> primaryNodeTypes = new ArrayList<String>(); 
        for(Class<? extends HippoBean> annotatedBean : filterBeans) {
           String primaryNodeTypeNameForBean = objectConverter.getPrimaryNodeTypeNameFor(annotatedBean);
           if(primaryNodeTypeNameForBean != null) {
               primaryNodeTypes.add(primaryNodeTypeNameForBean);
           } else {
               throw new QueryException("Cannot find primaryNodeType for '"+annotatedBean.getClass().getName()+"'.");
           }
        }
        NodeTypeFilter primaryNodeTypeFilter = null;
        if(primaryNodeTypes.size() > 0) {
            primaryNodeTypeFilter  = new PrimaryNodeTypeFilterImpl(primaryNodeTypes.toArray(new String[primaryNodeTypes.size()]));
        }
        return createQuery(scope, primaryNodeTypeFilter);
    }
    
    public HstQuery createQuery(Node scope, String nodeType, boolean includeSubTypes) throws QueryException {
        if (nodeType == null) {
            throw new IllegalArgumentException("The node type for query must not be null!");
        }
        
        if (!includeSubTypes) {
            return createQuery(scope, nodeType);
        }
        
        IsNodeTypeFilter isNodeTypeFilter = new IsNodeTypeFilter(nodeType);
        HstQueryImpl query = new HstQueryImpl(session, this.hstCtxWhereClauseComputer, this.objectConverter, scope, isNodeTypeFilter);
        query.setDefaultResolution(defaultResolution);
        return query;
    }
    
    public HstQuery createQuery(HippoBean scope, String... primaryNodeTypes) throws QueryException {
        if (scope.getNode() == null) {
            return createQuery((Node) null, primaryNodeTypes);
        }
        
        return createQuery(scope.getNode(), primaryNodeTypes);
    }
    
    public HstQuery createQuery(Node scope, String ... primaryNodeTypes) throws QueryException {
        if (primaryNodeTypes == null) {
            throw new IllegalArgumentException("Primary node types for query must not be null!");
        }
        
        NodeTypeFilter primaryNodeTypeFilter = null;
        primaryNodeTypeFilter = new PrimaryNodeTypeFilterImpl(primaryNodeTypes);
        return createQuery(scope, primaryNodeTypeFilter);
    }
    
    private HstQuery createQuery(Node scope, NodeTypeFilter filter) throws QueryException {
        HstQueryImpl query  =  new HstQueryImpl(session, this.hstCtxWhereClauseComputer, this.objectConverter, scope, filter);
        query.setDefaultResolution(defaultResolution);
        return query;
    }
    
}
