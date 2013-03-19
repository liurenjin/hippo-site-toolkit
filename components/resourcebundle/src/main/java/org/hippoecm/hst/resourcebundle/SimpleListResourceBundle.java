/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.resourcebundle;

import java.util.ListResourceBundle;

import org.apache.commons.lang.ArrayUtils;

/**
 * SimpleListResourceBundle
 */
public class SimpleListResourceBundle extends ListResourceBundle {

    private Object[][] contents;

    public SimpleListResourceBundle(Object[][] contents) {
        super();

        this.contents = new Object[contents.length][];

        for (int i = 0; i < contents.length; i++) {
            this.contents[i] = ArrayUtils.clone(contents[i]);
        }
    }

    @Override
    protected Object[][] getContents() {
        return contents;
    }

}
