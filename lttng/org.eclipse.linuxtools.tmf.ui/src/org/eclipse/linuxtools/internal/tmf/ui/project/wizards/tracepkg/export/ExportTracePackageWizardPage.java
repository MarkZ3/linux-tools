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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.linuxtools.internal.tmf.ui.Activator;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageBookmarkElement;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageContentProvider;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageElement;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageFilesElement;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageLabelProvider;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageSupplFileElement;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageSupplFilesElement;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageTraceElement;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfTraceElement;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
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
import org.eclipse.swt.widgets.TreeItem;

/**
 * Wizard page for the export trace package wizard
 *
 * @author Marc-Andre Laperle
 */
public class ExportTracePackageWizardPage extends WizardPage {

    private static final int COMBO_HISTORY_LENGTH = 5;
    private static final int CONTENT_COL_WIDTH = 300;
    private static final int SIZE_COL_WIDTH = 100;

    private static final String ZIP_EXTENSION = ".zip"; //$NON-NLS-1$
    private static final String TAR_EXTENSION = ".tar"; //$NON-NLS-1$
    private static final String TAR_GZ_EXTENSION = ".tar.gz"; //$NON-NLS-1$
    private static final String TGZ_EXTENSION = ".tgz"; //$NON-NLS-1$

    static private final String ICON_PATH = "icons/wizban/export_wiz.png"; //$NON-NLS-1$

    // dialog store id constants
    private final static String STORE_DESTINATION_NAMES_ID = "ExportTracePackageWizardPage1.STORE_DESTINATION_NAMES_ID"; //$NON-NLS-1$
    private final static String STORE_COMPRESS_CONTENTS_ID = "ExportTracePackageWizardPage1.STORE_COMPRESS_CONTENTS_ID"; //$NON-NLS-1$
    private final static String STORE_FORMAT_ID = "ExportTracePackageWizardPage1.STORE_FORMAT_ID"; //$NON-NLS-1$

    private Combo fDestinationNameField;

    private Button fDestinationBrowseButton;
    private Button fCompressContentsCheckbox;
    private Button fZipFormatButton;
    private Button fTargzFormatButton;
    private CheckboxTreeViewer fTraceExportElementViewer;
    private Button fSelectAllButton;
    private Button fDeselectAllButton;

    private IStructuredSelection fSelection;
    private Label fApproximateSizeLabel;

    /**
     * Constructor for the export trace wizard page
     *
     * @param pageName
     *            the name of the page
     * @param selection
     *            the current object selection
     */
    public ExportTracePackageWizardPage(String pageName, IStructuredSelection selection) {
        super(pageName, Messages.ExportTracePackageWizardPage_Title, Activator.getDefault().getImageDescripterFromPath(ICON_PATH));
        fSelection = selection;
    }

    @Override
    public void createControl(Composite parent) {

        initializeDialogUnits(parent);

        Composite composite = new Composite(parent, SWT.NULL);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));

        createTraceElementsGroup(composite);
        createButtonsGroup(composite);
        createDestinationGroup(composite);
        createOptionsGroup(composite);

        updateApproximateSize();

        restoreWidgetValues();
        updateMessage();

        updatePageCompletion();
        setPageComplete(determinePageCompletion());

        setControl(composite);
    }

    /**
     * Restore widget values to the values that they held last time this wizard
     * was used to completion.
     */
    private void restoreWidgetValues() {
        IDialogSettings settings = getDialogSettings();
        if (settings != null) {
            String[] directoryNames = settings.getArray(STORE_DESTINATION_NAMES_ID);
            if (directoryNames == null || directoryNames.length == 0) {
                // No settings stored
                return;
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
    }

    private void updateMessage() {
        setMessage(Messages.ExportTracePackageWizardPage_ChooseContent);
    }

    private void createOptionsGroup(Composite parent) {
        // options group
        Group optionsGroup = new Group(parent, SWT.NONE);
        optionsGroup.setLayout(new RowLayout(SWT.VERTICAL));
        optionsGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL
                | GridData.GRAB_HORIZONTAL));
        optionsGroup.setText(Messages.ExportTracePackageWizardPage_Options);

        SelectionAdapter listener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateFileNameWithFormat();
            }
        };

        // create directory structure radios
        fZipFormatButton = new Button(optionsGroup, SWT.RADIO | SWT.LEFT);
        fZipFormatButton.setText(Messages.ExportTracePackageWizardPage_SaveInZipFormat);
        fZipFormatButton.setSelection(true);
        fZipFormatButton.addSelectionListener(listener);

        // create directory structure radios
        fTargzFormatButton = new Button(optionsGroup, SWT.RADIO | SWT.LEFT);
        fTargzFormatButton.setText(Messages.ExportTracePackageWizardPage_SaveInTarFormat);
        fTargzFormatButton.setSelection(false);
        fTargzFormatButton.addSelectionListener(listener);

        fCompressContentsCheckbox = new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
        fCompressContentsCheckbox.setText(Messages.ExportTracePackageWizardPage_CompressContents);
        fCompressContentsCheckbox.setSelection(true);
        fCompressContentsCheckbox.addSelectionListener(listener);
    }

    private void createTraceElementsGroup(Composite parent) {
        fTraceExportElementViewer = new CheckboxTreeViewer(parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.CHECK);

        fTraceExportElementViewer.addCheckStateListener(new ICheckStateListener() {
            @Override
            public void checkStateChanged(CheckStateChangedEvent event) {
                TracePackageElement element = (TracePackageElement) event.getElement();
                if (!element.isEnabled()) {
                    fTraceExportElementViewer.setChecked(element, element.isChecked());
                } else {
                    setSubtreeChecked(element, true, event.getChecked());
                }

                maintainCheckIntegrity(element);
                updateApproximateSize();
                updatePageCompletion();
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
        });

        GridData layoutData = new GridData(GridData.FILL_BOTH);
        fTraceExportElementViewer.getTree().setHeaderVisible(true);
        fTraceExportElementViewer.getTree().setLayoutData(layoutData);
        fTraceExportElementViewer.setContentProvider(new TracePackageContentProvider());
        fTraceExportElementViewer.setLabelProvider(new TracePackageLabelProvider());

        // Content column
        TreeViewerColumn column = new TreeViewerColumn(fTraceExportElementViewer, SWT.NONE);
        column.getColumn().setWidth(CONTENT_COL_WIDTH);
        column.getColumn().setText(Messages.ExportTracePackageWizardPage_ContentColumnName);
        column.setLabelProvider(new TracePackageLabelProvider());

        // Size column
        column = new TreeViewerColumn(fTraceExportElementViewer, SWT.NONE);
        column.getColumn().setWidth(SIZE_COL_WIDTH);
        column.getColumn().setText(Messages.ExportTracePackageWizardPage_SizeColumnName);
        column.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                TracePackageElement tracePackageElement = (TracePackageElement) element;
                long size = tracePackageElement.getSize(false);
                if (size == 0) {
                    return null;
                }
                int level = 0;
                TracePackageElement curElement = tracePackageElement.getParent();
                while (curElement != null) {
                    curElement = curElement.getParent();
                    ++level;
                }

                return indent(getHumanReadable(size), level);
            }

            private String indent(String humanReadable, int level) {
                StringBuilder s = new StringBuilder(humanReadable);
                for (int i = 0; i < level; ++i) {
                    final String indentStr = "  "; //$NON-NLS-1$
                    s.insert(0, indentStr);
                }
                return s.toString();
            }
        });

        setTraceElementsInput();
        fTraceExportElementViewer.expandToLevel(2);

        setAllChecked(false, true);
    }

    private void updateApproximateSize() {
        long checkedSize = 0;
        TracePackageElement[] tracePackageElements = (TracePackageElement[]) fTraceExportElementViewer.getInput();
        for (TracePackageElement element : tracePackageElements) {
            checkedSize += element.getSize(true);
        }
        checkedSize = Math.max(0, checkedSize);
        fApproximateSizeLabel.setText(Messages.ExportTracePackageWizardPage_ApproximateSizeLbl + getHumanReadable(checkedSize));
    }

    /**
     * Get the human readable string for a size in bytes. (KB, MB, etc).
     *
     * @param size
     *            the size to print in human readable,
     * @return the human readable string
     */
    private static String getHumanReadable(long size) {
        String humanSuffix[] = { Messages.ExportTracePackageWizardPage_SizeByte, Messages.ExportTracePackageWizardPage_SizeKilobyte,
                Messages.ExportTracePackageWizardPage_SizeMegabyte, Messages.ExportTracePackageWizardPage_SizeGigabyte,
                Messages.ExportTracePackageWizardPage_SizeTerabyte };
        long curSize = size;

        int suffixIndex = 0;
        while (curSize >= 1024) {
            curSize /= 1024;
            ++suffixIndex;
        }

        return Long.toString(curSize) + " " + humanSuffix[suffixIndex]; //$NON-NLS-1$
    }

    /**
     * A version of setSubtreeChecked that is aware of isEnabled
     *
     * @param element
     *            the element
     * @param state
     *            true if the item should be checked, and false if it should be
     *            unchecked
     * @param checked
     */
    private void setSubtreeChecked(TracePackageElement element, boolean enabledOnly, boolean checked) {
        if (!enabledOnly || element.isEnabled()) {
            fTraceExportElementViewer.setChecked(element, checked);
            element.setChecked(checked);
            if (element.getChildren() != null) {
                for (TracePackageElement child : element.getChildren()) {
                    setSubtreeChecked(child, enabledOnly, checked);
                }
            }
        }
    }

    private void setTraceElementsInput() {
        Object[] selectedElements = fSelection.toArray();
        List<TracePackageTraceElement> traceElements = new ArrayList<TracePackageTraceElement>();
        for (Object selectedElement : selectedElements) {
            if (selectedElement instanceof TmfTraceElement) {
                TmfTraceElement tmfTraceElement = (TmfTraceElement) selectedElement;
                TracePackageTraceElement traceElement = new TracePackageTraceElement(null, tmfTraceElement);

                // Trace files
                List<TracePackageElement> children = new ArrayList<TracePackageElement>();
                TracePackageFilesElement filesElement = new TracePackageFilesElement(traceElement, tmfTraceElement.getResource());
                filesElement.setChecked(true);
                // Always export the files
                filesElement.setEnabled(false);
                children.add(filesElement);

                // Supplementary files
                IResource[] supplementaryResources = tmfTraceElement.getSupplementaryResources();
                List<TracePackageElement> suppFilesChildren = new ArrayList<TracePackageElement>();
                TracePackageSupplFilesElement suppFilesElement = new TracePackageSupplFilesElement(traceElement);
                children.add(suppFilesElement);
                for (IResource res : supplementaryResources) {
                    suppFilesChildren.add(new TracePackageSupplFileElement(res, suppFilesElement));
                }
                suppFilesElement.setChildren(suppFilesChildren.toArray(new TracePackageElement[] {}));

                // Bookmarks
                children.add(new TracePackageBookmarkElement(traceElement, null));

                traceElement.setChildren(children.toArray(new TracePackageElement[] {}));

                traceElements.add(traceElement);
            }
        }

        fTraceExportElementViewer.setInput(traceElements.toArray(new TracePackageTraceElement[] {}));
    }

    /**
     * Creates the buttons for selecting all or none of the elements.
     *
     * @param parent
     *            the parent control
     */
    private final void createButtonsGroup(Composite parent) {

        // top level group
        Composite buttonComposite = new Composite(parent, SWT.NONE);

        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        buttonComposite.setLayout(layout);
        buttonComposite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL
                | GridData.HORIZONTAL_ALIGN_FILL));

        fSelectAllButton = new Button(buttonComposite, SWT.PUSH);
        fSelectAllButton.setText(org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.Messages.TracePackage_SelectAll);

        SelectionListener listener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setAllChecked(true, true);
                updateApproximateSize();
                updatePageCompletion();
            }
        };
        fSelectAllButton.addSelectionListener(listener);

        fDeselectAllButton = new Button(buttonComposite, SWT.PUSH);
        fDeselectAllButton.setText(org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.Messages.TracePackage_DeselectAll);

        listener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setAllChecked(true, false);
                updateApproximateSize();
                updatePageCompletion();
            }
        };
        fDeselectAllButton.addSelectionListener(listener);

        fApproximateSizeLabel = new Label(buttonComposite, SWT.SINGLE | SWT.RIGHT);
        GridData layoutData = new GridData(GridData.FILL_HORIZONTAL);
        layoutData.grabExcessHorizontalSpace = true;
        fApproximateSizeLabel.setLayoutData(layoutData);
    }

    /**
     * Sets all items in the element viewer to be checked or unchecked
     *
     * @param checked
     *            whether or not items should be checked
     */
    private void setAllChecked(boolean enabledOnly, boolean checked) {
        TreeItem[] items = fTraceExportElementViewer.getTree().getItems();
        for (int i = 0; i < items.length; i++) {
            Object element = items[i].getData();
            setSubtreeChecked((TracePackageElement) element, enabledOnly, checked);
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
        return validateSourceGroup() && validateDestinationGroup();
    }

    private boolean validateSourceGroup() {
        boolean traceElementSelected = fTraceExportElementViewer.getCheckedElements().length > 0;
        return traceElementSelected;
    }

    private boolean validateDestinationGroup() {
        boolean traceElementSelected = !getDestinationValue().isEmpty();
        return traceElementSelected;
    }

    private void createDestinationGroup(Composite parent) {

        Composite destinationSelectionGroup = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        destinationSelectionGroup.setLayout(layout);
        destinationSelectionGroup.setLayoutData(new GridData(
                GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL));

        Label destinationLabel = new Label(destinationSelectionGroup, SWT.NONE);
        destinationLabel.setText(Messages.ExportTracePackageWizardPage_ToArchive);

        // destination name entry field
        fDestinationNameField = new Combo(destinationSelectionGroup, SWT.SINGLE
                | SWT.BORDER);
        GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL
                | GridData.GRAB_HORIZONTAL);
        data.grabExcessHorizontalSpace = true;
        data.widthHint = 50;
        fDestinationNameField.setLayoutData(data);

        // destination browse button
        fDestinationBrowseButton = new Button(destinationSelectionGroup,
                SWT.PUSH);
        fDestinationBrowseButton.setText(org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.Messages.TracePackage_Browse);
        fDestinationBrowseButton.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                handleDestinationBrowseButtonPressed();
            }
        });
        setButtonLayoutData(fDestinationBrowseButton);

        new Label(parent, SWT.NONE); // vertical spacer
    }

    private String getDestinationValue() {
        return fDestinationNameField.getText().trim();
    }

    /**
     * Open an appropriate destination browser so that the user can specify a
     * source to import from
     */
    private void handleDestinationBrowseButtonPressed() {
        FileDialog dialog = new FileDialog(getContainer().getShell(), SWT.SAVE | SWT.SHEET);
        dialog.setFilterExtensions(new String[] { "*.zip;*.tar.gz;*.tar;*.tgz", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
        dialog.setText(Messages.ExportTracePackageWizardPage_SelectDestinationTitle);
        String currentSourceString = getDestinationValue();
        int lastSeparatorIndex = currentSourceString.lastIndexOf(File.separator);
        if (lastSeparatorIndex != -1) {
            dialog.setFilterPath(currentSourceString.substring(0, lastSeparatorIndex));
        }
        String selectedFileName = dialog.open();

        if (selectedFileName != null) {
            setDestinationValue(selectedFileName);
            updateFileNameWithFormat();
        }
    }

    private void setDestinationValue(String value) {
        fDestinationNameField.setText(value);
        updatePageCompletion();
    }

    private void handleError(String message, Throwable exception) {
        Activator.getDefault().logError(message, exception);

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
        ErrorDialog.openError(getContainer().getShell(), org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.Messages.TracePackage_InternalErrorTitle, message, s);
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
            String[] directoryNames = settings.getArray(STORE_DESTINATION_NAMES_ID);
            if (directoryNames == null) {
                directoryNames = new String[0];
            }

            directoryNames = addToHistory(directoryNames, getDestinationValue());
            settings.put(STORE_DESTINATION_NAMES_ID, directoryNames);

            settings.put(STORE_COMPRESS_CONTENTS_ID, fCompressContentsCheckbox.getSelection());
            settings.put(STORE_FORMAT_ID, fZipFormatButton.getSelection());
        }
    }

    private String getOutputExtension() {
        if (fZipFormatButton.getSelection()) {
            return ZIP_EXTENSION;
        } else if (fCompressContentsCheckbox.getSelection()) {
            return TAR_GZ_EXTENSION;
        } else {
            return TAR_EXTENSION;
        }
    }

    private void updateFileNameWithFormat() {
        String destinationValue = getDestinationValue();
        if (destinationValue.isEmpty()) {
            return;
        }

        destinationValue = stripKnownExtension(destinationValue);
        destinationValue = destinationValue.concat(getOutputExtension());

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
     * Finish the wizard page
     *
     * @return true on success
     */
    public boolean finish() {

        saveWidgetValues();

        TracePackageTraceElement[] traceExportElements = (TracePackageTraceElement[]) fTraceExportElementViewer.getInput();
        boolean useCompression = fCompressContentsCheckbox.getSelection();
        boolean useTar = fTargzFormatButton.getSelection();
        String fileName = getDestinationValue();
        final TracePackageExportOperation exporter = new TracePackageExportOperation(traceExportElements, useCompression, useTar, fileName);

        try {
            getContainer().run(true, true, new IRunnableWithProgress() {

                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    exporter.run(monitor);
                }
            });

            IStatus status = exporter.getStatus();
            if (status.getSeverity() == IStatus.ERROR) {
                String message = status.getMessage().length() > 0 ? status.getMessage() : org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.Messages.TracePackage_ErrorOperation;
                handleError(message, status.getException());
            }

        } catch (InvocationTargetException e) {
            handleError(org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.Messages.TracePackage_ErrorOperation, e);
        } catch (InterruptedException e) {
        }

        return exporter.getStatus().getSeverity() == IStatus.OK;
    }

}
