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

public class ExportTraceBookmarkElement extends ExportTraceElement {
    private static final String BOOKMARK_IMAGE_PATH = "icons/elcl16/bookmark_obj.gif"; //$NON-NLS-1$

    public ExportTraceBookmarkElement(ExportTraceElement parent) {
        super(parent);
    }

    @Override
    public String getText() {
        return Messages.ExportTraceWizardPage_Bookmarks;
    }

    @Override
    public Image getImage() {
        return Activator.getDefault().getImageFromImageRegistry(BOOKMARK_IMAGE_PATH);
    }

}