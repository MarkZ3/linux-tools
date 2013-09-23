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
import java.util.Map;

import org.eclipse.linuxtools.internal.tmf.ui.Activator;
import org.eclipse.swt.graphics.Image;

public class ExportTraceBookmarkElement extends ExportTraceElement {
    private static final String BOOKMARK_IMAGE_PATH = "icons/elcl16/bookmark_obj.gif"; //$NON-NLS-1$
    private List<Map<String, String>> bookmarks;
    private List<BookmarkInfo> bookmarkInfos;

    public static class BookmarkInfo {



        public String messageAttr;
        public Integer location;

        public BookmarkInfo(Integer location, String messageAttr) {
            this.location = location;
            this.messageAttr = messageAttr;
            // TODO Auto-generated constructor stub
        }

    }

    public ExportTraceBookmarkElement(ExportTraceElement parent, List<Map<String,String>> bookmarks, List<BookmarkInfo> bookmarkInfos) {
        super(parent);
        this.bookmarks = bookmarks;
        this.bookmarkInfos = bookmarkInfos;
    }

    @Override
    public String getText() {
        return Messages.ExportTraceWizardPage_Bookmarks;
    }

    @Override
    public Image getImage() {
        return Activator.getDefault().getImageFromImageRegistry(BOOKMARK_IMAGE_PATH);
    }

    public List<Map<String, String>> getBookmarks() {
        return bookmarks;
    }

    public List<BookmarkInfo> getBookmarkInfos() {
        return bookmarkInfos;
    }

}