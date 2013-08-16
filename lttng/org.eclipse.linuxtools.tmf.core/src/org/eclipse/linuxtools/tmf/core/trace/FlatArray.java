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
package org.eclipse.linuxtools.tmf.core.trace;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;

import org.eclipse.linuxtools.internal.tmf.core.Activator;
import org.eclipse.linuxtools.tmf.core.trace.indexer.checkpoint.ITmfCheckpoint;

/**
 * @since 3.0
 */
public class FlatArray {
    private static final int INT_SIZE = 4;
//    private static final int LONG_SIZE = 8;

    ITmfTrace trace;

    private static final int VERSION = 0;
    private int checkpointSize = 0;
    private int numCheckpoints = 0;
    private int cacheMisses = 0;

    private RandomAccessFile file;

    private FileChannel fileChannel;
    private final static int HEADER_SIZE = INT_SIZE + INT_SIZE + INT_SIZE;
    private final static int NUM_OFFSET = INT_SIZE + INT_SIZE;

    public FlatArray(File indexFile, ITmfTrace trace) {
        this.trace = trace;
        try {
            boolean createdFromScratch = !indexFile.exists();
            this.file = new RandomAccessFile(indexFile, "rw"); //$NON-NLS-1$
            int traceCheckpointSize = trace.getCheckointSize();
            if (!createdFromScratch) {
                int version = file.readInt();
                checkpointSize = file.readInt();
                numCheckpoints = file.readInt();

                if (version != VERSION || checkpointSize != traceCheckpointSize) {
                    boolean delete = indexFile.delete();
                    if (delete) {
                        this.file = new RandomAccessFile(indexFile, "rw"); //$NON-NLS-1$
                        createdFromScratch = true;
                        numCheckpoints = 0;
                    } else {
                        Activator.logError("Unable to delete outdated ranks file, index will no work");
                        return;
                    }
                }
            }

            checkpointSize = traceCheckpointSize;

            if (createdFromScratch) {
                file.writeInt(VERSION);
                file.writeInt(checkpointSize);
                file.writeInt(numCheckpoints);
            }

            fileChannel = file.getChannel();

        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Insert a checkpoint into the file-backed array
     *
     * @param checkpoint
     */
    public void insert(ITmfCheckpoint checkpoint) {
        try {
            ++numCheckpoints;
            file.seek(file.length());
            BufferedOutputStream outputStream = new BufferedOutputStream(Channels.newOutputStream(fileChannel), checkpointSize);
            //OutputStream outputStream = Channels.newOutputStream(fileChannel);
            checkpoint.serialize(outputStream);
            outputStream.flush();
        } catch (IOException e) {
            Activator.logError("Unable to write event to ranks file", e);
        }
    }

    public ITmfCheckpoint get(int rank) {
        ITmfCheckpoint checkpoint = null;
        try {
            int pos = HEADER_SIZE + checkpointSize * rank;
            file.seek(pos);
            BufferedInputStream inputStream = new BufferedInputStream(Channels.newInputStream(fileChannel), checkpointSize);
            checkpoint = trace.restoreCheckPoint(inputStream);
        } catch (IOException e) {
            Activator.logError("Unable to read event from ranks file", e);
        }
        return checkpoint;
    }

    public void dispose() {
        try {
            file.seek(NUM_OFFSET);
            file.writeInt(numCheckpoints);
            file.close();
//            System.out.println("Cache misses: " + cacheMisses);
        } catch (IOException e) {
            Activator.logError("Error closing ranks file", e);
        }
    }

    public ITmfCheckpoint binarySearch2(ITmfCheckpoint checkpoint) {
        int lower = 0;
        int upper = numCheckpoints - 1;
        while (lower < upper) {
            int middle = (lower + upper) / 2;
            ITmfCheckpoint found = get(middle);
            ++cacheMisses;
            int compare = checkpoint.compareTo(found);
            if (compare == 0) {
                return found;
            }

            if (compare < 0) {
                upper= middle;
            } else {
                lower= middle + 1;
            }
        }
        return get(lower);
    }

    public int binarySearch(ITmfCheckpoint checkpoint) {
        int lower = 0;
        int upper = numCheckpoints - 1;
        while (lower < upper) {
            int middle = (lower + upper) / 2;
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
        return lower;
    }

    public long getCacheMisses() {
        return cacheMisses;
    }

}
