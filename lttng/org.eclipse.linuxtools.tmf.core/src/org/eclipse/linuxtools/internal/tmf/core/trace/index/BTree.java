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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.MessageFormat;

import org.eclipse.linuxtools.internal.tmf.core.Activator;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.indexer.checkpoint.ITmfCheckpoint;

/**
 * A BTree made of BTreeNodes representing a series of ITmfCheckpoints
 * ordered by time stamps. @link{ BTreeNodeCache } is used to improve performance by caching some nodes
 * in memory and the other nodes are kept on disk.
 */
public class BTree {

    //private static final int VERSION = 0;
    private static final int INT_SIZE = 4;
    private static final int LONG_SIZE = 8;

    ITmfTrace fTrace;

    final int fMaxNumRecords;
    final int fMaxNumChildren;
    final int fMedianRecord;
    RandomAccessFile fFile;
    FileChannel fFileChannel;
    private CacheHeader fCacheHeader = null;
    private BTreeNodeCache fNodeCache;

    TmfTimeRange fTimeRange;
    private final static boolean ALWAYS_CACHE_ROOT = true;

    class CacheHeader {
        //int version;
        int fSize = 0;
        long fRoot;
        long fNbEvents = 0;
        long fTimeRangeOffset = 0;

        int SIZE = INT_SIZE +
                LONG_SIZE +
                LONG_SIZE +
                LONG_SIZE;

        void serializeIn() throws IOException {
            fSize = fFile.readInt();
            fRoot = fFile.readLong();
            fNbEvents = fFile.readLong();
            fTimeRangeOffset = fFile.readLong();
        }

        void serializeOut() throws IOException {
            fFile.seek(0);
            fFile.writeInt(fSize);
            fFile.writeLong(fRoot);
            fFile.writeLong(fNbEvents);
            fFile.writeLong(fTimeRangeOffset);
        }
    }

    /**
     * Constructs a BTree for a given trace from scratch or from an existing file.
     * The degree is used to calibrate the number of entries in each node which
     * can affect performance. When the BTree is created from scratch, it is populated
     * by subsequent calls to {@link #insert}.
     *
     * @param degree the degree to use in the tree
     * @param file the file to use as the persistent storage
     * @param trace the trace is
     */
    public BTree(int degree, File file, ITmfTrace trace) {
        fTrace = trace;
        fNodeCache = new BTreeNodeCache(this);
        fCreatedFromScratch = !file.exists();
        try {
            this.fFile = new RandomAccessFile(file, "rw"); //$NON-NLS-1$
            fFileChannel = this.fFile.getChannel();
        } catch (FileNotFoundException e) {
            Activator.logError(MessageFormat.format(Messages.BTree_ErrorOpeningIndex, file, e));
        }

        fMaxNumRecords = 2 * degree - 1;
        fMaxNumChildren = 2 * degree;
        fMedianRecord = degree - 1;

        fCacheHeader = new CacheHeader();
        try {
            BTreeNode rootNode;
            if (fCreatedFromScratch) {
                // Write initial header, seek to the start of nodes
                fCacheHeader.serializeOut();
                rootNode = allocateNode();
                fTimeRange = new TmfTimeRange(new TmfTimestamp(0), new TmfTimestamp(0));
            } else {
                fCacheHeader.serializeIn();
                serializeInTimeRange();
                rootNode = fNodeCache.getNode(fCacheHeader.fRoot);
            }

            setRootNode(rootNode);
        } catch (IOException e) {
            Activator.logError(MessageFormat.format(Messages.BTree_IOErrorReadingHeader, file), e);
        }

    }

    /**
     * Dispose the structure and its resources
     */
    public void dispose() {
        try {
            // This only needs to be written to disk for a new BTree
            if (fCreatedFromScratch) {
                serializeOutTimeRange();

                fCacheHeader.serializeOut();
                fNodeCache.serializeOut();
            }

            fFile.close();
        } catch (IOException e) {
            Activator.logError(MessageFormat.format(Messages.BTree_IOErrorClosingIndex, fFile, e));
        }
    }

    private void serializeInTimeRange() throws IOException {
        fFile.seek(fCacheHeader.fTimeRangeOffset);
        ByteBuffer b = ByteBuffer.allocate(64);
        fFileChannel.read(b);
        b.flip();
        fTimeRange = new TmfTimeRange(TmfTimestamp.newSerialized(b), TmfTimestamp.newSerialized(b));
    }

    private void serializeOutTimeRange() throws IOException {
        fCacheHeader.fTimeRangeOffset = fFile.length();
        fFile.seek(fCacheHeader.fTimeRangeOffset);
        ByteBuffer b = ByteBuffer.allocate(64);
        fTimeRange.getStartTime().serializeOut(b);
        fTimeRange.getEndTime().serializeOut(b);
        b.flip();
        fFileChannel.write(b);
    }

    /**
     *
     * @return true if BTree was created from scratch, false otherwise
     */
    public boolean isCreatedFromScratch() {
        return fCreatedFromScratch;
    }

    /**
     * Insert a checkpoint into the file-backed BTree
     *
     * @param checkpoint the checkpoint to insert
     */
    public void insert(ITmfCheckpoint checkpoint) {
        insert(checkpoint, fCacheHeader.fRoot, null, 0);
    }

    private void setRootNode(BTreeNode newRootNode) {
        fCacheHeader.fRoot = newRootNode.getOffset();
        if (ALWAYS_CACHE_ROOT) {
            fNodeCache.setRootNode(newRootNode);
        } else {
            fNodeCache.addNode(newRootNode);
        }
    }

    private void insert(ITmfCheckpoint checkpoint, long nodeOffset, BTreeNode pParent, int iParent) {
        BTreeNode parent = pParent;
        BTreeNode node = fNodeCache.getNode(nodeOffset);

        // If this node is full (last record isn't null), split it
        if (node.getEntry(fMaxNumRecords - 1) != null) {

            ITmfCheckpoint median = node.getEntry(fMedianRecord);
            if (median.compareTo(checkpoint) == 0) {
                // Found it
                return;
            }

            // Split it.
            // Create the new node and move the larger records over.
            BTreeNode newnode = allocateNode();
            fNodeCache.addNode(newnode);
            long newNodeOffset = newnode.getOffset();
            for (int i = 0; i < fMedianRecord; ++i) {
                newnode.setEntry(i, node.getEntry(fMedianRecord + 1 + i));
                node.setEntry(fMedianRecord + 1 + i, null);
                newnode.setChild(i, node.getChild(fMedianRecord + 1 + i));
                node.setChild(fMedianRecord + 1 + i, BTreeNode.NULL_CHILD);
            }
            newnode.setChild(fMedianRecord, node.getChild(fMaxNumRecords));
            node.setChild(fMaxNumRecords, BTreeNode.NULL_CHILD);

            if (parent == null) {
                parent = allocateNode();
                setRootNode(parent);
                parent.setChild(0, nodeOffset);
            } else {
                // Insert the median into the parent.
                for (int i = fMaxNumRecords - 2; i >= iParent; --i) {
                    ITmfCheckpoint r = parent.getEntry(i);
                    if (r != null) {
                        parent.setEntry(i + 1, r);
                        parent.setChild(i + 2, parent.getChild(i + 1));
                    }
                }
            }

            fNodeCache.getNode(parent.getOffset());

            parent.setEntry(iParent, median);
            parent.setChild(iParent + 1, newNodeOffset);

            node.setEntry(fMedianRecord, null);

            // Set the node to the correct one to follow.
            if (checkpoint.compareTo(median) > 0) {
                node = newnode;
            }
        }

        // Binary search to find the insert point.
        int lower= 0;
        int upper= fMaxNumRecords - 1;
        while (lower < upper && node.getEntry(upper - 1) == null) {
            upper--;
        }

        while (lower < upper) {
            int middle= (lower + upper) / 2;
            ITmfCheckpoint checkRec= node.getEntry(middle);
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
        if (child != BTreeNode.NULL_CHILD) {
            // Visit the children.
            insert(checkpoint, child, node, i);
        } else {
            // We are at the leaf, add us in.
            // First copy everything after over one.
            for (int j = fMaxNumRecords - 2; j >= i; --j) {
                ITmfCheckpoint r = node.getEntry(j);
                if (r != null) {
                    node.setEntry(j + 1, r);
                }
            }
            node.setEntry(i, checkpoint);
            return;
        }
    }

    int btreeNodeSize = - 1;
    int getNodeSize() {
        if (btreeNodeSize == -1) {
            btreeNodeSize = INT_SIZE; // num entries
            btreeNodeSize += fTrace.getCheckointSize() * fMaxNumRecords;
            btreeNodeSize += LONG_SIZE * fMaxNumChildren;
        }

        return btreeNodeSize;
    }

    private BTreeNode allocateNode() {
        try {
            // TODO, cache this
            long offset = fFile.length();
            fFile.setLength(offset + getNodeSize());
            BTreeNode node = new BTreeNode(this, offset);
            return node;
        } catch (IOException e) {
            Activator.logError(MessageFormat.format(Messages.BTree_IOErrorAllocatingNode, fFile), e);
        }
        return null;
    }

    /**
     * Accept a visitor. This visitor can be used to search through the whole
     * tree.
     *
     * @param treeVisitor the visitor to accept
     */
    public void accept(IBTreeVisitor treeVisitor) {
        accept(fCacheHeader.fRoot, treeVisitor);
    }

    private boolean accept(long nodeOffset, IBTreeVisitor visitor) {

        // If found is false, we are still in search mode.
        // Once found is true visit everything.
        // Return false when ready to quit.
        if (nodeOffset == BTreeNode.NULL_CHILD) {
            return true;
        }

        BTreeNode node = fNodeCache.getNode(nodeOffset);

        try {
            // Binary search to find first record greater or equal.
            int lower = 0;
            int upper = fMaxNumRecords - 1;
            while (lower < upper && node.getEntry(upper - 1) == null) {
                upper--;
            }
            while (lower < upper) {
                int middle = (lower + upper) / 2;
                ITmfCheckpoint checkRec = node.getEntry(middle);
                if (checkRec == null) {
                    upper = middle;
                } else {
                    int compare = visitor.compare(checkRec);
                    if (compare >= 0) {
                        upper = middle;
                    } else {
                        lower = middle + 1;
                    }
                }
            }

            // Start with first record greater or equal, reuse comparison
            // results.
            int i = lower;
            for (; i < fMaxNumRecords; ++i) {
                ITmfCheckpoint record = node.getEntry(i);
                if (record == null) {
                    break;
                }

                int compare = visitor.compare(record);
                if (compare > 0) {
                    // Start point is to the left.
                    return accept(node.getChild(i), visitor);
                } else if (compare == 0) {
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
    private boolean fCreatedFromScratch;

    int getCheckpointSize() {
        if (checkPointSize == -1) {
            checkPointSize = fTrace.getCheckointSize();
        }

        return checkPointSize;
    }

    /**
     * Returns the size of the BTree expressed as a number of checkpoints.
     *
     * @return the size of the BTree
     */
    public int size() {
        return fCacheHeader.fSize;
    }

    /**
     * Set the size of the BTree, expressed as a number of checkpoints
     *
     * @param size the size of the BTree
     */
    public void setSize(int size) {
        fCacheHeader.fSize = size;
    }

    public void setTimeRange(TmfTimeRange timeRange) {
        fTimeRange = timeRange;
    }

    public void setNbEvents(long nbEvents) {
        fCacheHeader.fNbEvents = nbEvents;
    }

    public TmfTimeRange getTimeRange() {
        return fTimeRange;
    }

    /**
     *
     *
     * @return the number of events in the trace
     */
    public long getNbEvents() {
        return fCacheHeader.fNbEvents;
    }

    /**
     * @see BTreeNodeCache
     */
    public long getCacheMisses() {
        return fNodeCache.getCacheMisses();
    }
}
