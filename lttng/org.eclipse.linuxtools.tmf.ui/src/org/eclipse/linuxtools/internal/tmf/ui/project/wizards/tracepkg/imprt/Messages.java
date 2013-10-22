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

import org.eclipse.osgi.util.NLS;

/**
 * Messages for the trace package import wizard
 *
 * @author Marc-Andre Laperle
 */
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.imprt.messages"; //$NON-NLS-1$

    /**
     * Title for the import page
     */
    public static String ImportTracePackageWizardPage_Title;

    /**
     * Text for the source archive label
     */
    public static String ImportTracePackageWizardPage_FromArchive;

    /**
     * Text for the reading package job
     */
    public static String ImportTracePackageWizardPage_ReadingPackage;

    /**
     * Message when file is not found
     */
    public static String ImportTracePackageWizardPage_ErrorFileNotFound;

    /**
     * Message when trace type could not be set
     */
    public static String ImportTracePackageWizardPage_ErrorSettingTraceType;

    /**
     * The message displayed under the title
     */
    public static String ImportTracePackageWizardPage_Message;

    /**
     * Generic error message for the import operation
     */
    public static String ImportTracePackageWizardPage_ErrorOperation;

    /**
     * Text when error occurs creating a bookmark
     */
    public static String TracePackageImportOperation_ErrorCreatingBookmark;

    /**
     * Text when error occurs creating a bookmark file
     */
    public static String TracePackageImportOperation_ErrorCreatingBookmarkFile;

    /**
     * Text for the importing package job
     */
    public static String TracePackageImportOperation_ImportingPackage;

    /**
     * Text when error occurs when the manifest is not found in the archive
     */
    public static String TracePackageExtractManifestOperation_ErrorManifestNotFound;

    /**
     * Generic error message when reading the manifest
     */
    public static String TracePackageExtractManifestOperation_ErrorReadingManifest;

    /**
     * Error message when the file is an invalid format
     */
    public static String TracePackageExtractManifestOperation_InvalidFormat;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }

}
