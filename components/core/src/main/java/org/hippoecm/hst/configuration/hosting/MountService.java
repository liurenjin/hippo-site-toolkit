/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.hosting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.ConfigurationUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.StringPool;
import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.configuration.internal.ContextualizableMount;
import org.hippoecm.hst.configuration.model.HstManagerImpl;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.configuration.model.HstSiteRootNode;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.site.HstSiteService;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MountService implements ContextualizableMount, MutableMount {

    private static final Logger log = LoggerFactory.getLogger(MountService.class);
    
    private static final String DEFAULT_TYPE = Mount.LIVE_NAME;
    /**
     * The name of this {@link Mount}. If it is the root, it is called hst:root
     */
    private String name;
    
    /**
     * the identifier of this {@link Mount}
     */
    private String uuid;
    
    /**
     * The virtual host of where this {@link Mount} belongs to
     */
    private VirtualHost virtualHost;

    /**
     * The channel to which this {@link Mount} belongs
     */
    private String channelPath;

    /**
     * The {@link Channel} to which this {@link Mount} belongs
     */
    private Channel channel;

    /**
     * The parent of this {@link Mount} or null when this {@link Mount} is the root
     */
    private Mount parent;

    /**
     * the HstSite this {@link Mount} points to. It can be <code>null</code>
     */
    private HstSite hstSite;
    
    /**
     * the previewHstSite equivalent of this {@link Mount}. If this {@link Mount} is a preview,
     * then previewHstSite == hstSite.  It can be <code>null</code>
     */
    private HstSite previewHstSite;
    
    /**
     * The child {@link Mount} below this {@link Mount}
     */
    private Map<String, MutableMount> childMountServices = new HashMap<String, MutableMount>();

    /**
     * the alias of this {@link Mount}. <code>null</code> if there is no alias property
     */
    private String alias;
    
    private Map<String, Object> allProperties;

    /**
     * The primary type of this {@link Mount}. If not specified, we use {@link #DEFAULT_TYPE} as a value
     */
    private String type = DEFAULT_TYPE;
    
    /**
     * The list of types excluding the primary <code>type</code> this {@link Mount} also belongs to
     */
    private List<String> types;
    
    
    /**
     * When the {@link Mount} is preview, and this isVersionInPreviewHeader is true, the used HST version is set as a response header. 
     * Default this variable is true when it is not configured explicitly
     */
    private boolean versionInPreviewHeader;
    
    /**
     * If this {@link Mount} must use some custom other than the default pipeline, the name of the pipeline is contained by <code>namedPipeline</code>
     */
    private String namedPipeline;
    

    /**
     * The mountpath of this {@link Mount}. Note that it can contain wildcards
     */
    private String mountPath;
    
    /**
     * The absolute path of the content (which might be the location of some mirror node)
     */
    private String contentPath;

    /**
     * The absolute path of the content (which might be the location of some mirror node) of the preview version of this {@link Mount}. If
     * there is no preview, or this Mount is already a preview, then previewContentPath equals contentPath
     */
    private String previewContentPath;
    
    /**
     * The absolute canonical path of the content : In case <code>contentPath</code> points to a mirror,
     * this <code>canonicalContentPath</code> points to the location the mirror points to
     */
    private String canonicalContentPath;
    
    /**
     * The absolute canonical path of the content of the preview version of this {@link Mount}: In case <code>previewContentPath</code> points to a mirror,
     * this <code>canonicalContentPath</code> points to the location the mirror points to.
     * 
     * Note that although <code>contentPath</code> and <code>previewContentPath</code> may point to a differrent mirror,
     * <code>canonicalContentPath</code> and <code>previewCanonicalContentPath</code> are most of the time equal
     */
    private String previewCanonicalContentPath;

    /**
     * The path where the {@link Mount} is pointing to
     */
    private String mountPoint;
    
    /**
     * The path where the preview equivalent of this {@link Mount} is pointing to. If this {@link Mount} is 
     * a preview. the previewMountPoint is the same as mountPoint
     */
    private String previewMountPoint;
    
    /**
     * <code>true</code> (default) when this {@link Mount} is used as a site. False when used only as content mount point and possibly a namedPipeline
     */
    private boolean isMapped = true;
    
    /**
     * The homepage for this {@link Mount}. When the backing configuration does not contain a homepage, then, the homepage from the backing {@link VirtualHost} is 
     * taken (which still might be <code>null</code> though)
     */
    private String homepage;
    

    /**
     * The pagenotfound for this {@link Mount}. When the backing configuration does not contain a pagenotfound, then, the pagenotfound from the backing {@link VirtualHost} is 
     * taken (which still might be <code>null</code> though)
     */
    private String pageNotFound;

    /**
     * whether the context path should be in the url.
     */
    private boolean contextPathInUrl;
    
    // by default, isSite = true
    private boolean isSite = true;
    
    /**
     * whether the port number should be in the url.
     */
    private boolean showPort;
    
    /**
     * default port is 0, which means, the {@link Mount} is port agnostic
     */
    private int port;
    
    /**
     *  when this {@link Mount} is only applicable for certain contextpath, this property for the contextpath tells which value it must have. It must start with a slash.
     */
    private String onlyForContextPath;

    private String scheme;
    private int schemeNotMatchingResponseCode = -1;
    
    /**
     * The locale for this {@link Mount}. When the backing configuration does not contain a locale, the value from a parent {@link Mount} is used. If there is
     * no parent, the value will be {@link VirtualHosts#getLocale()}. The locale can be <code>null</code>
     */
    private String locale;
    
    private boolean authenticated;
    
    private Set<String> roles;
    
    private Set<String> users;

    /**
     * for embedded delegation of sites a mountpath needs to point to the delegated {@link Mount}. This is only relevant for portal environment
     */
    private String embeddedMountPath;
    
    private boolean subjectBasedSession;
    
    private boolean sessionStateful;

    private final boolean cacheable;

    private String defaultResourceBundleId;

    private String formLoginPage;
    private ChannelInfo channelInfo;
    
    private String[] defaultSiteMapItemHandlerIds;
    
    private String cmsLocation;
    private String lockedBy;
    private Calendar lockedOn;

    public MountService(HstNode mount, Mount parent, VirtualHost virtualHost, HstManagerImpl hstManager, int port) throws ServiceException {
        this.virtualHost = virtualHost;
        this.parent = parent;
        this.port = port;
        this.name = StringPool.get(mount.getValueProvider().getName());
        this.uuid = mount.getValueProvider().getIdentifier();
        // default for when there is no alias property
        
        this.allProperties = mount.getValueProvider().getProperties();
      
        if(mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_ALIAS)) {
            this.alias = StringPool.get(mount.getValueProvider().getString(HstNodeTypes.MOUNT_PROPERTY_ALIAS).toLowerCase());
        }
        
        if(parent == null) {
            mountPath = "";
        } else {
            mountPath = StringPool.get((parent.getMountPath() + "/" + name));
        }
       
        // is the context path visible in the url
        if(mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_SHOWCONTEXTPATH)) {
            this.contextPathInUrl = mount.getValueProvider().getBoolean(HstNodeTypes.MOUNT_PROPERTY_SHOWCONTEXTPATH);
        } else {
            if(parent != null) {
                this.contextPathInUrl = parent.isContextPathInUrl();
            } else {
                this.contextPathInUrl = virtualHost.isContextPathInUrl();
            }
        }
     
        // is the port number visible in the url
        if(mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_SHOWPORT)) {
            this.showPort = mount.getValueProvider().getBoolean(HstNodeTypes.MOUNT_PROPERTY_SHOWPORT);
        } else {
            if(parent != null) {
                this.showPort = parent.isPortInUrl();
            } else {
                this.showPort = virtualHost.isPortInUrl();
            }
        }
        
        if(mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_ONLYFORCONTEXTPATH)) {
            this.onlyForContextPath = mount.getValueProvider().getString(HstNodeTypes.MOUNT_PROPERTY_ONLYFORCONTEXTPATH);
        } else {
            if(parent != null) {
                this.onlyForContextPath = parent.onlyForContextPath();
            } else {
                this.onlyForContextPath = virtualHost.onlyForContextPath();
            }
        }
        
        if(onlyForContextPath != null && !"".equals(onlyForContextPath)) {
            if(onlyForContextPath.startsWith("/")) {
                // onlyForContextPath starts with a slash. If it contains another /, it is configured incorrectly
                if(onlyForContextPath.substring(1).contains("/")) {
                    log.warn("Incorrectly configured 'onlyForContextPath' : It must start with a '/' and is not allowed to contain any other '/' slashes. We set onlyForContextPath to null");
                    onlyForContextPath = null;
                }
            }else {
                log.warn("Incorrect configured 'onlyForContextPath': It must start with a '/' to be used, but it is '{}'. We set onlyForContextPath to null", onlyForContextPath);
                onlyForContextPath = null;
            }
        }
        
        if(mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_SCHEME)) {
            scheme = StringPool.get(mount.getValueProvider().getString(HstNodeTypes.MOUNT_PROPERTY_SCHEME));
        }
        if (StringUtils.isBlank(scheme)) {
            scheme = parent != null ? parent.getScheme() : virtualHost.getScheme();
        }

        if(mount.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_SCHEME_NOT_MATCH_RESPONSE_CODE)) {
            schemeNotMatchingResponseCode = (int)mount.getValueProvider().getLong(HstNodeTypes.GENERAL_PROPERTY_SCHEME_NOT_MATCH_RESPONSE_CODE).longValue();
            if (ConfigurationUtils.isSupportedSchemeNotMatchingResponseCode(schemeNotMatchingResponseCode)) {
                log.warn("Invalid '{}' configured on '{}'. Use inherited value. Supported values are '{}'", new String[]{HstNodeTypes.GENERAL_PROPERTY_SCHEME_NOT_MATCH_RESPONSE_CODE,
                        mount.getValueProvider().getPath(), ConfigurationUtils.suppertedSchemeNotMatchingResponseCodesAsString()});
            }
        }
        if (schemeNotMatchingResponseCode == -1) {
            schemeNotMatchingResponseCode = parent != null ?
                    parent.getSchemeNotMatchingResponseCode() : virtualHost.getSchemeNotMatchingResponseCode();
        }
        
        if(mount.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_HOMEPAGE)) {
            this.homepage = mount.getValueProvider().getString(HstNodeTypes.GENERAL_PROPERTY_HOMEPAGE);
            homepage = StringPool.get(homepage);
        } else {
           // try to get the one from the parent
            if(parent != null) {
                this.homepage = parent.getHomePage();
            } else {
                this.homepage = virtualHost.getHomePage();
            }
        }
        
        if(mount.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCALE)) {
            this.locale = mount.getValueProvider().getString(HstNodeTypes.GENERAL_PROPERTY_LOCALE);
            locale = StringPool.get(locale);
        } else {
           // try to get the one from the parent
            if(parent != null) {
                this.locale = parent.getLocale();
            } else {
                this.locale = virtualHost.getLocale();
            }
        }
        
        if(mount.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_PAGE_NOT_FOUND)) {
            this.pageNotFound = mount.getValueProvider().getString(HstNodeTypes.GENERAL_PROPERTY_PAGE_NOT_FOUND);
            pageNotFound = StringPool.get(pageNotFound);
        } else {
           // try to get the one from the parent
            if(parent != null) {
                this.pageNotFound = parent.getPageNotFound();
            } else {
                this.pageNotFound = virtualHost.getPageNotFound();
            }
        }
        
        
        if(mount.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_VERSION_IN_PREVIEW_HEADER)) {
            this.versionInPreviewHeader = mount.getValueProvider().getBoolean(HstNodeTypes.GENERAL_PROPERTY_VERSION_IN_PREVIEW_HEADER);
        } else {
           // try to get the one from the parent
            if(parent != null) {
                this.versionInPreviewHeader = parent.isVersionInPreviewHeader();
            } else {
                this.versionInPreviewHeader = virtualHost.isVersionInPreviewHeader();
            }
        }
        
        if(mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_TYPE)) {
            this.type = mount.getValueProvider().getString(HstNodeTypes.MOUNT_PROPERTY_TYPE);
            type = StringPool.get(type);
        } else if(parent != null) {
            this.type = parent.getType();
        }
        
        if(mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_TYPES)) {
            String[] typesProperty = mount.getValueProvider().getStrings(HstNodeTypes.MOUNT_PROPERTY_TYPES);
            for(int i = 0; i< typesProperty.length ; i++) {
                typesProperty[i] = StringPool.get(typesProperty[i]);
            }
            this.types = Arrays.asList(typesProperty);
        } else if(parent != null) {
            // because the parent.getTypes also includes the primary type, below we CANNOT use parent.getTypes() !!
            this.types = ((MountService)parent).types;
        }
        
        if(mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_ISMAPPED)) {
            this.isMapped = mount.getValueProvider().getBoolean(HstNodeTypes.MOUNT_PROPERTY_ISMAPPED);
        } else if(parent != null) {
            this.isMapped = parent.isMapped();
        }
        
        if(mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_IS_SITE)) {
            this.isSite = mount.getValueProvider().getBoolean(HstNodeTypes.MOUNT_PROPERTY_IS_SITE);
        } else if(parent != null) {
            this.isSite = parent.isSite();
        }
        
        if(mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_NAMEDPIPELINE)) {
            this.namedPipeline = mount.getValueProvider().getString(HstNodeTypes.MOUNT_PROPERTY_NAMEDPIPELINE);
            namedPipeline  = StringPool.get(namedPipeline);
        } else if(parent != null) {
            this.namedPipeline = parent.getNamedPipeline();
        }
        

        if(mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_EMBEDDEDMOUNTPATH)) {
            this.embeddedMountPath = mount.getValueProvider().getString(HstNodeTypes.MOUNT_PROPERTY_EMBEDDEDMOUNTPATH);
        } else if(parent != null) {
            this.embeddedMountPath = parent.getEmbeddedMountPath();
        }
        
        if(mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT)) {
            this.mountPoint = mount.getValueProvider().getString(HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT);
            mountPoint = StringPool.get(mountPoint);
            // now, we need to create the HstSite object
            if("".equals(mountPoint)){
                mountPoint = null;
            }
        } else if(parent != null) {
            this.mountPoint = ((MountService)parent).mountPoint;
            if(mountPoint != null) {
                log.info("mountPoint for Mount '{}' is inherited from its parent Mount and is '{}'", getName() , mountPoint);
            }
        }
        
        if(Mount.PREVIEW_NAME.equals(type)) {
            previewMountPoint = mountPoint;
        } else {
            previewMountPoint = mountPoint + "-" + Mount.PREVIEW_NAME;
        }
        
        
        if (mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_AUTHENTICATED)) {
            this.authenticated = mount.getValueProvider().getBoolean(HstNodeTypes.MOUNT_PROPERTY_AUTHENTICATED);
        } else if (parent != null){
            this.authenticated = parent.isAuthenticated();
        } 
        
        if (mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_ROLES)) {
            String [] rolesProp = mount.getValueProvider().getStrings(HstNodeTypes.MOUNT_PROPERTY_ROLES);
            this.roles = new HashSet<String>();
            CollectionUtils.addAll(this.roles, rolesProp);
        } else if (parent != null){
            this.roles = new HashSet<String>(parent.getRoles());
        } else {
            this.roles = new HashSet<String>();
        }
        
        if (mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_USERS)) {
            String [] usersProp = mount.getValueProvider().getStrings(HstNodeTypes.MOUNT_PROPERTY_USERS);
            this.users = new HashSet<String>();
            CollectionUtils.addAll(this.users, usersProp);
        } else if (parent != null){
            this.users = new HashSet<String>(parent.getUsers());
        } else {
            this.users = new HashSet<String>();
        }
        
        if (mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_SUBJECTBASEDSESSION)) {
            this.subjectBasedSession = mount.getValueProvider().getBoolean(HstNodeTypes.MOUNT_PROPERTY_SUBJECTBASEDSESSION);
        } else if (parent != null){
            this.subjectBasedSession = parent.isSubjectBasedSession();
        }
        
        if (mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_SESSIONSTATEFUL)) {
            this.sessionStateful = mount.getValueProvider().getBoolean(HstNodeTypes.MOUNT_PROPERTY_SESSIONSTATEFUL);
        } else if (parent != null){
            this.sessionStateful = parent.isSessionStateful();
        }
        
        if (mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_FORMLOGINPAGE)) {
            this.formLoginPage = StringPool.get(mount.getValueProvider().getString(HstNodeTypes.MOUNT_PROPERTY_FORMLOGINPAGE));
        } else if (parent != null){
            this.formLoginPage = parent.getFormLoginPage();
        }

        if(mount.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_CACHEABLE)) {
            this.cacheable = mount.getValueProvider().getBoolean(HstNodeTypes.GENERAL_PROPERTY_CACHEABLE);
        } else if(parent != null) {
            this.cacheable = parent.isCacheable();
        } else {
            this.cacheable =  virtualHost.isCacheable();
        }

        if(mount.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_DEFAULT_RESOURCE_BUNDLE_ID)) {
            this.defaultResourceBundleId = mount.getValueProvider().getString(HstNodeTypes.GENERAL_PROPERTY_DEFAULT_RESOURCE_BUNDLE_ID);
        } else if(parent != null) {
            this.defaultResourceBundleId = parent.getDefaultResourceBundleId();
        } else {
            this.defaultResourceBundleId =  virtualHost.getDefaultResourceBundleId();
        }

        this.cmsLocation = ((VirtualHostService)virtualHost).getCmsLocation();

        defaultSiteMapItemHandlerIds = mount.getValueProvider().getStrings(HstNodeTypes.MOUNT_PROPERTY_DEFAULTSITEMAPITEMHANDLERIDS);
        if(defaultSiteMapItemHandlerIds == null && parent != null) {
            defaultSiteMapItemHandlerIds = parent.getDefaultSiteMapItemHandlerIds();
        }

        if (mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_CHANNELPATH)) {
            channelPath = mount.getValueProvider().getString(HstNodeTypes.MOUNT_PROPERTY_CHANNELPATH);
        }

        // We do recreate the HstSite object, even when inherited from parent, such that we do not share the same HstSite object. This might be
        // needed in the future though, for example for performance reasons
        if(mountPoint == null ){
            log.info("Mount '{}' at '{}' does have an empty mountPoint. This means the Mount is not using a HstSite and does not have a content path", getName(), mount.getValueProvider().getPath());
        } else if(!mountPoint.startsWith("/")) {
            throw new ServiceException("Mount at '"+mount.getValueProvider().getPath()+"' has an invalid mountPoint '"+mountPoint+"'. A mount point is absolute and must start with a '/'");
        } else if(!isMapped()){
            log.info("Mount '{}' at '{}' does contain a mountpoint, but is configured to not use a HstSiteMap because isMapped() is false", getName(), mount.getValueProvider().getPath());
            
            // check if the mountpoint points to a hst:site node:
            HstSiteRootNode hstSiteNodeForMount = hstManager.getHstSiteRootNodes().get(mountPoint);
            if(hstSiteNodeForMount == null) {
                // for non Mounts, the contentPath is just the mountpoint when the mountpoint does not point to a hst:site
                this.contentPath = mountPoint;
                // when not mapped we normally do not need the mount for linkrewriting. Hence we just take it to be the same as the contentPath.
                this.canonicalContentPath = mountPoint;
            } else {
                // the mountpoint does point to a hst:site. Since we do not need the HstSiteMap because isMapped = false, we only use the content mapping
                canonicalContentPath = hstSiteNodeForMount.getContentPath();
                //contentPath = hstSiteNodeForMount.getContentPath();
                contentPath = hstSiteNodeForMount.getContentPath();
            }
        } else {
             
            HstSiteRootNode hstSiteNodeForMount = hstManager.getHstSiteRootNodes().get(mountPoint);
            if(hstSiteNodeForMount == null) {
                throw new ServiceException("mountPoint '" + mountPoint
                        + "' does not point to a hst:site node for Mount '" + mount.getValueProvider().getPath()
                        + "'. Cannot create HstSite for Mount. Either fix the mountpoint or add 'hst:ismapped=false' if this mount is not meant to have a mount point");
            }
            
            hstSite = new HstSiteService(hstSiteNodeForMount, this, hstManager);
            canonicalContentPath = hstSiteNodeForMount.getContentPath();
            contentPath = hstSiteNodeForMount.getContentPath();

            log.info("Succesfull initialized hstSite '{}' for Mount '{}'", hstSite.getName(), getName());
            
            // now also try to get hold of the previewHstSite. If we cannot load it, we log an info: 
            HstSiteRootNode previewHstSiteNodeForMount = hstManager.getHstSiteRootNodes().get(previewMountPoint);
            if(previewHstSiteNodeForMount == null || isPreview()) {
                if (!isPreview()) {
                    log.warn("There is no preview version '{}-preview' for mount '{}'. Cannot create automatic PREVIEW HstSite " +
                		"for this Mount. The mount '{}' will be used as preview.", new String[] {mountPoint,  mount.getValueProvider().getPath(), mountPoint});
                    }
                previewHstSite = hstSite;
                previewCanonicalContentPath = canonicalContentPath;
                previewContentPath = contentPath;
            } else {
                previewHstSite = new HstSiteService(previewHstSiteNodeForMount, this, hstManager);
                previewCanonicalContentPath = previewHstSiteNodeForMount.getContentPath();
                //previewContentPath = previewHstSiteNodeForMount.getContentPath();
                previewContentPath = previewHstSiteNodeForMount.getContentPath();
            }
        }

        for (HstNode childMount : mount.getNodes()) {
            if (HstNodeTypes.NODETYPE_HST_MOUNT.equals(childMount.getNodeTypeName())) {
                try {
                    MountService childMountService = new MountService(childMount, this, virtualHost, hstManager, port);
                    MutableMount prevValue = this.childMountServices.put(childMountService.getName(), childMountService);
                    if (prevValue != null) {
                        log.warn("Duplicate child mount with same name below '{}'. The first one is overwritten and ignored.", mount.getValueProvider().getPath());
                    }
                } catch (ServiceException e) {
                    String path = childMount.getValueProvider().getPath();
                    if (log.isDebugEnabled()) {
                        log.warn("Skipping incorrect mount for mount node '" + path + "'. ", e);
                    } else {
                        log.warn("Skipping incorrect mount for mount node '{}' because of '{}'. ", path, e.toString());
                    }
                }

            }
        }

        // add this Mount to the maps in the VirtualHostsService
        ((VirtualHostsService)virtualHost.getVirtualHosts()).addMount(this);
    }


    @Override
    public void addMount(MutableMount mount) throws IllegalArgumentException, ServiceException {
        if(childMountServices.containsKey(mount.getName())) {
            throw new IllegalArgumentException("Cannot add Mount with name '"+mount.getName()+"' because already exists for " + this.toString());
        }
        childMountServices.put(mount.getName(), mount);
        ((MutableVirtualHosts)virtualHost.getVirtualHosts()).addMount(mount);
    }
    
    @Override
    public List<Mount> getChildMounts() {
        return Collections.unmodifiableList(new ArrayList<Mount>(childMountServices.values()));
    }
    
    public Mount getChildMount(String name) {
        return childMountServices.get(name);
    }

    public HstSite getHstSite() {
        return hstSite;
    }

    // not an API method. Only internal for the core
    public HstSite getPreviewHstSite() {
        return previewHstSite;
    }

    
    public String getName() {
        return name;
    }
    
    public String getIdentifier() {
        return uuid;
    }

    public String getAlias() {
        return alias;
    }
    
    public String getMountPath() {
        return mountPath;
    }
    
    public String getContentPath() {
        return contentPath;
    }

    public String getCanonicalContentPath() {
        return canonicalContentPath;
    }
    
    /*
     * internal only : not api 
     */
    public String getPreviewContentPath() {
        return previewContentPath;
    }
    /*
     * internal only: not api 
     */
    public String getPreviewCanonicalContentPath() {
        return previewCanonicalContentPath;
    }

    public String getMountPoint() {
        return mountPoint;
    }
    
    /*
     * internal only: not api 
     */
    public String getPreviewMountPoint() {
        return previewMountPoint;
    }
    
    public boolean isMapped() {
        return isMapped;
    }

    
    public Mount getParent() {
        return parent;
    }

    public String getScheme() {
        return scheme;
    }

    public int getSchemeNotMatchingResponseCode() {
        return schemeNotMatchingResponseCode;
    }

    public String getLocale() {
        return locale;
    }

    public String getHomePage() {
        return homepage;
    }
    
    public String getPageNotFound() {
        return pageNotFound;
    }

    public VirtualHost getVirtualHost() {
        return virtualHost;
    }

    public boolean isContextPathInUrl() {
        return contextPathInUrl;
    }

    public int getPort() {
        return port;
    }

    public boolean isPortInUrl() {
        return showPort;
    }
    
    public boolean isSite() {
        return isSite;
    } 
    
    public String onlyForContextPath() {
        return onlyForContextPath;
    }

    public boolean isPreview() {
        return isOfType(Mount.PREVIEW_NAME);
    }

    public String getType() {
        return type;
    }
    
    public List<String> getTypes(){
        List<String> combined = new ArrayList<String>();
        // add the primary type  first
        combined.add(getType());
        
        if(types != null) {
            if(types.contains(getType())) {
                for(String extraType : types) {
                    if(extraType != null) {
                       if(extraType.equals(getType())) {
                           // already got it
                           continue;
                       } 
                       combined.add(extraType);
                    }
                }
            } else {
                combined.addAll(types);
            }
        }
        return Collections.unmodifiableList(combined);
    }
    
    public boolean isOfType(String type) {
        return getTypes().contains(type);
    }

    
    public boolean isVersionInPreviewHeader() {
        return versionInPreviewHeader;
    }
    
   public String getCmsLocation() {
        return cmsLocation;
    }

    @Override
    public String getLockedBy() {
        return lockedBy;
    }

    @Override
    public void setLockedBy(final String userId) {
        lockedBy = userId;
    }

    @Override
    public Calendar getLockedOn() {
        return lockedOn;
    }

    @Override
    public void setLockedOn(final Calendar lockedOn) {
        this.lockedOn = lockedOn;
    }

    public String getNamedPipeline(){
        return namedPipeline;
    }

    public HstSiteMapMatcher getHstSiteMapMatcher() {
        return getVirtualHost().getVirtualHosts().getHstManager().getSiteMapMatcher();
    }

    public String getEmbeddedMountPath() {
        return embeddedMountPath;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }
    
    public Set<String> getRoles() {
        return Collections.unmodifiableSet(this.roles);
    }
    
    public Set<String> getUsers() {
        return Collections.unmodifiableSet(this.users);
    }
    
    public boolean isSubjectBasedSession() {
        return subjectBasedSession;
    }
    
    public boolean isSessionStateful() {
        return sessionStateful;
    }
    
    public String getFormLoginPage() {
        return formLoginPage;
    }
    
    public String getProperty(String name) {
        Object o = allProperties.get(name);
        if(o != null) {
            return o.toString();
        }
        return null;
    }


    @Override
    public String[] getDefaultSiteMapItemHandlerIds() {
        return defaultSiteMapItemHandlerIds;
    }

    @Override
    public boolean isCacheable() {
        return cacheable;
    }

    @Override
    public String getDefaultResourceBundleId() {
        return defaultResourceBundleId;
    }

    public Map<String, String> getMountProperties() {
        Map<String, String> mountProperties = new HashMap<String, String>();
        for(Entry<String, Object> entry : allProperties.entrySet()) {
            if(entry.getValue() instanceof String) {
                if(entry.getKey().startsWith(PROPERTY_NAME_MOUNT_PREFIX)) {
                    if(entry.getKey().equals(HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT)) {
                        // skip the hst:mountpoint property as this is a reserved property with a different meaning
                        continue;
                    }
                    mountProperties.put(entry.getKey().substring(PROPERTY_NAME_MOUNT_PREFIX.length()).toLowerCase(), ((String)entry.getValue()).toLowerCase());
                }
            }
        }
        return mountProperties;
    }

    @Override
    public String getChannelPath() {
        return channelPath;
    }

    @Override
    public Channel getChannel() {
        return channel;
    }

    @SuppressWarnings("unchecked")
    public <T extends ChannelInfo> T getChannelInfo() {
        return (T) channelInfo;
    }

    public void setChannelInfo(final ChannelInfo channelInfo) {
        this.channelInfo = channelInfo;
    }

    @Override
    public void setChannel(final Channel channel) throws UnsupportedOperationException {
        if (channel == null) {
            throw new IllegalArgumentException("Channel to set is not allowed to be null");
        }
        this.channel = channel;
    }


    @Override
    public String toString() {
        
        StringBuilder builder = new StringBuilder("MountService [name=");
        builder.append(name).append(", uuid=").append(uuid).append(", hostName=").append(virtualHost.getHostName())
        .append(", channelPath=").append(channelPath).append(", alias=").append(alias).append(", type=").append(type)
        .append(", types=").append(types).append(", versionInPreviewHeader=").append(versionInPreviewHeader)
        .append(", namedPipeline=").append(namedPipeline).append(", mountPath=").append(mountPath)
        .append(", contentPath=").append(contentPath).append(", mountPoint=").append(mountPoint)
        .append( ", isMapped=").append(isMapped).append(", homepage=").append(homepage).append(", pageNotFound=").append(pageNotFound)
        .append(", contextPathInUrl=").append(contextPathInUrl).append(", isSite=").append(isSite).append(", showPort=")
        .append(showPort).append(", port=").append(port).append(", onlyForContextPath=").append(onlyForContextPath)
        .append(", scheme=").append(scheme).append(", locale=").append(locale).append(", authenticated=").append(authenticated)
        .append(", roles=").append(roles).append(", users=").append(users).append(", subjectBasedSession=")
        .append(subjectBasedSession).append(", sessionStateful=").append(sessionStateful).append(", formLoginPage=" ).append(formLoginPage)
        .append(", cmsLocation=" ).append(cmsLocation)
        .append("]");
        return  builder.toString();
    }

}
