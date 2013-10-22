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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.linuxtools.internal.tmf.ui.Activator;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.AbstractTracePackageWizard;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageBookmarkElement;
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

/**
 * Wizard page for the export trace package wizard
 *
 * @author Marc-Andre Laperle
 */
public class ExportTracePackageWizardPage extends AbstractTracePackageWizard {

    private static final int CONTENT_COL_WIDTH = 300;
    private static final int SIZE_COL_WIDTH = 100;

    private static final String ZIP_EXTENSION = ".zip"; //$NON-NLS-1$
    private static final String TAR_EXTENSION = ".tar"; //$NON-NLS-1$
    private static final String TAR_GZ_EXTENSION = ".tar.gz"; //$NON-NLS-1$
    private static final String TGZ_EXTENSION = ".tgz"; //$NON-NLS-1$

    static private final String ICON_PATH = "icons/wizban/export_wiz.png"; //$NON-NLS-1$

    // dialog store id constants
    private static final String PAGE_NAME = "ExportTracePackageWizardPage1"; //$NON-NLS-1$
    private final static String STORE_COMPRESS_CONTENTS_ID = PAGE_NAME + ".STORE_COMPRESS_CONTENTS_ID"; //$NON-NLS-1$
    private final static String STORE_FORMAT_ID = "ExportTracePackageWizardPage1.STORE_FORMAT_ID"; //$NON-NLS-1$

    private Button fCompressContentsCheckbox;
    private Button fZipFormatButton;
    private Button fTargzFormatButton;
    private Label fApproximateSizeLabel;

    /**
     * Constructor for the export trace package wizard page
     *
     * @param selection
     *            the current object selection
     */
    public ExportTracePackageWizardPage(IStructuredSelection selection) {
        super(PAGE_NAME, Messages.ExportTracePackageWizardPage_Title, Activator.getDefault().getImageDescripterFromPath(ICON_PATH), selection);
    }

    @Override
    public void createControl(Composite parent) {

        initializeDialogUnits(parent);

        Composite composite = new Composite(parent, SWT.NULL);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));

        createElementViewer(composite);
        createButtonsGroup(composite);
        createFilePathGroup(composite, Messages.ExportTracePackageWizardPage_ToArchive);
        createOptionsGroup(composite);

        restoreWidgetValues();
        setMessage(Messages.ExportTracePackageWizardPage_ChooseContent);

        updateApproximateSelectedSize();
        updatePageCompletion();

        setControl(composite);
    }

    /**
     * Restore widget values to the values that they held last time this wizard
     * was used to completion.
     */
    @Override
    protected void restoreWidgetValues() {
        super.restoreWidgetValues();

        IDialogSettings settings = getDialogSettings();
        if (settings != null) {
            if (getFilePathCombo().getItemCount() > 0) {
                setFilePathValue(getFilePathCombo().getItem(0));
                updateWithFilePathSelection();
            }
            fCompressContentsCheckbox.setSelection(settings.getBoolean(STORE_COMPRESS_CONTENTS_ID));
            fZipFormatButton.setSelection(settings.getBoolean(STORE_FORMAT_ID));
            fTargzFormatButton.setSelection(!settings.getBoolean(STORE_FORMAT_ID));
        }
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
                updateWithFilePathSelection();
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

    @Override
    protected void createElementViewer(Composite parent) {
        super.createElementViewer(parent);

        CheckboxTreeViewer elementViewer = getElementViewer();
        elementViewer.getTree().setHeaderVisible(true);
        // Content column
        TreeViewerColumn column = new TreeViewerColumn(elementViewer, SWT.NONE);
        column.getColumn().setWidth(CONTENT_COL_WIDTH);
        column.getColumn().setText(Messages.ExportTracePackageWizardPage_ContentColumnName);
        column.setLabelProvider(new TracePackageLabelProvider());

        // Size column
        column = new TreeViewerColumn(elementViewer, SWT.NONE);
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

        elementViewer.setInput(createElementViewerInput());
        elementViewer.expandToLevel(2);
        setAllChecked(elementViewer, false, true);
    }

    @Override
    protected void updateApproximateSelectedSize() {
        long checkedSize = 0;
        TracePackageElement[] tracePackageElements = (TracePackageElement[]) getElementViewer().getInput();
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

    @Override
    protected Object createElementViewerInput() {
        Object[] selectedElements = getSelection().toArray();
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

        return traceElements.toArray(new TracePackageTraceElement[] {});
    }

    @Override
    protected final Composite createButtonsGroup(Composite parent) {
        Composite buttonGroup = super.createButtonsGroup(parent);

        // Add the label on the same row of the select/deselect all buttons
        fApproximateSizeLabel = new Label(buttonGroup, SWT.SINGLE | SWT.RIGHT);
        GridData layoutData = new GridData(GridData.FILL_HORIZONTAL);
        layoutData.grabExcessHorizontalSpace = true;
        fApproximateSizeLabel.setLayoutData(layoutData);

        return buttonGroup;
    }

    /**
     * Save widget values to Dialog settings
     */
    @Override
    protected void saveWidgetValues() {
        super.saveWidgetValues();

        IDialogSettings settings = getDialogSettings();
        if (settings != null) {
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

    @Override
    protected void updateWithFilePathSelection() {
        String filePathValue = getFilePathValue();
        if (filePathValue.isEmpty()) {
            return;
        }

        filePathValue = stripKnownExtension(filePathValue);
        filePathValue = filePathValue.concat(getOutputExtension());

        setFilePathValue(filePathValue);
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

        TracePackageTraceElement[] traceExportElements = (TracePackageTraceElement[]) getElementViewer().getInput();
        boolean useCompression = fCompressContentsCheckbox.getSelection();
        boolean useTar = fTargzFormatButton.getSelection();
        String fileName = getFilePathValue();
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
