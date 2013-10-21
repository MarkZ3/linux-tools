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

package org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.linuxtools.internal.tmf.ui.Activator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeItem;

/**
 * An abstract wizard containing common code useful for both import and
 * export trace package wizards
 *
 * @author Marc-Andre Laperle
 */
abstract public class AbstractTracePackageWizard extends WizardPage {

    private static final int COMBO_HISTORY_LENGTH = 5;

    private final IStructuredSelection fSelection;

    protected CheckboxTreeViewer fTraceExportElementViewer;
    private Button fSelectAllButton;
    private Button fDeselectAllButton;
//    private Combo fDestinationNameField;
//
//    private final static String STORE_DESTINATION_NAMES_ID = PAGE_NAME + ".STORE_DESTINATION_NAMES_ID"; //$NON-NLS-1$

//    /**
//     * Restore widget values to the values that they held last time this wizard
//     * was used to completion.
//     */
//    private void restoreWidgetValues() {
//        IDialogSettings settings = getDialogSettings();
//        if (settings != null) {
//            String[] directoryNames = settings.getArray(STORE_DESTINATION_NAMES_ID);
//            if (directoryNames == null || directoryNames.length == 0) {
//                // No settings stored
//                return;
//            }
//
//            // destination
//            for (int i = 0; i < directoryNames.length; i++) {
//                fDestinationNameField.add(directoryNames[i]);
//            }
//        }
//    }
//


    /**
     * Determine if the page is complete and update the page appropriately.
     */
    protected void updatePageCompletion() {
        boolean pageComplete = determinePageCompletion();
        setPageComplete(pageComplete);
        if (pageComplete) {
            setErrorMessage(null);
        }
    }

    abstract protected boolean determinePageCompletion();

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
    protected static void setSubtreeChecked(CheckboxTreeViewer viewer, TracePackageElement element, boolean enabledOnly, boolean checked) {
        if (!enabledOnly || element.isEnabled()) {
            viewer.setChecked(element, checked);
            element.setChecked(checked);
            if (element.getChildren() != null) {
                for (TracePackageElement child : element.getChildren()) {
                    setSubtreeChecked(viewer, child, enabledOnly, checked);
                }
            }
        }
    }

    /**
     * Sets all items in the element viewer to be checked or unchecked
     *
     * @param checked
     *            whether or not items should be checked
     */
    protected static void setAllChecked(CheckboxTreeViewer viewer, boolean enabledOnly, boolean checked) {
        TreeItem[] items = viewer.getTree().getItems();
        for (int i = 0; i < items.length; i++) {
            Object element = items[i].getData();
            setSubtreeChecked(viewer, (TracePackageElement) element, enabledOnly, checked);
        }
    }

    protected AbstractTracePackageWizard(String pageName, String title, ImageDescriptor titleImage, IStructuredSelection selection) {
        super(pageName, title, titleImage);
        fSelection = selection;
    }

    protected void handleError(String message, Throwable exception) {
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

    protected static void addToHistory(List<String> history, String newEntry) {
        history.remove(newEntry);
        history.add(0, newEntry);

        // since only one new item was added, we can be over the limit
        // by at most one item
        if (history.size() > COMBO_HISTORY_LENGTH) {
            history.remove(COMBO_HISTORY_LENGTH);
        }
    }

    protected IStructuredSelection getSelection() {
        return fSelection;
    }

    protected static String[] addToHistory(String[] history, String newEntry) {
        ArrayList<String> l = new ArrayList<String>(Arrays.asList(history));
        addToHistory(l, newEntry);
        String[] r = new String[l.size()];
        l.toArray(r);
        return r;
    }

    protected void createTraceElementsGroup(Composite parent) {
        fTraceExportElementViewer = new CheckboxTreeViewer(parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.CHECK);

        fTraceExportElementViewer.addCheckStateListener(new ICheckStateListener() {
            @Override
            public void checkStateChanged(CheckStateChangedEvent event) {
                TracePackageElement element = (TracePackageElement) event.getElement();
                if (!element.isEnabled()) {
                    fTraceExportElementViewer.setChecked(element, element.isChecked());
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
        fTraceExportElementViewer.getTree().setLayoutData(layoutData);
        fTraceExportElementViewer.setContentProvider(new TracePackageContentProvider());
        fTraceExportElementViewer.setLabelProvider(new TracePackageLabelProvider());
    }

    /**
     * Creates the buttons for selecting all or none of the elements.
     *
     * @param parent
     *            the parent control
     * @return the button group
     */
    protected Composite createButtonsGroup(Composite parent) {

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
                setAllChecked(fTraceExportElementViewer, true, true);
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
                setAllChecked(fTraceExportElementViewer, true, false);
                updateApproximateSize();
                updatePageCompletion();
            }
        };
        fDeselectAllButton.addSelectionListener(listener);

        return buttonComposite;
    }

    protected void updateApproximateSize() {
    }
}
