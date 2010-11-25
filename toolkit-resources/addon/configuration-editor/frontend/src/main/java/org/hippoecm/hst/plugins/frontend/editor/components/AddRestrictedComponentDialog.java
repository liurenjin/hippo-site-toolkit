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

package org.hippoecm.hst.plugins.frontend.editor.components;

import java.util.List;

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.hst.plugins.frontend.editor.dao.ComponentDAO;
import org.hippoecm.hst.plugins.frontend.editor.dialogs.AddNodeDialog;
import org.hippoecm.hst.plugins.frontend.editor.domain.Component;

public class AddRestrictedComponentDialog extends AddNodeDialog<Component> {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public AddRestrictedComponentDialog(ComponentDAO dao, RenderPlugin plugin, JcrNodeModel parent, List<String> choices) {
        super(dao, plugin, parent);

        if (choices == null || choices.size() == 0) {
            throw new IllegalArgumentException("List of choices cannot be null or empty");
        }
        getBean().setName(choices.get(0));
        DropDownChoice dc = new DropDownChoice("name", choices);

        add(dc);
        
        setFocusOnCancel();
    }

}
