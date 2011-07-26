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
package org.hippoecm.hst.util;

import java.util.UUID;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.site.HstServices;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;

/**
 * NodeUtils
 * 
 * @version $Id$
 */
public class NodeUtils {

    private static final String LOGGER_CATEGORY_NAME = NodeUtils.class.getName();
    /**
     * Returns the canonical version of this node, and <code>null</code> when there is no canonical node
     * 
     * @param node
     * @return  the canonical version of this node, and <code>null</code> when there is no canonical node.
     * @throws java.lang.RuntimeException when the repository throws a general repository exception
     */
    public static Node getCanonicalNode(Node node) {
        if(node instanceof HippoNode) {
            HippoNode hnode = (HippoNode)node;
            try {
                Node canonical = hnode.getCanonicalNode();
                if(canonical == null) {
                    HstServices.getLogger(LOGGER_CATEGORY_NAME).debug("Cannot get canonical node for '{}'. This means there is no phyiscal equivalence of the " +
                            "virtual node. Return null", node.getPath());
                }
                return canonical;
            } catch (RepositoryException e) {
                HstServices.getLogger(LOGGER_CATEGORY_NAME).error("Repository exception while fetching canonical node. Return null" , e);
                throw new RuntimeException(e);
            }
        } 
        return node;
    }
    
   
    /**
     * Returns the canonical version of this node, and <code>defaultNode</code> when there is no canonical node
     * 
     * @param node
     * @param defaultNode
     * @return  the canonical version of this node, and <code>defaultNode</code> when there is no canonical node.
     * @throws java.lang.RuntimeException when the repository throws a general repository exception
     * @deprecated use {@link #getCanonicalNode(Node)} instead
     */
    @Deprecated
    public static Node getCanonicalNode(Node node, Node defaultNode) {
        if (node == null) {
            throw new IllegalArgumentException("Node should be not null in finding canonical node.");
        }
        
        Node canonicalNode = null;
        
        if (node instanceof HippoNode) {
            try {
                canonicalNode = ((HippoNode) node).getCanonicalNode();
                
                if(canonicalNode == null) {
                    HstServices.getLogger(LOGGER_CATEGORY_NAME).debug("Cannot get canonical node for '{}'. This means there is no phyiscal equivalence of the " +
                            "virtual node. Return null", node.getPath());
                }
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        }
        
        return (canonicalNode != null ? canonicalNode : defaultNode);
    }
    
    /**
     * 
     * @param mirrorNode
     * @return the dereferenced node or <code>null</code> when no dereferenced node can be found
     */
    public static Node getDeref(Node mirrorNode) {
        String docBaseUUID = null;
        try {
            if(!mirrorNode.isNodeType(HippoNodeType.NT_FACETSELECT) && !mirrorNode.isNodeType(HippoNodeType.NT_MIRROR)) {
                HstServices.getLogger(LOGGER_CATEGORY_NAME).info("Cannot deref a node that is not of (sub)type '{}' or '{}'. Return null", HippoNodeType.NT_FACETSELECT, HippoNodeType.NT_MIRROR);
                return null;
            }
            // HippoNodeType.HIPPO_DOCBASE is a mandatory property so no need to test if exists
            docBaseUUID = mirrorNode.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
            
            // test whether docBaseUUID can be parsed as a uuid
            try {
                UUID.fromString(docBaseUUID);
            } catch(IllegalArgumentException e) {
                HstServices.getLogger(LOGGER_CATEGORY_NAME).warn("Docbase cannot be parsed to a valid uuid. Return null");
                return null;
            }
            return mirrorNode.getSession().getNodeByIdentifier(docBaseUUID);
        } catch (ItemNotFoundException e) {
            String path = null;
            try {
                path = mirrorNode.getPath();
            } catch (RepositoryException e1) {
                HstServices.getLogger(LOGGER_CATEGORY_NAME).error("RepositoryException, cannot return deferenced node: {}", e1);
            }
            HstServices.getLogger(LOGGER_CATEGORY_NAME).info("ItemNotFoundException, cannot return deferenced node because docbase uuid '{}' cannot be found. The docbase property is at '{}/hippo:docbase'. Return null", docBaseUUID, path);
        } catch (RepositoryException e) {
            HstServices.getLogger(LOGGER_CATEGORY_NAME).error("RepositoryException, cannot return deferenced node: {}", e);
        }
        
        return null;
    }
}
