package org.hippoecm.hst.configuration.hosting;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.site.HstSiteService;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.service.AbstractJCRService;
import org.hippoecm.hst.service.Service;
import org.hippoecm.hst.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SiteMountService extends AbstractJCRService implements SiteMount, Service {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(SiteMountService.class);
    
    
    /**
     * The name of this sitemount. If it is the root, it is called hst:root
     */
    private String name;
    
    /**
     * The virtual host of where this SiteMount belongs to
     */
    private VirtualHost virtualHost;

    /**
     * The parent of this sitemount or null when this sitemount is the root
     */
    private SiteMount parent;
    
    /**
     * the HstSite this SiteMount points to
     */
    private HstSite hstSite;
    
    /**
     * The child site mounts below this sitemount
     */
    private Map<String, SiteMountService> childSiteMountServices = new HashMap<String, SiteMountService>();


    /**
     * Whether this sitemount points to a preview. Default is false
     */
    private boolean preview;
    

    /**
     * When the SiteMount is preview, and this isVersionInPreviewHeader is true, the used HST version is set as a response header. 
     * Default this variable is true when it is not configured explicitly
     */
    private boolean versionInPreviewHeader;
    
    /**
     * If this site mount must use some custom other than the default pipeline, the name of the pipeline is contained by <code>namedPipeline</code>
     */
    private String namedPipeline;
    

    /**
     * The mountpath of this sitemount. Note that it can contain wildcards
     */
    private String mountPath;
    
    /**
     * The jcr path where the mount is pointing to
     */
    private String mountPoint;
    
    /**
     * <code>true</code> (default) when this SiteMount is used as a site mount. False when used only as content mount point and possibly a namedPipeline
     */
    private boolean isSiteMount = true;
    
    /**
     * The homepage for this SiteMount. When the backing configuration does not contain a homepage, then, the homepage from the backing {@link VirtualHost} is 
     * taken (which still might be <code>null</code> though)
     */
    private String homepage;
    

    /**
     * The pagenotfound for this SiteMount. When the backing configuration does not contain a pagenotfound, then, the pagenotfound from the backing {@link VirtualHost} is 
     * taken (which still might be <code>null</code> though)
     */
    private String pageNotFound;
    
    private boolean contextPathInUrl;
    
    /**
     *  when this sitemount is only applicable for certain contextpath, this property for the contextpath tells which value it must have. It must start with a slash.
     */
    private String onlyForContextPath;
    
    private String scheme;

    /**
     * for embedded delegation of sites a sitemountpath needs to point to the delegated sitemount. This is only relevant for portal environment
     */
    private String embeddedSiteMountPath;
    
    public SiteMountService(Node siteMount, SiteMount parent, VirtualHost virtualHost) throws ServiceException {
        super(siteMount);
        this.virtualHost = virtualHost;
        this.parent = parent;
        
        this.name = getValueProvider().getName();

        
        if(parent == null) {
            mountPath = "";
        } else {
            mountPath = parent.getMountPath() + "/" + name;
        }
       
        // is the context path visible in the url
        if(this.getValueProvider().hasProperty(HstNodeTypes.SITEMOUNT_PROPERTY_SHOWCONTEXTPATH)) {
            this.contextPathInUrl = this.getValueProvider().getBoolean(HstNodeTypes.SITEMOUNT_PROPERTY_SHOWCONTEXTPATH);
        } else {
            if(parent != null) {
                this.contextPathInUrl = parent.isContextPathInUrl();
            } else {
                this.contextPathInUrl = virtualHost.isContextPathInUrl();
            }
        }
        
        if(this.getValueProvider().hasProperty(HstNodeTypes.SITEMOUNT_PROPERTY_ONLYFORCONTEXTPATH)) {
            this.onlyForContextPath = this.getValueProvider().getString(HstNodeTypes.SITEMOUNT_PROPERTY_ONLYFORCONTEXTPATH);
        } else {
            if(parent != null) {
                this.onlyForContextPath = parent.onlyForContextPath();
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
        
        if(this.getValueProvider().hasProperty(HstNodeTypes.SITEMOUNT_PROPERTY_SCHEME)) {
            this.scheme = this.getValueProvider().getString(HstNodeTypes.SITEMOUNT_PROPERTY_SCHEME);
            if(this.scheme == null || "".equals(this.scheme)) {
                this.scheme = VirtualHostsService.DEFAULT_SCHEME;
            }
        } else {
           // try to get the one from the parent
            if(parent != null) {
                this.scheme = parent.getScheme();
            } else {
                this.scheme = virtualHost.getScheme();
            }
        }
        
        if(this.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_HOMEPAGE)) {
            this.homepage = this.getValueProvider().getString(HstNodeTypes.GENERAL_PROPERTY_HOMEPAGE);
        } else {
           // try to get the one from the parent
            if(parent != null) {
                this.homepage = parent.getHomePage();
            } else {
                this.homepage = virtualHost.getHomePage();
            }
        }
        
        if(this.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_PAGE_NOT_FOUND)) {
            this.pageNotFound = this.getValueProvider().getString(HstNodeTypes.GENERAL_PROPERTY_PAGE_NOT_FOUND);
        } else {
           // try to get the one from the parent
            if(parent != null) {
                this.pageNotFound = parent.getPageNotFound();
            } else {
                this.pageNotFound = ((VirtualHostService)virtualHost).getPageNotFound();
            }
        }
        
        
        if(this.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_VERSION_IN_PREVIEW_HEADER)) {
            this.versionInPreviewHeader = this.getValueProvider().getBoolean(HstNodeTypes.GENERAL_PROPERTY_VERSION_IN_PREVIEW_HEADER);
        } else {
           // try to get the one from the parent
            if(parent != null) {
                this.versionInPreviewHeader = parent.isVersionInPreviewHeader();
            } else {
                this.versionInPreviewHeader = ((VirtualHostService)virtualHost).isVersionInPreviewHeader();
            }
        }
        
        
         
        
        if(this.getValueProvider().hasProperty(HstNodeTypes.SITEMOUNT_PROPERTY_ISPREVIEW)) {
            this.preview = this.getValueProvider().getBoolean(HstNodeTypes.SITEMOUNT_PROPERTY_ISPREVIEW);
        } else if(parent != null) {
            this.preview = parent.isPreview();
        }
        
        if(this.getValueProvider().hasProperty(HstNodeTypes.SITEMOUNT_PROPERTY_ISSITEMOUNT)) {
            this.isSiteMount = this.getValueProvider().getBoolean(HstNodeTypes.SITEMOUNT_PROPERTY_ISSITEMOUNT);
        } else if(parent != null) {
            this.isSiteMount = parent.isSiteMount();
        }

        if(this.getValueProvider().hasProperty(HstNodeTypes.SITEMOUNT_PROPERTY_NAMEDPIPELINE)) {
            this.namedPipeline = this.getValueProvider().getString(HstNodeTypes.SITEMOUNT_PROPERTY_NAMEDPIPELINE);
        } else if(parent != null) {
            this.namedPipeline = parent.getNamedPipeline();
        }
        

        if(this.getValueProvider().hasProperty(HstNodeTypes.SITEMOUNT_PROPERTY_EMBEDDEDSITEMOUNTPATH)) {
            this.embeddedSiteMountPath = this.getValueProvider().getString(HstNodeTypes.SITEMOUNT_PROPERTY_EMBEDDEDSITEMOUNTPATH);
        } else if(parent != null) {
            this.embeddedSiteMountPath = parent.getEmbeddedSiteMountPath();
        }
        
        if(this.getValueProvider().hasProperty(HstNodeTypes.SITEMOUNT_PROPERTY_MOUNTPATH)) {
            this.mountPoint = this.getValueProvider().getString(HstNodeTypes.SITEMOUNT_PROPERTY_MOUNTPATH);
            // now, we need to create the HstSite object
            if(mountPoint == null || "".equals(mountPoint)){
                mountPoint = null;
            }
        } else if(parent != null) {
            this.mountPoint = ((SiteMountService)parent).mountPoint;
            if(mountPoint != null) {
                log.info("mountPoint for SiteMount '{}' is inherited from its parent SiteMount and is '{}'", getName() , mountPoint);
            }
        }
        
        // We do recreate the HstSite object, even when inherited from parent, such that we do not share the same HstSite object. This might be
        // needed in the future though, for example for performance reasons
        if(mountPoint == null ){
            log.info("SiteMount '{}' at '{}' does have an empty mountPoint. This means the SiteMount is not using a HstSite and does not have a content path", getName(), getValueProvider().getPath());
        } else if(!mountPoint.startsWith("/")) {
            throw new ServiceException("SiteMount at '"+getValueProvider().getPath()+"' has an invalid mountPoint '"+mountPoint+"'. A mount point is absolute and must start with a '/'");
        } else if(!isSiteMount()){
            log.info("SiteMount '{}' at '{}' does contain a mountpoint, but is configured not to be a mount to a hstsite", getName(), getValueProvider().getPath());
        } else {
            try {
                if (siteMount.getSession().itemExists(mountPoint) && siteMount.getSession().getItem(mountPoint).isNode() && ((Node) siteMount.getSession().getItem(mountPoint)).isNodeType(HstNodeTypes.NODETYPE_HST_SITE)) {
                    Node hstSiteNode = (Node) siteMount.getSession().getItem(mountPoint);
                    this.hstSite = new HstSiteService(hstSiteNode, this);
                    log.info("Succesfull initialized hstSite '{}' for site mount '{}'", hstSite.getName(), getName());
                } else {
                    throw new ServiceException("mountPoint '" + mountPoint
                            + "' does not point to a hst:site node for SiteMount '" + getValueProvider().getPath()
                            + "'. Cannot create HstSite for SiteMount");
                }
                
            } catch (RepositoryException e) {
                throw new ServiceException("Error during creating HstSite. Cannot add SiteMount for '"+getValueProvider().getPath()+"'", e);
            }
        }
        
        // check whether there are child SiteMounts now for this SiteMount
        try {
            NodeIterator childMounts = siteMount.getNodes();
            while (childMounts.hasNext()) {
                Node childMountNode = childMounts.nextNode();
                if (childMountNode == null) {
                    continue;
                }
                SiteMountService childMount = new SiteMountService(childMountNode, this, virtualHost);
                SiteMountService prevValue = this.childSiteMountServices.put(childMount.getName(), childMount);
                if(prevValue != null) {
                    log.warn("Duplicate child mount with same name below '{}'. The first one is overwritten and ignored.", siteMount.getPath());
                }
            }
        } catch (RepositoryException e) {
            throw new ServiceException("Error during initializing site mounts", e);
        }
    }
    
    public SiteMount getChildMount(String name) {
        return childSiteMountServices.get(name);
    }

    public HstSite getHstSite() {
        return hstSite;
    }

    public String getName() {
        return name;
    }

    public String getMountPath() {
        return mountPath;
    }
    
    public String getMountPoint() {
        return mountPoint;
    }

    public boolean isSiteMount() {
        return isSiteMount;
    }

    
    public SiteMount getParent() {
        return parent;
    }

  
    public String getScheme() {
        return scheme;
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
    
    public String onlyForContextPath() {
        return onlyForContextPath;
    }

    public boolean isPreview() {
        return preview;
    }

    public boolean isVersionInPreviewHeader() {
        return versionInPreviewHeader;
    }

    public Service[] getChildServices() {
        // the services are the child mounts AND the hstSite if this one is not null
        Service[] childServices = childSiteMountServices.values().toArray(new Service[childSiteMountServices.values().size()]);
        if(this.hstSite != null) {
            Service[] servicesPlusHstSite = new Service[childServices.length + 1];
            System.arraycopy(childServices, 0, servicesPlusHstSite, 0, childServices.length);
            // and add to the end the hstSite
            servicesPlusHstSite[childServices.length] = (Service)hstSite;
            return servicesPlusHstSite;
        } 
        return childServices;
    }
    
    public String getNamedPipeline(){
        return namedPipeline;
    }

    public HstSiteMapMatcher getHstSiteMapMatcher() {
        return getVirtualHost().getVirtualHosts().getVirtualHostsManager().getSiteMapMatcher();
    }

    public String getEmbeddedSiteMountPath() {
        return embeddedSiteMountPath;
    }



}
