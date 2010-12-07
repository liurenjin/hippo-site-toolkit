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
package org.hippoecm.hst.content.rewriter.impl;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Pattern;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.utils.SimpleHtmlExtractor;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.HippoWorkspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SimpleContentRewriter
 * 
 * @version $Id: SimpleContentRewriter.java 24267 2010-10-11 09:09:56Z aschrijvers $
 */
public class SimpleContentRewriter extends AbstractContentRewriter<String> {
    
    private final static Logger log = LoggerFactory.getLogger(SimpleContentRewriter.class);
    
    static final String[] EXTERNALS = {"http:", "https:", "webdav:", "ftp:", "mailto:", "#"};
    protected static final String LINK_TAG = "<a";
    protected static final String IMG_TAG = "<img";
    protected static final String END_TAG = ">";
    protected static final String HREF_ATTR_NAME = "href=\"";
    protected static final String SRC_ATTR_NAME = "src=\"";
    protected static final String ATTR_END = "\"";
    protected static final Pattern HTML_TAG_PATTERN = Pattern.compile("<html[\\s\\/>]", Pattern.CASE_INSENSITIVE);
    protected static final Pattern BODY_TAG_PATTERN = Pattern.compile("<body[\\s\\/>]", Pattern.CASE_INSENSITIVE);
    
    public SimpleContentRewriter() {
        
    }
    
    /**
     * @deprecated
     */
    @Override
    public String rewrite(String html, Node node, HstRequest request, HstResponse response) {
        return rewrite(html, node, request.getRequestContext());
    }
    
    @Override
    public String rewrite(String html, Node node, HstRequestContext requestContext) {
        return rewrite(html, node, requestContext, (Mount)null);
    }
    
    @Override
    public String rewrite(String html, Node node, HstRequestContext requestContext, String targetSiteAlias) {
        Mount targetMount = requestContext.getMount(targetSiteAlias);
        return rewrite(html, node, requestContext, targetMount);
    }
    
    @Override
    public String rewrite(String html, Node node, HstRequestContext requestContext, Mount targetMount) {
        // only create if really needed
        StringBuilder sb = null;
        
        // strip off html & body tag
        String innerHTML = SimpleHtmlExtractor.getInnerHtml(html, "body", false);
        
        if (innerHTML == null) {
            // if the input html doesn't have html and body tag at all, return the input
            if (html != null && !HTML_TAG_PATTERN.matcher(html).find() && !BODY_TAG_PATTERN.matcher(html).find()) {
                return html;
            }
            
            return null;
        }
        
        html = innerHTML.trim();
        
        if ("".equals(html)) {
            return "";
        }
        
        int globalOffset = 0;
        while (html.indexOf(LINK_TAG, globalOffset) > -1) {
            int offset = html.indexOf(LINK_TAG, globalOffset);

            int hrefIndexStart = html.indexOf(HREF_ATTR_NAME, offset);
            if (hrefIndexStart == -1) {
                break;
            }

            if (sb == null) {
                sb = new StringBuilder(html.length());
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
                        HstLink href = getDocumentLink(documentPath,node, requestContext, targetMount);
                        if(href != null && href.getPath() != null) {
                            sb.append(href.toUrlForm(requestContext, false));
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
                sb = new StringBuilder(html.length());
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
                        HstLink binaryLink = getBinaryLink(srcPath, node, requestContext, targetMount);
                        if(binaryLink != null && binaryLink.getPath() != null) {
                             sb.append(binaryLink.toUrlForm(requestContext,false));
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
    
    /**
     * @deprecated
     * @param path
     * @param node
     * @param request
     * @param response
     * @return
     */
    protected HstLink getDocumentLink(String path, Node node, HstRequest request, HstResponse response) {
        return getDocumentLink(path, node, request.getRequestContext(), null);
    }
    
    protected HstLink getDocumentLink(String path, Node node, HstRequestContext requestContext, Mount targetMount) {
        return getLink(path, node, requestContext, targetMount);
    }
    
    /**
     * @deprecated
     * @param path
     * @param node
     * @param request
     * @param response
     * @return
     */
    protected HstLink getBinaryLink(String path, Node node, HstRequest request, HstResponse response) {
        return getBinaryLink(path, node, request.getRequestContext(), null);
    }
    
    protected HstLink getBinaryLink(String path, Node node, HstRequestContext requestContext, Mount targetMount) {
        return getLink(path, node, requestContext, targetMount);
    }
    
    /**
     * @deprecated
     * @param path
     * @param node
     * @param request
     * @param response
     * @return
     */
    protected HstLink getLink(String path, Node node, HstRequest request, HstResponse response) {
        return getLink(path, node, request.getRequestContext(), null);
    }
    
    protected HstLink getLink(String path, Node node, HstRequestContext reqContext, Mount targetMount) {
        try {
            path = URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            log.warn("UnsupportedEncodingException for documentPath");
        }

        // translate the documentPath to a URL in combination with the Node and the mapping object
        if (path.startsWith("/")) {
            // this is an absolute path, which is not an internal content link. We just try to create a link for it directly
            if (targetMount == null) {
                return reqContext.getHstLinkCreator().create(path, reqContext.getResolvedMount().getMount());
            } else {
                return reqContext.getHstLinkCreator().create(path, targetMount);
            }
        } else {
            // relative node, most likely a mirror node:
            try {
                /*
                 * Important: use here the HippoWorkspace hierarchy resolver and not a direct getNode(path) because the hierarchy resolver
                 * contains logic about how to fetch relative paths for for example a picked image in an RTE field. 
                 * 
                 * For example, the relative path of a picked image in RTE field is something like: facetNode/[some-encoding-wildcard]/thumbnail
                 * 
                 * The hierarchy resolver knows how to solve: [some-encoding-wildcard]
                 * 
                 */
                Node mirrorNode = ((HippoWorkspace)((HippoSession)node.getSession()).getWorkspace()).getHierarchyResolver().getNode(node, path);    
                if (mirrorNode != null) {
                    if (targetMount == null) {
                        return reqContext.getHstLinkCreator().create(mirrorNode, reqContext);
                    } else {
                        return reqContext.getHstLinkCreator().create(mirrorNode, targetMount);
                    }
                } else {
                    log.warn("Cannot find node '{}' for internal link for document '{}'. Cannot create link", path, node.getPath());
                }
            } catch (InvalidItemStateException e) {
                log.warn("Unable to rewrite '{}' to proper url : '{}'. Return null", path, e.getMessage());
            } catch (RepositoryException e) {
                log.warn("Unable to rewrite '{}' to proper url : '{}'. Return null", path, e.getMessage());
            }
        }
        return null;
    }
    
    protected boolean isExternal(String path) {
        for (String prefix : EXTERNALS) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
    
}
