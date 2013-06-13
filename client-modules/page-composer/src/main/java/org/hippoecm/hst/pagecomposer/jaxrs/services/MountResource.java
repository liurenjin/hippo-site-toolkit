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
package org.hippoecm.hst.pagecomposer.jaxrs.services;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.MutableMount;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.internal.ContextualizableMount;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.content.beans.ObjectBeanPersistenceException;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManagerImpl;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.model.DocumentRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.PageModelRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ToolkitRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.util.HstConfigurationUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.util.NodeIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
@Path("/hst:mount/")
public class MountResource extends AbstractConfigResource {
    private static Logger log = LoggerFactory.getLogger(MountResource.class);

    @GET
    @Path("/pagemodel/{pageId}/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPageModelRepresentation(@Context HttpServletRequest servletRequest,
                                               @PathParam("pageId") String pageId) {
        try { 
            final HstRequestContext requestContext = getRequestContext(servletRequest);
            final HstSite editingPreviewHstSite = getEditingPreviewSite(requestContext);
            if (editingPreviewHstSite == null) {
                log.error("Could not get the editing site to create the page model representation.");
                return error("Could not get the editing site to create the page model representation.");
            }
            final PageModelRepresentation pageModelRepresentation = new PageModelRepresentation().represent(editingPreviewHstSite, pageId, getEditingPreviewMount(requestContext));
            return ok("PageModel loaded successfully", pageModelRepresentation.getComponents().toArray());
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve page model.", e);
            } else {
                log.warn("Failed to retrieve page model. {}", e.toString());
            }
            return error(e.toString());
        }
    }
    
    @GET
    @Path("/toolkit/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getToolkitRepresentation(@Context HttpServletRequest servletRequest) {
        final HstRequestContext requestContext = getRequestContext(servletRequest);
        final Mount editingMount = getEditingPreviewMount(requestContext);
        if (editingMount == null) {
            log.error("Could not get the editing site to create the toolkit representation.");
            return error("Could not get the editing site to create the toolkit representation.");
        }

        setCurrentMountCanonicalContentPath(servletRequest, editingMount.getCanonicalContentPath());

        ToolkitRepresentation toolkitRepresentation = new ToolkitRepresentation().represent(editingMount);
        return ok("Toolkit items loaded successfully", toolkitRepresentation.getComponents().toArray());
    }

    /**
     * Try to lock a mount.
     * @param servletRequest
     * @return  ok - already-locked {@link Response} when the mount was already locked.
     *          ok - lock-acquired {@link Response} when the lock was acquired
     *          error {@link Response} when something went wrong
     */
    @POST
    @Path("/lock/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setLock(@Context HttpServletRequest servletRequest) {
        final HstRequestContext requestContext = getRequestContext(servletRequest);

        MutableMount editingPreviewMount = (MutableMount)getEditingPreviewMount(requestContext);
        if (editingPreviewMount == null) {
            return error("This mount is not suitable for the template composer.");
        }

        if (editingPreviewMount.getVirtualHost().getVirtualHosts().isFinegrainedLocking()) {
            log.debug("Mount '{}' is finegrained locking hence setting lock is skipped", editingPreviewMount.getIdentifier());
            return ok("Finegrained locked mounts are lock free. Use lock-acquired to fake this. ", "lock-acquired");
        }

        String configPath = editingPreviewMount.getHstSite().getConfigurationPath();

        if(configPath != null) {
            try {
                Session session = requestContext.getSession();
                Node configurationNode = session.getNode(configPath);
                if (HstConfigurationUtils.isLockedBySomeoneElse(configurationNode)) {
                    return ok("This configuration was already locked.", "already-locked");
                } else {
                    setLockProperties(configurationNode);
                    HstConfigurationUtils.persistChanges(session, getHstManager());
                    return ok("This configuration lock was acquired.", "lock-acquired");
                }
            } catch (LoginException e) {
                return error("Could not get a jcr session : " + e + ".");
            } catch (RepositoryException e) {
                return error("Could not check lock : " + e );
            }
        }
        return error("Could not find the mount configuration.");
    }

    /**
     * If the {@link Mount} that this request belongs to does not have a preview configuration, it will 
     * be created. If it already has a preview configuration, just an ok {@link Response} is returned.
     * @param servletRequest
     * @return ok {@link Response} when editing can start, and error {@link Response} otherwise
     */
    @POST
    @Path("/edit/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response startEdit(@Context HttpServletRequest servletRequest) {
        final HstRequestContext requestContext = getRequestContext(servletRequest);

        try {
            final MutableMount editingPreviewMount = (MutableMount)getEditingPreviewMount(requestContext);
            final HstSite ctxEditingPreviewSite = editingPreviewMount.getHstSite();
            
            Session session = requestContext.getSession();
            
            if (ctxEditingPreviewSite.hasPreviewConfiguration()) {
                if (editingPreviewMount.getVirtualHost().getVirtualHosts().isFinegrainedLocking()) {
                    log.debug("Mount '{}' is finegrained locking hence checking lock is skipped", editingPreviewMount.getIdentifier());
                    return ok("Site can be edited now");
                }
                String configPath = ctxEditingPreviewSite.getConfigurationPath();
                Node configurationNode = session.getNode(configPath);
                if (HstConfigurationUtils.isLockedBySomeoneElse(configurationNode)) {
                    return error("This channel is locked.", "locked");
                }
                if (!HstConfigurationUtils.isLockedBySession(configurationNode)) {
                    setLockProperties(configurationNode);
                    HstConfigurationUtils.persistChanges(session, getHstManager());
                }
                return ok("Site can be edited now");
            }

            Node newPreviewConfigurationNode = createPreviewConfigurationNode(requestContext);
            if (!editingPreviewMount.getVirtualHost().getVirtualHosts().isFinegrainedLocking()) {
                setLockProperties(newPreviewConfigurationNode);
            }
            HstConfigurationUtils.persistChanges(session, getHstManager());
        } catch (IllegalStateException e) {
            return error("Cannot start editing : " + e);
        } catch (LoginException e) {
            return error("Could not get a jcr session : " + e + ". Cannot create a  preview configuration.");
        } catch (RepositoryException e) {
            return error("Could not create a preview configuration : " + e);
        }
   
        return ok("Site can be edited now");
    }

    private Node createPreviewConfigurationNode(final HstRequestContext requestContext) throws RepositoryException {
        HstSite ctxEditingLiveMountSite = getEditingLiveMount(requestContext).getHstSite();
        long liveVersion = ctxEditingLiveMountSite.getVersion();
        long newVersion = liveVersion + 1;
        String liveConfigurationPath = ctxEditingLiveMountSite.getConfigurationPath();
        String newPreviewConfigurationPath = getNewPreviewConfigurationPath(newVersion, liveConfigurationPath);
        Session session = requestContext.getSession();

        // cannot cast session from request context to HippoSession, hence, get it from jcr node first
        Node liveConfiguration = session.getNode(liveConfigurationPath);
        HippoSession hippoSession = HstConfigurationUtils.getNonProxiedSession(session);
        hippoSession.copy(liveConfiguration, newPreviewConfigurationPath);
        Node newPreviewConfigurationNode = session.getNode(newPreviewConfigurationPath);

        setVersionForPreviewSitesWithConfiguration(liveConfigurationPath, requestContext, newVersion);
        return newPreviewConfigurationNode;
    }

    /**
     * If the {@link Mount} that this request belongs to has a preview configuration, it will be discarded.
     * @param servletRequest
     * @return ok {@link Response} when the discard completed, error {@link Response} otherwise
     */
    @POST
    @Path("/discard/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response discardChanges(@Context HttpServletRequest servletRequest) {
        final HstRequestContext requestContext = getRequestContext(servletRequest);
        final MutableMount editingPreviewMount = (MutableMount)getEditingPreviewMount(requestContext);
        if (editingPreviewMount.getVirtualHost().getVirtualHosts().isFinegrainedLocking()) {
            return fineGrainedDiscardChangesOfCurrentUser(requestContext);
        } else {
            return coarseGrainedDiscardIfNotLocked(requestContext);
        }
    }

    /**
     * If the {@link Mount} that this request belongs to has a preview configuration, it will be unlocked and deleted.
     * @param servletRequest
     * @return ok {@link Response} when the discard completed, error {@link Response} otherwise
     */
    @POST
    @Path("/unlock/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response unlock(@Context HttpServletRequest servletRequest) {
        final HstRequestContext requestContext = getRequestContext(servletRequest);
        MutableMount editingPreviewMount = (MutableMount)getEditingPreviewMount(requestContext);
        if (editingPreviewMount == null) {
            return error("This mount is not suitable for the template composer.");
        }

        if (editingPreviewMount.getVirtualHost().getVirtualHosts().isFinegrainedLocking()) {
            log.debug("Mount '{}' is finegrained locking hence unlocking is skipped", editingPreviewMount.getIdentifier());
            return ok("Unlocking global lock skipped for finegrained locking mounts.");
        }
        return deletePreviewConfiguration(requestContext);
    }

    /**
     * If the {@link Mount} that this request belongs to does not have a preview configuration, it will 
     * be created. If it already has a preview configuration, just an ok {@link Response} is returned.
     * @param servletRequest
     * @return ok {@link Response} when editing can start, and error {@link Response} otherwise
     */
    @POST
    @Path("/publish/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response publish(@Context HttpServletRequest servletRequest) {
        final HstRequestContext requestContext = getRequestContext(servletRequest);
        final MutableMount editingPreviewMount = (MutableMount)getEditingPreviewMount(requestContext);

        if (!hasPreviewConfiguration(editingPreviewMount)) {
            return error("Cannot publish non preview site");
        }

        if (editingPreviewMount.getVirtualHost().getVirtualHosts().isFinegrainedLocking()) {
            return fineGrainedPublishChangesOfCurrentUser(requestContext);
        } else {
            return coarseGrainedPublicationIfNotLocked(requestContext);
        }
    }

    private Response coarseGrainedPublicationIfNotLocked(final HstRequestContext requestContext) {
        final HstSite editingLiveSite = getEditingLiveSite(requestContext);
        final HstSite editingPreviewSite = getEditingPreviewSite(requestContext);

        String previewConfigPath = editingPreviewSite.getConfigurationPath();

        try {
            Session session = requestContext.getSession();
            Node previewConfigurationNode = session.getNode(previewConfigPath);
            if (HstConfigurationUtils.isLockedBySomeoneElse(previewConfigurationNode)) {
                return error("Locked by another user.", "locked");
            }

            removeLockProperties(previewConfigurationNode);
            session.removeItem(editingLiveSite.getConfigurationPath());
            long newVersion = editingPreviewSite.getVersion();
            setVersionForLiveSitesWithConfiguration(editingLiveSite.getConfigurationPath(), requestContext, newVersion);
            HstConfigurationUtils.persistChanges(session, getHstManager());
        } catch (LoginException e) {
            return error("Could not get a jcr session : " + e + ". Cannot publish configuration.");
        } catch (RepositoryException e) {
            return error("Could not publish preview configuration : " + e);
        }

        return ok("Site is published");

    }

    private Response fineGrainedPublishChangesOfCurrentUser(final HstRequestContext requestContext) {
        try {
            String liveConfigurationPath = getEditingLiveMount(requestContext).getHstSite().getConfigurationPath();
            String previewConfigurationPath = getEditingPreviewMount(requestContext).getHstSite().getConfigurationPath();

            HippoSession session = HstConfigurationUtils.getNonProxiedSession(requestContext.getSession(false));
            List<String> relativePathsToPublish = findChangedContainersForUser(session, previewConfigurationPath);
            pushNodes(session, previewConfigurationPath, liveConfigurationPath, relativePathsToPublish);
            return ok("Site is published");
        } catch (RepositoryException e) {
            return error("Could not publish preview configuration : " + e);
        }
    }

    private String getNewPreviewConfigurationPath(final long newVersion, final String liveConfigurationPath) {
        StringBuilder newPreviewConfigurationPathBuilder = new StringBuilder();
        newPreviewConfigurationPathBuilder.append(StringUtils.substringBeforeLast(liveConfigurationPath, "/"));
        final String liveConfigurationNodeName = StringUtils.substringAfterLast(liveConfigurationPath, "/");
        final String previewConfigurationNodeName;
        if (newVersion == 0) {
            // liveConfiguration name does not contain a version yet
            previewConfigurationNodeName = liveConfigurationNodeName + "-v0";
        } else {
            String oldVersionPostfix = "-v"+(newVersion - 1);
            if (liveConfigurationNodeName.indexOf(oldVersionPostfix) == -1) {
                throw new IllegalStateException("Live configuration node '"+liveConfigurationNodeName+"' contains unexpected version.");
            }
            previewConfigurationNodeName = StringUtils.substringBefore(liveConfigurationNodeName, oldVersionPostfix) + "-v"+newVersion;
        }
        newPreviewConfigurationPathBuilder.append("/").append(previewConfigurationNodeName);
        return newPreviewConfigurationPathBuilder.toString();
    }

    private void setVersionForPreviewSitesWithConfiguration(final String configurationPath,
                                                               final HstRequestContext requestContext,
                                                               final long newVersion) throws RepositoryException {
        setVersionForSitesWithConfiguration(configurationPath, requestContext, newVersion, false);
    }

    private void setVersionForLiveSitesWithConfiguration(final String configurationPath,
                                                            final HstRequestContext requestContext,
                                                            final long newVersion) throws RepositoryException {
        setVersionForSitesWithConfiguration(configurationPath, requestContext, newVersion, true);
    }

    private void setVersionForSitesWithConfiguration(final String configurationPath,
                                                        final HstRequestContext requestContext,
                                                        final long newVersion,
                                                        final boolean liveSites) throws RepositoryException {
        Set<HstSite> sitesWithSameConfigurationPath = new HashSet<HstSite>();
        final VirtualHosts virtualHosts = requestContext.getResolvedMount().getMount().getVirtualHost().getVirtualHosts();
        for (String hostGroupName : virtualHosts.getHostGroupNames()) {
            for (Mount mount : virtualHosts.getMountsByHostGroup(hostGroupName)) {
                HstSite site;
                if (liveSites) {
                    if (mount.isPreview()) {
                        continue;
                    }
                    site = mount.getHstSite();
                } else {
                    if (mount instanceof ContextualizableMount) {
                        site = ((ContextualizableMount)mount).getPreviewHstSite();
                    } else {
                        continue;
                    }
                }
                
                if (site == null) {
                    continue;
                }
                if (configurationPath.equals(site.getConfigurationPath())) {
                    sitesWithSameConfigurationPath.add(site);
                }
            }
        }

        for (HstSite siteWithSameConfigurationPath : sitesWithSameConfigurationPath) {
            Node siteNode = requestContext.getSession().getNodeByIdentifier(siteWithSameConfigurationPath.getCanonicalIdentifier());
            if (liveSites) {
                // double check whether siteNode is really NOT a preview
                if (!siteNode.getName().endsWith("-preview")) {
                    siteNode.setProperty(HstNodeTypes.SITE_VERSION, newVersion);
                }
            } else {
                // double check whether siteNode is really a preview
                if (siteNode.getName().endsWith("-preview")) {
                    siteNode.setProperty(HstNodeTypes.SITE_VERSION, newVersion);
                }
            }

        }
    }
    
    /**
     * Creates a document in the repository using the WorkFlowManager
     * The post parameters should contain the 'path', 'docType' and 'name' of the document.
     * @param servletRequest Servlet Request
     * @param params The POST parameters
     * @return response JSON with the status of the result
     */
    @POST
    @Path("/create/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response createDocument(@Context HttpServletRequest servletRequest,
                                   MultivaluedMap<String, String> params) {

        final HstRequestContext requestContext = getRequestContext(servletRequest);
        try {
            final Mount editingPreviewMount = getEditingPreviewMount(requestContext);
            if (editingPreviewMount == null) {
                log.warn("Could not get the editing mount to get the content path for creating the document.");
                return error("Could not get the editing mount to get the content path for creating the document.");
            }
            String canonicalContentPath = editingPreviewMount.getCanonicalContentPath();
            WorkflowPersistenceManagerImpl workflowPersistenceManager = new WorkflowPersistenceManagerImpl(requestContext.getSession(),
                    getObjectConverter(requestContext));
            workflowPersistenceManager.createAndReturn(canonicalContentPath + "/" + params.getFirst("docLocation"), params.getFirst("docType"), params.getFirst("docName"), true);
        } catch (RepositoryException e) {
            log.warn("Exception happened while trying to create the document " + e, e);
            return error("Exception happened while trying to create the document " + e);
        } catch (ObjectBeanPersistenceException e) {
            log.warn("Exception happened while trying to create the document " + e, e);
            return error("Exception happened while trying to create the document " + e);
        }
        return ok("Successfully created a document", null);
    }

    /**
     * Method that returns a {@link Response} containing the list of document of (sub)type <code>docType</code> that
     * belong to the content of the site that is currently composed.
     * @param servletRequest
     * @param docType         the docType the found documents must be of. The documents can also be a subType of
     *                        docType
     * @return An ok Response containing the list of documents or an error response in case an exception occurred
     */
    @POST
    @Path("/documents/{docType}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDocumentsByType(@Context HttpServletRequest servletRequest,
                                       @PathParam("docType") String docType) {

        final HstRequestContext requestContext = getRequestContext(servletRequest);
        final Mount editingHstMount = getEditingPreviewMount(requestContext);
        if (editingHstMount == null) {
            log.warn("Could not get the editing mount to get the content path for listing documents.");
            return error("Could not get the editing mount to get the content path for listing documents.");
        }
        List<DocumentRepresentation> documentLocations = new ArrayList<DocumentRepresentation>();
        String canonicalContentPath = editingHstMount.getCanonicalContentPath();
        try {
            Session session = requestContext.getSession();

            Node contentRoot = (Node) session.getItem(canonicalContentPath);

            String statement = "//element(*," + docType + ")[@hippo:paths = '" + contentRoot.getIdentifier() + "' and @hippo:availability = 'preview' and not(@jcr:primaryType='nt:frozenNode')]";
            QueryManager queryMngr = session.getWorkspace().getQueryManager();
            QueryResult result = queryMngr.createQuery(statement, "xpath").execute();
            NodeIterator documents = result.getNodes();
            while (documents.hasNext()) {
                Node doc = documents.nextNode();
                if (doc == null) {
                    continue;
                }
                if (doc.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                    // take the handle
                    doc = doc.getParent();
                }
                String docPath = doc.getPath();
                if (!docPath.startsWith(canonicalContentPath + "/")) {
                    log.warn("Unexpected document path '{}'", docPath);
                    continue;
                }
                documentLocations.add(new DocumentRepresentation(docPath.substring(canonicalContentPath.length() + 1)));
            }
        } catch (RepositoryException e) {
            log.warn("Exception happened while trying to fetch documents of type '" + docType + "'", e);
            return error("Exception happened while trying to fetch documents of type '" + docType + "': " + e.getMessage());
        }
        return ok("Document list", documentLocations);
    }

    private Response coarseGrainedDiscardIfNotLocked(final HstRequestContext requestContext) {
        final Mount editingPreviewMount = getEditingPreviewMount(requestContext);
        try {
            Node previewConfigurationNode = getConfigurationNodeForMount(requestContext.getSession(), editingPreviewMount);
            if (HstConfigurationUtils.isLockedBySomeoneElse(previewConfigurationNode)) {
                return error("Preview for '"+editingPreviewMount.getMountPoint()+"' cannot be deleted because locked by another user.", "locked");
            }
        } catch (RepositoryException e) {
            return error("Could not discard preview configuration : " + e);
        }
        return deletePreviewConfiguration(requestContext);
    }

    private Response deletePreviewConfiguration(final HstRequestContext requestContext) {

        final Mount editingPreviewMount = getEditingPreviewMount(requestContext);
        if (!hasPreviewConfiguration(editingPreviewMount)) {
            return error("Cannot discard non preview site because there is no preview configuration");
        }

        String previewConfigPath = editingPreviewMount.getHstSite().getConfigurationPath();
        try {
            Session session = requestContext.getSession();
            session.removeItem(previewConfigPath);
            resetVersionToLive(requestContext);
            HstConfigurationUtils.persistChanges(session, getHstManager());
        } catch (LoginException e) {
            return error("Could not get a jcr session : " + e  + ". Cannot discard configuration.");
        } catch (RepositoryException e) {
            return error("Could not discard preview configuration : " + e);
        }
        return ok("Template is discarded");
        
    }

    private void resetVersionToLive(final HstRequestContext requestContext) throws RepositoryException {
        long liveVersion = getEditingLiveMount(requestContext).getHstSite().getVersion();
        HstSite previewSite = getEditingPreviewMount(requestContext).getHstSite();
        setVersionForPreviewSitesWithConfiguration(previewSite.getConfigurationPath(), requestContext, liveVersion);
    }


    /**
     * reverts the changes for the current cms user for the channel he is working on.
     * reverting changes need to be done directly on JCR level as for the hst model it get very complex as the
     * hst model has an enhanced model on top of jcr, with for example inheritance and referencing resolved
     */
    private Response fineGrainedDiscardChangesOfCurrentUser(final HstRequestContext requestContext) {
        try {
            String liveConfigurationPath = getEditingLiveMount(requestContext).getHstSite().getConfigurationPath();
            String previewConfigurationPath = getEditingPreviewMount(requestContext).getHstSite().getConfigurationPath();

            HippoSession session = HstConfigurationUtils.getNonProxiedSession(requestContext.getSession(false));
            List<String> relativePathsToRevert = findChangedContainersForUser(session, previewConfigurationPath);
            pushNodes(session, liveConfigurationPath, previewConfigurationPath, relativePathsToRevert);
            return ok("Changes of user '"+session.getUserID()+"' for site '"+getEditingPreviewMount(requestContext).getHstSite().getName()+"' are discarded.");

        } catch (RepositoryException e) {
            return error("Could not discard preview configuration : " + e);
        }
    }

    private List<String> findChangedContainersForUser(final HippoSession session, String previewConfigurationPath) throws RepositoryException {
        String xpath = "/jcr:root" + previewConfigurationPath+"//element(*,"+HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT+")" +
                "[@"+HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY+" = '"+session.getUserID()+"']";
        final QueryResult result = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH).execute();

        final NodeIterable containersToRevert = new NodeIterable(result.getNodes());
        List<String> relativePathsToRevert = new ArrayList<String>();
        for (Node containerToRevert : containersToRevert) {
            String containerPath = containerToRevert.getPath();
            if (!containerPath.startsWith(previewConfigurationPath)) {
                log.warn("Cannot discard container '{}' because does not start with preview config path '{}'.");
                continue;
            }
            relativePathsToRevert.add(containerToRevert.getPath().substring(previewConfigurationPath.length()));
        }
        return relativePathsToRevert;
    }

    private void pushNodes(final HippoSession session,
                           final String fromConfig,
                           final String toConfig,
                           final List<String> relativeConfigPaths) throws RepositoryException {

        for (String relativeConfigPath : relativeConfigPaths) {
            String absFromPath = fromConfig + relativeConfigPath;
            String absToPath = toConfig + relativeConfigPath;
            final Node rootNode = session.getRootNode();
            if (rootNode.hasNode(absFromPath.substring(1)) && rootNode.hasNode(absToPath.substring(1))) {
                final Node containerToRemove = rootNode.getNode(absToPath.substring(1));
                Node absFromNode = rootNode.getNode(absFromPath.substring(1));
                if (!containerToRemove.isNodeType(HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT) ||
                        !absFromNode.isNodeType(HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT)) {
                    log.warn("Cannot publish/discard nodes that are not of type hst:containercomponent. Cannot push " +
                            " '{}' to '{}'.", containerToRemove.getPath(), absFromNode.getPath());
                    continue;
                }
                containerToRemove.remove();
                if (absFromNode.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY)) {
                    absFromNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).remove();
                }
                if (absFromNode.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON)) {
                    absFromNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON).remove();
                }
                absFromNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED, Calendar.getInstance());
                absFromNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED_BY, session.getUserID());
                session.copy(absFromNode, absToPath);
            } else {
                log.warn("Cannot push node path '{}' because live or preview version for '{}' is not available.",
                        absToPath, relativeConfigPath);
            }
        }
        HstConfigurationUtils.persistChanges(session, getHstManager());
    }


    private Node getConfigurationNodeForMount(final Session session, final Mount mount) throws RepositoryException {
        String previewConfigPath = mount.getHstSite().getConfigurationPath();
        return session.getNode(previewConfigPath);
    }

    private boolean hasPreviewConfiguration(final Mount editingPreviewMount) {
        return editingPreviewMount.getHstSite().hasPreviewConfiguration();
    }

    /**
     * sets a not yet saved lock properties
     */
    private void setLockProperties(Node configurationNode) throws RepositoryException {
        assertCorrectNodeType(configurationNode, HstNodeTypes.NODETYPE_HST_CONFIGURATION);
        if (HstConfigurationUtils.isLockedBySomeoneElse(configurationNode)) {
            throw new IllegalStateException("Cannot lock '"+configurationNode.getPath()+"' because locked by someone else.");
        }
        configurationNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY, configurationNode.getSession().getUserID());
        configurationNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON, new GregorianCalendar());
    }

    private void removeLockProperties(Node configurationNode) throws RepositoryException {
        assertCorrectNodeType(configurationNode, HstNodeTypes.NODETYPE_HST_CONFIGURATION);
        if (configurationNode.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY)) {
            configurationNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).remove();
        }
        if (configurationNode.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON)) {
            configurationNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON).remove();
        }
    }

    private void assertCorrectNodeType(final Node node, String nodeType) throws RepositoryException {
        if (!node.isNodeType(nodeType)) {
            throw new IllegalArgumentException("Unexpected nodetype for '"+node.getPath()+"'. Expected a node" +
                    "of type '"+nodeType+"'");
        }
    }

}