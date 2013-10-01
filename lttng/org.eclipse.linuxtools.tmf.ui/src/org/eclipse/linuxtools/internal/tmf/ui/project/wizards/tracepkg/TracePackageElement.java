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

import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.model.WorkbenchAdapter;

/**
 * An ExportTraceElement represents an item in the ExportTraceWizard
 * tree.
 */
public abstract class TracePackageElement extends WorkbenchAdapter {
    TracePackageElement[] fChildren;
    TracePackageElement fParent;
    boolean fEnabled;
    long fSize = 0;
    private boolean fChecked;

    /**
     *
     * @param parent the parent of this element, can be set to null
     */
    public TracePackageElement(TracePackageElement parent) {
        this.fParent = parent;
        fEnabled = true;
    }

    /**
     * @return the parent of this element or null if there is no parent
     */
    public Object getParent() {
        return fParent;
    }

    /**
     * Get the text representation of this element to be displayed in the
     * tree.
     *
     * @return the text representation
     */
    abstract public String getText();

    public TracePackageElement[] getChildren() {
        return fChildren;
    }

    public void setChildren(TracePackageElement[] children) {
        this.fChildren = children;
    }

    /**
     * Get the total size of the element including its children
     *
     * @return
     */
    public long getSize() {
        return 0;
    }


    /**
     * Get the total size of the element including its children
     *
     * @return
     */
    public long getCheckedSize() {
        long size = 0;
        if (fChildren != null) {
            for (TracePackageElement child : fChildren) {
                size += child.getCheckedSize();
            }
        } else if (fChecked) {
            size += getSize();
        }
        return size;
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

    public boolean isEnabled() {
        return fEnabled;
    }

    public void setEnabled(boolean enabled) {
        fEnabled = enabled;
    }

    public void setChecked(boolean state) {
        fChecked = state;
    }
}