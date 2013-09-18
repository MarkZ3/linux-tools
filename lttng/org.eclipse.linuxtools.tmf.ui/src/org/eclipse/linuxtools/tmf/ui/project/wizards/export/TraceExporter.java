package org.eclipse.linuxtools.tmf.ui.project.wizards.export;

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
import org.eclipse.linuxtools.tmf.ui.project.model.TmfTraceElement;
import org.eclipse.ui.internal.wizards.datatransfer.ArchiveFileExportOperation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

@SuppressWarnings("restriction")
public class TraceExporter {

    ExportTraceTraceElement fTraceExportElement;
    Object[] fCheckedElements;
    boolean fUseCompression;
    boolean fUseTar;
    String fFileName;
    List<IResource> fResources;
    IStatus fStatus;

    public TraceExporter(ExportTraceTraceElement traceExportElement, Object[] checkedElements, boolean useCompression, boolean useTar, String fileName) {
        fTraceExportElement = traceExportElement;
        fCheckedElements = checkedElements;
        fUseCompression = useCompression;
        fUseTar = useTar;
        fFileName = fileName;
        fResources = new ArrayList<IResource>();
    }

    public void doExport(IProgressMonitor monitor) {

        try {

            int totalWork = fCheckedElements.length * 2;
            monitor.beginTask(Messages.ExportTraceWizardPage_GeneratingPackage, totalWork);

            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element createElement = doc.createElement("tmf-export"); //$NON-NLS-1$
            Node tmfNode = doc.appendChild(createElement);
            Element traceXmlElement = doc.createElement("trace"); //$NON-NLS-1$
            TmfTraceElement traceElement = fTraceExportElement.getTraceElement();
            traceXmlElement.setAttribute("name", traceElement.getResource().getName()); //$NON-NLS-1$
            traceXmlElement.setAttribute("type", traceElement.getTraceType()); //$NON-NLS-1$
            Node traceNode = tmfNode.appendChild(traceXmlElement);

            // List<IResource> resources = new ArrayList<IResource>();
            for (Object element : fCheckedElements) {
                ModalContext.checkCanceled(monitor);

                if (element instanceof ExportTraceSupplFilesElement) {
                    exportSupplementaryFiles(monitor, traceNode, (ExportTraceSupplFilesElement) element);
                } else if (element instanceof ExportTraceBookmarkElement) {
                    exportBookmarks(monitor, traceNode, (ExportTraceBookmarkElement) element);
                } else if (element instanceof ExportTraceFilesElement) {
                    exportTraceFiles(monitor, traceNode, (ExportTraceFilesElement) element);
                }

                monitor.worked(1);
            }

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4"); //$NON-NLS-1$ //$NON-NLS-2$
            DOMSource source = new DOMSource(doc);
            StringWriter buffer = new StringWriter();
            StreamResult result = new StreamResult(buffer);
            transformer.transform(source, result);
            String content = buffer.getBuffer().toString();

            ModalContext.checkCanceled(monitor);

            // TODO use System.getProperty("java.io.tmpdir") ??;
            IFile file = root.getFile(fTraceExportElement.getTraceElement().getSupplementaryFolderParent().getFullPath().append("export-manifest.xml")); //$NON-NLS-1$
            ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes());
            if (file.exists()) {
                file.setContents(inputStream, IResource.FORCE, null);
            } else {
                file.create(inputStream, IResource.FORCE | IResource.HIDDEN, null);
            }
            fResources.add(file);

            fStatus = exportToArchive(monitor, totalWork);
            monitor.done();

        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                fStatus = Status.CANCEL_STATUS;
            } else {
                fStatus = new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.ExportTraceWizardPage_ErrorOperation, e);
            }
        }
    }

    private IStatus exportToArchive(IProgressMonitor monitor, int totalWork) throws InvocationTargetException, InterruptedException {
        ArchiveFileExportOperation op = new ArchiveFileExportOperation(fResources, fFileName);
        op.setCreateLeadupStructure(false);
        op.setUseCompression(fUseCompression);
        op.setUseTarFormat(fUseTar);
        op.run(new SubProgressMonitor(monitor, totalWork / 2, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK));

        return op.getStatus();
    }

    private void exportSupplementaryFiles(IProgressMonitor monitor, Node traceNode, ExportTraceSupplFilesElement element) throws InterruptedException {
        Document doc = traceNode.getOwnerDocument();
        for (IResource res : element.getResources()) {
            ModalContext.checkCanceled(monitor);
            fResources.add(res);
            Element suppFileElement = doc.createElement("supplementary-file"); //$NON-NLS-1$
            suppFileElement.setAttribute("name", res.getName()); //$NON-NLS-1$
            traceNode.appendChild(suppFileElement);
        }
    }

    private void exportTraceFiles(IProgressMonitor monitor, Node traceNode, ExportTraceFilesElement element) throws CoreException, InterruptedException {
        Document doc = traceNode.getOwnerDocument();
        IResource resource = ((ExportTraceTraceElement)element.getParent()).getTraceElement().getResource();
        fResources.add(resource);
        Element suppFileElement = doc.createElement("file"); //$NON-NLS-1$
        suppFileElement.setAttribute("name", resource.getName()); //$NON-NLS-1$
        traceNode.appendChild(suppFileElement);
        for (QualifiedName key : resource.getPersistentProperties().keySet()) {
            ModalContext.checkCanceled(monitor);

            Element singlePersistentPropertyElement = doc.createElement("persistent-property"); //$NON-NLS-1$
            singlePersistentPropertyElement.setAttribute("name", key.getQualifier() + "." + key.getLocalName()); //$NON-NLS-1$ //$NON-NLS-2$
            singlePersistentPropertyElement.setAttribute("value", resource.getPersistentProperty(key)); //$NON-NLS-1$
            traceNode.appendChild(singlePersistentPropertyElement);
        }
    }

    private static void exportBookmarks(IProgressMonitor monitor, Node traceNode, ExportTraceBookmarkElement element) throws CoreException, InterruptedException {
        Document doc = traceNode.getOwnerDocument();
        IMarker[] findMarkers = ((ExportTraceTraceElement)element.getParent()).getTraceElement().getBookmarksFile().findMarkers(IMarker.BOOKMARK, false, IResource.DEPTH_ZERO);
        if (findMarkers.length > 0) {
            Element bookmarksXmlElement = doc.createElement("bookmarks"); //$NON-NLS-1$
            Node bookmarksNode = traceNode.appendChild(bookmarksXmlElement);

            for (IMarker marker : findMarkers) {
                ModalContext.checkCanceled(monitor);

                Element singleBookmarkXmlElement = doc.createElement("bookmark"); //$NON-NLS-1$
                for (String key : marker.getAttributes().keySet()) {
                    singleBookmarkXmlElement.setAttribute(key, marker.getAttribute(key).toString());
                }

                bookmarksNode.appendChild(singleBookmarkXmlElement);
            }
        }
    }

    public IStatus getStatus() {
        return fStatus;
    }
}
