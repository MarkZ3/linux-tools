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

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

class ExportTraceContentProvider implements ITreeContentProvider {

    @Override
    public void dispose() {
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

    @Override
    public Object[] getElements(Object inputElement) {
        if (inputElement instanceof ExportTraceElement[]) {
            return (ExportTraceElement[]) inputElement;
        }
        return null;
    }

    @Override
    public Object[] getChildren(Object parentElement) {
        return ((ExportTraceElement) parentElement).getChildren();
    }

    @Override
    public Object getParent(Object element) {
        return ((ExportTraceElement) element).getParent();
    }

    @Override
    public boolean hasChildren(Object element) {
        ExportTraceElement traceTransferElement = (ExportTraceElement) element;
        return traceTransferElement.getChildren() != null && traceTransferElement.getChildren().length > 0;
    }

}