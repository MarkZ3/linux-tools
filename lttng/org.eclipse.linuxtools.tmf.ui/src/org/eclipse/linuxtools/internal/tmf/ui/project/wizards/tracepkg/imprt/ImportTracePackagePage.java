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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
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
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageBookmarkElement.BookmarkInfo;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageContentProvider;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageElement;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageLabelProvider;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageTraceElement;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfTraceElement;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfTraceFolder;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfTraceType;
import org.eclipse.linuxtools.tmf.ui.project.model.TraceTypeHelper;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TreeItem;

/**
 * Wizard page for the import trace package wizard
 *
 * @author Marc-Andre Laperle
 */
public class ImportTracePackagePage extends WizardPage {

    static private final String ICON_PATH = "icons/wizban/trace_import_wiz.png"; //$NON-NLS-1$
    private static final int SIZING_TEXT_FIELD_WIDTH = 250;
    private static final String PAGE_NAME = "ImportTracePackagePage1"; //$NON-NLS-1$
    private final static String STORE_SOURCE_NAMES_ID = PAGE_NAME + ".STORE_SOURCE_NAMES_ID"; //$NON-NLS-1$
    private static final int COMBO_HISTORY_LENGTH = 5;

    IStructuredSelection fSelection;
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
        super(PAGE_NAME, Messages.ImportTracePkgPage_title, Activator.getDefault().getImageDescripterFromPath(ICON_PATH));
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

        createSourceGroup(composite);
        createTraceElementsGroup(composite);
        createButtonsGroup(composite);

        restoreWidgetValues();

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
                IDialogConstants.SELECT_ALL_ID, org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.Messages.TracePackage_SelectAll, false);

        SelectionListener listener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setAllChecked(true);
                updatePageCompletion();
            }
        };
        fSelectAllButton.addSelectionListener(listener);
        fSelectAllButton.setFont(font);
        setButtonLayoutData(fSelectAllButton);

        fDeselectAllButton = createButton(buttonComposite,
                IDialogConstants.DESELECT_ALL_ID, org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.Messages.TracePackage_DeselectAll, false);

        listener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setAllChecked(false);
                updatePageCompletion();
            }
        };
        fDeselectAllButton.addSelectionListener(listener);
        fDeselectAllButton.setFont(font);
        setButtonLayoutData(fDeselectAllButton);
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
    }

    private void setTraceElements() {

        final TracePackageImportOperation op = new TracePackageImportOperation(getSourceValue());

        try {
            getContainer().run(true, true, new IRunnableWithProgress() {

                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    monitor.beginTask(Messages.TraceImporter_ReadingPackage, 10);
                    op.runExtractManifestOperation(monitor);
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

        if (op.getResultElement() != null) {
            fTraceExportElementViewer.setInput(new TracePackageElement[] { op.getResultElement() });
        }
    }

    private void setAllChecked(boolean checked) {
        TreeItem[] items = fTraceExportElementViewer.getTree().getItems();
        for (int i = 0; i < items.length; i++) {
            Object element = items[i].getData();
            fTraceExportElementViewer.setSubtreeChecked(element, checked);
        }
    }

    private void createSourceGroup(Composite parent) {

        Font font = parent.getFont();
        // source specification group
        Composite sourceSelectionGroup = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        sourceSelectionGroup.setLayout(layout);
        sourceSelectionGroup.setLayoutData(new GridData(
                GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL));
        sourceSelectionGroup.setFont(font);

        Label sourceLabel = new Label(sourceSelectionGroup, SWT.NONE);
        sourceLabel.setText(Messages.FileImport_FromArchive);
        sourceLabel.setFont(font);

        // source name entry field
        fSourceNameField = new Combo(sourceSelectionGroup, SWT.SINGLE
                | SWT.BORDER);
        GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL
                | GridData.GRAB_HORIZONTAL);
        data.widthHint = SIZING_TEXT_FIELD_WIDTH;
        fSourceNameField.setLayoutData(data);
        fSourceNameField.setFont(font);
        fSourceNameField.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateWithSelection();
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
        fSourceBrowseButton.setFont(font);
        setButtonLayoutData(fSourceBrowseButton);

        new Label(parent, SWT.NONE); // vertical spacer
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
            setErrorMessage(null);
            setSourceValue(selectedFileName);

            updateWithSelection();
        }
    }

    private void updateWithSelection() {
        setTraceElements();
        fTraceExportElementViewer.expandAll();
        setAllChecked(true);
    }

    private void setSourceValue(String value) {
        fSourceNameField.setText(value);
        updatePageCompletion();
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
        return validateTraceElementsGroup() && validateSourceGroup();
    }

    private boolean validateTraceElementsGroup() {
        boolean traceElementSelected = fTraceExportElementViewer.getCheckedElements().length > 0;
        return traceElementSelected;
    }

    private boolean validateSourceGroup() {
        boolean traceElementSelected = !getSourceValue().isEmpty();
        return traceElementSelected;
    }

    /**
     * Answer the contents of self's source specification widget. If this
     * value does not have a suffix then add it first.
     */
    private String getSourceValue() {
        return fSourceNameField.getText().trim();
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

            setSourceValue(directoryNames[0]);
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

    private void handleError(String message, Throwable exception) {
        Activator.getDefault().logError(message, exception);

        displayErrorDialog(message, exception);
    }

    private void displayErrorDialog(String message, Throwable exception) {
        if (exception == null) {
            final Status s = new Status(IStatus.ERROR, Activator.PLUGIN_ID, message);
            ErrorDialog.openError(getContainer().getShell(), org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.Messages.TracePackage_InternalErrorTitle, null, s);
            return;
        }

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

    /**
     * Finish the wizard page
     *
     * @return true on success
     */
    public boolean finish() {
        saveWidgetValues();

        String fileName = getSourceValue();
        TmfTraceFolder tmfTraceFolder = (TmfTraceFolder) fSelection.getFirstElement();

        TracePackageElement[] input = (TracePackageElement[]) fTraceExportElementViewer.getInput();
        TracePackageTraceElement exportTraceTraceElement = (TracePackageTraceElement) input[0];
        final TracePackageImportOperation exporter = new TracePackageImportOperation(fileName, exportTraceTraceElement, tmfTraceFolder);

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

        IResource traceRes = tmfTraceFolder.getResource().findMember(exportTraceTraceElement.getText());

        TraceTypeHelper traceType = TmfTraceType.getInstance().getTraceType(exportTraceTraceElement.getTraceType());
        try {
            TmfTraceType.setTraceType(traceRes.getFullPath(), traceType);
        } catch (CoreException e) {
            // Only log errors from this point because they are non-fatal
            Activator.getDefault().logError(MessageFormat.format(Messages.ImportTracePackagePage_ErrorSettingTraceType, traceType.getCanonicalName(), traceRes.getName()), e);
        }

        // Add bookmarks
        Object[] checkedElements = fTraceExportElementViewer.getCheckedElements();
        for (Object o : checkedElements) {
            if (o instanceof TracePackageBookmarkElement) {

                // Get element
                IFile bookmarksFile = null;
                List<TmfTraceElement> traces = tmfTraceFolder.getTraces();
                for (TmfTraceElement t : traces) {
                    if (t.getName().equals(traceRes.getName())) {
                        try {
                            bookmarksFile = t.createBookmarksFile();
                        } catch (CoreException e) {
                            Activator.getDefault().logError(MessageFormat.format(Messages.ImportTracePackagePage_ErrorCreatingBookmarkFile, traceRes.getName()), e);
                        }
                        break;
                    }
                }

                if (bookmarksFile == null) {
                    break;
                }

                TracePackageBookmarkElement exportTraceBookmarkElement = (TracePackageBookmarkElement) o;

                List<TracePackageBookmarkElement.BookmarkInfo> bookmarks = exportTraceBookmarkElement.getBookmarks();
                for (BookmarkInfo attrs : bookmarks) {
                    IMarker createMarker = null;
                    try {
                        createMarker = bookmarksFile.createMarker(IMarker.BOOKMARK);
                    } catch (CoreException e) {
                        Activator.getDefault().logError(MessageFormat.format(Messages.ImportTracePackagePage_ErrorCreatingBookmark, traceRes.getName()), e);
                    }
                    if (createMarker != null && createMarker.exists()) {
                        try {
                            createMarker.setAttribute(IMarker.MESSAGE, attrs.getMessage());
                            createMarker.setAttribute(IMarker.LOCATION, attrs.getLocation());
                        } catch (CoreException e) {
                            Activator.getDefault().logError(MessageFormat.format(Messages.ImportTracePackagePage_ErrorCreatingBookmark, traceRes.getName()), e);
                        }

                    }

                }

            }
        }

        return exporter.getStatus().getSeverity() == IStatus.OK;
    }

}
