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
package org.hippoecm.hst.test;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.site.container.SpringComponentManager;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * AbstractSpringTestCase
 * </p>
 * <p>
 * 
 * </p>
 * 
 * @version $Id$
 *  
 */
public abstract class AbstractSpringTestCase
{

    protected final static Logger log = LoggerFactory.getLogger(AbstractSpringTestCase.class);
    protected ComponentManager componentManager;

    @Before
    public void setUp() throws Exception {
        this.componentManager = new SpringComponentManager(getContainerConfiguration());
        ((SpringComponentManager) this.componentManager).setConfigurationResources(getConfigurations());
        
        this.componentManager.initialize();
        this.componentManager.start();
    }

    @After
    public void tearDown() throws Exception {
        this.componentManager.stop();
        this.componentManager.close();
    }

    /**
     * required specification of spring configurations
     * the derived class can override this.
     */
    protected String[] getConfigurations() {
        String classXmlFileName = getClass().getName().replace(".", "/") + ".xml";
        String classXmlFileName2 = getClass().getName().replace(".", "/") + "-*.xml";
        return new String[] { classXmlFileName, classXmlFileName2 };
    }
    
    protected ComponentManager getComponentManager() {
        return this.componentManager;
    }

    protected <T> T getComponent(String name) {
        return getComponentManager().<T>getComponent(name);
    }
    
    protected Configuration getContainerConfiguration() {
        PropertiesConfiguration propConf = new PropertiesConfiguration();
        return propConf;
    }
}
