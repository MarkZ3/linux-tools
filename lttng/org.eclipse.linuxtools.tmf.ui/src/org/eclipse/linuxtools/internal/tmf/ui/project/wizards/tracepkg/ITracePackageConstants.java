package org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg;

@SuppressWarnings("nls")
public interface ITracePackageConstants {
    /**
     * The file name for the package manifest file
     */
    public static final String MANIFEST_FILENAME = "export-manifest.xml";

    /**
     * The root element of an export
     */
    public static final String TMF_EXPORT_ELEMENT = "tmf-export";
    /**
     * Element representing a single trace
     */
    public static final String TRACE_ELEMENT = "trace";

    /**
     * Attribute for the name of a trace
     */
    public static final String TRACE_NAME_ATTRIB = "name";
    /**
     * Attribute for the type of a trace
     */
    public static final String TRACE_TYPE_ATTRIB = "type";

    /**
     * Element representing a single supplementary file
     */
    public static final String SUPPLEMENTARY_FILE_ELEMENT = "supplementary-file";

    /**
     * Attribute for the name of a supplementary file
     */
    public static final String SUPPLEMENTARY_FILE_NAME_ATTRIB = "name";

    /**
     * Element representing a trace file or folder
     */
    public static final String TRACE_FILE_ELEMENT = "file";
    /**
     * Attribute for the name of the file
     */
    public static final String TRACE_FILE_NAME_ATTRIB = "name";

    public static final String PERSISTENT_PROPERTY_ELEMENT = "persistent-property";
    public static final String PERSISTENT_PROPERTY_NAME_ATTRIB = "name";
    public static final String PERSISTENT_PROPERTY_VALUE_ATTRIB = "value";

    public static final String BOOKMARKS_ELEMENT = "bookmarks";
    public static final String BOOKMARK_ELEMENT = "bookmark";
}
