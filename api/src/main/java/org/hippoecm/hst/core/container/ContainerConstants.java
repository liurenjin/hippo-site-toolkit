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
}
