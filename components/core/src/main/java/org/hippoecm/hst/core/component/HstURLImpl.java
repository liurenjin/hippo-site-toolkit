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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.container.HstContainerURLProvider;

public class HstURLImpl implements HstURL {
    
    protected String type = RENDER_TYPE;
    protected String referenceNamespace;
    protected HstContainerURL baseContainerURL;
    protected Map<String, String[]> parameterMap = new HashMap<String, String[]>();
    protected String resourceID;
    protected HstContainerURLProvider urlProvider;
    
    public HstURLImpl(String type, HstContainerURLProvider urlProvider) {
        this.type = type;
        this.urlProvider = urlProvider;
    }

    public Map<String, String[]> getParameterMap() {
        return this.parameterMap;
    }

    public String getType() {
        return this.type;
    }

    public String getReferenceNamespace() {
        return this.referenceNamespace;
    }
    
    public HstContainerURL getBaseContainerURL() {
        return this.baseContainerURL;
    }

    public void setParameter(String name, String value) {
        setParameter(name, value != null ? new String [] { value } : (String []) null);
    }

    public void setParameter(String name, String[] values) {
        this.parameterMap.put(name, values);
    }

    public void setParameters(Map<String, String[]> parameters) {
        for (Map.Entry<String, String []> entry : parameters.entrySet()) {
            setParameter(entry.getKey(), entry.getValue());
        }
    }

    public void setResourceID(String resourceID) {
        this.resourceID = resourceID;
    }
    
    public String getResourceID() {
        return this.resourceID;
    }

    public void setReferenceNamespace(String referenceNamespace) {
        this.referenceNamespace = referenceNamespace;
    }
    
    public void setBaseContainerURL(HstContainerURL baseContainerURL) {
        this.baseContainerURL = baseContainerURL;
    }
    
    public void write(Writer out) throws IOException {
        out.write(toString());
    }

    public void write(Writer out, boolean escapeXML) throws IOException {
        write(out);
    }
    
    public String toString() {
        HstContainerURL containerURL = this.urlProvider.createURL(this.baseContainerURL, this);
        
        try {
            return this.urlProvider.toURLString(containerURL);
        } catch (UnsupportedEncodingException e) {
            throw new HstComponentException(e);
        } catch (ContainerException e) {
            throw new HstComponentException(e);
        }
    }

}
