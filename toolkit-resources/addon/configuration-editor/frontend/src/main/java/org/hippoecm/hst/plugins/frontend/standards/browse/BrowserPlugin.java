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

package org.hippoecm.hst.plugins.frontend.standards.browse;

import java.util.Iterator;

import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.browse.AbstractBrowseView;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is forked from org.hippoecm.frontend.plugins.standards.browse.BrowserPlugin 
 * See http://issues.onehippo.com/browse/HREPTWO-2867
 */

public class BrowserPlugin extends RenderPlugin {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    
    static final Logger log = LoggerFactory.getLogger(BrowserPlugin.class);
    
    protected final BrowseView browseView;
    
    public BrowserPlugin(IPluginContext context, final IPluginConfig config) {
        super(context, config);

        browseView = new BrowseView(context, config, (JcrNodeModel) getModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected String getExtensionPoint() {
                return config.getString("dialog.list");
            }
        };
    }
 
    abstract public class BrowseView extends AbstractBrowseView {
        private static final long serialVersionUID = 1L;

        protected BrowseView(IPluginContext context, IPluginConfig config, JcrNodeModel document) {
            super(context, config, document);

            final IModelReference documentReference = context.getService(config.getString("model.document"), IModelReference.class);
            context.registerService(new IObserver() {
                private static final long serialVersionUID = 1L;

                public IObservable getObservable() {
                    return documentReference;
                }

                public void onEvent(Iterator<? extends IEvent> event) {
                    BrowserPlugin.this.setModel(documentReference.getModel());
                }

            }, IObserver.class.getName());

            final IModelReference folderReference = context.getService(config.getString("model.folder"), IModelReference.class);
            context.registerService(new IObserver() {
                private static final long serialVersionUID = 1L;

                public IObservable getObservable() {
                    return folderReference;
                }

                public void onEvent(Iterator<? extends IEvent> event) {
                    BrowserPlugin.this.setModel(folderReference.getModel());
                }

            }, IObserver.class.getName());

        }
    }

}
