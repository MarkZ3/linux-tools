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
 *
 * @author Marc-Andre Laperle
 */
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.export.messages"; //$NON-NLS-1$

    public static String ExportTracePackageWizardPage_ApproximateSizeLbl;
    public static String ExportTracePackageWizardPage_ChooseContent;
    public static String ExportTracePackageWizardPage_CompressContents;
    public static String ExportTracePackageWizardPage_ContentColumnName;
    public static String ExportTracePackageWizardPage_GeneratingPackage;
    public static String ExportTracePackageWizardPage_Options;
    public static String ExportTracePackageWizardPage_SaveInTarFormat;
    public static String ExportTracePackageWizardPage_SaveInZipFormat;
    public static String ExportTracePackageWizardPage_SelectDestinationTitle;
    public static String ExportTracePackageWizardPage_SizeByte;
    public static String ExportTracePackageWizardPage_SizeColumnName;
    public static String ExportTracePackageWizardPage_SizeGigabyte;
    public static String ExportTracePackageWizardPage_SizeKilobyte;
    public static String ExportTracePackageWizardPage_SizeMegabyte;
    public static String ExportTracePackageWizardPage_SizeTerabyte;
    public static String ExportTracePackageWizardPage_Title;
    public static String ExportTracePackageWizardPage_ToArchive;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }

}
