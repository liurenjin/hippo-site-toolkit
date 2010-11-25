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

import javax.jcr.Node;

import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.request.HstRequestContext;

public interface HstQueryManager {

    /**
     * Creates a empty query, with scope
     * @param scope 
     * @return a new <code>{@link HstQuery}</code> with scope {@code scope}
     */
    HstQuery createQuery(Node scope) throws QueryException;
    
    /**
     * Creates a query, with scope  and Filter for types of filterBean. If includeSubTypes is <code>true</code>,
     * the result may also contain HippoBean's whose primarytype is a subtype of the filterBean type. 
     * 
     * @param scope
     * @param filterBean
     * @param includeSubTypes
     * @return a new <code>{@link HstQuery}</code> with scope & filter
     * @throws QueryException
     */
    HstQuery createQuery(Node scope, Class<? extends HippoBean> filterBean, boolean includeSubTypes) throws QueryException;
    
    /**
     * Creates a query, with scope and Filter for node types. If includeSubTypes is <code>true</code>,
     * the result may also contain node types whose primary type is a subtype of the filter node type. 
     * 
     * @param scope
     * @param nodeType
     * @param includeSubTypes
     * @return a new <code>{@link HstQuery}</code> with scope & filter
     * @throws QueryException
     */
    HstQuery createQuery(Node scope, String nodeType, boolean includeSubTypes) throws QueryException;
    
    /**
     * Creates a empty query, with scope
     * @param scope 
     * @return a new <code>{@link HstQuery}</code> with scope {@code scope}
     */ 
    HstQuery createQuery(HippoBean scope) throws QueryException;
    
    /**
     * Creates a query, with scope HippoBean and Filter for types of filterBean. If includeSubTypes is <code>true</code>,
     * the result may also contain HippoBean's whose primarytype is a subtype of the filterBean type. 
     * 
     * @param scope
     * @param filterBean
     * @param includeSubTypes
     * @return a new <code>{@link HstQuery}</code> with scope & filter
     * @throws QueryException
     */
    HstQuery createQuery(HippoBean scope, Class<? extends HippoBean> filterBean, boolean includeSubTypes) throws QueryException;
    
    /**
     * Creates a query, with the scope HippoBean and with a Filter that filters to only return HippoBeans of the types that are 
     * added as variable arguments. It is not possible to retrieve subtypes of the applied filterBeans. If needed, use 
     * {@link #createQuery(HstRequestContext, HippoBean, Class, boolean)} instead.
     * 
     * @param scope
     * @param filterBeans
     * @return a new <code>{@link HstQuery}</code> with scope & filter on jcr primary nodetype of the filterBean
     * @throws QueryException
     */
    HstQuery createQuery(Node scope, Class<? extends HippoBean>... filterBeans) throws QueryException;
    
    /**
     * Creates a query, with the scope HippoBean and with a Filter that filters to only return HippoBeans of the types that are 
     * added as variable arguments. It is not possible to retrieve subtypes of the applied primary node types. If needed, use 
     * {@link #createQuery(HstRequestContext, HippoBean, Class, boolean)} instead.
     * 
     * @param scope
     * @param primaryNodeTypes
     * @return a new <code>{@link HstQuery}</code> with scope & filter on jcr primary nodetype of the filterBean
     * @throws QueryException
     */
    HstQuery createQuery(Node scope, String ... primaryNodeTypes) throws QueryException;
    
    /**
     * Creates a query, with a scope and with a Filter that filters to only return HippoBeans of the types that are 
     * added as variable arguments. It is not possible to retrieve subtypes of the applied filterBeans with this method. If needed, use 
     * {@link #createQuery(HstRequestContext, Node, Class, boolean)} instead.
     * 
     * @param scope
     * @param filterBean
     * @return a new <code>{@link HstQuery}</code> with scope and filter on jcr primary nodetype of the filterBean
     * @throws QueryException
     */
    HstQuery createQuery(HippoBean scope, Class<? extends HippoBean>... filterBean) throws QueryException;
    
    /**
     * Creates a query, with a scope and with a Filter that filters to only return HippoBeans of the types that are 
     * added as variable arguments. It is not possible to retrieve subtypes of the applied primary node types with this method. If needed, use 
     * {@link #createQuery(HstRequestContext, Node, Class, boolean)} instead.
     * 
     * @param scope
     * @param primaryNodeTypes
     * @return a new <code>{@link HstQuery}</code> with scope and filter on jcr primary nodetype of the filterBean
     * @throws QueryException
     */
    HstQuery createQuery(HippoBean scope, String ... primaryNodeTypes) throws QueryException;
    
}
