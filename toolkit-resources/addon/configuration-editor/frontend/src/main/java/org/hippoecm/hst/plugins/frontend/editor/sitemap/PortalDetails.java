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

package org.hippoecm.hst.plugins.frontend.editor.sitemap;

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

public class PortalDetails extends Panel {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public PortalDetails(String id, IModel choices) {
        super(id);

        final DropDownChoice ddo = new DropDownChoice("portlet", choices, new IChoiceRenderer() {
            private static final long serialVersionUID = 1L;

            public Object getDisplayValue(Object object) {
                return object;
            }

            public String getIdValue(Object object, int index) {
                return (String) object;
            }

        });
        ddo.setNullValid(false);
        ddo.setOutputMarkupId(true);
        add(ddo);

    }

}
