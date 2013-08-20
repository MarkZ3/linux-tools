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
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.indexer.checkpoint.ITmfCheckpoint;

/**
 * @since 3.0
 */
public class FlatArray {
    private static final int INT_SIZE = 4;

    ITmfTrace trace;

    private static final int VERSION = 0;
    private int numCheckpoints = 0;
    private int cacheMisses = 0;
    private int fSize = 0;
    private boolean fCreatedFromScratch;

    private RandomAccessFile fRandomAccessFile;
    private File fFile;

    // Cached values
    private int checkpointSize = 0;
    private FileChannel fileChannel;

    private TmfTimeRange fTimeRange;

    private long fNbEvents;

    private final static int HEADER_SIZE = INT_SIZE + INT_SIZE + INT_SIZE;
    private final static int NUM_OFFSET = INT_SIZE + INT_SIZE;

    public FlatArray(File indexFile, ITmfTrace trace) {
        this.trace = trace;
        try {
            fFile = indexFile;
            fCreatedFromScratch = !indexFile.exists();
            this.fRandomAccessFile = new RandomAccessFile(indexFile, "rw"); //$NON-NLS-1$
            int traceCheckpointSize = trace.getCheckointSize();
            if (!fCreatedFromScratch) {
                int version = fRandomAccessFile.readInt();
                checkpointSize = fRandomAccessFile.readInt();
                numCheckpoints = fRandomAccessFile.readInt();

                if (version != VERSION || checkpointSize != traceCheckpointSize) {
                    boolean delete = indexFile.delete();
                    if (delete) {
                        this.fRandomAccessFile = new RandomAccessFile(indexFile, "rw"); //$NON-NLS-1$
                        fCreatedFromScratch = true;
                        numCheckpoints = 0;
                    } else {
                        Activator.logError("Unable to delete outdated ranks file, index will no work");
                        return;
                    }
                }
            }

            checkpointSize = traceCheckpointSize;

            if (fCreatedFromScratch) {
                fRandomAccessFile.writeInt(VERSION);
                fRandomAccessFile.writeInt(checkpointSize);
                fRandomAccessFile.writeInt(numCheckpoints);
            }

            fileChannel = fRandomAccessFile.getChannel();

        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

//    /**
//     * @return true if the FlatArray could be restored from disk, false otherwise
//     */
//    private boolean tryRestore() {
//
//    }

    /**
     * Dispose and delete the FlatArray
     */
    public void delete() {
        dispose();
        if (fFile.exists()) {
            fFile.delete();
        }
    }

    /**
     * Insert a checkpoint into the file-backed array
     *
     * @param checkpoint the checkpoint to insert
     */
    public void insert(ITmfCheckpoint checkpoint) {
        try {
            ++numCheckpoints;
            fRandomAccessFile.seek(fRandomAccessFile.length());
            ByteBuffer bb = ByteBuffer.allocate(checkpointSize);
            checkpoint.serializeOut(bb);
            bb.flip();
            fileChannel.write(bb);
        } catch (IOException e) {
            Activator.logError("Unable to write event to ranks file", e);
        }
    }

    /**
     * Get a checkpoint from a rank
     *
     * @param rank the rank to search
     * @return the checkpoint that has been found or null if not found
     */
    public ITmfCheckpoint get(int rank) {
        ITmfCheckpoint checkpoint = null;
        try {
            int pos = HEADER_SIZE + checkpointSize * rank;
            fRandomAccessFile.seek(pos);
            ByteBuffer bb = ByteBuffer.allocate(checkpointSize);
            fileChannel.read(bb);
            bb.flip();
            checkpoint = trace.restoreCheckPoint(bb);
        } catch (IOException e) {
            Activator.logError("Unable to read event from ranks file", e);
        }
        return checkpoint;
    }

    /**
     * Dispose the structure and its resources
     */
    public void dispose() {
        try {
            fRandomAccessFile.seek(NUM_OFFSET);
            fRandomAccessFile.writeInt(numCheckpoints);
            fRandomAccessFile.close();
        } catch (IOException e) {
            Activator.logError(MessageFormat.format("Error closing index. File: {0}", fRandomAccessFile, e));
        }
    }

    /**
     * Search for a checkpoint and return the rank.
     *
     * @param checkpoint the checkpoint to search
     * @return
     */
    public int binarySearch(ITmfCheckpoint checkpoint) {
        if (numCheckpoints == 1) {
            return 0;
        }

        int lower = 0;
        int upper = numCheckpoints - 1;
        int lastMiddle = -1;
        int middle = 0;
        while (lower <= upper && lastMiddle != middle) {
            lastMiddle = middle;
            middle = (lower + upper) / 2;
            ITmfCheckpoint found = get(middle);
            ++cacheMisses;
            int compare = checkpoint.compareTo(found);
            if (compare == 0) {
                return middle;
            }

            if (compare < 0) {
                upper= middle;
            } else {
                lower= middle + 1;
            }
        }
        return lower -1;
    }

    /**
    *
    * @return true if the FlatArray was created from scratch, false otherwise
    */
   public boolean isCreatedFromScratch() {
       return fCreatedFromScratch;
   }

    /**
     * @return
     */
    public long getCacheMisses() {
        return cacheMisses;
    }

    /**
     * Returns the size of the FlatArray expressed as a number of checkpoints.
     *
     * @return the size of the FlatArray
     */
    public int size() {
        return fSize;
    }

    /**
     * Set the size of the FlatArray, expressed as a number of checkpoints
     *
     * @param size the size of the FlatArray
     */
    public void setSize(int size) {
        fSize = size;
    }

    /**
     * Set the trace time range
     *
     * @param timeRange the trace time range
     */
    public void setTimeRange(TmfTimeRange timeRange) {
        fTimeRange = timeRange;
    }

    /**
     * Get the trace time range
     *
     * @return the trace time range
     */
    public TmfTimeRange getTimeRange() {
        return fTimeRange;
    }

    /**
     * Set the number of events in the trace
     *
     * @param nbEvents the number of events in the trace
     */
    public void setNbEvents(long nbEvents) {
        fNbEvents = nbEvents;
    }

    /**
     * Get the number of events in the trace
     *
     * @return the number of events in the trace
     */
    public long getNbEvents() {
        return fNbEvents;
    }
}
