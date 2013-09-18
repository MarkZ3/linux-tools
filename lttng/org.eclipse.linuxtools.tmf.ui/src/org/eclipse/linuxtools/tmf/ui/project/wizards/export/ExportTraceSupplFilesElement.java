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

import org.eclipse.core.resources.IResource;
import org.eclipse.linuxtools.internal.tmf.ui.Activator;
import org.eclipse.swt.graphics.Image;

class ExportTraceSupplFilesElement extends ExportTraceElement {

    private static final String SUPPL_FILE_ICON_PATH = "icons/obj16/thread_obj.gif"; //$NON-NLS-1$

    IResource[] resources;

    ExportTraceSupplFilesElement(IResource[] resources, ExportTraceElement parent) {
        super(parent);
        this.resources = resources;
    }

    IResource[] getResources() {
        return resources;
    }

    @Override
    public String getText() {
        return Messages.ExportTraceWizardPage_SupplementaryFiles;
    }

    @Override
    public Image getImage() {
        return Activator.getDefault().getImageFromImageRegistry(SUPPL_FILE_ICON_PATH);
    }

}