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
package org.hippoecm.hst.proxy;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.apache.commons.proxy.Invoker;
import org.hippoecm.hst.service.ServiceBeanAccessProvider;
import org.hippoecm.hst.service.ServiceNamespace;

public class NamespacedBeanMethodInvoker implements Invoker, Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private ServiceBeanAccessProvider provider;
    private String defaultNamespacePrefix;
    
    public NamespacedBeanMethodInvoker(final ServiceBeanAccessProvider provider, String defaultNamespacePrefix) {
        this.provider = provider;
        this.defaultNamespacePrefix = defaultNamespacePrefix;
    }
    
    public Object invoke(Object proxy, Method method, Object [] args) throws Throwable {
        String namespacePrefix = getNamespacePrefix(method);
        String methodName = method.getName();
        Class [] paramTypes = method.getParameterTypes();
        Class returnType = method.getReturnType();
        
        if (methodName.startsWith("get") && paramTypes.length == 0) {
            String propName = getCamelString(methodName.substring(3));
            return provider.getProperty(namespacePrefix, propName, returnType, method);
        } else if (methodName.startsWith("is") && paramTypes.length == 0 && (returnType == boolean.class || returnType == Boolean.class)) {
            String propName = getCamelString(methodName.substring(2));
            return provider.getProperty(namespacePrefix, propName, returnType, method);
        } else if (methodName.startsWith("set") && paramTypes.length == 1) {
            String propName = getCamelString(methodName.substring(3));
            return provider.setProperty(namespacePrefix, propName, args[0], returnType, method);
        } else {
            return provider.invoke(namespacePrefix, methodName, args, returnType, method);
        }
    }
    
    private String getNamespacePrefix(Method method) {
        String namespacePrefix = this.defaultNamespacePrefix;
        
        if (method.isAnnotationPresent(ServiceNamespace.class)) {
            namespacePrefix = method.getAnnotation(ServiceNamespace.class).prefix();
        } else if (method.getDeclaringClass().isAnnotationPresent(ServiceNamespace.class)) {
            namespacePrefix = method.getDeclaringClass().getAnnotation(ServiceNamespace.class).prefix();
        }
        
        return namespacePrefix;
    }
    
    private static String getCamelString(String s) {
        char firstChar = s.charAt(0);
        
        if (Character.isUpperCase(firstChar)) {
            StringBuilder sb = new StringBuilder(s);
            sb.setCharAt(0, Character.toLowerCase(firstChar));
            s = sb.toString();
        }
        
        return s;
    }

}
