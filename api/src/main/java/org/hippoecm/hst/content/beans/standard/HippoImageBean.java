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
package org.hippoecm.hst.content.beans.standard;

/**
 * This is a base interface for possible beans containing an imageset. A custom imageset might not have thumbnail or picture. This is more meant as an example api for the
 * default image set as delivered by the hippo ecm. You might have an image set with many more variants, like get100By100Picture(). 
 *
 * @deprecated since 7.5 : Use {@link HippoGalleryImageSetBean} instead
 */
@Deprecated
public interface HippoImageBean extends HippoBean {
    
    /**
     * Get the thumbnail version of the image.
     *
     * @return the thumbnail version of the image
     */
    HippoResourceBean getThumbnail();

    /**
     * Get the picture version of the image.
     *
     * @return the picture version of the image
     */
    HippoResourceBean getPicture();
}
