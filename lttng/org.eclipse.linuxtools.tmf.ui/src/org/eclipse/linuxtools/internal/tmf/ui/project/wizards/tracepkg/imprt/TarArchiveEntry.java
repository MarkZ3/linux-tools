package org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.imprt;

import org.eclipse.ui.internal.wizards.datatransfer.TarEntry;

/**
 * Adapter for TarEntry to ArchiveEntry
 *
 * @author Marc-Andre Laperle
 */
@SuppressWarnings("restriction")
public class TarArchiveEntry implements ArchiveEntry {
    private TarEntry fTarEntry;

    public TarArchiveEntry(TarEntry tarEntry) {
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
