/*******************************************************************************
 * Copyright (c) 2013 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Marc-Andre Laperle - Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.imprt;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.linuxtools.internal.tmf.ui.Activator;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.AbstractTracePackageWizard;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageElement;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageFilesElement;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageTraceElement;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfTraceFolder;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

/**
 * Wizard page for the import trace package wizard
 *
 * @author Marc-Andre Laperle
 */
public class ImportTracePackageWizardPage extends AbstractTracePackageWizard {

    static private final String ICON_PATH = "icons/wizban/trace_import_wiz.png"; //$NON-NLS-1$
    private static final String PAGE_NAME = "ImportTracePackagePage1"; //$NON-NLS-1$
    private String fValidatedFilePath;

    /**
     * Constructor for the import trace package wizard page
     *
     * @param selection
     *            the current object selection
     */
    public ImportTracePackageWizardPage(IStructuredSelection selection) {
        super(PAGE_NAME, Messages.ImportTracePackageWizardPage_Title, Activator.getDefault().getImageDescripterFromPath(ICON_PATH), selection);
    }

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        Composite composite = new Composite(parent, SWT.NULL);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL
                | GridData.HORIZONTAL_ALIGN_FILL));
        composite.setFont(parent.getFont());

        createFilePathGroup(composite, Messages.ImportTracePackageWizardPage_FromArchive);
        createElementViewer(composite);
        createButtonsGroup(composite);

        restoreWidgetValues();
        setMessage(Messages.ImportTracePackageWizardPage_Message);
        updatePageCompletion();

        setControl(composite);
    }

    @Override
    protected Object createElementViewerInput() {

        final TracePackageExtractManifestOperation op = new TracePackageExtractManifestOperation(getFilePathValue());

        try {
            getContainer().run(true, true, new IRunnableWithProgress() {

                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    monitor.beginTask(Messages.ImportTracePackageWizardPage_ReadingPackage, 10);
                    op.run(monitor);
                    monitor.done();
                }

            });

            IStatus status = op.getStatus();
            if (status.getSeverity() == IStatus.ERROR) {
                String message = status.getMessage().length() > 0 ? status.getMessage() : Messages.TracePackageExtractManifestOperation_ErrorReadingManifest;
                handleError(message, status.getException());
            }
        } catch (InvocationTargetException e1) {
            handleError(Messages.TracePackageExtractManifestOperation_ErrorReadingManifest, e1);
        } catch (InterruptedException e1) {
            // Canceled
        }

        TracePackageElement resultElement = op.getResultElement();
        if (resultElement == null) {
            return null;
        }

        for (TracePackageElement e : resultElement.getChildren()) {
            if (e instanceof TracePackageFilesElement) {
                e.setEnabled(false);
            }
        }

        return new TracePackageElement[] { resultElement };
    }

    @Override
    protected void createFilePathGroup(Composite parent, String label) {
        super.createFilePathGroup(parent, label);

        Combo filePathCombo = getFilePathCombo();
        filePathCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateWithFilePathSelection();
            }
        });

        // User can type-in path and press return to validate
        filePathCombo.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.keyCode == '\r') {
                    updateWithFilePathSelection();
                }
            }
        });
    }

    @Override
    protected void updateWithFilePathSelection() {
        if (!new File(getFilePathValue()).exists()) {
            setErrorMessage(Messages.ImportTracePackageWizardPage_ErrorFileNotFound);
            return;
        }
        setErrorMessage(null);

        getContainer().getShell().getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                CheckboxTreeViewer elementViewer = getElementViewer();
                Object elementViewerInput = createElementViewerInput();
                elementViewer.setInput(elementViewerInput);
                if (elementViewerInput != null) {
                    elementViewer.expandToLevel(2);
                    setAllChecked(elementViewer, false, true);
                    fValidatedFilePath = getFilePathValue();
                }

                updatePageCompletion();
            }
        });
    }

    /**
     * Finish the wizard page
     *
     * @return true on success
     */
    public boolean finish() {
        saveWidgetValues();

        TmfTraceFolder tmfTraceFolder = (TmfTraceFolder) getSelection().getFirstElement();

        TracePackageElement[] input = (TracePackageElement[]) getElementViewer().getInput();
        TracePackageTraceElement exportTraceTraceElement = (TracePackageTraceElement) input[0];
        final TracePackageImportOperation exporter = new TracePackageImportOperation(fValidatedFilePath, exportTraceTraceElement, tmfTraceFolder);

        try {
            getContainer().run(true, true, new IRunnableWithProgress() {

                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    exporter.run(monitor);
                }
            });

            IStatus status = exporter.getStatus();
            if (status.getSeverity() == IStatus.ERROR) {
                String message = status.getMessage().length() > 0 ? status.getMessage() : Messages.ImportTracePackageWizardPage_ErrorOperation;
                handleError(message, status.getException());
            }

        } catch (InvocationTargetException e) {
            handleError(Messages.ImportTracePackageWizardPage_ErrorOperation, e);
        } catch (InterruptedException e) {
        }

        return exporter.getStatus().getSeverity() == IStatus.OK;
    }
}
