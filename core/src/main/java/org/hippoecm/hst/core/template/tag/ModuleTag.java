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
package org.hippoecm.hst.core.template.tag;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.hippoecm.hst.core.HSTHttpAttributes;
import org.hippoecm.hst.core.template.node.PageContainerModuleNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The tag class that performs the render() and or execute() methods in a module template (JSP).
 *
 */
public class ModuleTag extends ModuleTagBase {
	
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ModuleTag.class);
	
	public int doStartTag() throws JspException {		
	    return EVAL_BODY_BUFFERED;
	}
	
	public int doEndTag() throws JspException {
		HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
		PageContainerModuleNode pcm = null;
		if (doExecute || doRender) {
			pcm = (PageContainerModuleNode) request.getAttribute(HSTHttpAttributes.CURRENT_PAGE_MODULE_NAME_REQ_ATTRIBUTE);
		}
		
		if(pcm == null) {
     	   log.warn("PageContainerModuleNode is null. Cannot render module. This might happen because the hst:layout node is pointing to a module jsp instead of to a layout jsp. Check you hst:configuration for this");
     	   throw new JspException("PageContainerModuleNode is null. Cannot render module. This might happen because the hst:layout node is pointing to a module jsp instead of to a layout jsp. Check you hst:configuration for this");
        }
		
		if (doExecute) {
			doExecute(request, pcm);
		}
		
		if (doRender) {
		   try {
		   doRender(request, pcm);
		   } catch (JspException e) {
               log.warn("error rendering module: " + e.getMessage());
               log.debug("error rendering module: " + e);
		       throw(e);
		   }
		}
		
		/*
		 * important to release after the module is rendered to clear the used parameters
		 * We need to call it here directly as a workaround, because Jetty does not properly seem 
		 * to call the release() method. If we do not cleanup parameters, moduleParameters from different
		 * BodyTagSupport classes within one single jsp seem to be mixed up.
		 */ 
		
		cleanup();
		
		return EVAL_PAGE;
	}
}


