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

import java.util.EventObject;
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
     * Returns the registered component from a child context.
     * If <CODE>addonModuleNames</CODE> consists of multiple items, then 
     * each <CODE>addonModuleNames</CODE> item is regarded as child addon module name 
     * in the descendant hierarchy, as ordered.
     * Returns null if a component is not found by the specified name.
     * 
     * @param <T>
     * @param name
     * @param addonModuleNames
     * @return
     * @throws ModuleNotFoundException thrown when module is not found by the addonModuleNames
     */
    <T> T getComponent(String name, String ... addonModuleNames);

    /**
     * Returns the registered container component that match the given object type (including subclasses) from a child context.
     * Returns empty map if a component is not found from a child context
     * by the specified required type.
     * If <CODE>addonModuleNames</CODE> consists of multiple items, then 
     * each <CODE>addonModuleNames</CODE> item is regarded as child addon module name 
     * in the descendant hierarchy, as ordered.
     * Returns empty map if a component is not found by the specified type.
     * 
     * @param <T>
     * @param requiredType
     * @param addonModuleNames
     * @return component map
     * @throws ModuleNotFoundException thrown when module is not found by the addonModuleNames
     */
    <T> Map<String, T> getComponentsOfType(Class<T> requiredType, String ... addonModuleNames);

    /**
     * Publish the given event to all components which wants to listen to.
     * Note that an implementation may decide to support specific child types of <CODE>EventObject</CODE> objects only.
     * Spring Framework based implementations can support Spring Framework's <CODE>ApplicationEvent</CODE> objects only, for instance.
     * If an implementation doesn't support the specific type of EventObject objects, then the EventObject object will be just ignored.
     * @param event the event to publish (may be an application-specific or built-in HST-2 event)
     */
    void publishEvent(EventObject event);

    /**
     * Publish the given event to all components which wants to listen to.
     * Note that an implementation may decide to support specific child types of <CODE>EventObject</CODE> objects only.
     * Spring Framework based implementations can support Spring Framework's <CODE>ApplicationEvent</CODE> objects only, for instance.
     * If an implementation doesn't support the specific type of EventObject objects, then the EventObject object will be just ignored.
     * <P>
     * If <CODE>addonModuleNames</CODE> is provided, then the given event is published to the specific module instance and its child
     * module instances only.
     * </P>
     * <P>
     * If <CODE>addonModuleNames</CODE> consists of multiple items, then 
     * each <CODE>addonModuleNames</CODE> item is regarded as child addon module name 
     * in the descendant hierarchy, as ordered.
     * </P>
     * @param event the event to publish (may be an application-specific or built-in HST-2 event)
     * @param addonModuleNames
     */
    void publishEvent(EventObject event, String ... addonModuleNames);

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
