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
package org.hippoecm.hst.core;

/**
 * Resource management interface.
 * Some resource pool such as JCR session pool can expose an implementation
 * of this interface, and then the container can register disposable resources
 * and unregister the disposable resources after serving request.
 * 
 * @version $Id$
 */
public interface ResourceLifecycleManagement {
    
    /**
     * Returns true if resource lifecycle management is turned on.
     * 
     * @return
     */
    boolean isActive();
    
    /**
     * Turns on or off the resource lifecycle management.
     * 
     * @param active
     */
    void setActive(boolean active);
    
    /**
     * Registers a disposable resource.
     * 
     * @param resource
     */
    void registerResource(Object resource);
    
    /**
     * Unregisters the disposable resource.
     * 
     * @param resource
     */
    void unregisterResource(Object resource);
    
    /**
     * Dispose the specified resource.
     * 
     * @param resource
     */
    void disposeResource(Object resource);
    
    /**
     * Dispose all the resources.
     */
    void disposeAllResources();
    
}
