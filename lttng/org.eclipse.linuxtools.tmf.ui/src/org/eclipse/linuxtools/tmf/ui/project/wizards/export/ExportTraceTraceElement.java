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

package org.eclipse.linuxtools.tmf.ui.project.wizards.export;

import org.eclipse.linuxtools.tmf.ui.project.model.TmfTraceElement;

/**
 * An ExportTraceElement associated to a TmfTraceElement. This will be the parent
 * of other elements (events, supplementary files, bookmarks, etc).
 *
 */
public class ExportTraceTraceElement extends ExportTraceElement {

    private TmfTraceElement fTraceElement;

    /**
     * Construct an instance associated to a TmfTraceElement.
     *
     * @param parent the parent of this element, can be set to null
     * @param traceElement the associated TmfTraceElement
     */
    public ExportTraceTraceElement(ExportTraceElement parent, TmfTraceElement traceElement) {
        super(parent);
        fTraceElement = traceElement;
    }

    @Override
    public String getText() {
        return fTraceElement.getName();
    }

    /**
     * @return the associated TmfTraceElement
     */
    public TmfTraceElement getTraceElement() {
        return fTraceElement;
    }

}
