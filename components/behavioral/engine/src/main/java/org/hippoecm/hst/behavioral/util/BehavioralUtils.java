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
package org.hippoecm.hst.behavioral.util;

import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.behavioral.BehavioralProfile;
import org.hippoecm.hst.behavioral.BehavioralService;


public class BehavioralUtils {

    public static BehavioralService getBehavioralService() {
        BehavioralService bs = (BehavioralService)HstServices.getComponentManager().getComponent(BehavioralService.class.getName());
        return bs;
    }
    
    public static BehavioralProfile getBehavioralProfile(HttpServletRequest request) {
         return getBehavioralService().getBehavioralProfile(request);
    }
    
}
