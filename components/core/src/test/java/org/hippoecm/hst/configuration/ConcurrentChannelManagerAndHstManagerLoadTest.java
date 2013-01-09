/*
 *  Copyright 2013 Hippo.
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
package org.hippoecm.hst.configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.configuration.channel.ChannelManager;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.model.HstManagerImpl;
import org.hippoecm.hst.core.container.CmsJcrSessionThreadLocal;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.test.AbstractTestConfigurations;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ConcurrentChannelManagerAndHstManagerLoadTest extends AbstractTestConfigurations {

    private HstManager hstManager;
    private ChannelManager channelManager;
    private List<Session> sessionList = null;

    private enum Job {
        GET_VIRTUALHOSTS_SYNC,
        GET_VIRTUALHOSTS_ASYNC,
        GET_CHANNELS,
        MODIFY_CHANNEL,
        MODIFY_HSTHOSTS
    }

    final Job[] enumJobs = Job.values();

    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.hstManager = getComponent(HstManager.class.getName());
        ((HstManagerImpl)hstManager).setStaleConfigurationSupported(true);
        this.channelManager = getComponent(ChannelManager.class.getName());
    }

    @Test
    public void testHstManagerASynchronousFirstLoad() throws Exception {
        // even though async, if the model is not built before, the async built is sync
        final VirtualHosts asyncVirtualHosts = hstManager.getVirtualHosts(true);
        assertNotNull(asyncVirtualHosts);
    }

    @Test
    public void testHstManagerASynchronousFirstLoadAfterEvent() throws Exception {
        hstManager.invalidate("/hst:hst/hst:hosts");
        // even though async, if the model is not built before, the async built is sync
        final VirtualHosts asyncVirtualHosts = hstManager.getVirtualHosts(true);
        assertNotNull(asyncVirtualHosts);
    }

    @Test
    public void testHstManagerConcurrentSynchronousLoad() throws Exception {
        try {
            Collection<Callable<Object>> jobs = new ArrayList<Callable<Object>>(500);
            for (int i = 0; i < 500; i++) {
                jobs.add(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        return hstManager.getVirtualHosts();
                    }
                });
            }
            final Collection<Future<Object>> futures = executeAllJobs(jobs, 50);
            VirtualHosts current = null;
            for (Future<Object> future : futures) {
                if (!future.isDone()) {
                    fail("unfinished jobs");
                }
                VirtualHosts next = (VirtualHosts)future.get();
                if (current == null) {
                    current = next;
                    continue;
                }
                assertTrue(current == next);
            }
        } catch (AssertionError e) {
            throw e;
        } catch (Throwable e) {
            fail(e.toString());
        }
    }

    @Test
    public void testConcurrentHstManagerSynchronousAndAsynchronousLoad() throws Exception {
        try {
            Collection<Callable<Object>> jobs = new ArrayList<Callable<Object>>(500);
            final Random random = new Random();
            for (int i = 0; i < 500; i++) {
                final boolean allowStale;
                Job randomJob = enumJobs[random.nextInt(2)];
                switch (randomJob) {
                    case GET_VIRTUALHOSTS_SYNC:
                        allowStale = false;
                        break;
                    case GET_VIRTUALHOSTS_ASYNC:
                        allowStale = true;
                        break;
                    default :
                        allowStale = false;
                }
                jobs.add(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        return hstManager.getVirtualHosts(allowStale);
                    }
                });
            }
            final Collection<Future<Object>> futures = executeAllJobs(jobs, 50);
            VirtualHosts current = null;
            for (Future<Object> future : futures) {
                if (!future.isDone()) {
                    fail("unfinished jobs");
                }
                VirtualHosts next = (VirtualHosts)future.get();
                if (current == null) {
                    current = next;
                    continue;
                }
                assertTrue(current == next);
            }
        } catch (AssertionError e) {
            throw e;
        } catch (Throwable e) {
            fail(e.toString());
        }
    }


    @Test
    public void testConcurrentSyncAndAsyncHstManagerAndChannelManagerLoad() throws Exception {
        try {
            Collection<Callable<Object>> jobs = new ArrayList<Callable<Object>>(500);
            final Random random = new Random();
            for (int i = 0; i < 500; i++) {
                int rand = random.nextInt(3);
                Job randomJob = enumJobs[rand];
                switch (randomJob) {
                    case GET_VIRTUALHOSTS_SYNC:
                        jobs.add(new Callable<Object>() {
                            @Override
                            public VirtualHosts call() throws Exception {
                                return hstManager.getVirtualHosts();
                            }
                        });
                        break;
                    case GET_VIRTUALHOSTS_ASYNC:
                        jobs.add(new Callable<Object>() {
                            @Override
                            public VirtualHosts call() throws Exception {
                                return hstManager.getVirtualHosts(true);
                            }
                        });
                        break;
                    case GET_CHANNELS:
                        jobs.add(new Callable<Object>() {
                            @Override
                            public Object call() throws Exception {
                                return channelManager.getChannels();
                            }
                        });
                        break;
                    default :
                        break;
                }
            }
            final Collection<Future<Object>> futures = executeAllJobs(jobs, 50);
            VirtualHosts currentHost = null;
            VirtualHosts nextHost;
            Channel currentChannel = null;
            Channel nextChannel;
            
            for (Future<Object> future : futures) {
                if (!future.isDone()) {
                    fail("unfinished jobs");
                }
                Object o = future.get();
                if (o instanceof VirtualHosts) {
                    nextHost = (VirtualHosts) o;
                    if (currentHost == null) {
                        currentHost = nextHost;
                        continue;
                    }
                    assertTrue(currentHost == nextHost);
                } else {
                    Map<String, Channel> channelMap = (Map<String, Channel>) o;
                    assertTrue("Expected only one channel configured in unit test data ",channelMap.size() == 1);
                    nextChannel = channelMap.values().iterator().next();
                    if (currentChannel == null) {
                        currentChannel = nextChannel;
                        continue;
                    }
                    assertTrue(currentChannel == nextChannel);
                }
            }
        } catch (AssertionError e) {
            throw e;
        } catch (Throwable e) {
            fail(e.toString());
        }
    }

    private final static String TEST_PROP =  "ConcurrentChannelManagerAndHstManagerLoadTest.testProp";
    
    @Test
    public void testHstManagerLoadingAfterConfigChanges() throws Exception {
        populateSessions(2);
        Node mountNode = getSession1().getNode("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");
        int counter = 0;
        mountNode.setProperty(TEST_PROP, "testVal"+counter);
        mountNode.getSession().save();
        // load the model first ones to make sure async model is really async
        hstManager.getVirtualHosts();
        try {
            final int synchronousJobCount = 100;
            Collection<JobResultWrapperModifyMount> results = new ArrayList<JobResultWrapperModifyMount>(synchronousJobCount);
            for (int i = 0; i < synchronousJobCount; i++) {
                String prevVal = "testVal"+counter;
                counter++;
                String nextVal = "testVal"+counter;
                mountNode.setProperty(TEST_PROP, nextVal);
                mountNode.getSession().save();
                // Make sure to directly invalidate and do not wait for jcr event which is async and might arrive too late
                hstManager.invalidate(mountNode.getPath());

                JobResultWrapperModifyMount result = new JobResultWrapperModifyMount();
                result.testPropBeforeChange = prevVal;
                result.testPropAfterChange = nextVal;
                // ASYNC load
                result.asyncLoadedHosts = hstManager.getVirtualHosts(true);
                // SYNC load
                result.syncLoadedHosts = hstManager.getVirtualHosts();
                results.add(result);
            }

            for (JobResultWrapperModifyMount result : results) {
                {
                    assertFalse(result.testPropAfterChange.equals(result.testPropBeforeChange));
                    Mount mountFromSyncModel = result.syncLoadedHosts.matchMount("localhost", "/site", "").getMount();
                    assertTrue(mountFromSyncModel.getProperty(TEST_PROP).equals(result.testPropAfterChange));

                    // because the jobs above are done in a synchronous loop (single threaded) and AFTER every ASYNC
                    // there is a SYNC load, we expect that the ASYNC model in this case is ALWAYS ONE instance behind
                    // Note that this does not hold in concurrent loading as below in
                    // #testConcurrentSyncAndAsyncHstManagerAndChannelManagerWithConfigChanges
                    Mount mountFromASyncModel = result.asyncLoadedHosts.matchMount("localhost","/site", "").getMount();
                    assertTrue("The async model should be one version behind",
                            mountFromASyncModel.getProperty(TEST_PROP).equals(result.testPropBeforeChange));
                }
            }

        } catch (AssertionError e) {
            throw e;
        } catch (Throwable e) {
            e.printStackTrace();
            fail(e.toString());
        } finally {
            mountNode.getProperty(TEST_PROP).remove();
            mountNode.getSession().save();
            logoutSessions(sessionList);
        }
    }

    @Test
    public void testChannelsLoadingAfterSaving() throws Exception {
        final AtomicLong channelModCount = new AtomicLong(0);
        final Map<String, Channel> channels = channelManager.getChannels();
        final Channel existingChannel = channels.values().iterator().next();
        try {
            final int synchronousJobCount = 100;
            Collection<JobResultWrapperModifyChannel> results = new ArrayList<JobResultWrapperModifyChannel>(synchronousJobCount);
            for (int i = 0; i < synchronousJobCount; i++) {
                Long newTestValue = channelModCount.incrementAndGet();
                CmsJcrSessionThreadLocal.setJcrSession(getSession2());
                // need to set the channel info to be able to store properties
                existingChannel.setChannelInfoClassName(ConcurrentChannelManagerAndHstManagerLoadTest.class.getCanonicalName() + "$" + TestChannelInfo.class.getSimpleName());
                existingChannel.getProperties().put(TEST_PROP, newTestValue);
                channelManager.save(existingChannel);

                CmsJcrSessionThreadLocal.clearJcrSession();
                // get channel must always reflect LATEST version.
                
                final Map<String, Channel> loadedChannels = channelManager.getChannels();

                JobResultWrapperModifyChannel result = new JobResultWrapperModifyChannel();
                result.loadedChannels = loadedChannels;
                result.expectedNewTestValue = newTestValue;
                results.add(result);
            }

            for (JobResultWrapperModifyChannel result : results) {
                assertTrue(result.expectedNewTestValue.equals(result.loadedChannels.values().iterator().next().getProperties().get(TEST_PROP)));
            }
        } finally {
            existingChannel.getProperties().remove(TEST_PROP);
            CmsJcrSessionThreadLocal.setJcrSession(getSession1());
            channelManager.save(existingChannel);
            CmsJcrSessionThreadLocal.clearJcrSession();
        }
    }
    
    @Test
    public void testConcurrentSyncAndAsyncHstManagerAndChannelManagerWithConfigChanges() throws Exception {
        populateSessions(2);
        Node mountNode = getSession1().getNode("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");
        final AtomicInteger counter = new AtomicInteger(0);
        mountNode.setProperty(TEST_PROP, "testVal"+counter);
        mountNode.getSession().save();
        
        final Map<String, Channel> channels = channelManager.getChannels();
        assertTrue(channels.size() == 1);
        final Channel existingChannel = channels.values().iterator().next();
        try {
            final int jobCount = 1000;
            Collection<Callable<Object>> jobs = new ArrayList<Callable<Object>>(jobCount);
            final Random random = new Random();
            final AtomicLong channelModCount = new AtomicLong(0);
            final Object MUTEX = new Object();
            for (int i = 0; i < jobCount; i++) {
                int rand = random.nextInt(5);
                Job randomJob = enumJobs[rand];
                switch (randomJob) {
                    case GET_VIRTUALHOSTS_SYNC:
                        jobs.add(new Callable<Object>() {
                            @Override
                            public Boolean call() throws Exception {
                                hstManager.getVirtualHosts();
                                return Boolean.TRUE;
                            }
                        });
                        break;
                    case GET_VIRTUALHOSTS_ASYNC:
                        jobs.add(new Callable<Object>() {
                            @Override
                            public Boolean call() throws Exception {
                                hstManager.getVirtualHosts(true);
                                return Boolean.TRUE;
                            }
                        });
                        break;
                    case GET_CHANNELS:
                        jobs.add(new Callable<Object>() {
                            @Override
                            public Boolean call() throws Exception {
                                channelManager.getChannels();
                                return Boolean.TRUE;
                            }
                        });
                        break;
                    case MODIFY_CHANNEL:
                        jobs.add(new Callable<Object>() {
                            @Override
                            public Object call() throws Exception {
                                Long newTestValue = channelModCount.incrementAndGet();
                                // need to set the channel info to be able to store properties
                                existingChannel.setChannelInfoClassName(ConcurrentChannelManagerAndHstManagerLoadTest.class.getCanonicalName() + "$" + TestChannelInfo.class.getSimpleName());
                                existingChannel.getProperties().put(TEST_PROP, newTestValue);
                                CmsJcrSessionThreadLocal.setJcrSession(getSession2());
                                
                                channelManager.save(existingChannel);

                                CmsJcrSessionThreadLocal.clearJcrSession();
                                // get channel must always reflect LATEST version. Since this MODIFY_CHANNEL is 
                                // called concurrently, we can only guarantee that the loaded value for TEST_PROP
                                // is AT LEAST AS big as newTestValue
                                
                                final Map<String, Channel> loadedChannels = channelManager.getChannels();

                                JobResultWrapperModifyChannel result = new JobResultWrapperModifyChannel();
                                result.loadedChannels = loadedChannels;
                                result.expectedNewTestValue = newTestValue;
                                return result;
                            }
                        });
                        break;
                    case MODIFY_HSTHOSTS:
                        jobs.add(new Callable<Object>() {
                            @Override
                            public Object call() throws Exception {
                                synchronized (MUTEX) {
                                    String prevVal = "testVal" + counter.get();

                                    String nextVal = "testVal" + counter.incrementAndGet();
                                    Node node = getSession1().getNode("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");
                                    node.setProperty(TEST_PROP, nextVal);
                                    node.getSession().save();
                                    // Make sure to directly invalidate and do not wait for jcr event which is async and might arrive too late
                                    hstManager.invalidate(node.getPath());

                                    JobResultWrapperModifyMount result = new JobResultWrapperModifyMount();
                                    result.testPropBeforeChange = prevVal;
                                    result.testPropAfterChange = nextVal;
                                    // ASYNC load
                                    result.asyncLoadedHosts = hstManager.getVirtualHosts(true);
                                    // SYNC load
                                    result.syncLoadedHosts = hstManager.getVirtualHosts();
                                    return result;
                                }
                            }
                        });
                    default:
                        break;
                }
            }
            final Collection<Future<Object>> futures = executeAllJobs(jobs, 50);

            for (Future<Object> future : futures) {
                if (!future.isDone()) {
                    fail("unfinished jobs");
                }
                Object o = future.get();
                if (o instanceof Boolean) {
                    // nothing to check
                } else if (o instanceof JobResultWrapperModifyChannel) {
                    JobResultWrapperModifyChannel jobResultWrapperModifyChannel = (JobResultWrapperModifyChannel)o;
                    // since the model is loaded concurrently by multiple threads, the only thing we can guarantee is that
                    // the ACTUAL testValue on the channel is AT LEAST as big as the expectedNewTestValue (since channel 
                    // manager is synchronously loaded the channel will have a testValue that is at least as big as the 
                    // testValue that was saved just before the channel manager got reloaded)
                    Long valueFromChannel = (Long)jobResultWrapperModifyChannel.loadedChannels.values().iterator().next().getProperties().get(TEST_PROP);
                    assertTrue(valueFromChannel.longValue() >= jobResultWrapperModifyChannel.expectedNewTestValue.longValue());
                } else if (o instanceof JobResultWrapperModifyMount) {
                    JobResultWrapperModifyMount job = (JobResultWrapperModifyMount)o;
                    Mount mountFromSyncModel = job.syncLoadedHosts.matchMount("localhost", "/site", "").getMount();
                    // the sync model should always have the changed prop directly
                    assertTrue(mountFromSyncModel.getProperty(TEST_PROP).equals(job.testPropAfterChange));

                    // the async model is hard to predict which version it gets as it is loaded concurrently by many different
                    // threads. We can at least guarantee that there should match a mount
                    Mount mountFromASyncModel = job.asyncLoadedHosts.matchMount("localhost", "/site", "").getMount();
                    assertNotNull(mountFromASyncModel);
                }
            }

        } catch (AssertionError e) {
            throw e;
        } catch (Throwable e) {
            e.printStackTrace();
            fail(e.toString());
        } finally {
            existingChannel.getProperties().remove(TEST_PROP);
            CmsJcrSessionThreadLocal.setJcrSession(getSession1());
            channelManager.save(existingChannel);
            CmsJcrSessionThreadLocal.clearJcrSession();
            mountNode.getProperty(TEST_PROP).remove();
            mountNode.getSession().save();
            logoutSessions(sessionList);
        }
    }

    protected Collection<Future<Object>> executeAllJobs(final Collection<Callable<Object>> jobs, final int threads) throws InterruptedException {
        final ExecutorService executorService = Executors.newFixedThreadPool(threads);
        final List<Future<Object>> futures = executorService.invokeAll(jobs);
        executorService.shutdown();
        if (!executorService.awaitTermination(15, TimeUnit.SECONDS)) {
            executorService.shutdownNow(); // Cancel currently executing tasks
            // Wait a while for tasks to respond to being cancelled
            if (!executorService.awaitTermination(15, TimeUnit.SECONDS)) {
                fail("Pool did not terminate");
            }
        }
        return futures;
    }

    protected Session getSession1() {
        populateSessions(2);
        return sessionList.get(0);
    }

    protected Session getSession2() {
        populateSessions(2);
        return sessionList.get(1);
    }
    
    protected void populateSessions(int number) {
        if (sessionList != null) {
            return;
        }
        sessionList = new ArrayList<Session>(number);
        for (int i = 0; i < number; i++) {
            try {
                Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName() + ".delegating");
                Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
                sessionList.add(session);
            } catch (LoginException e) {
                e.printStackTrace();
            } catch (RepositoryException e) {
                e.printStackTrace();
            }
        }
    }

    protected void logoutSessions(List<Session> sessionList) {
        if (sessionList == null) {
            return;
        }
        for (Session session : sessionList) {
            session.logout();
        }
    }

    private class JobResultWrapperModifyMount {
        private String testPropBeforeChange;
        private String testPropAfterChange;
        private VirtualHosts asyncLoadedHosts;
        private VirtualHosts syncLoadedHosts;
    }

    private class JobResultWrapperModifyChannel {
        private Map<String, Channel> loadedChannels;
        private Long expectedNewTestValue;
    }

    public static interface TestChannelInfo extends ChannelInfo {

        @Parameter(name = TEST_PROP, defaultValue = "0")
        Long getTitle();

    }

}