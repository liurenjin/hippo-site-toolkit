/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.pagecomposer.jaxrs.services;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.pagecomposer.jaxrs.api.PageCopyContext;
import org.hippoecm.hst.pagecomposer.jaxrs.model.DocumentRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.MountRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapPageRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapPagesRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.treepicker.AbstractTreePickerRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.treepicker.SiteMapTreePickerRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.SiteMapHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validators.NotNullValidator;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validators.Validator;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validators.ValidatorBuilder;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validators.ValidatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_SITEMAP;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_WORKSPACE;
import static org.hippoecm.hst.pagecomposer.jaxrs.util.DocumentUtils.findAvailableDocumentRepresentations;
import static org.hippoecm.hst.pagecomposer.jaxrs.util.DocumentUtils.getDocumentRepresentationHstConfigUser;

@Path("/" + HstNodeTypes.NODETYPE_HST_SITEMAP + "/")
@Produces(MediaType.APPLICATION_JSON)
public class SiteMapResource extends AbstractConfigResource {

    private static final Logger log = LoggerFactory.getLogger(SiteMapResource.class);

    private SiteMapHelper siteMapHelper;
    private ValidatorFactory validatorFactory;

    public void setSiteMapHelper(final SiteMapHelper siteMapHelper) {
        this.siteMapHelper = siteMapHelper;
    }

    public void setValidatorFactory(final ValidatorFactory validatorFactory) {
        this.validatorFactory = validatorFactory;
    }

    @GET
    @Path("/mount")
    public Response getMountRepresentation() {
        return tryGet(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                final Mount mount = getPageComposerContextService().getEditingMount();
                return ok("Hostname loaded successfully", new MountRepresentation().represent(mount));
            }
        });
    }

    @GET
    @Path("/pages")
    public Response getSiteMapPages() {
        return tryGet(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                final HstSite site = getPageComposerContextService().getEditingPreviewSite();
                final HstSiteMap siteMap = site.getSiteMap();
                final Mount mount = getPageComposerContextService().getEditingMount();
                final SiteMapPagesRepresentation pages = new SiteMapPagesRepresentation().represent(siteMap,
                        mount, getPreviewConfigurationPath());
                return ok("Sitemap loaded successfully", pages);
            }
        });
    }

    @GET
    @Path("/item/{siteMapItemUuid}")
    public Response getSiteMapItem(@PathParam("siteMapItemUuid") final String siteMapItemUuid) {
        return tryGet(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                final HstSiteMapItem siteMapItem = siteMapHelper.getConfigObject(siteMapItemUuid);
                final HstSite site = getPageComposerContextService().getEditingPreviewSite();
                final HstComponentsConfiguration componentsConfiguration = site.getComponentsConfiguration();
                String componentConfigurationId = siteMapItem.getComponentConfigurationId();
                final HstComponentConfiguration page = componentsConfiguration.getComponentConfiguration(componentConfigurationId);

                DocumentRepresentation primaryDocumentRepresentation = null;
                final String relativeContentPath = siteMapItem.getRelativeContentPath();
                final String rootContentPath = getPageComposerContextService().getEditingMount().getContentPath();
                if (relativeContentPath != null) {
                    final String absPath;
                    if (relativeContentPath.startsWith("/")) {
                        absPath = rootContentPath + relativeContentPath;
                    } else {
                        absPath = rootContentPath + "/" + relativeContentPath;
                    }
                    primaryDocumentRepresentation = getDocumentRepresentationHstConfigUser(absPath);
                    primaryDocumentRepresentation.setSelected(true);
                }
                Set<DocumentRepresentation> availableDocumentRepresentations = findAvailableDocumentRepresentations(
                        getPageComposerContextService(), page, primaryDocumentRepresentation, true, rootContentPath + "/");

                final SiteMapItemRepresentation siteMapItemRepresentation = new SiteMapItemRepresentation()
                        .represent(siteMapItem, getPageComposerContextService().getEditingMount(), primaryDocumentRepresentation, availableDocumentRepresentations);

                return ok("Sitemap item loaded successfully", siteMapItemRepresentation);
            }
        });
    }

    @GET
    @Path("/picker")
    public Response getSiteMapTreePicker() {
        return tryGet(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                AbstractTreePickerRepresentation representation = SiteMapTreePickerRepresentation.representRequestSiteMap(getPageComposerContextService());
                return ok("Sitemap loaded successfully", representation);
            }
        });
    }

    @POST
    @Path("/update")
    public Response update(final SiteMapItemRepresentation siteMapItem) {
        final ValidatorBuilder preValidatorBuilder = ValidatorBuilder.builder()
                .add(validatorFactory.getHasPreviewConfigurationValidator(getPageComposerContextService()))
                .add(validatorFactory.getCurrentPreviewConfigurationValidator(siteMapItem.getId(), siteMapHelper))
                .add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationWorkspacePath(),
                        siteMapItem.getId(), HstNodeTypes.NODETYPE_HST_SITEMAPITEM))
                .add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationPath(), getPageComposerContextService().getRequestConfigIdentifier(),
                        HstNodeTypes.NODETYPE_HST_SITEMAP))
                .add(validatorFactory.getNameValidator(siteMapItem.getName()))
                .add(validatorFactory.getPathInfoValidator(siteMapItem, null, siteMapHelper));

        // if the update has a uuid for componenent id, we need to re-apply a prototype. In that case we also need to
        // validate the prototype page
        boolean isCompIdUUID = false;
        if (siteMapItem.getComponentConfigurationId() != null) {
            try {
                UUID.fromString(siteMapItem.getComponentConfigurationId());
                // new page id: reapply prototype
                preValidatorBuilder.add(validatorFactory.getPrototypePageValidator(siteMapItem.getComponentConfigurationId()));
                isCompIdUUID = true;
            } catch (IllegalArgumentException e) {
                // no problem: no new page id has been set
            }
        }

        final boolean reapplyPrototype = isCompIdUUID;
        preValidatorBuilder.add(new NotNullValidator(siteMapItem.getName(), ClientError.ITEM_NO_NAME));
        return tryExecute(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                siteMapHelper.update(siteMapItem, reapplyPrototype);
                final SiteMapPageRepresentation siteMapPageRepresentation = createSiteMapPageRepresentation(siteMapItem.getId(), null);
                return ok("Item updated successfully", siteMapPageRepresentation);
            }
        }, preValidatorBuilder.build());
    }

    @POST
    @Path("/create")
    public Response create(final SiteMapItemRepresentation siteMapItem) {
        return create(siteMapItem, null);
    }

    @POST
    @Path("/create/{parentId}")
    public Response create(final SiteMapItemRepresentation siteMapItem,
                           final @PathParam("parentId") String parentId) {
        final ValidatorBuilder preValidators = ValidatorBuilder.builder()
                .add(validatorFactory.getHasPreviewConfigurationValidator(getPageComposerContextService()))
                .add(validatorFactory.getNotNullValidator(siteMapItem.getName(), ClientError.ITEM_NO_NAME))
                .add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationPath(), getPageComposerContextService().getRequestConfigIdentifier(),
                        HstNodeTypes.NODETYPE_HST_SITEMAP))
                .add(validatorFactory.getPrototypePageValidator(siteMapItem.getComponentConfigurationId()))
                .add(validatorFactory.getNameValidator(siteMapItem.getName()))
                .add(validatorFactory.getPathInfoValidator(siteMapItem, parentId, siteMapHelper));

        if (parentId != null) {
            preValidators.add(validatorFactory.getCurrentPreviewConfigurationValidator(parentId, siteMapHelper));
            preValidators.add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationWorkspacePath(),
                    parentId, HstNodeTypes.NODETYPE_HST_SITEMAPITEM));
        }

        return tryExecute(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                Node newSiteMapItem = siteMapHelper.create(siteMapItem, parentId);
                SiteMapPageRepresentation siteMapPageRepresentation = createSiteMapPageRepresentation(newSiteMapItem.getIdentifier() , parentId);
                return ok("Item created successfully", siteMapPageRepresentation);
            }
        }, preValidators.build());
    }

    @POST
    @Path("/duplicate/{siteMapItemId}")
    public Response copy(final @PathParam("siteMapItemId") String siteMapItemId) {
        final ValidatorBuilder preValidators = ValidatorBuilder.builder()
                .add(validatorFactory.getHasPreviewConfigurationValidator(getPageComposerContextService()))
                .add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationPath(), getPageComposerContextService().getRequestConfigIdentifier(),
                        HstNodeTypes.NODETYPE_HST_SITEMAP));
        preValidators.add(validatorFactory.getCurrentPreviewConfigurationValidator(siteMapItemId, siteMapHelper));

        return tryExecute(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                Node copy = siteMapHelper.duplicate(siteMapItemId);
                final SiteMapPageRepresentation siteMapPageRepresentation = createSiteMapPageRepresentation(copy.getIdentifier(), null);
                return ok("Item created successfully", siteMapPageRepresentation);
            }
        }, preValidators.build());
    }

    @POST
    @Path("/copy")
    public Response copy(
            @HeaderParam("targetName")final String targetName,
            @HeaderParam("siteMapItemUUId") final String siteMapItemUUId,
            @HeaderParam("targetSiteMapItemUUID")final String targetSiteMapItemUUID,
            @HeaderParam("mountId")final String mountId) {

        final ValidatorBuilder preValidators = ValidatorBuilder.builder()
                .add(validatorFactory.getNotNullValidator(targetName,
                        ClientError.INVALID_NAME, "Name of the copied page is not allowed to be null"))
                .add(validatorFactory.getHasPreviewConfigurationValidator(getPageComposerContextService()))
                .add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationPath(), getPageComposerContextService().getRequestConfigIdentifier(),
                        HstNodeTypes.NODETYPE_HST_SITEMAP));
        preValidators.add(validatorFactory.getCurrentPreviewConfigurationValidator(siteMapItemUUId, siteMapHelper));
        if (StringUtils.isNotBlank(targetSiteMapItemUUID)) {
            // TODO cross channel copy does need a different validator than 'getCurrentPreviewConfigurationValidator'
            preValidators.add(validatorFactory.getCurrentPreviewConfigurationValidator(targetSiteMapItemUUID, siteMapHelper));
            preValidators.add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationWorkspacePath(),
                    targetSiteMapItemUUID, HstNodeTypes.NODETYPE_HST_SITEMAPITEM));
            preValidators.add(validatorFactory.getCanCopyFromSourceToTargetValidator(siteMapItemUUId,
                    targetSiteMapItemUUID));
        }

        return tryExecute(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                PageCopyContext pcc = siteMapHelper.copy(siteMapItemUUId,
                        targetSiteMapItemUUID, targetName);
                final SiteMapPageRepresentation siteMapPageRepresentation = createSiteMapPageRepresentation(pcc.getNewSiteMapNode().getIdentifier(), null);
                return ok("Item created successfully", siteMapPageRepresentation);
            }
        }, preValidators.build());
    }


    /**
     * if <code>parentId</code> is <code>null</code> the move will be done to the root sitemap
     */
    @POST
    @Path("/move/{id}/{parentId}")
    public Response move(final @PathParam("id") String id,
                         final @PathParam("parentId") String parentId) {
        final ValidatorBuilder preValidators = ValidatorBuilder.builder()
                .add(validatorFactory.getHasPreviewConfigurationValidator(getPageComposerContextService()))
                .add(validatorFactory.getCurrentPreviewConfigurationValidator(id, siteMapHelper))
                .add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationWorkspacePath(),
                        id, HstNodeTypes.NODETYPE_HST_SITEMAPITEM))
                .add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationPath(), getPageComposerContextService().getRequestConfigIdentifier(),
                        HstNodeTypes.NODETYPE_HST_SITEMAP));

        if (parentId != null) {
            preValidators.add(validatorFactory.getCurrentPreviewConfigurationValidator(parentId, siteMapHelper));
            preValidators.add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationWorkspacePath(),
                    parentId, HstNodeTypes.NODETYPE_HST_SITEMAPITEM));
        }
        return tryExecute(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                siteMapHelper.move(id, parentId);
                final SiteMapPageRepresentation siteMapPageRepresentation = createSiteMapPageRepresentation(id, parentId);
                return ok("Item moved successfully", siteMapPageRepresentation);
            }
        }, preValidators.build());
    }

    @POST
    @Path("/delete/{id}")
    public Response delete(final @PathParam("id") String id) {
        final Validator preValidator = ValidatorBuilder.builder()
                .add(validatorFactory.getHasPreviewConfigurationValidator(getPageComposerContextService()))
                .add(validatorFactory.getCurrentPreviewConfigurationValidator(id, siteMapHelper))
                .add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationWorkspacePath(),
                        id, HstNodeTypes.NODETYPE_HST_SITEMAPITEM))
                .add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationPath(), getPageComposerContextService().getRequestConfigIdentifier(),
                        HstNodeTypes.NODETYPE_HST_SITEMAP))
                .build();

        return tryExecute(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                siteMapHelper.delete(id);
                return ok("Item deleted successfully", id);
            }
        }, preValidator);
    }


    private SiteMapPageRepresentation createSiteMapPageRepresentation(final String siteMapItemUUID, final String parentId) throws RepositoryException {
        SiteMapPageRepresentation siteMapPageRepresentation = new SiteMapPageRepresentation();
        siteMapPageRepresentation.setId(siteMapItemUUID);
        // siteMapPathInfo without starting /
        Node siteMapItem = getPageComposerContextService().getRequestContext().getSession().getNodeByIdentifier(siteMapItemUUID);
        String siteMapPathInfo = StringUtils.substringAfter(siteMapItem.getPath(), NODENAME_HST_WORKSPACE + "/" + NODENAME_HST_SITEMAP );
        siteMapPageRepresentation.setPathInfo(siteMapPathInfo.substring(1));
        final Mount mount = getPageComposerContextService().getEditingMount();
        siteMapPageRepresentation.setRenderPathInfo(mount.getMountPath() + siteMapPathInfo);
        siteMapPageRepresentation.setParentId(parentId);
        return siteMapPageRepresentation;
    }

}
