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
package org.hippoecm.hst.core.component;

import java.security.AccessController;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.collections.EnumerationUtils;
import org.apache.commons.collections.collection.CompositeCollection;
import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.HstComponentWindow;
import org.hippoecm.hst.core.request.HstRequestContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HstRequestImpl
 * 
 * @version $Id$
 */
public class HstRequestImpl extends HttpServletRequestWrapper implements HstRequest {

    private static Logger log = LoggerFactory.getLogger(HstRequestImpl.class);
    
    public static final String CONTAINER_ATTR_NAME_PREFIXES_PROP_KEY = HstRequest.class.getName() + ".containerAttributeNamePrefixes"; 
    
    public static final String CONTAINER_USER_PRINCIPAL_CLASSNAME_PROP_KEY = HstRequest.class.getName() + ".userPrincipalClassName";

    public static final String CONTAINER_ROLE_PRINCIPAL_CLASSNAME_PROP_KEY = HstRequest.class.getName() + ".rolePrincipalClassName";

    private static String [] CONTAINER_ATTR_NAME_PREFIXES = null;
    
    private static Class<? extends Principal> USER_PRINCIPAL_CLASS = null;

    private static Class<? extends Principal> ROLE_PRINCIPAL_CLASS = null;
    
    protected String lifecyclePhase;
    protected HstRequestContext requestContext;
    protected Map<String, Map<String, String []>> namespaceParametersMap = new HashMap<String, Map<String, String []>>();
    protected Map<String, Map<String, Object>> namespaceAttributesMap = new HashMap<String, Map<String, Object>>();
    protected HstComponentWindow componentWindow;
    protected String parameterNameComponentSeparator;
    protected boolean referenceNamespaceIgnored;
    
    public HstRequestImpl(HttpServletRequest servletRequest, HstRequestContext requestContext, HstComponentWindow componentWindow, String lifecyclePhase) {
        super(servletRequest);
        this.lifecyclePhase = lifecyclePhase;
        this.requestContext = requestContext;
        this.referenceNamespaceIgnored = this.requestContext.getURLFactory().isReferenceNamespaceIgnored();
        this.componentWindow = componentWindow;
        this.parameterNameComponentSeparator = requestContext.getContainerURLProvider().getParameterNameComponentSeparator();
    }
    
    public void setRequest(HttpServletRequest servletRequest) {
        super.setRequest(servletRequest);
    }

    public Map<String, String []> getParameterMap() {
        String referenceNamespace = this.componentWindow.getReferenceNamespace();
        return getParameterMap(referenceNamespaceIgnored ? "" : referenceNamespace);
    }
    
    public Map<String, String []> getParameterMap(String referencePath) {
        Map<String, String []> parameterMap = null;
        
        String namespace = getReferenceNamespacePath(referencePath);
        String prefix = getFullNamespacePrefix(namespace);
        
        if (namespaceParametersMap.isEmpty()) {
            
            if (referenceNamespaceIgnored) {
                
                namespaceParametersMap.put("", new HashMap<String, String[]>((Map<String,String[]>)super.getParameterMap()));
                
            } else {
                
                boolean isRenderPhase = HstRequest.RENDER_PHASE.equals(lifecyclePhase);
                String targetComponentReferencePrefix = (isRenderPhase ? "" : getFullNamespacePrefix(getReferenceNamespace()));
                boolean emptyTargetComponentReferencePrefix = "".equals(targetComponentReferencePrefix);
                boolean namespacedParameter = false;
                
                for (Enumeration paramNames = super.getParameterNames(); paramNames.hasMoreElements(); ) {
                    String paramName = (String) paramNames.nextElement();
                    String prefixKey = null;
                    String paramKey = null;
                    
                    int index = paramName.indexOf(parameterNameComponentSeparator);
                    namespacedParameter = (index != -1 && index < paramName.length() - 1);
                    
                    if (namespacedParameter) {
                        prefixKey = paramName.substring(0, index + 1);
                        paramKey = paramName.substring(index + 1);
                    } else {
                        prefixKey = targetComponentReferencePrefix;
                        paramKey = paramName;
                    }
                    
                    parameterMap = namespaceParametersMap.get(prefixKey);
                    if (parameterMap == null ) {
                        parameterMap = new HashMap<String, String[]>();
                        namespaceParametersMap.put(prefixKey, parameterMap);
                    }
                    
                    String [] paramValues = super.getParameterValues(paramName);
                    
                    // if query parameter with namespace prefix exists and form parameter exists with same name,
                    // the query parameter must be kept, not overwriting it by the form parameter.
                    if (!parameterMap.containsKey(paramKey)) {
                        parameterMap.put(paramKey, paramValues);
                    }
                    
                    // if it is not render phase and the target component ref prefix is not an empty one,
                    // then put the parameter into the empty prefix map also to allow clients to retrieve
                    // the *public request parameter*. 
                    if (!namespacedParameter && !isRenderPhase && !emptyTargetComponentReferencePrefix) {
                        parameterMap = namespaceParametersMap.get("");
                        if (parameterMap == null ) {
                            parameterMap = new HashMap<String, String[]>();
                            namespaceParametersMap.put("", parameterMap);
                        }
                        
                        // if query parameter with namespace prefix exists and form parameter exists with same name,
                        // the query parameter must be kept, not overwriting it by the form parameter.
                        if (!parameterMap.containsKey(paramKey)) {
                            parameterMap.put(paramKey, paramValues);
                        }
                    }
                }
                
            }
        }
        
        parameterMap = namespaceParametersMap.get(prefix);
        if (parameterMap == null ) {
            parameterMap = Collections.emptyMap();
        }
        
        return parameterMap;
    }
    
    public Map<String, Object> getAttributeMap() {
        String referenceNamespace = this.componentWindow.getReferenceNamespace();
        return getAttributeMap(referenceNamespace);
    }
    
    public String getReferenceNamespace() {
        return this.componentWindow.getReferenceNamespace();
    }
    
    public Map<String, Object> getAttributeMap(String referencePath) {
        String namespace = getReferenceNamespacePath(referencePath);
        String prefix = getFullNamespacePrefix(namespace);
        int prefixLen = prefix.length();
        Map<String, Object> attributesMap = this.namespaceAttributesMap.get(prefix);
        
        if (attributesMap == null) {
            attributesMap = new HashMap<String, Object>();
            
            for (Enumeration attributeNames = super.getAttributeNames(); attributeNames.hasMoreElements(); ) {
                String encodedAttributeName = (String) attributeNames.nextElement();
                
                if (encodedAttributeName.startsWith(prefix)) {
                    String attributeName = encodedAttributeName.substring(prefixLen);
                    Object attributeValue = super.getAttribute(encodedAttributeName);
                    attributesMap.put(attributeName, attributeValue);
                }
            }
            
            this.namespaceAttributesMap.put(prefix, attributesMap);
        }
        
        return attributesMap;
    }

    @Override
    public String getParameter(String name) {
        if (name == null) {
            throw new IllegalArgumentException("parameter name cannot be null.");
        }
        
        Object value = getParameterMap().get(name);

        if (value == null) {
            return null;
        } else if (value instanceof String[]) {
            return (((String[]) value)[0]);
        } else if (value instanceof String) {
            return ((String) value);
        } else {
            return (value.toString());
        }
    }

    @Override
    public Enumeration getParameterNames() {
        return Collections.enumeration(this.getParameterMap().keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        if (name == null) {
            throw new IllegalArgumentException("parameter name cannot be null.");
        }
        
        Object value = getParameterMap().get(name);
        
        if (value == null) {
            return null;
        } else if (value instanceof String[]) {
            return ((String[]) value);
        } else if (value instanceof String) {
            return new String [] { (String) value };
        } else {
            return new String [] { value.toString() };
        }
    }

    @Override
    public Enumeration getAttributeNames() {
        List servletRequestAttrs = EnumerationUtils.toList(super.getAttributeNames());
        Set localRequestAttrs = this.getAttributeMap().keySet();
        Collection composite = new CompositeCollection(new Collection [] { servletRequestAttrs, localRequestAttrs });
        return Collections.enumeration(composite);
    }
    
    @Override
    public Object getAttribute(String name) {
        if (name == null) {
            throw new IllegalArgumentException("attribute name cannot be null.");
        }
        
        Object value = null;
        
        if (isContainerAttributeName(name)) {
            value = super.getAttribute(name);
        } else {
            value = getAttributeMap().get(name);
            
            if (value == null) {
                String prefix = getFullNamespacePrefix(this.componentWindow.getReferenceNamespace(), false);
                
                value = super.getAttribute(prefix + name);
                
                if (value == null) {
                    value = super.getAttribute(name);
                }
            }
        }
        
        return value;
    }

    @Override
    public void setAttribute(String name, Object value) {
        if (name == null) {
            throw new IllegalArgumentException("attribute name cannot be null.");
        }
        
        if (value == null) {
            removeAttribute(name);
        } else if (isContainerAttributeName(name)) {
            super.setAttribute(name, value);
        } else {
            getAttributeMap().put(name, value);
            String prefix = getFullNamespacePrefix(this.componentWindow.getReferenceNamespace(), false);
            super.setAttribute(prefix + name, value);
        }
    }

    @Override
    public void removeAttribute(String name) {
        if (name == null) {
            throw new IllegalArgumentException("attribute name cannot be null.");
        }
        
        if (isContainerAttributeName(name)) {
            super.removeAttribute(name);
        } else {
            Object value = getAttributeMap().remove(name);
            
            String prefix = getFullNamespacePrefix(this.componentWindow.getReferenceNamespace(), false);
            super.removeAttribute(prefix + name);
        }
    }
    
    public HstRequestContext getRequestContext() {
        return (HstRequestContext) super.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
    }

    public HstComponentWindow getComponentWindow() {
        return this.componentWindow;
    }
    
    public String getResourceID() {
        return this.requestContext.getBaseURL().getResourceId();
    }

    protected String getReferenceNamespacePath(String referencePath) {
        return referencePath;
    }
    
    protected String getFullNamespacePrefix(String referenceNamespace) {
        return getFullNamespacePrefix(referenceNamespace, true);
    }

    protected String getFullNamespacePrefix(String referenceNamespace, boolean noSeparatorForEmpty) {
        if (referenceNamespace == null || "".equals(referenceNamespace)) {
            return (noSeparatorForEmpty ? "" : this.parameterNameComponentSeparator);
        }
        
        String prefix = referenceNamespace + this.parameterNameComponentSeparator;
        return prefix;
    }

    protected boolean isContainerAttributeName(String attrName) {
        boolean containerAttrName = false;
        
        if (CONTAINER_ATTR_NAME_PREFIXES == null) {
            synchronized (HstRequestImpl.class) {
                if (CONTAINER_ATTR_NAME_PREFIXES == null) {
                    ArrayList<String> containerAttrNamePrefixes = new ArrayList<String>(Arrays.asList("javax.servlet.", "javax.portlet.", "org.hippoecm.hst.container."));
                    ContainerConfiguration containerConfiguration = this.requestContext.getContainerConfiguration();
                    
                    if (containerConfiguration != null) {
                        containerAttrNamePrefixes.addAll(this.requestContext.getContainerConfiguration().getList(CONTAINER_ATTR_NAME_PREFIXES_PROP_KEY));
                    }
                    
                    CONTAINER_ATTR_NAME_PREFIXES = (String []) containerAttrNamePrefixes.toArray(new String[0]);
                }
            }
        }
        
        for (String prefix : CONTAINER_ATTR_NAME_PREFIXES) {
            if (attrName.startsWith(prefix)) {
                containerAttrName = true;
                break;
            }
        }
        
        return containerAttrName;
    }

    public String getLifecyclePhase() {
        return this.lifecyclePhase;
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServletRequestWrapper#getUserPrincipal()
     */
    public Principal getUserPrincipal() {
        // initialize configuration of user principal class
        initUserPrincipalClass(requestContext);

        // check for user principal in current thread's user subject
        boolean [] subjectFound = new boolean[1];
        Principal principal = getSubjectUserPrincipal(subjectFound);
        if (subjectFound[0]) {
            return principal;
        }

        // return existing principal for request if any
        return super.getUserPrincipal();
    }
    
    @SuppressWarnings("unchecked")
    public static void initUserPrincipalClass(HstRequestContext requestContext) {
        if (USER_PRINCIPAL_CLASS == null) {
            synchronized (HstRequestImpl.class) {
                if (USER_PRINCIPAL_CLASS == null) {
                    Class<? extends Principal> userPrincipalClass = Principal.class;
                    if (requestContext != null) {
                        ContainerConfiguration containerConfiguration = requestContext.getContainerConfiguration();
                        if (containerConfiguration != null) {
                            String userPrincipalClassName = containerConfiguration.getString(CONTAINER_USER_PRINCIPAL_CLASSNAME_PROP_KEY);
                            if (userPrincipalClassName != null) {
                                try {
                                    userPrincipalClass = (Class<? extends Principal>)Class.forName(userPrincipalClassName);
                                } catch (ClassNotFoundException cnfe) {
                                    log.error("Container configuration property "+CONTAINER_USER_PRINCIPAL_CLASSNAME_PROP_KEY+" invalid: "+cnfe, cnfe);
                                }
                            }
                        }
                    }
                    USER_PRINCIPAL_CLASS = userPrincipalClass;        
                }
            }
        }
    }
    
    public static Principal getSubjectUserPrincipal(boolean [] subjectFound) {
        if ((USER_PRINCIPAL_CLASS != null) && (USER_PRINCIPAL_CLASS != Principal.class)) {
            Subject userSubject = Subject.getSubject(AccessController.getContext());
            if (userSubject != null) {
                if (subjectFound != null) {
                    subjectFound[0] = true;
                }
                Set<? extends Principal> userPrincipals = userSubject.getPrincipals(USER_PRINCIPAL_CLASS);
                if (!userPrincipals.isEmpty()) {
                    Principal userPrincipal = userPrincipals.iterator().next();
                    return userPrincipal;
                }
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServletRequestWrapper#isUserInRole(java.lang.String)
     */
    public boolean isUserInRole(String role) {
        // initialize configuration of role principal class
        initRolePrincipalClass(requestContext);

        // check for role principals in current thread's user subject
        boolean [] subjectFound = new boolean[1];
        boolean userInRole = isSubjectUserInRole(role, subjectFound);
        if (subjectFound[0]) {
            return userInRole;
        }
        
        // return existing role tests if available
        return super.isUserInRole(role);
    }
    
    @SuppressWarnings("unchecked")
    public static void initRolePrincipalClass(HstRequestContext requestContext) {
        if (ROLE_PRINCIPAL_CLASS == null) {
            synchronized (HstRequestImpl.class) {
                if (ROLE_PRINCIPAL_CLASS == null) {
                    Class<? extends Principal> rolePrincipalClass = Principal.class;
                    if (requestContext != null) {
                        ContainerConfiguration containerConfiguration = requestContext.getContainerConfiguration();
                        if (containerConfiguration != null) {
                            String rolePrincipalClassName = containerConfiguration.getString(CONTAINER_ROLE_PRINCIPAL_CLASSNAME_PROP_KEY);
                            if (rolePrincipalClassName != null) {
                                try {
                                    rolePrincipalClass = (Class<? extends Principal>)Class.forName(rolePrincipalClassName);
                                } catch (ClassNotFoundException cnfe) {
                                    log.error("Container configuration property "+CONTAINER_ROLE_PRINCIPAL_CLASSNAME_PROP_KEY+" invalid: "+cnfe, cnfe);
                                }
                            }
                        }
                    }
                    ROLE_PRINCIPAL_CLASS = rolePrincipalClass;
                }
            }
        }
    }
    
    public static boolean isSubjectUserInRole(String role, boolean [] subjectFound) {
        if ((ROLE_PRINCIPAL_CLASS != null) && (ROLE_PRINCIPAL_CLASS != Principal.class)) {
            Subject userSubject = Subject.getSubject(AccessController.getContext());
            if (userSubject != null) {
                if (subjectFound != null) {
                    subjectFound[0] = true;
                }
                Set<? extends Principal> rolePrincipals = userSubject.getPrincipals(ROLE_PRINCIPAL_CLASS);
                for (Principal rolePrincipal : rolePrincipals) {
                    if (rolePrincipal.getName().equalsIgnoreCase(role)) {
                        return true;
                    }
                }
            }
        }
        return false;        
    }
}
