package org.eclipse.linuxtools.internal.lttng2.control.ui.views.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;

public class EnableEventsDialog extends Dialog {
    public EnableEventsDialog(Shell shell) {
        super(shell);
        setShellStyle(SWT.RESIZE | getShellStyle());
    }

    @Override
    protected Control createDialogArea(Composite parent) {

        Composite fDialogComposite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, true);
        fDialogComposite.setLayout(layout);
        fDialogComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

        Group domainGroup = new Group(fDialogComposite, SWT.SHADOW_NONE);
        domainGroup.setText("Domain");
        layout = new GridLayout(2, true);
        domainGroup.setLayout(layout);

        Button fKernelButton = new Button(domainGroup, SWT.RADIO);
        fKernelButton.setText("Kernel");
        fKernelButton.setSelection(false);
        final Button fUstButton = new Button(domainGroup, SWT.RADIO);
        fUstButton.setText("UST");
        fUstButton.setSelection(true);

        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        domainGroup.setLayoutData(data);

        data = new GridData(SWT.BEGINNING, SWT.BEGINNING, true, true);
        fKernelButton.setLayoutData(data);
        data = new GridData(SWT.BEGINNING, SWT.BEGINNING, true, true);
        fUstButton.setLayoutData(data);
        createUstComposite(fDialogComposite);

        fDialogComposite.layout();

        getShell().setMinimumSize(new Point(500, 650));

        return fDialogComposite;
    }

    private static void createUstComposite(Composite fDialogComposite) {
        Composite fUstComposite;
        fUstComposite = new Composite(fDialogComposite, SWT.NONE);
        GridLayout layout = new GridLayout(1, true);
        fUstComposite.setLayout(layout);
        fUstComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

        Group tpMainGroup = new Group(fUstComposite, SWT.SHADOW_NONE);
        tpMainGroup.setText("Tracepoint Events");
        GridLayout layout2 = new GridLayout(2, false);
        tpMainGroup.setLayout(layout2);
        GridData data = new GridData(GridData.FILL_BOTH);
        tpMainGroup.setLayoutData(data);

        Composite buttonComposite = new Composite(tpMainGroup, SWT.NONE);
        layout2 = new GridLayout(1, true);
        buttonComposite.setLayout(layout2);
        data = new GridData(SWT.BEGINNING, SWT.CENTER, false, true);
        buttonComposite.setLayoutData(data);

        Button fTracepointsActivateButton = new Button(buttonComposite, SWT.RADIO);
        fTracepointsActivateButton.setText("Select");
        data = new GridData(GridData.FILL_HORIZONTAL);
        fTracepointsActivateButton.setLayoutData(data);

        Group tpGroup = new Group(tpMainGroup, SWT.SHADOW_NONE);
        layout2 = new GridLayout(1, true);
        tpGroup.setLayout(layout2);
        data = new GridData(GridData.FILL_BOTH);
        tpGroup.setLayoutData(data);
        Tree fTracepointsViewer = new Tree(tpGroup, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        fTracepointsViewer.setLayoutData(new GridData(GridData.FILL_BOTH));
    }
}
