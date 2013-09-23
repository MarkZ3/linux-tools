package org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.imprt;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

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
import org.eclipse.jface.operation.ModalContext;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.linuxtools.internal.tmf.ui.Activator;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.ExportTraceBookmarkElement;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.ExportTraceBookmarkElement.BookmarkInfo;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.ExportTraceContentProvider;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.ExportTraceElement;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.ExportTraceFilesElement;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.ExportTraceLabelProvider;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.ExportTraceSupplFilesElement;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.ExportTraceTraceElement;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.ITracePackageConstants;
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
import org.eclipse.ui.internal.wizards.datatransfer.ArchiveFileManipulations;
import org.eclipse.ui.internal.wizards.datatransfer.TarEntry;
import org.eclipse.ui.internal.wizards.datatransfer.TarException;
import org.eclipse.ui.internal.wizards.datatransfer.TarFile;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@SuppressWarnings("restriction")
public class ImportTracePackagePage extends WizardPage {

    static private final String ICON_PATH = "icons/wizban/trace_import_wiz.png"; //$NON-NLS-1$
    private static final int SIZING_TEXT_FIELD_WIDTH = 250;
    private final static String STORE_SOURCE_NAMES_ID = "ImportTracePackagePage1.STORE_SOURCE_NAMES_ID"; //$NON-NLS-1$
    private static final int COMBO_HISTORY_LENGTH = 5;
    private static boolean isTarFile;

    IStructuredSelection fSelection;
    private CheckboxTreeViewer fTraceExportElementViewer;

    private Combo fDestinationNameField;
    private Button fSelectAllButton;
    private Button fDeselectAllButton;

    private Button fDestinationBrowseButton;

    public ImportTracePackagePage(String pageName, IStructuredSelection selection) {
        super(pageName, Messages.ImportTracePkgPage_title, Activator.getDefault().getImageDescripterFromPath(ICON_PATH));
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

        createDestinationGroup(composite);
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
                IDialogConstants.SELECT_ALL_ID, Messages.SelectAll, false);

        SelectionListener listener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setAllChecked(true);
                // updateWidgetEnablements();
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
                // updateWidgetEnablements();
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
                // updatePageCompletion();
            }
        });
        GridData layoutData = new GridData(GridData.FILL_BOTH);
        fTraceExportElementViewer.getTree().setLayoutData(layoutData);
        fTraceExportElementViewer.setContentProvider(new ExportTraceContentProvider());
        fTraceExportElementViewer.setLabelProvider(new ExportTraceLabelProvider());
    }

    private void setTraceElements() {

        // disposeStructureProvider();
        class Container {
            public ExportTraceElement element;
        }
        final Container c = new Container();
        final String fileName = getDestinationValue();

        try {
            getContainer().run(true, true, new IRunnableWithProgress() {

                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    monitor.beginTask("Reading package", 10);
                    c.element = extractManifest(monitor, fileName);
                    monitor.done();
                }

            });
        } catch (InvocationTargetException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (InterruptedException e1) {
            // Canceled
        }

        ExportTraceElement element = c.element;

        if (element != null) {
            fTraceExportElementViewer.setInput(new ExportTraceElement[] { element });
        }
    }

    private static ExportTraceElement extractManifest(IProgressMonitor monitor, String fileName) throws InterruptedException {
        ExportTraceElement element = null;
        isTarFile = ArchiveFileManipulations.isTarFile(fileName);
        monitor.worked(1);
        if (isTarFile) {
            TarFile sourceTarFile = getSpecifiedTarSourceFile(fileName);
            monitor.worked(1);
            if (sourceTarFile == null) {
                return null;
            }

            Enumeration<?> entries = sourceTarFile.entries();

            while (entries.hasMoreElements()) {
                ModalContext.checkCanceled(monitor);

                TarEntry entry = (TarEntry) entries.nextElement();
                if (entry.getName().equalsIgnoreCase(ITracePackageConstants.MANIFEST_FILENAME)) {
                    try {
                        InputStream inputStream = sourceTarFile.getInputStream(entry);
                        element = loadElementsFromManifest(inputStream);

                    } catch (TarException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    break;
                }

                monitor.worked(1);
            }

            // sourceTarFile.
            // ExportTraceTraceElement element ==
            // element =
            // return selectFiles(structureProvider.getRoot(),
            // structureProvider);
        }
        return element;
    }

    private static ExportTraceElement loadElementsFromManifest(InputStream inputStream) {
        ExportTraceElement element = null;
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);

            // TODO: validate file

            NodeList traceElements = doc.getDocumentElement().getElementsByTagName(ITracePackageConstants.TRACE_ELEMENT);
            for (int i = 0; i < traceElements.getLength(); ++i) {
                Node traceNode = traceElements.item(i);
                if (traceNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element traceElement = (Element) traceNode;
                    String traceName = traceElement.getAttribute(ITracePackageConstants.TRACE_NAME_ATTRIB);
                    String traceType = traceElement.getAttribute(ITracePackageConstants.TRACE_TYPE_ATTRIB);
                    element = new ExportTraceTraceElement(null, traceName, traceType);

                    List<ExportTraceElement> children = new ArrayList<ExportTraceElement>();
                    NodeList fileElements = traceElement.getElementsByTagName(ITracePackageConstants.TRACE_FILE_ELEMENT);
                    for (int j = 0; j < fileElements.getLength(); ++j) {
                        Node fileNode = fileElements.item(j);
                        if (fileNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element fileElement = (Element) fileNode;
                            String fileName = fileElement.getAttribute(ITracePackageConstants.TRACE_FILE_NAME_ATTRIB);
                            children.add(new ExportTraceFilesElement(element, fileName));
                        }
                    }

                    List<String> suppFiles = new ArrayList<String>();
                    NodeList suppFilesElements = traceElement.getElementsByTagName(ITracePackageConstants.SUPPLEMENTARY_FILE_ELEMENT);
                    for (int j = 0; j < suppFilesElements.getLength(); ++j) {
                        Node suppFileNode = suppFilesElements.item(j);
                        if (suppFileNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element suppFileElement = (Element) suppFileNode;
                            suppFiles.add(suppFileElement.getAttribute(ITracePackageConstants.SUPPLEMENTARY_FILE_NAME_ATTRIB));
                        }
                    }

                    if (!suppFiles.isEmpty()) {
                        children.add(new ExportTraceSupplFilesElement(suppFiles, element));
                    }

                    List<Map<String, String>> bookmarks = new ArrayList<Map<String, String>>();
                    List<ExportTraceBookmarkElement.BookmarkInfo> bookmarkInfos = new ArrayList<ExportTraceBookmarkElement.BookmarkInfo>();
                    NodeList bookmarksElements = traceElement.getElementsByTagName(ITracePackageConstants.BOOKMARKS_ELEMENT);
                    for (int j = 0; j < bookmarksElements.getLength(); ++j) {
                        Node bookmarksNode = bookmarksElements.item(j);
                        if (bookmarksNode.getNodeType() == Node.ELEMENT_NODE) {
                            NodeList bookmarkElements = traceElement.getElementsByTagName(ITracePackageConstants.BOOKMARK_ELEMENT);
                            for (int k = 0; k < bookmarkElements.getLength(); ++k) {
                                Node bookmarkNode = bookmarkElements.item(k);
                                if (bookmarkNode.getNodeType() == Node.ELEMENT_NODE) {
                                    Element bookmarkElement = (Element) bookmarkNode;
                                    NamedNodeMap attributesMap = bookmarkElement.getAttributes();
                                    Node locationNode = attributesMap.getNamedItem(IMarker.LOCATION);
                                    Node messageNode = attributesMap.getNamedItem(IMarker.MESSAGE);

                                    if (locationNode != null && messageNode != null) {
                                        Attr locationAttr = (Attr) locationNode;
                                        Integer location = Integer.valueOf(locationAttr.getValue());
                                        Attr messageAttr = (Attr) messageNode;
                                        bookmarkInfos.add(new ExportTraceBookmarkElement.BookmarkInfo(location, messageAttr.getValue()));

                                    }
                                    // Map<String, String> attributes = new
                                    // HashMap<String, String>();
                                    // for (int l = 0; l <
                                    // attributesMap.getLength(); ++l) {
                                    // Attr attr = (Attr)attributesMap.item(l);
                                    // attributes.put(attr.getName(),
                                    // attr.getNodeValue());
                                    // }

                                    // bookmarks.add(attributes);
                                }
                            }
                            // bookmarks
                        }
                    }
                    if (!bookmarkInfos.isEmpty()) {
                        children.add(new ExportTraceBookmarkElement(element, bookmarks, bookmarkInfos));
                    }

                    element.setChildren(children.toArray(new ExportTraceElement[] {}));
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return element;
    }

    /**
     * Answer a handle to the zip file currently specified as being the source.
     * Return null if this file does not exist or is not of valid format.
     */
    private static TarFile getSpecifiedTarSourceFile(String fileName) {
        if (fileName.length() == 0) {
            return null;
        }

        try {
            return new TarFile(fileName);
        } catch (TarException e) {
            // ignore
        } catch (IOException e) {
            // ignore
        }

        // sourceNameField.setFocus();
        return null;
    }

    private void setAllChecked(boolean checked) {
        TreeItem[] items = fTraceExportElementViewer.getTree().getItems();
        for (int i = 0; i < items.length; i++) {
            Object element = items[i].getData();
            fTraceExportElementViewer.setSubtreeChecked(element, checked);
        }
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
        fDestinationNameField.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateWithSelection();
            }
        });

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

    /**
     * Open an appropriate destination browser so that the user can specify a
     * source to import from
     */
    private void handleDestinationBrowseButtonPressed() {
        FileDialog dialog = new FileDialog(getContainer().getShell(), SWT.OPEN | SWT.SHEET);
        dialog.setFilterExtensions(new String[] { "*.zip;*.tar.gz;*.tar;*.tgz", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
        dialog.setText(Messages.ArchiveImport_selectSourceTitle);
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

            updateWithSelection();
        }
    }

    private void updateWithSelection() {
        setTraceElements();
        fTraceExportElementViewer.expandAll();
        setAllChecked(true);
    }

    private void setDestinationValue(String value) {
        fDestinationNameField.setText(value);
    }

    /**
     * Answer the contents of self's destination specification widget. If this
     * value does not have a suffix then add it first.
     */
    private String getDestinationValue() {
        String destinationText = fDestinationNameField.getText().trim();

        return destinationText;
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

            // destination
            //setDestinationValue(directoryNames[0]);
            for (int i = 0; i < directoryNames.length; i++) {
                fDestinationNameField.add(directoryNames[i]);
            }
        }

        updateMessage();
    }

    private void updateMessage() {
        setMessage("Choose the content to import");
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

            directoryNames = addToHistory(directoryNames, getDestinationValue());
            settings.put(STORE_SOURCE_NAMES_ID, directoryNames);
        }
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
        ErrorDialog.openError(getContainer().getShell(), Messages.ImportTracePackagePage_internalErrorTitle, message, s);
    }

    public boolean finish() {
        saveWidgetValues();

        String fileName = getDestinationValue();

        // TODO: should always be true?
        // if (fSelection.getFirstElement() instanceof TmfTraceFolder) {
        TmfTraceFolder tmfTraceFolder = (TmfTraceFolder) fSelection.getFirstElement();
        // }

        ExportTraceElement[] input = (ExportTraceElement[]) fTraceExportElementViewer.getInput();
        ExportTraceTraceElement exportTraceTraceElement = (ExportTraceTraceElement) input[0];
        final TraceImporter exporter = new TraceImporter(fileName, isTarFile, exportTraceTraceElement, tmfTraceFolder);

        try {
            getContainer().run(true, true, new IRunnableWithProgress() {

                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    exporter.doImport(monitor);
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

        IStatus ret = Status.OK_STATUS;
        try {
            TraceTypeHelper traceType = TmfTraceType.getInstance().getTraceType(exportTraceTraceElement.getTraceType());
            ret = TmfTraceType.setTraceType(traceRes.getFullPath(), traceType);
        } catch (CoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // Add bookmarks
        Object[] checkedElements = fTraceExportElementViewer.getCheckedElements();

        // FIXME: Will not work for experiments
        for (Object o : checkedElements) {
            if (o instanceof ExportTraceBookmarkElement) {

                // Get element
                IFile bookmarksFile = null;
                List<TmfTraceElement> traces = tmfTraceFolder.getTraces();
                for (TmfTraceElement t : traces) {
                    if (t.getName().equals(traceRes.getName())) {
                        try {
                            bookmarksFile = t.createBookmarksFile();
                        } catch (CoreException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        break;
                    }
                }

                if (bookmarksFile == null) {
                    break;
                }

                ExportTraceBookmarkElement exportTraceBookmarkElement = (ExportTraceBookmarkElement) o;

                List<ExportTraceBookmarkElement.BookmarkInfo> bookmarks = exportTraceBookmarkElement.getBookmarkInfos();
                for (BookmarkInfo attrs : bookmarks) {
                    IMarker createMarker = null;
                    try {
                        createMarker = bookmarksFile.createMarker(IMarker.BOOKMARK);
                    } catch (CoreException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    if (createMarker != null && createMarker.exists()) {
                        try {
                            createMarker.setAttribute(IMarker.MESSAGE, attrs.messageAttr);
                            createMarker.setAttribute(IMarker.LOCATION, attrs.location);
                        } catch (CoreException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                    }

                }

            }
        }

        return exporter.getStatus().getSeverity() == IStatus.OK && ret.getSeverity() == IStatus.OK;
    }

}
