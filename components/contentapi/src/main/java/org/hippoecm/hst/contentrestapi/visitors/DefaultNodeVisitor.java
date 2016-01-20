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

package org.hippoecm.hst.contentrestapi.visitors;

import java.util.ArrayList;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.hippoecm.hst.contentrestapi.ResourceContext;
import org.onehippo.cms7.services.contenttype.ContentTypeProperty;

import static org.onehippo.repository.util.JcrConstants.NT_BASE;
import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_STATESUMMARY;

public class DefaultNodeVisitor extends AbstractNodeVisitor {

    public DefaultNodeVisitor(VisitorFactory visitorFactory) {
        super(visitorFactory);
    }

    @Override
    public String getNodeType() {
        return NT_BASE;
    }

    protected void visitNode(final ResourceContext context, final Node node, final Map<String, Object> response)
            throws RepositoryException {
        super.visitNode(context, node, response);
        response.put("type", node.getPrimaryNodeType().getName());
        NodeType[] mixinTypes = node.getMixinNodeTypes();
        if (mixinTypes.length > 0) {
            ArrayList<String> mixins = new ArrayList<>();
            for (NodeType mixin : mixinTypes) {
                mixins.add(mixin.getName());
            }
            response.put("mixins", mixins);
        }
    }

    protected boolean skipProperty(final ResourceContext context, final ContentTypeProperty propertyType,
                                   final Property property) throws RepositoryException {
        if (HIPPOSTD_STATESUMMARY.equals(property.getName())) {
            return true;
        }
        return super.skipProperty(context, propertyType, property);
    }
}