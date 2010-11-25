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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.IRequestTarget;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.hst.plugins.frontend.editor.BasicEditorPlugin;
import org.hippoecm.hst.plugins.frontend.editor.dao.DescriptionDAO;
import org.hippoecm.hst.plugins.frontend.editor.dao.EditorDAO;
import org.hippoecm.hst.plugins.frontend.editor.dao.SitemapItemDAO;
import org.hippoecm.hst.plugins.frontend.editor.description.DescriptionPicker;
import org.hippoecm.hst.plugins.frontend.editor.description.DescriptionPicker.DescriptionProvider;
import org.hippoecm.hst.plugins.frontend.editor.dialogs.HstContentPickerDialog;
import org.hippoecm.hst.plugins.frontend.editor.domain.Descriptive;
import org.hippoecm.hst.plugins.frontend.editor.domain.SitemapItem;
import org.hippoecm.hst.plugins.frontend.editor.sitemap.wizard.NewPageWizard;
import org.hippoecm.hst.plugins.frontend.editor.validators.UniqueSitemapItemValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SitemapItemEditorPlugin extends BasicEditorPlugin<SitemapItem> {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(SitemapItemEditorPlugin.class);

    private static final String BROWSE_LABEL = "[...]";

    DescriptionPicker pagePicker;
    NewPageWizard newPageWizard;
    boolean wizardEnabled;

    public SitemapItemEditorPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        FormComponent fc;
        fc = new RequiredTextField("matcher");
        fc.add(new UniqueSitemapItemValidator(this, hstContext.sitemap));
        form.add(fc);

        // Linkpicker
        final List<String> nodetypes = new ArrayList<String>();
        if (config.getStringArray("nodetypes") != null) {
            String[] nodeTypes = config.getStringArray("nodetypes");
            nodetypes.addAll(Arrays.asList(nodeTypes));
        }
        if (nodetypes.size() == 0) {
            log.debug("No configuration specified for filtering on nodetypes. No filtering will take place.");
        }

        IDialogFactory dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog() {
                String path = hstContext.sitemap.decodeContentPath(getBean().getContentPath());
                Model model = new Model(hstContext.content.absolutePath(path));
                return new HstContentPickerDialog(context, config, model, nodetypes) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void saveNode(Node node) {
                        try {
                            getBean().setContentPath(hstContext.content.relativePath(node.getPath()));
                            redraw();
                        } catch (RepositoryException e) {
                            log.error(e.getMessage());
                        }
                    }
                };
            }
        };

        DialogLink link = new DialogLink("path", new Model() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getObject() {
                return BROWSE_LABEL;
            }
        }, dialogFactory, getDialogService());
        link.setOutputMarkupId(true);
        form.add(link);

        TextField contentPath = new TextField("contentPath");
        contentPath.setOutputMarkupId(true);
        form.add(contentPath);

        AjaxLink newPage = new AjaxLink("newPage") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                renderNewPageWizard(true, target);
            }

        };
        form.add(newPage);

        DescriptionProvider provider = new DescriptionProvider() {

            public List<Descriptive> load() {
                DescriptionDAO descDao = new DescriptionDAO(context, hstContext.page.getNamespace());
                List<Descriptive> descriptives = new ArrayList<Descriptive>();
                Node pages = hstContext.page.getModel().getNode();
                try {
                    if (pages.hasNodes()) {
                        for (NodeIterator it = pages.getNodes(); it.hasNext();) {
                            Node page = it.nextNode();
                            descriptives.add(descDao.load(new JcrNodeModel(page.getPath())));
                        }
                    }
                } catch (RepositoryException e) {
                    e.printStackTrace();
                }
                return descriptives;
            }
        };

        pagePicker = new DescriptionPicker("pagePicker", new PropertyModel(form.getModel(), "page"), provider) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return !wizardEnabled;
            }
        };
        form.add(pagePicker);

        renderNewPageWizard(false, null);

        form.add(hstContext.site.isPortalConfigEnabled() ? new PortalDetails("portalDetails",
                new AbstractReadOnlyModel() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Object getObject() {
                        return hstContext.component.getReferenceables(true);
                    }

                }) : new EmptyPanel("portalDetails"));
    }

    @Override
    protected void onModelChanged() {
        super.onModelChanged();
        if (pagePicker != null) {
            pagePicker.refresh();
        }

        checkWizard();
    }

    private void renderNewPageWizard(boolean show, IRequestTarget target) {
        wizardEnabled = show;
        if (wizardEnabled) {
            form.addOrReplace(newPageWizard = new NewPageWizard("wizard", getPluginContext(), getPluginConfig()) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onCancel() {
                    super.onCancel();
                    renderNewPageWizard(false, RequestCycle.get().getRequestTarget());
                }

                @Override
                protected void onFinish(org.hippoecm.hst.plugins.frontend.editor.domain.Component page) {
                    IRequestTarget target = RequestCycle.get().getRequestTarget();
                    renderNewPageWizard(false, target);

                    getBean().setPage(page.getName());
                    if (pagePicker != null) {
                        pagePicker.refresh();
                    }

                    if (target != null && target instanceof AjaxRequestTarget) {
                        ((AjaxRequestTarget) target).addComponent(form);
                    }
                }
            });
        } else {
            form.addOrReplace(new EmptyPanel("wizard").setOutputMarkupId(true));
            newPageWizard = null;
        }

        if (target != null && target instanceof AjaxRequestTarget) {
            ((AjaxRequestTarget) target).addComponent(form);
        }
    }

    @Override
    protected String getAddDialogTitle() {
        return new StringResourceModel("dialog.title", this, null).getString();
    }

    @Override
    protected EditorDAO<SitemapItem> newDAO() {
        return new SitemapItemDAO(getPluginContext(), hstContext.sitemap.getNamespace());
    }

    @Override
    protected Dialog newAddDialog() {
        return new AddSitemapItemDialog(dao, this, (JcrNodeModel) getModel());
    }

    private void checkWizard() {
        if (newPageWizard != null) {
            newPageWizard.getWizardModel().cancel();
        }
    }

    @Override
    public void stop() {
        checkWizard();
    }

}
