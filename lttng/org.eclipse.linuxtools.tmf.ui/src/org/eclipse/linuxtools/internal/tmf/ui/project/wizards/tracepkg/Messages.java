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

/**
 *
 * @author Marc-Andre Laperle
 */
public class Messages extends NLS {

    private static final String BUNDLE_NAME = "org.eclipse.linuxtools.internal.tmf.ui.project.wizards.tracepkg.messages"; //$NON-NLS-1$

    public static String TracePackage_Bookmarks;
    public static String TracePackage_Browse;
    public static String TracePackage_DeselectAll;
    public static String TracePackage_ErrorOperation;
    public static String TracePackage_InternalErrorTitle;
    public static String TracePackage_SelectAll;
    public static String TracePackage_SupplementaryFiles;
    public static String TracePackage_TraceElement;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }

}
