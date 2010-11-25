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
package org.hippoecm.hst.core.template.node.el;

import java.util.Map;

import javax.jcr.Node;

public interface ELNode {
    public Node getJcrNode();
    public Map getProperty();
    public Map getHasProperty();
    public Map getNode();
    public Map getNodes();
    public Map getNodesoftype();
    /*
     * use getDeref in EL like node.deref['hippo:docbase']. This returns a node that 
     * is represented by the uuid in String value for the property
     */
    public Map getDeref(); 
    public ELNode getParent();
    public String getDecodedName();
    public String getName();
    public String getNodetype();
    /*
     * Return the uuid of the canonical (!!) node or null when the node is not referenceable 
     */
    public String getUuid(); 
    public String getPath();
    public String getRelpath();
    
}
