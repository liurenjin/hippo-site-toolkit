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
package org.hippoecm.hst.core.template.tag;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.hippoecm.hst.core.HSTHttpAttributes;
import org.hippoecm.hst.core.template.module.PaginatedDataBean;
import org.hippoecm.hst.core.template.module.PagingModule;
import org.hippoecm.hst.core.template.node.PageContainerModuleNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  Tag for paginated display of module data. This tag is almost identical to the module tag.
 *  The difference that a render call does not result in a call to the execute() method. Instead it
 *  gets the items of the paginated view and puts them in a bean that is made available in the
 *  pageScope.
 *   
 *
 */
public class PagingModuleTag extends ModuleTagBase {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ModuleRenderTag.class);
    
    public int doStartTag() throws JspException {       
        return EVAL_BODY_BUFFERED;
    }
    
    public int doEndTag() throws JspException {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();     
        PageContainerModuleNode pcm = null;
        if (doExecute || doRender) {
            pcm = (PageContainerModuleNode) request.getAttribute(HSTHttpAttributes.CURRENT_PAGE_MODULE_NAME_REQ_ATTRIBUTE);
        }
        if (doExecute) {
            doExecute(request, pcm);
        }
        
        if (doRender) {
           doRender(request, pcm);
        }
        return EVAL_PAGE;
    }
    
    /**
     * Renders the module but paginated.
     */
    protected final void doRender(HttpServletRequest request, PageContainerModuleNode pcm) throws JspException {       
            try {
                PagingModule pagingModule = getPageModule();              
                pagingModule.setVar(var);
                pagingModule.setPageModuleNode(getPageModuleNode(request, pcm.getName()));
                pagingModule.setModuleParameters(parameters);             
                
                pagingModule.prePagingRender(pageContext);
                
                int pageSize = pagingModule.getPageSize(pageContext);
                int pageNo = pagingModule.getPageNumber(pageContext);
                int totalItems = pagingModule.totalItems();
                int from = (pageNo == 0 ? 0 : (pageNo * pageSize));
                int to = ((pageNo + 1) * pageSize);
                to = (to > totalItems) ? totalItems : to;
                
                if (log.isDebugEnabled()) {
                    log.debug("paginated pagesize" + pageSize);
                    log.debug("current page number" + pageNo);
                    log.debug("total result" + totalItems);
                    log.debug("current page displays items[" + from + ".." + to + "]");
                }
                
                PaginatedDataBean paginatedData = new PaginatedDataBean();
                paginatedData.setItems(pagingModule.getElements(from, to));
                paginatedData.setTotalItems(totalItems);
                paginatedData.setPageSize(pageSize);
                paginatedData.setPageNo(pageNo);
                
                try {
                    String pageParameter = pagingModule.getPageParameter();
                    paginatedData.setPageParameter(pageParameter);
                } catch (Exception e) {
                    log.warn("PageParameter not available");
                }
                
                log.debug("paginatedData: " + paginatedData);
                pageContext.setAttribute(var, paginatedData);
                } catch (Exception e) {
                    throw new JspException(e);
                }
    }
    
    protected final PagingModule getPageModule() throws Exception {
        Object o = null;
        log.info("Create instance of class " + getClassName());
        o = Class.forName(getClassName()).newInstance();
        if (!PagingModule.class.isInstance(o)) {
            throw new Exception(getClassName() + " does not implement the interface " + PagingModule.class.getName());
        }
        return (PagingModule) o;
    }
    
    
    protected int getPageId(HttpServletRequest request, String pageRequestParameter) {      
        int pageId = 0;
        try {
            pageId = Integer.parseInt(request.getParameter(pageRequestParameter));
        } catch (NumberFormatException e) {             
            log.error("Cannot parse requestParameter " + pageRequestParameter + " value=" + request.getParameter(pageRequestParameter));
            pageId = 0;
        }
        return pageId;
    }
   
}
