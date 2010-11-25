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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;
import java.net.URL;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;


public class TestResourceFactoryBean {
    
    private ResourceLoader resourceLoader;
    
    @Before
    public void setUp() throws Exception {
        resourceLoader = new ClassPathXmlApplicationContext();
    }
    
    @Test
    public void testResourceFactoryBean() throws Exception {
        String resourcePath = "classpath:/" + ResourceFactoryBean.class.getName().replace('.', '/') + ".class";
        
        FactoryBean factoryBean = new ResourceFactoryBean(resourcePath);
        ((ResourceLoaderAware) factoryBean).setResourceLoader(resourceLoader);
        Object bean = factoryBean.getObject();
        assertTrue(bean instanceof Resource);
        assertEquals(Resource.class, factoryBean.getObjectType());
        
        factoryBean = new ResourceFactoryBean(resourcePath, URL.class);
        ((ResourceLoaderAware) factoryBean).setResourceLoader(resourceLoader);
        bean = factoryBean.getObject();
        assertTrue(bean instanceof URL);
        assertTrue(bean.toString().endsWith("/ResourceFactoryBean.class"));
        assertEquals(URL.class, factoryBean.getObjectType());
        
        factoryBean = new ResourceFactoryBean(resourcePath, URI.class);
        ((ResourceLoaderAware) factoryBean).setResourceLoader(resourceLoader);
        bean = factoryBean.getObject();
        assertTrue(bean instanceof URI);
        assertTrue(bean.toString().endsWith("/ResourceFactoryBean.class"));
        assertEquals(URI.class, factoryBean.getObjectType());
        
        factoryBean = new ResourceFactoryBean(resourcePath, String.class);
        ((ResourceLoaderAware) factoryBean).setResourceLoader(resourceLoader);
        bean = factoryBean.getObject();
        assertTrue(bean instanceof String);
        assertTrue(bean.toString().endsWith("/ResourceFactoryBean.class"));
        assertEquals(String.class, factoryBean.getObjectType());
        
        factoryBean = new ResourceFactoryBean(resourcePath, File.class);
        ((ResourceLoaderAware) factoryBean).setResourceLoader(resourceLoader);
        bean = factoryBean.getObject();
        assertTrue(bean instanceof File);
        assertEquals(((File) bean).getName(), "ResourceFactoryBean.class");
        assertEquals(File.class, factoryBean.getObjectType());
    }
}
