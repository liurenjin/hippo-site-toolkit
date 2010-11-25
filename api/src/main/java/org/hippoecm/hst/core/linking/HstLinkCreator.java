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

import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.configuration.HstSites;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.hosting.VirtualHosts;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;

/**
 * HstLinkCreator implementations must be able to create a <code>{@link HstLink}</code> for the methods
 * <ul>
 *  <li>{@link #create(HstSiteMapItem)}</li>
 *  <li>{@link #create(HstSite, String)}</li>
 *  <li>{@link #create(Node, ResolvedSiteMapItem)}</li>
 *  <li>{@link #create(String, ResolvedSiteMapItem)}</li>
 * </ul>
 * 
 * A specific implementation must be available on the <code>HstRequestContext</code> through the 
 * {@link org.hippoecm.hst.core.request.HstRequestContext#getHstLinkCreator()}.
 *
 */
public interface HstLinkCreator {


    /**
     * Rewrite a jcr uuid to a HstLink wrt its current ResolvedSiteMapItem. 
     * @param uuid the uuid of the node that must be used to link to
     * @param session jcr session 
     * @param resolvedSiteMapItem
     * @return an <code>HstLink</code> instance or <code>null<code> 
     */
    HstLink create(String uuid, Session session, ResolvedSiteMapItem resolvedSiteMapItem);
    
    
    /**
     * Rewrite a jcr Node to a HstLink wrt its current ResolvedSiteMapItem
     * @param node
     * @param resolvedSiteMapItem
     * @return the HstLink for this jcr Node or <code>null</code>
     */
    HstLink create(Node node, ResolvedSiteMapItem resolvedSiteMapItem);
    
    /**
     * Rewrite a jcr Node to a HstLink wrt its current ResolvedSiteMapItem and preferredItem. When <code>preferredItem</code> is not <code>null</code>, the link is tried to be rewritten to 
     * one of the descendants (including itself) of the preferred {@link HstSiteMapItem}. When <code>preferredItem</code> is <code>null</code>, a link is created against the entire sitemap item tree. When there cannot be created an HstLink to a descendant HstSiteMapItem 
     * or self, then:
     * 
     * <ol>
     *  <li>when <code>fallback = true</code>, a fallback to {@link #create(Node, ResolvedSiteMapItem)} is done</li>
     *  <li>when <code>fallback = false</code>, dependent on the implementation some error HstLink or <code>null</code> can be returned</li>
     * </ol>
     * <p>
     * This method returns an {@link HstLink} that takes the current URL into account, but does compute the link with respect to the physical (canonical) location
     * of the jcr Node. <b>If</b> you need a {@link HstLink} within the context of the possible virtual jcr Node (for example in case of in context showing documents in faceted navigation), use
     * {@link #create(Node, ResolvedSiteMapItem, HstSiteMapItem, boolean, boolean)} with <code>navigationStateful = true</code>
     * </p>
     * @see #create(Node, ResolvedSiteMapItem, HstSiteMapItem, boolean, boolean) 
     * @param node the jcr node
     * @param resolvedSiteMapItem the current resolved sitemap item
     * @param preferredItem if not null (null means no preferred sitemap item), first a link is trying to be created for this item
     * @param fallback value true or false
      * @return the HstLink for this jcr Node or <code>null</code>
     */
    HstLink create(Node node, ResolvedSiteMapItem resolvedSiteMapItem, HstSiteMapItem preferredItem, boolean fallback);
    
    /**
     * <p>
     * This method creates the same {@link HstLink} as {@link #create(Node, ResolvedSiteMapItem, HstSiteMapItem, boolean)} when <code>navigationStateful = false</code>. When <code>navigationStateful = true</code>, 
     * the link that is created is with respect to the jcr Node <code>node</code>, even if this node is a virtual location. This is different then {@link #create(Node, ResolvedSiteMapItem, HstSiteMapItem, boolean)}: that
     * method always first tries to find the canonical location of the jcr Node before it is creating a link for the node. 
     * </p>
     * 
     * <p>
     * <b>Expert:</b> Note there is a difference between context relative with respect to the current URL and with respect to the current jcr Node. <b>Default</b>, links in the HST are
     * created always taking into account the current URL (thus context aware linking) unless you call {@link #createCanonical(Node, ResolvedSiteMapItem)} or {@link #createCanonical(Node, ResolvedSiteMapItem, HstSiteMapItem)}. Also,
     * <b>default</b>, it always (unless there is no) takes the <i>canonical</i> location of the jcr Node. Thus, multiple virtual versions of the same physical Node, result in the same HstLink. Only when having <code>navigationStateful = true</code>, 
     * also the jcr Node is context relative, and thus multiple virtual versions of the same jcr Node can result in multiple links. This is interesting for example in 
     * faceted navigation views, where you want 'in context' documents to be shown.
     * </p>
     * @see #create(Node, ResolvedSiteMapItem, HstSiteMapItem, boolean)
     * @param node the jcr node 
     * @param resolvedSiteMapItem  the current resolved sitemap item
     * @param preferredItem  if not null (null means no preferred sitemap item), first a link is trying to be created for this item
     * @param fallback value true or false
     * @param navigationStateful value true or false
     * @return  the HstLink for this jcr Node or <code>null</code>
     */
    HstLink create(Node node, ResolvedSiteMapItem resolvedSiteMapItem, HstSiteMapItem preferredItem, boolean fallback, boolean navigationStateful);
    
    /**
     * This creates a canonical HstLink: regardless the context, one and the same jcr Node is garantueed to return the same HstLink. This is
     * useful when showing one and the same content via multiple urls, for example in faceted navigation. Search engines can better index your
     * website when defining a canonical location for duplicate contents: See 
     * <a href="http://googlewebmastercentral.blogspot.com/2009/02/specify-your-canonical.html">specify-your-canonical</a> for more info on this subject.
     * 
     * @param node
     * @param resolvedSiteMapItem
     * @return the HstLink for this jcr Node or <code>null</code>
     */
    HstLink createCanonical(Node node, ResolvedSiteMapItem resolvedSiteMapItem);
    
    /**
     * @see {@link #createCanonical(Node, ResolvedSiteMapItem)}.
     * When specifying a preferredItem, we try to create a canonical link wrt this preferredItem. If the link cannot be created for this preferredItem,
     * a fallback to {@link #createCanonical(Node, ResolvedSiteMapItem)} without preferredItem is done.
     * 
     * @param node
     * @param resolvedSiteMapItem
     * @param preferredItem if <code>null</code>, a fallback to {@link #createCanonical(Node, ResolvedSiteMapItem)} is done
     * @return the HstLink for this jcr Node or <code>null</code>
     */
    HstLink createCanonical(Node node, ResolvedSiteMapItem resolvedSiteMapItem, HstSiteMapItem preferredItem);
    
    
    /**
     * <p>Expert: Rewrite a jcr <code>node</code> to a {@link HstLink} with respect to the <code>hstSite</code>. Note that this HstLink creation does only take into account the
     * <code>hstSite</code>.
     * This might be a different one then the one of the current request context, reflected in the {@link ResolvedSiteMapItem}, 
     * for example in the method {@link #create(Node, ResolvedSiteMapItem)}. 
     * If the <code>hstSite</code> cannot be used to create a HstLink for the jcr <code>node</code>, because the <code>node</code> belongs
     * to a different (sub)site, <code>null</code> is returned. </p>
     * <p>note: if an link is returned, this is always the canonical link, also see {@link #createCanonical(Node, ResolvedSiteMapItem)}</p>
     * @param node the jcr node for that should be translated into a HstLink
     * @param hstSite the (sub)site for which the hstLink should be created for
     * @return the {@link HstLink} for the jcr <code>node</code> and the <code>hstSite</code> or <code>null</code> when no link for the node can be made in the <code>hstSite</code>
     */
    HstLink create(Node node, HstSite hstSite);


    /**
     * <p>Expert: Rewrite a jcr <code>node</code> to a {@link HstLink} wrt the the {@link #HstSites} <code>hstSites</code>: the (sub)site to which the 
     * jcr <code>node</code> belongs is not known on beforehand. The  {@link HstSite} belonging to the resulting {@link HstLink} can be different than the {@link HstSite} from the current request. 
     * This means, that the final url being created from this {@link HstLink} even can have a different host name, depending on the {@link VirtualHosts}</p> configuration
     * <p>note: if an link is returned, this is always the canonical link, also see {@link #createCanonical(Node, ResolvedSiteMapItem)}</p>
     * @param node the jcr node for that should be translated into a {@link HstLink} 
     * @param hstSites the {@link HstSites} that are being tried to create a {@link HstLink} for
     * @return the {@link HstLink}  for this jcr <code>node</code> and <code>hstSites</code> or <code>null</code> when no link can be created
     */
    HstLink create(Node node, HstSites hstSites);
    
    /**
     * 
     * @param node
     * @param hstRequestContext
     * @return
     * @deprecated  Use {@link  #create(Node, ResolvedSiteMapItem)} 
     */
    HstLink create(Node node, HstRequestContext hstRequestContext);
    
    /**
     * 
     * @param bean
     * @param hstRequestContext
     * @return
     */
    HstLink create(HippoBean bean, HstRequestContext hstRequestContext);
    
    
    /**
     * For creating a link from a HstSiteMapItem to a HstSiteMapItem with toSiteMapItemId
     * @param toSiteMapItemId
     * @param resolvedSiteMapItem
     * @return an <code>HstLink</code> instance or <code>null<code> 
     */
    HstLink create(String toSiteMapItemId, ResolvedSiteMapItem resolvedSiteMapItem);
    
    /**
     * Regardless the current context, create a HstLink to the HstSiteMapItem that you use as argument. This is only possible if the sitemap item does not
     * contain any ancestor including itself with a wildcard, because the link is ambiguous in that case. 
     * If a wildcard is encountered, this method can return <code>null</code>, though this is up to the implementation
     * @param toHstSiteMapItem the {@link HstSiteMapItem} to link to
     * @return an <code>HstLink</code> instance or <code>null<code> 
     */
    HstLink create(HstSiteMapItem toHstSiteMapItem);
    
    /**
     * Regardless the current context, create a HstLink to the path that you use as argument. 
     * @param path the path to the sitemap item
     * @param hstSite the HstSite the siteMapPath should be in
     * @return an <code>HstLink</code> instance or <code>null<code> 
     */
    HstLink create(String path, HstSite hstSite);
    
    /**
     * Regardless the current context, create a HstLink to the path that you use as argument. 
     * @param path the path to the sitemap item
     * @param hstSite the HstSite the siteMapPath should be in
     * @param containerResource whether it is a static link, for example for css/js
     * @return an <code>HstLink</code> instance or <code>null<code> 
     */
    HstLink create(String path, HstSite hstSite, boolean containerResource);
    
    /**
     * create a link to a HstSiteMapItem with id <code>toSiteMapItemId</code> that belongs to <code>HstSite</code> hstSite.
     * Note that the HstSite can be a different one then the current, possibly resulting in a cross-domain (host) link. 
     * A <code>HstLink</code> can only be created unambiguous if the <code>HstSiteMapItem</code> belonging to toSiteMapItemId does not
     * contain any ancestor including itself with a wildcard. 
     * If a wildcard is encountered, this method can return <code>null</code>, though this is up to the implementation
     * @param hstSite the HstSite the toSiteMapItemId should be in
     * @param toSiteMapItemId the id of the SiteMapItem ({@link HstSiteMapItem#getId()})
     * @return HstLink
     */
    HstLink create(HstSite hstSite, String toSiteMapItemId);
    
    /**
     * Binaries frequently have a different linkrewriting mechanism. If this method returns <code>true</code> the location is a
     * binary location. 
     * @param path
     * @return <code>true</code> when the path points to a binary location
     */
    boolean isBinaryLocation(String path);
    
    /**
     * @return The prefix that is used for binary locations. The returned binaries prefix is relative to <code>/</code> and 
     * does not include the <code>/</code> itself. If no binaries prefix is configured, <code>""</code> will be returned
     */ 
    String getBinariesPrefix();
    
    /**
     * @return the list of location resolvers, primarily used for resolving custom binary locations 
     */
    List<LocationResolver> getLocationResolvers();
}
