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
package org.hippoecm.hst.core.linking;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstLinkImpl implements HstLink {

    private final static Logger log = LoggerFactory.getLogger(HstLinkImpl.class);

    private String path;
    private String subPath;
    private Mount mount;
    private boolean containerResource;
    private boolean notFound = false;
    
    
    public HstLinkImpl(String path, Mount mount) {
        this(path, mount,false);
    }
    
    public HstLinkImpl(String path, Mount mount, boolean containerResource) {
        this.path = PathUtils.normalizePath(path);
        this.mount = mount;
        this.containerResource = containerResource;
    }
    
    public Mount getMount() {
        return mount;
    }
    
    public String getPath() {
        return this.path;
    }
    
    public void setPath(String path) {
        this.path = PathUtils.normalizePath(path);
    }
    

    public String getSubPath() {
        return subPath;
    }

    public void setSubPath(String subPath) {
        this.subPath = subPath;
    }

    
    public boolean getContainerResource() {
        return this.containerResource;
    }

    public void setContainerResource(boolean containerResource) {
       this.containerResource = containerResource;
    }
    
    public String[] getPathElements() {
        if(this.path == null) {
            return null;
        }
        return this.path.split("/");
    }


    public String toUrlForm(HstRequestContext requestContext, boolean external) {
        String characterEncoding = requestContext.getBaseURL().getCharacterEncoding();
        if (characterEncoding == null) {
            characterEncoding = "UTF-8";
        }
       
        if(path == null) {
            log.warn("Unable to rewrite link. Return EVAL_PAGE");
            return null;
        }
        
        String urlString = null;
        
        if (this.containerResource) {
            HstURL hstUrl = requestContext.getURLFactory().createURL(HstURL.RESOURCE_TYPE, ContainerConstants.CONTAINER_REFERENCE_NAMESPACE , null, requestContext);
            hstUrl.setResourceID(path);
            urlString = hstUrl.toString();
        } else {
            
            if(subPath != null) {
                // subPath is allowed to be empty ""
                path += PATH_SUBPATH_DELIMITER + subPath;
            } else if (mount != null && !mount.isSite()) {
                // mount is configured to support subPath: Include the PATH_SUBPATH_DELIMITER for locations that that would be exclused by virtualhosts configuration
                // like resources ending on .jpg or .pdf etc 
                if(mount.getVirtualHost().getVirtualHosts().isExcluded(path)) {
                    // path should not be excluded for hst request processing because for example it is a REST call for a binary. Add the PATH_SUBPATH_DELIMITER
                    // to avoid this
                    path += PATH_SUBPATH_DELIMITER;
                }
            }
            
            HstContainerURL navURL = requestContext.getContainerURLProvider().createURL(mount, requestContext.getBaseURL() , path);
            urlString  = requestContext.getURLFactory().createURL(HstURL.RENDER_TYPE, null, navURL, requestContext).toString();
        }
        
        Mount requestMount = requestContext.getResolvedMount().getMount();
        /*
         * we create a url including http when the Mount is not null and one of the lines below is true
         * 0) requestContext.isFullyQualifiedURLs() = true
         * 1) external = true
         * 2) The virtualhost from current request Mount is different than the Mount for this link
         * 3) The portnumber is in the url, and the current request Mount has a different portnumber than the Mount for this link
         */
        String renderHost = requestContext.getRenderHost();
        if(mount != null) {
            if(requestContext.getRenderHost() != null) {
                // when renderhost is not null, we never create fully qualified URLs any more: Cross-domain is not a use case.
                if(requestMount != mount) {
                    // URL is cross mount. Check whether we should return # or a cross mount link:
                    // TODO: for now, we never return a cross mount link when having a render host. When we want to support a 
                    // preview from the cms, we might need a switch to support cross mount linking in combination with a renderhost.
                    boolean enableCrossMountLink = true;
                    if(enableCrossMountLink) {
                        // return the url with renderHost which might be a different host.
                        renderHost =  mount.getVirtualHost().getHostName(); 
                    } else {
                        return "#";
                    }
                }
            } else if(requestContext.isFullyQualifiedURLs() || external || requestMount.getVirtualHost() != mount.getVirtualHost()
                         || (mount.isPortInUrl() && requestMount.getPort() != mount.getPort())
                         || (mount.getScheme() != null && !mount.getScheme().equals(requestMount.getScheme())) ) {
                
               String host = mount.getScheme() + "://" + mount.getVirtualHost().getHostName();
               
               if(mount.isPortInUrl()) {
                   int port = mount.getPort();
                   if(port == 0) {
                       // the Mount is port agnostic. Take port from current container url
                      port = requestContext.getBaseURL().getPortNumber();
                   }
                   if(port == 80 || port == 443) {
                       // do not include default ports
                   } else {
                       host += ":"+port;
                   }
               }
               
               
               urlString =  host + urlString;
            }
        }
        
        // TODO HSTTWO-1599 improve below as this does not work for navigationStateful links
        if(renderHost != null && !this.containerResource) {
            // we need to append the render host as a request parameter but it is not needed for resources
            if(urlString.contains("?")) {
                urlString += "&";
            } else {
                urlString += "?";
            }
            urlString += ContainerConstants.RENDERING_HOST +"=" + renderHost;
        }
        
        return urlString;
    }
    
    public boolean isNotFound() {
        return notFound;
    }

    public void setNotFound(boolean notFound) {
        this.notFound = notFound;
    }

}
