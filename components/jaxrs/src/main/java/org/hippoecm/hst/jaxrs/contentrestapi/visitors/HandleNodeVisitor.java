/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.jaxrs.contentrestapi.visitors;

import java.util.Map;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

class HandleNodeVisitor extends Visitor {

    public HandleNodeVisitor(VisitorFactory factory) {
        super(factory);
    }

    public void visit(final Item sourceItem, final Map<String, Object> destination) throws RepositoryException {
        final Node sourceNode = (Node) sourceItem;
        final String sourceNodeName = sourceNode.getName();

        destination.put("jcr:name", sourceNodeName);
        destination.put("jcr:uuid", sourceNode.getIdentifier());

        final Node variant = sourceNode.getNode(sourceNodeName);
        visitAllSiblings(getFactory(), variant, destination);
    }

}