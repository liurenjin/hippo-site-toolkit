/*
 *  Copyright 2008-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.cache.ehcache;

import java.util.concurrent.Callable;

import org.apache.commons.collections.map.LRUMap;
import org.hippoecm.hst.cache.CacheElement;
import org.hippoecm.hst.cache.HstCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.InvalidConfigurationException;
import net.sf.ehcache.config.PersistenceConfiguration;

/**
 * HstCacheEhCacheImpl
 */
public class HstCacheEhCacheImpl implements HstCache {

    private static final Logger log = LoggerFactory.getLogger(HstCacheEhCacheImpl.class);

    private final Ehcache ehcache;

    private Cache staleCache;

    private Cache secondLevelCache;

    /**
     * Cache that contains uncacheable keys which can be used to avoid more expensive lookups in second level page
     * cache in case that cache is outside the JVM (for example redis)
     */
    private LRUMap uncacheableKeys;
    // one global object for uncacheableKeys LRUMap because it is only about the keys
    private final static Boolean DUMMY_VALUE = Boolean.TRUE;

    private volatile int invalidationCounter;

    public HstCacheEhCacheImpl(final Ehcache ehcache) {
        this(ehcache, new PersistenceConfiguration().strategy(PersistenceConfiguration.Strategy.NONE));
    }

    public HstCacheEhCacheImpl(final Ehcache ehcache, final PersistenceConfiguration persistenceConfiguration) {
        this.ehcache = ehcache;
        try {
            ehcache.getCacheConfiguration().addPersistence(persistenceConfiguration);
        } catch (InvalidConfigurationException e) {
            log.warn("Cannot add PersistenceConfiguration since deprecated 'overflowToDisk' or 'diskPersistent' is still " +
                    "configured.");
        }
    }

    @SuppressWarnings("unused")
    public void setStaleCache(final Cache staleCache) {
        this.staleCache = staleCache;
    }

    @SuppressWarnings("unused")
    public void setSecondLevelCache(final Cache secondLevelCache) {
        this.secondLevelCache = secondLevelCache;
        uncacheableKeys = new LRUMap(1000);
    }

    public CacheElement get(Object key) {
        Element element = ehcache.get(key);
        if (element != null) {
            if (log.isDebugEnabled()) {
                log.debug("Serving cached element created at '{}' from primary cache.", element.getCreationTime());
            }
            return new CacheElementEhCacheImpl(element);
        }
        if (secondLevelCache != null && !uncacheableKeys.containsKey(key)) {
            final Element secondLevelElement = secondLevelCache.get(key, Element.class);
            if (secondLevelElement != null) {
                // when this cache is decorated with eh blocking cache, we need to "put" with the exact same key instance
                // as used in the get, otherwise keys with the same hashcode/equals will still be blocked. Is very
                // opaque when using eh blocking cache. That is why we need to create a new 'Element' object
                // with the same key instance as in the get of this method

                final Element elementBasedOnKeyInstance = new Element(key, secondLevelElement.getObjectValue(), 1, secondLevelElement.getCreationTime(), 0, 0, 0);

                final long timeLived = (System.currentTimeMillis() - secondLevelElement.getCreationTime()) / 1000;

                // The page can be cached until it is evicted from the second level cache by the TTL of the second level
                // cached entry: When the entry is evicted from the second level cache, it also needs to be evicted from the primary cache
                // because otherwise in a clustered setup, different primary caches can start to contain different
                // cached responses resulting in alternating pages (per cluster node) which is not acceptable
                final long newTTL = secondLevelElement.getTimeToLive() - timeLived;

                if (newTTL <= 0) {
                    // if newTTL < 0 : just expired from second level cache
                    if (staleCache != null) {
                        // since we do support stale cached responses, let's inject this stale element in the primary
                        // ehcache and return null: The result is that the blocking lock is freed, resulting in other
                        // threads for the same key can continue with the stale result, and by returning null, the
                        // eventual updated value will be stored in primary cache
                        log.info("Temporarily restoring stale element in primary cache. Current thread will continue" +
                                " recreating the element to be cached which will later on replace the stale item in primary cache" +
                                " and stale page cache.");
                        ehcache.put(elementBasedOnKeyInstance);
                    }
                    return null;
                }

                elementBasedOnKeyInstance.setTimeToLive((int)newTTL);
                ehcache.put(elementBasedOnKeyInstance);
                return new CacheElementEhCacheImpl(elementBasedOnKeyInstance);
            }
        }
        if (staleCache != null) {
            final Element staleElement = staleCache.get(key, Element.class);
            if (staleElement != null) {
                // put in the primary ehcache the staleElement and return null : This way the
                // blocking lock on key for blocking caches will be released: From then on, other requests for the
                // same key get the stale response. We now return null, resulting in that the current thread continues
                // to recreate the element, which in the end, will replace the staleElement from primary cache
                log.info("Temporarily restoring stale element created at '{}' in primary cache. Current thread will continue" +
                        " recreating the element to be cached which will later on replace the stale item in primary cache.", staleElement.getCreationTime());
                // use key instance to clear the lock on the key instance
                final Element elementBasedOnKeyInstance = new Element(key, staleElement.getObjectValue(), 1, staleElement.getCreationTime(), 0, 0, 0);
                ehcache.put(elementBasedOnKeyInstance);
                return null;
            }
        }
        return null;
    }

    @Override
    public CacheElement get(final Object key, final Callable<? extends CacheElement> valueLoader) throws Exception {
        if (valueLoader == null) {
            throw new IllegalArgumentException("valueLoader is not allowed to be null");
        }
        CacheElement cached = get(key);
        if (cached != null) {
            return cached;
        }

        int preCallInvalidationCounter = invalidationCounter;
        // to make sure the lock is freed in case of blocking cache, make sure we start with an non null element,
        CacheElement element = null;
        try {
            element = valueLoader.call();
            if (element == null) {
                log.debug("valueLoader '{}#call()' did " +
                        "return null for key '{}'", valueLoader.getClass().getName(), key);
            }
        } finally {
            int postCallInvalidationCounter = invalidationCounter;
            // if exceptions (also unchecked) happened or the CacheElement is uncacheable, we put an empty element (createElement(key,null))
            // to make sure that if a blocking cache is used, the lock on the key is freed. Also see ehcache BlockingCache
            if (element == null) {
                element = createElement(key, null);
                put(element);
            } else if (postCallInvalidationCounter != preCallInvalidationCounter) {
                put(createElement(key, null));
                // the element is stale, so let's put it in stale cache if this one is present
                if (staleCache != null) {
                    CacheElementEhCacheImpl cacheElem = (CacheElementEhCacheImpl)element;
                    staleCache.put(cacheElem.element.getObjectKey(), cacheElem.element);
                }
            } else {
                put(element);
            }
        }

        return element;
    }

    public int getTimeToIdleSeconds() {
        return (int)ehcache.getCacheConfiguration().getTimeToIdleSeconds();
    }

    public int getTimeToLiveSeconds() {
        return (int)ehcache.getCacheConfiguration().getTimeToLiveSeconds();
    }

    public boolean isKeyInCache(Object key) {
        return ehcache.isKeyInCache(key);
    }

    public void put(CacheElement element) {
        if (!element.isCacheable()) {
            final CacheElement uncacheable = createElement(element.getKey(), null);
            ehcache.put(((CacheElementEhCacheImpl)uncacheable).element);
            if (secondLevelCache != null && uncacheableKeys != null) {
                uncacheableKeys.put(element.getKey(), DUMMY_VALUE);
            }
            return;
        }
        CacheElementEhCacheImpl cacheElem = (CacheElementEhCacheImpl)element;
        ehcache.put(cacheElem.element);
        if (secondLevelCache != null) {
            secondLevelCache.put(cacheElem.getKey(), cacheElem.element);
        }
        if (staleCache != null) {
            staleCache.put(cacheElem.element.getObjectKey(), cacheElem.element);
        }
    }

    public CacheElement createElement(Object key, Object content) {
        return new CacheElementEhCacheImpl(key, content);
    }

    public CacheElement createUncacheableElement(Object key, Object content) {
        return new CacheElementEhCacheImpl(key, content, false);
    }

    public boolean remove(Object key) {
        return ehcache.remove(key);
    }

    public void clear() {
        invalidationCounter++;
        ehcache.removeAll();
    }

    public int getSize() {
        return ehcache.getSize();
    }

    public int getMaxSize() {
        return new Long(ehcache.getCacheConfiguration().getMaxEntriesLocalHeap()).intValue()
                + (int)ehcache.getCacheConfiguration().getMaxEntriesLocalDisk();
    }

}
