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
package org.hippoecm.hst.components.modules.navigation;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.core.template.node.el.AbstractELNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jreijn
 * 
 *
 */
public class NavigationItem extends AbstractELNode {
    private static final Logger log = LoggerFactory.getLogger(NavigationItem.class);

    Boolean selected = false;

    public NavigationItem(Node node) {
        super(node); 
    }

    public NavigationItem(Node node,Boolean selected) {
        super(node);
        this.selected = selected;
    }

    public List<NavigationItem> getChildren() {
        List<NavigationItem> wrappedChildNodes = new ArrayList<NavigationItem>();
        NodeIterator subNodes;
        try {
            subNodes = this.getJcrNode().getNodes();
            while (subNodes.hasNext()) {
                Node subNode = subNodes.nextNode();
                // always check a node iterator node for null
                if (subNode == null) {
                    continue;
                }
                if (subNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                    // if there is no document present with the same name as the handle
                    // skip this one (ie, there is a unpublished version, but not a published one)
                    if (!subNode.hasNode(subNode.getName())) {
                        continue;
                    }
                    subNode = subNode.getNode(subNode.getName());
                }
                wrappedChildNodes.add(new NavigationItem(subNode));
            }

        } catch (RepositoryException e) {
            log.error("RepositoryException while fetching children " + e.getMessage());
        }
        return wrappedChildNodes;
    }

    public Boolean getSelected() {
        return this.selected;
    }

}
