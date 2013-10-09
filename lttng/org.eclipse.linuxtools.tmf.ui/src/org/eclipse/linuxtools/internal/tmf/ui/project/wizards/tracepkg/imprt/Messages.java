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
 *
 * @author Marc-Andre Laperle
 */
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.imprt.messages"; //$NON-NLS-1$

    public static String ImportTracePkgPage_title;
    public static String ArchiveImport_selectSourceTitle;
    public static String FileImport_FromArchive;
    public static String ImportTracePackagePage_ErrorCreatingBookmark;

    public static String ImportTracePackagePage_ErrorCreatingBookmarkFile;

    public static String ImportTracePackagePage_ErrorManifestNotFound;

    public static String ImportTracePackagePage_ErrorOperation;

    public static String ImportTracePackagePage_ErrorReadingManifest;

    public static String ImportTracePackagePage_ErrorSettingTraceType;
    public static String ImportTracePackagePage_Title;
    public static String TraceImporter_ReadingPackage;
    public static String TraceImporter_ImportingPackage;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }

}
