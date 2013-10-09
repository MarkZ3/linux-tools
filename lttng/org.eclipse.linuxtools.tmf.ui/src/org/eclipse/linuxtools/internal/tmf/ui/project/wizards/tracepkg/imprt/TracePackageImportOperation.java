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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.ModalContext;
import org.eclipse.linuxtools.internal.tmf.ui.Activator;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.ITracePackageConstants;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageBookmarkElement;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageElement;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageFilesElement;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageSupplFileElement;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageSupplFilesElement;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageTraceElement;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfTraceElement;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfTraceFolder;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.internal.wizards.datatransfer.TarException;
import org.eclipse.ui.wizards.datatransfer.IImportStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * An operation that imports a trace package from an archive
 *
 * @author Marc-Andre Laperle
 */
@SuppressWarnings("restriction")
public class TracePackageImportOperation implements IOverwriteQuery {

    private final String fFileName;
    private final TracePackageTraceElement fImportTraceElement;
    private final TmfTraceFolder fTmfTraceFolder;
    private IStatus fStatus;

    // Result of reading the manifest
    private TracePackageElement fResultElement;

    /**
     * Constructs a new import operation
     *
     * @param importTraceElement
     *            the trace element to be imported
     * @param fileName
     *            the output file name
     * @param tmfTraceFolder
     *            the destination folder
     */
    public TracePackageImportOperation(String fileName, TracePackageTraceElement importTraceElement, TmfTraceFolder tmfTraceFolder) {
        fFileName = fileName;
        fImportTraceElement = importTraceElement;
        fTmfTraceFolder = tmfTraceFolder;
    }

    /**
     * Constructs a new import operation for reading the manifest
     *
     * @param fileName
     *            the output file name
     */
    public TracePackageImportOperation(String fileName) {
        fFileName = fileName;
        fImportTraceElement = null;
        fTmfTraceFolder = null;
    }

    private class ImportProvider implements IImportStructureProvider {

        private Exception fException;

        @Override
        public List getChildren(Object element) {
            return null;
        }

        @Override
        public InputStream getContents(Object element) {
            InputStream inputStream = null;
            // We can add throws
            try {
                inputStream = ((ArchiveProviderElement) element).getContents();
            } catch (IOException e) {
                fException = e;
            } catch (TarException e) {
                fException = e;
            }
            return inputStream;
        }

        @Override
        public String getFullPath(Object element) {
            return ((ArchiveProviderElement) element).getFullPath();
        }

        @Override
        public String getLabel(Object element) {
            return ((ArchiveProviderElement) element).getLabel();
        }

        @Override
        public boolean isFolder(Object element) {
            return ((ArchiveProviderElement) element).isFolder();
        }

        public Exception getException() {
            return fException;
        }
    }

    private class ArchiveProviderElement {

        private final String fPath;
        private final String fLabel;

        private ArchiveFile fArchiveFile;
        private ArchiveEntry fEntry;

        public ArchiveProviderElement(String destinationPath, String label, ArchiveFile archiveFile, ArchiveEntry entry) {
            fPath = destinationPath;
            fLabel = label;
            this.fArchiveFile = archiveFile;
            this.fEntry = entry;
        }

        public InputStream getContents() throws TarException, IOException {
            return fArchiveFile.getInputStream(fEntry);
        }

        public String getFullPath() {
            return fPath;
        }

        public String getLabel() {
            return fLabel;
        }

        public boolean isFolder() {
            return false;
        }
    }

    /**
     * Returns the status of the operation (result)
     *
     * @return the status of the operation
     */
    public IStatus getStatus() {
        return fStatus;
    }

    private int getTotalWork(TracePackageElement[] elements) {
        int totalWork = 0;
        for (TracePackageElement tracePackageElement : elements) {
            if (tracePackageElement.getChildren() != null) {
                totalWork += getTotalWork(tracePackageElement.getChildren());
            } else if (tracePackageElement.isChecked()) {
                ++totalWork;
            }
        }

        return totalWork;
    }

    /**
     * Run the operation. The status (result) of the operation can be obtained
     * with {@link #getStatus}
     *
     * @param progressMonitor
     *            the progress monitor to use to display progress and receive
     *            requests for cancellation
     */
    public void run(IProgressMonitor progressMonitor) {
        int totalWork = getTotalWork(new TracePackageElement[] { fImportTraceElement }) * 2;
        progressMonitor.beginTask(Messages.TraceImporter_ImportingPackage, totalWork);
        try {
            fStatus = Status.OK_STATUS;
            TracePackageElement[] children = fImportTraceElement.getChildren();
            for (TracePackageElement element : children) {
                ModalContext.checkCanceled(progressMonitor);

                if (element instanceof TracePackageFilesElement) {
                    TracePackageFilesElement exportTraceFilesElement = (TracePackageFilesElement) element;
                    fStatus = importTraceFiles(progressMonitor, exportTraceFilesElement);

                } else if (element instanceof TracePackageSupplFilesElement) {
                    TracePackageSupplFilesElement suppFilesElement = (TracePackageSupplFilesElement) element;
                    fStatus = importSupplFiles(progressMonitor, suppFilesElement);
                }

                if (fStatus.getSeverity() != IStatus.OK) {
                    break;
                }
            }

            progressMonitor.done();
        } catch (InterruptedException e) {
            fStatus = Status.CANCEL_STATUS;
        }
        return;
    }

    private static boolean fileNameMatches(String fileName, String entryName) {
        boolean fileMatch = entryName.equalsIgnoreCase(fileName);
        // TODO: does that work on Windows?
        boolean folderMatch = entryName.startsWith(fileName + "/"); //$NON-NLS-1$
        return fileMatch || folderMatch;
    }

    private IStatus importTraceFiles(IProgressMonitor monitor, TracePackageFilesElement exportTraceFilesElement) {
        List<String> fileNames = new ArrayList<String>();
        fileNames.add(exportTraceFilesElement.getFileName());
        IPath containerPath = fTmfTraceFolder.getPath();
        return importFiles(ArchiveUtil.getSpecifiedArchiveFile(fFileName), fileNames, containerPath, monitor);
    }

    private IStatus importSupplFiles(IProgressMonitor monitor, TracePackageSupplFilesElement suppFilesElement) {
        List<String> fileNames = new ArrayList<String>();
        for (TracePackageElement child : suppFilesElement.getChildren()) {
            TracePackageSupplFileElement supplFile = (TracePackageSupplFileElement) child;
            fileNames.add(supplFile.getText());
        }

        List<TmfTraceElement> traces = fTmfTraceFolder.getTraces();
        TmfTraceElement traceElement = null;
        for (TmfTraceElement t : traces) {
            if (t.getName().equals(fImportTraceElement.getText())) {
                traceElement = t;
            }
        }

        if (traceElement != null) {
            ArchiveFile archiveFile = ArchiveUtil.getSpecifiedArchiveFile(fFileName);
            traceElement.refreshSupplementaryFolder();
            IPath containerPath = traceElement.getTraceSupplementaryFolder(traceElement.getResource().getName()).getFullPath();
            return importFiles(archiveFile, fileNames, containerPath, monitor);
        }

        return Status.OK_STATUS;
    }

    private IStatus importFiles(ArchiveFile archiveFile, List<String> fileNames, IPath destinationPath, IProgressMonitor monitor) {
        List<ArchiveProviderElement> objects = new ArrayList<ArchiveProviderElement>();
        Enumeration<?> entries = archiveFile.entries();
        while (entries.hasMoreElements()) {
            ArchiveEntry entry = (ArchiveEntry) entries.nextElement();
            for (String fileName : fileNames) {
                if (fileNameMatches(fileName, entry.getName())) {
                    Path path = new Path(entry.getName());
                    ArchiveProviderElement pe = new ArchiveProviderElement(entry.getName(), path.lastSegment(), archiveFile, entry);
                    objects.add(pe);
                }
            }
        }

        ImportProvider provider = new ImportProvider();

        ImportOperation operation = new ImportOperation(destinationPath,
                null, provider, this,
                objects);
        operation.setCreateContainerStructure(true);
        operation.setOverwriteResources(true);

        try {
            operation.run(monitor);
            archiveFile.close();
        } catch (InvocationTargetException e) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.Messages.TracePackage_ErrorOperation, e);
        } catch (InterruptedException e) {
            return Status.CANCEL_STATUS;
        } catch (IOException e) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.Messages.TracePackage_ErrorOperation, e);
        }

        if (provider.getException() != null) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.Messages.TracePackage_ErrorOperation, provider.getException());
        }

        return operation.getStatus();
    }

    @Override
    public String queryOverwrite(String pathString) {
        // We always overwrite once we reach this point
        return null;
    }

    /**
     * Run extract the manifest operation. The status (result) of the operation
     * can be obtained with {@link #getStatus}
     *
     * @param progressMonitor
     *            the progress monitor to use to display progress and receive
     *            requests for cancellation
     */
    public void runExtractManifestOperation(IProgressMonitor progressMonitor) {
        TracePackageElement element = null;
        try {
            progressMonitor.worked(1);
            ArchiveFile archiveFile = ArchiveUtil.getSpecifiedArchiveFile(fFileName);
            progressMonitor.worked(1);
            if (archiveFile == null) {
                fStatus = new Status(IStatus.ERROR, Activator.PLUGIN_ID, "The selected file is not a supported file format");
                return;
            }

            Enumeration<?> entries = archiveFile.entries();

            boolean found = false;
            while (entries.hasMoreElements()) {
                ModalContext.checkCanceled(progressMonitor);

                ArchiveEntry entry = (ArchiveEntry) entries.nextElement();
                if (entry.getName().equalsIgnoreCase(ITracePackageConstants.MANIFEST_FILENAME)) {
                    found = true;
                    InputStream inputStream = archiveFile.getInputStream(entry);
                    element = loadElementsFromManifest(inputStream);
                    break;
                }

                progressMonitor.worked(1);
            }

            if (found) {
                fStatus = Status.OK_STATUS;
            }
            else {
                fStatus = new Status(IStatus.ERROR, Activator.PLUGIN_ID, MessageFormat.format(Messages.ImportTracePackagePage_ErrorManifestNotFound, ITracePackageConstants.MANIFEST_FILENAME));
            }

            fResultElement = element;

        } catch (InterruptedException e) {
            fStatus = Status.CANCEL_STATUS;
        } catch (Exception e) {
            fStatus = new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.ImportTracePackagePage_ErrorReadingManifest, e);
        }
    }

    /**
     * Get the resulting element from extracting the manifest from the archive
     *
     * @return the resulting element
     */
    public TracePackageElement getResultElement() {
        return fResultElement;
    }

    private static TracePackageElement loadElementsFromManifest(InputStream inputStream) throws IOException, SAXException, ParserConfigurationException {
        TracePackageElement element = null;
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);

        // TODO: validate file

        NodeList traceElements = doc.getDocumentElement().getElementsByTagName(ITracePackageConstants.TRACE_ELEMENT);
        for (int i = 0; i < traceElements.getLength(); ++i) {
            Node traceNode = traceElements.item(i);
            if (traceNode.getNodeType() == Node.ELEMENT_NODE) {
                Element traceElement = (Element) traceNode;
                String traceName = traceElement.getAttribute(ITracePackageConstants.TRACE_NAME_ATTRIB);
                String traceType = traceElement.getAttribute(ITracePackageConstants.TRACE_TYPE_ATTRIB);
                element = new TracePackageTraceElement(null, traceName, traceType);

                List<TracePackageElement> children = new ArrayList<TracePackageElement>();
                NodeList fileElements = traceElement.getElementsByTagName(ITracePackageConstants.TRACE_FILE_ELEMENT);
                for (int j = 0; j < fileElements.getLength(); ++j) {
                    Node fileNode = fileElements.item(j);
                    if (fileNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element fileElement = (Element) fileNode;
                        String fileName = fileElement.getAttribute(ITracePackageConstants.TRACE_FILE_NAME_ATTRIB);
                        children.add(new TracePackageFilesElement(element, fileName));
                    }
                }

                TracePackageSupplFilesElement supplFilesElement = new TracePackageSupplFilesElement(element);

                // Supplementary files
                List<TracePackageSupplFileElement> suppFiles = new ArrayList<TracePackageSupplFileElement>();
                NodeList suppFilesElements = traceElement.getElementsByTagName(ITracePackageConstants.SUPPLEMENTARY_FILE_ELEMENT);
                for (int j = 0; j < suppFilesElements.getLength(); ++j) {
                    Node suppFileNode = suppFilesElements.item(j);
                    if (suppFileNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element suppFileElement = (Element) suppFileNode;
                        String fileName = suppFileElement.getAttribute(ITracePackageConstants.SUPPLEMENTARY_FILE_NAME_ATTRIB);
                        TracePackageSupplFileElement supplFile = new TracePackageSupplFileElement(fileName, supplFilesElement);
                        suppFiles.add(supplFile);
                    }
                }

                if (!suppFiles.isEmpty()) {
                    supplFilesElement.setChildren(suppFiles.toArray(new TracePackageElement[] {}));
                    children.add(supplFilesElement);
                }

                // bookmarks
                List<TracePackageBookmarkElement.BookmarkInfo> bookmarkInfos = new ArrayList<TracePackageBookmarkElement.BookmarkInfo>();
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
                                    bookmarkInfos.add(new TracePackageBookmarkElement.BookmarkInfo(location, messageAttr.getValue()));

                                }
                            }
                        }
                    }
                }
                if (!bookmarkInfos.isEmpty()) {
                    children.add(new TracePackageBookmarkElement(element, bookmarkInfos));
                }

                element.setChildren(children.toArray(new TracePackageElement[] {}));
            }
        }
        return element;
    }

}
