package org.hippoecm.hst.core.filters.urlmapping;

import java.io.IOException;

import javax.jcr.Session;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.HSTHttpAttributes;
import org.hippoecm.hst.core.filters.base.HstBaseFilter;
import org.hippoecm.hst.core.filters.base.HstRequestContext;
import org.hippoecm.hst.core.mapping.RelativeURLMappingImpl;
import org.hippoecm.hst.core.mapping.URLMapping;
import org.hippoecm.hst.core.mapping.URLMappingException;
import org.hippoecm.hst.core.mapping.URLMappingManager;
import org.hippoecm.hst.core.mapping.URLMappingManagerImpl;
import org.hippoecm.hst.core.template.node.content.SimpleContentRewriterImpl;
import org.hippoecm.hst.jcr.JcrSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter that set the URLMapping the hstRequest
 * 
 */
public class URLMappingFilter extends HstBaseFilter implements Filter{
   
    private static final Logger log = LoggerFactory.getLogger(URLMappingFilter.class);
 
    private URLMappingManager urlMappingManager;
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.urlMappingManager = new URLMappingManagerImpl(new JcrSessionFactory(filterConfig));
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        super.doFilter(request, response, chain);
    }
    
    @Override
    public void handleRequestForThisFilter(HttpServletRequest request, ServletResponse res, FilterChain chain, HstRequestContext hstRequestContext)
            throws IOException, ServletException {
        
        if( hstRequestContext == null) {
            log.warn("hstRequestContext is null. Cannot process filter");
            chain.doFilter(request, res);
            return;
        }
        
        hstRequestContext.setURLMappingManager(this.urlMappingManager);
        
        Session session = hstRequestContext.getJcrSession();
        HttpServletResponse response = (HttpServletResponse)res;
        
        if(session == null) {
            log.warn("No jcr session available for request. Cannot process request");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        
        URLMapping urlMapping = null;
        try {
            urlMapping = this.urlMappingManager.getUrlMapping(hstRequestContext);
        } catch (URLMappingException e) {
            log.error("Failure initialize urlMapping. Request cannot be processed. {}", e.getMessage());
            ((HttpServletResponse)response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        hstRequestContext.setAbsoluteUrlMapping(urlMapping);
        URLMapping relativeURLMapping = new RelativeURLMappingImpl(hstRequestContext.getRequest().getRequestURI(), urlMapping);
        
        hstRequestContext.setRelativeUrlMapping(relativeURLMapping);
        
        // now we have a urlMapping obj, also set the ContentRewriter on the HstRequestContext
        hstRequestContext.setContentRewriter(new SimpleContentRewriterImpl(hstRequestContext));
        
        request.setAttribute(HSTHttpAttributes.URL_MAPPING_ATTR, relativeURLMapping);
        chain.doFilter(request, response);
    }

}
