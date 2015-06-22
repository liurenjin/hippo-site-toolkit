/*
 *  Copyright 2009-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.servlet;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.proxy.Interceptor;
import org.apache.commons.proxy.Invocation;
import org.hippoecm.hst.configuration.hosting.MutableVirtualHost;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.freemarker.DelegatingTemplateLoader;
import org.hippoecm.hst.freemarker.HstClassTemplateLoader;
import org.hippoecm.hst.freemarker.jcr.JcrTemplateLoader;
import org.hippoecm.hst.freemarker.jcr.WebFileTemplateLoader;
import org.hippoecm.hst.proxy.ProxyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.core.Configurable;
import freemarker.core.Environment;
import freemarker.ext.jsp.TaglibFactory;
import freemarker.ext.servlet.AllHttpScopesHashModel;
import freemarker.ext.servlet.FreemarkerServlet;
import freemarker.template.Configuration;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import static org.hippoecm.hst.core.container.ContainerConstants.FREEMARKER_CLASSPATH_TEMPLATE_PROTOCOL;
import static org.hippoecm.hst.core.container.ContainerConstants.FREEMARKER_JCR_TEMPLATE_PROTOCOL;
import static org.hippoecm.hst.core.container.ContainerConstants.FREEMARKER_WEB_FILE_TEMPLATE_PROTOCOL;

public class HstFreemarkerServlet extends FreemarkerServlet {

    public static final String INIT_PARAM_LOGGER_LIBRARY = "loggerLibrary";

    private static final Logger log = LoggerFactory.getLogger(HstFreemarkerServlet.class);

    private static final long serialVersionUID = 1L;

    private static final String ATTR_JSP_TAGLIBS_MODEL = ".freemarker.JspTaglibs";

    private boolean lookupVirtualWebappLibResourcePathsEnabled;

    private static final String PROJECT_BASEDIR_PROPERTY = "project.basedir";

    private static final TemplateExceptionHandler LOGGING_IGNORE_HANDLER = new TemplateExceptionHandler() {
        @Override
        public void handleTemplateException(final TemplateException te, final Environment env, final Writer out) throws TemplateException {
            logFreemarkerException(te);
        }
    };

    @Override
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);

        configureLoggerLibrary();

        Configuration conf = super.getConfiguration();

        if (!hasInitParameter(Configurable.TEMPLATE_EXCEPTION_HANDLER_KEY)) {
            log.info("No '" + Configurable.TEMPLATE_EXCEPTION_HANDLER_KEY + "' init param set. HST will setup " +
                    " a default Freemarker TemplateExceptionHandler to log and *continue* (ignore) in case of template exceptions.");
            conf.setTemplateExceptionHandler(LOGGING_IGNORE_HANDLER);
        }

        ServletContext servletContext = config.getServletContext();
        Set libPaths = servletContext.getResourcePaths("/WEB-INF/lib");
        lookupVirtualWebappLibResourcePathsEnabled = (libPaths == null || libPaths.isEmpty());

        ProxyFactory factory = new ProxyFactory();
        Interceptor interceptor = new Interceptor() {
            public Object intercept(Invocation invocation) throws Throwable {
                Method method = invocation.getMethod();
                String methodName = method.getName();
                Object[] args = invocation.getArguments();

                if ("getResourcePaths".equals(methodName) && args.length > 0 && ("/WEB-INF/lib".equals(args[0]) || "/WEB-INF/lib/".equals(args[0]))) {
                    ClassLoader loader = Thread.currentThread().getContextClassLoader();

                    if (loader instanceof URLClassLoader) {
                        URL[] urls = ((URLClassLoader) loader).getURLs();

                        if (urls != null) {
                            Set<String> paths = new HashSet<>();

                            for (int i = 0; i < urls.length; i++) {
                                String url = urls[i].toString();
                                paths.add(url);
                            }

                            return paths;
                        }
                    }
                } else if ("getResourceAsStream".equals(methodName) && args.length > 0 && args[0].toString().startsWith("file:")) {
                    URL url = new URL((String) args[0]);
                    return url.openStream();
                } else if ("getResource".equals(methodName) && args.length > 0 && args[0].toString().startsWith("file:")) {
                    URL url = new URL((String) args[0]);
                    return url;
                }

                return invocation.proceed();
            }
        };

        ServletContext virtualContext = (ServletContext) factory.createInterceptorProxy(servletContext, interceptor, new Class[]{ServletContext.class});

        TaglibFactory taglibs = new TaglibFactory(virtualContext);
        servletContext.setAttribute(ATTR_JSP_TAGLIBS_MODEL, taglibs);

        final String projectBaseDir = System.getProperty(PROJECT_BASEDIR_PROPERTY);
        if (projectBaseDir != null) {
            log.info("Setting freemarker template update delay to '0ms' since running locally");
            conf.setTemplateUpdateDelay(0);
        }
        conf.setLocalizedLookup(false);
    }

    private void configureLoggerLibrary() {
        final int loggerLibrary = getLoggerLibrary();
        try {
            log.info("Using freemarker.log.Logger library '{}'", loggerLibrary);
            freemarker.log.Logger.selectLoggerLibrary(loggerLibrary);
        } catch (ClassNotFoundException e) {
            log.warn("Failed to enable logging with freemarker.log.Logger library '{}'", loggerLibrary, e);
        }
    }

    private int getLoggerLibrary() {
        if (!hasInitParameter(INIT_PARAM_LOGGER_LIBRARY)) {
            return freemarker.log.Logger.LIBRARY_NONE;
        }

        final String initParamValue = getInitParameter(INIT_PARAM_LOGGER_LIBRARY);
        switch (initParamValue) {
            case "auto": return freemarker.log.Logger.LIBRARY_AUTO;
            case "none": return freemarker.log.Logger.LIBRARY_NONE;
            case "java": return freemarker.log.Logger.LIBRARY_JAVA;
            case "avalon": return freemarker.log.Logger.LIBRARY_AVALON;
            case "log4j": return freemarker.log.Logger.LIBRARY_LOG4J;
            case "commons": return freemarker.log.Logger.LIBRARY_COMMONS;
            case "slf4j": return freemarker.log.Logger.LIBRARY_SLF4J;
            default:
                log.warn("HstFreemarkerServlet has invalid value for init param '{}': '{}'." +
                        " Valid values are 'auto', 'none' (the default), 'java', 'avalon', 'log4j', 'commons', and 'slf4j'. " +
                        " Using 'none' instead'.",
                        INIT_PARAM_LOGGER_LIBRARY, initParamValue);
                return freemarker.log.Logger.LIBRARY_NONE;
        }
    }

    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        try {
            super.doGet(request, response);
        } catch (IOException e) {
            logFreemarkerException(e);
        } catch (ServletException e) {
            Throwable wrapped = e.getCause();
            if (wrapped == null) {
                wrapped = e;
            }
            logFreemarkerException(wrapped);
        }
    }

    private static void logFreemarkerException(final Throwable t) {
        if (log.isDebugEnabled()) {
            log.warn("Error in Freemarker template:", t);
        } else {
            StringBuilder loggingURLs = new StringBuilder();
            if(RequestContextProvider.get() != null) {
                final VirtualHost virtualHost = RequestContextProvider.get().getResolvedMount().getMount().getVirtualHost();
                if (virtualHost instanceof MutableVirtualHost) {
                    List<String> cmsLocation = ((MutableVirtualHost)virtualHost).getCmsLocations();
                    for (String location : cmsLocation) {
                       if (loggingURLs.length() > 0) {
                           loggingURLs.append(", ");
                       }
                        loggingURLs.append(location + "/logging/");
                    }
                }
            }
            // by default, log a clean error message without stack traces
            String msgLoggingURLs;
            if (loggingURLs.length() > 0) {
                msgLoggingURLs = String.format(" or if enabled via %s.", loggingURLs);
            } else {
                msgLoggingURLs = ".";
            }
            String msg = String.format("To see the stack trace, set '%s' log-level to debug in log4j configuration%s" +
                    "", HstFreemarkerServlet.class.getName(), msgLoggingURLs);
            log.warn(t.getMessage() + ". " + msg);
        }
    }

    /**
     * Special dispatch info is included when the request contains the attribute {@link
     * ContainerConstants#DISPATCH_URI_PROTOCOL}. For example this value is 'classpath:' or 'jcr:' or 'webfile:' to
     * load a template from a classpath or repository
     */
    @Override
    protected String requestUrlToTemplatePath(HttpServletRequest request) throws ServletException {
        String path = super.requestUrlToTemplatePath(request);
        if (request.getAttribute(ContainerConstants.DISPATCH_URI_PROTOCOL) != null) {
            path = request.getAttribute(ContainerConstants.DISPATCH_URI_PROTOCOL) + path;
        }
        return path;
    }

    @Override
    protected TemplateModel createModel(ObjectWrapper wrapper, ServletContext servletContext,
                                        final HttpServletRequest request, final HttpServletResponse response) throws TemplateModelException {

        if (!lookupVirtualWebappLibResourcePathsEnabled) {
            return super.createModel(wrapper, servletContext, request, response);
        }

        TemplateModel params = super.createModel(wrapper, servletContext, request, response);

        if (params instanceof AllHttpScopesHashModel) {
            ((AllHttpScopesHashModel) params).putUnlistedModel(KEY_JSP_TAGLIBS, (TemplateModel) servletContext.getAttribute(ATTR_JSP_TAGLIBS_MODEL));
        }

        return params;
    }

    protected boolean hasInitParameter(String paramName) {
        if (getServletConfig().getInitParameter(paramName) != null) {
            return true;
        }
        if (getServletConfig().getServletContext().getInitParameter(paramName) != null) {
            return true;
        }
        return false;
    }

    /**
     * Overrides {@link FreemarkerServlet#createTemplateLoader(String)} in order to use {@link MultiTemplateLoader}
     * instead which cascades {@link HstClassTemplateLoader} and {@link org.hippoecm.hst.freemarker.jcr.JcrTemplateLoader}
     * until it finds a template by the <code>templatePath</code>.
     */
    @Override
    protected TemplateLoader createTemplateLoader(String templatePath) throws IOException {
        final String[] prefixExclusions = {
                FREEMARKER_CLASSPATH_TEMPLATE_PROTOCOL,
                FREEMARKER_JCR_TEMPLATE_PROTOCOL,
                FREEMARKER_WEB_FILE_TEMPLATE_PROTOCOL};
        final TemplateLoader[] loaders = {
                new DelegatingTemplateLoader(super.createTemplateLoader(templatePath), null, prefixExclusions),
                new HstClassTemplateLoader(getClass()),
                new JcrTemplateLoader(),
                new WebFileTemplateLoader()};
        return new MultiTemplateLoader(loaders);
    }
}
