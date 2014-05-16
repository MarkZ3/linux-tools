package org.eclipse.linuxtools.tmf.core.callstack;

/**
 * @since 3.1
 */
public class LibraryInfo {
    private String fPath;
    private long fBaseAddress;
    private long fSize;

    public LibraryInfo(String path, long baseAddress, long size) {
        fPath = path;
        fBaseAddress = baseAddress;
        fSize = size;
    }

    public String getPath() {
        return fPath;
    }

    public long getBaseAddress() {
        return fBaseAddress;
    }

    public long getSize() {
        return fSize;
    }
}
