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
package org.hippoecm.hst.content.beans.manager.workflow;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

import org.hippoecm.hst.content.beans.ContentNodeBinder;
import org.hippoecm.hst.content.beans.ObjectBeanPersistenceException;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManagerImpl;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.util.NodeUtils;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.slf4j.LoggerFactory;

/**
 * An implementation for {@link WorkflowPersistenceManager} interface with Hippo Repository Workflow API.
 * <P>
 * This implementation does not provide automatic bindings from content object to JCR node(s).
 * So, client codes should provide custom binders for their own node types. These custom binders map can be
 * given by a constructor argument, or a custom binder can be given by an argument of {@link #update(Object, ContentNodeBinder)} method.
 * </P>
 * <P>
 * Another useful option is to make a content POJO object implement {@link ContentNodeBinder} interface.
 * When client codes invoke {@link #update(Object)} method, this implementation will look up the custom binder 
 * from the internal map at first. If there's nothing found, then this implementation will check if the content POJO
 * object is an instance of {@link ContentNodeBinder}. If it is, this implementation will use the content POJO object itself
 * as a <CODE>ContentNodeBinder</CODE>.
 * </P>
 * <P>
 * If this implementation cannot find any <CODE>ContentNodeBinder</CODE>, it will do updating the content without any bindings.
 * </P>
 * 
 */
public class WorkflowPersistenceManagerImpl extends ObjectBeanManagerImpl implements WorkflowPersistenceManager {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(WorkflowPersistenceManagerImpl.class);
    
    /**
     * Custom content node binders map, which is used to look up a custom binder for a node type.
     */
    protected Map<String, ContentNodeBinder> contentNodeBinders;
    
    /**
     * Hippo Repository specific predefined folder node type name
     */
    protected String folderNodeTypeName = "hippostd:folder";

    /**
     * The workflow category name to get a folder workflow. We use threepane as this is the same as the CMS uses
     */
    protected String folderNodeWorkflowCategory = "threepane"; 
    
    /**
     * The workflow category name to get a document workflow. 
     */
    protected String documentNodeWorkflowCategory = "default"; 
    
    /**
     * The workflow category name to add a new document.
     */
    protected String documentAdditionWorkflowCategory = "new-document"; 
    
    /**
     * The workflow category name to add a new folder.
     */
    protected String folderAdditionWorkflowCategory = "new-folder"; 

    /**
     * Workflow callback handler
     */
    protected WorkflowCallbackHandler workflowCallbackHandler;

    /**
     * The codec which is used for the node names
     */
    protected StringCodec uriEncoding = new StringCodecFactory.UriEncoding();

    /**
     * The workflow category name to localize the new document
     */
    protected String defaultWorkflowCategory = "core";

    /**
     * Constructor
     * @param session the session for this manager context
     * @param objectConverter the object converter to do mapping from JCR nodes to content POJO objects
     */
    public WorkflowPersistenceManagerImpl(Session session, ObjectConverter objectConverter) {
        this(session, objectConverter, null);
    }
     
    /**
     * Constructor
     * @param session the session for this manager context
     * @param objectConverter the object converter to do mapping from JCR nodes to content POJO objects
     * @param contentNodeBinders the predefined content node binders map which item is node type name key and custom binder object value.
     */
    public WorkflowPersistenceManagerImpl(Session session, ObjectConverter objectConverter, Map<String, ContentNodeBinder> contentNodeBinders) {
        super(session, objectConverter);
        this.contentNodeBinders = contentNodeBinders;
    }
   
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
     * @throws ObjectBeanPersistenceException
     * @deprecated the name of the created node can differ from the passed name. Use {@link #createAndReturn(String absPath, String nodeTypeName, String name, boolean autoCreateFolders)}
     * to get the absolute path of the created node.
     */
    @Deprecated
    public void create(String absPath, String nodeTypeName, String name) throws ObjectBeanPersistenceException {
        createAndReturn(absPath, nodeTypeName, name, false);
    }
    
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
     * @throws ObjectBeanPersistenceException
     * @deprecated the name of the created node can differ from the passed name. Use {@link #createAndReturn(String absPath, String nodeTypeName, String name, boolean autoCreateFolders)}
     * to get the absolute path of the created node.
     */
    @Deprecated
    public void create(String absPath, String nodeTypeName, String name, boolean autoCreateFolders) throws ObjectBeanPersistenceException {
        createAndReturn(absPath, nodeTypeName, name, autoCreateFolders);
    }

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
     * @return the absolute path of the created node
     * @throws ObjectBeanPersistenceException
     */
    public String createAndReturn(final String absPath, final String nodeTypeName, final String name, final boolean autoCreateFolders) throws ObjectBeanPersistenceException {
        try {
            final Node parentFolderNode;
            if (!session.itemExists(absPath)) {
                if (!autoCreateFolders) {
                    throw new ObjectBeanPersistenceException("The folder node is not found on the path: " + absPath);
                } else {
                    parentFolderNode = createMissingFolders(absPath);
                }
            } else {
                parentFolderNode = session.getNode(absPath);
            }

            return createNodeByWorkflow(parentFolderNode, nodeTypeName, name);
        } catch (ObjectBeanPersistenceException e) {
            throw e;
        } catch (Exception e) {
            throw new ObjectBeanPersistenceException(e);
        }
    }

    protected Node createMissingFolders(String absPath) throws ObjectBeanPersistenceException {
        try {
            String [] folderNames = absPath.split("/");

            Node rootNode = session.getRootNode();
            Node curNode = rootNode;
            String folderNodePath = null;
            
            for (String folderName : folderNames) {
                String folderNodeName = uriEncoding.encode(folderName);

                if (!"".equals(folderNodeName)) {
                    if (curNode == rootNode) {
                        folderNodePath = "/" + folderNodeName;
                    } else {
                        folderNodePath = curNode.getPath() + "/" + folderNodeName;
                    }
                    
                    if (!session.itemExists(folderNodePath)) {
                        curNode = session.getNode(createNodeByWorkflow(curNode, folderNodeTypeName, folderName));
                    } else {
                        curNode = curNode.getNode(folderNodeName);
                    }

                    if (curNode.isNodeType(HippoNodeType.NT_FACETSELECT) || curNode.isNodeType(HippoNodeType.NT_MIRROR )) {
                        String docbaseUuid = curNode.getProperty("hippo:docbase").getString();
                        // check whether docbaseUuid is a valid uuid, otherwise a runtime IllegalArgumentException is thrown
                        try {
                            UUID.fromString(docbaseUuid);
                        } catch (IllegalArgumentException e){
                            throw new ObjectBeanPersistenceException("hippo:docbase in mirror does not contain a valid uuid", e);
                        }
                        // this is always the canonical
                        curNode = session.getNodeByIdentifier(docbaseUuid);
                    } else {
                        curNode = getCanonicalNode(curNode);
                    }
                }
            }

            return curNode;
        } catch (ObjectBeanPersistenceException e) {
            throw e;
        } catch (Exception e) {
            throw new ObjectBeanPersistenceException(e);
        }
    }
    
    protected String createNodeByWorkflow(Node folderNode, String nodeTypeName, String name)
            throws ObjectBeanPersistenceException {
        try {
            folderNode = getCanonicalNode(folderNode);
            Workflow wf = getWorkflow(folderNodeWorkflowCategory, folderNode);

            if (wf instanceof FolderWorkflow) {
                FolderWorkflow fwf = (FolderWorkflow) wf;

                String category = documentAdditionWorkflowCategory;

                
                if (nodeTypeName.equals(folderNodeTypeName)) {
                    category = folderAdditionWorkflowCategory;
                    
                    // now check if there is some more specific workflow for hippostd:folder
                    if(fwf.hints() != null &&  fwf.hints().get("prototypes") != null ) {
                        Object protypesMap = fwf.hints().get("prototypes");
                        if(protypesMap instanceof Map) {
                            for(Object o : ((Map)protypesMap).entrySet()) {
                                Entry entry = (Entry) o;
                                if(entry.getKey() instanceof String && entry.getValue() instanceof Set) {
                                    if( ((Set)entry.getValue()).contains(folderNodeTypeName)) {
                                        // we found possibly a more specific workflow for folderNodeTypeName. Use the key as category
                                        category =  (String)entry.getKey();
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }

                String nodeName = uriEncoding.encode(name);
                String added = fwf.add(category, nodeTypeName, nodeName);
                if (added == null) {
                    throw new ObjectBeanPersistenceException("Failed to add document/folder for type '" + nodeTypeName
                            + "'. Make sure there is a prototype.");
                }
                Item addedDocumentVariant = folderNode.getSession().getItem(added);
                if (addedDocumentVariant instanceof Node && !nodeName.equals(name)) {
                    DefaultWorkflow defaultWorkflow = (DefaultWorkflow) getWorkflow(defaultWorkflowCategory, (Node)addedDocumentVariant);
                    defaultWorkflow.localizeName(name);
                }
                return added;
            } else {
                throw new ObjectBeanPersistenceException("The workflow is not a FolderWorkflow for "
                        + folderNode.getPath() + ": " + wf);
            }
        } catch (RepositoryException e) {
            throw new ObjectBeanPersistenceException(e);
        } catch (RemoteException e) {
            throw new ObjectBeanPersistenceException(e);
        } catch (WorkflowException e) {
            throw new ObjectBeanPersistenceException(e);
        }
    }

    
    /**
     * Updates the content node which is mapped to the object.
     * <P>
     * This will look up a propery custom content node binder from the internal map. ({@link #contentNodeBinders}).
     * If it is not found there, this implementation will check if the content object is an instance of {@link ContentNodeBinder} interface.
     * If so, the content object will be used as a custom binder.
     * </P>
     * <P>
     * If there's no content node binder found, then this implementation will do updating
     * only without any bindings.
     * </P>
     * @param content
     * @throws ObjectBeanPersistenceException
     */
    public void update(Object content) throws ObjectBeanPersistenceException {
        if (content instanceof HippoBean) {
            ContentNodeBinder binder = null;

            if (contentNodeBinders != null && !contentNodeBinders.isEmpty()) {
                try {
                    HippoBean contentBean = (HippoBean) content;
                    Node contentNode = contentBean.getNode();
                    contentNode = getCanonicalNode(contentNode);
                    binder = contentNodeBinders.get(contentNode.getPrimaryNodeType().getName());
                } catch (Exception e) {
                    throw new ObjectBeanPersistenceException(e);
                }
            }
            
            if (binder == null && content instanceof ContentNodeBinder) {
                binder = (ContentNodeBinder) content;
            }
            
            update(content, binder);
        } else {
            throw new ObjectBeanPersistenceException("The content object parameter should be an instance of HippoBean.");
        }
    }
    
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
     * @throws ObjectBeanPersistenceException
     */
    public void update(Object content, ContentNodeBinder customContentNodeBinder) throws ObjectBeanPersistenceException {
        String path = null; 
        if (content instanceof HippoBean) {
            try {
                HippoBean contentBean = (HippoBean) content;
                Node contentNode = contentBean.getNode();
                path = contentNode.getPath();
                contentNode = getCanonicalNode(contentNode);
                Workflow wf = getWorkflow(documentNodeWorkflowCategory, contentNode);
                
                if (wf != null) {
                    Document document = null;
                    if(customContentNodeBinder != null) {
                        if (wf instanceof EditableWorkflow) {
                            EditableWorkflow ewf = (EditableWorkflow) wf;
                            document = ewf.obtainEditableInstance();
                            String uuid = document.getIdentity();
                            
                            if (uuid != null && !"".equals(uuid)) {
                                contentNode = session.getNodeByIdentifier(uuid);
                            }
                            boolean changed = customContentNodeBinder.bind(content, contentNode);
                            
                            if (changed) {
                                contentNode.save();
                                // we need to recreate the EditableWorkflow because the node has changed
                                ewf = (EditableWorkflow) getWorkflow(documentNodeWorkflowCategory, contentNode);
                                document = ewf.commitEditableInstance();
                                if (workflowCallbackHandler != null) {
                                    // recreate the wf because now the is changed
                                    wf = getWorkflow(documentNodeWorkflowCategory, document);
                                    if (wf != null) {
                                        workflowCallbackHandler.processWorkflow(wf);
                                    } else {
                                        throw new ObjectBeanPersistenceException("Workflow callback cannot be called because the workflow is null. ");
                                    }
                                }
                            } else {
                                document = ewf.disposeEditableInstance();
                            }
                        } else {
                            throw new ObjectBeanPersistenceException("The workflow is not a EditableWorkflow for " + contentBean.getPath() + ": " + wf);
                        } 
                    } else if (workflowCallbackHandler != null) {
                        if (wf != null) {
                            workflowCallbackHandler.processWorkflow(wf);
                        } else {
                            throw new ObjectBeanPersistenceException("Workflow callback cannot be called because the workflow is null. ");
                        }
                    }
                }
            } catch (ObjectBeanPersistenceException e) {
                throw e;
            } catch (Exception e) {
                if(path != null) {
                    throw new ObjectBeanPersistenceException("Exception while trying to update '"+path+"'" ,e);
                } else {
                    throw new ObjectBeanPersistenceException(e);
                }
            }
        } else {
            throw new ObjectBeanPersistenceException("The content object parameter should be an instance of HippoBean.");
        }
    }
    
    /**
     * Removes the content node which is mapped to the object.
     * @param content
     * @throws ObjectBeanPersistenceException
     */
    public void remove(Object content) throws ObjectBeanPersistenceException {
        if (!(content instanceof HippoBean)) {
            throw new ObjectBeanPersistenceException("The content object parameter should be an instance of HippoBean.");
        }
        
        try {
            HippoBean beanToRemove = (HippoBean) content;
            
            Node canonicalNodeToRemove = getCanonicalNode(beanToRemove.getNode());
            
            if(beanToRemove instanceof HippoDocumentBean) {
                canonicalNodeToRemove = canonicalNodeToRemove.getParent();
            } else if(beanToRemove instanceof HippoFolderBean){
                // do nothing
            } else {
                throw new ObjectBeanPersistenceException("Don't know how to persist a bean of type '"+beanToRemove.getClass().getName()+"'");
            }
            
            String nodeNameToRemove = canonicalNodeToRemove.getName();
            Node folderNodeToRemoveFrom = canonicalNodeToRemove.getParent();
            Workflow wf = getWorkflow(folderNodeWorkflowCategory, folderNodeToRemoveFrom);
            
            if (wf instanceof FolderWorkflow) {
                FolderWorkflow fwf = (FolderWorkflow) wf;
                fwf.delete(nodeNameToRemove);
                
            } else {
                throw new ObjectBeanPersistenceException("The workflow is not a FolderWorkflow for " + folderNodeToRemoveFrom.getPath() + ": " + wf);
            }
        } catch (Exception e) {
            throw new ObjectBeanPersistenceException(e);
        }
    }

    /**
     * Saves all pending changes. 
     * @throws ObjectBeanPersistenceException
     */
    public void save() throws ObjectBeanPersistenceException {
        try {
            session.save();
            // also do a refresh, because it is possible that through workflow another jcr session made the changes, and that the current
            // has no changes, hence a session.save() does not trigger a refresh
            session.refresh(false); 
        } catch (Exception e) {
            throw new ObjectBeanPersistenceException(e);
        }
    }

    /**
     * Invokes {@link javax.jcr.Session#refresh(boolean)} with <CODE>false</CODE> parameter.
     * @throws ObjectBeanPersistenceException
     */
    public void refresh() throws ObjectBeanPersistenceException {
        refresh(false);
    }
    
    /**
     * Invokes {@link javax.jcr.Session#refresh(boolean)}.  
     * @param keepChanges
     * @throws ObjectBeanPersistenceException
     */
    public void refresh(boolean keepChanges) throws ObjectBeanPersistenceException {
        try {
            session.refresh(keepChanges);
        } catch (Exception e) {
            throw new ObjectBeanPersistenceException(e);
        }
    }
    
    /**
     * Sets the folder node type name which is used to create folders.
     * @param folderNodeTypeName
     */
    public void setFolderNodeTypeName(String folderNodeTypeName) {
        this.folderNodeTypeName = folderNodeTypeName;
    }

    /**
     * Gets the folder node type name which is used to create folders.
     * @return
     */
    public String getFolderNodeTypeName() {
        return folderNodeTypeName;
    }
    
    /**
     * Gets the workflow category name used to get a folder workflow.
     * @return
     */
    public String getFolderNodeWorkflowCategory() {
        return folderNodeWorkflowCategory;
    }

    /**
     * Sets the workflow category name used to get a folder workflow.
     * @param folderNodeWorkflowCategory
     */
    public void setFolderNodeWorkflowCategory(String folderNodeWorkflowCategory) {
        this.folderNodeWorkflowCategory = folderNodeWorkflowCategory;
    }

    /**
     * Gets the workflow category name used to get a document workflow.
     * @return
     */
    public String getDocumentNodeWorkflowCategory() {
        return documentNodeWorkflowCategory;
    }

    /**
     * Sets the workflow category name used to get a document workflow.
     * @param documentNodeWorkflowCategory
     */
    public void setDocumentNodeWorkflowCategory(String documentNodeWorkflowCategory) {
        this.documentNodeWorkflowCategory = documentNodeWorkflowCategory;
    }

    /**
     * Gets the workflow category name used to add a folder.
     * @return
     */
    public String getFolderAdditionWorkflowCategory() {
        return folderAdditionWorkflowCategory;
    }

    /**
     * Sets the workflow category name used to add a folder.
     * @param folderAdditionWorkflowCategory
     */
    public void setFolderAdditionWorkflowCategory(String folderAdditionWorkflowCategory) {
        this.folderAdditionWorkflowCategory = folderAdditionWorkflowCategory;
    }

    /**
     * Gets the workflow category name used to add a document.
     * @return
     */
    public String getDocumentAdditionWorkflowCategory() {
        return documentAdditionWorkflowCategory;
    }

    /**
     * Sets the workflow category name used to add a document.
     * @param documentAdditionWorkflowCategory
     */
    public void setDocumentAdditionWorkflowCategory(String documentAdditionWorkflowCategory) {
        this.documentAdditionWorkflowCategory = documentAdditionWorkflowCategory;
    }
    
    public void setWorkflowCallbackHandler(WorkflowCallbackHandler<? extends Workflow> workflowCallbackHandler) {
        this.workflowCallbackHandler = workflowCallbackHandler;
    }
    
    public Workflow getWorkflow(String category, Node node) throws RepositoryException {
        Workspace workspace = session.getWorkspace();
        
        ClassLoader workspaceClassloader = workspace.getClass().getClassLoader();
        ClassLoader currentClassloader = Thread.currentThread().getContextClassLoader();
        
        try {
            if (workspaceClassloader != currentClassloader) {
                Thread.currentThread().setContextClassLoader(workspaceClassloader);
            }
            
            WorkflowManager wfm = ((HippoWorkspace) workspace).getWorkflowManager();
            return wfm.getWorkflow(category, node);
        } catch (RepositoryException e) {
            throw e;
        } catch (Exception ignore) {
            // Just ignore other exceptions which are not handled properly in the repository such as NPE.
        } finally {
            if (workspaceClassloader != currentClassloader) {
                Thread.currentThread().setContextClassLoader(currentClassloader);
            }
        }
        
        return null;
    }
    
    public Workflow getWorkflow(String category, Document document) throws RepositoryException {
        Workspace workspace = session.getWorkspace();
        
        ClassLoader workspaceClassloader = workspace.getClass().getClassLoader();
        ClassLoader currentClassloader = Thread.currentThread().getContextClassLoader();
        
        try {
            if (workspaceClassloader != currentClassloader) {
                Thread.currentThread().setContextClassLoader(workspaceClassloader);
            }
            
            WorkflowManager wfm = ((HippoWorkspace) workspace).getWorkflowManager();
            return wfm.getWorkflow(category, document);
        } catch (RepositoryException e) {
            throw e;
        } catch (Exception e) {
            // other exception which are not handled properly in the repository (we cannot do better here then just log them)
            if(log.isDebugEnabled()) {
                log.warn("Exception in workflow", e);
            } else {
                log.warn("Exception in workflow: {}", e.toString());
            }
        } finally {
            if (workspaceClassloader != currentClassloader) {
                Thread.currentThread().setContextClassLoader(currentClassloader);
            }
        }
        
        return null;
    }
    
    private Node getCanonicalNode(Node folderNode) throws ObjectBeanPersistenceException {
        folderNode = NodeUtils.getCanonicalNode(folderNode);
        if(folderNode == null) {
            throw new ObjectBeanPersistenceException("Cannot perform workflow on a node that does not have a canonical version");
        }
        return folderNode;
    }

}
