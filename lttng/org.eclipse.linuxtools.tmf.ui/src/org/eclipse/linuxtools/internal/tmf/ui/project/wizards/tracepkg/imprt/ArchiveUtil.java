package org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.imprt;

import java.io.IOException;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.eclipse.ui.internal.wizards.datatransfer.TarException;
import org.eclipse.ui.internal.wizards.datatransfer.TarFile;

@SuppressWarnings("restriction")
public abstract class ArchiveUtil {
    /**
     * Answer a handle to the zip file currently specified as being the source.
     * Return null if this file does not exist or is not of valid format.
     * @param fileName
     * @return
     */
    public static ArchiveFile getSpecifiedArchiveFile(String fileName) {
        if (fileName.length() == 0) {
            return null;
        }

        try {
            ZipFile zipFile = new ZipFile(fileName);
            return new ZipArchiveFile(zipFile);
        } catch (ZipException e) {
            // ignore
        } catch (IOException e) {
            // ignore
        }

        try {
            TarFile tarFile = new TarFile(fileName);
            return new TarAchiveFile(tarFile);
        } catch (TarException e) {
            // ignore
        } catch (IOException e) {
            // ignore
        }

        return null;
    }
}
