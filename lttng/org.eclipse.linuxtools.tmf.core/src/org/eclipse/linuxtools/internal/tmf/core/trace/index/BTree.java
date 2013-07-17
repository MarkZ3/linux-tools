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

    final int DEGREE;
    final int MAX_RECORDS;
    final int MAX_CHILDREN;
    final int MIN_RECORDS;
    //final int OFFSET_CHILDREN;
    final int MEDIAN_RECORD;

//    static final int MAX_RECORDS = 10;
//    static final int MEDIAN_RECORD = MAX_RECORDS / 2;
//    static final int MAX_CHILDREN = MAX_RECORDS + 1;
    BTreeNode root;

//    void accept(IBTreeVisitor visitor) {
//        if (root != null) {
//            root.accept(visitor);
//        }
//    }

    public BTree(int degree) {
        this.DEGREE = degree;
        this.MIN_RECORDS = DEGREE - 1;
        this.MAX_RECORDS = 2*DEGREE - 1;
        this.MAX_CHILDREN = 2*DEGREE;
        //this.OFFSET_CHILDREN = MAX_RECORDS * Database.INT_SIZE;
        this.MEDIAN_RECORD = DEGREE - 1;
        root = new BTreeNode();
    }

    public void insert(ITmfCheckpoint checkpoint) {

        System.out.println("inserting " + checkpoint.getTimestamp());
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
                root = parent;
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

    public void accept(IBTreeVisitor treeVisitor) {
        // TODO Auto-generated method stub
        accept(root, treeVisitor);
    }

    private boolean accept(BTreeNode node, IBTreeVisitor visitor) {
        // If found is false, we are still in search mode.
        // Once found is true visit everything.
        // Return false when ready to quit.

        if (node == null) {
            return true;
        }

        try {
            //Chunk chunk = db.getChunk(node);

            // Binary search to find first record greater or equal.
            int lower= 0;
            int upper= MAX_RECORDS - 1;
            while (lower < upper && node.getKey(upper - 1) == null) {
                upper--;
            }
            while (lower < upper) {
                int middle= (lower + upper) / 2;
                ITmfCheckpoint checkRec = node.getKey(middle);
                if (checkRec == null) {
                    upper= middle;
                } else {
                    int compare= visitor.compare(checkRec);
                    if (compare >= 0) {
                        upper= middle;
                    } else {
                        lower= middle + 1;
                    }
                }
            }

            // Start with first record greater or equal, reuse comparison results.
            int i= lower;
            for (; i < MAX_RECORDS; ++i) {
                ITmfCheckpoint record = node.getKey(i);
                if (record == null) {
                    break;
                }

                int compare= visitor.compare(record);
                if (compare > 0) {
                    // Start point is to the left.
                    return accept(node.getChild(i), visitor);
                }  else if (compare == 0) {
                    if (!accept(node.getChild(i), visitor)) {
                        return false;
                    }
                    if (!visitor.visit(record)) {
                        return false;
                    }
                }
            }
            return accept(node.getChild(i), visitor);
        } finally {

        }
    }

    /**
     * @since 3.0
     */
    private class BTreeNode {
        ITmfCheckpoint keys[];
        BTreeNode children[];

        public BTreeNode() {
            keys = new ITmfCheckpoint[MAX_RECORDS];
            children = new BTreeNode[MAX_CHILDREN];
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
}
