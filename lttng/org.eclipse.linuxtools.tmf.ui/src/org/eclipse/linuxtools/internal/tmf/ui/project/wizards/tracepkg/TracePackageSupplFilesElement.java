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

import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.linuxtools.internal.tmf.ui.Activator;
import org.eclipse.swt.graphics.Image;

public class TracePackageSupplFilesElement extends TracePackageElement {

    private static final String SUPPL_FILE_ICON_PATH = "icons/obj16/thread_obj.gif"; //$NON-NLS-1$

    private IResource[] fResources;

    private List<String> fSuppFileNames;

    public TracePackageSupplFilesElement(IResource[] resources, TracePackageElement parent) {
        super(parent);
        this.fResources = resources;
    }

    public TracePackageSupplFilesElement(List<String> suppFileNames, TracePackageElement parent) {
        super(parent);
        this.fSuppFileNames = suppFileNames;
    }

    public IResource[] getResources() {
        return fResources;
    }

    @Override
    public String getText() {
        return Messages.ExportTraceWizardPage_SupplementaryFiles;
    }

    @Override
    public Image getImage() {
        return Activator.getDefault().getImageFromImageRegistry(SUPPL_FILE_ICON_PATH);
    }

    public List<String> getSuppFileNames() {
        return fSuppFileNames;
    }

}