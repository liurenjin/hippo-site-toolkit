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
package org.hippoecm.hst.core.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstRequestImpl;
import org.hippoecm.hst.core.container.HstComponentWindow;
import org.hippoecm.hst.diagnosis.HDC;
import org.hippoecm.hst.diagnosis.Task;
import org.hippoecm.hst.logging.LoggerFactory;

public class HstComponentInvokerProfiler {

    public HstComponentInvokerProfiler() {
    }

    /**
     * @deprecated it does not need loggerFactory argument any more
     */
    public HstComponentInvokerProfiler(LoggerFactory loggerFactory) {
    }

    public Object profile(ProceedingJoinPoint call) throws Throwable {
        if (HDC.isStarted()) {
            Object [] args = call.getArgs();
            String method = call.toShortString();
            String windowName = "";
            String refNamespace = "";

            Task profileTask = HDC.getCurrentTask().startSubtask(HstComponentInvokerProfiler.class.getSimpleName());
            profileTask.setAttribute("method", method);

            try {
                if (args.length > 1 && args[1] instanceof HstRequest) {
                    HstRequest hstRequest = (HstRequest) args[1];
                    HstComponentWindow window = ((HstRequestImpl) hstRequest).getComponentWindow();
                    windowName = window.getName();
                    profileTask.setAttribute("window", windowName);
                    profileTask.setAttribute("component", window.getComponentName());
                    refNamespace = hstRequest.getReferenceNamespace();
                    if ("".equals(refNamespace)) {
                        refNamespace = "root";
                    }
                    profileTask.setAttribute("ref", refNamespace);
                }

                return call.proceed();
            } finally {
                profileTask.stop();
            }
        } else {
            return call.proceed();
        }
    }
    
}
