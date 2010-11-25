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

package org.hippoecm.hst.plugins.frontend.editor.sitemap.wizard;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.wizard.IWizardModel;
import org.apache.wicket.extensions.wizard.IWizardStep;
import org.apache.wicket.extensions.wizard.Wizard;
import org.apache.wicket.extensions.wizard.WizardButtonBar;
import org.apache.wicket.markup.html.form.Form;

public class AjaxWizardButtonBar extends WizardButtonBar {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public AjaxWizardButtonBar(String id, final Wizard wizard) {
        super(id, wizard);

        setOutputMarkupId(true);

        addOrReplace(new AjaxWizardButton("next", wizard, "next") {
            private static final long serialVersionUID = 1L;

            @Override
            public final boolean isEnabled() {
                return getWizardModel().isNextAvailable();
            }

            @Override
            protected void onClick(AjaxRequestTarget target, Form form) {
                IWizardModel wizardModel = getWizardModel();
                IWizardStep step = wizardModel.getActiveStep();

                // let the step apply any state
                step.applyState();

                // if the step completed after applying the state, move the model onward
                if (step.isComplete()) {
                    wizardModel.next();
                } else {
                    error(getLocalizer().getString(
                            "org.apache.wicket.extensions.wizard.NextButton.step.did.not.complete", this));
                }

                target.addComponent(wizard);
            }

        });

        addOrReplace(new AjaxWizardButton("previous", wizard, "prev") {
            private static final long serialVersionUID = 1L;

            @Override
            public final boolean isEnabled() {
                return getWizardModel().isPreviousAvailable();
            }

            @Override
            protected void onClick(AjaxRequestTarget target, Form form) {
                getWizardModel().previous();
                target.addComponent(wizard);
            }
        });

        addOrReplace(new AjaxWizardButton("finish", wizard, "finish") {
            private static final long serialVersionUID = 1L;

            @Override
            public final boolean isEnabled() {
                IWizardStep activeStep = getWizardModel().getActiveStep();
                return (activeStep != null && getWizardModel().isLastStep(activeStep));
            }

            @Override
            protected void onClick(AjaxRequestTarget target, Form form) {
                IWizardModel wizardModel = getWizardModel();
                IWizardStep step = wizardModel.getActiveStep();

                // let the step apply any state
                step.applyState();

                // if the step completed after applying the state, notify the wizard
                if (step.isComplete()) {
                    getWizardModel().finish();
                } else {
                    error(getLocalizer().getString(
                            "org.apache.wicket.extensions.wizard.FinishButton.step.did.not.complete", this));
                }
                // target.addComponent(wizard);
            }
        });

    }
}
