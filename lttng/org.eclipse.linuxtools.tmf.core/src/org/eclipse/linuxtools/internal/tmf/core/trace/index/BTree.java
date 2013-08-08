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
package org.eclipse.linuxtools.internal.tmf.core.trace.index;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.linuxtools.internal.tmf.core.IndexHelper;
import org.eclipse.linuxtools.tmf.core.trace.ITmfCheckpoint;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;

/**
 * @since 3.0
 */
@SuppressWarnings({"javadoc", "unused"})
public class BTree {

    private static final int NULL_CHILD = -1;
    private static final int VERSION = 0;
    private static final int INT_SIZE = 4;
    private static final int LONG_SIZE = 8;
    private static final int CACHE_SIZE = 2;

    ITmfTrace trace;

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

    public void dispose() {
        try {
            file.seek(0);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        cacheHeader.serializeOut(file.getChannel());
        nodeCache.flush();
    }

    class BTreeNodeCache {
        ArrayDeque<BTreeNode> cachedNodes = new ArrayDeque<BTree.BTreeNode>(CACHE_SIZE);
        BTreeNode getNode(long offset) {
            for (BTreeNode nodeSearch : cachedNodes) {
                if (nodeSearch.getOffset() == offset) {
                    ++cacheHits;
                    return nodeSearch;
                }
            }

            BTreeNode node = new BTreeNode(offset);
            node.load();
            addNode(node);

            return node;
        }
        public void flush() {
            for (BTreeNode nodeSearch : cachedNodes) {
                nodeSearch.flush();
            }
            cachedNodes.clear();
        }
        public void addNode(BTreeNode node) {
            if (cachedNodes.size() == CACHE_SIZE) {
                BTreeNode removed = cachedNodes.removeLast();
                removed.flush();
            }
            cachedNodes.push(node);
        }
    }

    private class CacheHeader {
        int version;
        long root;
        void serializeIn(FileChannel fileChannel) {
            try {
                root = file.readLong();
                nodeCache.getNode(root);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        void serializeOut(FileChannel fileChannel) {
            try {
                file.writeLong(root);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        int SIZE = INT_SIZE + LONG_SIZE;
    }

    public BTree(int degree, File file, ITmfTrace trace) {
        this.trace = trace;
        nodeCache = new BTreeNodeCache();
        boolean exists = file.exists();
        try {
            this.file = new RandomAccessFile(file, "rw"); //$NON-NLS-1$
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

        cacheHeader = new CacheHeader();
        if (exists) {
            cacheHeader.serializeIn(this.file.getChannel());
        } else {
            cacheHeader.serializeOut(this.file.getChannel());
            cacheHeader.root = allocateNode().getOffset();
        }

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
            BTreeNode newnode = allocateNode();
            long newNodeOffset = newnode.getOffset();
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
                parent = allocateNode();
                parent.setChild(0, nodeOffset);
                cacheHeader.root = parent.getOffset();
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

    int btreeNodeSize = - 1;
    int getNodeSize() {
        if (btreeNodeSize == -1) {
            btreeNodeSize = INT_SIZE; // num entries
            btreeNodeSize += trace.getCheckointSize() * MAX_RECORDS;
            btreeNodeSize += LONG_SIZE * MAX_CHILDREN;
        }

        return btreeNodeSize;
    }

    private BTreeNode allocateNode() {
        try {
            // TODO, cache this
            long offset = file.length();
            file.setLength(offset + getNodeSize());
            BTreeNode node = new BTreeNode(offset);
            nodeCache.addNode(node);
            return node;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public void accept(IBTreeVisitor treeVisitor) {
        // TODO Auto-generated method stub
        accept(cacheHeader.root, treeVisitor);
    }

    private boolean accept(long nodeOffset, IBTreeVisitor visitor) {

        // If found is false, we are still in search mode.
        // Once found is true visit everything.
        // Return false when ready to quit.
        if (nodeOffset == NULL_CHILD) {
            return true;
        }

        BTreeNode node = nodeCache.getNode(nodeOffset);

        try {
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

    static int checkPointSize = -1;

    /**
     * @since 3.0
     */
    private class BTreeNode {
        ITmfCheckpoint keys[];
        long children[];
        long offset;
        int numEntry = 0;

        void flush() {
            try {
                if ((offset - 8) % getNodeSize() != 0) {
                    throw new IllegalStateException("Misaligned node (" + offset + ") :  " + this);
                }

                file.seek(offset);
                file.writeInt(numEntry);
                for (int i = 0; i < numEntry; ++i) {
                    OutputStream outputStream = Channels.newOutputStream(file.getChannel());

                    long filePointer = file.getFilePointer();
                    ITmfCheckpoint key = keys[i];
                    key.serialize(outputStream);
                    long cSize = file.getFilePointer() - filePointer;
                    if (cSize > trace.getCheckointSize()) {
                        throw new IllegalStateException("Oversize checkpoint (" + cSize + ") :  " + key);
                    }
                    file.getChannel().force(false);
                    file.seek(filePointer);
                    InputStream inputStream = Channels.newInputStream(file.getChannel());
                    ITmfCheckpoint verifyKey = trace.restoreCheckPoint(inputStream);
                    if (!verifyKey.getTimestamp().equals(key.getTimestamp())) {
                        throw new IllegalStateException("timestamp not properly saved (" + key.getTimestamp() + " != " + verifyKey.getTimestamp() + ") :  " + key);
                    }
                }

                for (int i = 0; i < MAX_CHILDREN; ++i) {
                    file.writeLong(children[i]);
                }

                long filePointer = file.getFilePointer();
                if (filePointer - offset > getNodeSize()) {
                    throw new IllegalStateException(filePointer - offset + " > " + getNodeSize());
                }

                file.getChannel().force(false);

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        public long getOffset() {
            return offset;
        }

        public BTreeNode(long offset) {
            this.offset = offset;
            keys = new ITmfCheckpoint[MAX_RECORDS];
            children = new long[MAX_CHILDREN];
            Arrays.fill(children, NULL_CHILD);
        }

        void load() {
            try {
                if ((offset - 8) % getNodeSize() != 0) {
                    throw new IllegalStateException("Misaligned loading node (" + offset + ") :  " + this);
                }
                file.seek(offset);
                numEntry = file.readInt();
                for (int i = 0; i < numEntry; ++i) {
                    InputStream inputStream = Channels.newInputStream(file.getChannel());
                    keys[i] = trace.restoreCheckPoint(inputStream);
                }

                for (int i = 0; i < MAX_CHILDREN; ++i) {
                    children[i] = file.readLong();
                }

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
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
            // Update number of entries
            if (keys[i] == null && c != null) {
                ++numEntry;
            } else if (keys[i] != null && c == null) {
                numEntry = Math.max(0, numEntry - 1);
            }

            if (numEntry < 0 || numEntry > MAX_RECORDS) {
                throw new IllegalStateException();
            }

            keys[i] = c;
        }

        public void setChild(int i, long n) {
            children[i] = n;
        }
    }
}
