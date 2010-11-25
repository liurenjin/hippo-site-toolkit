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
package org.hippoecm.hst.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.util.PathUtils;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleHmlStringParser {

    private final static Logger log = LoggerFactory.getLogger(SimpleHmlStringParser.class);
    public static final String[] EXTERNALS = {"http:", "https:", "webdav:", "ftp:", "mailto:"};
    public static final String LINK_TAG = "<a";
    public static final String IMG_TAG = "<img";
    public static final String END_TAG = ">";
    public static final String HREF_ATTR_NAME = "href=\"";
    public static final String SRC_ATTR_NAME = "src=\"";
    public static final String ATTR_END = "\"";

    public static String parse(HippoNode node, String html, HttpServletRequest request, HttpServletResponse response) {
        // only create if really needed
        StringBuffer sb = null;
        
        HstRequestContext reqContext = ((HstRequest)request).getRequestContext();
        
        int globalOffset = 0;
        while (html.indexOf(LINK_TAG, globalOffset) > -1) {
            int offset = html.indexOf(LINK_TAG, globalOffset);

            int hrefIndexStart = html.indexOf(HREF_ATTR_NAME, offset);
            if (hrefIndexStart == -1) {
                break;
            }

            if (sb == null) {
                sb = new StringBuffer(html.length());
            }

            hrefIndexStart += HREF_ATTR_NAME.length();
            offset = hrefIndexStart;
            int endTag = html.indexOf(END_TAG, offset);
            boolean appended = false;
            if (hrefIndexStart < endTag) {
                int hrefIndexEnd = html.indexOf(ATTR_END, hrefIndexStart);
                if (hrefIndexEnd > hrefIndexStart) {
                    String documentPath = html.substring(hrefIndexStart, hrefIndexEnd);

                    offset = endTag;
                    sb.append(html.substring(globalOffset, hrefIndexStart));
                    
                    if(isExternal(documentPath)) {
                        sb.append(documentPath);
                    } else {
                        String url = getHref(documentPath,node, reqContext, response);
                        if(url != null) {
                            if(request.getContextPath() != null) {
                                sb.append(request.getContextPath());
                            } 
                            if(request.getServletPath() != null) {
                                sb.append(request.getServletPath());
                            } 
                            sb.append(url);
                        } else {
                           log.warn("Skip href because url is null"); 
                        }
                    }
                    sb.append(html.substring(hrefIndexEnd, endTag));
                    appended = true;
                }
            }
            if (!appended && offset > globalOffset) {
                sb.append(html.substring(globalOffset, offset));
            }
            globalOffset = offset;
        }

        if (sb != null) {
            sb.append(html.substring(globalOffset, html.length()));
            html = String.valueOf(sb);
            sb = null;
        }

        globalOffset = 0;
        while (html.indexOf(IMG_TAG, globalOffset) > -1) {
            int offset = html.indexOf(IMG_TAG, globalOffset);

            int srcIndexStart = html.indexOf(SRC_ATTR_NAME, offset);

            if (srcIndexStart == -1) {
                break;
            }

            if (sb == null) {
                sb = new StringBuffer(html.length());
            }
            srcIndexStart += SRC_ATTR_NAME.length();
            offset = srcIndexStart;
            int endTag = html.indexOf(END_TAG, offset);
            boolean appended = false;
            if (srcIndexStart < endTag) {
                int srcIndexEnd = html.indexOf(ATTR_END, srcIndexStart);
                if (srcIndexEnd > srcIndexStart) {
                    String srcPath = html.substring(srcIndexStart, srcIndexEnd);
                    
                    offset = endTag;
                    sb.append(html.substring(globalOffset, srcIndexStart));
                   
                    if(isExternal(srcPath)) {
                        sb.append(srcPath);
                    } else {
                        String translatedSrc = getSrcLink(srcPath, node, reqContext, response);
                        if(translatedSrc != null) {
                            if(request.getContextPath() != null) {
                                sb.append(request.getContextPath());
                            }
                            sb.append(translatedSrc);
                        } else {
                            log.warn("Could not translate image src. Skip src");
                        }
                    }
                    
                    sb.append(html.substring(srcIndexEnd, endTag));
                    appended = true;
                }
            }
            if (!appended && offset > globalOffset) {
                sb.append(html.substring(globalOffset, offset));
            }
            globalOffset = offset;
        }

        if (sb == null) {
            return html;
        } else {
            sb.append(html.substring(globalOffset, html.length()));
            return sb.toString();
        }
    }

    public static String getHref(String path, HippoNode node, HstRequestContext reqContext,
            HttpServletResponse response) {
        
        try {
            path = URLDecoder.decode(path, "utf-8");
        } catch (UnsupportedEncodingException e1) {
            log.warn("UnsupportedEncodingException for documentPath");
        }

        // translate the documentPath to a URL in combination with the Node and the mapping object
        if (path.startsWith("/")) {
            // absolute location, try to translate directly
            log.warn("Cannot rewrite absolute path '{}'. Expected a relative path. Return '{}'", path, path);
            return path;
        } else {
            // relative node, most likely a facetselect node:
            String uuid = null;
            try {
                Node facetSelectNode = node.getNode(path);
                if (facetSelectNode.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                    uuid = facetSelectNode.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
                    Session session = reqContext.getSession();
                    Node deref = session.getNodeByUUID(uuid);
                    
                    HstLink link = reqContext.getHstLinkCreator().create(deref, reqContext.getResolvedSiteMapItem());
                    
                    if(link == null) {
                        log.warn("Unable to create a link for '{}'. Return orginal path", path);
                    } else {
                        StringBuffer href = new StringBuffer();
                        for(String elem : link.getPathElements()) {
                            String enc = response.encodeURL(elem);
                            href.append("/").append(enc);
                        }
                        log.debug("Rewrote internal link '{}' to link '{}'", path, href.toString());
                        return href.toString();
                    }
                } else {
                    log.warn("relative node as link, but the node is not a facetselect. Unable to rewrite this to a URL. Return '{}'", path);
                    return path;
                }
            } catch (ItemNotFoundException e) {
                log.warn("Unable to rewrite href '{}' to proper url : '{}'. Return null", path, e.getMessage());
            } catch (PathNotFoundException e) {
                log.warn("Unable to rewrite href '{}' to proper url : '{}'. Return null", path, e.getMessage());
            } catch (RepositoryException e) {
                log.warn("Unable to rewrite href '{}' to proper url : '{}'. Return null", path, e.getMessage());
            }
        }
        return null;
    }
    
    public static boolean isExternal(String path) {
        for (String prefix : EXTERNALS) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
    
    public static String getSrcLink(String path, HippoNode node, HstRequestContext reqContext, HttpServletResponse response) {

        try {
            path = URLDecoder.decode(path, "utf-8");
        } catch (UnsupportedEncodingException e1) {
            log.warn("UnsupportedEncodingException for documentPath");
        }

        try {
            Session session = reqContext.getSession();
            if (path.startsWith("/") && reqContext.getSession().itemExists(path)) {
                // TODO resolve these binaries though they should not be absolute in the first place
            }

            String[] pathEls = path.split("/");

            if (node.hasNode(pathEls[0])) {
                Node facetSelect = node.getNode(pathEls[0]);
                if (facetSelect.isNodeType(HippoNodeType.NT_FACETSELECT)
                        && facetSelect.hasProperty(HippoNodeType.HIPPO_DOCBASE)) {
                    String uuid = facetSelect.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
                    Node deref = session.getNodeByUUID(uuid);
                    
                    String derefedPath  = PathUtils.normalizePath(deref.getPath());
                    StringBuffer srcLink = new StringBuffer("/binaries");
                    
                    for(String elem : derefedPath.split("/")) {
                        String enc = response.encodeURL(elem);
                        srcLink.append("/").append(enc);
                    }
                    
                    if (pathEls.length > 1) {
                        int i = 0;
                        while(++i < pathEls.length) {
                            String enc = response.encodeURL(pathEls[i]);
                            srcLink.append("/").append(enc);
                        }
                        return srcLink.toString();
                    } else {
                        // did not point to some resource...return just the location of the handle
                        return srcLink.toString();
                    }

                } else {
                    log.warn("Expected node of nodetype " + HippoNodeType.NT_FACETSELECT
                            + ". Cannot translate binary link");
                }
            } else {
                log.warn("Expected to find facetselect node '" + pathEls[0]
                        + "' but not found. Unable to translate binary link");
            }
        } catch (PathNotFoundException e) {
            log.warn("Unable to rewrite src '{}' to proper url : '{}'. Return null", path, e.getMessage());
        } catch (ValueFormatException e) {
            log.warn("Unable to rewrite src '{}' to proper url : '{}'. Return null", path, e.getMessage());
        } catch (ItemNotFoundException e) {
            log.warn("Unable to rewrite src '{}' to proper url : '{}'. Return null", path, e.getMessage());
        } catch (RepositoryException e) {
            log.warn("Unable to rewrite src '{}' to proper url : '{}'. Return null", path, e.getMessage());
        }
        return null;
    }
    
}
