package org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.imprt;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.ui.internal.wizards.datatransfer.TarException;

/**
 * Adapter for ZipFile to ArchiveFile
 *
 * @author Marc-Andre Laperle
 */
@SuppressWarnings("restriction")
public class ZipArchiveFile implements ArchiveFile {

    private ZipFile fZipFile;

    public ZipArchiveFile(ZipFile zipFile) {
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