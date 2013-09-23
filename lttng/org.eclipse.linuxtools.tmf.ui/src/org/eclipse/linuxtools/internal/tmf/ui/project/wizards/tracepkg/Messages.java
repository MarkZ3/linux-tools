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

package org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg;

import org.eclipse.osgi.util.NLS;

@SuppressWarnings("javadoc")
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.messages"; //$NON-NLS-1$
    public static String WizardDataTransfer_exceptionMessage;
    public static String WizardExportPage_internalErrorTitle;
    public static String WizardExportPage_options;
    public static String WizardExportPage_title;
    public static String DataTransfer_browse;
    public static String DataTransfer_exportProblems;
    public static String FileExport_toArchive;
    public static String FileExport_createDirectoryStructure;
    public static String FileExport_createSelectedDirectories;
    public static String ExportFile_overwriteExisting;
    public static String ExportTraceEventsElement_Trace;
    public static String ExportTraceWizardPage_Bookmarks;
    public static String ExportTraceWizardPage_ChooseContent;
    public static String ExportTraceWizardPage_ErrorOperation;
    public static String ExportTraceWizardPage_GeneratingPackage;
    public static String ExportTraceWizardPage_SupplementaryFiles;
    public static String ArchiveExport_selectDestinationTitle;
    public static String ZipExport_compressContents;
    public static String ArchiveExport_saveInZipFormat;
    public static String ArchiveExport_saveInTarFormat;
    public static String SelectAll;
    public static String DeselectAll;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }

}
