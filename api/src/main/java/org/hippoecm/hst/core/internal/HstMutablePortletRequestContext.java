/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.hst.core.internal;

import javax.portlet.PortletConfig;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;

import org.hippoecm.hst.core.request.HstPortletRequestContext;

/**
 * 
 * This is an INTERNAL USAGE ONLY API. Clients should not cast to these interfaces as they should never be used from client code
 * 
 * @version $Id$
 *
 */
public interface HstMutablePortletRequestContext extends HstMutableRequestContext, HstPortletRequestContext {

	public void setPortletConfig(PortletConfig portletConfig);

	public void setPortletRequest(PortletRequest portletRequest);

	public void setPortletResponse(PortletResponse portletResponse);

}
