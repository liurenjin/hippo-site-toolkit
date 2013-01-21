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

import javax.jcr.Session;

import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.configuration.channel.ChannelManager;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.model.HstManagerImpl;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.test.AbstractSpringTestCase;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ConcurrentChannelManagerAndHstManagerLoadTest extends AbstractSpringTestCase {

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
		// even though async, if the model is not built before, the async built is sync
		final VirtualHosts asyncVirtualHosts = hstManager.getVirtualHosts(true);
		assertNotNull(asyncVirtualHosts);
	}

	@Test
	public void testHstManagerConcurrentSynchronousLoad() throws Exception {
		try {
			Collection<Callable<Object>> jobs = new ArrayList<Callable<Object>>(100);
			for (int i = 0; i < 100; i++) {
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
			Collection<Callable<Object>> jobs = new ArrayList<Callable<Object>>(100);
			final Random random = new Random();
			for (int i = 0; i < 100; i++) {
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
			Collection<Callable<Object>> jobs = new ArrayList<Callable<Object>>(100);
			final Random random = new Random();
			for (int i = 0; i < 100; i++) {
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
                    // channelManager.getChannels() clones all existing channels hence no instance identity
					assertTrue(currentChannel.equals(nextChannel));
				}
			}
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
            e.printStackTrace();
			fail(e.toString());
		}
	}

	private final static String TEST_PROP =  "ConcurrentChannelManagerAndHstManagerLoadTest.testProp";

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


	public static interface TestChannelInfo extends ChannelInfo {

		@Parameter(name = TEST_PROP, defaultValue = "0")
		Long getTitle();

	}

}
