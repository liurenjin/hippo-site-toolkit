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
package org.hippoecm.hst.site.container;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.jcr.LoginException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationConverter;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConfigurationFactory;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HST Site Container Servlet
 * 
 * This servlet should initialize all the components that can be accessed via HstServices
 * from the each HST-based applications.
 * Under portal environment, this servlet may not be used because the portal itself can 
 * initialize all the necessary component for each HST-based portlet application.
 * 
 * @author <a href="mailto:w.ko@onehippo.com">Woonsan Ko</a>
 * @version $Id$
 */
public class HstSiteConfigServlet extends HttpServlet {

    public static final String HST_CONFIGURATION_PARAM = "hst-configuration";

    public static final String HST_CONFIG_PROPERTIES_PARAM = "hst-config-properties";
    
    private static final String HST_CONFIGURATION_XML = "hst-configuration.xml";
    
    private static final String HST_CONFIG_PROPERTIES = "hst-config.properties";

    private static final String CHECK_REPOSITORIES_RUNNING_INIT_PARAM = "check.repositories.running"; 
    
    private static final String REPOSITORY_ADDRESS_PARAM_SUFFIX = ".repository.address";

    private final static Logger log = LoggerFactory.getLogger(HstSiteConfigServlet.class);
    
    private static final long serialVersionUID = 1L;

    protected ComponentManager componentManager;
    
    protected boolean initialized;
    protected boolean allRepositoriesAvailable;
    
    protected Configuration configuration;

    // -------------------------------------------------------------------
    // I N I T I A L I Z A T I O N
    // -------------------------------------------------------------------
    private static final String INIT_START_MSG = "HST Site Starting Initialization...";
    private static final String INIT_DONE_MSG = "HST Site Initialization complete, Ready to service requests.";
    
    protected Map<String [], Boolean> repositoryCheckingStatus = new HashMap<String [], Boolean>();

    /**
     * Intialize Servlet.
     */
    @Override
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);
        
        this.configuration = getConfiguration(config);

        boolean checkRepositoriesRunning = this.configuration.getBoolean(CHECK_REPOSITORIES_RUNNING_INIT_PARAM);
        
        if (!checkRepositoriesRunning) {
            doInit(config);
        } else {
            this.allRepositoriesAvailable = false;
            this.repositoryCheckingStatus.clear();
            
            for (Iterator it = this.configuration.getKeys(); it.hasNext(); ) {
                String propName = (String) it.next();
                
                if (propName.endsWith(REPOSITORY_ADDRESS_PARAM_SUFFIX)) {
                    String repositoryAddress = this.configuration.getString(propName);
                    String repositoryParamPrefix = propName.substring(0, propName.length() - REPOSITORY_ADDRESS_PARAM_SUFFIX.length());
                    String repositoryUsername = this.configuration.getString(repositoryParamPrefix + ".repository.user.name");
                    String repositoryPassword = this.configuration.getString(repositoryParamPrefix + ".repository.password");
    
                    if (repositoryAddress != null && !"".equals(repositoryAddress.trim())) {
                        this.repositoryCheckingStatus.put(new String [] { repositoryAddress.trim(), repositoryUsername, repositoryPassword }, Boolean.FALSE);
                    }
                }
            }
            
            if (!this.allRepositoriesAvailable) {
                final Thread repositoryCheckerThread = new Thread("RepositoryChecker") {
                    public void run() {
                        while (!allRepositoriesAvailable) {
                           // check the repository is accessible
                           allRepositoriesAvailable = checkAllRepositoriesRunning();
    
                           if (!allRepositoriesAvailable) {
                               try {
                                    Thread.sleep(3000);
                               } catch (InterruptedException e) {
                               }
                           }
                        }
                        
                        if (allRepositoriesAvailable) {
                            doInit(config);
                        }
                    }
                };
                
                repositoryCheckerThread.start();
            }
        }
    }
    
    protected synchronized void doInit(ServletConfig config) {

        Properties initProperties = ConfigurationConverter.getProperties(this.configuration);
        
        try {
            log.info("HSTSiteServlet attempting to create the Component manager...");
            this.componentManager = new SpringComponentManager(initProperties);
            log.info("HSTSiteServlet attempting to start the Component Manager...");
            this.componentManager.initialize();
            this.componentManager.start();
            HstServices.setComponentManager(this.componentManager);
            
            log.info("HSTSiteServlet has successfuly started the Component Manager....");
            this.initialized = true;
            log.info(INIT_DONE_MSG);
        } catch (Throwable e) {
            if (this.componentManager != null) {
                try { 
                    this.componentManager.stop();
                    this.componentManager.close();
                } catch (Exception ce) {
                }
            }
            // save the exception to complain loudly later :-)
            final String msg = "HSTSite: init() failed.";
            log.error(msg, e);
        }
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        if (!this.initialized) {
            doInit(getServletConfig());
        }
        
    }

    /**
     * In this application doGet and doPost are the same thing.
     * 
     * @param req
     *            Servlet request.
     * @param res
     *            Servlet response.
     * @exception IOException
     *                a servlet exception.
     * @exception ServletException
     *                a servlet exception.
     */
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        doGet(req, res);
    }

    // -------------------------------------------------------------------
    // S E R V L E T S H U T D O W N
    // -------------------------------------------------------------------

    public void destroy() {
        log.info("Done shutting down!");

        try {
            this.componentManager.stop();
            this.componentManager.close();
        } catch (Exception e) {
        } finally {
            HstServices.setComponentManager(null);
        }
    }
    
    protected boolean checkAllRepositoriesRunning() {
        boolean allRunning = true;
        
        for (Map.Entry<String [], Boolean> entry : this.repositoryCheckingStatus.entrySet()) {
            String [] repositoryInfo = entry.getKey();
            String repositoryAddress = repositoryInfo[0];
            String repositoryUsername = (repositoryInfo[1] != null ? repositoryInfo[1] : "");
            String repositoryPassword = (repositoryInfo[2] != null ? repositoryInfo[2] : "");
            
            if (!entry.getValue().booleanValue()) {
                HippoRepository hippoRepository = null;
                Session session = null;
                
                try {
                    hippoRepository = HippoRepositoryFactory.getHippoRepository(repositoryAddress);
                    
                    if (!"".equals(repositoryUsername)) {
                        try {
                            session = hippoRepository.login(new SimpleCredentials(repositoryUsername, repositoryPassword.toCharArray()));
                            
                            if (session != null) {
                                entry.setValue(Boolean.TRUE);
                            }
                        } catch (LoginException le) {
                            log("Failed to try to log on to " + repositoryAddress + " with userID=" + repositoryUsername + ". Skip this repository.");
                            entry.setValue(Boolean.TRUE);
                        }
                    } else {
                        entry.setValue(Boolean.TRUE);
                    }
                } catch (Exception e) {
                    allRunning = false;
                } finally {
                    if (session != null) {
                        try { session.logout(); } catch (Exception ce) { }
                    }
                    if (hippoRepository != null) {
                        try { hippoRepository.close(); } catch (Exception ce) { }
                    }
                }
                
                log("checked repository: " + repositoryAddress + " --> " + (entry.getValue().booleanValue() ? "Running" : "Not running"));
            }
        }
        
        return allRunning;
    }

    protected Configuration getConfiguration(ServletConfig servletConfig) throws ServletException {
        Configuration configuration = null;
        ConfigurationFactory factory = new ConfigurationFactory();
        
        String hstConfigurationFilePath = servletConfig.getInitParameter(HST_CONFIGURATION_PARAM);
        File hstConfigurationFile = null;
        
        if (hstConfigurationFilePath == null) {
            hstConfigurationFilePath = "/WEB-INF/" + HST_CONFIGURATION_XML;
        }
        
        hstConfigurationFile = new File(servletConfig.getServletContext().getRealPath(hstConfigurationFilePath));
        
        try {
            if (hstConfigurationFile.isFile()) {
                factory.setConfigurationFileName(hstConfigurationFile.toURI().toString());
                configuration = factory.getConfiguration();
            } else {
                String hstConfigPropFilePath = servletConfig.getInitParameter(HST_CONFIG_PROPERTIES_PARAM);
                
                if (hstConfigPropFilePath == null) {
                    hstConfigPropFilePath = servletConfig.getServletContext().getRealPath("/WEB-INF/" + HST_CONFIG_PROPERTIES);
                }
                
                configuration = new PropertiesConfiguration(new File(hstConfigPropFilePath));
            }
        } catch (ConfigurationException e) {
            throw new ServletException(e);
        }
        
        return configuration;
    }
}
