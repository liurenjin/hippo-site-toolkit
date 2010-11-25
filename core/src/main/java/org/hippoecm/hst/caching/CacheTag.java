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
package org.hippoecm.hst.caching;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.hippoecm.hst.caching.validity.ExpiresValidity;
import org.hippoecm.hst.core.filters.base.HstRequestContext;

public class CacheTag extends BodyTagSupport {

    private static final long serialVersionUID = 1L;
    
    private String nameExpr;  // tag attribute
    private CacheKey key; // parsed tag attribute
    private int ttl; // parsed tag attribute

    private transient Cache cache;   // cache
    private CachedResponse cachedResponse;
    private static final int TTL = 60; // time to live seconds 
    private boolean includeParams;
    private HstRequestContext hstRequestContext;
    
    @Override
    public int doStartTag() throws JspException {
        
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        hstRequestContext = (HstRequestContext) request.getAttribute(HstRequestContext.class
                .getName());
       
        if(hstRequestContext == null) {
            // log.warn("hstRequestContext is null");
             return SKIP_BODY;
         }
        
        if(hstRequestContext.isPreview()) {
            // log.warn("hstRequestContext is null");
             return EVAL_BODY_BUFFERED;
         }
        
        StringBuilder keyBuilder = new StringBuilder(hstRequestContext.getRepositoryMapping().getContentPath());
        keyBuilder.append("_").append(hstRequestContext.getHstRequestUri());
        
        if(this.includeParams) {
            Enumeration params = request.getParameterNames();
            while(params.hasMoreElements()) {
                String param = (String)params.nextElement();
                keyBuilder.append("_").append(param).append(":").append(request.getParameter(param));
            }
        }
        
        keyBuilder.append("_").append(nameExpr);
        
        key = new CacheKey(keyBuilder.toString(), CacheTag.class);

        this.cache = CacheManagerImpl.getCache(hstRequestContext.getJcrSession().getUserID());
        this.cachedResponse = this.cache.get(this.key);
        if (this.cachedResponse != null) {
            return SKIP_BODY;
        } else {
            return EVAL_BODY_BUFFERED;
        }
    }

    @Override
    public int doEndTag() throws JspException {
        if(ttl == 0) {
            ttl = TTL;
        }
        try {
            if(hstRequestContext.isPreview()) {
                pageContext.getOut().write(bodyContent.getString().trim());
                return EVAL_PAGE;
            }
            ttl = hstRequestContext.getCacheExpiresMinutes()*ttl;
            String body = null;
            if (this.cachedResponse == null) {
                if (bodyContent == null || bodyContent.getString() == null) {
                    body = "";
                } else {
                    body = bodyContent.getString().trim();
                }
                CachedResponse newCachedResponse = new CachedResponseImpl(new ExpiresValidity(ttl*1000), body);
                this.cache.store(this.key, newCachedResponse);
            } else {
                body = (String)this.cachedResponse.getResponse();
            }
            pageContext.getOut().write(body);
        } catch (IOException ex) {
            throw new JspException(ex);
        }
        this.cache =null;
        this.cachedResponse = null;
        this.ttl = 0;
        this.key = null;
        this.nameExpr = null;
        this.includeParams = false;
        this.hstRequestContext = null;
        
        return EVAL_PAGE;
    }


    public void setName(String nameExpr) {
        this.nameExpr = nameExpr;
    }
    public void setTtl(int ttl) {
        this.ttl = ttl;
    }
    
    public void setParams(boolean include) {
        this.includeParams = include;
    }


}
