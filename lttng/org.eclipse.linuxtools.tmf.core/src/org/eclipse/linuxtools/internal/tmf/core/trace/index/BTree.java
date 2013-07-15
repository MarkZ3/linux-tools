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
public class BTree {

    static final int MAX_RECORDS = 10;
    static final int MEDIAN_RECORD = MAX_RECORDS / 2;
    static final int MAX_CHILDREN = MAX_RECORDS + 1;
    BTreeNode root;

//    void accept(IBTreeVisitor visitor) {
//        if (root != null) {
//            root.accept(visitor);
//        }
//    }

    public BTree() {
        root = new BTreeNode();
    }

    public void insert(ITmfCheckpoint checkpoint) {

        insert(checkpoint, root, null, 0);
    }

    private void insert(ITmfCheckpoint checkpoint, BTreeNode node, BTreeNode parent, int iParent) {

     // If this node is full (last record isn't null), split it.
        if (node.getKey(MAX_RECORDS - 1) != null) {
            ITmfCheckpoint median = node.getKey(MEDIAN_RECORD);
            if (median.compareTo(checkpoint) == 0) {
                // Found it, never mind.
//                return median;
                return;
            }

            // Split it.
            // Create the new node and move the larger records over.
            BTreeNode newnode = new BTreeNode();
//                long newnode = allocateNode();
//                Chunk newchunk = db.getChunk(newnode);
            for (int i = 0; i < MEDIAN_RECORD; ++i) {
                newnode.setKey(i, node.getKey(MEDIAN_RECORD + 1 + i));
                node.setKey(MEDIAN_RECORD + 1 + i, null);
                newnode.setChild(i, node.getChild(MEDIAN_RECORD + 1 + i));
                node.setChild(MEDIAN_RECORD + 1 + i, null);
            }
            newnode.setChild(MEDIAN_RECORD, node.getChild(MAX_RECORDS));
            node.setChild(MAX_RECORDS, null);

            if (parent == null) {
                // Create a new root
                parent = new BTreeNode();
                parent.setChild(0, node);
            } else {
                // Insert the median into the parent.
                for (int i = MAX_RECORDS - 2; i >= iParent; --i) {
                    ITmfCheckpoint r = parent.getKey(i);
                    if (r != null) {
                        parent.setKey(i + 1, r);
                        parent.setChild(i + 2, parent.getChild(i + 1));
                    }
                }
            }
            parent.setKey(iParent, median);
            parent.setChild(iParent + 1, newnode);

            node.setKey(MEDIAN_RECORD, null);

            // Set the node to the correct one to follow.
            if (checkpoint.compareTo(median) > 0) {
                node = newnode;
            }
        }

        // Binary search to find the insert point.
        int lower= 0;
        int upper= MAX_RECORDS - 1;
        while (lower < upper && node.getKey(upper - 1) == null) {
            upper--;
        }

        while (lower < upper) {
            int middle= (lower + upper) / 2;
            ITmfCheckpoint checkRec= node.getKey(middle);
            if (checkRec == null) {
                upper= middle;
            } else {
                int compare= checkRec.compareTo(checkpoint);
                if (compare > 0) {
                    upper= middle;
                } else if (compare < 0) {
                    lower= middle + 1;
                } else {
                    // Found it, no insert, just return the matched record.
//                    return checkRec;
                    return;
                }
            }
        }
        final int i= lower;
        BTreeNode child = node.getChild(i);
        if (child != null) {
            // Visit the children.
            insert(checkpoint, child, node, i);
        } else {
            // We are at the leaf, add us in.
            // First copy everything after over one.
            for (int j = MAX_RECORDS - 2; j >= i; --j) {
                ITmfCheckpoint r = node.getKey(j);
                if (r != null) {
                    node.setKey(j + 1, r);
                }
            }
            node.setKey(i, checkpoint);
            return;
        }

//        root.accept(b);
    }
}
