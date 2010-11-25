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
package org.hippoecm.hst.core.linking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hippoecm.hst.configuration.Configuration;
import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItemService;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.util.PropertyParser;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BasicLocationMapTree implements LocationMapTree{

    private final static Logger log = LoggerFactory.getLogger(BasicLocationMapTree.class);
    
    private Map<String, LocationMapTreeItem> children = new HashMap<String, LocationMapTreeItem>();
    private String canonicalSiteContentPath;
    @SuppressWarnings("unchecked")
    public BasicLocationMapTree(String canonicalSiteContentPath) {
        this.canonicalSiteContentPath = canonicalSiteContentPath;
        
    }
    
    public void add(String unresolvedPath, HstSiteMapItem hstSiteMapItem){
        if(unresolvedPath == null) {
            return;
        }
        Properties params = new Properties();
        // see if there are any SiteMapItems which are wildcard or any matchers and set these as properties.
        
        // list of the sitemap items ancestors + the sitemap item itself. The last item in the list is the top parent
        List<HstSiteMapItem> ancestorItems = new ArrayList<HstSiteMapItem>();
        ancestorItems.add(hstSiteMapItem);
        HstSiteMapItem parent = hstSiteMapItem.getParentItem();
        while(parent != null) {
            ancestorItems.add(parent);
            parent = parent.getParentItem();
        }
        
        // traverse the ancestors list now to see if there are wildcard or any matchers
        int index = ancestorItems.size();
        while(index-- != 0) {
            HstSiteMapItemService s = (HstSiteMapItemService)ancestorItems.get(index);
            if(s.isWildCard()) {
                params.put(String.valueOf(params.size()+1), Configuration.WILDCARD);
            } else if(s.isAny()) {
                params.put(String.valueOf(params.size()+1), Configuration.ANY);
            } else if( s.containsWildCard() ) {
                // we assume a postfix containing a "." only meant for document url extension, disregard for linkmatching first
                String paramVal = s.getPrefix()+Configuration.WILDCARD;
                if(s.getPostfix().indexOf(".") > -1) {
                    String post = s.getPostfix().substring(0,s.getPostfix().indexOf("."));
                    if(!"".equals(post)) {
                        paramVal += post;
                    }
                } else {
                    paramVal += s.getPostfix();
                }
                params.put(String.valueOf(params.size()+1), paramVal);
            } else if( s.containsAny() ) {
               // we assume a postfix containing a "." only meant for document url extension, disregard for linkmatching first
                String paramVal = s.getPrefix()+Configuration.ANY;
                if(s.getPostfix().indexOf(".") > -1) {
                    String post = s.getPostfix().substring(0,s.getPostfix().indexOf("."));
                    if(!"".equals(post)) {
                        paramVal += post;
                    }
                } else {
                    paramVal += s.getPostfix();
                }
                params.put(String.valueOf(params.size()+1), paramVal);
            }
        }
        
        PropertyParser pp = new PropertyParser(params);
        
        /*
         * If and only IF all property placeholders do occur in the 'unresolvedPath' it is possible to use this 
         * sitemapItem plus relative contentpath in the LocationMapTree: think about it: If my sitemap path would be 
         * '_default_/home' (_default_ = * ) and the relative content path would be /common/home, than obviously, we cannot
         * create a link to this sitemap item: namely, what would we take for _default_ ? If the relative content path would be 
         * ${1}/home, then we can use it. Therfor, we now check whether all 
         */
        
        for(Object param : params.keySet()) {
            String propertyPlaceHolder = PropertyParser.DEFAULT_PLACEHOLDER_PREFIX + ((String)param) + PropertyParser.DEFAULT_PLACEHOLDER_SUFFIX;
            if(!unresolvedPath.contains(propertyPlaceHolder)) {
                log.warn("Skipping relative content path '{}' for linkrewriting map for sitemap item '{}' because we can never write a correct link to it: There are " +
                		"more wildcards in the sitemap item path than in the relative content path, hence we cannot use it for linkrewriting. ", unresolvedPath, hstSiteMapItem.getId());
                return;
            }
        }
        
        String resolvedPath = (String)pp.resolveProperty("relative contentpah", unresolvedPath);
       
        if(resolvedPath == null) {
            log.warn("Unable to translate relative content path : '{}' because the wildcards in sitemap item path ('{}') does not match the property placeholders in the relative content path. We skip this path for linkrewriting", unresolvedPath, hstSiteMapItem.getId());
            return;
        } 
        log.debug("Translated relative contentpath '{}' --> '{}'", unresolvedPath, resolvedPath);
        List<String> pathFragment = new ArrayList<String>(Arrays.asList(resolvedPath.split("/")));
        
        add(pathFragment, hstSiteMapItem);
    }
    
    public void add(List<String> pathFragment, HstSiteMapItem hstSiteMapItem){
        
        BasicLocationMapTreeItem child = (BasicLocationMapTreeItem) getTreeItem(pathFragment.get(0));
        if(child == null) {
            child = new BasicLocationMapTreeItem();
            this.children.put(pathFragment.get(0), child);
        }
        pathFragment.remove(0);
        child.add(pathFragment , hstSiteMapItem);
    }
    
    public LocationMapTreeItem getTreeItem(String name) {
        return children.get(name);
    }

    
    public ResolvedLocationMapTreeItem match(String path, HstSite hstSite,boolean representsDocument, ResolvedSiteMapItem resolvedSiteMapItem) {
        String origPath = path;
      
        if(!path.startsWith(this.getCanonicalSiteContentPath())){
            log.debug("Cannot convert path '{}' for hstSite '{}' because the path does not start " +
                    "with the content path of the hstSite. Return null", path, hstSite.getName());
            return null;
        }
        path = path.substring(this.getCanonicalSiteContentPath().length());
        // normalize leading and trailing slashes
        path = PathUtils.normalizePath(path);
     
        String[] elements = path.split("/"); 
        
        
        LocationMapTreeItem matchedLocationMapTreeItem = null;
       
        Properties params = new Properties();
        PropertyParser pp = new PropertyParser(params);
        

        LocationMapTreeItem locationMapTreeItem = this.getTreeItem(elements[0]);
        if(locationMapTreeItem != null) {
            matchedLocationMapTreeItem =  resolveMatchingSiteMap(locationMapTreeItem, params, 1, elements);
        }
        
       // test for * matcher because we did not yet be able to resolve to a matching sitemap
        if(matchedLocationMapTreeItem == null) {
            params.clear();
            locationMapTreeItem = this.getTreeItem(Configuration.WILDCARD);
            if(locationMapTreeItem != null) {
                params.put(String.valueOf(params.size()+1), elements[0]);
                matchedLocationMapTreeItem =  resolveMatchingSiteMap(locationMapTreeItem, params, 1, elements);
            }
        }
        
       // test for ** matcher because we did not yet be able to resolve to a matching sitemap
        if(matchedLocationMapTreeItem == null) {
            params.clear();
            locationMapTreeItem = this.getTreeItem(Configuration.ANY);
            if(locationMapTreeItem != null) {
                params.put(String.valueOf(params.size()+1), path);
                matchedLocationMapTreeItem =  locationMapTreeItem;
            }
        }

        if(matchedLocationMapTreeItem == null || matchedLocationMapTreeItem.getHstSiteMapItems().size() == 0) {
            log.warn("Unable to linkrewrite '{}' to any sitemap item", origPath);
            return null;
        }
        
        HstSiteMapItem hstSiteMapItem = null;
        
      
        if(matchedLocationMapTreeItem.getHstSiteMapItems().size() > 1) {
            log.debug("Multiple sitemap items are suited equally for linkrewrite of '{}'. If we represent a document, see if can map to an extension", path);
           
            List<HstSiteMapItem> matchingSiteMapItems = new ArrayList<HstSiteMapItem>();
            for(HstSiteMapItem item : matchedLocationMapTreeItem.getHstSiteMapItems()) {
                HstSiteMapItemService serv = (HstSiteMapItemService)item;
                if(representsDocument) {
                    if(serv.getExtension() != null) {
                        //  found a sitemap item with an extension! Add this one
                        matchingSiteMapItems.add(serv);
                    } else {
                        continue;
                    }
                } else {
                    if(serv.getExtension() == null) {
                        //  found a sitemap item without an extension! Add this one
                        matchingSiteMapItems.add(serv);
                    } else {
                        continue;
                    }
                }
            }
            if(matchingSiteMapItems.size() == 0) {
                log.debug("Did not find a sitemap item that can represent this item. We return the first sitemap item.");
            } else if(matchingSiteMapItems.size() == 1) {
                hstSiteMapItem = matchingSiteMapItems.get(0);
            } else {
                log.debug("Multiple sitemap items are equally well suited for linkrewriting. Let's find if there is a sitemap item that" +
                		"has precedence.");
                
                // fetch the best matching sitemap item: 
                hstSiteMapItem = fetchBestInContext(matchingSiteMapItems, resolvedSiteMapItem.getHstSiteMapItem());
                
            }
        } 
        
        
        if(hstSiteMapItem == null){
            hstSiteMapItem = matchedLocationMapTreeItem.getHstSiteMapItems().get(0);
        }
        
        String resolvedPath = (String)pp.resolveProperty("parameterizedPath", ((HstSiteMapItemService)hstSiteMapItem).getParameterizedPath());
        if(resolvedPath == null) {
            log.warn("Unable to resolve '{}'. Return null", ((HstSiteMapItemService)hstSiteMapItem).getParameterizedPath());
          
            return null;
        }
        
        log.info("Succesfully rewrote path '{}' into new sitemap path '{}'", origPath, resolvedPath);
        
        ResolvedLocationMapTreeItem r = new ResolvedLocationMapTreeItemImpl(resolvedPath, hstSiteMapItem.getId());
        
        
        return r;
      
    }

    private LocationMapTreeItem resolveMatchingSiteMap(LocationMapTreeItem locationMapTreeItem, Properties params, int position, String[] elements) {
        return traverseInToLocationMapTreeItem(locationMapTreeItem, params, position, elements, new ArrayList<LocationMapTreeItem>());
     }

     private LocationMapTreeItem traverseInToLocationMapTreeItem(LocationMapTreeItem locationMapTreeItem, Properties params, int position, String[] elements, List<LocationMapTreeItem> checkedLocationMapTreeItems) {
         
         checkedLocationMapTreeItems.add(locationMapTreeItem);
         if(position == elements.length) {
            // we are ready
            return locationMapTreeItem;
        }
        if(locationMapTreeItem.getChild(elements[position]) != null && !checkedLocationMapTreeItems.contains(locationMapTreeItem.getChild(elements[position]))) {
            return traverseInToLocationMapTreeItem(locationMapTreeItem.getChild(elements[position]), params, ++position, elements, checkedLocationMapTreeItems);
        } else if(locationMapTreeItem.getChild(Configuration.WILDCARD) != null && !checkedLocationMapTreeItems.contains(locationMapTreeItem.getChild(Configuration.WILDCARD))) {
            params.put(String.valueOf(params.size()+1), elements[position]);
            return traverseInToLocationMapTreeItem(locationMapTreeItem.getChild(Configuration.WILDCARD), params, ++position, elements, checkedLocationMapTreeItems);
        } else if(locationMapTreeItem.getChild(Configuration.ANY) != null ) {
            return getANYMatchingLocationMapTreeItem(locationMapTreeItem, params,position, elements);
        }  
        else {
            // We did not find a match for traversing this sitemap item tree. Traverse up, and try another tree
            return traverseUp(locationMapTreeItem, params, position, elements, checkedLocationMapTreeItems);
        }
        
     }


     private LocationMapTreeItem traverseUp(LocationMapTreeItem locationMapTreeItem, Properties params, int position, String[] elements, List<LocationMapTreeItem> checkedLocationMapTreeItems) {
        if(locationMapTreeItem == null) {
            return null;
        }
        if(((BasicLocationMapTreeItem)locationMapTreeItem).isWildCard()) {
            // as this tree path did not result in a match, remove some params again
            if(locationMapTreeItem.getChild(Configuration.WILDCARD) != null && !checkedLocationMapTreeItems.contains(locationMapTreeItem.getChild(Configuration.WILDCARD))){
                return traverseInToLocationMapTreeItem(locationMapTreeItem, params, position, elements, checkedLocationMapTreeItems);
            } else if(locationMapTreeItem.getChild(Configuration.ANY) != null) {
                return traverseInToLocationMapTreeItem(locationMapTreeItem, params,position, elements, checkedLocationMapTreeItems);
            }
            params.remove(String.valueOf(params.size()));
            return traverseUp(locationMapTreeItem.getParentItem(),params, --position, elements, checkedLocationMapTreeItems );
        } else if(locationMapTreeItem.getChild(Configuration.WILDCARD) != null && !checkedLocationMapTreeItems.contains(locationMapTreeItem.getChild(Configuration.WILDCARD))){
            return traverseInToLocationMapTreeItem(locationMapTreeItem, params, position, elements, checkedLocationMapTreeItems);
        } else if(locationMapTreeItem.getChild(Configuration.ANY) != null ){
            return traverseInToLocationMapTreeItem(locationMapTreeItem, params,position, elements, checkedLocationMapTreeItems);
        } else {    
            return traverseUp(locationMapTreeItem.getParentItem(),params, --position, elements, checkedLocationMapTreeItems );
        }
        
     }
     
     private LocationMapTreeItem getANYMatchingLocationMapTreeItem(LocationMapTreeItem locationMapTreeItem, Properties params, int position,
             String[] elements) {
             StringBuffer remainder = new StringBuffer(elements[position]);
             while(++position < elements.length) {
                 remainder.append("/").append(elements[position]);
             }
             params.put(String.valueOf(params.size()+1), remainder.toString());
             return locationMapTreeItem.getChild(Configuration.ANY);
     }
    
    public String getCanonicalSiteContentPath() {
      return this.canonicalSiteContentPath;
    }
    
    
    /*
     * find the sitemap item that matches the context best: first whether it is equal to the current ctx sitemap item,
     * then the sitemap item that is a closest child, and the the closest parent. If this results in no match, pick the first.
     * 
     * if the list is empty, return null
     */
    private HstSiteMapItem fetchBestInContext(List<HstSiteMapItem> matchingSiteMapItems, HstSiteMapItem currentCtxSiteMapItem) {
        int bestDepth = Integer.MAX_VALUE;
        HstSiteMapItem bestMatch = null;
        
        for(HstSiteMapItem siteMapItem : matchingSiteMapItems) {
            if(siteMapItem == currentCtxSiteMapItem) {
                return siteMapItem;
            }
            
            int depth = 0;
            HstSiteMapItem current = siteMapItem;
            while(current.getParentItem() != null) {
                current = current.getParentItem();
                depth++;
                if(current == currentCtxSiteMapItem) {
                    if(depth < bestDepth) {
                        bestMatch = siteMapItem;
                        bestDepth = depth;
                    }
                    break;
                }
            }
        }
        
        if(bestMatch != null) {
            return bestMatch;
        }
        
        // checked all childs and self. No result. Now check parents & parents sibblings
        for(HstSiteMapItem siteMapItem : matchingSiteMapItems) {
           
            int depth = 0;
            HstSiteMapItem current = currentCtxSiteMapItem;
            
            while(current.getParentItem() != null) {
                current = current.getParentItem();
                depth++;
                
                if(current == siteMapItem) {
                    if(depth < bestDepth) {
                        bestMatch = siteMapItem;
                        bestDepth = depth;
                    }
                    break;
                }
                
                // check sibblings
                
                for(HstSiteMapItem sibbling : current.getChildren()) {
                    if(sibbling == siteMapItem) {
                        if(depth < bestDepth) {
                            bestMatch = siteMapItem;
                            bestDepth = depth;
                        }
                        break;
                    }
                }
                
            }
        }
        
        if(bestMatch != null) {
          return bestMatch;  
        }
        
        // there is no sitemap item with a better precedence, return the first in the list
        if(matchingSiteMapItems.size() > 0) {
            return matchingSiteMapItems.get(0);
        }
        
        
        return null;
    }

    

}
