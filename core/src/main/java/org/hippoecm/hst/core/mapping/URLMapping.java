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
package org.hippoecm.hst.core.mapping;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.core.template.ContextBase;
import org.hippoecm.hst.core.template.node.PageNode;

public interface URLMapping {
    
    public String rewriteLocation(Node node);
    public String getContextPath();
    public String getContextPrefix();
    public String rewriteLocation(String path, Session jcrSession);
    public String getLocation(String path);
    public List<String> getCanonicalPathsConfiguration();
    public PageNode getMatchingPageNode(HttpServletRequest request, ContextBase contextBase);
}
