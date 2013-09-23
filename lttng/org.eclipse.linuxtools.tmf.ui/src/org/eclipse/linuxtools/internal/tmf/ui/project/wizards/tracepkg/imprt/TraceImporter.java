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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.linuxtools.internal.tmf.ui.Activator;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.ExportTraceElement;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.ExportTraceFilesElement;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.ExportTraceSupplFilesElement;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.ExportTraceTraceElement;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfTraceElement;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfTraceFolder;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.internal.wizards.datatransfer.TarEntry;
import org.eclipse.ui.internal.wizards.datatransfer.TarException;
import org.eclipse.ui.internal.wizards.datatransfer.TarFile;
import org.eclipse.ui.wizards.datatransfer.IImportStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;

@SuppressWarnings("restriction")
public class TraceImporter implements IOverwriteQuery {

    String fFileName;
    IStatus fStatus;
    // private List<Object> fileSystemObjects;
    private ExportTraceTraceElement fExportTraceTraceElement;
    private TmfTraceFolder fTmfTraceFolder;
    private boolean isTarFile;

    public TraceImporter(String fileName, boolean isTarFile, ExportTraceTraceElement exportTraceTraceElement, TmfTraceFolder tmfTraceFolder) {
        fFileName = fileName;
        this.isTarFile = isTarFile;
        fExportTraceTraceElement = exportTraceTraceElement;
        fTmfTraceFolder = tmfTraceFolder;
    }

    public IStatus getStatus() {
        return fStatus;
    }

    private abstract class ProviderElement {

        private String fPath;
        private String fLabel;

        public ProviderElement(String destinationPath, String label) {
            fPath = destinationPath;
            fLabel = label;
        }

        abstract public InputStream getContents();

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

    private class ImportProvider implements IImportStructureProvider {

        @Override
        public List getChildren(Object element) {
            return null;
        }

        @Override
        public InputStream getContents(Object element) {
            return ((ProviderElement) element).getContents();
        }

        @Override
        public String getFullPath(Object element) {
            return ((ProviderElement) element).getFullPath();
        }

        @Override
        public String getLabel(Object element) {
            return ((ProviderElement) element).getLabel();
        }

        @Override
        public boolean isFolder(Object element) {
            return ((ProviderElement) element).isFolder();
        }

    }

    private class TarProviderElement extends ProviderElement {

        private TarFile tarFile;
        private TarEntry entry;

        public TarProviderElement(String destinationPath, String label, TarFile tarFile, TarEntry entry) {
            super(destinationPath, label);
            this.tarFile = tarFile;
            this.entry = entry;
        }

        @Override
        public InputStream getContents() {
            InputStream inputStream = null;
            try {
                inputStream = tarFile.getInputStream(entry);
            } catch (TarException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return inputStream;
        }

    }

    public void doImport(IProgressMonitor monitor) {
        int totalWork = 20;
        monitor.beginTask(Messages.TraceImporter_ReadingPackage, totalWork);

        if (isTarFile) {
            TarFile tarFile = getSpecifiedTarSourceFile(fFileName);

            ExportTraceElement[] children = fExportTraceTraceElement.getChildren();
            for (ExportTraceElement element : children) {
                if (element instanceof ExportTraceFilesElement) {
                    ExportTraceFilesElement exportTraceFilesElement = (ExportTraceFilesElement) element;
                    String fileName = exportTraceFilesElement.getFileName();
                    Enumeration<?> entries = tarFile.entries();
                    IPath containerPath = fTmfTraceFolder.getPath();
                    List<Object> objects = new ArrayList<Object>();
                    while (entries.hasMoreElements()) {
                        TarEntry entry = (TarEntry) entries.nextElement();
                        boolean fileMatch = entry.getName().equalsIgnoreCase(fileName);
                        boolean folderMatch = entry.getName().startsWith(fileName + "/");
                        if (fileMatch || folderMatch) {
                            ProviderElement pe;
                            Path path = new Path(entry.getName());
                            pe = new TarProviderElement(entry.getName(), path.lastSegment(), tarFile, entry);

                            objects.add(pe);
                        }
                    }

                    ImportProvider p = new ImportProvider();

                    ImportOperation operation = new ImportOperation(containerPath,
                            null, p, this,
                            objects);
                    operation.setCreateContainerStructure(true);
                    operation.setOverwriteResources(true);

                    try {
                        operation.run(monitor);
                    } catch (InvocationTargetException e) {
                        fStatus = new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Error!", e);
                        return;
                    } catch (InterruptedException e) {
                        fStatus = Status.CANCEL_STATUS;
                        return;
                    }

                    fStatus = operation.getStatus();

                } else if (element instanceof ExportTraceSupplFilesElement) {
                    ExportTraceSupplFilesElement suppFilesElement = (ExportTraceSupplFilesElement) element;
                    List<String> fileNames = suppFilesElement.getSuppFileNames();

                    // Find suppl folder
                    List<TmfTraceElement> traces = fTmfTraceFolder.getTraces();
                    TmfTraceElement traceElement = null;
                    for (TmfTraceElement t : traces) {
                        if (t.getName().equals(fExportTraceTraceElement.getText())) {
                            traceElement = t;
                        }
                    }

                    if (traceElement != null) {
                        try {
                            tarFile.close();
                        } catch (IOException e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }
                        tarFile = getSpecifiedTarSourceFile(fFileName);
                        traceElement.refreshSupplementaryFolder();
                        IPath containerPath = traceElement.getTraceSupplementaryFolder(traceElement.getResource().getName()).getFullPath();

                        List<Object> objects = new ArrayList<Object>();
                        Enumeration<?> entries = tarFile.entries();
                        while (entries.hasMoreElements()) {
                            TarEntry entry = (TarEntry) entries.nextElement();
                            for (String fileName : fileNames) {
                                boolean fileMatch = entry.getName().equalsIgnoreCase(fileName);
                                boolean folderMatch = entry.getName().startsWith(fileName + "/");
                                if (fileMatch || folderMatch) {
                                    ProviderElement pe;
                                    Path path = new Path(entry.getName());
                                    pe = new TarProviderElement(entry.getName(), path.lastSegment(), tarFile, entry);

                                    objects.add(pe);
                                }
                            }
                        }

                        ImportProvider p = new ImportProvider();

                        ImportOperation operation = new ImportOperation(containerPath,
                                null, p, this,
                                objects);
                        operation.setCreateContainerStructure(true);
                        operation.setOverwriteResources(true);

                        try {
                            operation.run(monitor);
                        } catch (InvocationTargetException e) {
                            fStatus = new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Error!", e);
                            return;
                        } catch (InterruptedException e) {
                            fStatus = Status.CANCEL_STATUS;
                            return;
                        }

                        fStatus = operation.getStatus();
                    }

                }

            }
        }

        monitor.done();
        fStatus = Status.OK_STATUS;
        return;
    }

    /**
     * Answer a handle to the zip file currently specified as being the source.
     * Return null if this file does not exist or is not of valid format.
     */
    protected ZipFile getSpecifiedZipSourceFile() {
        return getSpecifiedZipSourceFile(fFileName);
    }

    /**
     * Answer a handle to the zip file currently specified as being the source.
     * Return null if this file does not exist or is not of valid format.
     */
    private static ZipFile getSpecifiedZipSourceFile(String fileName) {
        if (fileName.length() == 0) {
            return null;
        }

        try {
            return new ZipFile(fileName);
        } catch (ZipException e) {
            // ignore
        } catch (IOException e) {
            // ignore
        }

        // sourceNameField.setFocus();
        return null;
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

    @Override
    public String queryOverwrite(String pathString) {
        // TODO Auto-generated method stub
        return null;
    }

}
