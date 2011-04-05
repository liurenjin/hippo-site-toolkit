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
package org.hippoecm.hst.core.container;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.util.HstRequestUtils;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default implementation providing HstContainerURL.
 * This implementation assume the urls are like the following examples:
 * <P>
 * <code><xmp>
 * 1) render url will not be encoded : http://localhost/site/content/news/2008/08
 * 2) action url will be encoded     : http://localhost/site/content/_hn:<encoded_params>/news/2008/08
 *          the <encoded_params>     : <request_type>|<action reference namespace>
 * 3) resource url will be encoded   : http://localhsot/site/content/_hn:<encoded_params>/news/2008/08
 *          the <encoded_params>     : <request_type>|<resource reference namespace>|<resource ID>
 * </xmp></code> 
 * </P>
 * 
 * @version $Id$
 */
public class HstContainerURLProviderImpl implements HstContainerURLProvider {
    
    protected final static Logger log = LoggerFactory.getLogger(HstContainerURLProvider.class);
    
    protected static final String REQUEST_INFO_SEPARATOR = "|";

    protected static final String DEFAULT_HST_URL_NAMESPACE_PREFIX = "_hn:";

    protected String urlNamespacePrefix = DEFAULT_HST_URL_NAMESPACE_PREFIX;
    protected String urlNamespacePrefixedPath = "/" + urlNamespacePrefix;
    protected String parameterNameComponentSeparator = ":";
    
    protected HstNavigationalStateCodec navigationalStateCodec;
    
    protected boolean portletResourceURLEnabled;
    
    protected HstPortletContainerURLWriter portletContainerURLWriter;
    protected HstEmbeddedPortletContainerURLWriter embeddedPortletContainerURLWriter;
    
    public void setPortletResourceURLEnabled(boolean portletResourceURLEnabled) {
        this.portletResourceURLEnabled = portletResourceURLEnabled;
    }
    
    public boolean isPortletResourceURLEnabled() {
    	return portletResourceURLEnabled;
    }
    
    public void setUrlNamespacePrefix(String urlNamespacePrefix) {
        this.urlNamespacePrefix = urlNamespacePrefix;
        this.urlNamespacePrefixedPath = "/" + urlNamespacePrefix;
    }
    
    public String getUrlNamespacePrefix() {
        return this.urlNamespacePrefix;
    }
    
    public void setParameterNameComponentSeparator(String parameterNameComponentSeparator) {
        this.parameterNameComponentSeparator = parameterNameComponentSeparator;
    }
    
    public String getParameterNameComponentSeparator() {
        return this.parameterNameComponentSeparator;
    }
    
    public void setNavigationalStateCodec(HstNavigationalStateCodec navigationalStateCodec) {
        this.navigationalStateCodec = navigationalStateCodec;
    }
    
    public HstNavigationalStateCodec getNavigationalStateCodec() {
        return this.navigationalStateCodec;
    }

    public HstContainerURL parseURL(HttpServletRequest request, ResolvedMount mount, String requestPath) {
        HstContainerURLImpl url = new HstContainerURLImpl();
        url.setContextPath(request.getContextPath());
        url.setHostName(mount.getResolvedVirtualHost().getResolvedHostName());
        url.setPortNumber(mount.getResolvedVirtualHost().getPortNumber());
    	url.setResolvedMountPath(mount.getResolvedMountPath());
        url.setRequestPath(requestPath);
        url.setPathInfo(requestPath.substring(mount.getResolvedMountPath().length()));
        String characterEncoding = request.getCharacterEncoding();
        if (characterEncoding == null) {
            characterEncoding = "ISO-8859-1";
        }
        url.setCharacterEncoding(characterEncoding);
        return url;
    }
    
    public HstContainerURL parseURL(HstRequestContext requestContext, ResolvedMount mount, String requestPath) {
        HstContainerURLImpl url = new HstContainerURLImpl();
        HstContainerURL baseURL = requestContext.getBaseURL();
        url.setContextPath(baseURL.getContextPath());
        url.setHostName(baseURL.getHostName());
        url.setPortNumber(baseURL.getPortNumber());
    	url.setResolvedMountPath(mount.getResolvedMountPath());
        url.setRequestPath(requestPath);
        String [] namespacedPartAndPathInfo = splitPathInfo(requestPath.substring(mount.getResolvedMountPath().length()), baseURL.getCharacterEncoding());
        url.setPathInfo(namespacedPartAndPathInfo[1]);
        parseRequestInfo(url,namespacedPartAndPathInfo[0]);
        url.setCharacterEncoding(baseURL.getCharacterEncoding());
        return url;
    }
    
    public HstContainerURL parseURL(HttpServletRequest request, HttpServletResponse response, ResolvedMount resolvedMount) {

        HstContainerURLImpl url = new HstContainerURLImpl();
        
        url.setContextPath(request.getContextPath());
        url.setHostName(HstRequestUtils.getFarthestRequestHost(request));
        url.setPortNumber(HstRequestUtils.getRequestServerPort(request));
        url.setRequestPath(HstRequestUtils.getRequestPath(request));
        
        String characterEncoding = request.getCharacterEncoding();
        
        if (characterEncoding == null) {
            characterEncoding = "ISO-8859-1";
        }
        
        url.setCharacterEncoding(characterEncoding);

        Map<String, String []> paramMap = HstRequestUtils.parseQueryString(request);
        url.setParameters(paramMap);
        
        url.setResolvedMountPath(resolvedMount.getResolvedMountPath());

        String [] namespacedPartAndPathInfo = splitPathInfo(resolvedMount, request, characterEncoding);
        url.setPathInfo(namespacedPartAndPathInfo[1]);
        parseRequestInfo(url,namespacedPartAndPathInfo[0]);
        
        return url;
    }
    
    public HstContainerURL createURL(HstContainerURL baseContainerURL, String pathInfo) {
        HstContainerURLImpl url = new HstContainerURLImpl();
        url.setContextPath(baseContainerURL.getContextPath());
        url.setHostName(baseContainerURL.getHostName());
        url.setPortNumber(baseContainerURL.getPortNumber());
        pathInfo = PathUtils.normalizePath(pathInfo);
        if (pathInfo != null) {
        	pathInfo = "/" + pathInfo;
        }
        url.setRequestPath(baseContainerURL.getResolvedMountPath()+pathInfo);
        url.setCharacterEncoding(baseContainerURL.getCharacterEncoding());
        url.setResolvedMountPath(baseContainerURL.getResolvedMountPath());
        url.setPathInfo(pathInfo);
        
        return url;
    }
    
    public HstContainerURL createURL(Mount mount ,HstContainerURL baseContainerURL, String pathInfo) {
        HstContainerURLImpl url = new HstContainerURLImpl();
        
        url.setContextPath(baseContainerURL.getContextPath());
        url.setCharacterEncoding(baseContainerURL.getCharacterEncoding());
        
        // if the Mount is port agnostic, in other words, has port = 0, we take the port from the baseContainerURL
        if(mount.getPort() == 0) {
            url.setPortNumber(baseContainerURL.getPortNumber());
        } else {
            url.setPortNumber(mount.getPort());
        }
        
        boolean includeTrailingSlash = false;
        if(pathInfo != null && pathInfo.endsWith(HstLink.PATH_SUBPATH_DELIMITER)) {
            // we now must not strip the trailing slash because it is part of the rest call ./
            includeTrailingSlash = true; 
        }
        
        pathInfo = PathUtils.normalizePath(pathInfo);
        if (pathInfo != null) {
            pathInfo = "/" + pathInfo;
            if(includeTrailingSlash) {
                pathInfo = pathInfo + "/";
            }
        }

        url.setHostName(mount.getVirtualHost().getHostName());
        url.setRequestPath(mount.getMountPath() + pathInfo);
        url.setResolvedMountPath(mount.getMountPath());
        url.setPathInfo(pathInfo);
        
        return url;
    }
    
    public HstContainerURL createURL(HstContainerURL baseContainerURL, HstURL hstUrl) {
        HstContainerURLImpl containerURL = null;
        
        try {
            containerURL = (HstContainerURLImpl) ((HstContainerURLImpl) baseContainerURL).clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Clone not supported on the container url. " + e);
        }
        
        containerURL.setActionWindowReferenceNamespace(null);
        containerURL.setResourceWindowReferenceNamespace(null);
        
        String type = hstUrl.getType();
        
        if (HstURL.ACTION_TYPE.equals(type)) {
            containerURL.setActionWindowReferenceNamespace(hstUrl.getReferenceNamespace());
            mergeParameters(containerURL, hstUrl.getReferenceNamespace(), hstUrl.getParameterMap());
        } else if (HstURL.RESOURCE_TYPE.equals(type)) {
            containerURL.setResourceWindowReferenceNamespace(hstUrl.getReferenceNamespace());
            containerURL.setResourceId(hstUrl.getResourceID());
            mergeParameters(containerURL, hstUrl.getReferenceNamespace(), hstUrl.getParameterMap());
        } else {
            mergeParameters(containerURL, hstUrl.getReferenceNamespace(), hstUrl.getParameterMap());
        }
        
        return containerURL;
    }
    
    public void mergeParameters(HstContainerURL containerURL, String referenceNamespace, Map<String, String []> parameterMap) {
        String prefix = (StringUtils.isEmpty(referenceNamespace) ? "" : referenceNamespace + parameterNameComponentSeparator);

        // first drop existing referenceNamespace parameters
        if (!"".equals(prefix)) {
            Map<String, String []> containerParamsMap = containerURL.getParameterMap();
            if (containerParamsMap != null && !containerParamsMap.isEmpty()) {
                int prefixLen = prefix.length();
                String paramName = null;
                
                for (Iterator<Map.Entry<String, String[]>> iter = containerParamsMap.entrySet().iterator(); iter.hasNext(); ) {
                    paramName = iter.next().getKey();
                    if (paramName.startsWith(prefix) && paramName.length() > prefixLen) {
                        iter.remove();
                    }
                }
            }
        }
        
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String paramName = prefix + entry.getKey();
            String [] paramValues = entry.getValue();
            
            if (paramValues == null || paramValues.length == 0) {
                containerURL.setParameter(paramName, (String) null);
            } else {
                containerURL.setParameter(paramName, paramValues);
            }
        }
    }
    
    public String toContextRelativeURLString(HstContainerURL containerURL, HstRequestContext requestContext) throws UnsupportedEncodingException, ContainerException {
        StringBuilder url = new StringBuilder(100);
        String mountPrefix = requestContext.isEmbeddedRequest() ? requestContext.getResolvedEmbeddingMount().getResolvedMountPath() : containerURL.getResolvedMountPath();
        if(mountPrefix != null) {
            url.append(mountPrefix);
        }
        String pathInfo = buildHstURLPath(containerURL);
        url.append(pathInfo);
        return url.toString();
    }
    
    protected HstPortletContainerURLWriter getPortletContainerURLWriter() {
    	if (this.portletContainerURLWriter == null) {
    		this.portletContainerURLWriter = new HstPortletContainerURLWriter();
    	}
    	return this.portletContainerURLWriter;
    }
    
    protected HstEmbeddedPortletContainerURLWriter getEmbeddedPortletContainerURLWriter() {
    	if (this.embeddedPortletContainerURLWriter == null) {
    		this.embeddedPortletContainerURLWriter = new HstEmbeddedPortletContainerURLWriter();
    	}
    	return this.embeddedPortletContainerURLWriter;
    }
    
    protected String buildHstURLPath(HstContainerURL containerURL) throws UnsupportedEncodingException {
        String characterEncoding = containerURL.getCharacterEncoding();
        StringBuilder url = new StringBuilder(100);
        
        if (containerURL.getActionWindowReferenceNamespace() != null) {
            url.append(this.urlNamespacePrefixedPath);
            
            StringBuilder sbRequestInfo = new StringBuilder(80);
            sbRequestInfo.append(HstURL.ACTION_TYPE).append(REQUEST_INFO_SEPARATOR);
            sbRequestInfo.append(containerURL.getActionWindowReferenceNamespace()).append(REQUEST_INFO_SEPARATOR);
            
            url.append(URLEncoder.encode(sbRequestInfo.toString(), characterEncoding));
        } else if (containerURL.getResourceWindowReferenceNamespace() != null) {
            url.append(this.urlNamespacePrefixedPath);
            
            String resourceId = containerURL.getResourceId();
            // resource id should be double-encoded because it can have slashes,
            // which can make a problem in Tomcat 6.
            if (resourceId != null) {
                resourceId = URLEncoder.encode(resourceId, characterEncoding);
            }
            
            String requestInfo = 
                HstURL.RESOURCE_TYPE + REQUEST_INFO_SEPARATOR + 
                containerURL.getResourceWindowReferenceNamespace() + REQUEST_INFO_SEPARATOR + 
                (resourceId != null ? resourceId : "");
            
            url.append(URLEncoder.encode(requestInfo, characterEncoding));
        }
        
        String[] unEncodedPaths = containerURL.getPathInfo().split("/");
        for(String path : unEncodedPaths) {
            if(!"".equals(path)) {
                url.append("/").append(URLEncoder.encode(path, characterEncoding));
            }
        }
        if(containerURL.getPathInfo().endsWith(HstLink.PATH_SUBPATH_DELIMITER)) {
            // the trailing slash is removed above, but for ./ we need to append the slash again
            url.append("/");
        }
        
        boolean firstParamDone = (url.indexOf("?") >= 0);
        
        Map<String, String []> parameters = containerURL.getParameterMap();
        
        if (parameters != null) {
            for (Map.Entry<String, String[]> entry : parameters.entrySet()) {
                String name = entry.getKey();
                
                for (String value : entry.getValue()) {
                    url.append(firstParamDone ? "&" : "?")
                    .append(name)
                    .append("=")
                    .append(URLEncoder.encode(value, characterEncoding));
                    
                    firstParamDone = true;
                }
            }
        }
        
        return url.toString();
    }
    
    protected void parseRequestInfo(HstContainerURL url, String encodedRequestInfo) {
        try {
            if (encodedRequestInfo != null) {
                String encodedInfo = encodedRequestInfo.substring(urlNamespacePrefixedPath.length());
                String [] requestInfos = StringUtils.splitByWholeSeparatorPreserveAllTokens(encodedInfo, REQUEST_INFO_SEPARATOR, 3);
                
                String requestType = requestInfos[0];
                
                if (HstURL.ACTION_TYPE.equals(requestType)) {
                    String actionWindowReferenceNamespace = requestInfos[1];
                    url.setActionWindowReferenceNamespace(actionWindowReferenceNamespace);
                    
                    if (log.isDebugEnabled()) {
                        log.debug("action window chosen for {}: {}", url.getPathInfo(), actionWindowReferenceNamespace);
                    }
                } else if (HstURL.RESOURCE_TYPE.equals(requestType)) {
                    String resourceWindowReferenceNamespace = requestInfos[1];
                    String resourceId = requestInfos.length > 2 ? requestInfos[2] : null;
                    
                    if (resourceId != null) {
                        // resource id is double-encoded because it can have slashes,
                        // which can make a problem in Tomcat 6.
                        resourceId = URLDecoder.decode(resourceId, url.getCharacterEncoding());
                    }
                    
                    url.setResourceId(resourceId);
                    url.setResourceWindowReferenceNamespace(resourceWindowReferenceNamespace);
                    
                    if (log.isDebugEnabled()) {
                        log.debug("resource window chosen for {}: {}", url.getPathInfo(), resourceWindowReferenceNamespace + ", " + resourceId);
                    }
                }
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Invalid container URL path: {}", url.getRequestPath(), e);
            } else if (log.isWarnEnabled()) {
                log.warn("Invalid container URL path: {}", url.getRequestPath());
            }
        }
        
    }
  
    /*
     * Splits path info to an array of namespaced path part and remainder. 
     */
    protected String [] splitPathInfo(String requestPath, String characterEncoding) {
       
    	String pathInfo = requestPath;
    	try {
    		pathInfo = URLDecoder.decode(requestPath, characterEncoding);
    	}
    	catch (UnsupportedEncodingException e) {
    		if (log.isDebugEnabled()) {
                log.warn("Invalid request path: {}", requestPath, e);
    		}
    	}
        if (!pathInfo.startsWith(urlNamespacePrefixedPath)) {
            return new String [] { null, pathInfo };
        }
        
        String temp = requestPath.substring(requestPath.indexOf(urlNamespacePrefixedPath));
        int offset = temp.indexOf('/', 1);
        String namespacedPathPart = temp.substring(0, offset);
        pathInfo = "";

        try {
            namespacedPathPart = URLDecoder.decode(namespacedPathPart, characterEncoding);
            pathInfo = URLDecoder.decode(temp.substring(offset), characterEncoding);
        } catch (UnsupportedEncodingException e) {
            if (log.isDebugEnabled()) {
                log.warn("Invalid request uri: {}", requestPath, e);
            } else if (log.isWarnEnabled()) {
                log.warn("Invalid request uri: {}", requestPath);
            }
        }        
        return new String [] { namespacedPathPart, pathInfo };
    }
    
    /*
     * Splits path info to an array of namespaced path part and remainder. 
     */
    protected String [] splitPathInfo(ResolvedMount resolvedMount, HttpServletRequest request, String characterEncoding) {
       
        String pathInfo = HstRequestUtils.getPathInfo(resolvedMount, request, characterEncoding);
        
        if (!pathInfo.startsWith(urlNamespacePrefixedPath)) {
            return new String [] { null, pathInfo };
        }
        
        String requestURI = (String) request.getAttribute("javax.servlet.include.request_uri");
        
        if (requestURI == null) {
            requestURI = HstRequestUtils.getRequestURI(request, true);
        }
        
        String namespacedPathPart = "";
        String pathInfoFromURI = "";
        
        String temp = requestURI.substring(requestURI.indexOf(urlNamespacePrefixedPath));
        int offset = temp.indexOf('/', 1);
        if (offset != -1) {
            namespacedPathPart = temp.substring(0, offset);
            pathInfoFromURI = temp.substring(offset);
        } else {
            namespacedPathPart = temp;
        }
        
        try {
            namespacedPathPart = URLDecoder.decode(namespacedPathPart, characterEncoding);
            pathInfoFromURI = URLDecoder.decode(pathInfoFromURI, characterEncoding);
        } catch (UnsupportedEncodingException e) {
            if (log.isDebugEnabled()) {
                log.warn("Invalid request uri: {}", requestURI, e);
            } else if (log.isWarnEnabled()) {
                log.warn("Invalid request uri: {}", requestURI);
            }
        }
        
        return new String [] { namespacedPathPart, pathInfoFromURI };
    }
	
    public String toURLString(HstContainerURL containerURL, HstRequestContext requestContext) throws UnsupportedEncodingException, ContainerException {
        return toURLString(containerURL, requestContext, null);
    }
    
    public String toURLString(HstContainerURL containerURL, HstRequestContext requestContext, String contextPath) throws UnsupportedEncodingException, ContainerException {
    	if (requestContext.isPortletContext()) {
    		if (requestContext.isEmbeddedRequest()) {
    			return getEmbeddedPortletContainerURLWriter().toURLString(this, containerURL, requestContext, contextPath);
    		}
    		else {
    			return getPortletContainerURLWriter().toURLString(this, containerURL, requestContext, contextPath);
    		}
    	}
    	
        StringBuilder urlBuilder = new StringBuilder(100);
        if(contextPath != null) {
            urlBuilder.append(contextPath);
        } else if (requestContext.getResolvedMount().getMount().isContextPathInUrl()) {
            urlBuilder.append(containerURL.getContextPath());
        }
        
        String resourceWindowReferenceNamespace = containerURL.getResourceWindowReferenceNamespace();
        String path = null;
        
        if (ContainerConstants.CONTAINER_REFERENCE_NAMESPACE.equals(resourceWindowReferenceNamespace)) {
            String oldPathInfo = containerURL.getPathInfo();
            String resourcePath = containerURL.getResourceId();
            Map<String, String[]> oldParamMap = containerURL.getParameterMap();

            try {
                containerURL.setResourceWindowReferenceNamespace(null);
                ((HstContainerURLImpl) containerURL).setPathInfo(resourcePath);
                ((HstContainerURLImpl) containerURL).setParameters(null);
                path = buildHstURLPath(containerURL);
            } finally {
                containerURL.setResourceWindowReferenceNamespace(resourceWindowReferenceNamespace);
                ((HstContainerURLImpl) containerURL).setPathInfo(oldPathInfo);
                ((HstContainerURLImpl) containerURL).setParameters(oldParamMap);
            }
        } else {
            urlBuilder.append(containerURL.getResolvedMountPath());
            path = buildHstURLPath(containerURL);
        }
        
        urlBuilder.append(path);
        
        return urlBuilder.toString();
    }
}
