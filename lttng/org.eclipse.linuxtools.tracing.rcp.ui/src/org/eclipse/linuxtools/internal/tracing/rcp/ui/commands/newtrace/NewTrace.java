package org.eclipse.linuxtools.internal.tracing.rcp.ui.commands.newtrace;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;

public class NewTrace extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        WizardDialog dialog = new NewTraceWizardDialog(Display.getCurrent().getActiveShell(), new NewTraceWizard());
        dialog.open();
        return null;
    }

}
