package org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.imprt;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

import org.eclipse.ui.internal.wizards.datatransfer.TarException;

/**
 * Common interface between ZipFile and TarFile
 *
 * @author Marc-Andre Laperle
 */
public interface ArchiveFile {
    Enumeration<? extends ArchiveEntry> entries();
    void close() throws IOException;

    InputStream getInputStream(ArchiveEntry entry) throws TarException, IOException;
}

