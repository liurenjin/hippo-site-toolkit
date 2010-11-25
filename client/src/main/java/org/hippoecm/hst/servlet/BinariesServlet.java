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
package org.hippoecm.hst.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;

import javax.jcr.Credentials;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.configuration.HstSitesManager;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.PathUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BinariesServlet extends HttpServlet {

    public static final String BASE_BINARIES_CONTENT_PATH_INIT_PARAM = "baseBinariesContentPath";
    public static final String PRIMARYITEM_INIT_PARAM = "primaryitem";
    public static final String DEFAULT_BASE_BINARIES_CONTENT_PATH = "";

    private static final long serialVersionUID = 1L;
    
    private static Logger log = LoggerFactory.getLogger(BinariesServlet.class);
    
    protected Repository repository;
    protected Credentials defaultCredentials;
    
    protected HstSitesManager hstSitesManager;
    
    protected String baseBinariesContentPath = DEFAULT_BASE_BINARIES_CONTENT_PATH;

    protected String primaryItem;
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        
        String param = config.getInitParameter(BASE_BINARIES_CONTENT_PATH_INIT_PARAM);
        
        if (param != null) {
            this.baseBinariesContentPath = param;
        }
        
        primaryItem = config.getInitParameter(PRIMARYITEM_INIT_PARAM);
        
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String relPath = getResourceRelPath(request);
        String resourcePath = null;
        Session session = null;
        
        try {
            String baseContentPath = this.baseBinariesContentPath;
            StringBuilder resourcePathBuilder = new StringBuilder(80);
            
            if (baseContentPath != null && !"".equals(baseContentPath)) {
                resourcePathBuilder.append('/').append(PathUtils.normalizePath(baseContentPath));
            }
            
            if (relPath != null) {
                resourcePathBuilder.append('/').append(PathUtils.normalizePath(relPath));
            }
            
            resourcePath = resourcePathBuilder.toString();
            
            session = getSession(request);
            Item item = null;
            
            if (resourcePath != null) {
                item = session.getItem(resourcePath);
            }

            if (item == null) {
                if (log.isWarnEnabled()) {
                    log.warn("item at path " + resourcePath + " not found, response status = 404)");
                }
                
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            if (!item.isNode()) {
                if (log.isWarnEnabled()) {
                    log.warn("item at path " + resourcePath + " is not a node, response status = 415)");
                }
                
                response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                return;
            }

            Node node = (Node) item;
            if(node.isNodeType(HippoNodeType.NT_HANDLE)) {
                try {
                node = node.getNode(node.getName());
                } catch(ItemNotFoundException e) {
                    log.warn("Cannot return binary for a handle with no hippo document. Return");
                    response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                    return;
                }
            }
            
            if (node.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                try {
                    if(primaryItem != null && node.hasNode(primaryItem)) {
                        node = node.getNode(primaryItem);
                    } else {
                        // fallback to the jcr primaryitem as we do not have a specific resource pointed at, and do not have
                        // a primary item configured in the web.xml, or a primary item that does not exist
                        log.debug("Show jcr primaryitem for resource at '{}'", node.getPath());
                        Item resource = node.getPrimaryItem();
                        if (resource.isNode() && ((Node) resource).isNodeType(HippoNodeType.NT_RESOURCE)) {
                            node = (Node) resource;
                        } else {
                            if (log.isWarnEnabled()) {
                                log.warn("expected a hippo:resource node as primary item.");
                            }
                        }
                    }
                } catch (ItemNotFoundException e) {
                    if (log.isWarnEnabled()) {
                        log.warn("No primary item found for binary");
                    }
                }

            }

            if (!node.hasProperty("jcr:mimeType")) {
                if (log.isWarnEnabled()) {
                    log.warn("item at path " + resourcePath + " has no property jcr:mimeType, response status = 415)");
                }
                
                response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                return;
            }

            String mimeType = node.getProperty("jcr:mimeType").getString();

            if (!node.hasProperty("jcr:data")) {
                if (log.isWarnEnabled()) {
                    log.warn("item at path " + resourcePath + " has no property jcr:data, response status = 404)");
                }
                
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            Property data = node.getProperty("jcr:data");
            InputStream istream = data.getStream();

            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(mimeType);

            // TODO add a configurable factor + default minimum for expires. Ideally, this value is
            // stored in the repository
            if (node.hasProperty("jcr:lastModified")) {
                long lastModified = 0;
                
                try {
                    lastModified = node.getProperty("jcr:lastModified").getDate().getTimeInMillis();
                } catch (ValueFormatException e) {
                    if (log.isWarnEnabled()) {
                        log.warn("jcr:lastModified not of type Date");
                    }
                }
                
                long expires = 0;
                
                if(lastModified > 0) {
                    expires = (System.currentTimeMillis() - lastModified);
                }
                
                response.setDateHeader("Expires", expires + System.currentTimeMillis());
                response.setDateHeader("Last-Modified", lastModified); 
                response.setHeader("Cache-Control", "max-age="+(expires/1000));
            } else {
                response.setDateHeader("Expires", 0);
                response.setHeader("Cache-Control", "max-age=0");
            }
            
            
            OutputStream ostream = response.getOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = istream.read(buffer)) >= 0) {
                ostream.write(buffer, 0, len);
            }
        } catch (PathNotFoundException ex) {
            if (log.isWarnEnabled()) {
                log.warn("PathNotFoundException with message " + ex.getMessage() + " while getting binary data stream item "
                        + "at path " + resourcePath + ", response status = 404)");
            }
            
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } catch (RepositoryException ex) {
            if (log.isWarnEnabled()) {
                log.warn("Repository exception while resolving binaries request '" + request.getRequestURI() + "' : " + ex.getMessage());
            }
            
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            if (session != null) {
                releaseSession(request, session);
            }
        }
    }
    
    private Session getSession(HttpServletRequest request) throws LoginException, RepositoryException {
        Session session = null;
        
        // if hstRequest is retrieved, then this servlet has been dispatched by hst component.
        HstRequest hstRequest = (HstRequest) request.getAttribute(ContainerConstants.HST_REQUEST);
        
        if (hstRequest == null && request instanceof HstRequest) {
            hstRequest = (HstRequest) request;
        }
        
        if (hstRequest != null) {
            session = hstRequest.getRequestContext().getSession();
        } else {
            if (this.repository == null) {
                if (HstServices.isAvailable()) {
                    this.defaultCredentials = HstServices.getComponentManager().getComponent(Credentials.class.getName() + ".default");
                    this.repository = HstServices.getComponentManager().getComponent(Repository.class.getName());
                }
            }

            if (this.repository != null) {
                if (this.defaultCredentials != null) {
                    session = this.repository.login(this.defaultCredentials);
                } else {
                    session = this.repository.login();
                }
            }
        }
        
        return session;
    }
    
    private void releaseSession(HttpServletRequest request, Session session) {
        // if hstRequest is retrieved, then this servlet has been dispatched by hst component.
        HstRequest hstRequest = (HstRequest) request.getAttribute(ContainerConstants.HST_REQUEST);
        
        if (hstRequest == null && request instanceof HstRequest) {
            hstRequest = (HstRequest) request;
        }

        if (hstRequest == null) {
            try {
                session.logout();
            } catch (Exception e) {
            }
        }
    }
    
    private String getResourceRelPath(HttpServletRequest request) {
        String path = null;
        
        // if hstRequest is retrieved, then this servlet has been dispatched by hst component.
        HstRequest hstRequest = (HstRequest) request.getAttribute(ContainerConstants.HST_REQUEST);
        
        if (hstRequest == null && request instanceof HstRequest) {
            hstRequest = (HstRequest) request;
        }
        
        if (hstRequest != null) {
            path = hstRequest.getResourceID();
        }

        if (path == null) {
            path = request.getPathInfo();
            
            try {
                String characterEncoding = request.getCharacterEncoding();
                
                if (characterEncoding == null) {
                    characterEncoding = "ISO-8859-1";
                }

                path = URLDecoder.decode(path, characterEncoding);
            } catch (Exception e) {
                if (log.isWarnEnabled()) {
                    log.warn("Cannot decode path: {}. {}", path, e.toString());
                }
            }
        }

        if (path != null && !path.startsWith("/") && path.indexOf(':') > 0) {
            path = path.substring(path.indexOf(':') + 1);
        }
        
        return path;
    }
    
  
}
