/*******************************************************************************
 * Copyright (c) 2013 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Marc-Andre Laperle - Initial API and implementation
 *******************************************************************************/
package org.eclipse.linuxtools.internal.tmf.core.trace.index;

/**
 * @since 3.0
 */
public class BTreeKey {
    BTreeNode prev;
    BTreeNode next;

    public BTreeNode getPrev() {
        return prev;
    }
    public void setPrev(BTreeNode prev) {
        this.prev = prev;
    }
    public BTreeNode getNext() {
        return next;
    }
    public void setNext(BTreeNode next) {
        this.next = next;
    }
}
