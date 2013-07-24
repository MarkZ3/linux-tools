package org.eclipse.linuxtools.internal.tracing.rcp.ui.commands.newtrace;

import org.eclipse.jface.wizard.Wizard;

public class NewTraceWizard extends Wizard {

    @Override
    public boolean performFinish() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void addPages() {
        super.addPages();

        addPage(new NewTraceWizardPage("Create a new trace", "New Trace", null));
    }

}
