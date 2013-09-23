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
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.ExportTraceTraceElement;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfTraceFolder;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.internal.wizards.datatransfer.ArchiveFileManipulations;
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

    public TraceImporter(String fileName, ExportTraceTraceElement exportTraceTraceElement, TmfTraceFolder tmfTraceFolder) {
        fFileName = fileName;
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

        // ILeveledImportStructureProvider importStructureProvider = null;
        if (ArchiveFileManipulations.isTarFile(fFileName)) {
            if (ensureTarSourceIsValid()) {
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
                            boolean folderMatch = entry.getName().startsWith(fileName + "/");
                            boolean fileMatch = entry.getName().equalsIgnoreCase(fileName);
                            if (fileMatch || folderMatch) {
                                // ExportTraceTraceElement traceElement =
                                // (ExportTraceTraceElement)
                                // exportTraceFilesElement.getParent();
                                // String traceName = traceElement.getText();

                                // if (folderMatch) {
                                // IPath tracePath =
                                // containerPath.append(traceName);
                                // // IWorkspaceRoot workspaceRoot =
                                // ResourcesPlugin.getWorkspace().getRoot();
                                // // IResource tracePathRes =
                                // workspaceRoot.findMember(tracePath);
                                // // if (tracePathRes == null ||
                                // !tracePathRes.exists()) {
                                // // try {
                                // //
                                // fTmfTraceFolder.getResource().getFolder(traceName).create(true,
                                // true, monitor);
                                // // } catch (CoreException e) {
                                // // // TODO Auto-generated catch block
                                // // e.printStackTrace();
                                // // }
                                // // }
                                // containerPath = tracePath;
                                //
                                // }

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

                    }

                }
                // importStructureProvider = new
                // TarLeveledStructureProvider(tarFile);
            }
        } /*
           * else if (ensureZipSourceIsValid()) { ZipFile zipFile =
           * getSpecifiedZipSourceFile(); importStructureProvider = new
           * ZipLeveledStructureProvider(zipFile); }
           *
           * if (importStructureProvider == null) { fStatus = new
           * Status(IStatus.ERROR, Activator.PLUGIN_ID, "Error!"); return; }
           */

        // IPath containerPath = null;
        // ImportOperation operation = new ImportOperation(containerPath,
        // importStructureProvider.getRoot(), importStructureProvider, this,
        // fileSystemObjects);
        //
        // try {
        // operation.run(monitor);
        // } catch (InvocationTargetException e) {
        // fStatus = new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Error!",
        // e);
        // } catch (InterruptedException e) {
        // fStatus = Status.CANCEL_STATUS;
        // }
        // fStatus = operation.getStatus();
        //
        // //
        // ArchiveFileManipulations.closeStructureProvider(importStructureProvider,
        // // getShell());

        monitor.done();
        fStatus = Status.OK_STATUS;
        return;
    }

    // /**
    // * Answer a boolean indicating whether the specified source currently
    // exists
    // * and is valid (ie.- proper format)
    // */
    // private boolean ensureZipSourceIsValid() {
    // ZipFile specifiedFile = getSpecifiedZipSourceFile();
    // if (specifiedFile == null) {
    // // setErrorMessage(DataTransferMessages.ZipImport_badFormat);
    // return false;
    // }
    // try {
    // specifiedFile.close();
    // } catch (IOException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // return false;
    // }
    // // return ArchiveFileManipulations.closeZipFile(specifiedFile,
    // // getShell());
    // return true;
    // }

    /**
     * Answer a handle to the zip file currently specified as being the source.
     * Return null if this file does not exist or is not of valid format.
     */
    protected ZipFile getSpecifiedZipSourceFile() {
        return getSpecifiedZipSourceFile(fFileName);
    }

    private boolean ensureTarSourceIsValid() {
        TarFile specifiedFile = getSpecifiedTarSourceFile(fFileName);
        if (specifiedFile == null) {
            // setErrorMessage(DataTransferMessages.TarImport_badFormat);
            return false;
        }
        try {
            specifiedFile.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
        return true;
        // return ArchiveFileManipulations.closeTarFile(specifiedFile,
        // getShell());
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
