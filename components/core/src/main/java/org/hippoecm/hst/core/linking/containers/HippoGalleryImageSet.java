/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.hst.core.linking.containers;

import org.hippoecm.hst.core.linking.AbstractResourceContainer;

/**
 * This is rewriting links for images into a 'pretty' url according some mapped configuration. 
 * 
 * The path from resourceNode typically is something like: /content/gallery/..../handle_nodeName/document_nodeName/hippogallery:thumbnail , where 
 * handle_nodeName equals document_nodeName. 
 * 
 * If the configured mapping would be: <entry key="hippogallery:thumbnail" value="thumbnail"/>, we create a link like this:
 * /thumbnail/content/gallery/..../handle_nodeName. 
 * 
 * The default mapping (where value="") is like this: 
 * 
 * <entry key="hippogallery:picture" value=""/>
 * 
 * and the url becomes just /content/gallery/..../handle_nodeName
 *
 */
public class HippoGalleryImageSet extends AbstractResourceContainer {

    public String getNodeType() {
        return "hippogallery:exampleImageSet";
    }
    
    
}
