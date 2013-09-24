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

package org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.export;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.linuxtools.internal.tmf.ui.Activator;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageBookmarkElement;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageContentProvider;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageElement;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageFilesElement;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageLabelProvider;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageSupplFilesElement;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageTraceElement;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.Messages;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfTraceElement;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TreeItem;

/**
 * Wizard page for the export trace wizard
 */
public class ExportTraceWizardPage extends WizardPage {

    private static final int SIZING_TEXT_FIELD_WIDTH = 250;
    private static final int COMBO_HISTORY_LENGTH = 5;

    private static final String ZIP_EXTENSION = ".zip"; //$NON-NLS-1$
    private static final String TAR_EXTENSION = ".tar"; //$NON-NLS-1$
    private static final String TAR_GZ_EXTENSION = ".tar.gz"; //$NON-NLS-1$
    private static final String TGZ_EXTENSION = ".tgz"; //$NON-NLS-1$

    static private final String ICON_PATH = "icons/wizban/export_wiz.png"; //$NON-NLS-1$

    // dialog store id constants
    private final static String STORE_DESTINATION_NAMES_ID = "ExportTraceWizardPage1.STORE_DESTINATION_NAMES_ID"; //$NON-NLS-1$
    private final static String STORE_COMPRESS_CONTENTS_ID = "ExportTraceWizardPage1.STORE_COMPRESS_CONTENTS_ID"; //$NON-NLS-1$
    private final static String STORE_FORMAT_ID = "ExportTraceWizardPage1.STORE_FORMAT_ID"; //$NON-NLS-1$

    private Combo fDestinationNameField;

    private Button fDestinationBrowseButton;
    private Button fCompressContentsCheckbox;
    private Button fZipFormatButton;
    private Button fTargzFormatButton;
    private CheckboxTreeViewer fTraceExportElementViewer;
    private Button fSelectAllButton;
    private Button fDeselectAllButton;

    private IStructuredSelection fSelection;

    /**
     * Constructor for the export trace wizard page
     *
     * @param pageName
     *            the name of the page
     * @param selection
     *            the current object selection
     */
    public ExportTraceWizardPage(String pageName, IStructuredSelection selection) {
        super(pageName, Messages.WizardExportPage_title, Activator.getDefault().getImageDescripterFromPath(ICON_PATH));
        fSelection = selection;
    }

    @Override
    public void createControl(Composite parent) {

        initializeDialogUnits(parent);

        Composite composite = new Composite(parent, SWT.NULL);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL
                | GridData.HORIZONTAL_ALIGN_FILL));
        composite.setFont(parent.getFont());

        createTraceElementsGroup(composite);
        createButtonsGroup(composite);
        createDestinationGroup(composite);
        createOptionsGroup(composite);

        restoreWidgetValues();

        updateWidgetEnablements();
        setPageComplete(determinePageCompletion());

        setControl(composite);
    }

    /**
     * Restore widget values to the values that they held last
     * time this wizard was used to completion.
     */
    private void restoreWidgetValues() {
        IDialogSettings settings = getDialogSettings();
        if (settings != null) {
            String[] directoryNames = settings
                    .getArray(STORE_DESTINATION_NAMES_ID);
            if (directoryNames == null || directoryNames.length == 0) {
                return; // ie.- no settings stored
            }

            // destination
            setDestinationValue(directoryNames[0]);
            for (int i = 0; i < directoryNames.length; i++) {
                fDestinationNameField.add(directoryNames[i]);
            }

            fCompressContentsCheckbox.setSelection(settings.getBoolean(STORE_COMPRESS_CONTENTS_ID));
            fZipFormatButton.setSelection(settings.getBoolean(STORE_FORMAT_ID));
            fTargzFormatButton.setSelection(!settings.getBoolean(STORE_FORMAT_ID));
        }

        updateMessage();
    }

    private void updateMessage() {
        setMessage(Messages.ExportTraceWizardPage_ChooseContent);
    }

    private void createOptionsGroup(Composite parent) {
        // options group
        Group optionsGroup = new Group(parent, SWT.NONE);
        optionsGroup.setLayout(new RowLayout(SWT.VERTICAL));
        optionsGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL
                | GridData.GRAB_HORIZONTAL));
        optionsGroup.setText(Messages.WizardExportPage_options);
        optionsGroup.setFont(parent.getFont());

        Font font = optionsGroup.getFont();

        SelectionAdapter listener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateFileNameWithFormat();
            }
        };

        // create directory structure radios
        fZipFormatButton = new Button(optionsGroup, SWT.RADIO | SWT.LEFT);
        fZipFormatButton.setText(Messages.ArchiveExport_saveInZipFormat);
        fZipFormatButton.setSelection(true);
        fZipFormatButton.setFont(font);
        fZipFormatButton.addSelectionListener(listener);

        // create directory structure radios
        fTargzFormatButton = new Button(optionsGroup, SWT.RADIO | SWT.LEFT);
        fTargzFormatButton.setText(Messages.ArchiveExport_saveInTarFormat);
        fTargzFormatButton.setSelection(false);
        fTargzFormatButton.setFont(font);
        fTargzFormatButton.addSelectionListener(listener);

        fCompressContentsCheckbox = new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
        fCompressContentsCheckbox.setText(Messages.ZipExport_compressContents);
        fCompressContentsCheckbox.setFont(font);
        fCompressContentsCheckbox.setSelection(true);
        fCompressContentsCheckbox.addSelectionListener(listener);

    }

    private void createTraceElementsGroup(Composite parent) {
        fTraceExportElementViewer = new CheckboxTreeViewer(parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.CHECK);

        fTraceExportElementViewer.addCheckStateListener(new ICheckStateListener() {
            @Override
            public void checkStateChanged(CheckStateChangedEvent event) {
                updatePageCompletion();
            }
        });
        GridData layoutData = new GridData(GridData.FILL_BOTH);
        fTraceExportElementViewer.getTree().setLayoutData(layoutData);
        fTraceExportElementViewer.setContentProvider(new TracePackageContentProvider());
        fTraceExportElementViewer.setLabelProvider(new TracePackageLabelProvider());

        setTraceElements();
        fTraceExportElementViewer.expandAll();
        setAllChecked(true);
    }

    private void setTraceElements() {
        if (fSelection.getFirstElement() instanceof TmfTraceElement) {
            TmfTraceElement tmfTraceElement = (TmfTraceElement) fSelection.getFirstElement();
            TracePackageElement traceTransferElement = new TracePackageTraceElement(null, tmfTraceElement);
            List<TracePackageElement> children = new ArrayList<TracePackageElement>();
            children.add(new TracePackageFilesElement(traceTransferElement));
            children.add(new TracePackageSupplFilesElement(tmfTraceElement.getSupplementaryResources(), traceTransferElement));

            children.add(new TracePackageBookmarkElement(traceTransferElement, null, null));

            traceTransferElement.setChildren(children.toArray(new TracePackageElement[] {}));
            fTraceExportElementViewer.setInput(new TracePackageElement[] { traceTransferElement });
        }
    }

    private Button createButton(Composite parent, int id, String label,
            boolean defaultButton) {
        // increment the number of columns in the button bar
        ((GridLayout) parent.getLayout()).numColumns++;

        Button button = new Button(parent, SWT.PUSH);

        GridData buttonData = new GridData(GridData.FILL_HORIZONTAL);
        button.setLayoutData(buttonData);

        button.setData(new Integer(id));
        button.setText(label);
        button.setFont(parent.getFont());

        if (defaultButton) {
            Shell shell = parent.getShell();
            if (shell != null) {
                shell.setDefaultButton(button);
            }
            button.setFocus();
        }
        button.setFont(parent.getFont());
        setButtonLayoutData(button);
        return button;
    }

    /**
     * Creates the buttons for selecting specific types or selecting all or none
     * of the elements.
     *
     * @param parent
     *            the parent control
     */
    private final void createButtonsGroup(Composite parent) {

        Font font = parent.getFont();

        // top level group
        Composite buttonComposite = new Composite(parent, SWT.NONE);
        buttonComposite.setFont(parent.getFont());

        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        layout.makeColumnsEqualWidth = true;
        buttonComposite.setLayout(layout);
        buttonComposite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL
                | GridData.HORIZONTAL_ALIGN_FILL));

        fSelectAllButton = createButton(buttonComposite,
                IDialogConstants.SELECT_ALL_ID, Messages.SelectAll, false);

        SelectionListener listener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setAllChecked(true);
                updateWidgetEnablements();
            }
        };
        fSelectAllButton.addSelectionListener(listener);
        fSelectAllButton.setFont(font);
        setButtonLayoutData(fSelectAllButton);

        fDeselectAllButton = createButton(buttonComposite,
                IDialogConstants.DESELECT_ALL_ID, Messages.DeselectAll, false);

        listener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setAllChecked(false);
                updateWidgetEnablements();
            }
        };
        fDeselectAllButton.addSelectionListener(listener);
        fDeselectAllButton.setFont(font);
        setButtonLayoutData(fDeselectAllButton);
    }

    private void setAllChecked(boolean checked) {
        TreeItem[] items = fTraceExportElementViewer.getTree().getItems();
        for (int i = 0; i < items.length; i++) {
            Object element = items[i].getData();
            fTraceExportElementViewer.setSubtreeChecked(element, checked);
        }
    }

    /**
     * Check if widgets are enabled or disabled by a change in the dialog.
     */
    private void updateWidgetEnablements() {

        boolean pageComplete = determinePageCompletion();
        setPageComplete(pageComplete);
        if (pageComplete) {
            setMessage(null);
        }
    }

    /**
     * Determine if the page is complete and update the page appropriately.
     */
    private void updatePageCompletion() {
        boolean pageComplete = determinePageCompletion();
        setPageComplete(pageComplete);
        if (pageComplete) {
            setErrorMessage(null);
        }
    }

    private boolean determinePageCompletion() {
        boolean complete = validateSourceGroup();

        // Avoid draw flicker by not clearing the error
        // message unless all is valid.
        if (complete) {
            setErrorMessage(null);
        }

        return complete;
    }

    private boolean validateSourceGroup() {
        boolean traceElementSelected = fTraceExportElementViewer.getCheckedElements().length > 0;
        return traceElementSelected;
    }

    private void createDestinationGroup(Composite parent) {

        Font font = parent.getFont();
        // destination specification group
        Composite destinationSelectionGroup = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        destinationSelectionGroup.setLayout(layout);
        destinationSelectionGroup.setLayoutData(new GridData(
                GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL));
        destinationSelectionGroup.setFont(font);

        Label destinationLabel = new Label(destinationSelectionGroup, SWT.NONE);
        destinationLabel.setText(Messages.FileExport_toArchive);
        destinationLabel.setFont(font);

        // destination name entry field
        fDestinationNameField = new Combo(destinationSelectionGroup, SWT.SINGLE
                | SWT.BORDER);
        GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL
                | GridData.GRAB_HORIZONTAL);
        data.widthHint = SIZING_TEXT_FIELD_WIDTH;
        fDestinationNameField.setLayoutData(data);
        fDestinationNameField.setFont(font);

        // destination browse button
        fDestinationBrowseButton = new Button(destinationSelectionGroup,
                SWT.PUSH);
        fDestinationBrowseButton.setText(Messages.DataTransfer_browse);
        fDestinationBrowseButton.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                handleDestinationBrowseButtonPressed();
            }
        });
        fDestinationBrowseButton.setFont(font);
        setButtonLayoutData(fDestinationBrowseButton);

        new Label(parent, SWT.NONE); // vertical spacer
    }

    private String getOutputSuffix() {
        if (fZipFormatButton.getSelection()) {
            return ".zip"; //$NON-NLS-1$
        } else if (fCompressContentsCheckbox.getSelection()) {
            return ".tar.gz"; //$NON-NLS-1$
        } else {
            return ".tar"; //$NON-NLS-1$
        }
    }

    /**
     * Answer the contents of self's destination specification widget. If this
     * value does not have a suffix then add it first.
     */
    private String getDestinationValue() {
        String idealSuffix = getOutputSuffix();
        String destinationText = fDestinationNameField.getText().trim();

        // only append a suffix if the destination doesn't already have a . in
        // its last path segment.
        // Also prevent the user from selecting a directory. Allowing this will
        // create a ".zip" file in the directory
        if (destinationText.length() != 0
                && !destinationText.endsWith(File.separator)) {
            int dotIndex = destinationText.lastIndexOf('.');
            if (dotIndex != -1) {
                // the last path seperator index
                int pathSepIndex = destinationText.lastIndexOf(File.separator);
                if (pathSepIndex != -1 && dotIndex < pathSepIndex) {
                    destinationText += idealSuffix;
                }
            } else {
                destinationText += idealSuffix;
            }
        }

        return destinationText;
    }

    /**
     * Open an appropriate destination browser so that the user can specify a
     * source to import from
     */
    private void handleDestinationBrowseButtonPressed() {
        FileDialog dialog = new FileDialog(getContainer().getShell(), SWT.SAVE | SWT.SHEET);
        dialog.setFilterExtensions(new String[] { "*.zip;*.tar.gz;*.tar;*.tgz", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
        dialog.setText(Messages.ArchiveExport_selectDestinationTitle);
        String currentSourceString = getDestinationValue();
        int lastSeparatorIndex = currentSourceString
                .lastIndexOf(File.separator);
        if (lastSeparatorIndex != -1) {
            dialog.setFilterPath(currentSourceString.substring(0,
                    lastSeparatorIndex));
        }
        String selectedFileName = dialog.open();

        if (selectedFileName != null) {
            setErrorMessage(null);
            setDestinationValue(selectedFileName);
            updateFileNameWithFormat();
        }
    }

    private void setDestinationValue(String value) {
        fDestinationNameField.setText(value);
    }

    private void handleError(String message, Throwable exception) {
        Activator.getDefault().logError(message, exception);

        displayErrorDialog(message, exception);
    }

    private void displayErrorDialog(String message, Throwable exception) {
        String exceptionMessage = exception.getMessage();
        // Some system exceptions have no message
        if (exceptionMessage == null) {
            exceptionMessage = exception.toString();
        }

        String stackMessage = exceptionMessage;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        exception.printStackTrace(ps);
        ps.flush();
        try {
            baos.flush();
            stackMessage = baos.toString();
        } catch (IOException e) {
        }

        // ErrorDialog only prints the call stack for a CoreException
        CoreException coreException = new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, IStatus.ERROR, stackMessage, exception));
        final Status s = new Status(IStatus.ERROR, Activator.PLUGIN_ID, IStatus.ERROR, exceptionMessage, coreException);
        ErrorDialog.openError(getContainer().getShell(), Messages.WizardExportPage_internalErrorTitle, message, s);
    }

    private static void addToHistory(List<String> history, String newEntry) {
        history.remove(newEntry);
        history.add(0, newEntry);

        // since only one new item was added, we can be over the limit
        // by at most one item
        if (history.size() > COMBO_HISTORY_LENGTH) {
            history.remove(COMBO_HISTORY_LENGTH);
        }
    }

    private static String[] addToHistory(String[] history, String newEntry) {
        ArrayList<String> l = new ArrayList<String>(Arrays.asList(history));
        addToHistory(l, newEntry);
        String[] r = new String[l.size()];
        l.toArray(r);
        return r;
    }

    /**
     * Save widget values to Dialog settings
     */
    private void saveWidgetValues() {
        IDialogSettings settings = getDialogSettings();
        if (settings != null) {
            // update directory names history
            String[] directoryNames = settings
                    .getArray(STORE_DESTINATION_NAMES_ID);
            if (directoryNames == null) {
                directoryNames = new String[0];
            }

            directoryNames = addToHistory(directoryNames, getDestinationValue());
            settings.put(STORE_DESTINATION_NAMES_ID, directoryNames);

            settings.put(STORE_COMPRESS_CONTENTS_ID, fCompressContentsCheckbox.getSelection());
            settings.put(STORE_FORMAT_ID, fZipFormatButton.getSelection());
        }
    }

    private void updateFileNameWithFormat() {

        String destinationValue = stripKnownExtension(getDestinationValue());
        if (fZipFormatButton.getSelection()) {
            destinationValue = destinationValue.concat(ZIP_EXTENSION);
        } else if (fCompressContentsCheckbox.getSelection()) {
            destinationValue = destinationValue.concat(TAR_GZ_EXTENSION);
        } else {
            destinationValue = destinationValue.concat(TAR_EXTENSION);
        }

        setDestinationValue(destinationValue);
    }

    private static String stripKnownExtension(String str) {
        String ret = str;
        if (str.endsWith(TAR_GZ_EXTENSION)) {
            ret = ret.substring(0, ret.lastIndexOf(".")); //$NON-NLS-1$
        }

        if (ret.endsWith(ZIP_EXTENSION) | ret.endsWith(TAR_EXTENSION) | ret.endsWith(TGZ_EXTENSION)) {
            ret = ret.substring(0, ret.lastIndexOf(".")); //$NON-NLS-1$
        }

        return ret;
    }

    /**
     * @return true on success
     */
    public boolean finish() {

        saveWidgetValues();

        TracePackageTraceElement traceExportElement = (TracePackageTraceElement) fTraceExportElementViewer.getTree().getItem(0).getData();
        Object[] checkedElements = fTraceExportElementViewer.getCheckedElements();
        boolean useCompression = fCompressContentsCheckbox.getSelection();
        boolean useTar = fTargzFormatButton.getSelection();
        String fileName = getDestinationValue();
        final TraceExporter exporter = new TraceExporter(traceExportElement, checkedElements, useCompression, useTar, fileName);

        try {
            getContainer().run(true, true, new IRunnableWithProgress() {

                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    exporter.doExport(monitor);
                }
            });

            IStatus status = exporter.getStatus();
            if (status.getSeverity() == IStatus.ERROR) {
                String message = status.getMessage().length() > 0 ? status.getMessage() : Messages.ExportTraceWizardPage_ErrorOperation;
                handleError(message, status.getException());
            }

        } catch (InvocationTargetException e) {
            handleError(Messages.ExportTraceWizardPage_ErrorOperation, e);
        } catch (InterruptedException e) {
        }

        return exporter.getStatus().getSeverity() == IStatus.OK;
    }

}
