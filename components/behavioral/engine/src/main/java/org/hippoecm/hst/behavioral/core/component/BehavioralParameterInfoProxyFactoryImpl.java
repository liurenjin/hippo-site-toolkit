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
package org.hippoecm.hst.behavioral.core.component;

import org.hippoecm.hst.behavioral.BehavioralProfile;
import org.hippoecm.hst.behavioral.util.BehavioralUtils;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.component.HstParameterInfoInvocationHandler;
import org.hippoecm.hst.core.component.HstParameterInfoProxyFactory;
import org.hippoecm.hst.core.component.HstParameterInfoProxyFactoryImpl;
import org.hippoecm.hst.core.component.HstParameterValueConverter;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.request.ComponentConfiguration;

/**
 * The BehavioralParameterInfoProxyFactoryImpl extends the HstParameterInfoProxyFactoryImpl by returning a different parameterInfoInvocationHandler : namely
 * one that first checks if there is a prefixed parameter name available before checking the default parameter name. 
 * 
 * For example, if the current persona is 'developer' , this invocation handler first tries to fetch a parametername that is
 * prefixed by 'developer' : If that one is present, that parametervalue is returned. Otherwise, a fallback to the default non prefixed parametername is done
 *
 */
public class BehavioralParameterInfoProxyFactoryImpl extends HstParameterInfoProxyFactoryImpl implements HstParameterInfoProxyFactory {
    
    @Override
    protected HstParameterInfoInvocationHandler createHstParameterInfoInvocationHandler(
            ComponentConfiguration componentConfig, HstRequest request,
            HstParameterValueConverter parameterValueConverter) {
        
        HstParameterInfoInvocationHandler parameterInfoInvocationHandler = new ParameterInfoInvocationHandler(componentConfig, request, parameterValueConverter) {

            @Override
            public String getParameterValue(final String parameterName, ComponentConfiguration config, HstRequest req) {
                
                String prefixedParameterName = parameterName;
                
                BehavioralProfile profile = BehavioralUtils.getBehavioralProfile(req);

                if (profile != null && profile.hasPersona()) {
                    for (String name : config.getParameterNames()) {
                        int offset = name.indexOf(HstComponentConfiguration.PARAMETER_PREFIX_NAME_DELIMITER);
                        if (offset != -1) {
                            String id = name.substring(0, offset);
                            if (profile.isPersona(id)) {
                                prefixedParameterName = name;
                                break;
                            }
                        }
                    }
                }

                return config.getParameter(prefixedParameterName, req.getRequestContext().getResolvedSiteMapItem()); 
            }
        };
        
        return parameterInfoInvocationHandler;
    }

    
    
}
