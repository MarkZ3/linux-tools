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
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.linuxtools.internal.tmf.ui.Activator;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageElement;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageFilesElement;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageSupplFileElement;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageSupplFilesElement;
import org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.TracePackageTraceElement;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfTraceElement;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfTraceFolder;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.internal.wizards.datatransfer.TarEntry;
import org.eclipse.ui.internal.wizards.datatransfer.TarException;
import org.eclipse.ui.internal.wizards.datatransfer.TarFile;
import org.eclipse.ui.wizards.datatransfer.IImportStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;

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

    /**
     * Constructs a new import operation
     *
     * @param importTraceElement the trace element to be imported
     * @param fileName the output file name
     * @param tmfTraceFolder the destination folder
     */
    public TracePackageImportOperation(String fileName, TracePackageTraceElement importTraceElement, TmfTraceFolder tmfTraceFolder) {
        fFileName = fileName;
        fImportTraceElement = importTraceElement;
        fTmfTraceFolder = tmfTraceFolder;
    }

    private class ImportProvider implements IImportStructureProvider {

        @Override
        public List getChildren(Object element) {
            return null;
        }

        @Override
        public InputStream getContents(Object element) {
            return ((ArchiveProviderElement) element).getContents();
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

    }

    private class ArchiveProviderElement {

        private final String fPath;
        private final String fLabel;

        private ArchiveFile archiveFile;
        private ArchiveEntry entry;

        public ArchiveProviderElement(String destinationPath, String label, ArchiveFile archiveFile, ArchiveEntry entry) {
            fPath = destinationPath;
            fLabel = label;
            this.archiveFile = archiveFile;
            this.entry = entry;
        }

        public InputStream getContents() {
            InputStream inputStream = null;
            try {
                inputStream = archiveFile.getInputStream(entry);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (TarException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return inputStream;
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
     *  Common interface between ZipEntry and TarEntry
     */
    private interface ArchiveEntry {
        String getName();
    }

    /**
     * Common interface between ZipFile and TarFile
     */
    private interface ArchiveFile {
        Enumeration<? extends ArchiveEntry> entries();
        void close() throws IOException;

        InputStream getInputStream(ArchiveEntry entry) throws TarException, IOException;
    }

    /**
     * Adapter for TarEntry to ArchiveEntry
     */
    private class TarArchiveEntry implements ArchiveEntry {
        private TarEntry fTarEntry;

        private TarArchiveEntry(TarEntry tarEntry) {
            this.fTarEntry = tarEntry;
        }

        @Override
        public String getName() {
            return fTarEntry.getName();
        }

        public TarEntry getTarEntry() {
            return fTarEntry;
        }
    }

    /**
     * Adapter for TarFile to ArchiveFile
     */
    private class TarAchiveFile implements ArchiveFile {
        private TarFile fTarFile;
        private TarAchiveFile(TarFile tarFile) {
            this.fTarFile = tarFile;
        }

        @Override
        public Enumeration<? extends ArchiveEntry> entries() {
            Vector<ArchiveEntry> v = new Vector<ArchiveEntry>();
            for (Enumeration<?> e = fTarFile.entries(); e.hasMoreElements();) {
                v.add(new TarArchiveEntry((TarEntry) e.nextElement()));
            }

            return v.elements();
        }

        @Override
        public void close() throws IOException {
            fTarFile.close();
        }

        @Override
        public InputStream getInputStream(ArchiveEntry entry) throws TarException, IOException {
            return fTarFile.getInputStream(((TarArchiveEntry)entry).getTarEntry());
        }
    }

    /**
     * Adapter for ArchiveEntry to ArchiveEntry
     */
    private class ZipAchiveEntry implements ArchiveEntry {
        private ZipEntry fZipEntry;

        private ZipAchiveEntry(ZipEntry zipEntry) {
            this.fZipEntry = zipEntry;
        }

        @Override
        public String getName() {
            return fZipEntry.getName();
        }

        public ZipEntry getZipEntry() {
            return fZipEntry;
        }
    }

    /**
     * Adapter for ZipFile to ArchiveFile
     */
    private class ZipArchiveFile implements ArchiveFile {
        private ZipFile fZipFile;
        private ZipArchiveFile(ZipFile zipFile) {
            this.fZipFile = zipFile;
        }

        @Override
        public Enumeration<? extends ArchiveEntry> entries() {
            Vector<ArchiveEntry> v = new Vector<ArchiveEntry>();
            for (Enumeration<?> e = fZipFile.entries(); e.hasMoreElements();) {
                v.add(new ZipAchiveEntry((ZipEntry) e.nextElement()));
            }

            return v.elements();
        }

        @Override
        public void close() throws IOException {
            fZipFile.close();
        }

        @Override
        public InputStream getInputStream(ArchiveEntry entry) throws TarException, IOException {
            return fZipFile.getInputStream(((ZipAchiveEntry)entry).getZipEntry());
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
     * Run the operation. The status (result) of the operation can be obtained with {@link #getStatus}
     *
     * @param progressMonitor the progress monitor to use to display progress and receive
     *   requests for cancellation
     */
    public void run(IProgressMonitor progressMonitor) {
        int totalWork = getTotalWork(new TracePackageElement[] { fImportTraceElement }) * 2;
        progressMonitor.beginTask(Messages.TraceImporter_ImportingPackage, totalWork);
        try {
            TracePackageElement[] children = fImportTraceElement.getChildren();
            for (TracePackageElement element : children) {
                if (element instanceof TracePackageFilesElement) {
                    TracePackageFilesElement exportTraceFilesElement = (TracePackageFilesElement) element;
                    importTraceFiles(progressMonitor, exportTraceFilesElement);

                } else if (element instanceof TracePackageSupplFilesElement) {
                    TracePackageSupplFilesElement suppFilesElement = (TracePackageSupplFilesElement) element;
                    importSupplFiles(progressMonitor, suppFilesElement);
                }

            }

            progressMonitor.done();
            fStatus = Status.OK_STATUS;
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                fStatus = Status.CANCEL_STATUS;
            } else {
                fStatus = new Status(IStatus.ERROR, Activator.PLUGIN_ID, org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.Messages.TracePackage_ErrorOperation, e);
            }
        }
        return;
    }

    private static boolean fileNameMatches(String fileName, String entryName) {
        boolean fileMatch = entryName.equalsIgnoreCase(fileName);
        //TODO: does that work on Windows?
        boolean folderMatch = entryName.startsWith(fileName + "/"); //$NON-NLS-1$
        return fileMatch || folderMatch;
    }

    private void importTraceFiles(IProgressMonitor monitor, TracePackageFilesElement exportTraceFilesElement) throws IOException, InvocationTargetException, InterruptedException {
        List<String> fileNames = new ArrayList<String>();
        fileNames.add(exportTraceFilesElement.getFileName());
        IPath containerPath = fTmfTraceFolder.getPath();
        importFiles(getSpecifiedArchiveFile(), fileNames, containerPath, monitor);
    }

    private void importSupplFiles(IProgressMonitor monitor, TracePackageSupplFilesElement suppFilesElement) throws IOException, InvocationTargetException, InterruptedException {
        List<String> fileNames = new ArrayList<String>();
        for (TracePackageElement child : suppFilesElement.getChildren()) {
            TracePackageSupplFileElement supplFile = (TracePackageSupplFileElement)child;
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
            ArchiveFile archiveFile = getSpecifiedArchiveFile();
            traceElement.refreshSupplementaryFolder();
            IPath containerPath = traceElement.getTraceSupplementaryFolder(traceElement.getResource().getName()).getFullPath();
            importFiles(archiveFile, fileNames, containerPath, monitor);
        }
    }

    private void importFiles(ArchiveFile archiveFile, List<String> fileNames, IPath destinationPath, IProgressMonitor monitor) throws IOException, InvocationTargetException, InterruptedException {
        List<Object> objects = new ArrayList<Object>();
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

        ImportProvider p = new ImportProvider();

        ImportOperation operation = new ImportOperation(destinationPath,
                null, p, this,
                objects);
        operation.setCreateContainerStructure(true);
        operation.setOverwriteResources(true);

        operation.run(monitor);
        archiveFile.close();
        fStatus = operation.getStatus();
    }

    /**
     * Answer a handle to the zip file currently specified as being the source.
     * Return null if this file does not exist or is not of valid format.
     */
    private ArchiveFile getSpecifiedArchiveFile() {
        if (fFileName.length() == 0) {
            return null;
        }

        try {
            ZipFile zipFile = new ZipFile(fFileName);
            return new ZipArchiveFile(zipFile);
        } catch (ZipException e) {
            // ignore
        } catch (IOException e) {
            // ignore
        }

        try {
            TarFile tarFile = new TarFile(fFileName);
            return new TarAchiveFile(tarFile);
        } catch (TarException e) {
            // ignore
        } catch (IOException e) {
            // ignore
        }

        return null;
    }


    @Override
    public String queryOverwrite(String pathString) {
        // We always overwrite once we reach this point
        return null;
    }

}
