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
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.linuxtools.internal.tmf.ui.Activator;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.AbstractTracePackageWizard;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageContentProvider;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageElement;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageFilesElement;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageLabelProvider;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageTraceElement;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfTraceFolder;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

/**
 * Wizard page for the import trace package wizard
 *
 * @author Marc-Andre Laperle
 */
public class ImportTracePackagePage extends AbstractTracePackageWizard {

    static private final String ICON_PATH = "icons/wizban/trace_import_wiz.png"; //$NON-NLS-1$
    private static final String PAGE_NAME = "ImportTracePackagePage1"; //$NON-NLS-1$
    private final static String STORE_SOURCE_NAMES_ID = PAGE_NAME + ".STORE_SOURCE_NAMES_ID"; //$NON-NLS-1$

    private CheckboxTreeViewer fTraceExportElementViewer;

    private Combo fSourceNameField;
    private Button fSelectAllButton;
    private Button fDeselectAllButton;

    private Button fSourceBrowseButton;

    /**
     * Constructor for the import trace package wizard page
     *
     * @param selection
     *            the current object selection
     */
    public ImportTracePackagePage(IStructuredSelection selection) {
        super(PAGE_NAME, Messages.ImportTracePkgPage_title, Activator.getDefault().getImageDescripterFromPath(ICON_PATH), selection);
    }

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        Composite composite = new Composite(parent, SWT.NULL);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL
                | GridData.HORIZONTAL_ALIGN_FILL));
        composite.setFont(parent.getFont());

        createSourceGroup(composite);
        createTraceElementsGroup(composite);
        createButtonsGroup(composite);

        restoreWidgetValues();
        updatePageCompletion();

        setControl(composite);
    }

    /**
     * Creates the buttons for selecting specific types or selecting all or none
     * of the elements.
     *
     * @param parent
     *            the parent control
     */
    private final void createButtonsGroup(Composite parent) {

        // top level group
        Composite buttonComposite = new Composite(parent, SWT.NONE);

        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        layout.makeColumnsEqualWidth = true;
        buttonComposite.setLayout(layout);
        buttonComposite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL
                | GridData.HORIZONTAL_ALIGN_FILL));

        fSelectAllButton = new Button(buttonComposite, SWT.PUSH);
        fSelectAllButton.setText(org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.Messages.TracePackage_SelectAll);

        SelectionListener listener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setAllChecked(fTraceExportElementViewer, true, true);
                updatePageCompletion();
            }
        };
        fSelectAllButton.addSelectionListener(listener);
        setButtonLayoutData(fSelectAllButton);

        fDeselectAllButton = new Button(buttonComposite, SWT.PUSH);
        fDeselectAllButton.setText(org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.Messages.TracePackage_DeselectAll);

        listener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setAllChecked(fTraceExportElementViewer, true, false);
                updatePageCompletion();
            }
        };
        fDeselectAllButton.addSelectionListener(listener);
        setButtonLayoutData(fDeselectAllButton);
    }

    private void createTraceElementsGroup(Composite parent) {
        fTraceExportElementViewer = new CheckboxTreeViewer(parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.CHECK);

        fTraceExportElementViewer.addCheckStateListener(new ICheckStateListener() {
            @Override
            public void checkStateChanged(CheckStateChangedEvent event) {
                TracePackageElement element = (TracePackageElement) event.getElement();
                if (!element.isEnabled()) {
                    fTraceExportElementViewer.setChecked(element, element.isChecked());
                }
                maintainCheckIntegrity(element);
                updatePageCompletion();
            }
        });
        GridData layoutData = new GridData(GridData.FILL_BOTH);
        fTraceExportElementViewer.getTree().setLayoutData(layoutData);
        fTraceExportElementViewer.setContentProvider(new TracePackageContentProvider());
        fTraceExportElementViewer.setLabelProvider(new TracePackageLabelProvider());
    }

    private void maintainCheckIntegrity(final TracePackageElement element) {
        TracePackageElement parentElement = element.getParent();
        boolean allChecked = true;
        if (parentElement != null) {
            if (parentElement.getChildren() != null) {
                for (TracePackageElement child : parentElement.getChildren()) {
                    allChecked &= fTraceExportElementViewer.getChecked(child);
                }
            }
            fTraceExportElementViewer.setChecked(parentElement, allChecked);
            maintainCheckIntegrity(parentElement);
        }
    }

    private void setTraceElements() {

        final TracePackageExtractManifestOperation op = new TracePackageExtractManifestOperation(getSourceValue());

        try {
            getContainer().run(true, true, new IRunnableWithProgress() {

                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    monitor.beginTask(Messages.TraceImporter_ReadingPackage, 10);
                    op.run(monitor);
                    monitor.done();
                }

            });

            IStatus status = op.getStatus();
            if (status.getSeverity() == IStatus.ERROR) {
                String message = status.getMessage().length() > 0 ? status.getMessage() : Messages.ImportTracePackagePage_ErrorReadingManifest;
                handleError(message, status.getException());
            }
        } catch (InvocationTargetException e1) {
            handleError(Messages.ImportTracePackagePage_ErrorReadingManifest, e1);
        } catch (InterruptedException e1) {
            // Canceled
        }

        TracePackageElement resultElement = op.getResultElement();
        if (resultElement != null) {
            for (TracePackageElement e : resultElement.getChildren()) {
                if (e instanceof TracePackageFilesElement) {
                    e.setEnabled(false);
                }
            }
            fTraceExportElementViewer.setInput(new TracePackageElement[] { resultElement });
        } else {
            fTraceExportElementViewer.setInput(null);
        }
    }

    private void createSourceGroup(Composite parent) {

        // source specification group
        Composite sourceSelectionGroup = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        sourceSelectionGroup.setLayout(layout);
        sourceSelectionGroup.setLayoutData(new GridData(
                GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL));

        Label sourceLabel = new Label(sourceSelectionGroup, SWT.NONE);
        sourceLabel.setText(Messages.FileImport_FromArchive);

        // source name entry field
        fSourceNameField = new Combo(sourceSelectionGroup, SWT.SINGLE
                | SWT.BORDER);
        GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL
                | GridData.GRAB_HORIZONTAL);
        fSourceNameField.setLayoutData(data);
        fSourceNameField.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateWithSelection();
            }
        });
        fSourceNameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.keyCode == '\r') {
                    updateWithSelection();
                }
            }
        });

        // source browse button
        fSourceBrowseButton = new Button(sourceSelectionGroup, SWT.PUSH);
        fSourceBrowseButton.setText(org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.Messages.TracePackage_Browse);
        fSourceBrowseButton.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                handleSourceBrowseButtonPressed();
            }
        });
        setButtonLayoutData(fSourceBrowseButton);
    }

    /**
     * Open an appropriate source browser so that the user can specify a
     * source to import from
     */
    private void handleSourceBrowseButtonPressed() {
        FileDialog dialog = new FileDialog(getContainer().getShell(), SWT.OPEN | SWT.SHEET);
        dialog.setFilterExtensions(new String[] { "*.zip;*.tar.gz;*.tar;*.tgz", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
        dialog.setText(Messages.ArchiveImport_selectSourceTitle);
        String currentSourceString = getSourceValue();
        int lastSeparatorIndex = currentSourceString
                .lastIndexOf(File.separator);
        if (lastSeparatorIndex != -1) {
            dialog.setFilterPath(currentSourceString.substring(0,
                    lastSeparatorIndex));
        }
        String selectedFileName = dialog.open();

        if (selectedFileName != null) {
            setSourceValue(selectedFileName);

            updateWithSelection();
        }
    }

    private void updateWithSelection() {
        if (!new File(getSourceValue()).exists()) {
            setErrorMessage(Messages.ImportTracePackagePage_ErrorFileNotFound);
            return;
        }
        setErrorMessage(null);

        getContainer().getShell().getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                setTraceElements();
                fTraceExportElementViewer.expandToLevel(2);
                setAllChecked(fTraceExportElementViewer, false, true);
            }
        });
    }

    private void setSourceValue(String value) {
        fSourceNameField.setText(value);
        updatePageCompletion();
    }

    @Override
    protected boolean determinePageCompletion() {
        return fTraceExportElementViewer.getCheckedElements().length > 0 && !getSourceValue().isEmpty();
    }

    /**
     * Answer the contents of self's source specification widget. If this
     * value does not have a suffix then add it first.
     */
    private String getSourceValue() {
        return fSourceNameField.getText().trim();
    }

    /**
     * Restore widget values to the values that they held last time this wizard
     * was used to completion.
     */
    private void restoreWidgetValues() {
        IDialogSettings settings = getDialogSettings();
        if (settings != null) {
            String[] directoryNames = settings
                    .getArray(STORE_SOURCE_NAMES_ID);
            if (directoryNames == null || directoryNames.length == 0) {
                return; // ie.- no settings stored
            }

            for (int i = 0; i < directoryNames.length; i++) {
                fSourceNameField.add(directoryNames[i]);
            }
        }

        updateMessage();
    }

    private void updateMessage() {
        setMessage(Messages.ImportTracePackagePage_Title);
    }

    /**
     * Save widget values to Dialog settings
     */
    private void saveWidgetValues() {
        IDialogSettings settings = getDialogSettings();
        if (settings != null) {
            // update directory names history
            String[] directoryNames = settings
                    .getArray(STORE_SOURCE_NAMES_ID);
            if (directoryNames == null) {
                directoryNames = new String[0];
            }

            directoryNames = addToHistory(directoryNames, getSourceValue());
            settings.put(STORE_SOURCE_NAMES_ID, directoryNames);
        }
    }

    /**
     * Finish the wizard page
     *
     * @return true on success
     */
    public boolean finish() {
        saveWidgetValues();

        TmfTraceFolder tmfTraceFolder = (TmfTraceFolder) getSelection().getFirstElement();

        TracePackageElement[] input = (TracePackageElement[]) fTraceExportElementViewer.getInput();
        TracePackageTraceElement exportTraceTraceElement = (TracePackageTraceElement) input[0];
        final TracePackageImportOperation exporter = new TracePackageImportOperation(getSourceValue(), exportTraceTraceElement, tmfTraceFolder);

        try {
            getContainer().run(true, true, new IRunnableWithProgress() {

                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    exporter.run(monitor);
                }
            });

            IStatus status = exporter.getStatus();
            if (status.getSeverity() == IStatus.ERROR) {
                String message = status.getMessage().length() > 0 ? status.getMessage() : Messages.ImportTracePackagePage_ErrorOperation;
                handleError(message, status.getException());
            }

        } catch (InvocationTargetException e) {
            handleError(Messages.ImportTracePackagePage_ErrorOperation, e);
        } catch (InterruptedException e) {
        }

        return exporter.getStatus().getSeverity() == IStatus.OK;
    }
}
