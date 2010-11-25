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
package org.hippoecm.hst.servlet;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.InputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.util.HstRequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StaticResourceServlet extends HttpServlet {
    
    private static final long serialVersionUID = 1L;

    static Logger log = LoggerFactory.getLogger(StaticResourceServlet.class);

    private static final int BUF_SIZE = 4096;
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        String path = getResourcePath(request);
        
        ServletContext context = getServletConfig().getServletContext();
        
        InputStream is = null;
        BufferedInputStream bis = null;
        ServletOutputStream sos = null;
        BufferedOutputStream bos = null;
        
        try {
            if (path == null) {
                if (log.isWarnEnabled()) {
                    log.warn("path is null, response status = 404)");
                }
                
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            long lastModified = 0L;
            
            File file = new File(context.getRealPath(path));
            
            if (file.isFile()) {
                lastModified = file.lastModified();
            }
            
            if (lastModified > 0L) {
                response.setDateHeader("Last-Modified", lastModified);
                long expires = (System.currentTimeMillis() - lastModified);
                response.setDateHeader("Expires", expires + System.currentTimeMillis());
                response.setHeader("Cache-Control", "max-age=" + (expires / 1000));
            }
            
            String mimeType = context.getMimeType(path);
            
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            
            response.setContentType(mimeType);
            
            is = context.getResourceAsStream(path);
            bis = new BufferedInputStream(is);
            sos = response.getOutputStream();
            bos = new BufferedOutputStream(sos);
            
            byte [] buffer = new byte[BUF_SIZE];
            
            int readLen = bis.read(buffer, 0, BUF_SIZE);
            
            while (readLen != -1) {
                bos.write(buffer, 0, readLen);
                readLen = bis.read(buffer, 0, BUF_SIZE);
            }
            
            bos.flush();
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                log.warn("Exception during writing content of {}: {}", path, e.toString());
            }
        } finally {
            if (bos != null) try { bos.close(); } catch (Exception ce) { }
            if (sos != null) try { sos.close(); } catch (Exception ce) { }
            if (bis != null) try { bis.close(); } catch (Exception ce) { }
            if (is != null) try { is.close(); } catch (Exception ce) { }
        }
    }
    
    private String getResourcePath(HttpServletRequest request) {
        String path = null;
        
        // if hstRequest is retrieved, then this servlet has been dispatched by hst component.
        HstRequest hstRequest = HstRequestUtils.getHstRequest(request);

        if (hstRequest != null) {
            path = hstRequest.getResourceID();
        }
        
        if (path == null) {
            path = HstRequestUtils.getPathInfo(request);
        }

        if (path != null && !path.startsWith("/") && path.indexOf(':') > 0) {
            path = path.substring(path.indexOf(':') + 1);
        }
        
        return path;
    }
    
}
