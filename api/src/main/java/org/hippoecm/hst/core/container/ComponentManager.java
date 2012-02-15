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
package org.hippoecm.hst.core.container;

import java.util.Map;

/**
 * ComponentManager interface.
 * This is responsible for initializing, starting, stopping and closing container components.
 * 
 * @version $Id$
 */
public interface ComponentManager
{
    
    /**
     * Sets configuration resources for components assembly
     * @param configurationResources
     */
    void setConfigurationResources(String [] configurationResources);
    
    /**
     * Returns configuration resources for components assembly
     * @param configurationResource
     */
    String [] getConfigurationResources();
    
    /**
     * Initializes the component manager and container components.
     */
    void initialize();
    
    /**
     * Starts the component manager to serve container components.
     */
    void start();
    
    /**
     * Returns the registered container component by name.
     * Returns null if a component is not found by the specified name.
     * 
     * @param <T> component type
     * @param name the name of the component
     * @return component
     */
    <T> T getComponent(String name);

    /**
     * Returns the registered container components that match the given object type (including subclasses).
     * Returns empty map if a component is not found by the specified required type.
     * 
     * @param <T> component type
     * @param requiredType the required type of the component
     * @return component map
     */
    <T> Map<String, T> getComponentsOfType(Class<T> requiredType);

    /**
     * Stop the component manager.
     */
    void stop();
    
    /**
     * Closes the component manager and all the components.
     */
    void close();
    
    /**
     * Returns the container configuration
     * @return
     */
    ContainerConfiguration getContainerConfiguration();
    
}
