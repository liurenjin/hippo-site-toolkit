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
package org.hippoecm.hst.configuration.components;

import java.util.Map;

import org.hippoecm.hst.core.component.HstComponent;

/**
 * A <code>HstComponentConfigurations</code> contains a map of (root) <code>HstComponentConfiguration</code> objects which themselves might 
 * contain additional <code>HstComponentConfiguration</code> children and so on.
 * Each root <code>HstComponentConfiguration</code> is identified by a unique id within the <code>HstComponentConfiguration<'/code>s object. 
 *
 * <p/>
 * NOTE: As {@link HstComponent} instances can access <code>HstComponentConfigurations</code> instances but should not be able to modify them, 
 * implementations must make sure that through the api a <code>HstComponentConfigurations</code> instance cannot be changed. Returned List and Map
 * should be therefor unmodifiable.  
 */
public interface HstComponentsConfiguration {

    /**
     * Return the map of all root <code>HstComponentConfiguration</code>'s where the keys are the the <code>HstComponentConfiguration</code>'s 
     * ({@link HstComponentConfiguration#getId()}).
     * Implementations should return an unmodifiable map, for example {@link java.util.Collections$UnmodifiableMap} to avoid 
     * client code changing configuration
     * @return the map of all root <code>HstComponentConfiguration</code>'s and an empty map 
     * if not root <code>HstComponentConfiguration</code>'s are present. The keys in the map are the id's of the <code>HstComponentConfiguration</code>'s 
     * ({@link HstComponentConfiguration#getId()})
     */
    Map<String, HstComponentConfiguration> getComponentConfigurations();

    /**
     * Returns the root <code>HstComponentConfiguration</code> whose {@link HstComponentConfiguration#getId()} equals this <code>id</code>.
     * @param id the id of the root <code>HstComponentConfiguration</code>
     * @return a root <code>HstComponentConfiguration</code> whose {@link HstComponentConfiguration#getId()} equals this <code>id</code>. 
     * When there is no <code>HstComponentConfiguration</code> with this <code>id</code>, <code>null</code>  is returned.
     */
    HstComponentConfiguration getComponentConfiguration(String id);

}
