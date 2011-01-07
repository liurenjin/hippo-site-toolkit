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
package org.hippoecm.hst.configuration.hosting;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.request.ResolvedMount;

/**
 * <p>A {@link Mount} object is the mount from a prefix to some (sub)site *or* content location: when the {@link Mount#isMapped()} property returns <code>true</code> or missing,
 * the {@link Mount} is linked to a {@link HstSite}. When {@link Mount#isMapped()} property returns <code>false</code>, the {@link Mount} should have it's own namedPipeline and the
 * <code>hst:mountpoint</code> property is used a content path: for example a simple jcr browser unaware of HstSite at all could be easily built with this. 
 * </p>
 * <p>
 * {@link Mount} is a Composite pattern: Each {@link Mount} can contain any descendant
 * child {@link Mount} tree. A {@link Mount} 'lives' below a {@link VirtualHost}. The {@link Mount} directly below a {@link VirtualHost} <b>must</b> be called <b>hst:root</b> by definition. 
 * The <code>hst:root</code> {@link Mount} is where the {@link Mount} matching starts. Once a virtual host is matched for the incoming {@link HttpServletRequest},
 * we inspect the request path (the path after the context path and before the query string) and try to match the request path as deep as possible to the {@link Mount} tree: note that 
 * we try to map to the best matching {@link Mount}: This means, that 'exact' matching names have precedence over wildcard names
 * </p>
 * 
 * Thus, suppose we have the following {@link Mount} tree configuration:
 * 
 * <pre>
 *    127.0.0.1
 *      `- hst:root  (hst:mountpoint = /live/myproject, hst:ismapped = true)
 *            `- preview (hst:mountpoint = /preview/myproject, hst:ismapped = true)
 * </pre>
 * <p>
 * The configuration above means, that below the host 127.0.0.1 we have of course the mandatory {@link Mount} <code>hst:root</code>, and below it, we have 
 * a {@link Mount} <code>preview</code>. Every request path that starts with <code>/preview</code> will be mapped to 'preview' {@link Mount}, all other request path's that do not start with '/preview' 
 * resolve to the <code>hst:root<code> item. While the request path does match the next {@link Mount} descendant in the tree, the matching is continued to descendant {@link Mount} items. Thus, in the current example,
 * the request path <code>/preview/home</code> will return the {@link Mount} with name 'preview'. 
 * </p>
 * 
 * Also, you can configure some of the same properties the {@link VirtualHost} also has:
 *  <ul>
 *  <li>hst:port (long)</li>
 *  <li>hst:showcontextpath (boolean)</li>
 *  <li>hst:showport(boolean)</li>
 *  <li>hst:scheme (string)</li>
 *  </ul>
 * 
 * <p>One extra is possible, <code>hst:namedpipeline</code>, see below for an example.</p> 
 * 
 * <p>Just as with the virtual hosts, properties are <b>inherited</b> by child {@link Mount} items as long as they are not defined by themselves
 * </p>
 * 
 * Obviously, the above configuration might not be desirable in some circumstances, for example, when on a production host, you do not want 
 * a preview available at all. Configuration then for example could be:
 * 
 * <pre>
 *    www.mycompany.com
 *        ` hst:root  (hst:mountpoint = /live/myproject)
 *    preview.mycompany.com
 *        ` hst:root  (hst:mountpoint = /preview/myproject)
 * </pre>
 * 
 * <p>As can be seen, instead of using the request path prefix to distuinguish between live and preview, we now do so by hostname.</p>
 * 
 * An example with more {@link Mount} items is for example:
 * 
 * <pre>
 *    127.0.0.1
 *      `- hst:root  (hst:mountpoint = /live/myproject)
 *            |-myrestservice (hst:mountpoint = /live/myproject, hst:namedpipeline=JaxrsPipeline)
 *            `- preview (hst:mountpoint = /preview/myproject)
 *                  `-myrestservice (hst:mountpoint = /preview/myproject, hst:namedpipeline=JaxrsPipeline)
 * </pre>
 * 
 * <p>Now, all request path's that start with <code>/myrestservice</code> or  <code>/preview/myrestservice</code> resolve to the <code>myrestservice</code>
 * {@link Mount} item. This one has a custom <code>hst:namedpipeline</code>, where you can configure a complete custom hst request processing, in this example
 * some pipeline exposing some rest interface.</p>
 * 
 * <p>
 * Optionally, wildcard matching support can be implemented, where even the wilcards can be used within the property values. For example:
 * <pre>
 *    127.0.0.1
 *      `- hst:root  (hst:mountpoint = /live/myproject)
 *            |- * (hst:mountpoint = /live/${1})
 *            `- preview (hst:mountpoint = /preview/myproject)
 *                  ` - * (hst:mountpoint = /preview/${1})
 * </pre>
 * 
 * The above means, that when there is a (sub)site complying to the wildcard, this one is used, and otherwise, the default one pointing
 * to <code>myproject</code> is used. Thus, a request path like <code>/preview/mysubsite/home</code> will map to the {@link Mount} <code>/preview/*</code>
 * if there is a (sub)site at the mountpoint <code>/preview/${1}</code>, where obviously ${1} is substituted by 'mysubsite'. Assuming that there
 * is no subsite called 'news', a request path like /preview/news/2010/05/my-news-item will thus be handled by the site 'myproject'
 * 
 * </p>
 * 
 */
public interface Mount {

    /**
     * the predefined property name prefix to indicates mount aliases
     */
    static final String PROPERTY_NAME_MOUNT_PREFIX = "hst:mount";
    
    /**
     * @return The name of this {@link Mount} item
     */
    String getName();
    
    /**
     * Returns the alias of this {@link Mount} item. The alias of a {@link Mount} <b>must</b> be unique in combination with every type {@link #getTypes()} within a single host group, also 
     * see ({@link VirtualHost#getHostGroupName()}). When there is no alias defined on the {@link Mount}, <code>null</code> is returned. The {@link Mount} can then not be used 
     * to lookup by alias
     * @return The alias of this {@link Mount} item or <code>null</code> when it does not have one
     */
    String getAlias();
    
    /**
     * When this {@link Mount} is not using a {@link HstSite} for the request processing, this method returns <code>true</code>. When it returns <code>true</code>, then 
     * {@link #getNamedPipeline()} should also be configured, and a pipeline should be invoked that is independent of the {@link ResolvedSiteMapItem} as their won't be one.
     */
    boolean isMapped();
    
    /**
     * @return the parent {@link Mount} of this {@link Mount} and <code>null</code> if we are at the root {@link Mount}
     */
    Mount getParent();
    
    /**
     * <p>
     * Returns the mount point for this {@link Mount} object. The mount point can be the absolute jcr path to the root site node, for example 
     * something like '/hst:hst/hst:sites/mysite-live', but it can also be some jcr path to some virtual or canonical node in the repository. For example
     * it can be '/content/gallery' , which might be the case for a Mount suited for REST gallery calls. 
     * </p>
     * 
     * @see ResolvedMount#getResolvedMountPath()
     * @return the mountPoint for this {@link Mount}
     */
    String getMountPoint();
    
    /**
     * <p>
     * Returns the content path for this {@link Mount} object. The content path is the absolute jcr path to the root site node content, for example 
     * something like '/hst:hst/hst:sites/mysite-live/hst:content'. The {@link #getContentPath()} can be the same as {@link #getMountPoint()}, but
     * this is in general only for {@link Mount}'s that have {@link #isMapped()} returning false. When the {@link Mount} does have
     * {@link #isMapped()} equal to true, the {@link #getContentPath()} can return a different path than {@link #getMountPoint()}. In general, it will be
     * then {@link #getMountPoint()} + "/hst:content". 
     * </p>
     * 
     * @return the content path for this {@link Mount}. It cannot be <code>null</code>
     */
    String getContentPath();
    
    /**
     * Returns the absolute canonical content path for the content of this {@link Mount}. Note that it can return the same
     * value as {@link #getContentPath()}, but this is in general not the case: When the {@link #getContentPath()} points
     * to a virtual node, this method returns the location of the canonical version. When the {@link #getContentPath()} points to 
     * a node which behaves like a <b>mirror</b>, then this method returns the location where the mirror points to. If 
     * {@link #getContentPath()} does not point to a virtual node, nor to a mirror, this method returns the same value. 
     * 
     * @return The absolute absolute content path for this {@link Mount}. It can be <code>null</code> in case {@link #getContentPath()} points to a virtual node
     * that does not have a canonical version.
     */
    String getCanonicalContentPath();
    

    /**
     * <p>
     * Returns the mount path for this {@link Mount} object. The root {@link Mount} has an empty {@link String} ("") as mount path. A mountPath for a 
     * {@link Mount} is its own {@link #getName()} plus all ancestors up to the root and always starts with a "/" (unless for the root, this one is empty).
     * It can contain wildcards, for example /preview/*. Typically, these wildcards are replaced by their request specific values in the {@link ResolvedMount}.
     * </p>
     * 
     * <p>
     * Note the difference with {@link #getMountPoint()}: the {@link #getMountPoint()} returns the jcr location of the (sub)site or of the content
     * </p>
     * 
     * @see ResolvedMount#getResolvedMountPath()
     * @return the mountPath for this {@link Mount}
     */
    String getMountPath();
    /**
     * 
     * @param name of the child {@link Mount}
     * @return a {@link Mount} with {@link #getName()} equal to <code>name</code> or <code>null</code> when there is no such item
     */
    Mount getChildMount(String name);
    
    /**
     * @return the virtualHost where this {@link Mount} belongs to
     */
    VirtualHost getVirtualHost();
    
    /**
     * @return the {@link HstSite} this <code>{@link Mount}</code> is pointing to or <code>null</code> when none found
     */
    HstSite getHstSite();
  
    /**
     * @return <code>true</code> when the created url should have the contextpath in it
     */
    boolean isContextPathInUrl();

    /**
     * @return <code>true</code> when the created url should have contain the port number
     */
    boolean isPortInUrl();
    
    /**
     * When this method returns <code>false</code>, then {@link HstLink} will always have the {@link HstLink#PATH_SUBPATH_DELIMITER} included, even if the {@link HstLink}
     * does have an empty or <code>null</code> {@link HstLink#getSubPath()}
     * @return true when the {@link Mount} is meant to be a site (false in case of for example being used for REST calls)
     */
    boolean isSite();
    

    /**
     * @return the portnumber for this {@link Mount}
     */
    int getPort();
    
    /**
     * In case the {@link HttpServletRequest#getContextPath()} does not matter, this method must return <code>null</code> or empty. <b>If</b> only this {@link Mount} 
     * can be used for a certain contextPath, this method should return that contextPath. A contextPath has to start with a "/" and is not allowed to have any other "/". 
     * 
     * @return <code>null</code> or empty if the contextPath does not matter, otherwise it returns the value the contextPath must have a possible to match to this {@link Mount}
     */
    String onlyForContextPath();
    
    /**
     * @return the homepage for this {@link Mount} or <code>null</code> when not present
     */
    String getHomePage();
    
    /**
     * 
     * @return the pagenotfound for this {@link Mount} or <code>null</code> when not present
     */
    String getPageNotFound();
    
    /**
     * @return the scheme to use for creating external urls, for example http / https
     */
    String getScheme();
    
    /**
     * This method returns the same as {@link Mount#isOfType(String type)} with <code>type="preview"</code>
     * 
     * @return <code>true</code> when this {@link Mount} is configured to be a preview Mount. 
     */
    boolean isPreview();
    
    
    /**
     * When a this {@link Mount} is of type <code>type</code> this returns <code>true</code>. A {@link Mount} can be of multiple types at once.
     * @param type the type to test
     * @return <code>true</code> when this {@link Mount} is of type <code>type</code>
     */
    boolean isOfType(String type);
    
    /**
     * @return the primary type of this {@link Mount}
     */
    String getType();
    
    /**
     * @return the list of all types this {@link Mount} belongs to, including the primary type {@link #getType()}. The primary type is the first item in the List
     */
    List<String> getTypes();
    
    /**
     * When this {@link Mount} has {@link #isPreview()} return <code>false</code>, this method always returns false. When the {@link Mount} is preview,
     * and the {@link Mount} is configured to have the hst version number in preview, then this method returns <code>true</code> 
     * @return <code>true</code> when for this {@link Mount} the current hst version should be added as a response header
     */
    boolean isVersionInPreviewHeader();
    
    /**
     * Note that if an ancestor {@link Mount} contains a namedPipeline, this value is inherited unless this {@link Mount} explicitly defines its own
     * @return the named pipeline to be used for this {@link Mount} or <code>null</code> when the default pipeline is to be used
     */
    String getNamedPipeline();
    
    /**
     * the locale for this {@link Mount} or <code>null</code> when it does not contain one. Note that if an ancestor {@link Mount} contains a 
     * locale, this value is inherited unless this {@link Mount} explicitly defines its own. The root {@link Mount} inherits the value from 
     * the {@link VirtualHost} if the virtual host contains a locale
     * @return the locale for this {@link Mount} or <code>null</code> when it does not contain one. 
     */
    String getLocale();

    /**
     * This is a shortcut method fetching the HstSiteMapMatcher from the backing {@link HstManager}
     * @return the HstSiteMapMatcher implementation
     */
    HstSiteMapMatcher getHstSiteMapMatcher();
    
    /**
     * for embedded delegation of sites a mountpath needs to point to the delegated {@link Mount}. This is only relevant for portal environment
     * @return the embedded {@link Mount} path and <code>null</code> if not present
     */
    String getEmbeddedMountPath();
    
    /**
     * If this method returns true, then only if the user is explicitly allowed or <code>servletRequest.isUserInRole(role)</code> returns <code>true</code> this
     * Mount is accessible for the request. 
     * 
     * If a Mount does not have a configuration for authenticated, the value from the parent item is taken.
     * 
     * @return <code>true</code> if the Mount is authenticated. 
     */
    boolean isAuthenticated();
    
    /**
     * Returns the roles that are allowed to access this Mount when {@link #isAuthenticated()} is true. If the Mount does not have any roles defined by itself, it
     * inherits them from the parent. If it defines roles, the roles from any ancestor are ignored. An empty set of roles
     * in combination with {@link #isAuthenticated()} return <code>true</code> means nobody has access to the item
     * 
     * @return The set of roles that are allowed to access this Mount. When no roles defined, the roles from the parent item are inherited. If none of the 
     * parent items have a role defined, an empty set is returned
     */
    Set<String> getRoles();  
    
    /**
     * Returns the users that are allowed to access this Mount when {@link #isAuthenticated()} is true. If the Mount does not have any users defined by itself, it
     * inherits them from the parent. If it defines users, the users from any ancestor are ignored. An empty set of users
     * in combination with {@link #isAuthenticated()} return <code>true</code> means nobody has access to the item
     * 
     * @return The set of users that are allowed to access this Mount. When no users defined, the users from the parent item are inherited. If none of the 
     * parent items have a user defined, an empty set is returned
     */
    Set<String> getUsers();  
    
    /**
     * Returns true if subject based jcr session should be used for this Mount 
     * @return
     */
    boolean isSubjectBasedSession();
    
    /**
     * Returns true if subject based jcr session should be statefully managed. 
     * @return
     */
    boolean isSessionStateful();
    
    /**
     * Returns FORM Login Page
     * @return <code>true</code> if the Mount is authenticated. 
     */
    String getFormLoginPage();
    
    /**
     * the string value of the property or <code>null</code> when the property is not present. When the property value is not of
     * type {@link String}, we'll return the {@link Object#toString()} value
     * @param name the name of the property
     * @return the value of the property or <code>null</code> when the property is not present
     */
    String getProperty(String name);
    
    /**
     * <p>
     * Returns all the properties that start with {@value #PROPERTY_NAME_MOUNT_PREFIX} and have value of type {@link String}. This map has as key the 
     * propertyname after {@value #PROPERTY_NAME_MOUNT_PREFIX}.
     * </p>
     * <p> 
     * <b>Note</b> The property called <code>hst:mountpoint</code> is excluded from this map, as it has a complete different purpose
     * </p>
     * @return all the mount properties and an empty map if there where no mount properties
     */
    Map<String, String> getMountProperties();
}
