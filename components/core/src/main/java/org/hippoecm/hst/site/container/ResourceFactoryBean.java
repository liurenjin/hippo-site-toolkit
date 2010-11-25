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
package org.hippoecm.hst.site.container;

import java.io.File;
import java.net.URI;
import java.net.URL;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * ResourceFactoryBean
 * <P>
 * Simple Resource Factory Bean to create URL string or FILE from resource string for convenience.
 * </P>
 * @version $Id$
 */
public class ResourceFactoryBean implements FactoryBean<Object>, ResourceLoaderAware {
    
    private ResourceLoader resourceLoader;
    private String resourcePath;
    private Class<?> objectType;
    private boolean singleton = true;
    private Object singletonBean;
    
    public ResourceFactoryBean(String resourcePath) {
        this(resourcePath, null);
    }
    
    public ResourceFactoryBean(String resourcePath, Class<?> objectType) {
        this.resourcePath = resourcePath;
        this.objectType = objectType;
    }
    
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public Object getObject() throws Exception {
        if (singleton) {
            if (singletonBean == null) {
                singletonBean = createInstance();
                resourceLoader = null;
            }
            
            return singletonBean;
        } else {
            return createInstance();
        }
    }

    public boolean isSingleton() {
        return singleton;
    }
    
    public void setSingleton(boolean singleton) {
        this.singleton = singleton;
    }

    public Class<?> getObjectType() {
        return (objectType != null ? objectType : Resource.class);
    }

    protected Object createInstance() throws Exception {
        Resource resource = resourceLoader.getResource(resourcePath);
        
        if (URL.class == objectType) {
            return resource.getURL();
        } else if (String.class == objectType) {
            return resource.getURL().toString();
        } else if (URI.class == objectType) {
            return resource.getURI();
        } else if (File.class == objectType) {
            return resource.getFile();
        }
        
        return resource;
    }

}
