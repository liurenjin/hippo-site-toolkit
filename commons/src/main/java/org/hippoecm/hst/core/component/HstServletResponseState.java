/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.util.HstRequestUtils;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Temporarily holds the current state of a HST response
 */
public class HstServletResponseState extends AbstractHstResponseState
{
    protected HttpServletResponse parentResponse;
    
    public HstServletResponseState(HttpServletRequest request, HttpServletResponse parentResponse) {
        super(request, parentResponse);
        this.parentResponse = parentResponse;
        
        HstRequestContext requestContext = HstRequestUtils.getHstRequestContext(request);
        
        isActionResponse = (requestContext.getBaseURL().getActionWindowReferenceNamespace() != null);
        isResourceResponse = (requestContext.getBaseURL().getResourceWindowReferenceNamespace() != null);
        isRenderResponse = (!this.isActionResponse && !this.isResourceResponse);
        
        isStateAwareResponse = isActionResponse;
        isMimeResponse = isRenderResponse || isResourceResponse;
    }

    public Element createElement(String tagName) {
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
        
        try
        {
            docBuilder = dbfac.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            return doc.createElement(tagName);
        }
        catch (ParserConfigurationException e)
        {
            throw new DOMException((short) 0, "Initialization failure");
        }
    }

    protected void setResponseLocale(Locale locale) {
        this.parentResponse.setLocale(locale);
    }
    
    protected void addResponseCookie(Cookie cookie) {
        this.parentResponse.addCookie(cookie);
    }
    
    protected void setResponseCharacterEncoding(String characterEncoding) {
        this.parentResponse.setCharacterEncoding(characterEncoding);
    }
    
    protected void setResponseContentType(String contentType) {
        this.parentResponse.setContentType(contentType);
    }
    
    protected void addResponseHeader(String name, String value) {
        this.parentResponse.addHeader(name, value);
    }
    
    protected void setResponseHeader(String name, String value) {
        this.parentResponse.setHeader(name, value);
    }
    
    protected void addResponseHeadElement(Element element, String keyHint) {
        if (this.parentResponse instanceof HstResponse) {
            ((HstResponse) this.parentResponse).addHeadElement(element, keyHint);
        }
    }
    
    protected void setResponseStatus(int status) {
        this.parentResponse.setStatus(status);
    }
    
    protected void setResponseContentLength(int len) {
        this.parentResponse.setContentLength(len);
    }
    
    protected OutputStream getResponseOutputStream() throws IOException {
        return this.parentResponse.getOutputStream();
    }
    
    protected PrintWriter getResponseWriter() throws IOException {
        return this.parentResponse.getWriter();
    }

}
