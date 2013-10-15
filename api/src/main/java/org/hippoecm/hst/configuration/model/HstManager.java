/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.model;

import java.util.List;

import javax.jcr.observation.EventIterator;

import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandler;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandlerFactory;

public interface HstManager {

    /**
     * @return the <code>VirtualHosts</code>, aka the HST model.
     */
    VirtualHosts getVirtualHosts() throws ContainerException;

    /**
     * When <code>allowStale</code> is <code>true</code> a stale {@link VirtualHosts} might be returned. This might be favoured
     * over {@link #getVirtualHosts()} when the model is very large and thus might take longer to reload.
     * @return the <code>VirtualHosts</code> object and possibly a stale version of it when <code>allowStale</code> is
     * <code>true</code>
     */
    VirtualHosts getVirtualHosts(boolean allowStale) throws ContainerException;

    /**
     * @return the HstURLFactory
     */
    HstURLFactory getUrlFactory();
    
    /**
     * a HstSitesManager must contain a reference to the {@link HstSiteMapMatcher} that is being used. You can inject your own
     *  {@link HstSiteMapMatcher} implementation if needed
     * @return the global HstSiteMapMatcher implementation used for all the hosts & sites
     */
    HstSiteMapMatcher getSiteMapMatcher();
    
    /**
     * @return the siteMapItemHandler factory which can create {@link HstSiteMapItemHandler} instances
     */
    HstSiteMapItemHandlerFactory getSiteMapItemHandlerFactory();
    
    /**
     * Invalidates this HstSitesManager with EventIterator events. Typically this invalidate is called after a received event indicating that for example
     * the backing hosts configuration has been changed.
     * @param events
     * @deprecated deprecated since 7.9.0 Use {@link xxx} object instead
     */
    @Deprecated
    void invalidate(EventIterator events);

    /**
     * Invalidates this HstSitesManager with all the varargs absEventPaths. This is useful in cases where you directly want to
     * invalidate the hst model and cannot wait for asynchronous jcr events to be dispatched.
     * @param absEventPaths the varargs abs paths of the event that should be invalidated
     * @deprecated deprecated since 7.9.0 Use {@link xxx} object instead
     */
    @Deprecated
    void invalidate(String... absEventPaths);
    
    /**
     * Invalidates this HstSitesManager completely. For example useful after a 
     * repository reconnection where you do not know whether some events might have been missed
     */
    void invalidateAll();

    /**
     *
     * Typically, some paths we do not want to be handle by the hst framework request processing. Typically, this would
     * be for example paths starting with /binaries/, or paths ending with some extension, like .pdf
     *
     * When a path must be excluded, this method return true.
     *
     * @param pathInfo
     * @return true when the path must be excluded for matching to a host.
     */
    boolean isExcludedByHstFilterInitParameter(String pathInfo);

    /**
     * Returns the request path suffix delimiter.
     * @return
     */
    String getPathSuffixDelimiter();
    
    /**
     * @return the {@link List} of {@link HstConfigurationAugmenter}s and empty list if no providers available.
     */
    List<HstConfigurationAugmenter> getHstConfigurationAugmenters();

    /**
     * marks that the hst model is dirty
     */
    void markStale();
}