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

/**
 * HstComponent container constants
 * 
 * @version $Id$
 */
public interface ContainerConstants {

    /**
     * The key used to bind the <code>HstRequest</code> to the underlying
     * <code>HttpServletRequest</code>.
     */
    String HST_REQUEST = "org.hippoecm.hst.container.request";

    /**
     * The key used to bind the <code>HstResponse</code> to the underlying
     * <code>HttpServletRequest</code>.
     */
    String HST_RESPONSE = "org.hippoecm.hst.container.response";
    
    /**
     * The attribute name used to set the request context object into the servlet request.
     */
    String HST_REQUEST_CONTEXT = "org.hippoecm.hst.core.request.HstRequestContext";
    
    /**
     * The reference namespace for container managed resource url.
     */
    String CONTAINER_REFERENCE_NAMESPACE = "org.hippoecm.hst.container.reference.namespace";

    /**
     * The key used to set forward path.
     */
    String HST_FORWARD_PATH_INFO = "org.hippoecm.hst.container.forward.path_info";
    
    /**
     * The key to indicate HstFilter should "reset" itself from being done, allowing multiple invokations.
     */
    String HST_RESET_FILTER = "org.hippoecm.hst.container.HstFilter.reset";
    /**
     * The key used to set the cms location for the surf and edit 
     */
    String CMS_LOCATION = "cms.location";
    
    /**
     * The head element attribute name prefix used as a hint for container to aggregate.
     */
    String HEAD_ELEMENT_CONTRIBUTION_HINT_ATTRIBUTE_PREFIX = "org.hippoecm.hst.container.head.element.contribution.hint.";
    
    /**
     * The category key hint for head elements. This category can be used to filter head elements during writing head elements.
     */
    String HEAD_ELEMENT_CONTRIBUTION_CATEGORY_HINT_ATTRIBUTE = HEAD_ELEMENT_CONTRIBUTION_HINT_ATTRIBUTE_PREFIX + "category";
    
    /**
     * The parameter name for custom error handler class name in the root component configuration
     */ 
    String CUSTOM_ERROR_HANDLER_PARAM_NAME = "org.hippoecm.hst.core.container.custom.errorhandler";  

    /**
     * Subject session attribute name
     */
    String SUBJECT_ATTR_NAME = "org.hippoecm.hst.security.servlet.subject";

    /**
     * attribute that has a value when the request URI is already decoded.
     */
    String IS_REQUEST_URI_DECODED = "org.hippoecm.hst.container.HstContainerRequest.requestURI.isDecoded";
    
    /**
     * attribute that has a value when the request URI is already decoded.
     */
    String VIRTUALHOSTS_REQUEST_ATTR = "org.hippoecm.hst.configuration.hosting.VirtualHost.requestAttr";
    
    /**
     * Subject's repository credentials session attribute name (This one can be optionally and temporarily set in a container that doesn't support JACC.)
     */
    String SUBJECT_REPO_CREDS_ATTR_NAME = "org.hippoecm.hst.security.servlet.subject.repo.creds";
    
    /**
     * Preferred local request or session attribute name
     */
    String PREFERRED_LOCALE_ATTR_NAME = "org.hippoecm.hst.container.preferred.locale";
    
    /**
     * The dispatch URI scheme attribute name
     */
    String DISPATCH_URI_SCHEME = "org.hippoecm.hst.core.container.HstComponentWindow.dispatch.uri.scheme";
    
    String MOUNT_ALIAS_REST = "rest";
    
    String MOUNT_ALIAS_SITE = "site";
    
    String MOUNT_ALIAS_GALLERY = "gallery";
    
    String MOUNT_ALIAS_ASSETS = "assets";
    
    /**
     * 'composermode' type name
     */
    String COMPOSERMODE = "composermode";
    
    /**
     * 'composermode' attr type name
     */
    String COMPOSERMODE_ATTR_NAME = "org.hippoecm.hst.composermode";
    
    /**
     * 'composermode template view' attr type name
     */
    String COMPOSERMODE_TEMPLATE_VIEW_ATTR_NAME = "org.hippoecm.hst.composermode-template-view";
    
    /**
     * The parameter name used in the request to store whether or not all URLs that are created must be fully qualified
     */
    String HST_REQUEST_USE_FULLY_QUALIFIED_URLS = "org.hippoecm.hst.container.request.fqu";

    /**
     * The parameter name used in the request to store whether or not a different host than the one in the request needs to be used
     */
    String RENDERING_HOST = "org.hippoecm.hst.container.render_host"; 
    
    /**
     * The parameter/attribute name used to store the real_host 
     */
    String REAL_HOST = "org.hippoecm.hst.container.real_host"; 
 
    /**
     * http session attribute to indicate a single sign on session is created through the cms
     */
    String CMS_SSO_AUTHENTICATED = "org.hippoecm.hst.container.sso_cms_authenticated";
    
    /**
     * request attribute to indicate that cms single sign on is needed
     */
    String CMS_SSO_AUTHENTICATION_NEEDED = "org.hippoecm.hst.container.cms_sso_authentication_needed";
}
