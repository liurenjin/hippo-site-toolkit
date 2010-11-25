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
package org.hippoecm.hst.persistence;

import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;

/**
 * <CODE>ContentPersistenceManager</CODE> is the primary interface for HST-2-Persistence-aware application components.
 * <P>
 * An implementation of this interface should be able to convert content nodes to objects, and vice versa.
 * Also it should have knowledges on how to create, update or remove a content node with node type and absolute path.
 * </P>
 * <P>
 * If the content models are not complex and so just simple node structures are enough for the requirements, then
 * an implementation for this interface can probably provide an automatic bi-directional mappings.
 * </P>
 * <P>
 * However, in most real cases, the content models are very sophisticated, so they need careful workflow management
 * to fulfill real requirements such as faceted selection, virtual nodes, approval or publishing management.
 * Therefore, this sophisticated management should be implemented properly in separated classes.
 * </P>
 * <P>
 * Some sophisticated JCR repository engine already have their own workflow knowledges on 
 * how to create, update and remove content nodes based on node types.
 * Or, some domain specific content-based application should know the knowledges on their own content models.
 * In these cases, they can provide an implementation for this interface.
 * </P>
 * 
 * @version $Id$
 */
public interface ContentPersistenceManager {
    
    /**
     * Returns the content object at the specified absolute path.
     * @param absPath
     * @return an object mapped to the specified absolute path and its primary node type.
     * @throws ContentPersistenceException
     */
    Object getObject(String absPath) throws ContentPersistenceException;
    
    /**
     * Returns the content object by the specified uuid.
     * @param uuid
     * @return an object having the specified uuid
     * @throws ContentPersistenceException
     */
    Object getObjectByUuid(String uuid) throws ContentPersistenceException;
    
    /**
     * Creates content node(s) with the specified node type at the specified absolute path.
     * <P>
     * The absolute path could be regarded differently according to physical implementations.
     * For example, an implementation can regard the path as a simple one to create a simple JCR node.
     * On the other hand, a sophisticated implementation can regard the path as an input for 
     * a workflow-enabled document/folder path. 
     * </P>
     * 
     * @param absPath the absolute node path
     * @param nodeTypeName the node type name of the content object
     * @param name the content node name
     * @throws ContentPersistenceException
     */
    void create(String absPath, String nodeTypeName, String name) throws ContentPersistenceException;
    
    /**
     * Creates content node(s) with the specified node type at the specified absolute path.
     * <P>
     * The absolute path could be regarded differently according to physical implementations.
     * For example, an implementation can regard the path as a simple one to create a simple JCR node.
     * On the other hand, a sophisticated implementation can regard the path as an input for 
     * a workflow-enabled document/folder path. 
     * </P>
     * <P>
     * If <CODE>autoCreateFolders</CODE> is true, then folders will be automatically created.
     * </P>
     * 
     * @param absPath the absolute node path
     * @param nodeTypeName the node type name of the content object
     * @param name the content node name
     * @param autoCreateFolders the flag to create folders
     * @throws ContentPersistenceException
     */
    void create(String absPath, String nodeTypeName, String name, boolean autoCreateFolders) throws ContentPersistenceException;
    
    /**
     * Updates the content node which is mapped to the object.
     * <P>
     * An implementation can provide binding the content object to the physical JCR node(s) and updates.
     * An implementation can provide automatic content binding, or another requires pre-registered
     * <CODE>ContentNodeBinder</CODE> map to do real bindings. It probably depends on the functionalities
     * of underlying repository.
     * </P>
     * @param content
     * @throws ContentPersistenceException
     */
    void update(Object content) throws ContentPersistenceException;
    
    /**
     * Updates the content node which is mapped to the object by the <CODE>customContentNodeBinder</CODE>
     * provided by client.
     * <P>
     * Unlike {@link #update(Object)}, the implementation should not try to do automatic or predefined bindings.
     * Instead, it should invoke <CODE>customContentNodeBinder</CODE> to do bindings.
     * </P>
     * <P>
     * Therefore, if a developer wants to customize the bindings, the developer should provide a <CODE>customContentNodeBinder</CODE>.
     * </P>
     * @param content
     * @param customContentNodeBinder
     * @throws ContentPersistenceException
     */
    void update(Object content, ContentNodeBinder customContentNodeBinder) throws ContentPersistenceException;
    
    /**
     * Removes the content node which is mapped to the object.
     * @param content
     * @throws ContentPersistenceException
     */
    void remove(Object content) throws ContentPersistenceException;
    
    /**
     * Saves all pending changes. 
     * @throws ContentPersistenceException
     */
    void save() throws ContentPersistenceException;
    
    /**
     * Invokes {@link javax.jcr.Session#refresh(boolean)} with <CODE>false</CODE> parameter.  
     * @param keepChanges
     * @throws ContentPersistenceException
     */
    void refresh() throws ContentPersistenceException;
    
    /**
     * Invokes {@link javax.jcr.Session#refresh(boolean)}.  
     * @param keepChanges
     * @throws ContentPersistenceException
     */
    void refresh(boolean keepChanges) throws ContentPersistenceException;
    
}
