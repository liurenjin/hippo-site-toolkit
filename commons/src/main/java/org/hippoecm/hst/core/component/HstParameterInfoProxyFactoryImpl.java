/*
 *  Copyright 2011 Hippo.
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

import java.beans.PropertyEditor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.hippoecm.hst.core.parameters.EmptyPropertyEditor;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.ComponentConfiguration;

public class HstParameterInfoProxyFactoryImpl implements HstParameterInfoProxyFactory {

    @Override
    public <T> T createParameterInfoProxy(ParametersInfo parametersInfo, ComponentConfiguration componentConfig,
            HstRequest request, HstParameterValueConverter parameterValueConverter) {

        Class<?> parametersInfoType = parametersInfo.type();

        if (!parametersInfoType.isInterface()) {
            throw new IllegalArgumentException("The ParametersInfo annotation type must be an interface.");
        }
        if(parameterValueConverter == null) {
            throw new IllegalArgumentException("The HstParameterValueConverter is not allowed to be null");
        }

        InvocationHandler parameterInfoHandler =  createHstParameterInfoInvocationHandler(componentConfig, request,
                parameterValueConverter);
        T parametersInfoInterface = (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class[] { parametersInfoType }, parameterInfoHandler);
        return parametersInfoInterface;
    }
    
    public HstParameterInfoInvocationHandler createHstParameterInfoInvocationHandler(final ComponentConfiguration componentConfig,final HstRequest request,final HstParameterValueConverter parameterValueConverter) {
        return new ParameterInfoInvocationHandler(componentConfig, request,
                parameterValueConverter);
    }

    public static class ParameterInfoInvocationHandler implements HstParameterInfoInvocationHandler {

        private final ComponentConfiguration componentConfig;
        private final HstRequest request;
        private final HstParameterValueConverter parameterValueConverter;

        public ParameterInfoInvocationHandler(final ComponentConfiguration componentConfig,final HstRequest request,final HstParameterValueConverter parameterValueConverter) {
            this.componentConfig = componentConfig;
            this.request = request;
            this.parameterValueConverter = parameterValueConverter;
        }

        @Override
        public Object invoke(Object object, Method method, Object[] args) throws Throwable {
            if (isSetter(method, args)) {
                throw new UnsupportedOperationException("Setter method (" + method.getName() + ") is not supported.");
            }
 
            if (!isGetter(method, args)) {
                return null;
            }

            Parameter parameterAnnotation = method.getAnnotation(Parameter.class);
            if (parameterAnnotation == null) {
                throw new IllegalArgumentException("Component " + componentConfig.getCanonicalPath() + " uses ParametersInfo annotation, but "
                        + method.getDeclaringClass().getSimpleName() + "#" + method.getName() + " is not annotated with " + Parameter.class.getName());
            }

            String parameterName = parameterAnnotation.name();
            if (parameterName == null || "".equals(parameterName)) {
                throw new IllegalArgumentException("The parameter name is empty.");
            }

            String parameterValue = getParameterValue(parameterName, componentConfig, request);
            
            if (parameterValue == null || "".equals(parameterValue)) {
                // when the parameter value is null or an empty string we return the default value from the annotation
                parameterValue = parameterAnnotation.defaultValue();
            }
            if (parameterValue == null) {
                return null;
            }

            Class<? extends PropertyEditor> customEditorType = parameterAnnotation.customEditor();
            Class<?> returnType = method.getReturnType();

            if (customEditorType == null || customEditorType == EmptyPropertyEditor.class) {
                return parameterValueConverter.convert(parameterValue, returnType);
            } else {
                PropertyEditor customEditor = customEditorType.newInstance();
                customEditor.setAsText(parameterValue);
                return customEditor.getValue();
            }
        }

        public String getParameterValue (final String parameterName, final ComponentConfiguration config, final HstRequest req) {
            return config.getParameter(parameterName, req.getRequestContext().getResolvedSiteMapItem());
        }
    }
    
    public static final boolean isGetter(final Method method, final Object[] args) {
        if (args == null || args.length == 0) {
            final String methodName = method.getName();
            return methodName.startsWith("get") || methodName.startsWith("is");
        }
        return false;
    }

    public static final boolean isSetter(final Method method, final Object[] args) {
        return (args != null && args.length == 1) && method.getName().startsWith("set");
    }


}
