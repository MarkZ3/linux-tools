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

import org.eclipse.core.resources.IResource;
import org.eclipse.linuxtools.internal.tmf.ui.Activator;
import org.eclipse.swt.graphics.Image;

public class TracePackageSupplFileElement extends TracePackageElement {

    private static final String SUPPL_FILE_ICON_PATH = "icons/obj16/thread_obj.gif"; //$NON-NLS-1$

    private IResource fResource;

    private String fSuppFileName;

    public TracePackageSupplFileElement(IResource resource, TracePackageElement parent) {
        super(parent);
        this.fResource = resource;
    }

    public TracePackageSupplFileElement(String suppFileName, TracePackageElement parent) {
        super(parent);
        this.fSuppFileName = suppFileName;
    }

    public IResource getResource() {
        return fResource;
    }

    @Override
    public String getText() {
        return fResource != null ? fResource.getName() : fSuppFileName;
    }

    @Override
    public long getSize() {
        return fResource.getLocation().toFile().length() + super.getSize();
    }

    @Override
    public Image getImage() {
        return Activator.getDefault().getImageFromImageRegistry(SUPPL_FILE_ICON_PATH);
    }

}