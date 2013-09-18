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

import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.model.WorkbenchAdapter;

/**
 * An ExportTraceElement represents an item in the ExportTraceWizard
 * tree.
 */
public abstract class ExportTraceElement extends WorkbenchAdapter {
    ExportTraceElement[] children;
    ExportTraceElement parent;

    /**
     *
     * @param parent the parent of this element, can be set to null
     */
    public ExportTraceElement(ExportTraceElement parent) {
        this.parent = parent;
    }

    /**
     * @return the parent of this element or null if there is no parent
     */
    public Object getParent() {
        return parent;
    }

    /**
     * Get the text representation of this element to be displayed in the
     * tree.
     *
     * @return the text representation
     */
    abstract public String getText();

    ExportTraceElement[] getChildren() {
        return children;
    }

    void setChildren(ExportTraceElement[] children) {
        this.children = children;
    }

    /**
     * Get the image representation of this element to be displayed in the
     * tree.
     *
     * @return the image representation
     */
    public Image getImage() {
        return null;
    }
}