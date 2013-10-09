package org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.imprt;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Vector;

import org.eclipse.ui.internal.wizards.datatransfer.TarEntry;
import org.eclipse.ui.internal.wizards.datatransfer.TarException;
import org.eclipse.ui.internal.wizards.datatransfer.TarFile;

/**
 * Adapter for TarFile to ArchiveFile
 *
 * @author Marc-Andre Laperle
 */
@SuppressWarnings("restriction")
public class TarAchiveFile implements ArchiveFile {

    private TarFile fTarFile;

    public TarAchiveFile(TarFile tarFile) {
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
        return fTarFile.getInputStream(((TarArchiveEntry) entry).getTarEntry());
    }
}