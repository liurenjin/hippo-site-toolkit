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
package org.hippoecm.hst.content.beans.standard;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Node(jcrType="hippo:document")
public class HippoDocument extends HippoItem implements HippoDocumentBean{

    private static Logger log = LoggerFactory.getLogger(HippoDocument.class);
    
    private Map<String, BeanWrapper<HippoHtml>> htmls = new HashMap<String, BeanWrapper<HippoHtml>>();
    
    private String canonicalHandleUUID;
    
    /**
     * @param relPath
     * @return <code>HippoHtml</code> or <code>null</code> if no node exists as relPath or no node of type "hippostd:html"
     */
    public HippoHtml getHippoHtml(String relPath) {
        BeanWrapper<HippoHtml> wrapped = htmls.get(relPath);
        if(wrapped != null) {
            return wrapped.getBean();
        } else {
            Object o = getBean(relPath);
            if(o == null) {
                if(log.isDebugEnabled()) {
                    log.debug("No bean found for relPath '{}' at '{}'", relPath, this.getPath());
                }
                wrapped = new BeanWrapper<HippoHtml>(null);
                htmls.put(relPath, wrapped);
                return null;
            } else if(o instanceof HippoHtml) { 
                wrapped = new BeanWrapper<HippoHtml>((HippoHtml)o);
                htmls.put(relPath, wrapped);
                return wrapped.getBean();
            } else {
                log.warn("Cannot get HippoHtml bean for relPath '{}' at '{}' because returned bean is not of type HippoHtml but is '"+o.getClass().getName()+"'", relPath, this.getPath());
                // even when null, put it in the map to avoid being refetched
                wrapped = new BeanWrapper<HippoHtml>(null);
                htmls.put(relPath, wrapped);
                return null;
            }
        }
    }

    public String getCanonicalHandleUUID() {
        if(canonicalHandleUUID != null) {
            return canonicalHandleUUID;
        }
        if(this.getNode() == null) {
            log.warn("Cannot get handle uuid for detached node '{}'", this.getPath());
            return null;
        }
        try {
            javax.jcr.Node handle = this.getNode().getParent();
            javax.jcr.Node canonicalNode;
            if (handle.hasProperty(HippoNodeType.HIPPO_UUID)) {
                canonicalHandleUUID =  handle.getProperty(HippoNodeType.HIPPO_UUID).getString();
            } else if (handle.isNodeType("mix:referenceable")) {
                canonicalHandleUUID =  handle.getIdentifier();
            } else if( (canonicalNode = ((HippoNode)handle).getCanonicalNode()) != null) {
                // TODO once HREPTWO-4680 is fixed, this else if can (should) be removed again as it is less efficient
                canonicalHandleUUID = canonicalNode.getIdentifier();
            } else {
                log.warn("Cannot get uuid of handle for '{}'", this.getPath());
            }
        } catch (RepositoryException e) {
            log.error("Cannot get handle uuid for node '"+this.getPath()+"'", e);
        }
        return canonicalHandleUUID;
    }
    
    @Override
    public void detach(){
        super.detach();
        for(BeanWrapper<HippoHtml> wrapper : this.htmls.values()) {
            if(wrapper.getBean() != null) {
                wrapper.getBean().detach();
            }
        }
    }
    
    @Override
    public void attach(Session session){
        super.attach(session);
        this.htmls.clear();
    }


}
