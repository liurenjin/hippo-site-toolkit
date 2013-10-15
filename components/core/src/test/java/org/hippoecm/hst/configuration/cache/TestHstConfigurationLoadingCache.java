/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.cache;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestHstConfigurationLoadingCache extends AbstractHstLoadingCacheTestCase {


    @Test(expected = IllegalArgumentException.class)
    public void testGetRootConfigPathFromInvalidEvent() {
        hstConfigurationLoadingCache.getRootConfigPath(new HstEvent("invalid", false));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetRootConfigPathFromInvalidEvent2() {
        hstConfigurationLoadingCache.getRootConfigPath(new HstEvent("/hst:hst/hst:configurations/", false));
    }

    @Test
    public void testGetRootConfigPathFromEvents() {
        String rootConfigEventPath = hstConfigurationLoadingCache.getRootConfigPath(new HstEvent("/hst:hst/hst:configurations/foo", false));
        assertEquals("/hst:hst/hst:configurations/foo", rootConfigEventPath);

        rootConfigEventPath = hstConfigurationLoadingCache.getRootConfigPath(new HstEvent("/hst:hst/hst:configurations/foo/", false));
        assertEquals("/hst:hst/hst:configurations/foo", rootConfigEventPath);

        rootConfigEventPath = hstConfigurationLoadingCache.getRootConfigPath(new HstEvent("/hst:hst/hst:configurations/foo/bar", false));
        assertEquals("/hst:hst/hst:configurations/foo", rootConfigEventPath);

        rootConfigEventPath = hstConfigurationLoadingCache.getRootConfigPath(new HstEvent("/hst:hst/hst:configurations/foo[1]", false));
        assertEquals("/hst:hst/hst:configurations/foo[1]", rootConfigEventPath);
    }
}
