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
package org.hippoecm.hst.caching;

import java.util.Map;

public interface Cache {
    public boolean containsKey(CacheKey key);
    public CachedResponse get(CacheKey key);
    public Map<String, String> getStatistics();
    public void store(CacheKey key, CachedResponse value);
    public void remove(CacheKey key);
    public void setActive(boolean active);
    public boolean isActive();
    public void clear();
}
