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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.ModalContext;
import org.eclipse.linuxtools.internal.tmf.ui.Activator;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.AbstractTracePackageOperation;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.ITracePackageConstants;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageBookmarkElement;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageElement;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageFilesElement;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageSupplFileElement;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageSupplFilesElement;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageTraceElement;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * An operation that extracts information from the manifest located in an
 * archive
 *
 * @author Marc-Andre Laperle
 */
public class TracePackageExtractManifestOperation extends AbstractTracePackageOperation {

    // Result of reading the manifest
    private TracePackageElement fResultElement;

    /**
     * Constructs a new import operation for reading the manifest
     *
     * @param fileName
     *            the output file name
     */
    public TracePackageExtractManifestOperation(String fileName) {
        super(fileName);
    }

    /**
     * Run extract the manifest operation. The status (result) of the operation
     * can be obtained with {@link #getStatus}
     *
     * @param progressMonitor
     *            the progress monitor to use to display progress and receive
     *            requests for cancellation
     */
    @Override
    public void run(IProgressMonitor progressMonitor) {
        TracePackageElement element = null;
        try {
            progressMonitor.worked(1);
            ArchiveFile archiveFile = getSpecifiedArchiveFile();
            progressMonitor.worked(1);
            if (archiveFile == null) {
                setStatus(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.TracePackageExtractManifestOperation_InvalidFormat));
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
                setStatus(Status.OK_STATUS);
            }
            else {
                setStatus(new Status(IStatus.ERROR, Activator.PLUGIN_ID, MessageFormat.format(Messages.ImportTracePackagePage_ErrorManifestNotFound, ITracePackageConstants.MANIFEST_FILENAME)));
            }

            fResultElement = element;

        } catch (InterruptedException e) {
            setStatus(Status.CANCEL_STATUS);
        } catch (Exception e) {
            setStatus(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.ImportTracePackagePage_ErrorReadingManifest, e));
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
