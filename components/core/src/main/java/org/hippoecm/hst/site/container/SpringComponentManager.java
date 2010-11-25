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

import java.util.Properties;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationConverter;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ComponentManagerAware;
import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.hippoecm.hst.core.container.ContainerConfigurationImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringComponentManager implements ComponentManager, BeanPostProcessor {
    
    protected AbstractApplicationContext applicationContext;
    protected Configuration configuration;
    protected ContainerConfiguration containerConfiguration;
    protected String [] configurationResources;

    public SpringComponentManager() {
        this(null);
    }
    
    public SpringComponentManager(Configuration configuration) {
        this.configuration = configuration;
        this.containerConfiguration = new ContainerConfigurationImpl(this.configuration);
    }
    
    public void initialize() {
        String [] configurationResources = getConfigurationResources();
        
        if (null == configurationResources) {
            String classXmlFileName = getClass().getName().replace(".", "/") + ".xml";
            String classXmlFileName2 = getClass().getName().replace(".", "/") + "-*.xml";
            configurationResources = new String[] { classXmlFileName, classXmlFileName2 };            
        }

        this.applicationContext = new ClassPathXmlApplicationContext(configurationResources, false) {
            // According to the javadoc of org/springframework/context/support/AbstractApplicationContext.html#postProcessBeanFactory,
            // this allows for registering special BeanPostProcessors.
            protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
                beanFactory.addBeanPostProcessor(SpringComponentManager.this);
            }
        };
        
        if (this.configuration != null) {
            Properties initProps = ConfigurationConverter.getProperties(this.configuration);
            PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
            ppc.setIgnoreUnresolvablePlaceholders(true);
            ppc.setSystemPropertiesMode(PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_FALLBACK);
            ppc.setProperties(initProps);
            this.applicationContext.addBeanFactoryPostProcessor(ppc);
        }
        
        this.applicationContext.refresh();
    }

    public void start() {
        this.applicationContext.start();
    }

    public void stop() {
        this.applicationContext.stop();
    }
    
    public void close() {
        this.applicationContext.close();
    }
    
    public <T> T getComponent(String name) {
        return (T) this.applicationContext.getBean(name);
    }
    
    public ContainerConfiguration getContainerConfiguration() {
        return this.containerConfiguration;
    }

    public String[] getConfigurationResources() {
        return this.configurationResources;
    }
    
    public void setConfigurationResources(String [] configurationResources) {
        this.configurationResources = configurationResources;
    }

    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof ComponentManagerAware) {
            ((ComponentManagerAware) bean).setComponentManager(this);
        }
        
        return bean;
    }

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean; 
    }

}
