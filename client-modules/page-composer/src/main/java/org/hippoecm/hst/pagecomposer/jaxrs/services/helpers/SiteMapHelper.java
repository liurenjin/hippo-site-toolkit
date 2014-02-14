/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.pagecomposer.jaxrs.services.helpers;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.util.ISO9075;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SiteMapHelper extends AbstractHelper {

    private static final Logger log = LoggerFactory.getLogger(SiteMapHelper.class);
    private static final String WORKSPACE_PATH_ELEMENT = "/" + HstNodeTypes.NODENAME_HST_WORKSPACE + "/";

    @Override
    public <T> T getConfigObject(final String itemId) {
        final HstSite editingPreviewSite = pageComposerContextService.getEditingPreviewSite();
        return (T) getSiteMapItem(editingPreviewSite.getSiteMap(), itemId);
    }

    public void update(final SiteMapItemRepresentation siteMapItem) throws RepositoryException {
        HstRequestContext requestContext = pageComposerContextService.getRequestContext();
        final Session session = requestContext.getSession();
        final String itemId = siteMapItem.getId();
        Node jcrNode = session.getNodeByIdentifier(itemId);

        acquireLock(jcrNode);

        final String modifiedName = siteMapItem.getName();
        if (modifiedName != null && !modifiedName.equals(jcrNode.getName())) {
            // we do not need to check lock for parent as this is a rename within same parent
            String oldLocation = jcrNode.getPath();
            String target = jcrNode.getParent().getPath() + "/" + modifiedName;
            validateTarget(session, target);
            session.move(jcrNode.getPath(), jcrNode.getParent().getPath() + "/" + modifiedName);
            createMarkedDeletedIfLiveExists(session, oldLocation);
        }

        setSitemapItemProperties(siteMapItem, jcrNode);

        final Map<String, String> modifiedLocalParameters = siteMapItem.getLocalParameters();
        setLocalParameters(jcrNode, modifiedLocalParameters);

        final Set<String> modifiedRoles = siteMapItem.getRoles();
        setRoles(jcrNode, modifiedRoles);
    }


    public Node create(final SiteMapItemRepresentation siteMapItem, final String parentId) throws RepositoryException {

        HstRequestContext requestContext = pageComposerContextService.getRequestContext();
        final Session session = requestContext.getSession();
        Node parent = session.getNodeByIdentifier(parentId);

        validateTarget(session, parent.getPath() + "/" + siteMapItem.getName());

        final Node newChild = parent.addNode(siteMapItem.getName(), HstNodeTypes.NODETYPE_HST_SITEMAPITEM);
        acquireLock(newChild);
        // TODO clone page definition
        setSitemapItemProperties(siteMapItem, newChild);

        final Map<String, String> modifiedLocalParameters = siteMapItem.getLocalParameters();
        setLocalParameters(newChild, modifiedLocalParameters);

        final Set<String> modifiedRoles = siteMapItem.getRoles();
        setRoles(newChild, modifiedRoles);
        return newChild;
    }

    public void move(final String id, final String parentId) throws RepositoryException {
        if (id.equals(parentId)) {
            final String msg = "Cannot move node to become child of itself";
            throw new ClientException(ClientError.INVALID_MOVE_TO_SELF, msg);
        }
        HstRequestContext requestContext = pageComposerContextService.getRequestContext();
        final Session session = requestContext.getSession();
        Node nodeToMove = session.getNodeByIdentifier(id);
        Node newParent = session.getNodeByIdentifier(parentId);
        Node oldParent = nodeToMove.getParent();
        if (oldParent.isSame(newParent)) {
            log.info("Move to same parent for '" + nodeToMove.getPath() + "' does not result in a real move");
            return;
        }
        if (hasSelfOrAncestorLockBySomeOneElse(newParent)) {
            throw new IllegalStateException("Cannot move node to '" + newParent.getPath() + "' because that node is locked " +
                    "by '" + getSelfOrAncestorLockedBy(newParent) + "'");
        }
        acquireLock(nodeToMove);
        String nodeName = nodeToMove.getName();
        validateTarget(session, newParent.getPath() + "/" + nodeName);
        String oldLocation = nodeToMove.getPath();
        session.move(oldParent.getPath() + "/" + nodeName, newParent.getPath() + "/" + nodeName);
        acquireLock(nodeToMove);

        createMarkedDeletedIfLiveExists(session, oldLocation);
    }

    public void delete(final String id) throws RepositoryException {
        HstRequestContext requestContext = pageComposerContextService.getRequestContext();
        final Session session = requestContext.getSession();
        Node toDelete = session.getNodeByIdentifier(id);
        acquireLock(toDelete);
        deleteOrMarkDeletedIfLiveExists(toDelete);
    }


    public static HstSiteMapItem getSiteMapItem(HstSiteMap siteMap, String siteMapItemId) {

        for (HstSiteMapItem hstSiteMapItem : siteMap.getSiteMapItems()) {
            final HstSiteMapItem siteMapItem = getSiteMapItem(hstSiteMapItem, siteMapItemId);
            if (siteMapItem != null) {
                return siteMapItem;
            }
        }

        final String msg = "SiteMap item with id '%s' is not part of currently edited preview site.";
        throw new ClientException(ClientError.ITEM_NOT_IN_PREVIEW, msg, siteMapItemId);
    }

    public static HstSiteMapItem getSiteMapItem(HstSiteMapItem siteMapItem, String siteMapItemId) {
        if (!(siteMapItem instanceof CanonicalInfo)) {
            return null;
        }
        if (((CanonicalInfo) siteMapItem).getCanonicalIdentifier().equals(siteMapItemId)) {
            return siteMapItem;
        }
        for (HstSiteMapItem child : siteMapItem.getChildren()) {
            HstSiteMapItem o = getSiteMapItem(child, siteMapItemId);
            if (o != null) {
                return o;
            }
        }
        return null;
    }


    private void setSitemapItemProperties(final SiteMapItemRepresentation siteMapItem, final Node jcrNode) throws RepositoryException {
        setProperty(jcrNode, HstNodeTypes.SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID, siteMapItem.getComponentConfigurationId());
        setProperty(jcrNode, HstNodeTypes.SITEMAPITEM_PROPERTY_SCHEME, siteMapItem.getScheme());
        setProperty(jcrNode, HstNodeTypes.SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH, siteMapItem.getRelativeContentPath());
    }


    private void createMarkedDeletedIfLiveExists(final Session session, final String oldLocation) throws RepositoryException {
        boolean liveExists = liveExists(session, oldLocation);
        if (liveExists) {
            Node deleted = session.getRootNode().addNode(oldLocation.substring(1), HstNodeTypes.NODETYPE_HST_SITEMAPITEM);
            markDeleted(deleted);
        }
    }

    private void deleteOrMarkDeletedIfLiveExists(final Node toDelete) throws RepositoryException {
        boolean liveExists = liveExists(toDelete.getSession(), toDelete.getPath());
        if (liveExists) {
            markDeleted(toDelete);
        } else {
            toDelete.remove();
        }
    }

    private boolean liveExists(final Session session, final String previewLocation) throws RepositoryException {
        if (!previewLocation.contains("-preview/hst:workspace/")) {
            throw new IllegalStateException("Unexpected location '" + previewLocation + "'");
        }
        String liveLocation = previewLocation.replace("-preview/hst:workspace/", "/hst:workspace/");
        return session.nodeExists(liveLocation);
    }

    private void markDeleted(final Node deleted) throws RepositoryException {
        acquireLock(deleted);
        deleted.setProperty(HstNodeTypes.EDITABLE_PROPERTY_STATE, "deleted");
    }

    private boolean isMarkedDeleted(final Node node) throws RepositoryException {
        return "deleted".equals(JcrUtils.getStringProperty(node, HstNodeTypes.EDITABLE_PROPERTY_STATE, null));
    }

    private void validateTarget(final Session session, final String target) throws RepositoryException {
        // check non workspace sitemap for collisions
        final HstSiteMap siteMap = pageComposerContextService.getEditingPreviewSite().getSiteMap();
        if (!(siteMap instanceof CanonicalInfo)) {
            throw new IllegalStateException("Unexpected sitemap for site '" + siteMap.getSite().getName() + "' because not an instanceof CanonicalInfo");
        }
        if (!target.contains(WORKSPACE_PATH_ELEMENT)) {
            throw new IllegalArgumentException("Target '" + target + "' expected to at least contain ");
        }
        if (session.nodeExists(target)) {
            Node targetNode = session.getNode(target);
            if (isMarkedDeleted(targetNode)) {
                // see if we own the lock
                acquireLock(targetNode);
                targetNode.remove();
            } else {
                final String msg = "Target node '%s' already exists";
                throw new ClientException(ClientError.ITEM_NAME_NOT_UNIQUE, msg, targetNode.getPath());
            }
        } else {
            final CanonicalInfo canonical = (CanonicalInfo) siteMap;
            if (canonical.isWorkspaceConfiguration()) {
                // the hst:sitemap node is from workspace so there is no non workspace sitemap for current site (inherited one
                // does not have precendence)
                return;
            } else {
                // non workspace sitemap
                final Node siteMapNode = session.getNodeByIdentifier(canonical.getCanonicalIdentifier());
                final Node siteNode = siteMapNode.getParent();
                if (!siteNode.isNodeType(HstNodeTypes.NODETYPE_HST_CONFIGURATION)) {
                    throw new IllegalStateException("Expected node type '" + HstNodeTypes.NODETYPE_HST_CONFIGURATION + "' for " +
                            "'" + siteNode.getPath() + "' but was '" + siteNode.getPrimaryNodeType().getName() + "'.");
                }
                if (!target.startsWith(siteNode.getPath() + "/")) {
                    throw new IllegalArgumentException("Target '" + target + "' does not start with the path of the " +
                            "targeted hst site '" + siteMapNode.getPath() + "'.");
                }
                // check whether non workspace sitemap does not already contain the target without /hst:workspace/ part
                String nonWorkspaceTarget = target.replace(WORKSPACE_PATH_ELEMENT, "/");
                // now we have a path like /hst:hst/hst:configurations/myproject/hst:sitemap/foo/bar/lux
                // we need to make sure 'foo' does not already exist
                String siteMapRelPath = nonWorkspaceTarget.substring(siteMapNode.getPath().length() + 1);
                String[] segments = siteMapRelPath.split("/");
                if (siteMapNode.hasNode(segments[0])) {
                    final String msg = "Target '%s' not allowed since the *non-workspace* sitemap already contains '%s'";
                    throw new ClientException(ClientError.ITEM_EXISTS_IN_NON_WORKSPACE, msg, target, siteMapNode.getPath() + "/" + segments[0]);
                }
                // valid!
                return;
            }
        }
    }

    @Override
    protected String buildXPathQueryLockedWorkspaceNodesForUsers(final String previewWorkspacePath,
                                                                 final List<String> userIds) {
        if (userIds.isEmpty()) {
            throw new IllegalArgumentException("List of user IDs cannot be empty");
        }

        StringBuilder xpath = new StringBuilder("/jcr:root");
        xpath.append(ISO9075.encodePath(previewWorkspacePath + "/" + HstNodeTypes.NODENAME_HST_SITEMAP));

        xpath.append("//element(*,");
        xpath.append(HstNodeTypes.NODETYPE_HST_SITEMAPITEM);
        xpath.append(")[");

        String concat = "";
        for (String userId : userIds) {
            xpath.append(concat);
            xpath.append('@');
            xpath.append(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY);
            xpath.append(" = '");
            xpath.append(userId);
            xpath.append("'");
            concat = " or ";
        }
        xpath.append("]");

        return xpath.toString();
    }

}
