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
package org.hippoecm.hst.configuration.sitemap;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.sitemapitemhandlers.HstSiteMapItemHandlerConfiguration;
import org.hippoecm.hst.configuration.sitemapitemhandlers.HstSiteMapItemHandlersConfiguration;
import org.hippoecm.hst.service.AbstractJCRService;
import org.hippoecm.hst.service.Service;
import org.hippoecm.hst.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstSiteMapItemService extends AbstractJCRService implements HstSiteMapItem, Service{

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(HstSiteMapItemService.class);

    private Map<String, HstSiteMapItem> childSiteMapItems = new HashMap<String, HstSiteMapItem>();
    
    private Map<String, HstSiteMapItemHandlerConfiguration> siteMapItemHandlerConfigurations = new HashMap<String, HstSiteMapItemHandlerConfiguration>();

    private String siteMapRootNodePath;
    
    private String id;
    
    private String qualifiedId;
    
    private String value;

    private int statusCode; 
    
    private int errorCode; 
        
    private String parameterizedPath;
    
    private int occurences;
    
    private String relativeContentPath;
    
    private String componentConfigurationId;

    private String portletComponentConfigurationId;
  
    private List<String> roles;
    
    private boolean secured = false;
    
    private boolean isExcludedForLinkRewriting = false;

    private boolean isWildCard;
    
    private boolean isAny;
    
    private String namedPipeline;
    
    /*
     * Internal only: used for linkrewriting: when true, it indicates, that this HstSiteMapItem can only be used in linkrewriting
     * when the current context helps to resolve some wildcards
     */
    private boolean useableInRightContextOnly = false;
    /*
     * Internal only: needed for context aware linkrewriting
     */
    private Map<String, String> keyToPropertyPlaceHolderMap = new HashMap<String,String>();
    
    private int depth;
    
    private HstSiteMap hstSiteMap;
    
    private HstSiteMapItemService parentItem;
    
    private HstSiteMapItemHandlersConfiguration siteMapItemHandlersConfiguration;

    private Map<String,String> parameters = new HashMap<String,String>();
    private Map<String,String> localParameters = new HashMap<String,String>();
    
    private List<HstSiteMapItemService> containsWildCardChildSiteMapItems = new ArrayList<HstSiteMapItemService>();
    private List<HstSiteMapItemService> containsAnyChildSiteMapItems = new ArrayList<HstSiteMapItemService>();
    private boolean containsAny;
    private boolean containsWildCard;
    private String postfix; 
    private String extension;
    private String prefix; 
    
    public HstSiteMapItemService(Node jcrNode, String siteMapRootNodePath, HstSiteMapItemHandlersConfiguration siteMapItemHandlersConfiguration, HstSiteMapItem parentItem, HstSiteMap hstSiteMap, int depth) throws ServiceException{
        super(jcrNode);
        this.parentItem = (HstSiteMapItemService)parentItem;
        this.hstSiteMap = hstSiteMap; 
        this.siteMapItemHandlersConfiguration = siteMapItemHandlersConfiguration;
        this.depth = depth;
        String nodePath = getValueProvider().getPath();
        if(!getValueProvider().getPath().startsWith(siteMapRootNodePath)) {
            this.closeValueProvider(false);
            throw new ServiceException("Node path of the sitemap cannot start without the global sitemap root path. Skip SiteMapItem");
        }

        this.siteMapRootNodePath = siteMapRootNodePath;
        
        this.qualifiedId = nodePath;
        
        // path & id are the same
        this.id = nodePath.substring(siteMapRootNodePath.length()+1);
        // currently, the value is always the nodename
        this.value = getValueProvider().getName();

        this.statusCode = getValueProvider().getLong(HstNodeTypes.SITEMAPITEM_PROPERTY_STATUSCODE).intValue();
        this.errorCode = getValueProvider().getLong(HstNodeTypes.SITEMAPITEM_PROPERTY_ERRORCODE).intValue();
       
        if(this.value == null){
            log.error("The 'value' of a SiteMapItem is not allowed to be null: '{}'", nodePath);
            this.closeValueProvider(false);
            throw new ServiceException("The 'value' of a SiteMapItem is not allowed to be null. It is so for '"+nodePath+"'");
        }
        if(parentItem != null) {
            this.parameterizedPath = this.parentItem.getParameterizedPath()+"/";
            this.occurences = this.parentItem.getWildCardAnyOccurences();
        } else {
            parameterizedPath = "";
        }
        if(HstNodeTypes.WILDCARD.equals(value)) {
            occurences++; 
            parameterizedPath = parameterizedPath + "${" + occurences + "}";
            this.isWildCard = true;
        } else if(HstNodeTypes.ANY.equals(value)) {
            occurences++;
            parameterizedPath = parameterizedPath + "${" + occurences + "}";
            this.isAny = true;
        } else if(value.indexOf(HstNodeTypes.WILDCARD) > -1) {
            this.containsWildCard = true;
            this.postfix = value.substring(value.indexOf(HstNodeTypes.WILDCARD) + HstNodeTypes.WILDCARD.length());
            this.prefix = value.substring(0, value.indexOf(HstNodeTypes.WILDCARD));
            if(this.postfix.indexOf(".") > -1) {
                this.extension = this.postfix.substring(this.postfix.indexOf("."));
            }
            if(parentItem != null) {
                ((HstSiteMapItemService)parentItem).addWildCardPrefixedChildSiteMapItems(this);
            }
            occurences++;
            parameterizedPath = parameterizedPath + value.replace(HstNodeTypes.WILDCARD, "${"+occurences+"}" );
        } else if(value.indexOf(HstNodeTypes.ANY) > -1) {
            this.containsAny = true;
            this.postfix = value.substring(value.indexOf(HstNodeTypes.ANY) + HstNodeTypes.ANY.length());
            if(this.postfix.indexOf(".") > -1) {
                this.extension = this.postfix.substring(this.postfix.indexOf("."));
            }
            this.prefix = value.substring(0, value.indexOf(HstNodeTypes.ANY));
            if(parentItem != null) {
                ((HstSiteMapItemService)parentItem).addAnyPrefixedChildSiteMapItems(this);
            }
            occurences++;
            parameterizedPath = parameterizedPath + value.replace(HstNodeTypes.ANY, "${"+occurences+"}" );
        }
        else {
            parameterizedPath = parameterizedPath + value;
        }
        
        String[] parameterNames = getValueProvider().getStrings(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES);
        String[] parameterValues = getValueProvider().getStrings(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES);
        
        if(parameterNames != null && parameterValues != null){
           if(parameterNames.length != parameterValues.length) {
               log.warn("Skipping parameters for component because they only make sense if there are equal number of names and values");
           }  else {
               for(int i = 0; i < parameterNames.length ; i++) {
                   this.parameters.put(parameterNames[i], parameterValues[i]);
                   this.localParameters.put(parameterNames[i], parameterValues[i]);
               }
           }
        }
        
        if(this.parentItem != null){
            // add the parent parameters that are not already present
            for(Entry<String, String> parentParam : this.parentItem.getParameters().entrySet()) {
                if(!this.parameters.containsKey(parentParam.getKey())) {
                    this.parameters.put(parentParam.getKey(), parentParam.getValue());
                }
            }
        }
        
        this.relativeContentPath = getValueProvider().getString(HstNodeTypes.SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH);
        this.componentConfigurationId = getValueProvider().getString(HstNodeTypes.SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID);
        
        String[] siteMapItemHandlerIds = getValueProvider().getStrings(HstNodeTypes.SITEMAPITEM_PROPERTY_SITEMAPITEMHANDLERIDS);
        if(siteMapItemHandlerIds != null) {
            for(String handlerId : siteMapItemHandlerIds) {
                HstSiteMapItemHandlerConfiguration handlerConfiguration = this.siteMapItemHandlersConfiguration.getSiteMapItemHandlerConfiguration(handlerId);
                if(handlerConfiguration == null) {
                    log.error("Incorrect configuration: SiteMapItem '{}' contains a handlerId '{}' which cannot be found in the siteMapItemHandlers configuration. The handler will be ignored", getQualifiedId(), handlerId);
                } else {
                    this.siteMapItemHandlerConfigurations.put(handlerId, handlerConfiguration);
                }
            }
        }
        
        this.portletComponentConfigurationId = getValueProvider().getString(HstNodeTypes.SITEMAPITEM_PROPERTY_PORTLETCOMPONENTCONFIGURATIONID);
        
        if(getValueProvider().hasProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_ROLES)) {
            String[] rolesProp = getValueProvider().getStrings(HstNodeTypes.SITEMAPITEM_PROPERTY_ROLES);
            this.roles = Arrays.asList(rolesProp);
        } else if(this.parentItem != null){
            this.roles = parentItem.getRoles();
        } else {
            this.roles = new ArrayList<String>();
        }
        
        if(getValueProvider().hasProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_SECURED)) {
            this.secured = getValueProvider().getBoolean(HstNodeTypes.SITEMAPITEM_PROPERTY_SECURED);
        } else if(this.parentItem != null){
            this.secured = parentItem.isSecured();
        } 
        
        if(getValueProvider().hasProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_EXCLUDEDFORLINKREWRITING)) {
            this.isExcludedForLinkRewriting = getValueProvider().getBoolean(HstNodeTypes.SITEMAPITEM_PROPERTY_EXCLUDEDFORLINKREWRITING);
        }
        
        if(getValueProvider().hasProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_NAMEDPIPELINE)) {
            this.namedPipeline = getValueProvider().getString(HstNodeTypes.SITEMAPITEM_PROPERTY_NAMEDPIPELINE);
        } else if(this.parentItem != null) {
            this.namedPipeline = parentItem.getNamedPipeline();
        } else {
            // inherit the namedPipeline from the sitemount (can be null)
            this.namedPipeline = this.getHstSiteMap().getSite().getSiteMount().getNamedPipeline();
        }
        
        init(jcrNode);
    }

    private void init(Node node) {
        try{
            for(NodeIterator nodeIt = node.getNodes(); nodeIt.hasNext();) {
                Node child = nodeIt.nextNode();
                if(child == null) {
                    log.warn("skipping null node");
                    continue;
                }
                if(child.isNodeType(HstNodeTypes.NODETYPE_HST_SITEMAPITEM)) {
                    try {
                        HstSiteMapItemService siteMapItemService = new HstSiteMapItemService(child, siteMapRootNodePath, siteMapItemHandlersConfiguration , this, this.hstSiteMap, ++depth);
                        childSiteMapItems.put(siteMapItemService.getValue(), siteMapItemService);
                    } catch (ServiceException e) {
                        if (log.isDebugEnabled()) {
                            log.warn("Skipping root sitemap '{}'", child.getPath(), e);
                        } else if (log.isWarnEnabled()) {
                            log.warn("Skipping root sitemap '{}'", child.getPath());
                        }
                    }
                } else {
                    if (log.isWarnEnabled()) {
                        log.warn("Skipping node '{}' because is not of type '{}'", child.getPath(), HstNodeTypes.NODETYPE_HST_SITEMAPITEM);
                    }
                }
            } 
        } catch (RepositoryException e) {
            log.warn("Skipping SiteMap structure due to Repository Exception ", e);
        }
    }
    
    public Service[] getChildServices() {
        return childSiteMapItems.values().toArray(new Service[childSiteMapItems.size()]);
    }

    public HstSiteMapItem getChild(String value) {
        return this.childSiteMapItems.get(value);
    }

    
    
    public List<HstSiteMapItem> getChildren() {
        return Collections.unmodifiableList(new ArrayList<HstSiteMapItem>(this.childSiteMapItems.values()));
    }

    public String getComponentConfigurationId() {
        return this.componentConfigurationId;
    }

    public String getPortletComponentConfigurationId() {
        return this.portletComponentConfigurationId;
    }
    
    public String getId() {
        return this.id;
    }

    public String getRelativeContentPath() {
        return this.relativeContentPath;
    }
    

    public String getParameter(String name) {
        return this.parameters.get(name);
    }
    

    public Map<String, String> getParameters() {
        return Collections.unmodifiableMap(this.parameters);
    }
    

	public String getLocalParameter(String name) {
		return this.localParameters.get(name);
	}

	public Map<String, String> getLocalParameters() {
		return Collections.unmodifiableMap(this.localParameters);
	}


	public HstSiteMapItemHandlerConfiguration getSiteMapItemHandlerConfiguration(String handlerId) {
	    return siteMapItemHandlerConfigurations.get(handlerId);
	}
	
    public List<HstSiteMapItemHandlerConfiguration> getSiteMapItemHandlerConfigurations() {
        return Collections.unmodifiableList(new ArrayList<HstSiteMapItemHandlerConfiguration>(siteMapItemHandlerConfigurations.values()));
    }

    public int getStatusCode() {
        return this.statusCode;
    }
    
    public int getErrorCode() {
        return this.errorCode;
    }

    public List<String> getRoles() {
        return Collections.unmodifiableList(this.roles);
    }

    public boolean isSecured() {
        return this.secured;
    }

    public String getValue() {
        return this.value;
    }
    
    public boolean isWildCard() {
        return this.isWildCard;
    }
    
    public boolean isAny() {
        return this.isAny;
    }
  
    public HstSiteMap getHstSiteMap() {
        return this.hstSiteMap;
    }

    public HstSiteMapItem getParentItem() {
        return this.parentItem;
    }
    
    public String getParameterizedPath(){
        return this.parameterizedPath;
    }
    
    public int getWildCardAnyOccurences(){
        return this.occurences;
    }

   
    // ---- BELOW FOR INTERNAL CORE SITEMAP MAP RESOLVING && LINKREWRITING ONLY  
    
    public void addWildCardPrefixedChildSiteMapItems(HstSiteMapItemService hstSiteMapItem){
        this.containsWildCardChildSiteMapItems.add(hstSiteMapItem);
    }
    
    public void addAnyPrefixedChildSiteMapItems(HstSiteMapItemService hstSiteMapItem){
        this.containsAnyChildSiteMapItems.add(hstSiteMapItem);
    }
    
    public HstSiteMapItem getWildCardPatternChild(String value, List<HstSiteMapItem> excludeList){
        if(value == null || containsWildCardChildSiteMapItems.isEmpty()) {
            return null;
        }
        return match(value, containsWildCardChildSiteMapItems, excludeList);
    }
    
    public HstSiteMapItem getAnyPatternChild(String[] elements, int position, List<HstSiteMapItem> excludeList){
        if(value == null || containsAnyChildSiteMapItems.isEmpty()) {
            return null;
        }
        StringBuffer remainder = new StringBuffer(elements[position]);
        while(++position < elements.length) {
            remainder.append("/").append(elements[position]);
        }
        return match(remainder.toString(), containsAnyChildSiteMapItems, excludeList);
    }
    
    
    public boolean patternMatch(String value, String prefix, String postfix ) {
     // postFix must match
        if(prefix != null && !"".equals(prefix)){
            if(prefix.length() >= value.length()) {
                // can never match
                return false;
            }
            if(!value.substring(0, prefix.length()).equals(prefix)){
                // wildcard prefixed sitemap does not match the prefix. we can stop
                return false;
            }
        }
        if(postfix != null && !"".equals(postfix)){
            if(postfix.length() >= value.length()) {
                // can never match
                return false;
            }
            if(!value.substring(value.length() - postfix.length()).equals(postfix)){
                // wildcard prefixed sitemap does not match the postfix . we can stop
                return false;
            }
        }
        // if we get here, the pattern matched
        return true;
    }
    
    private HstSiteMapItem match(String value, List<HstSiteMapItemService> patternSiteMapItems, List<HstSiteMapItem> excludeList) {
        
        for(HstSiteMapItemService item : patternSiteMapItems){
            // if in exclude list, go to next
            if(excludeList.contains(item)) {
                continue;
            }
            
            if(patternMatch(value, item.getPrefix(),  item.getPostfix())) {
                return item;
            }
            
        }
        return null;
    }


    public String getNamedPipeline() {
        return this.namedPipeline;
    }
    
    public String getPostfix(){
        return this.postfix;
    }
    
    public String getExtension(){
        return this.extension;
    }
    
    public String getPrefix(){
        return this.prefix;
    }
    
    public boolean containsWildCard() {
        return this.containsWildCard;
    }
    
    public boolean containsAny() {
        return this.containsAny;
    }

    public void setUseableInRightContextOnly(boolean useableInRightContextOnly) {
        this.useableInRightContextOnly = useableInRightContextOnly;
    }

    public boolean isUseableInRightContextOnly() {
        return this.useableInRightContextOnly;
    }

    public void setKeyToPropertyPlaceHolderMap(Map<String, String> keyToPropertyPlaceHolderMap) {
       this.keyToPropertyPlaceHolderMap = keyToPropertyPlaceHolderMap; 
    }
    
    public Map<String, String> getKeyToPropertyPlaceHolderMap() {
        return this.keyToPropertyPlaceHolderMap;
    }
    
    public int getDepth() {
        return this.depth;
    }

    public String getQualifiedId() {
        return qualifiedId;
    }

    public boolean isExcludedForLinkRewriting() {
        return isExcludedForLinkRewriting;
    }

}
