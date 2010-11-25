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
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.LoginException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConfigurationFactory;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.ArrayUtils;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.ServletConfigUtils;
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
 * <P>
 * The configuration could be set by a properties file or an xml file.
 * If you would set the configuration by a properties file, you can set an init parameter 
 * named 'hst-config-properties' for the servlet config or for the servlet context. 
 * </P>
 * <P>
 * <EM>The parameter value for the properties file or the xml file is regarded as a web application
 * context relative path or file system relative path if the path does not start with 'file:'.
 * So, you should use a 'file:' prefixed URI for the path parameter value if you want to set an absolute path.
 * When the path starts with a leading slash ('/'), the path is regarded as a servlet context relative path.
 * If the path does not start with 'file:' nor with a leading slash ('/'), it is regarded as a relative path of the file system.
 * </EM>
 * </P>
 * <P>
 * For example, you can add an init parameter named 'hst-config-properties' for this servlet config
 * like the following:
 * <PRE><CODE>
 *   &lt;servlet>
 *    &lt;servlet-name>HstSiteConfigServlet&lt;/servlet-name>
 *    &lt;servlet-class>org.hippoecm.hst.site.container.HstSiteConfigServlet&lt;/servlet-class>
 *    &lt;init-param>
 *      &lt;param-name>hst-config-properties&lt;/param-name>
 *      &lt;param-value>/WEB-INF/hst-config.properties&lt;/param-value>
 *    &lt;/init-param>
 *    &lt;load-on-startup>1&lt;/load-on-startup>
 *  &lt;/servlet>
 * </CODE></PRE>
 * <BR/>
 * Also, you can set context init parameter instead of the config init parameter like the following: 
 * <PRE><CODE>
 *  &lt;context-param>
 *    &lt;param-name>hst-config-properties&lt;/param-name>
 *    &lt;param-value>/WEB-INF/hst-config.properties&lt;/param-value>
 *  &lt;/context-param>
 *  &lt;!-- SNIP -->
 *  &lt;servlet>
 *    &lt;servlet-name>HstSiteConfigServlet&lt;/servlet-name>
 *    &lt;servlet-class>org.hippoecm.hst.site.container.HstSiteConfigServlet&lt;/servlet-class>
 *    &lt;load-on-startup>1&lt;/load-on-startup>
 *  &lt;/servlet>
 * </CODE></PRE>
 * The servlet will retrieve the config init parameter first and it will retrieve the context init parameter
 * when the config init parameter is not set.
 * <BR/>
 * If you don't provide the init parameter named 'hst-config-properties' at all, the value is set to 
 * '/WEB-INF/hst-config.properties' by default.
 * </P>
 * <P>
 * Also, the configuration can be set by an XML file which is of the XML configuration format of
 * <A href="http://commons.apache.org/configuration/">Apache Commons Configuration</A>.
 * If you want to set the configuration by the Apache Commons Configuration XML file, you should provide
 * an init parameter named 'hst-configuration' for servlet config or servlet context. For example,
 * <PRE><CODE>
 *   &lt;servlet>
 *    &lt;servlet-name>HstSiteConfigServlet&lt;/servlet-name>
 *    &lt;servlet-class>org.hippoecm.hst.site.container.HstSiteConfigServlet&lt;/servlet-class>
 *    &lt;init-param>
 *      &lt;param-name>hst-configuration&lt;/param-name>
 *      &lt;param-value>/WEB-INF/hst-configuration.xml&lt;/param-value>
 *    &lt;/init-param>
 *    &lt;load-on-startup>1&lt;/load-on-startup>
 *  &lt;/servlet>
 * </CODE></PRE>
 * Also, you can set context init parameter instead of the config init parameter like the following:
 * <PRE><CODE>
 *  &lt;context-param>
 *    &lt;param-name>hst-configuration&lt;/param-name>
 *    &lt;param-value>/WEB-INF/hst-configuration.xml&lt;/param-value>
 *  &lt;/context-param>
 *  &lt;!-- SNIP -->
 *  &lt;servlet>
 *    &lt;servlet-name>HstSiteConfigServlet&lt;/servlet-name>
 *    &lt;servlet-class>org.hippoecm.hst.site.container.HstSiteConfigServlet&lt;/servlet-class>
 *    &lt;load-on-startup>1&lt;/load-on-startup>
 *  &lt;/servlet>
 * </CODE></PRE>
 * <BR/>
 * For your information, you can configure the <CODE>/WEB-INF/hst-configuration.xml</CODE> file like the following example.
 * In this example, you can see that system properties can be aggregated, multiple properties files can be added and 
 * system property values can be used to configure other properties file paths as well: 
 * <PRE><CODE>
 * &lt;?xml version='1.0'?>
 * &lt;configuration>
 *   &lt;system/>
 *   &lt;properties fileName='${catalina.home}/conf/hst-config-1.properties'/>
 *   &lt;properties fileName='${catalina.home}/conf/hst-config-2.properties'/>
 * &lt;/configuration>
 * </CODE></PRE>
 * <EM>Please refer to the documentation of <A href="http://commons.apache.org/configuration/">Apache Commons Configuration</A> for details.</EM>
 * <BR/>
 * The servlet will retrieve the config init parameter first and it will retrieve the context init parameter
 * when the config init parameter is not set.
 * <BR/>
 * If you don't provide the init parameter named 'hst-config-properties' at all, the value is set to 
 * '/WEB-INF/hst-configuration.xml' by default.
 * <BR/>
 * <EM>The parameter value for the properties file or the xml file is regarded as a web application
 * context relative path or file system relative path if the path does not start with 'file:'.
 * So, you should use a 'file:' prefixed URI for the path parameter value if you want to set an absolute path.
 * When the path starts with a leading slash ('/'), the path is regarded as a servlet context relative path.
 * If the path does not start with 'file:' nor with a leading slash ('/'), it is regarded as a relative path of the file system.
 * </EM>
 * </P>
 * 
 * @version $Id$
 */
public class HstSiteConfigServlet extends HttpServlet {

    public static final String HST_CONFIGURATION_PARAM = "hst-configuration";

    public static final String HST_CONFIG_PROPERTIES_PARAM = "hst-config-properties";
    
    private static final String HST_CONFIGURATION_XML = "hst-configuration.xml";
    
    private static final String HST_CONFIG_PROPERTIES = "hst-config.properties";

    private static final String CHECK_REPOSITORIES_RUNNING_INIT_PARAM = "check.repositories.running"; 
    
    private static final String FORCEFUL_REINIT_PARAM = "forceful.reinit";
    
    private static final String ASSEMBLY_OVERRIDES_CONFIGURATIONS_PARAM = "assembly.overrides";

    private static final String REPOSITORY_ADDRESS_PARAM_SUFFIX = ".repository.address";

    private final static Logger log = LoggerFactory.getLogger(HstSiteConfigServlet.class);
    
    private static final long serialVersionUID = 1L;

    protected ComponentManager componentManager;

    protected String [] assemblyOverridesConfigurations;
    
    protected boolean initialized;
    
    protected boolean forecefulReinitialization;
    protected boolean checkRepositoriesRunning;
    protected boolean allRepositoriesAvailable;
    
    protected Configuration configuration;

    // -------------------------------------------------------------------
    // I N I T I A L I Z A T I O N
    // -------------------------------------------------------------------
    private static final String INIT_START_MSG = "HstSiteConfigServlet Starting Initialization...";
    private static final String INIT_DONE_MSG = "HstSiteConfigServlet Initialization complete, Ready to service requests.";
    
    protected Map<String [], Boolean> repositoryCheckingStatus = new HashMap<String [], Boolean>();

    /**
     * Intialize Servlet.
     */
    @Override
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);

        // If the forceful re-initialization option is not turned on
        // and the component manager were intialized in other web application,
        // then just skip the following.
        // If this servlet is initialized inside a site web application 
        // and other web application already initialized the component manager,
        // then the followings are to be skipped.
        forecefulReinitialization = Boolean.parseBoolean(getConfigOrContextInitParameter(FORCEFUL_REINIT_PARAM, null));
        if (!forecefulReinitialization && HstServices.isAvailable()) {
            return;
        }
        // If it is not available or it is forceful re-initialization mode from the web app (like portal webapp)
        // then the initialization will go on from here.
        
        this.configuration = getConfiguration(config);
        
        assemblyOverridesConfigurations = this.configuration.getStringArray(ASSEMBLY_OVERRIDES_CONFIGURATIONS_PARAM);
        
        checkRepositoriesRunning = this.configuration.getBoolean(CHECK_REPOSITORIES_RUNNING_INIT_PARAM);
        
        if (!checkRepositoriesRunning) {
            initializeComponentManager(config);
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
                            initializeComponentManager(config);
                        }
                    }
                };
                
                repositoryCheckerThread.start();
            }
        }
    }
    
    protected synchronized void initializeComponentManager(ServletConfig config) {

        try {
            log.info(INIT_START_MSG);
            
            ComponentManager oldComponentManager = HstServices.getComponentManager();
            if (oldComponentManager != null) {
                log.info("HstSiteConfigServlet attempting to stop the old component manager...");
                try {
                    HstServices.setComponentManager(null);
                    oldComponentManager.stop();
                    oldComponentManager.close();
                } catch (Exception ce) {
                }
            }
            
            log.info("HstSiteConfigServlet attempting to create the Component manager...");
            this.componentManager = new SpringComponentManager(this.configuration);
            log.info("HSTSiteServlet attempting to start the Component Manager...");
            
            if (assemblyOverridesConfigurations != null && assemblyOverridesConfigurations.length > 0) {
                String [] configurations = this.componentManager.getConfigurationResources();
                configurations = (String []) ArrayUtils.addAll(configurations, assemblyOverridesConfigurations);
                this.componentManager.setConfigurationResources(configurations);
            }
            
            this.componentManager.initialize();
            this.componentManager.start();
            HstServices.setComponentManager(this.componentManager);
            
            log.info("HstSiteConfigServlet has successfuly started the Component Manager....");
            this.initialized = true;
            log.info(INIT_DONE_MSG);
        } catch (Exception e) {
            if (this.componentManager != null) {
                try { 
                    this.componentManager.stop();
                    this.componentManager.close();
                } catch (Exception ce) {
                    if (log.isDebugEnabled()) {
                        log.warn("Exception occurred during stopping componentManager.", e);
                    } else if (log.isWarnEnabled()) {
                        log.warn("Exception occurred during stopping componentManager. {}", e.toString());
                    }
                }
            }
            
            // save the exception to complain loudly later :-)
            final String msg = "HSTSite: init() failed.";
            log.error(msg, e);
        }
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        if (!this.initialized) {
            initializeComponentManager(getServletConfig());
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

    /**
     * <p>The goal of this method is to load the configuration using parameters provided by servlet config parameters,
     * and/or system parameters. Some sane defaults are available as well.</p>
     * <p>You can use the commons configuration xml config, which by default should be in the WEB-INF folder. You can
     * also configure the location of this file in init-params of the servlet. Check the example</p>
     * <p>Another way to configure commons configuration is to use a properties file. Again the location by default is
     * in the WEB-INF folder, but you can change location and name of the file using init-params of the servlet</p>
     * <p/>
     *
     * <strong>Example - using xml configuration and a system parameter</strong>
     * <pre>-DConfig.dir=/usr/local/tomcat/config</pre><br/>
     * <pre>hst-configuration.xml</pre> (in the WEB-INF folder)
     * <pre>
     * &lt;configuration>
     *     &lt;properties fileName="${Config.dir}/site/hst-config.properties"/&gt;
     * &lt;/configuration>
     * </pre>
     *
     * <strong>Example - using servlet init param for hst.properties</strong>
     * <pre>web.xml</pre>
     * <pre>
     * &lt;init-param>
     *     &lt;param-name>hst-config-properties&lt;/param-name>
     * &lt;param-value>/usr/local/tomcat/config/site/hst-config.properties&lt;/param-value>
     * &lt;/init-param>
     * </pre>
     *
     * @param servletConfig ServletConfig that contains the init params
     * @return Configuration containing all the params found in the system, jndi and the config file found
     * @throws ServletException thrown if file's cannot be found or configuration problems arise.
     */
    protected Configuration getConfiguration(ServletConfig servletConfig) throws ServletException {
        Configuration configuration = null;
        ConfigurationFactory factory = new ConfigurationFactory();

        // check if we have an commons configuration xml config file
        String hstConfigurationFilePath = getConfigOrContextInitParameter(HST_CONFIGURATION_PARAM, "/WEB-INF/" + HST_CONFIGURATION_XML);
        
        try {
            File hstConfigurationFile = getResourceFile(hstConfigurationFilePath);
            
            if (hstConfigurationFile != null && hstConfigurationFile.isFile()) {
                factory.setConfigurationFileName(hstConfigurationFile.getCanonicalPath());
                configuration = factory.getConfiguration();
                
                if (log.isInfoEnabled()) {
                    log.info("Using HST Configuration File: {}", hstConfigurationFile.getCanonicalPath());
                }
            } else {
                // no xml config found, try the properties file alternative
                if (log.isDebugEnabled()) {
                    log.debug("The file does not exist: {}", hstConfigurationFilePath);
                }
                
                String hstConfigPropFilePath = getConfigOrContextInitParameter(HST_CONFIG_PROPERTIES_PARAM, "/WEB-INF/" + HST_CONFIG_PROPERTIES);
                File hstConfigPropFile = getResourceFile(hstConfigPropFilePath);
                
                if (hstConfigPropFile != null && hstConfigPropFile.isFile()) {
                    configuration = new PropertiesConfiguration(hstConfigPropFile);

                    if (log.isInfoEnabled()) {
                        log.info("Using HST Configuration Properties File: {}", hstConfigPropFile.getCanonicalPath());
                    }
                } else {
                    if (log.isWarnEnabled()) {
                        log.warn("The file does not exist: {}", hstConfigPropFilePath);
                    }
                }
            }
        } catch (ConfigurationException e) {
            throw new ServletException(e);
        } catch (IOException e) {
            throw new ServletException(e);
        }
        
        if (configuration == null) {
            configuration = new PropertiesConfiguration();
        }

        return configuration;
    }
    
    protected String getConfigOrContextInitParameter(String paramName, String defaultValue) {
        String value = ServletConfigUtils.getInitParameter(getServletConfig(), getServletConfig().getServletContext(), paramName, defaultValue);
        return (value != null ? value.trim() : null);
    }
    
    /**
     * Returns the physical resource file object.
     * <P>
     * <UL>
     * <LI>When the resourcePath starts with 'file:', it is assumed as an absolute file URI path.</LI>
     * <LI>When the resourcePath starts with a leading slash ('/'), it is assumed as a servlet context relative path.</LI>
     * <LI>When the resourcePath does not starts with 'file' nor with a leading slash ('/'), it is assumed as a relative path of the file system.</LI>  
     * </UL>
     * </P>
     * @param resourcePath
     * @return
     */
    protected File getResourceFile(String resourcePath) {
        File resourceFile = null;
        
        if (resourcePath != null) {
            if (resourcePath.startsWith("file:")) {
                resourceFile = new File(URI.create(resourcePath));
            } else if (resourcePath.startsWith("/")) {
                String realPath = null;
                
                try {
                    realPath = getServletConfig().getServletContext().getRealPath(resourcePath);
                } catch (Exception re) {
                }
                
                if (realPath != null) {
                    resourceFile = new File(realPath);
                }
            } else {
                resourceFile = new File(resourcePath);
            }
        }
        
        return resourceFile;
    }
    
}
