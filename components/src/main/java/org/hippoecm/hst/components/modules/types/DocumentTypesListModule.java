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
package org.hippoecm.hst.components.modules.types;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import org.hippoecm.hst.core.HSTHttpAttributes;
import org.hippoecm.hst.core.template.ContextBase;
import org.hippoecm.hst.core.template.HstFilterBase;
import org.hippoecm.hst.core.template.TemplateException;
import org.hippoecm.hst.core.template.module.ModuleBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentTypesListModule extends ModuleBase{

	private static final Logger log = LoggerFactory.getLogger(DocumentTypesListModule.class);
	public static final String NAMESPACE = "namespace";
    private List items;
    private String namespace;
    
	
	public void render(PageContext pageContext) throws TemplateException{
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        ContextBase ctxBase = (ContextBase) request.getAttribute(HSTHttpAttributes.CURRENT_CONTENT_CONTEXTBASE_REQ_ATTRIBUTE);
        Session jcrSession = ctxBase.getSession();

        boolean params = false;
        if (moduleParameters != null) {
            params = true;
        }
        if (params && moduleParameters.containsKey(NAMESPACE)) {
            String namespace = moduleParameters.get(NAMESPACE);
            if (!"".equals(namespace)) {
                setNamespace(namespace);
            }
        }
        
        try {
			getNodeTypes(jcrSession,namespace);
		} catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        pageContext.setAttribute(getVar(), getTypes());
	}

	public List getTypes() {
		return this.items;
	}

	private void getNodeTypes(Session session, String namespacePrefix) throws RepositoryException {
		NodeTypeManager ntmgr = session.getWorkspace().getNodeTypeManager();
		NodeTypeIterator it = ntmgr.getAllNodeTypes();
		List<NodeType> types = new ArrayList<NodeType>();

		while (it.hasNext()) {
			NodeType nt = it.nextNodeType();			
			if (nt.getName().startsWith(namespacePrefix)) {
				types.add(nt);
			}
		}
		this.items=types;
	}
	
	private void setNamespace(String namespace){
		this.namespace=namespace;
	}

}
