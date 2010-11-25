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

package org.hippoecm.hst.plugins.frontend.editor.dialogs;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.hst.plugins.frontend.editor.dao.EditorDAO;
import org.hippoecm.hst.plugins.frontend.editor.domain.EditorBean;

public abstract class AddNodeDialog<K extends EditorBean> extends EditorDialog<K> {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final ResourceReference STYLE = new ResourceReference(AddNodeDialog.class, "style.css");

    private RenderPlugin plugin;

    public AddNodeDialog(EditorDAO<K> dao, RenderPlugin plugin, JcrNodeModel parent) {
        super(dao, dao.create(parent));
        this.plugin = plugin;
    }

    @Override
    public void update(K bean) {
        plugin.setModel(bean.getModel());
    }

    @Override
    public IValueMap getProperties() {
        return SMALL;
    }

    @Override
    public void renderHead(HtmlHeaderContainer container) {
        super.renderHead(container);
        container.getHeaderResponse().renderCSSReference(STYLE);
    }
}
