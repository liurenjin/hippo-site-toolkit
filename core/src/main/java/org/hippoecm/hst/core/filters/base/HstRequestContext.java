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
package org.hippoecm.hst.core.filters.base;

import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.core.context.ContextBase;
import org.hippoecm.hst.core.filters.domain.RepositoryMapping;
import org.hippoecm.hst.core.mapping.URLMapping;
import org.hippoecm.hst.core.mapping.URLMappingManager;
import org.hippoecm.hst.core.template.node.PageNode;
import org.hippoecm.hst.core.template.node.content.ContentRewriter;

public class HstRequestContext {

    private Session jcrSession;
    private ContextBase contentContextBase;
    private ContextBase hstConfigurationContextBase;
    private URLMapping absoluteUrlMapping;
    private URLMapping relativeUrlMapping;
    private ContentRewriter contentRewriter;
    private PageNode pageNode;
    private RepositoryMapping repositoryMapping;
    private URLMappingManager urlMappingManager;
    private HttpServletRequest request;
    private String hstRequestUri;

    public HstRequestContext() {
    }
    
    public RepositoryMapping getRepositoryMapping() {
        return repositoryMapping;
    }

    public PageNode getPageNode() {
        return pageNode;   
    }
    
    public Session getJcrSession(){
        return this.jcrSession;
    }
    
    public URLMappingManager getURLMappingManager(){
        return this.urlMappingManager;
    }
    
    public URLMapping getUrlMapping() {
        // default the relative url mapping is returned
        return this.getRelativeUrlMapping();
    }
    
    public URLMapping getAbsoluteUrlMapping() {
        return absoluteUrlMapping;
    }

    public URLMapping getRelativeUrlMapping() {
        return relativeUrlMapping;
    }
    
    public ContextBase getContentContextBase() {
        return contentContextBase;
    }

    public ContextBase getHstConfigurationContextBase() {
        return hstConfigurationContextBase;
    }

    public HttpServletRequest getRequest() {
        return request;
    }
    
    public String getHstRequestUri() {
        return hstRequestUri;
    }
    
    public ContentRewriter getContentRewriter() {
        return contentRewriter;
    }
  
    public void setRepositoryMapping(RepositoryMapping repositoryMapping) {
        this.repositoryMapping = repositoryMapping;
    }
    
    public void setPageNode(PageNode pageNode) {
        this.pageNode = pageNode;
    }
    
    public void setAbsoluteUrlMapping(URLMapping absoluteUrlMapping) {
        this.absoluteUrlMapping = absoluteUrlMapping;
    }

    public void setRelativeUrlMapping(URLMapping relativeUrlMapping) {
        this.relativeUrlMapping = relativeUrlMapping;
    }

    public void setJcrSession(Session jcrSession) {
        this.jcrSession =  jcrSession;
    }
 
    public void setContentContextBase(ContextBase contentContextBase) {
        this.contentContextBase = contentContextBase;
    }

    public void setHstConfigurationContextBase(ContextBase hstConfigurationContextBase) {
        this.hstConfigurationContextBase = hstConfigurationContextBase;
    }

    public void setURLMappingManager(URLMappingManager urlMappingManager) {
        this.urlMappingManager = urlMappingManager;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    public void setHstRequestUri(String hstRequestUri) {
        this.hstRequestUri = hstRequestUri;
    }

    public void setContentRewriter(ContentRewriter contentRewriter) {
        this.contentRewriter = contentRewriter;
    }


}
