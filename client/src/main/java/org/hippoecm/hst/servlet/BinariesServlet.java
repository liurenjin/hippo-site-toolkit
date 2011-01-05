/*
 *  Copyright 2010 Hippo.
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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.cache.HstCache;
import org.hippoecm.hst.container.HstFilter;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.linking.LocationResolver;
import org.hippoecm.hst.core.linking.ResourceContainer;
import org.hippoecm.hst.core.linking.ResourceLocationResolver;
import org.hippoecm.hst.servlet.utils.BinariesCache;
import org.hippoecm.hst.servlet.utils.BinaryPage;
import org.hippoecm.hst.servlet.utils.ContentDispositionUtils;
import org.hippoecm.hst.servlet.utils.HeaderUtils;
import org.hippoecm.hst.servlet.utils.ResourceUtils;
import org.hippoecm.hst.servlet.utils.SessionUtils;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.ServletConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serves binary files from the repository. Binary files are represented by nodes.
 *
 * <p/>This servlet has the ability to set the "Content-Disposition" header in the HTTP response to tell browsers to
 * download a binary file instead of trying to display the file directly. This needs some configuration, which is
 * described below.
 *
 * <h2>Content disposition configuration</h2>
 * To configure which mime types to enable content dispositioning for, set the "contentDispositionContentTypes" init
 * param in the web.xml in which this servlet is defined. Example:
 * <pre>
 * &lt;init-param&gt;
 *     &lt;param-name&gt;contentDispositionContentTypes&lt;/param-name&gt;
 *     &lt;param-value&gt;
 *         application/pdf,
 *         application/rtf,
 *         application/excel
 *     &lt;/param-value&gt;
 * &lt;/init-param&gt;
 * </pre>
 * 
 * In the above init param configuration, you can also set glob style configurations such as '*&#x2F;*' or 'application&#x2F;*'.
 *
 * Also, you can configure the JCR property to get the file name from. The file name is used to send along in the
 * HTTP response for content dispositioning. To configure this, set the "contentDispositionFilenameProperty" init
 * param in the web.xml in which this servlet is defined. Example:
 *
 * <pre>
 * &lt;init-param&gt;
 *     &lt;param-name&gt;contentDispositionFilenameProperty&lt;/param-name&gt;
 *     &lt;param-value&gt;demosite:filename&lt;/param-value&gt;
 * &lt;/init-param&gt;
 * </pre>
 * 
 * The contentDispositionFilenameProperty is being encoded.  By default, we try to do this user-agent-agnostic. This is preferrable when using 
 * caching reverse proxies in front of your application. In user-agent-agnostic mode, we try to convert filenames consisting non ascii chars to their
 * base form, in other words, replace diacritics. However for for example a language like Chinese this won't work. Then, you might want to opt for the 
 * user-agent-specific mode. You then have to take care of your reverse proxies taking care of the user-agent. They thus should take the user-agent into account.
 * Also see {@link #encodeContentDispositionFileName(HttpServletRequest, HttpServletResponse, String)}. 
 * Changing the default user-agent-agnostic mode to user-agent-specific mode can be done by adding the init-param:
 * 
 * <pre>
 * &lt;init-param&gt;
 *     &lt;param-name&gt;contentDispositionFilenameEncoding&lt;/param-name&gt;
 *     &lt;param-value&gt;user-agent-specific&lt;/param-value&gt;
 * &lt;/init-param&gt;
 * </pre>
 * 
 * You can also configure multiple JCR property names in the above init parameter by comma-separated value.
 */
public class BinariesServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(BinariesServlet.class);

    private static final String CACHE_NAME_INIT_PARAM = "cache-name";
    
    private static final String CACHE_MAX_OBJECT_SIZE_BYTES_INIT_PARAM = "cache-max-object-size-bytes";
    
    private static final String VALIDITY_CHECK_INTERVAL_SECONDS = "validity-check-interval-seconds";

    private static final String SET_EXPIRES_HEADERS_INIT_PARAM = "set-expires-headers";

    public static final String BASE_BINARIES_CONTENT_PATH_INIT_PARAM = "baseBinariesContentPath";

    public static final String CONTENT_DISPOSITION_CONTENT_TYPES_INIT_PARAM = "contentDispositionContentTypes";

    public static final String CONTENT_DISPOSITION_FILENAME_PROPERTY_INIT_PARAM = "contentDispositionFilenameProperty";

    /**
     * The init param indicating whether the fileName for the content disposition can be encoded 'user-agent-specific' or 
     * 'user-agent-agnostic', also see {@link #encodeContentDispositionFileName(HttpServletRequest, HttpServletResponse, String)}
     */
    public static final String CONTENT_DISPOSITION_FILENAME_ENCODING_INIT_PARAM = "contentDispositionFilenameEncoding";

    public static final String BINARY_RESOURCE_NODE_TYPE_INIT_PARAM = "binaryResourceNodeType";

    public static final String BINARY_DATA_PROP_NAME_INIT_PARAM = "binaryDataPropName";

    public static final String BINARY_MIME_TYPE_PROP_NAME_INIT_PARAM = "binaryMimeTypePropName";

    public static final String BINARY_LAST_MODIFIED_PROP_NAME_INIT_PARAM = "binaryLastModifiedPropName";

    private static final boolean DEFAULT_SET_EXPIRES_HEADERS = true;

    private String baseBinariesContentPath = ResourceUtils.DEFAULT_BASE_BINARIES_CONTENT_PATH;

    private Set<String> contentDispositionContentTypes;

    private String[] contentDispositionFilenamePropertyNames;

    private String contentDispositionFileNameEncoding = ContentDispositionUtils.USER_AGENT_AGNOSTIC_CONTENT_DISPOSITION_FILENAME_ENCODING;

    private Map<String, List<ResourceContainer>> prefix2ResourceContainer;

    private List<ResourceContainer> allResourceContainers;

    private boolean initialized = false;

    private boolean setExpires = DEFAULT_SET_EXPIRES_HEADERS;

    private String binaryResourceNodeType = ResourceUtils.DEFAULT_BINARY_RESOURCE_NODE_TYPE;

    private String binaryDataPropName = ResourceUtils.DEFAULT_BINARY_DATA_PROP_NAME;

    private String binaryMimeTypePropName = ResourceUtils.DEFAULT_BINARY_MIME_TYPE_PROP_NAME;

    private String binaryLastModifiedPropName = ResourceUtils.DEFAULT_BINARY_LAST_MODIFIED_PROP_NAME;

    /** FIXME: BinariesCache is not serializable. */
    private BinariesCache binariesCache;

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        initBinariesConfig();
        initContentDispostion();
        initExpires();
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!initialized) {
            synchronized (this) {
                doInit();
            }
        }

        final BinaryPage page = getPageFromCacheOrLoadPage(request);

        response.setStatus(page.getStatus());
        if (page.getStatus() != HttpServletResponse.SC_OK) {
            // nothing left to do
            return;
        }
        
        boolean setExpiresNeeded = setExpires;
        
        if (ContentDispositionUtils.isContentDispositionType(page.getMimeType(), contentDispositionContentTypes)) {
            setExpiresNeeded = false;
            ContentDispositionUtils.addContentDispositionHeader(request, response, page.getFileName(),
                    contentDispositionFileNameEncoding);
        }

        HeaderUtils.setLastModifiedHeaders(response, page);
        if (setExpiresNeeded) {
            HeaderUtils.setExpiresHeaders(response, page);
        }

        if (HeaderUtils.hasMatchingEtag(request, page)) {
            log.debug("Matching ETag for uri {} , page {}", request.getRequestURI(), page.getResourcePath());
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        if (!HeaderUtils.isModifiedSince(request, page)) {
            log.debug("Page not modified for uri {} , page {}", request.getRequestURI(), page.getResourcePath());
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        response.setContentType(page.getMimeType());
        response.setHeader("ETag", page.getETag());

        InputStream input = null;
        OutputStream output = null;
        Session session = null;
        try {
            if (page.containsData()) {
                input = page.getStream();
            } else {
                session = SessionUtils.getBinariesSession(request);
                input = getRepositoryResourceStream(session, page);
            }
            output = response.getOutputStream();
            IOUtils.copy(input, output);
            output.flush();
        } catch (RepositoryException e) {
            log.error(
                    "RepositoryException while getting stream for binaries request '" + request.getRequestURI() + "'",
                    e);
            binariesCache.removePage(page);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unable to stream binary content to client: " + e.getMessage());
        } catch (IOException e) {
            log.error("IOException while getting stream for binaries request '" + request.getRequestURI() + "'", e);
        } finally {
            IOUtils.closeQuietly(input);
            IOUtils.closeQuietly(output);
            SessionUtils.releaseSession(request, session);
        }
    }

    private InputStream getRepositoryResourceStream(Session session, BinaryPage page) throws RepositoryException {
        Node resourceNode = ResourceUtils.lookUpResource(session, page.getResourcePath(), prefix2ResourceContainer,
                allResourceContainers);
        return resourceNode.getProperty(binaryDataPropName).getStream();
    }

    private BinaryPage getPageFromCacheOrLoadPage(HttpServletRequest request) {
        String resourcePath = ResourceUtils.getResourcePath(request, baseBinariesContentPath);
        BinaryPage page = binariesCache.getPageFromBlockingCache(resourcePath);
        if (page != null) {
            page = getValidatedPageFromCache(request, page);
        } else {
            page = getPage(request, resourcePath);
            binariesCache.putPage(page);
        }
        return page;
    }

    private BinaryPage getValidatedPageFromCache(HttpServletRequest request, BinaryPage page) {
        if (HeaderUtils.isForcedCheck(request) || binariesCache.mustCheckValidity(page)) {
            long lastModified = getLastModifiedFromResource(request, page.getResourcePath());
            if (binariesCache.isPageStale(page, lastModified)) {
                binariesCache.removePage(page);
                page = getPage(request, page.getResourcePath());
                binariesCache.putPage(page);
            } else {
                binariesCache.updateNextValidityCheckTime(page);
            }
        }
        return page;
    }

    private long getLastModifiedFromResource(HttpServletRequest request, String resourcePath) {
        Session session = null;
        try {
            session = SessionUtils.getBinariesSession(request);
            Node resourceNode = ResourceUtils.lookUpResource(session, resourcePath, prefix2ResourceContainer,
                    allResourceContainers);
            return ResourceUtils.getLastModifiedDate(resourceNode, binaryLastModifiedPropName);
        } catch (RepositoryException e) {
            log.error("Repository exception while resolving binaries request '" + request.getRequestURI() + "' : "
                    + e.getMessage(), e);
        } finally {
            SessionUtils.releaseSession(request, session);
        }
        return -1L;
    }

    private BinaryPage getPage(HttpServletRequest request, String resourcePath) {
        BinaryPage page = new BinaryPage(resourcePath);
        Session session = null;
        try {
            session = SessionUtils.getBinariesSession(request);
            initPageValues(session, page);
            log.info("Page loaded: " + page);
        } catch (RepositoryException e) {
            page.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            log.error("Repository exception while resolving binaries request '" + request.getRequestURI() + "' : "
                    + e.getMessage(), e);
        } finally {
            SessionUtils.releaseSession(request, session);
        }
        return page;
    }

    private void initPageValues(Session session, BinaryPage page) throws RepositoryException {
        if (!ResourceUtils.isValidResourcePath(page.getResourcePath())) {
            page.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Node resourceNode = ResourceUtils.lookUpResource(session, page.getResourcePath(), prefix2ResourceContainer,
                allResourceContainers);
        if (resourceNode == null) {
            page.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (!ResourceUtils.hasValideType(resourceNode, binaryResourceNodeType)) {
            page.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
            return;
        }

        if (!ResourceUtils.hasBinaryProperty(resourceNode, binaryDataPropName)) {
            page.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (!ResourceUtils.hasMimeTypeProperty(resourceNode, binaryMimeTypePropName)) {
            page.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
            return;
        }

        page.setStatus(HttpServletResponse.SC_OK);
        page.setMimeType(resourceNode.getProperty(binaryMimeTypePropName).getString());
        page.setLastModified(ResourceUtils.getLastModifiedDate(resourceNode, binaryLastModifiedPropName));
        page.setNextValidityCheckTime(System.currentTimeMillis() + binariesCache.getValidityCheckIntervalMillis());
        page.setFileName(ResourceUtils.getFileName(resourceNode, contentDispositionFilenamePropertyNames));
        page.setLength(ResourceUtils.getDataLength(resourceNode, binaryDataPropName));

        if (binariesCache.isBinaryDataCacheable(page)) {
            cacheBinaryData(page, resourceNode);
        }
    }

    private void cacheBinaryData(BinaryPage page, Node resourceNode) {
        try {
            InputStream input = resourceNode.getProperty(binaryDataPropName).getStream();
            page.loadDataFromStream(input);
        } catch (RepositoryException e) {
            log.warn("Unable to cache page data for " + page.getResourcePath(), e);
        } catch (IOException e) {
            page.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            log.error("Error while copying datastream from resource node " + page.getResourcePath(), e);
        }
    }

    private void doInit() {
        if (initialized) {
            return;
        }
        
        if (HstServices.isAvailable()) {
            initPrefix2ResourceMappers();
            initAllResourceContainers();
            initBinariesCache();
            initialized = true;
        }
    }

    private void initPrefix2ResourceMappers() {
        if (prefix2ResourceContainer != null) {
            return;
        }
        HstLinkCreator linkCreator = HstServices.getComponentManager().getComponent(HstLinkCreator.class.getName());
        if (linkCreator.getLocationResolvers() == null) {
            prefix2ResourceContainer = Collections.emptyMap();
            return;
        }
        prefix2ResourceContainer = new HashMap<String, List<ResourceContainer>>();
        for (LocationResolver resolver : linkCreator.getLocationResolvers()) {
            if (resolver instanceof ResourceLocationResolver) {
                ResourceLocationResolver resourceResolver = (ResourceLocationResolver) resolver;
                for (ResourceContainer container : resourceResolver.getResourceContainers()) {
                    if (container.getMappings() == null) {
                        continue;
                    }
                    for (String prefix : container.getMappings().values()) {
                        List<ResourceContainer> resourceContainersForPrefix = prefix2ResourceContainer.get(prefix);
                        if (resourceContainersForPrefix == null) {
                            resourceContainersForPrefix = new ArrayList<ResourceContainer>();
                            prefix2ResourceContainer.put(prefix, resourceContainersForPrefix);
                        }
                        resourceContainersForPrefix.add(container);
                    }
                }
            }
        }
    }

    private void initAllResourceContainers() {
        if (allResourceContainers != null) {
            return;
        }
        HstLinkCreator linkCreator = HstServices.getComponentManager().getComponent(HstLinkCreator.class.getName());
        if (linkCreator.getLocationResolvers() == null) {
            allResourceContainers = Collections.emptyList();
            return;
        }
        allResourceContainers = new ArrayList<ResourceContainer>();
        for (LocationResolver resolver : linkCreator.getLocationResolvers()) {
            if (resolver instanceof ResourceLocationResolver) {
                ResourceLocationResolver resourceResolver = (ResourceLocationResolver) resolver;
                for (ResourceContainer container : resourceResolver.getResourceContainers()) {
                    allResourceContainers.add(container);
                }
            }
        }
    }

    private void initBinariesConfig() {
        baseBinariesContentPath = getInitParameter(BASE_BINARIES_CONTENT_PATH_INIT_PARAM, baseBinariesContentPath);
        binaryResourceNodeType = getInitParameter(BINARY_RESOURCE_NODE_TYPE_INIT_PARAM, binaryResourceNodeType);
        binaryDataPropName = getInitParameter(BINARY_DATA_PROP_NAME_INIT_PARAM, binaryDataPropName);
        binaryMimeTypePropName = getInitParameter(BINARY_MIME_TYPE_PROP_NAME_INIT_PARAM, binaryMimeTypePropName);
        binaryLastModifiedPropName = getInitParameter(BINARY_LAST_MODIFIED_PROP_NAME_INIT_PARAM,
                binaryLastModifiedPropName);
    }

    private void initContentDispostion() throws ServletException {
        contentDispositionFilenamePropertyNames = StringUtils.split(getInitParameter(
                CONTENT_DISPOSITION_FILENAME_PROPERTY_INIT_PARAM, null), ", \t\r\n");

        // Parse mime types from init-param
        contentDispositionContentTypes = new HashSet<String>();
        String mimeTypesString = getInitParameter(CONTENT_DISPOSITION_CONTENT_TYPES_INIT_PARAM, null);
        if (mimeTypesString != null) {
            contentDispositionContentTypes.addAll(Arrays.asList(StringUtils.split(mimeTypesString, ", \t\r\n")));
        }

        contentDispositionFileNameEncoding = getInitParameter(CONTENT_DISPOSITION_FILENAME_ENCODING_INIT_PARAM,
                contentDispositionFileNameEncoding);
        if (!ContentDispositionUtils.USER_AGENT_AGNOSTIC_CONTENT_DISPOSITION_FILENAME_ENCODING
                .equals(contentDispositionFileNameEncoding)
                && !ContentDispositionUtils.USER_AGENT_SPECIFIC_CONTENT_DISPOSITION_FILENAME_ENCODING
                        .equals(contentDispositionFileNameEncoding)) {
            throw new ServletException(
                    "Invalid init-param: the only allowed values for contentDispositionFilenameEncoding are '"
                            + ContentDispositionUtils.USER_AGENT_AGNOSTIC_CONTENT_DISPOSITION_FILENAME_ENCODING
                            + "' or '"
                            + ContentDispositionUtils.USER_AGENT_SPECIFIC_CONTENT_DISPOSITION_FILENAME_ENCODING + "'");
        }
    }

    private void initExpires() {
        setExpires = getBooleanInitParameter(SET_EXPIRES_HEADERS_INIT_PARAM, DEFAULT_SET_EXPIRES_HEADERS);
    }

    private void initBinariesCache() {
        HstCache cache = null;
        
        String binariesCacheComponentName = getInitParameter(CACHE_NAME_INIT_PARAM, "defaultBinariesCache");
        ComponentManager clientComponentManager = (ComponentManager) getServletContext().getAttribute(HstFilter.CLIENT_COMPONENT_MANAGER_CONTEXT_ATTRIBUTE_NAME_INIT_PARAM);
        
        if (clientComponentManager != null) {
            cache = clientComponentManager.getComponent(binariesCacheComponentName);
        }
        
        if (cache == null) {
            cache = HstServices.getComponentManager().getComponent(binariesCacheComponentName);
        }
        
        binariesCache = new BinariesCache(cache);
        binariesCache.setMaxObjectSizeBytes(getLongInitParameter(CACHE_MAX_OBJECT_SIZE_BYTES_INIT_PARAM,
                BinariesCache.DEFAULT_MAX_OBJECT_SIZE_BYTES));
        binariesCache.setValidityCheckIntervalMillis(getLongInitParameter(VALIDITY_CHECK_INTERVAL_SECONDS,
                BinariesCache.DEFAULT_VALIDITY_CHECK_INTERVAL_MILLIS/ 1000 ));
    }

    private String getInitParameter(String paramName, String defaultValue) {
        return ServletConfigUtils.getInitParameter(getServletConfig(), null, paramName, defaultValue);
    }

    private boolean getBooleanInitParameter(String paramName, boolean defaultValue) {
        String value = ServletConfigUtils.getInitParameter(getServletConfig(), null, paramName, null);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }
    
    private long getLongInitParameter(String paramName, long defaultValue) {
        String value = ServletConfigUtils.getInitParameter(getServletConfig(), null, paramName, null);
        if (value != null) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                log.warn("Expecting type long for parameter '{}', got '{}'", paramName, value);
            }
        }
        return defaultValue;
    }

}
