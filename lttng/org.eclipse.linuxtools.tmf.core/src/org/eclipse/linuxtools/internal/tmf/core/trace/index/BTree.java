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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
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
import java.util.Arrays;

import org.eclipse.linuxtools.internal.tmf.core.IndexHelper;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.indexer.checkpoint.ITmfCheckpoint;

/**
 * @since 3.0
 */
@SuppressWarnings("javadoc")
public class BTree {

    private static final int NULL_CHILD = -1;
    //private static final int VERSION = 0;
    private static final int INT_SIZE = 4;
    private static final int LONG_SIZE = 8;
    public static int CACHE_SIZE = 5;

    ITmfTrace trace;

    final int DEGREE;
    final int MAX_RECORDS;
    final int MAX_CHILDREN;
    final int MIN_RECORDS;
    final int MEDIAN_RECORD;
    RandomAccessFile file;
    FileChannel fileChannel;
    long cacheMisses = 0;
    private CacheHeader cacheHeader = null;
    private BTreeNodeCache nodeCache;

    TmfTimeRange fTimeRange;
    public static boolean ALWAYS_CACHE_ROOT = true;

    public void dispose() {

        System.out.println("ALWAYS_CACHE_ROOT: " + ALWAYS_CACHE_ROOT);
        System.out.println("Cache size: " + CACHE_SIZE);
        System.out.println("Cache misses: " + cacheMisses);
        System.out.println("DEGREE: " + DEGREE);

        if (existed) {
            return;
        }

        try {
            serializeOutTimeRange();

            cacheHeader.serializeOut();
            nodeCache.flush();
            file.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    void serializeInTimeRange() throws IOException {
        file.seek(cacheHeader.timeStampOffset);
        InputStream inputStream = Channels.newInputStream(fileChannel);
        fTimeRange = new TmfTimeRange(TmfTimestamp.newAndSerialize(inputStream), TmfTimestamp.newAndSerialize(inputStream));
    }

    void serializeOutTimeRange() {
        try {
            cacheHeader.timeStampOffset = file.length();
            file.seek(cacheHeader.timeStampOffset);
            OutputStream s = Channels.newOutputStream(fileChannel);
            fTimeRange.getStartTime().serialize(s);
            fTimeRange.getEndTime().serialize(s);
        } catch (IOException e) {
         // TODO Auto-generated method stub
            e.printStackTrace();
        }
    }

    class BTreeNodeCache {
        BTreeNode rootNode = null;
        ArrayDeque<BTreeNode> cachedNodes = new ArrayDeque<BTree.BTreeNode>(CACHE_SIZE);
        BTreeNode getNode(long offset) {
            if (rootNode != null && rootNode.getOffset() == offset) {
                return rootNode;
            }

            for (BTreeNode nodeSearch : cachedNodes) {
                if (nodeSearch.getOffset() == offset) {
                    // This node is now the most recently used
                    cachedNodes.remove(nodeSearch);
                    cachedNodes.push(nodeSearch);

                    return nodeSearch;
                }
            }

            ++cacheMisses;

            BTreeNode node = new BTreeNode(offset);
            node.load();
            addNode(node);

            return node;
        }

//        boolean hasNode(long offset) {
//            if (rootNode != null && rootNode.getOffset() == offset) {
//                return true;
//            }
//
//            for (BTreeNode nodeSearch : cachedNodes) {
//                if (nodeSearch.getOffset() == offset) {
//                    return true;
//                }
//            }
//
//            return false;
//        }

        public void flush() {
            if (rootNode != null) {
                rootNode.flush();
            }
            for (BTreeNode nodeSearch : cachedNodes) {
                nodeSearch.flush();
            }
        }
        public void addNode(BTreeNode node) {
            if (cachedNodes.size() == CACHE_SIZE) {
                BTreeNode removed = cachedNodes.removeLast();
                if (!existed && removed.isDirty()) {
                    removed.flush();
                }
            }
            cachedNodes.push(node);
        }

        public void setRootNode(BTreeNode newRootNode) {
            BTreeNode oldRootNode = rootNode;
            rootNode = newRootNode;
            if (oldRootNode != null) {
                addNode(oldRootNode);
            }
            return;
        }
    }

    private class CacheHeader {
        //int version;
        int size = 0;
        long root;
        long nbEvents = 0;
        long timeStampOffset = 0;

        int SIZE = INT_SIZE +
                LONG_SIZE +
                LONG_SIZE +
                LONG_SIZE;

        void serializeIn() throws IOException {
            size = file.readInt();
            root = file.readLong();
            nbEvents = file.readLong();
            timeStampOffset = file.readLong();
        }

        void serializeOut() throws IOException {
            file.seek(0);
            file.writeInt(size);
            file.writeLong(root);
            file.writeLong(nbEvents);
            file.writeLong(timeStampOffset);
        }
    }

    public BTree(int degree, File file, ITmfTrace trace) {
        this.trace = trace;
        nodeCache = new BTreeNodeCache();
        existed = file.exists();
        try {
            this.file = new RandomAccessFile(file, "rw"); //$NON-NLS-1$
            fileChannel = this.file.getChannel();
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
        try {
            BTreeNode rootNode;
            if (existed) {
                cacheHeader.serializeIn();
                serializeInTimeRange();
                rootNode = nodeCache.getNode(cacheHeader.root);
            } else {
                // Write initial header, seek to the start of nodes
                cacheHeader.serializeOut();
                rootNode = allocateNode();
                fTimeRange = new TmfTimeRange(new TmfTimestamp(0), new TmfTimestamp(0));
            }

            setRootNode(rootNode);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public boolean isExisted() {
        return existed;
    }

    public int size() {
        return cacheHeader.size;
    }

    public int setSize(int size) {
        return cacheHeader.size = size;
    }

    public void insert(ITmfCheckpoint checkpoint) {

//        System.out.println("inserting " + checkpoint.getTimestamp());
        insert(checkpoint, cacheHeader.root, null, 0);
    }

    private void setRootNode(BTreeNode newRootNode) {
        cacheHeader.root = newRootNode.getOffset();
        if (ALWAYS_CACHE_ROOT) {
            nodeCache.setRootNode(newRootNode);
        } else {
            nodeCache.addNode(newRootNode);
        }
    }

    private void insert(ITmfCheckpoint checkpoint, long nodeOffset, BTreeNode pParent, int iParent) {
        BTreeNode parent = pParent;
        BTreeNode node = nodeCache.getNode(nodeOffset);

     // If this node is full (last record isn't null), split it.
        if (node.getKey(MAX_RECORDS - 1) != null) {

            ITmfCheckpoint median = node.getKey(MEDIAN_RECORD);
            if (median.compareTo(checkpoint) == 0) {
                // Found it
                return;
            }

            // Split it.
            // Create the new node and move the larger records over.
            BTreeNode newnode = allocateNode();
            nodeCache.addNode(newnode);
            long newNodeOffset = newnode.getOffset();
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
                setRootNode(parent);
                parent.setChild(0, nodeOffset);
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

            nodeCache.getNode(parent.getOffset());

//            assert(nodeCache.hasNode(parent.getOffset()));
            parent.setKey(iParent, median);
            parent.setChild(iParent + 1, newNodeOffset);

//            assert(nodeCache.hasNode(node.getOffset()));
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
                    // Found it, no insert
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
            return node;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public void accept(IBTreeVisitor treeVisitor) {
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
    private boolean existed;
    int getCheckpointSize() {
        if (checkPointSize == -1) {
            checkPointSize = trace.getCheckointSize();
        }

        return checkPointSize;
    }

    /**
     * @since 3.0
     */
    private class BTreeNode {
        ITmfCheckpoint keys[];
        long children[];
        long offset;
        int numEntry = 0;
        boolean dirty = false;

        void flush() {
            try {
                if ((offset - cacheHeader.SIZE) % getNodeSize() != 0) {
                    throw new IllegalStateException("Misaligned node (" + offset + ") :  " + this); //$NON-NLS-1$ //$NON-NLS-2$
                }

                file.seek(offset);

                BufferedOutputStream outputStream = new BufferedOutputStream(Channels.newOutputStream(fileChannel), getNodeSize());

                file.seek(offset);
                for (int i = 0; i < MAX_CHILDREN; ++i) {
                    IndexHelper.writeLong(outputStream, children[i]);
                }
                IndexHelper.writeInt(outputStream, numEntry);

//                for (int i = 0; i < MAX_CHILDREN; ++i) {
//                    file.writeLong(children[i]);
//                }
//                file.writeInt(numEntry);

                for (int i = 0; i < numEntry; ++i) {

//                    long filePointer = file.getFilePointer();
                    ITmfCheckpoint key = keys[i];
                    key.serialize(outputStream);
//                    long cSize = file.getFilePointer() - filePointer;
//                    if (cSize > getCheckpointSize()) {
//                        throw new IllegalStateException("Oversize checkpoint (" + cSize + ") :  " + key); //$NON-NLS-1$ //$NON-NLS-2$
//                    }
//                    fileChannel.force(false);
//                    file.seek(filePointer);
//                    InputStream inputStream = Channels.newInputStream(fileChannel);
//                    ITmfCheckpoint verifyKey = trace.restoreCheckPoint(inputStream);
//                    if (!verifyKey.getTimestamp().equals(key.getTimestamp())) {
//                        throw new IllegalStateException("timestamp not properly saved (" + key.getTimestamp() + " != " + verifyKey.getTimestamp() + ") :  " + key); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//                    }
//
//                    if (verifyKey.getRank() != key.getRank()) {
//                        throw new IllegalStateException("timestamp not properly saved (" + key.getRank() + " != " + verifyKey.getRank() + ") :  " + key); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//                    }
                }

//                long filePointer = file.getFilePointer();
//                if (filePointer - offset > getNodeSize()) {
//                    throw new IllegalStateException(filePointer - offset + " > " + getNodeSize()); //$NON-NLS-1$
//                }

                //fileChannel.force(false);
                outputStream.flush();
                dirty = false;
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
                if ((offset - cacheHeader.SIZE) % getNodeSize() != 0) {
                    throw new IllegalStateException("Misaligned loading node, offset: " + offset + " nodeSize: " + getNodeSize()); //$NON-NLS-1$ //$NON-NLS-2$
                }
                file.seek(offset);
                BufferedInputStream buffInputStream = new BufferedInputStream(Channels.newInputStream(fileChannel), getNodeSize());

                byte arr[] = new byte[MAX_CHILDREN * LONG_SIZE + INT_SIZE];
                buffInputStream.read(arr);
                ByteBuffer wrap = ByteBuffer.wrap(arr);
                for (int i = 0; i < MAX_CHILDREN; ++i) {
                    children[i] = wrap.getLong(i * LONG_SIZE);
                }
                numEntry = wrap.getInt(MAX_CHILDREN * LONG_SIZE);

                for (int i = 0; i < numEntry; ++i) {
                    keys[i] = trace.restoreCheckPoint(buffInputStream);
                }

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        ITmfCheckpoint getKey(int i) {
            return keys[i];
        }

        long getChild(int i) {
            return children[i];
        }

        public void setKey(int i, ITmfCheckpoint c) {
            dirty = true;
//            if(!nodeCache.hasNode(offset)) {
//                throw new IllegalStateException();
//            }
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
            dirty = true;
//            if(!nodeCache.hasNode(offset)) {
//                throw new IllegalStateException();
//            }
            children[i] = n;
        }

        public boolean isDirty() {
            return dirty;
        }
    }

    public void setTimeRange(TmfTimeRange timeRange) {
        fTimeRange = timeRange;
    }

    public void setNbEvents(long nbEvents) {
        cacheHeader.nbEvents = nbEvents;
    }

    public TmfTimeRange getTimeRange() {
        return fTimeRange;
    }

    public long getNbEvents() {
        return cacheHeader.nbEvents;
    }
}
