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
package org.hippoecm.hst.core.util;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

public class PropertyParser extends PropertyPlaceholderConfigurer {
    
    private final static Logger log = LoggerFactory.getLogger(PropertyParser.class);
    private Properties properties;
    
    public PropertyParser(Properties properties){
        super();
        this.properties = properties;
    }
    public Object resolveProperty(String name, Object o) {
        if(o == null || properties == null) {
            return o;
        }
        
        if(o instanceof String) {
            String s = (String)o;
            Set<String> exprSet = new HashSet<String>();
            // replace possible expressions
            try {
              s = this.parseStringValue((String)o, properties, exprSet );
            } catch (BeanDefinitionStoreException e) {
              if(log.isDebugEnabled()) {
                  log.debug("Unable to replace property expression for property '{}'. Return null : '{}'" ,name, e);
                  return null;
              } else if (log.isWarnEnabled()) {
                  log.warn("Unable to replace property expression for property '{}'. Return original value '{}'.",name, s);
              }
              
            }
            
            if(!exprSet.isEmpty()) {
              if (log.isDebugEnabled()) log.debug("Translated property value from '{}' --> '{}' for property '" +name + "'", o, s);   
            }
            return s;
        }
        if(o instanceof String[]) {
            // replace possible expressions in every String
            String[] unparsed = (String[])o;
            String[] parsed = new String[unparsed.length];
            Set<String> exprSet = new HashSet<String>();
            for(int i = 0 ; i < unparsed.length ; i++) {
                String s = unparsed[i];
                try {
                    s = this.parseStringValue(unparsed[i], properties, exprSet );
                } catch (BeanDefinitionStoreException e ) {
                    if(log.isDebugEnabled()) {
                        log.debug("Unable to replace property expression for property '{}'. Return null : '{}'.",name, e);
                        s = null;
                    } else if (log.isWarnEnabled()) {
                        log.warn("Unable to replace property expression for property '{}'. Return original value '{}'.",name, s);
                    }    
                }
                parsed[i] = s;
            }
            
            if (log.isDebugEnabled()) {
                if(!exprSet.isEmpty()) {
                    for(int i = 0; i < parsed.length; i++) {
                        log.debug("Translated property value from '{}' --> '{}' for property '"+name+"'", unparsed[i], parsed[i]);
                    }
                }
            }
            
            return parsed;
        }
        return o;
    }
    
}


