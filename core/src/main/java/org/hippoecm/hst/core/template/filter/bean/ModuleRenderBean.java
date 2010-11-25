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
package org.hippoecm.hst.core.template.filter.bean;

import java.io.Serializable;

import org.hippoecm.hst.core.template.module.Module;
import org.hippoecm.hst.core.template.node.PageContainerModuleNode;
import org.hippoecm.hst.core.template.node.PageContainerNode;

public class ModuleRenderBean implements Serializable{
  
    private static final long serialVersionUID = 1L;
    private transient final PageContainerNode pageContainerNode;
    private transient final PageContainerModuleNode pageContainerModuleNode;
    private final Module module;
    private final String name;
    
    public ModuleRenderBean(String name, PageContainerNode pageContainerNode, PageContainerModuleNode pageContainerModuleNode, Module module) {
    	this.pageContainerNode = pageContainerNode;
    	this.pageContainerModuleNode = pageContainerModuleNode;
    	this.module = module;
    	this.name = name;
    }
    
    
    
    
    
}
