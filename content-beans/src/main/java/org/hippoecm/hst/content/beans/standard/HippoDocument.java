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
import java.util.Locale;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.LocaleUtils;
import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoAvailableTranslationsBean.NoopTranslationsBean;
import org.hippoecm.repository.api.HippoNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Node(jcrType="hippo:document")
public class HippoDocument extends HippoItem implements HippoDocumentBean{

    private static Logger log = LoggerFactory.getLogger(HippoDocument.class);
    
    private Map<String, BeanWrapper<HippoHtml>> htmls = new HashMap<String, BeanWrapper<HippoHtml>>();
    
    private String canonicalHandleUUID;


    private boolean availableTranslationsBeanMappingClassInitialized;
    private HippoAvailableTranslationsBean availableTranslationsBeanMappingClass;
    
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
            // first get the canonical handle. Because we can have a document in a faceted resultset, we first need to get the 
            // canonical node of the document, and then fetch the parent
            javax.jcr.Node canonical = ((HippoNode)getNode()).getCanonicalNode();
            if(canonical == null) {
                log.error("We cannot get the canonical handle uuid for a document that does not have a canonical version. Node '{}'. Return null", getNode().getPath());
                return null;
            }
            javax.jcr.Node handle = canonical.getParent();
            canonicalHandleUUID =  handle.getIdentifier();
        } catch (RepositoryException e) {
            log.error("Cannot get handle uuid for node '"+this.getPath()+"'", e);
        }
        return canonicalHandleUUID;
    }
    
    public String getLocaleString() {
        return getProperty("hippotranslation:locale");
    }
    
    public Locale getLocale() {
        String localeString = getLocaleString();
        
        if (localeString != null) {
            return LocaleUtils.toLocale(localeString);
        }
        
        return null; 
    }
    
    public <T extends HippoBean> HippoAvailableTranslationsBean<T> getAvailableTranslationsBean(Class<T> beanMappingClass) {
        if(!availableTranslationsBeanMappingClassInitialized) {
            availableTranslationsBeanMappingClassInitialized = true;
            try {
                availableTranslationsBeanMappingClass = getBean("hippotranslation:translations");
            } catch (ClassCastException e) {
                 log.warn("Bean with name 'hippotranslation:translations' was not of type '{}'. Unexpected. Cannot get translation bean", HippoAvailableTranslationsBean.class.getName());
            }
            if(availableTranslationsBeanMappingClass== null) {
                availableTranslationsBeanMappingClass = new NoopTranslationsBean<T>();
                log.debug("Did not find a translations bean for '{}'. Return a no-operation instance of it", getValueProvider().getPath());
            } else {
                ((HippoAvailableTranslations)availableTranslationsBeanMappingClass).setBeanMappingClass(beanMappingClass);
            }
        }
        return (HippoAvailableTranslationsBean<T>)availableTranslationsBeanMappingClass;
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

    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof HippoDocument)) {
            return false;
        }
        return super.equals(obj);
    }

}
