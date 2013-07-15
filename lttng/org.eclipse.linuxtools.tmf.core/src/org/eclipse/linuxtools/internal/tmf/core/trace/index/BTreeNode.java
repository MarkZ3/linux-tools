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

import org.eclipse.linuxtools.tmf.core.trace.ITmfCheckpoint;


/**
 * @since 3.0
 */
public class BTreeNode {
    ITmfCheckpoint keys[];
    BTreeNode children[];

    public BTreeNode() {
        keys = new ITmfCheckpoint[BTree.MAX_RECORDS];
        children = new BTreeNode[BTree.MAX_CHILDREN];
    }

    public void accept(IBTreeVisitor visitor) {
        for (ITmfCheckpoint key : keys) {
            visitor.visit(key);
        }
    }

    ITmfCheckpoint getKey(int i) {
        return keys[i];
    }

    BTreeNode getChild(int i) {
        return children[i];
    }

    public void setKey(int i, ITmfCheckpoint c) {
        keys[i] = c;
    }

    public void setChild(int i, BTreeNode n) {
        children[i] = n;
    }
}
