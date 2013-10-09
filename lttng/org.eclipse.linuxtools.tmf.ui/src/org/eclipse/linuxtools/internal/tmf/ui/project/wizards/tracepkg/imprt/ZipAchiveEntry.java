package org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.imprt;

import java.util.zip.ZipEntry;

/**
 * Adapter for ArchiveEntry to ArchiveEntry
 *
 * @author Marc-Andre Laperle
 */
public class ZipAchiveEntry implements ArchiveEntry {

    private ZipEntry fZipEntry;

    public ZipAchiveEntry(ZipEntry zipEntry) {
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
