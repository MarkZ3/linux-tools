package org.eclipse.linuxtools.internal.tracing.rcp.ui.commands.newtrace;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Sash;

public class NewTraceWizardPage extends WizardPage {

    protected NewTraceWizardPage(String pageName, String title, ImageDescriptor titleImage) {
        super(pageName, title, titleImage);
    }

    @Override
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.BORDER);
        setControl(composite);

        GridLayout layout = new GridLayout(3, false);
        composite.setLayout(layout);

        List tracerList = new List(composite, SWT.SINGLE);
        tracerList.add("LTTng");
        tracerList.add("DTrace");
        tracerList.add("SystemTap");
        tracerList.add("Perf");
        GridData gd = new GridData(SWT.FILL, SWT.FILL, false, true);
        gd.widthHint = 100;
        tracerList.setLayoutData(gd);

        final Sash sash = new Sash(composite, SWT.VERTICAL);
        Rectangle clientArea = composite.getClientArea();
        sash.setBounds (180, clientArea.y, 32, clientArea.height);
        sash.addListener (SWT.Selection, new Listener () {
            @Override
            public void handleEvent (Event e) {
                sash.setBounds (e.x, e.y, e.width, e.height);
            }
        });

        List analysis = new List(composite, SWT.SINGLE);
        analysis.add("CPU Scheduling");
        analysis.add("File system");
        analysis.add("Power");
        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        analysis.setLayoutData(gd);
    }

}
