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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.jaxrs.contentrestapi.ResourceContext;

public abstract class AbstractBaseVisitor implements Visitor {

    private final VisitorFactory visitorFactory;

    protected AbstractBaseVisitor(final VisitorFactory visitorFactory) {
        this.visitorFactory = visitorFactory;
    }

    @Override
    public VisitorFactory getVisitorFactory() {
        return visitorFactory;
    }

    @Override
    public void visit(final ResourceContext context, final PropertyIterator propertyIterator, final Map<String, Object> destination) throws RepositoryException {
        while (propertyIterator.hasNext()) {
            final Property property = propertyIterator.nextProperty();
            final Visitor visitor = getVisitorFactory().getVisitor(context, property);
            visitor.visit(context, property, destination);
        }
    }

    @Override
    public void visit(final ResourceContext context, final NodeIterator nodeIterator, final Map<String, Object> destination) throws RepositoryException {
        while (nodeIterator.hasNext()) {
            final Node node = nodeIterator.nextNode();
            final Visitor visitor = getVisitorFactory().getVisitor(context, node);
            visitor.visit(context, node, destination);
        }
    }

    @Override
    public void visit(final ResourceContext context, final Property property, final Map<String, Object> destination) throws RepositoryException {
        // noop
    }

    @Override
    public void visit(final ResourceContext context, final Node node, final Map<String, Object> destination) throws RepositoryException {
        // noop
    }
}
