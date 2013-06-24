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
package org.onehippo.cms7.hst.toolkit.addon.formdata;

import java.text.ParseException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.lock.LockManager;
import javax.jcr.observation.Event;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.simpl.RAMJobStore;
import org.quartz.simpl.SimpleThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FormDataCleanupModule extends AbstractReconfigurableDaemonModule {

    private static final Logger log = LoggerFactory.getLogger(FormDataCleanupModule.class);

    private static final String CONFIG_MODULECONFIGPATH = "moduleconfigpath";
    private static final String CONFIG_CRONEXPRESSION_PROPERTY = "cronexpression";
    private static final String CONFIG_MINUTES_TO_LIVE_PROPERTY = "minutestolive";
    private static final String CONFIG_LOCK_ISDEEP_PROPERTY = "jcr:lockIsDeep";
    private static final String CONFIG_LOCK_OWNER = "jcr:lockOwner";
    private static final long ONE_WEEK = 1000 * 60 * 60 * 24 * 7;

    private static final Properties SCHEDULER_FACTORY_PROPERTIES = new Properties();
    static {
        SCHEDULER_FACTORY_PROPERTIES.put(StdSchedulerFactory.PROP_SCHED_INSTANCE_NAME, "HippoFormDataCleanupScheduler");
        SCHEDULER_FACTORY_PROPERTIES.put(StdSchedulerFactory.PROP_SCHED_SKIP_UPDATE_CHECK, "true");
        SCHEDULER_FACTORY_PROPERTIES.put(StdSchedulerFactory.PROP_THREAD_POOL_CLASS, SimpleThreadPool.class.getName());
        SCHEDULER_FACTORY_PROPERTIES.put("org.quartz.threadPool.threadCount", "1");
        SCHEDULER_FACTORY_PROPERTIES.put(StdSchedulerFactory.PROP_JOB_STORE_CLASS, RAMJobStore.class);
    }

    private static String FORMDATA_QUERY = "SELECT * FROM hst:formdata ORDER BY hst:creationtime ASC";

    private static final AtomicBoolean busy = new AtomicBoolean(false);

    private String cronExpression;
    private long minutesToLive;
    private Scheduler scheduler;
    private JobDetail job;
    private JobListener jobListener = null;
    private String quartzNamePostfix = "";


    @Override
    protected void doConfigure(final Node moduleConfig) throws RepositoryException {
        cronExpression = JcrUtils.getStringProperty(moduleConfig, CONFIG_CRONEXPRESSION_PROPERTY, null);
        minutesToLive = JcrUtils.getLongProperty(moduleConfig, CONFIG_MINUTES_TO_LIVE_PROPERTY, 30l);
    }

    @Override
    protected void doInitialize(final Session session) throws RepositoryException {
        startScheduler();
        scheduleJob();
    }

    @Override
    protected void doShutdown() {
        if (scheduler != null) {
            try {
                scheduler.shutdown();
            } catch (SchedulerException e) {
                log.error("Error shutting down quartz scheduler: " + e);
            }
        }
    }

    @Override
    protected boolean isReconfigureEvent(Event event) throws RepositoryException {
        String eventPath = event.getPath();
        return !eventPath.endsWith(CONFIG_LOCK_ISDEEP_PROPERTY) && !eventPath.endsWith(CONFIG_LOCK_OWNER);
    }

    @Override
    protected void onConfigurationChange(final Node moduleConfig) throws RepositoryException {
        synchronized (FormDataCleanupModule.this) {
            try {
                unscheduleJob();
                configure(session.getNode(moduleConfigPath));
                scheduleJob();
            } catch (SchedulerException e) {
                log.error("Failed to reconfigure form data cleaner", e);
            }
        }
    }

    private void startScheduler() {
        if (cronExpression == null) {
            // no cron expression configured
            return;
        }
        try {
            SchedulerFactory factory = new StdSchedulerFactory(SCHEDULER_FACTORY_PROPERTIES);
            scheduler = factory.getScheduler();
            if (jobListener != null) {
                scheduler.addGlobalJobListener(jobListener);
            }
            scheduler.start();
        } catch (SchedulerException e) {
            log.error("Failed to initialize: cannot instantiate quartz scheduler", e);
        }
    }

    private void scheduleJob() {
        if (cronExpression == null) {
            // no cron expression configured
            return;
        }
        if (scheduler == null) {
            // starting scheduler failed
            return;
        }
        try {
            job = new JobDetail("FormDataCleanupJob" + quartzNamePostfix, FormDataCleanupJob.class);
            job.getJobDataMap().put(CONFIG_MODULECONFIGPATH, moduleConfigPath);
            job.getJobDataMap().put(CONFIG_MINUTES_TO_LIVE_PROPERTY, minutesToLive);
            job.getJobDataMap().put("session", session);
            CronTrigger trigger = new CronTrigger("FormDataCleanupTrigger" + quartzNamePostfix);
            trigger.setCronExpression(cronExpression);
            scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            log.error("Failed to initialize: cannot schedule job", e);
        } catch (ParseException e) {
            log.error("Failed to initialize: " + cronExpression + " is not a cron expression", e);
        }
    }

    private void unscheduleJob() throws SchedulerException {
        scheduler.deleteJob(job.getName(), job.getGroup());
    }

    public static class FormDataCleanupJob implements Job {

        private static final String DEFAULT_CLUSTER_NODE_ID = "default";

        @Override
        public void execute(final JobExecutionContext context) throws JobExecutionException {
            if (!busy.compareAndSet(false, true)) {
                return;
            }

            Session session = (Session) context.getMergedJobDataMap().get("session");
            String moduleConfigPath = (String) context.getMergedJobDataMap().get(CONFIG_MODULECONFIGPATH);

            try {
                // make sure only one cluster node runs the scheduled cleanup job
                if (!lock(session, moduleConfigPath)) {
                    return;
                }

                log.info("Running form data cleanup job");
                long minutesToLive = (Long) context.getMergedJobDataMap().get(CONFIG_MINUTES_TO_LIVE_PROPERTY);
                removeOldFormData(minutesToLive, session);

            } catch (RepositoryException e) {
                log.error("Error while cleaning up event log", e);
            } finally {
                unlock(session, moduleConfigPath);
                busy.set(false);
            }
        }

        private void removeOldFormData(long minutesToLive, Session session) throws RepositoryException {
            final QueryManager queryManager = session.getWorkspace().getQueryManager();
            final Query query = queryManager.createQuery(FORMDATA_QUERY, Query.SQL);
            final NodeIterator nodes = query.execute().getNodes();
            final long tooOldTimeStamp = System.currentTimeMillis() - minutesToLive * 60 * 1000L;
            int count = 0;
            while (nodes.hasNext()) {
                try {
                    final Node node = nodes.nextNode();
                    if (node.getProperty("hst:creationtime").getDate().getTimeInMillis() > tooOldTimeStamp) {
                        break;
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("Removing form data item at " + node.getPath());
                    }
                    remove(node, 2);
                    if (count++ % 10 == 0) {
                        session.save();
                        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                    }
                } catch (RepositoryException e) {
                    log.error("Error while cleaning up form data", e);
                }
            }
            if (session.hasPendingChanges()) {
                session.save();
            }
            if (count > 0) {
                log.info("Done cleaning " + count + " items");
            } else {
                log.info("No timed out items");
            }
        }

        private void remove(final Node node, int ancestorsToRemove) throws RepositoryException {
            final Node parent = node.getParent();
            node.remove();
            if (ancestorsToRemove > 0 && parent != null && parent.getName().length() == 1 && parent.isNodeType("hst:formdatacontainer") && parent.getNodes().getSize() == 0) {
                remove(parent, ancestorsToRemove - 1);
            }
        }

        private boolean lock(Session session, String moduleConfigPath) {
            try {
                Node lockable = session.getNode(moduleConfigPath);
                if (!lockable.isNodeType("mix:lockable")) {
                    log.error("Node " + moduleConfigPath + " must be lockable");
                    return false;
                }
                LockManager lockManager = session.getWorkspace().getLockManager();
                if (!lockManager.isLocked(moduleConfigPath)) {
                    try {
                        lockManager.lock(moduleConfigPath, false, false, ONE_WEEK, getClusterNodeId(session));
                        return true;
                    } catch (LockException e) {
                        log.warn("Failed to obtain lock: " + e.getMessage() + ". Event log cleanup will not run");
                    }
                }
            } catch (RepositoryException e) {
                log.error("Failed to obtain lock: event log cleanup will not run", e);
            }
            return false;
        }

        private void unlock(Session session, String moduleConfigPath) {
            try {
                LockManager lockManager = session.getWorkspace().getLockManager();
                Lock lock = lockManager.getLock(moduleConfigPath);
                if (lock.isLockOwningSession()) {
                    lockManager.unlock(moduleConfigPath);
                }
            } catch (RepositoryException e) {
                log.error("Error releasing lock", e);
            }
        }

        private String getClusterNodeId(Session session) {
            String clusteNodeId = session.getRepository().getDescriptor("jackrabbit.cluster.id");
            if (clusteNodeId == null) {
                clusteNodeId = DEFAULT_CLUSTER_NODE_ID;
            }
            return clusteNodeId;
        }

    }

}

