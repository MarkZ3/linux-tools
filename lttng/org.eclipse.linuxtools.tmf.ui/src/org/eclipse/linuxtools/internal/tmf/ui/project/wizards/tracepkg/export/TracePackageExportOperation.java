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

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
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
import org.eclipse.ui.internal.wizards.datatransfer.ArchiveFileExportOperation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * An operation that exports a trace package to an archive
 *
 * @author Marc-Andre Laperle
 */
@SuppressWarnings("restriction")
public class TracePackageExportOperation {

    private final TracePackageTraceElement[] fTraceExportElements;
    private final boolean fUseCompression;
    private final boolean fUseTar;
    private final String fFileName;
    private final List<IResource> fResources;
    private IStatus fStatus;

    /**
     * Constructs a new export operation
     *
     * @param traceExportElements the trace elements to be exported
     * @param useCompression whether or not to use compression
     * @param useTar use tar format or zip
     * @param fileName the output file name
     */
    public TracePackageExportOperation(TracePackageTraceElement[] traceExportElements, boolean useCompression, boolean useTar, String fileName) {
        fTraceExportElements = traceExportElements;
        fUseCompression = useCompression;
        fUseTar = useTar;
        fFileName = fileName;
        fResources = new ArrayList<IResource>();
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
     * Run the operation. The status (result) of the operation can be obtained with {@link #getStatus}
     *
     * @param progressMonitor the progress monitor to use to display progress and receive
     *   requests for cancellation
     */
    public void run(IProgressMonitor progressMonitor) {

        try {
            int totalWork = getTotalWork(fTraceExportElements) * 2;
            progressMonitor.beginTask(Messages.ExportTracePackageWizardPage_GeneratingPackage, totalWork);

            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element createElement = doc.createElement(ITracePackageConstants.TMF_EXPORT_ELEMENT);
            Node tmfNode = doc.appendChild(createElement);

            for (TracePackageTraceElement tracePackageElement : fTraceExportElements) {
                exportTrace(progressMonitor, tmfNode, tracePackageElement);
            }

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4"); //$NON-NLS-1$ //$NON-NLS-2$
            DOMSource source = new DOMSource(doc);
            StringWriter buffer = new StringWriter();
            StreamResult result = new StreamResult(buffer);
            transformer.transform(source, result);
            String content = buffer.getBuffer().toString();

            ModalContext.checkCanceled(progressMonitor);

            IFile file = createManifest(content);
            fResources.add(file);

            fStatus = exportToArchive(progressMonitor, totalWork);
            progressMonitor.done();

        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                fStatus = Status.CANCEL_STATUS;
            } else {
                fStatus = new Status(IStatus.ERROR, Activator.PLUGIN_ID, org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.Messages.TracePackage_ErrorOperation, e);
            }
        }
    }

    private void exportTrace(IProgressMonitor monitor, Node tmfNode, TracePackageTraceElement tracePackageElement) throws InterruptedException, CoreException {
        TmfTraceElement traceElement = tracePackageElement.getTraceElement();
        Element traceXmlElement = tmfNode.getOwnerDocument().createElement(ITracePackageConstants.TRACE_ELEMENT);
        traceXmlElement.setAttribute(ITracePackageConstants.TRACE_NAME_ATTRIB, traceElement.getResource().getName());
        traceXmlElement.setAttribute(ITracePackageConstants.TRACE_TYPE_ATTRIB, traceElement.getTraceType());
        Node traceNode = tmfNode.appendChild(traceXmlElement);

        for (TracePackageElement element : tracePackageElement.getChildren()) {
            ModalContext.checkCanceled(monitor);
            if (!element.isChecked()) {
                continue;
            }

            if (element instanceof TracePackageSupplFilesElement) {
                exportSupplementaryFiles(monitor, traceNode, (TracePackageSupplFilesElement) element);
            } else if (element instanceof TracePackageBookmarkElement) {
                exportBookmarks(monitor, traceNode, (TracePackageBookmarkElement) element);
            } else if (element instanceof TracePackageFilesElement) {
                exportTraceFiles(monitor, traceNode, (TracePackageFilesElement) element);
            }

            monitor.worked(1);
        }
    }

    private void exportSupplementaryFiles(IProgressMonitor monitor, Node traceNode, TracePackageSupplFilesElement element) throws InterruptedException {
        Document doc = traceNode.getOwnerDocument();
        for (TracePackageElement child : element.getChildren()) {
            TracePackageSupplFileElement supplFile = (TracePackageSupplFileElement)child;
            ModalContext.checkCanceled(monitor);
            IResource res = supplFile.getResource();
            fResources.add(res);
            Element suppFileElement = doc.createElement(ITracePackageConstants.SUPPLEMENTARY_FILE_ELEMENT);
            suppFileElement.setAttribute(ITracePackageConstants.SUPPLEMENTARY_FILE_NAME_ATTRIB, res.getName());
            traceNode.appendChild(suppFileElement);
        }
    }

    private void exportTraceFiles(IProgressMonitor monitor, Node traceNode, TracePackageFilesElement element) throws CoreException, InterruptedException {
        Document doc = traceNode.getOwnerDocument();
        IResource resource = ((TracePackageTraceElement) element.getParent()).getTraceElement().getResource();
        fResources.add(resource);
        Element fileElement = doc.createElement(ITracePackageConstants.TRACE_FILE_ELEMENT);
        fileElement.setAttribute(ITracePackageConstants.TRACE_FILE_NAME_ATTRIB, resource.getName());
        Node fileNode = traceNode.appendChild(fileElement);
        for (QualifiedName key : resource.getPersistentProperties().keySet()) {
            ModalContext.checkCanceled(monitor);

            Element singlePersistentPropertyElement = doc.createElement(ITracePackageConstants.PERSISTENT_PROPERTY_ELEMENT);
            singlePersistentPropertyElement.setAttribute(ITracePackageConstants.PERSISTENT_PROPERTY_NAME_ATTRIB, key.getQualifier() + "." + key.getLocalName()); //$NON-NLS-1$
            singlePersistentPropertyElement.setAttribute(ITracePackageConstants.PERSISTENT_PROPERTY_VALUE_ATTRIB, resource.getPersistentProperty(key));
            fileNode.appendChild(singlePersistentPropertyElement);
        }
    }

    private static void exportBookmarks(IProgressMonitor monitor, Node traceNode, TracePackageBookmarkElement element) throws CoreException, InterruptedException {
        Document doc = traceNode.getOwnerDocument();
        IMarker[] findMarkers = ((TracePackageTraceElement) element.getParent()).getTraceElement().getBookmarksFile().findMarkers(IMarker.BOOKMARK, false, IResource.DEPTH_ZERO);
        if (findMarkers.length > 0) {
            Element bookmarksXmlElement = doc.createElement(ITracePackageConstants.BOOKMARKS_ELEMENT);
            Node bookmarksNode = traceNode.appendChild(bookmarksXmlElement);

            for (IMarker marker : findMarkers) {
                ModalContext.checkCanceled(monitor);

                Element singleBookmarkXmlElement = doc.createElement(ITracePackageConstants.BOOKMARK_ELEMENT);
                for (String key : marker.getAttributes().keySet()) {
                    singleBookmarkXmlElement.setAttribute(key, marker.getAttribute(key).toString());
                }

                bookmarksNode.appendChild(singleBookmarkXmlElement);
            }
        }
    }

    private IFile createManifest(String content) throws CoreException {
        // TODO use System.getProperty("java.io.tmpdir") ??;
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IFile file = root.getFile(fTraceExportElements[0].getTraceElement().getSupplementaryFolderParent().getFullPath().append(ITracePackageConstants.MANIFEST_FILENAME));
        ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes());
        if (file.exists()) {
            file.setContents(inputStream, IResource.FORCE, null);
        } else {
            file.create(inputStream, IResource.FORCE | IResource.HIDDEN, null);
        }
        return file;
    }

    private IStatus exportToArchive(IProgressMonitor monitor, int totalWork) throws InvocationTargetException, InterruptedException {
        ArchiveFileExportOperation op = new ArchiveFileExportOperation(fResources, fFileName);
        op.setCreateLeadupStructure(false);
        op.setUseCompression(fUseCompression);
        op.setUseTarFormat(fUseTar);
        op.run(new SubProgressMonitor(monitor, totalWork / 2, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK));

        return op.getStatus();
    }

    /**
     * Returns the status of the operation (result)
     *
     * @return the status of the operation
     */
    public IStatus getStatus() {
        return fStatus;
    }
}
