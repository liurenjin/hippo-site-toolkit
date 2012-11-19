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

import org.hippoecm.hst.core.component.HstURL;


/**
 * {@link HstComponentConfiguration}.
 * <P>
 * Basic information interface for component configuration.
 * </P>
 * 
 * @see {@link HstComponentConfiguration}
 * @version $Id$
 */
public interface HstComponentInfo {
    
    /**
     * Returns the id for this component configuration. 
     * The id must be unique within the container {@link HstComponentsConfiguration}, 
     * or <code>null</code> if it is not needed to be directly accessed by the
     * <code>HstComponentsConfiguration</code> through {@link HstComponentsConfiguration#getComponentConfiguration(String)}. 
     * Every <code>HstComponentConfiguration</code> that can be referred to from within a 
     * {@link org.hippoecm.hst.configuration.sitemap.HstSiteMapItem} must have an id.
     * 
     * @return the id of this component configuration or <code>null</code> if no id set
     */
    String getId();
    
    /**
     * Return the name of this component configuration. It <strong>must</strong> be unique amongst siblings.
     * The value returned by this method, is the value that must be used in rendering code (jsp/velocity/freemarker) to include the output
     * of a child <code>HstComponent</code> instance.
     * 
     * @return the logical name this component configuration, unique amongst its siblings
     */
    String getName();
    
    /**
     * @return the fully-qualified class name of the class implementing the {@link org.hippoecm.hst.core.component.HstComponent} interface
     */
    String getComponentClassName();
    

    /**
     * @return <code>true</code> when this {@link HstComponentConfiguration} is configured to be rendered standalone in case of {@link HstURL#COMPONENT_RENDERING_TYPE}
     */
    boolean isStandalone();

    /**
     * Rendering asynchronous is very useful for hst components that are uncacheable, depend on external services, or take long to render.
     * @return <code>true</code> when this {@link HstComponentConfiguration} is configured to be rendered asynchronous.
     */
    boolean isAsync();

    /**
     * @return <code>true</code> if rendering / resource requests can have their entire page http responses cached. Note that
     * a {@link HstComponentConfiguration} by default is cachable unless configured not to be cachable. A {@link HstComponentConfiguration}
     * is only cachable if and only if <b>all</b> its descendant {@link HstComponentConfiguration}s for the request are cachable : <b>Note</b>
     * explicitly for 'the request', thus {@link HstComponentConfiguration} that are {@link HstComponentConfiguration#isAsync()}
     * and its descendants can be uncachable while an ancestor {@link HstComponentConfiguration} can stay cachable
     */
    boolean isCompositeCachable();

}
