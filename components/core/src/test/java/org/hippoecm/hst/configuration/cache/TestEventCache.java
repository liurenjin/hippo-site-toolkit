/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.configuration.cache;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.junit.Test;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertTrue;



public class TestEventCache {

    @Test
    public void testReferenceQueue() throws Exception {
        Map<String, WeakReference<FinalizableTinyObject>> configurationObjectEventRegistry = new HashMap<>();
        final ReferenceQueue<FinalizableTinyObject> integerReferenceQueue = new ReferenceQueue<>();
        Counter finalizedObjectsCounter = new Counter();
        final int numberOfObjects = 1000;
        for (int i = 0 ; i < numberOfObjects; i++) {
            configurationObjectEventRegistry.put("test" + i, new WeakReference<>(new FinalizableTinyObject(finalizedObjectsCounter), integerReferenceQueue));
        }

        // make sure all WeakReference referents are GC-ed
        while (finalizedObjectsCounter.finalized.get() != numberOfObjects) {
            System.gc();
            Thread.sleep(100);
            System.gc();
        }
        int queueLength = 0;
        Reference<? extends FinalizableTinyObject> poll = integerReferenceQueue.poll();
        while (poll != null) {
            queueLength++;
            poll = integerReferenceQueue.poll();
        }
        assertTrue(queueLength == numberOfObjects);
    }

    @Test
    public void testEventCache() throws InterruptedException {
        final EventCache<FinalizableTinyKey, FinalizableTinyObject, String> eventCache = new EventCache<>();
        final Counter finalizedObjectsCounter = new Counter();
        final Counter finalizedKeyCounter = new Counter();
        final int numberOfObjects = 1000;
        for (int i = 0 ; i < numberOfObjects; i++) {
            eventCache.put(new FinalizableTinyKey("key"+i, finalizedKeyCounter), new FinalizableTinyObject(finalizedObjectsCounter), "event"+i);
        }

        // this one should not be able to be GC-ed
        FinalizableTinyObject tinyObject =  new FinalizableTinyObject(finalizedObjectsCounter);
        eventCache.put(new FinalizableTinyKey("unGarbagableKey", finalizedKeyCounter), tinyObject, "unGarbagableEvent");
        while (finalizedObjectsCounter.finalized.get() != numberOfObjects) {
            System.out.println(finalizedObjectsCounter.finalized.get());
            System.gc();
            Thread.sleep(100);
            System.gc();
        }

        // the cache is only cleaned on next access
        assertTrue(eventCache.keyToValueMap.size() == numberOfObjects + 1);
        assertTrue(eventCache.valueKeyMap.size() == numberOfObjects + 1);

        // since the cachekey are strongly referenced in the EventCache and the EventCache did not yet had a cleanup()
        // there still can't be finalized a single cachekey
        Assert.assertTrue(finalizedKeyCounter.finalized.get() == 0);

        // access the cache for force cleanup
        eventCache.get(new FinalizableTinyKey("foo", null));

        assertTrue(eventCache.keyToValueMap.size() == 1);
        assertTrue(eventCache.valueKeyMap.size() == 1);

        // now, all items should have been evicted *EXCEPT* the one we still reference
        for (int i = 0 ; i < numberOfObjects; i++) {
            assertNull("since object is gc-ed, it should not be present any more",eventCache.get(new FinalizableTinyKey("key" + i, null)));
        }

        // the registry is only cleaned on next access when all keys have been GC-ed
        assertTrue(eventCache.eventCacheKeyRegistry.cacheKeyEventMap.size() == numberOfObjects + 1);
        assertTrue(eventCache.eventCacheKeyRegistry.eventCacheKeysMap.size() == numberOfObjects + 1);

        while (finalizedKeyCounter.finalized.get() != numberOfObjects) {
            System.gc();
            Thread.sleep(100);
            System.gc();
        }

        // only AFTER eventCache.get("foo") the STRONG KEYS have been removed from eventCache and now
        // have become available for the garbage collector. After they have been GC-ed, the eventCacheKeyRegistry
        // should be cleaned up

        // access the cache for force cleanup
        eventCache.eventCacheKeyRegistry.get("foo");
        assertTrue(eventCache.eventCacheKeyRegistry.cacheKeyEventMap.size() ==  1);
        assertTrue(eventCache.eventCacheKeyRegistry.eventCacheKeysMap.size() == 1);


        assertNotNull(eventCache.get(new FinalizableTinyKey("unGarbagableKey", null)));
        assertTrue(tinyObject == eventCache.get(new FinalizableTinyKey("unGarbagableKey", null)));

        eventCache.handleEvent("unGarbagableEvent");
        assertNull(eventCache.get(new FinalizableTinyKey("unGarbagableKey", null)));

    }

    /**
     * keep the cache running for a while and store big keys and objects all time : This should
     * not lead to memory issues when the keys/objects are ready for gc
     */
    @Test
    public void testEventCacheMemoryUsage() {
        final EventCache<FinalizableBigKey, FinalizableBigObject, String> eventCache = new EventCache<>();
        // per key and per object about 10 Mbyte, so 1000 * 20 Mbyte = 20 Gbyte should expose memory issues
        final int numberOfObjects = 1000;
        for (int i = 0 ; i < numberOfObjects; i++) {
            eventCache.put(new FinalizableBigKey("key"+i), new FinalizableBigObject(), "event"+i);
        }
        assertTrue("No OOM", true);
    }


    public class FinalizableTinyKey {

        final Counter c;
        final String key;

        public FinalizableTinyKey(final String key, final Counter c) {
            this.c = c;
            this.key = key;
        }

        @Override
        protected void finalize() throws Throwable {
            c.finalized.incrementAndGet();
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final FinalizableTinyKey that = (FinalizableTinyKey) o;

            if (!key.equals(that.key)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }
    }

    public class FinalizableBigKey {

        final String key;
        // about 10 Mbyte
        final byte[] bigMemoryTaker = new byte[10 * 1024 * 1024];

        public FinalizableBigKey(final String key) {
            this.key = key;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final FinalizableTinyKey that = (FinalizableTinyKey) o;

            if (!key.equals(that.key)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }
    }


    class FinalizableTinyObject{

        final Counter c;
        FinalizableTinyObject(final Counter c) {
            this.c = c;
        }

        @Override
        protected void finalize() throws Throwable {
            c.finalized.incrementAndGet();
        }
    }

    class FinalizableBigObject{

        // about 10 Mbyte
        final byte[] bigMemoryTaker = new byte[10 * 1024 * 1024];

    }

    class Counter {
        volatile AtomicInteger finalized = new AtomicInteger();
    }
}
