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

package org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.export;

import org.eclipse.osgi.util.NLS;

/**
 * Messages for the trace package export wizard
 *
 * @author Marc-Andre Laperle
 */
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.export.messages"; //$NON-NLS-1$

    /**
     * The approximate size label
     */
    public static String ExportTracePackageWizardPage_ApproximateSizeLbl;

    /**
     * The message under the wizard page title
     */
    public static String ExportTracePackageWizardPage_ChooseContent;

    /**
     * Text for the compress contents checkbox
     */
    public static String ExportTracePackageWizardPage_CompressContents;

    /**
     * Text for the first column (content)
     */
    public static String ExportTracePackageWizardPage_ContentColumnName;

    /**
     * Text for the options group
     */
    public static String ExportTracePackageWizardPage_Options;

    /**
     * Text for the tar format option
     */
    public static String ExportTracePackageWizardPage_SaveInTarFormat;

    /**
     * Text for the zip format option
     */
    public static String ExportTracePackageWizardPage_SaveInZipFormat;

    /**
     * Text for the file chooser dialog
     */
    public static String TracePackage_FileDialogTitle;

    /**
     * Byte units
     */
    public static String ExportTracePackageWizardPage_SizeByte;

    /**
     * Text for the second column (size)
     */
    public static String ExportTracePackageWizardPage_SizeColumnName;

    /**
     * Gigabyte units
     */
    public static String ExportTracePackageWizardPage_SizeGigabyte;

    /**
     * Kilobyte units
     */
    public static String ExportTracePackageWizardPage_SizeKilobyte;

    /**
     * Megabyte units
     */
    public static String ExportTracePackageWizardPage_SizeMegabyte;

    /**
     * Terabyte units
     */
    public static String ExportTracePackageWizardPage_SizeTerabyte;

    /**
     * Title for the wizard page
     */
    public static String ExportTracePackageWizardPage_Title;

    /**
     * Label for the file path
     */
    public static String ExportTracePackageWizardPage_ToArchive;

    /**
     * Text for the generating package job
     */
    public static String TracePackageExportOperation_GeneratingPackage;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }

}
