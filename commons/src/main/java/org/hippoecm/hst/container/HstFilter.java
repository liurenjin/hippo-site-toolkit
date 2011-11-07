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
package org.hippoecm.hst.container;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.jcr.Credentials;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hippoecm.hst.configuration.hosting.MatchException;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.internal.ContextualizableMount;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.sitemapitemhandlers.HstSiteMapItemHandlerConfiguration;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstContainerConfig;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.container.RepositoryNotAvailableException;
import org.hippoecm.hst.core.container.ServletContextAware;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.internal.HstRequestContextComponent;
import org.hippoecm.hst.core.internal.MountDecorator;
import org.hippoecm.hst.core.internal.MutableResolvedMount;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.request.ResolvedVirtualHost;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandler;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandlerException;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandlerFactory;
import org.hippoecm.hst.logging.Logger;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.GenericHttpServletRequestWrapper;
import org.hippoecm.hst.util.HstRequestUtils;
import org.hippoecm.hst.util.ServletConfigUtils;

public class HstFilter implements Filter {

    @SuppressWarnings("unused")
    private static final long serialVersionUID = 1L;

    private static final String LOGGER_CATEGORY_NAME = HstFilter.class.getName();

    private final static String FILTER_DONE_KEY = "filter.done_"+HstFilter.class.getName();
    private final static String REQUEST_START_TICK_KEY = "request.start_"+HstFilter.class.getName();

    // used to redirect to nodes with a certain JCR uuid in mounts of a certain type
    private static final String PATH_PREFIX_UUID_REDIRECT = "/previewfromcms";
    private static final String REQUEST_PARAM_UUID = "uuid";
    private static final String REQUEST_PARAM_TYPE = "type";
    private static final String DEFAULT_REQUEST_PARAM_TYPE = "live";

    private FilterConfig filterConfig;

    /* moved here from HstContainerServlet initialization */
    public static final String CONTEXT_NAMESPACE_INIT_PARAM = "hstContextNamespace";
    public static final String CLIENT_REDIRECT_AFTER_JAAS_LOGIN_BEHIND_PROXY = "clientRedirectAfterJaasLoginBehindProxy";
    public static final String CLIENT_COMPONENT_MANAGER_CLASS_INIT_PARAM = "clientComponentManagerClass";
    public static final String CLIENT_COMPONENT_MANAGER_CONFIGURATIONS_INIT_PARAM = "clientComponentManagerConfigurations";
    public static final String CLIENT_COMPONENT_MANAGER_CONTEXT_ATTRIBUTE_NAME_INIT_PARAM = "clientComponentManagerContextAttributeName";
    public static final String CLIENT_COMPONENT_MANANGER_DEFAULT_CONTEXT_ATTRIBUTE_NAME = HstFilter.class.getName() + ".clientComponentManager";

    private static final String DEFAULT_LOGIN_RESOURCE_PATH = "/login/resource";
    
    protected String contextNamespace;
    protected boolean doClientRedirectAfterJaasLoginBehindProxy;
    protected String clientComponentManagerClassName;
    protected String [] clientComponentManagerConfigurations;
    protected volatile boolean initialized;
    protected ComponentManager clientComponentManager;
    protected String clientComponentManagerContextAttributeName = CLIENT_COMPONENT_MANANGER_DEFAULT_CONTEXT_ATTRIBUTE_NAME;
    protected volatile HstContainerConfig requestContainerConfig;

    protected HstManager hstSitesManager;
    protected HstSiteMapItemHandlerFactory siteMapItemHandlerFactory;
    
    private String defaultLoginResourcePath;
    
    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;

        /* HST and ClientComponentManager initialization */

        contextNamespace = getConfigOrContextInitParameter(CONTEXT_NAMESPACE_INIT_PARAM, contextNamespace);
 
        doClientRedirectAfterJaasLoginBehindProxy = Boolean.parseBoolean(getConfigOrContextInitParameter(CLIENT_REDIRECT_AFTER_JAAS_LOGIN_BEHIND_PROXY, "true"));

        clientComponentManagerClassName = getConfigOrContextInitParameter(CLIENT_COMPONENT_MANAGER_CLASS_INIT_PARAM, clientComponentManagerClassName);

        String param = getConfigOrContextInitParameter(CLIENT_COMPONENT_MANAGER_CONFIGURATIONS_INIT_PARAM, null);

        if (param != null) {
            String [] configs = param.split(",");

            for (int i = 0; i < configs.length; i++) {
                configs[i] = configs[i].trim();
            }

            clientComponentManagerConfigurations = configs;
        }

        clientComponentManagerContextAttributeName = getConfigOrContextInitParameter(CLIENT_COMPONENT_MANAGER_CONTEXT_ATTRIBUTE_NAME_INIT_PARAM, clientComponentManagerContextAttributeName);
        
        defaultLoginResourcePath = getInitParameter(filterConfig, null, "loginResource", DEFAULT_LOGIN_RESOURCE_PATH);

        initialized = false;

        if (HstServices.isAvailable()) {
            synchronized (this) {
                doInit(filterConfig);
                initialized = true;
            }

            Logger logger = HstServices.getLogger(LOGGER_CATEGORY_NAME);

            if (hstSitesManager != null) {
                siteMapItemHandlerFactory = hstSitesManager.getSiteMapItemHandlerFactory();
                if(siteMapItemHandlerFactory == null) {
                    logger.error("Cannot find the siteMapItemHandlerFactory component");
                }
            } else {
                logger.error("Cannot find the virtualHostsManager component for '{}'", HstManager.class.getName());
            }
        }
    }

    protected void doInit(FilterConfig config) {
        hstSitesManager = HstServices.getComponentManager().getComponent(HstManager.class.getName());

        if (clientComponentManager != null) {
            try {
                clientComponentManager.stop();
                clientComponentManager.close();
            }
            catch (Exception e) {
            	// ignored
            }
            finally {
                clientComponentManager = null;
            }
        }

        try {
            if (clientComponentManagerClassName != null && clientComponentManagerConfigurations != null && clientComponentManagerConfigurations.length > 0) {
                clientComponentManager = (ComponentManager) Thread.currentThread().getContextClassLoader().loadClass(clientComponentManagerClassName).newInstance();

                if (clientComponentManager instanceof ServletContextAware) {
                    ((ServletContextAware) clientComponentManager).setServletContext(config.getServletContext());
                }

                clientComponentManager.setConfigurationResources(clientComponentManagerConfigurations);
                clientComponentManager.initialize();
                clientComponentManager.start();
                config.getServletContext().setAttribute(clientComponentManagerContextAttributeName, clientComponentManager);

            }
        }
        catch (Exception e) {
            log("Invalid client component manager class or configuration: " + e);
        }
    }

    /**
     * Returns the client component manager instance if available.
     * @param servletContext
     * @return
     */
    public static ComponentManager getClientComponentManager(ServletContext servletContext) {
        String attributeName = ServletConfigUtils.getInitParameter(null, servletContext,
                CLIENT_COMPONENT_MANAGER_CONTEXT_ATTRIBUTE_NAME_INIT_PARAM, CLIENT_COMPONENT_MANANGER_DEFAULT_CONTEXT_ATTRIBUTE_NAME);
        return (ComponentManager)servletContext.getAttribute(attributeName);
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
    ServletException {
        
    	if (request.getAttribute(ContainerConstants.HST_RESET_FILTER) != null) {
    		request.removeAttribute(FILTER_DONE_KEY);
    		request.removeAttribute(ContainerConstants.HST_RESET_FILTER);
    	}

		if (request.getAttribute(FILTER_DONE_KEY) != null) {
			chain.doFilter(request, response);
			return;
		}

    	HttpServletRequest req = (HttpServletRequest)request;
        HttpServletResponse res = (HttpServletResponse)response;
     	
    	// Cross-context includes are not (yet) supported to be handled directly by HstFilter
    	// Typical use-case for these is within a portal environment where the portal dispatches to a portlet (within in a separate portlet application)
    	// which *might* dispatch to HST. If such portlet dispatches again it most likely will run through this filter (being by default configured against /*)
    	// but in that case the portlet container will have setup a wrapper request as embedded within this web application (not cross-context).
    	if (isCrossContextInclude(req)) {
    		chain.doFilter(request, response);
    		return;
    	}

		request.setAttribute(FILTER_DONE_KEY, Boolean.TRUE);

    	Logger logger = HstServices.getLogger(LOGGER_CATEGORY_NAME);

    	try {
    		if (!HstServices.isAvailable()) {
    			res.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    			logger.error("The HST Container Services are not initialized yet.");
    			return;
    		}

    		// ensure ClientComponentManager (if defined) is initialized properly
    		if (!initialized) {
    		    synchronized (this) {
    		        if (!initialized) {
    	                doInit(filterConfig);
    	                initialized = true;
    		        }
    		    }

                if (hstSitesManager != null) {
                    siteMapItemHandlerFactory = hstSitesManager.getSiteMapItemHandlerFactory();
                    if(siteMapItemHandlerFactory == null) {
                        logger.error("Cannot find the siteMapItemHandlerFactory component");
                    }
                } else {
                    logger.error("Cannot find the virtualHostsManager component for '{}'", HstManager.class.getName());
                }
    		}

    		if(this.siteMapItemHandlerFactory == null || this.hstSitesManager == null) {
    			res.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    			logger.error("The HST virtualHostsManager or siteMapItemHandlerFactory is not available");
    			return;
    		}

    		if (requestContainerConfig == null) {
        		synchronized (this) {
        			if (requestContainerConfig == null) {
        				requestContainerConfig = new HstContainerConfigImpl(filterConfig.getServletContext(), Thread.currentThread().getContextClassLoader());
        			}
        		}
    		}

    		if (logger.isDebugEnabled()) {request.setAttribute(REQUEST_START_TICK_KEY, System.nanoTime());}

    		// Sets up the container request wrapper
            HstContainerRequest containerRequest = new HstContainerRequestImpl(req, hstSitesManager.getPathSuffixDelimiter());

    		VirtualHosts vHosts = hstSitesManager.getVirtualHosts();

    		// we always want to have the virtualhost available, even when we do not have hst request processing:
    		// We need to know whether to include the contextpath in URL's or not, even for jsp's that are not dispatched by the HST
    		// This info is on the virtual host.
            String hostName = HstRequestUtils.getFarthestRequestHost(containerRequest);
            ResolvedVirtualHost resolvedVirtualHost = vHosts.matchVirtualHost(hostName);
            // when resolvedVirtualHost = null, we cannot do anything else then fall through to the next filter
            if(resolvedVirtualHost == null) {
                logger.warn("hostName '{}' can not be matched. Skip HST Filter and request processing. ", hostName);
                chain.doFilter(request, response);
                return;
            } 
 
            /*
             * HSTTWO-1519
             * Below is a workaround for JAAS authentication: The j_security_check URL is always handled by the 
             * container, after which a REDIRECT takes place which always includes the contextpath. In case of a 
             * proxy like httpd in front that again adds the contextpath, the URL ends up with twice the contextpath.
             * This can only happen when the contextpath is not empty and when the !resolvedVirtualHost.getVirtualHost().isContextPathInUrl()
             * We check below whether the previous location was /login/resource, and if so, whether the contextpath is twice in the url
             * If so, we do a client redirect again to remove the duplicate contextpath
             */ 
            if (doClientRedirectAfterJaasLoginBehindProxy && !resolvedVirtualHost.getVirtualHost().isContextPathInUrl() && !"".equals(req.getContextPath())) {
                String referer = req.getHeader("Referer");
                if (referer != null && referer.endsWith(defaultLoginResourcePath)) {
                    String requestURI = req.getRequestURI();
                    if (requestURI.startsWith(req.getContextPath() + req.getContextPath() + "/")) {
                        String redirectTo = requestURI.substring((req.getContextPath() + req.getContextPath()).length());
                        res.sendRedirect(redirectTo);
                        return;
                    }
                }
    	    }

            request.setAttribute(ContainerConstants.VIRTUALHOSTS_REQUEST_ATTR, resolvedVirtualHost);

    		// when getPathSuffix() is not null, we have a REST url and never skip hst request processing
    		if(vHosts == null || (containerRequest.getPathSuffix() == null && vHosts.isExcluded(containerRequest.getPathInfo()))) {
    		    chain.doFilter(request, response);
    			return;
    		}

            HstMutableRequestContext requestContext = (HstMutableRequestContext)containerRequest.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
            
                
    		if (requestContext == null) {
        		HstRequestContextComponent rcc = HstServices.getComponentManager().getComponent(HstRequestContextComponent.class.getName());
        		requestContext = rcc.create(false);
        		if (this.contextNamespace != null) {
        			requestContext.setContextNamespace(contextNamespace);
        		}
        		containerRequest.setAttribute(ContainerConstants.HST_REQUEST_CONTEXT, requestContext);
    		}
    		requestContext.setServletContext(filterConfig.getServletContext());
            requestContext.setPathSuffix(containerRequest.getPathSuffix());
            
            if("true".equals(request.getParameter(ContainerConstants.HST_REQUEST_USE_FULLY_QUALIFIED_URLS))) {
                requestContext.setFullyQualifiedURLs(true);
            }

            if (containerRequest.getPathInfo().startsWith(PATH_PREFIX_UUID_REDIRECT)) {
                /*
                 * The request starts PATH_PREFIX_UUID_REDIRECT which means it is called from the cms with a uuid. Below, we compute
                 * a URL for the uuid, and send a browser redirect to this URL. 
                 */
                sendRedirectToUuidUrl(req, res, requestContext, resolvedVirtualHost, containerRequest, hostName, logger);
                return;
            } else {
                
                ResolvedMount resolvedMount = requestContext.getResolvedMount();
                
                if (resolvedMount == null) {
                    resolvedMount = vHosts.matchMount(hostName, containerRequest.getContextPath() , containerRequest.getPathInfo());
                    if(resolvedMount != null) {
                        requestContext.setResolvedMount(resolvedMount);
                        // if we are in RENDERING_HOST mode, we always need to include the contextPath, even if showcontextpath = false.
                        String renderingHost = HstRequestUtils.getRenderingHost(containerRequest);
                        if (renderingHost != null) {
                            requestContext.setRenderHost(renderingHost);
                            requestContext.setAttribute(ContainerConstants.REAL_HOST, HstRequestUtils.getFarthestRequestHost(req, false));
                            // check whether there is a SSO handshake already: If there is, we decorate the mount to a previewMount
                            HttpSession session = containerRequest.getSession(false);
                            if(session != null && Boolean.TRUE.equals(session.getAttribute(ContainerConstants.CMS_SSO_AUTHENTICATED ))) {
                                // we are in a CMS SSO context.
                                session.setAttribute(ContainerConstants.RENDERING_HOST, renderingHost);
                                if(resolvedMount instanceof MutableResolvedMount) {
                                    Mount mount = resolvedMount.getMount();
                                    if(!(mount instanceof ContextualizableMount)) {
                                        throw new MatchException("The matched mount for request '" + hostName + " and " +containerRequest.getRequestURI() + "' is not an instanceof of a ContextualizableMount. Cannot act as preview mount. Cannot proceed request for CMS SSO environment.");
                                    }
                                    MountDecorator mountDecorator = HstServices.getComponentManager().getComponent(MountDecorator.class.getName());
                                    Mount decoratedMount = mountDecorator.decorateMountAsPreview((ContextualizableMount)mount);
                                    if(decoratedMount == mount) {
                                        logger.debug("Matched mount pointing to site '{}' is already a preview so no need for CMS SSO context to decorate the mount to a preview", mount.getMountPoint());
                                    } else {
                                        logger.debug("Matched mount pointing to site '{}' is because of CMS SSO context replaced by preview decorated mount pointing to site '{}'", mount.getMountPoint(), decoratedMount.getMountPoint());
                                    }
                                    ((MutableResolvedMount)resolvedMount).setMount(decoratedMount);
                                } else {
                                    throw new MatchException("ResolvedMount must be an instance of MutableResolvedMount to be usable in CMS SSO environment. Cannot proceed request for " + hostName + " and " +containerRequest.getRequestURI());
                                }
                            }

                        }
                    }
                    else {
                        throw new MatchException("No matching Mount for '"+hostName+"' and '"+containerRequest.getRequestURI()+"'");
                    }
                }

                HstContainerURL hstContainerUrl = setMountPathAsServletPath(containerRequest, requestContext, resolvedMount, res);

                if (resolvedMount.getMount().isMapped()) {
                    ResolvedSiteMapItem resolvedSiteMapItem = requestContext.getResolvedSiteMapItem();
                    boolean processSiteMapItemHandlers = false;

                    if (resolvedSiteMapItem == null) {
                        processSiteMapItemHandlers = true;
                        resolvedSiteMapItem = resolvedMount.matchSiteMapItem(hstContainerUrl.getPathInfo());
                        if(resolvedSiteMapItem == null) {
                            // should not be possible as when it would be null, an exception should have been thrown
                            logger.warn(hostName+"' and '"+containerRequest.getRequestURI()+"' could not be processed by the HST: Error resolving request to sitemap item");
                            sendError(req, res, HttpServletResponse.SC_NOT_FOUND);
                            return;
                        }
                        requestContext.setResolvedSiteMapItem(resolvedSiteMapItem);
                    }

                    processResolvedSiteMapItem(containerRequest, res, requestContext, processSiteMapItemHandlers, logger);

                }
                else {
                    if(resolvedMount.getNamedPipeline() == null) {
                        logger.warn(hostName + "' and '" + containerRequest.getRequestURI() + "' could not be processed by the HST: No hstSite and no custom namedPipeline for Mount");
                        sendError(req, res, HttpServletResponse.SC_NOT_FOUND);
                    }
                    else {
                        logger.info("Processing request for pipeline '{}'", resolvedMount.getNamedPipeline());
                        HstServices.getRequestProcessor().processRequest(this.requestContainerConfig, requestContext, containerRequest, res, resolvedMount.getNamedPipeline());
                    }
                }
            }
    	}
    	catch (MatchException e) {
    	    if(logger.isDebugEnabled()) {
                logger.warn(e.getClass().getName() + " for '"+req.getRequestURI()+"':" , e);
            } else {
                logger.warn(e.getClass().getName() + " for '{}': '{}'" , req.getRequestURI(),  e.toString());
            }
            sendError(req, res, HttpServletResponse.SC_NOT_FOUND);
        } catch (ContainerException e) {
           if(logger.isDebugEnabled()) {
                logger.warn(e.getClass().getName() + " for '"+req.getRequestURI()+"':" , e);
            } else {
                logger.warn(e.getClass().getName() + " for '{}': '{}'" , req.getRequestURI(),  e.toString());
            }
            sendError(req, res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    	catch (Exception e) {
            logger.warn("Fatal error encountered while processing request '"+req.getRequestURI()+"':" , e);
            sendError(req, res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    	}
    	finally {
    		if (logger != null && logger.isDebugEnabled()) {
    			long starttick = request.getAttribute(REQUEST_START_TICK_KEY) == null ? 0 : (Long)request.getAttribute(REQUEST_START_TICK_KEY);
    			if(starttick != 0) {
    				logger.debug( "Handling request took --({})-- ms for '{}'.", (System.nanoTime() - starttick)/1000000, req.getRequestURI());
    			}
    		}
    	}
    }

    private String getJcrUuidParameter(ServletRequest request, Logger logger) throws IOException {
        String jcrUuid = request.getParameter(REQUEST_PARAM_UUID);

        if (jcrUuid == null || "".equals(jcrUuid)) {
            logger.warn("Cannot redirect when there is no UUID");
            return null;
        }

        try {
            UUID.fromString(jcrUuid);
        } catch (IllegalArgumentException e) {
            logger.warn("Cannot redirect because '{}' is not a valid UUID", jcrUuid);
            return null;
        }

        return jcrUuid;
    }

    private String getTypeParameter(ServletRequest request, Logger logger) {
        String type = request.getParameter(REQUEST_PARAM_TYPE);

        if (type == null) {
            logger.debug("No type defined. Default type is '{}'", DEFAULT_REQUEST_PARAM_TYPE);
            type = DEFAULT_REQUEST_PARAM_TYPE;
        } else if (!"live".equals(type) && !"preview".equals(type)) {
            logger.warn("Ignoring unknown type '{}', using '{}' instead. Known types are 'preview' and 'live'.", type, DEFAULT_REQUEST_PARAM_TYPE);
            type = DEFAULT_REQUEST_PARAM_TYPE;
        }

        return type;
    }

    /**
     * Finds a resolved mount of the correct type ('preview' or 'live') for a host in a host group in the given
     * virtual host. We take as Mount the mount that has the closest content path {@link Mount#getCanonicalContentPath()}
     * to the <code>nodePath</code>. If multiple {@link Mount}'s have an equally well suited {@link Mount#getCanonicalContentPath()}, 
     * we pick the mount  with the fewest types is picked. These mounts are in general the most generic ones. If multiple 
     * {@link Mount}'s have equally well suited {@link Mount#getCanonicalContentPath()} and equal number of types, we pick one at random
     *
     * @param containerRequest current request
     * @param type the mount type
     * @param hostName name of the host to match
     * @param nodePath the jcr nodePath of the document for which to get a {@link ResolvedMount}
     *
     * @return the resolved mount of the given type
     *
     * @throws org.hippoecm.hst.core.container.RepositoryNotAvailableException
     * @throws org.hippoecm.hst.configuration.hosting.MatchException when no matching mount could be found for the given host and type.
     */
    private ResolvedMount getMountForType(HstContainerRequest containerRequest, String type, String hostName, String hostGroupName, VirtualHosts vHosts, String nodePath) throws RepositoryNotAvailableException {
        List<Mount> mounts = vHosts.getMountsByHostGroup(hostGroupName);
        if (mounts == null) {
            throw new MatchException("No mounts found for host '" + hostName + "' and '" + containerRequest.getRequestURL() + "'");
        }
     
        List<Mount> candidateMounts = new ArrayList<Mount>();
        int bestPathLength = 0;
        for (Mount mount : mounts) {
            if(!mount.isMapped()) {
                // not a sitemap 
                continue;
            }
            if (mount.getType().equals(type) && (nodePath.startsWith(mount.getCanonicalContentPath() + "/") || nodePath.equals(mount.getCanonicalContentPath()))) {
                if(mount.getCanonicalContentPath().length() == bestPathLength) {
                    // Equally well as already found ones. Add to candidateMounts
                    candidateMounts.add(mount);
                } else if (mount.getCanonicalContentPath().length() > bestPathLength) {
                    // this is a better one than the ones already found. Clear the candidateMounts first
                    candidateMounts.clear();
                    candidateMounts.add(mount);
                    bestPathLength = mount.getCanonicalContentPath().length();
                } else {
                    // ignore, we already have a better mount
                }
            }
        }
        
        if(candidateMounts.isEmpty()) {
            throw new MatchException("There is no mount of type '" + type + "' in the host group for '" + hostName + "' and '" + containerRequest.getRequestURL() + "' that can create a link for a document with path '"+nodePath+"'");
        }
        
        Mount bestMount = candidateMounts.get(0);
        int typeCount = Integer.MAX_VALUE;
        for (Mount mount : candidateMounts) {
            if (mount.getTypes().size() < typeCount) {
                typeCount = mount.getTypes().size();
                bestMount = mount;
            }
        }
        return vHosts.matchMount(hostName, containerRequest.getContextPath(), bestMount.getMountPath());
    }

    /**
     * Sets the HST equivalent of the servletPath on the container request, namely the resolved path of the
     * resolved HST mount.
     *
     * @param containerRequest request to the set HST servlet path for
     * @param requestContext HST request context
     * @param mount the resolved HST mount
     * @param response servlet response for parsing the URL
     * @return the HST container URL for the resolved HST mount
     */
    private HstContainerURL setMountPathAsServletPath(HstContainerRequest containerRequest, HstMutableRequestContext requestContext, ResolvedMount mount, HttpServletResponse response) {
        ((GenericHttpServletRequestWrapper) containerRequest).setServletPath(mount.getResolvedMountPath());

        HstContainerURL hstContainerURL = requestContext.getBaseURL();

        if (hstContainerURL == null) {
            hstContainerURL = hstSitesManager.getUrlFactory().getContainerURLProvider().parseURL(containerRequest, response, mount);
            requestContext.setBaseURL(hstContainerURL);
        }

        return hstContainerURL;
    }

    /**
     * Sends a redirect to a URL for the JCR node with the given UUID.
     *
     * @param req HTTP servlet request
     * @param res HTTP servlet response
     * @param requestContext the HST request context
     * @param jcrUuid the UUID of the JCR node
     * @param logger
     * @throws javax.jcr.RepositoryException
     * @throws java.io.IOException
     */
    private void sendRedirectToUuidUrl(HttpServletRequest req, HttpServletResponse res, HstMutableRequestContext requestContext, 
            ResolvedVirtualHost resolvedVirtualHost, HstContainerRequest containerRequest, String hostName, Logger logger) throws RepositoryNotAvailableException, RepositoryException, IOException {
         
        final String jcrUuid = getJcrUuidParameter(req, logger);
        if (jcrUuid == null) {
            sendError(req, res, HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        
        Session session = null;
        try {
            Credentials configReaderCreds = HstServices.getComponentManager().getComponent(Credentials.class.getName() + ".hstconfigreader");
            Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName());
            session = repository.login(configReaderCreds);
           
            Node node = null;
            try {
                node = session.getNodeByIdentifier(jcrUuid);
            } catch (ItemNotFoundException e) {
                logger.warn("Cannot find a node for uuid '{}'", jcrUuid);
                sendError(req, res, HttpServletResponse.SC_NOT_FOUND);
                return;
            }
    
            final String mountType = getTypeParameter(req, logger);
    
            final String hostGroupName = resolvedVirtualHost.getVirtualHost().getHostGroupName();
            final ResolvedMount mount = getMountForType(containerRequest, mountType, hostName, hostGroupName, resolvedVirtualHost.getVirtualHost().getVirtualHosts(), node.getPath());
            if (mount != null) {
                requestContext.setResolvedMount(mount);
            } else {
                throw new MatchException("No matching mount for '" + hostName + "' and '" + containerRequest.getRequestURL() + "'");
            }
    
            ((GenericHttpServletRequestWrapper)containerRequest).setRequestURI(mount.getResolvedMountPath() + "/" + PATH_PREFIX_UUID_REDIRECT);
            setMountPathAsServletPath(containerRequest, requestContext, mount, res);

            
            final HstLinkCreator linkCreator = HstServices.getComponentManager().getComponent(HstLinkCreator.class.getName());
            if (linkCreator == null) {
                logger.error("Cannot create a 'uuid url' when there is no linkCreator available");
                sendError(req, res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }
    
            requestContext.setURLFactory(hstSitesManager.getUrlFactory());
            final HstLink link = linkCreator.create(node, requestContext);
            if (link == null) {
                logger.warn("Not able to create link for node '{}' belonging to uuid = '{}'", node.getPath(), jcrUuid);
                sendError(req, res, HttpServletResponse.SC_NOT_FOUND);
                return;
            }
    
            String url = link.toUrlForm(requestContext, false);
            if (requestContext.isFullyQualifiedURLs()) {
                url += "?" + ContainerConstants.HST_REQUEST_USE_FULLY_QUALIFIED_URLS + "=true";
            }
            if (logger.isInfoEnabled()) {
                logger.info("Created HstLink for uuid '{}': '{}'", node.getPath(), url);
            }
            req.removeAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
            res.sendRedirect(url);
        } finally {
            if(session != null) {
                session.logout();
            }
        }
    }

    /**
     * Removes the HST request context from the request and sends an error response.
     *
     * @param request HTTP servlet request
     * @param response HTTP servlet response
     * @param errorCode the error code to send
     */
    private static void sendError(HttpServletRequest request, HttpServletResponse response, int errorCode) throws IOException {
        request.removeAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
        response.sendError(errorCode);
    }

    protected void processResolvedSiteMapItem(HttpServletRequest req, HttpServletResponse res, HstMutableRequestContext requestContext, boolean processHandlers, Logger logger) throws ContainerException {
    	ResolvedSiteMapItem resolvedSiteMapItem = requestContext.getResolvedSiteMapItem();

    	if (processHandlers) {
        	// run the sitemap handlers if present: the returned resolvedSiteMapItem can be a different one then the one that is put in
    		resolvedSiteMapItem = processHandlers(resolvedSiteMapItem, req, res);
    		if(resolvedSiteMapItem == null) {
    			// one of the handlers has finished the request already
    			return;
    		}
    		// sync possibly changed ResolvedSiteMapItem
    		requestContext.setResolvedSiteMapItem(resolvedSiteMapItem);
    	}

		if (resolvedSiteMapItem.getErrorCode() > 0) {
			try {
				if (logger.isDebugEnabled()) {
					logger.debug("The resolved sitemap item for {} has error status: {}", requestContext.getBaseURL().getRequestPath(), Integer.valueOf(resolvedSiteMapItem.getErrorCode()));
				}
				res.sendError(resolvedSiteMapItem.getErrorCode());

			} catch (IOException e) {
				if (logger.isDebugEnabled()) {
					logger.warn("Exception invocation on sendError().", e);
				} else if (logger.isWarnEnabled()) {
					logger.warn("Exception invocation on sendError().");
				}
			}
			// we're done:
			return;
		}

        if (resolvedSiteMapItem.getStatusCode() > 0) {
            logger.debug("Setting the status code to '{}' for '{}' because the matched sitemap item has specified the status code" 
                    ,String.valueOf(resolvedSiteMapItem.getStatusCode()), req.getRequestURL().toString() );
            res.setStatus(resolvedSiteMapItem.getStatusCode());
        }

		HstServices.getRequestProcessor().processRequest(this.requestContainerConfig, requestContext, req, res, resolvedSiteMapItem.getNamedPipeline());

		// now, as long as there is a forward, we keep invoking processResolvedSiteMapItem:
        if(req.getAttribute(ContainerConstants.HST_FORWARD_PATH_INFO) != null) {
            String forwardPathInfo = (String) req.getAttribute(ContainerConstants.HST_FORWARD_PATH_INFO);
            req.removeAttribute(ContainerConstants.HST_FORWARD_PATH_INFO);

            resolvedSiteMapItem = resolvedSiteMapItem.getResolvedMount().matchSiteMapItem(forwardPathInfo);
            if(resolvedSiteMapItem == null) {
                // should not be possible as when it would be null, an exception should have been thrown
                throw new MatchException("Error resolving request to sitemap item: '"+HstRequestUtils.getFarthestRequestHost(req)+"' and '"+req.getRequestURI()+"'");
            }
            requestContext.setResolvedSiteMapItem(resolvedSiteMapItem);
            requestContext.setBaseURL(hstSitesManager.getUrlFactory().getContainerURLProvider().createURL(requestContext.getBaseURL(), forwardPathInfo));

            processResolvedSiteMapItem(req, res, requestContext, true, logger);
        }

		return;
    }

    /**
     * This method is invoked for every {@link HstSiteMapItemHandler} from the resolvedSiteMapItem that was matched from {@link ResolvedMount#matchSiteMapItem(String)}.
     * If in the for loop the <code>orginalResolvedSiteMapItem</code> switches to a different newResolvedSiteMapItem, then still
     * the handlers for  <code>orginalResolvedSiteMapItem</code> are processed and not the one from <code>newResolvedSiteMapItem</code>. If some intermediate
     * {@link HstSiteMapItemHandler#process(ResolvedSiteMapItem, HttpServletRequest, HttpServletResponse)} returns <code>null</code>, the loop and processing is stooped,
     * and <code>null</code> is returned. Entire request processing at that point is assumed to be completed already by one of the {@link HstSiteMapItemHandler}s (for
     * example if one of the handlers is a caching handler). When <code>null</code> is returned, request processing is stopped.
     * @param orginalResolvedSiteMapItem
     * @param req
     * @param res
     * @return a new or original {@link ResolvedSiteMapItem}, or <code>null</code> when request processing can be stopped
     */
    protected ResolvedSiteMapItem processHandlers(ResolvedSiteMapItem orginalResolvedSiteMapItem, HttpServletRequest req, HttpServletResponse res) {
        Logger logger = HstServices.getLogger(LOGGER_CATEGORY_NAME);

        ResolvedSiteMapItem newResolvedSiteMapItem = orginalResolvedSiteMapItem;
        List<HstSiteMapItemHandlerConfiguration> handlerConfigsFromMatchedSiteMapItem = orginalResolvedSiteMapItem.getHstSiteMapItem().getSiteMapItemHandlerConfigurations();
        for(HstSiteMapItemHandlerConfiguration handlerConfig : handlerConfigsFromMatchedSiteMapItem) {
           HstSiteMapItemHandler handler = siteMapItemHandlerFactory.getSiteMapItemHandlerInstance(requestContainerConfig, handlerConfig);
           logger.debug("Processing siteMapItemHandler for configuration handler '{}'", handlerConfig.getName() );
           try {
               newResolvedSiteMapItem = handler.process(newResolvedSiteMapItem, req, res);
               if(newResolvedSiteMapItem == null) {
                   logger.debug("handler for '{}' return null. Request processing done. Return null", handlerConfig.getName());
                   return null;
               }
           } catch (HstSiteMapItemHandlerException e){
               logger.error("Exception during executing siteMapItemHandler '"+handlerConfig.getName()+"'");
               throw e;
           }
        }
        return newResolvedSiteMapItem;
    }

    public synchronized void destroy() {
        if (clientComponentManager != null) {
            try{
                clientComponentManager.stop();
                clientComponentManager.close();
            }
            catch (Exception e) {
            	// ignored
            }
            finally {
                clientComponentManager = null;
            }
        }
    }

    /**
     * Determine if the current request is an cross-context include, as typically exercised by Portals dispatching to a targetted portlet
     * @param request
     * @return
     */
    protected boolean isCrossContextInclude(HttpServletRequest request) {
    	String includeContextPath = (String)request.getAttribute("javax.servlet.include.context_path");
    	return (includeContextPath != null && !includeContextPath.equals(request.getContextPath()));
    }

    /**
     * Writes the specified message to a filter log file, prepended by the
     * filter's name.  See {@link ServletContext#log(String)}.
     *
     * @param msg a <code>String</code> specifying
     * the message to be written to the log file
     */
    private void log(String msg) {
        filterConfig.getServletContext().log(filterConfig.getFilterName() + ": " + msg);
    }
    
    private String getConfigOrContextInitParameter(String paramName, String defaultValue) {
        String value = getInitParameter(filterConfig, filterConfig.getServletContext(), paramName, defaultValue);
        return (value != null ? value.trim() : null);
    }
    
    /**
     * Retrieves the init parameter from the filterConfig or servletContext.
     * If the init parameter is not found in filterConfig, then it will look up the init parameter from the servletContext.
     * If either filterConfig or servletContext is null, then either is not used to look up the init parameter.
     * If the parameter is not found, then it will return the defaultValue.
     * @param filterConfig filterConfig. If null, this is not used.
     * @param servletContext servletContext. If null, this is not used.
     * @param paramName parameter name
     * @param defaultValue the default value
     * @return
     */
    private static String getInitParameter(FilterConfig filterConfig, ServletContext servletContext, String paramName, String defaultValue) {
        String value = null;
        
        if (value == null && filterConfig != null) {
            value = filterConfig.getInitParameter(paramName);
        }
        
        if (value == null && servletContext != null) {
            value = servletContext.getInitParameter(paramName);
        }
        
        if (value == null) {
            value = defaultValue;
        }
        
        return value;
    }
}
