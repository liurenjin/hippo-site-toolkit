package org.hippoecm.hst.tag;

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.container.HstFilter;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManagerImpl;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.util.HstRequestUtils;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base tag support class with HST functionalities
 */
public class HstTagSupport extends TagSupport {

    private static final long serialVersionUID = 1L;
    
    protected static final Logger logger = LoggerFactory.getLogger(HstTagSupport.class);
    
    @Override
    public int doEndTag() throws JspException {

        final HttpServletRequest servletRequest = (HttpServletRequest) pageContext.getRequest();
        final HttpServletResponse servletResponse = (HttpServletResponse) pageContext.getResponse();
        final HstRequest hstRequest = HstRequestUtils.getHstRequest(servletRequest);
        final HstResponse hstResponse = HstRequestUtils.getHstResponse(servletRequest, servletResponse);
        
        if (hstRequest == null || hstResponse == null) {
            return EVAL_PAGE;
        }

        return doEndTag(hstRequest, hstResponse);
    }

    /**
     * A doEndTag hook for derived classes with HstRequest and HstResponse 
     * parameters that are never null. 
     */
    protected int doEndTag(final HstRequest hstRequest, final HstResponse hstResponse) {
        return EVAL_PAGE;
    }


    /**
     * Get the {@link Mount} for the current 
     */
    protected Mount getMount(final HstRequest request){
        return request.getRequestContext().getResolvedMount().getMount();
    }
    
    /**
     * Get the HST Site object from request.
     */
    protected HstSite getHstSite(final HstRequest request){
        return request.getRequestContext().getResolvedSiteMapItem().getHstSiteMapItem().getHstSiteMap().getSite();
    }
    
    /**
     * Is this a request in preview?
     */
    protected boolean isPreview(final HstRequest request) {
        return request.getRequestContext().isPreview();
    }
    
    /**
     * Get the default Spring configured client component manager.
     */
    protected ComponentManager getDefaultClientComponentManager() {
        ComponentManager clientComponentManager = HstFilter.getClientComponentManager(pageContext.getServletContext());
        if(clientComponentManager == null) {
            logger.warn("Cannot get a client component manager from servlet context for attribute name '{}'", HstFilter.CLIENT_COMPONENT_MANANGER_DEFAULT_CONTEXT_ATTRIBUTE_NAME);
        }
        return clientComponentManager;
    }

    /**
     * Get the site content base bean, which is the root document bean whithin 
     * preview or live context. 
     */
    protected HippoBean getSiteContentBaseBean(HstRequest request) {
        String base = getSiteContentBasePath(request);
        try {
            return (HippoBean) getObjectBeanManager(request).getObject("/"+base);
        } catch (ObjectBeanManagerException e) {
            logger.error("ObjectBeanManagerException. Return null : {}", e);
        }
        return null;
    }
    
    protected String getSiteContentBasePath(HstRequest request){
        return PathUtils.normalizePath(request.getRequestContext().getResolvedMount().getMount().getContentPath());
    }
    
    protected ObjectBeanManager getObjectBeanManager(HstRequest request) {
        try {
            HstRequestContext requestContext = request.getRequestContext();
            return new ObjectBeanManagerImpl(requestContext.getSession(), getObjectConverter());
        } catch (UnsupportedRepositoryOperationException e) {
            throw new HstComponentException(e);
        } catch (RepositoryException e) {
            throw new HstComponentException(e);
        }
    }
    
    protected ObjectConverter getObjectConverter()  {
        // get the objectconverter that was put in servlet context by HstComponent 
        return (ObjectConverter) pageContext.getServletContext().getAttribute(BaseHstComponent.OBJECT_CONVERTER_CONTEXT_ATTRIBUTE);
    }
}
