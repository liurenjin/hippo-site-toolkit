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
package org.onehippo.cms7.hst.ga.tags;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.googleanalytics.GoogleAnalyticsService;

public class GoogleAnalyticsAccountIdTag extends TagSupport {

    private static final long serialVersionUID = 1L;

    @Override
    public int doStartTag() throws JspException {

        GoogleAnalyticsService service = HippoServiceRegistry.getService(GoogleAnalyticsService.class);

        if (service != null) {
            JspWriter writer = pageContext.getOut();
            try {
                writer.write("<script type=\"text/javascript\">\n");
                writer.write("  Hippo_Ga_AccountId='" + service.getAccountId() + "';\n");
                writer.write("</script>\n");
            }
            catch (IOException e) {
                throw new JspException("IOException while trying to write script tag", e);
            }
        }

        return SKIP_BODY;
    }

    
}