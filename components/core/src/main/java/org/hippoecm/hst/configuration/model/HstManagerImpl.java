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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.EventIterator;

import org.hippoecm.hst.cache.HstCache;
import org.hippoecm.hst.configuration.cache.HstEventsDispatcher;
import org.hippoecm.hst.configuration.cache.HstNodeLoadingCache;
import org.hippoecm.hst.configuration.channel.MutableChannelManager;
import org.hippoecm.hst.configuration.hosting.MutableVirtualHosts;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.hosting.VirtualHostsService;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstComponentRegistry;
import org.hippoecm.hst.core.internal.StringPool;
import org.hippoecm.hst.core.jcr.RuntimeRepositoryException;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandlerFactory;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandlerRegistry;
import org.hippoecm.hst.service.ServiceException;
import org.hippoecm.hst.site.HstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstManagerImpl implements MutableHstManager {
    
    private static final Logger log = LoggerFactory.getLogger(HstManagerImpl.class);

    private Object hstModelMutex;

    private volatile VirtualHosts prevHstModel;
    private volatile VirtualHosts hstModel;


    private volatile BuilderState state = BuilderState.UNDEFINED;

    enum BuilderState {
        UNDEFINED,
        UP2DATE,
        FAILED,
        STALE,
        SCHEDULED,
        RUNNING,
    }
    
    private volatile int consecutiveBuildFailCounter = 0;

    private boolean staleConfigurationSupported = false;

    private HstURLFactory urlFactory;
    private HstSiteMapMatcher siteMapMatcher;
    private HstSiteMapItemHandlerFactory siteMapItemHandlerFactory;
    
    private HstComponentRegistry componentRegistry;
    private HstSiteMapItemHandlerRegistry siteMapItemHandlerRegistry;
    private HstCache pageCache;

    private HstNodeLoadingCache hstNodeLoadingCache;
    private HstEventsDispatcher hstEventsDispatcher;

    /**
     *
     * the default cms preview prefix : The prefix all URLs when accessed through the CMS 
     */
    private String cmsPreviewPrefix;

    /**
     * Request path suffix delimiter
     */
    private String pathSuffixDelimiter = "./";

    private String[] hstFilterPrefixExclusions;
    private String[] hstFilterSuffixExclusions;
    
    /**
     * The list of implicit configuration augmenters which can provide extra hst configuration after the {@link VirtualHosts} object 
     * has been created
     */
    List<HstConfigurationAugmenter> hstConfigurationAugmenters = new ArrayList<HstConfigurationAugmenter>();

    private MutableChannelManager channelManager;

    public void setHstModelMutex(Object hstModelMutex) {
        this.hstModelMutex = hstModelMutex;
    }

    public void setHstNodeLoadingCache(HstNodeLoadingCache hstNodeLoadingCache) {
        this.hstNodeLoadingCache = hstNodeLoadingCache;
    }

    public void setHstEventsDispatcher(final HstEventsDispatcher hstEventsDispatcher) {
        this.hstEventsDispatcher = hstEventsDispatcher;
    }

    public void setComponentRegistry(HstComponentRegistry componentRegistry) {
        this.componentRegistry = componentRegistry;
    }
    
    public void setSiteMapItemHandlerRegistry(HstSiteMapItemHandlerRegistry siteMapItemHandlerRegistry) {
        this.siteMapItemHandlerRegistry = siteMapItemHandlerRegistry;
    }

    public void setPageCache(HstCache pageCache) {
        this.pageCache = pageCache;
    }
    
    public synchronized String getCmsPreviewPrefix() {
        return cmsPreviewPrefix;
    }

    public synchronized void setCmsPreviewPrefix(String cmsPreviewPrefix) {
        this.cmsPreviewPrefix = cmsPreviewPrefix;
    }
    
    public void setUrlFactory(HstURLFactory urlFactory) {
        this.urlFactory = urlFactory;
    }

    public HstURLFactory getUrlFactory() {
        return this.urlFactory;
    }
    
    public void setSiteMapMatcher(HstSiteMapMatcher siteMapMatcher) {
        this.siteMapMatcher = siteMapMatcher;
    }
    
    public HstSiteMapMatcher getSiteMapMatcher() {
        return siteMapMatcher;
    }
    
    @Override
    public List<HstConfigurationAugmenter> getHstConfigurationAugmenters() {
        return hstConfigurationAugmenters;
    }

    /**
     * Adds <code>hstConfigurationProvider</code> to {@link #hstConfigurationAugmenters}
     * @param augmenter
     */
    public void addHstConfigurationAugmenter(HstConfigurationAugmenter augmenter) {
        hstConfigurationAugmenters.add(augmenter);
    }
    
    public void setSiteMapItemHandlerFactory(HstSiteMapItemHandlerFactory siteMapItemHandlerFactory) {
        this.siteMapItemHandlerFactory = siteMapItemHandlerFactory;
    }
    
    public HstSiteMapItemHandlerFactory getSiteMapItemHandlerFactory() {
        return siteMapItemHandlerFactory;
    } 
    
    public String getPathSuffixDelimiter() {
        return pathSuffixDelimiter;
    }
    
    public void setPathSuffixDelimiter(String pathSuffixDelimiter) {
        this.pathSuffixDelimiter = pathSuffixDelimiter;
    }

    public void setChannelManager(MutableChannelManager channelManager) {
        this.channelManager = channelManager;
    }

    public void setHstFilterPrefixExclusions(final String[] hstFilterPrefixExclusions) {
        this.hstFilterPrefixExclusions = hstFilterPrefixExclusions;
    }

    public void setHstFilterSuffixExclusions(final String[] hstFilterSuffixExclusions) {
        this.hstFilterSuffixExclusions = hstFilterSuffixExclusions;
    }

    public void setStaleConfigurationSupported(boolean staleConfigurationSupported) {
        log.info("Is stale configuraion for HST model supported : '{}'", staleConfigurationSupported);
        this.staleConfigurationSupported = staleConfigurationSupported;
    }

    public boolean isExcludedByHstFilterInitParameter(String pathInfo) {
        if (hstFilterPrefixExclusions != null) {
            for(String excludePrefix : hstFilterPrefixExclusions) {
                if(pathInfo.startsWith(excludePrefix)) {
                    log.debug("pathInfo '{}' is excluded by init parameter containing excludePrefix '{}'", pathInfo, excludePrefix);
                    return true;
                }
            }
        }
        if (hstFilterSuffixExclusions != null) {
            for(String excludeSuffix : hstFilterSuffixExclusions) {
                if(pathInfo.endsWith(excludeSuffix)) {
                    log.debug("pathInfo '{}' is excluded by init parameter containing excludeSuffix '{}'", pathInfo, excludeSuffix);
                    return true;
                }
            }
        }
        return false;
    }

    private void asynchronousBuild() {
        synchronized (hstModelMutex) {
            if (state == BuilderState.UP2DATE) {
                // other thread already built the model
                return;
            }
            if (state == BuilderState.SCHEDULED) {
                // already scheduled
                return;
            } 
            if (state == BuilderState.RUNNING) {
                log.error("BuilderState should not be possible to be in RUNNING state at this point. Return");
                return;
            }
            state = BuilderState.SCHEDULED;
            log.info("Asynchronous hst model build will be scheduled");
            Thread scheduled = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        long reloadDelay = computeReloadDelay(consecutiveBuildFailCounter);
                        if (reloadDelay > 0) {
                            Thread.sleep(reloadDelay);
                        }
                        synchronousBuild();
                    } catch (ContainerException e) {
                        log.warn("Exception during building virtualhosts model. ", e);
                    } catch (InterruptedException e) {
                        log.error("InterruptedException ", e);
                    }
                }
            });
            scheduled.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(final Thread t, final Throwable e) {
                    log.warn("Runtime exception "+e.getClass().getName()+" during building asynchronous " +
                            "HST model. Reason : " + e.getMessage(), e);
                }
            });
            scheduled.start();
        }
    }

    @Override
    public VirtualHosts getVirtualHosts(boolean allowStale) throws ContainerException {
        if (state == BuilderState.UP2DATE) {
            return hstModel;
        }
        if (state == BuilderState.UNDEFINED) {
            return synchronousBuild();
        }
        if (allowStale && staleConfigurationSupported) {
            asynchronousBuild();
            return prevHstModel;
        }
        return synchronousBuild();
    }

    @Override
    public VirtualHosts getVirtualHosts() throws ContainerException {
        return getVirtualHosts(false);
    }

    private VirtualHosts synchronousBuild() throws ContainerException {
        if (state != BuilderState.UP2DATE) {
            synchronized (hstModelMutex) {
                if (state == BuilderState.UP2DATE) {
                    return hstModel;
                } else {
                    try {
                        state = BuilderState.RUNNING;
                        try {
                            buildSites();
                            state = BuilderState.UP2DATE;
                        } catch (ModelLoadingException | ContainerException e) {
                            log.warn("Model was possibly not build correctly. A total rebuild will be done now after flushing all caches.", e);
                            invalidateAll();
                            state = BuilderState.FAILED;
                            consecutiveBuildFailCounter++;
                            return prevHstModel;
                        }
                    } finally {
                        if (state == BuilderState.RUNNING) {
                            log.warn("Model failed to built. Serve old virtualHosts model.");
                            consecutiveBuildFailCounter++;
                            state = BuilderState.FAILED;
                        }
                    }
                    if (state == BuilderState.FAILED) {
                        // do not flush pageCache but return old prev virtual host instance instead
                        return prevHstModel;
                    }
                    log.info("Flushing page cache after new model is loaded");
                    pageCache.clear();
                }
                if (state == BuilderState.UP2DATE) {
                    consecutiveBuildFailCounter = 0;
                    prevHstModel = hstModel;
                }
                return hstModel;
            }
        }
        return hstModel;
    }

    private long computeReloadDelay(final int consecutiveBuildFailCounter) {
        switch (consecutiveBuildFailCounter) {
            case 0 : return 0L;
            case 1 : return 0L;
            case 2 : return 100L;
            case 3 : return 1000L;
            case 4 : return 10000L;
            case 5 : return 30000L;
            default : return 60000L;
        }
    }

    private void buildSites() throws ContainerException {

        hstEventsDispatcher.dispatchHstEvents();

        log.info("Start building in memory hst configuration model");


        try {

            // TODO only when needed
            componentRegistry.unregisterAllComponents();
            try {

                // TODO only when needed
                siteMapItemHandlerRegistry.unregisterAllSiteMapItemHandlers();

                long start = System.currentTimeMillis();
                hstModel = new VirtualHostsService(this, hstNodeLoadingCache);
                log.info("Loading VirtualHostsService took '{}' ms.", (System.currentTimeMillis() - start));

                for(HstConfigurationAugmenter configurationAugmenter : hstConfigurationAugmenters ) {
                    configurationAugmenter.augment((MutableVirtualHosts)hstModel);
                }

                // TODO remove session usage here and load channel manager with HstNode only
                Session session = null;
                try {
                    session = getSession();
                    this.channelManager.load(hstModel, session);
                } finally {
                    if (session != null) {
                        session.logout();
                    }
                }

                log.info("Finished build in memory hst configuration model in '{}' ms.", (System.currentTimeMillis() - start));
            } catch (ServiceException e) {
                throw new ContainerException("Exception during building HST model", e);
            } catch (Exception e) {
                throw new ModelLoadingException("Could not load hst node model due to Runtime Exception :", e);
            }
        } catch (RuntimeRepositoryException e) {
            throw new ContainerException("RepositoryException during building HST model", e);
        } finally {
            // clear the StringPool as it is not needed any more
            // TODO After lazy loading is implemented, we should StringPool clear smarter...
            StringPool.clear();
        }
    }

    private Session getSession() throws RepositoryException {
        Credentials defaultCredentials = HstServices.getComponentManager().getComponent(Credentials.class.getName() + ".hstconfigreader");
        Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName());

        Session session = null;
        if (repository != null) {
             session = repository.login(defaultCredentials);

        }
        return session;
    }

    @Deprecated
    @Override
    public void invalidate(EventIterator events) {
        log.warn("deprecated. Not used any more ");
    }

    @Deprecated
    @Override
    public void invalidate(final String... absEventPaths) {
        log.warn("deprecated. Not used any more ");
    }

    @Override
    public void invalidateAll() {
        log.warn("deprecated. Not used any more ");
    }

    @Override
    public void markStale() {
        synchronized (hstModelMutex) {
            if (state != BuilderState.UNDEFINED) {
                state = BuilderState.STALE;
            }
        }
    }

}
