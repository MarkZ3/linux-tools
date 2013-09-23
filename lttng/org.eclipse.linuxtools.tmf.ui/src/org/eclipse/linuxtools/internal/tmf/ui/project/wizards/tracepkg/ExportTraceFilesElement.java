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

import org.eclipse.linuxtools.internal.tmf.ui.Activator;
import org.eclipse.swt.graphics.Image;

/**
 * An ExportTraceElement representing the trace files of a trace.
 */
public class ExportTraceFilesElement extends ExportTraceElement {

    private static final String TRACE_ICON_PATH = "icons/elcl16/trace.gif"; //$NON-NLS-1$
    private String fFileName;

    /**
     * Constructs an instance of ExportTraceFilesElement
     *
     * @param parent the parent of this element, can be set to null
     */
    public ExportTraceFilesElement(ExportTraceElement parent) {
        super(parent);
    }

    /**
     * Constructs an instance of ExportTraceFilesElement
     *
     * @param parent the parent of this element, can be set to null
     * @param fileName
     */
    public ExportTraceFilesElement(ExportTraceElement parent, String fileName) {
        super(parent);
        fFileName = fileName;
    }

    @Override
    public String getText() {
        return Messages.ExportTraceEventsElement_Trace;
    }

    @Override
    public Image getImage() {
        return Activator.getDefault().getImageFromImageRegistry(TRACE_ICON_PATH);
    }

    public String getFileName() {
        return fFileName;
    }

}
