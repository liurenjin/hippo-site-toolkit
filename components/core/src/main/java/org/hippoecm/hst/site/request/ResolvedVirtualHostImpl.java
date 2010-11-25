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
package org.hippoecm.hst.site.request;

import org.hippoecm.hst.configuration.hosting.MatchException;
import org.hippoecm.hst.configuration.hosting.PortMount;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedVirtualHost;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ResolvedVirtualHostImpl
 * @version $Id$
 */
public class ResolvedVirtualHostImpl implements ResolvedVirtualHost{

    private final static Logger log = LoggerFactory.getLogger(ResolvedVirtualHostImpl.class);
    
    private VirtualHost virtualHost;
    
    private String hostName;
    private int portNumber;
    
    public ResolvedVirtualHostImpl(VirtualHost virtualHost, String hostName, int portNumber) {
        this.virtualHost = virtualHost;
        this.hostName = hostName;
        this.portNumber = portNumber;
    }

    public VirtualHost getVirtualHost() {
        return virtualHost;
    }

    public ResolvedMount matchMount(String contextPath, String requestPath) throws MatchException {
        PortMount portMount = virtualHost.getPortMount(portNumber);
        if(portMount == null && portNumber != 0) {
            log.debug("Could not match the request to port '{}'. If there is a default port '0', we'll try this one");
            portMount = virtualHost.getPortMount(0);
            if(portMount == null) {
                log.warn("Virtual Host '{}' is not (correctly) mounted for portnumber '{}': We cannot return a ResolvedMount. Return null", virtualHost.getHostName(), String.valueOf(portNumber));
                return null;
            }
        }
        if(portMount == null) {
            log.warn("Cannot match virtual Host '{}' for portnumber '{}': We cannot return a ResolvedMount. Return null", virtualHost.getHostName(), String.valueOf(portNumber)); 
            return null;
        }
        
        if(portMount.getRootMount() == null) {
            log.warn("Virtual Host '{}' for portnumber '{}' is not (correctly) mounted: We cannot return a ResolvedMount. Return null", virtualHost.getHostName(), String.valueOf(portNumber)); 
            return null;
        }
        
        // strip leading and trailing slashes
        String path = PathUtils.normalizePath(requestPath);
        String[] requestPathSegments = path.split("/");
        int position = 0;
        Mount mount = portMount.getRootMount();
        
        while(position < requestPathSegments.length) {
            if(mount.getChildMount(requestPathSegments[position]) != null) {
                mount = mount.getChildMount(requestPathSegments[position]);
            } else {
                // we're done: we have the deepest Mount
                break;
            }
            position++;
        }
        
        // ensure "valid" matching ROOT contextPath which could be derived as "" -> turn it into "/"
        if (contextPath != null && contextPath.length() == 0) {
        	contextPath = "/";
        }
        
        // let's find a Mount that has a valid 'onlyForContextPath' : if onlyForContextPath is not null && not equal to the contextPath, we need to try the parent Mount until we have a valid one or have a Mount that is null
        while(mount != null && contextPath != null && (mount.onlyForContextPath() != null && !mount.onlyForContextPath().equals(contextPath) )) {
            log.debug("Mount '{}' cannot be used because the contextPath '{}' is not valid for this Mount, because it is only for context path. Let's try parent Mount's if present.'"+mount.onlyForContextPath()+"' ", mount.getName(), contextPath);
            mount = mount.getParent();
        }
        
        if(mount == null) {
            log.warn("Virtual Host '{}' is not (correctly) mounted for portnumber '{}': We cannot return a ResolvedMount. Return null", virtualHost.getHostName(), String.valueOf(portMount.getPortNumber()));
            return null;
        }
        
        
        // reconstruct the prefix that needs to be stripped of from the request because it belongs to the Mount
        // we thus create the resolvedPathInfoPrefix
        StringBuilder builder = new StringBuilder();
        while(position > 0) {
            builder.insert(0,requestPathSegments[--position]).insert(0,"/");
           
        }
        String resolvedMountPath = builder.toString();
        
        ResolvedMount resolvedMount = new ResolvedMountImpl(mount, this , resolvedMountPath);
        log.debug("Found ResolvedMount is '{}' and the mount prefix for it is :", resolvedMount.getResolvedMountPath());
        
        return resolvedMount;
    }

    public String getResolvedHostName() {
        return hostName;
    }
    
    public int getPortNumber() {
    	return portNumber;
    }

}
