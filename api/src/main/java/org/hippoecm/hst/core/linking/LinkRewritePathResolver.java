/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.linking;

import javax.jcr.Node;

import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * <p>
 *      <b>Expert feature</b> : Note that if you implement a custom {@link LinkRewritePathResolver}, that you make sure
 *      that {@link #getPath(Node, HstRequestContext, boolean, boolean)} is very fast! Namely,
 *       {@link #getPath(Node, HstRequestContext, boolean, boolean)} will be invoked for every
 *      link creation of a {@link javax.jcr.Node} or {@link org.hippoecm.hst.content.beans.standard.HippoBean}
 * </p>
 * <p>
 *     A usecase where this NodeToPathResolver can be used is for example when you have <i>comment</i> documents which have
 *     a link to the <i>news</i> article they are a comment about. The <i>comment</i> documents themselves do *not* have
 *     a URL. They are only visible in the context of the news article they are a comment about. In this case, when there
 *     is a link to a <i>comment</i> document or when a <i>comment</i> document is found via search, you want to actually
 *     have link creation for the node path of the <i><b>news</b></i> document. This can be achieved with a custom
 *     {@link LinkRewritePathResolver}. Remember however that the implementation <b>must</b> be fast. Whenever
 *     the method {@link #getPath(Node, HstRequestContext, boolean, boolean)} takes more than a couple
 *     (say 2 to 3) milliseconds, you'll run into performance issues when a lot of links have to be created. Custom implementation
 *     can best always logs at debug level how long parts of the method takes to execute (default hst logs the total
 *     time as well). Target time should be around 1/10 to 1 millisecond max.
 * </p>
 */
public interface LinkRewritePathResolver {

    /**
     * <p>
     * In general, the nodePath with which the link creation is tried is just {@link javax.jcr.Node#getPath()}, however,
     * if required differently by an end project, it can be done by implementing a custom {@link Node NodeToLinkRewritePathResolver
     * and set this on {@link HstLinkCreator} implementation
     * </p>
     * @param node the jcr node for which a {@link HstLink} is needed
     * @param context the {@link HstRequestContext} for the current request
     * @param canonical whether the link to be created was marked to be <b>canonical</b> or not.
     * @param navigationStateful whether the link to be created was marked to be <b>navigationStateful</b> or not. If
     * <code>canonical=true</code> then typically this parameter its value is ignored. Canonical normally has precedence
     * over navigationStateful
     * @return the node path with which the HstLinkCreator will try to create a {@link HstLink}. Returned path is <b>not</b>
     * allowed to be <code>null</code> and <b>must</b> start with a '/' and is not allowed to end with a '/'.
     */
    String getPath(Node node, HstRequestContext context, boolean canonical, boolean navigationStateful);
}
