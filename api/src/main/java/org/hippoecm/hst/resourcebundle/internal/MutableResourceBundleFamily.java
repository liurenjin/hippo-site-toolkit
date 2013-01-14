/**
 * Copyright 2013 Hippo.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.resourcebundle.internal;

import java.util.Locale;
import java.util.ResourceBundle;

import org.hippoecm.hst.resourcebundle.ResourceBundleFamily;

/**
 * MutableResourceBundleFamily
 * <P>
 * MutableResourceBundleFamily allows to manage the internal resource bundles.
 * </P>
 */
public interface MutableResourceBundleFamily extends ResourceBundleFamily {

    /**
     * Sets the default resource bundle for live mode.
     * @param bundle
     */
    void setDefaultBundle(ResourceBundle bundle);

    /**
     * Sets the default resource bundle for preview mode.
     * @param bundle
     */
    void setDefaultBundleForPreview(ResourceBundle bundle);

    /**
     * Sets the localized resource bundle specified by the locale for live mode.
     * @param locale
     * @param bundle
     */
    void setLocalizedBundle(Locale locale, ResourceBundle bundle);

    /**
     * Sets the localized resource bundle specified by the locale for preview mode.
     * @param locale
     * @param bundle
     */
    void setLocalizedBundleForPreview(Locale locale, ResourceBundle bundle);

    /**
     * Removes the localized resource bundle specified by the locale for live mode.
     * @param locale
     */
    void removeLocalizedBundle(Locale locale);

    /**
     * Removes the localized resource bundle specified by the locale for preview mode.
     * @param locale
     */
    void removeLocalizedBundleForPreview(Locale locale);

}
