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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Arrays;

import org.eclipse.linuxtools.tmf.core.trace.ITmfCheckpoint;

/**
 * @since 3.0
 */
public class BTree {

    private static final int NULL_CHILD = -1;
    private static final int VERSION = 0;
    private static final int INT_SIZE = 4;
    private static final int LONG_SIZE = 8;

    final int DEGREE;
    final int MAX_RECORDS;
    final int MAX_CHILDREN;
    final int MIN_RECORDS;
    //final int OFFSET_CHILDREN;
    final int MEDIAN_RECORD;
    RandomAccessFile file;
    static long cacheHits = 0;
    private CacheHeader cacheHeader = null;
    private BTreeNodeCache nodeCache;

    class BTreeNodeCache {
        BTreeNode[] cachedNodes;
        long[]      cachedNodesOffsets;
        BTreeNode getNode(long offset) {
            BTreeNode node = null;
            int index = Arrays.binarySearch(cachedNodesOffsets, offset);
            if (index > 0) {
                ++cacheHits;
                return cachedNodes[index];
            }
            return node;
        }
    };

    private class CacheHeader {
        int version;
        long root;
        void serializeIn(FileChannel fileChannel) {

        }

        int SIZE = INT_SIZE + LONG_SIZE;
    }

//    static final int MAX_RECORDS = 10;
//    static final int MEDIAN_RECORD = MAX_RECORDS / 2;
//    static final int MAX_CHILDREN = MAX_RECORDS + 1;
//    long root;

//    void accept(IBTreeVisitor visitor) {
//        if (root != null) {
//            root.accept(visitor);
//        }
//    }

    public BTree(int degree, File file) {
        nodeCache = new BTreeNodeCache();
        boolean exists = file.exists();
        try {
            this.file = new RandomAccessFile(file, "rw");
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        this.DEGREE = degree;
        this.MIN_RECORDS = DEGREE - 1;
        this.MAX_RECORDS = 2*DEGREE - 1;
        this.MAX_CHILDREN = 2*DEGREE;
        //this.OFFSET_CHILDREN = MAX_RECORDS * Database.INT_SIZE;
        this.MEDIAN_RECORD = DEGREE - 1;

        if (!exists) {
            cacheHeader = new CacheHeader();
            cacheHeader.root = 0;
        }

        cacheHeader.serializeIn(this.file.getChannel());
    }

    public void insert(ITmfCheckpoint checkpoint) {

        System.out.println("inserting " + checkpoint.getTimestamp());
        insert(checkpoint, cacheHeader.root, null, 0);
    }

    private void insert(ITmfCheckpoint checkpoint, long nodeOffset, BTreeNode parent, int iParent) {
        BTreeNode node = nodeCache.getNode(nodeOffset);

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
            long newNodeOffset = allocateNode();
            BTreeNode newnode = new BTreeNode();
//                long newnode = allocateNode();
//                Chunk newchunk = db.getChunk(newnode);
            for (int i = 0; i < MEDIAN_RECORD; ++i) {
                newnode.setKey(i, node.getKey(MEDIAN_RECORD + 1 + i));
                node.setKey(MEDIAN_RECORD + 1 + i, null);
                newnode.setChild(i, node.getChild(MEDIAN_RECORD + 1 + i));
                node.setChild(MEDIAN_RECORD + 1 + i, NULL_CHILD);
            }
            newnode.setChild(MEDIAN_RECORD, node.getChild(MAX_RECORDS));
            node.setChild(MAX_RECORDS, NULL_CHILD);

            if (parent == null) {
                long newRootOffset = allocateNode();
                parent = new BTreeNode();
                parent.setChild(0, nodeOffset);
                cacheHeader.root = newRootOffset;
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
            parent.setChild(iParent + 1, newNodeOffset);

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
        long child = node.getChild(i);
        if (child != NULL_CHILD) {
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

    private long allocateNode() {
        try {
            // TODO, cache this
            return file.length();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return -1;
    }

    public void accept(IBTreeVisitor treeVisitor) {
        // TODO Auto-generated method stub
        accept(cacheHeader.root, treeVisitor);
    }

    private boolean accept(long nodeOffset, IBTreeVisitor visitor) {

        // If found is false, we are still in search mode.
        // Once found is true visit everything.
        // Return false when ready to quit.
        BTreeNode node = nodeCache.getNode(nodeOffset);
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
//        BTreeNode children[];
        Long children[];
        int offset;
        int size;

        public BTreeNode() {
//            this.offset = offset;
//            this.size = size;
            keys = new ITmfCheckpoint[MAX_RECORDS];
//            children = new BTreeNode[MAX_CHILDREN];
            children = new Long[MAX_CHILDREN];
            Arrays.fill(children, NULL_CHILD);
        }

        public void accept(IBTreeVisitor visitor) {
            for (ITmfCheckpoint key : keys) {
                visitor.visit(key);
            }
        }

        ITmfCheckpoint getKey(int i) {
            return keys[i];
        }

        long getChild(int i) {
            return children[i];
        }

        public void setKey(int i, ITmfCheckpoint c) {
            keys[i] = c;
        }

        public void setChild(int i, long n) {
            children[i] = n;
        }
    }
}
