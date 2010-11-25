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

import javax.servlet.ServletConfig;

/**
 * Interface to be implemented by a {@link ComponentManager} object that wishes to be notified of the ServletConfig that it runs in.
 * @deprecated replaced by ServletContextAware as initialization is not longer done using servlets
 * @version $Id$
 */
public interface ServletConfigAware {
    
    /**
     * Set the ServletConfig that this object runs in.
     * @param servletConfig
     */
    void setServletConfig(ServletConfig servletConfig); 
    
}
