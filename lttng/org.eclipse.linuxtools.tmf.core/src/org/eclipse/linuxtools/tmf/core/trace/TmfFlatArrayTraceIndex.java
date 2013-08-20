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

import java.io.File;

import org.eclipse.linuxtools.internal.tmf.core.trace.index.FlatArray;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.linuxtools.tmf.core.trace.indexer.checkpoint.ITmfCheckpoint;

/**
 * A checkpoint index that uses a FlatArray to store and search checkpoints by time stamps and by rank.
 *
 * @since 3.0
 */
public class TmfFlatArrayTraceIndex implements ITmfTraceIndex {

    private FlatArray fDatabase;

    private final String INDEX_FILE_NAME = "index.ht"; //$NON-NLS-1$

    /**
     * Creates an index for the given trace
     *
     * @param trace the trace
     */
    public TmfFlatArrayTraceIndex(ITmfTrace trace) {
        fDatabase = new FlatArray(getIndexFile(trace, INDEX_FILE_NAME), trace);
    }

    private static File getIndexFile(ITmfTrace trace, String fileName) {
        String directory = TmfTraceManager.getSupplementaryFileDir(trace);
        return new File(directory + fileName);
    }

    @Override
    public void dispose() {
        fDatabase.dispose();
    }

    @Override
    public void add(ITmfCheckpoint checkpoint) {
        checkpoint.setRank(fDatabase.size());
        fDatabase.insert(checkpoint);
        fDatabase.setSize(fDatabase.size() + 1);
    }

    @Override
    public ITmfCheckpoint get(int checkpoint) {
        return fDatabase.get(checkpoint);
    }

    @Override
    public int binarySearch(ITmfCheckpoint checkpoint) {
        return fDatabase.binarySearch(checkpoint);
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public int size() {
        return fDatabase.size();
    }

    @Override
    public boolean isCreatedFromScratch() {
        return !fDatabase.isCreatedFromScratch();
    }

    @Override
    public void setTimeRange(TmfTimeRange timeRange) {
        fDatabase.setTimeRange(timeRange);
    }

    @Override
    public void setNbEvents(long nbEvents) {
        fDatabase.setNbEvents(nbEvents);
    }

    @Override
    public TmfTimeRange getTimeRange() {
        return fDatabase.getTimeRange();
    }

    @Override
    public long getNbEvents() {
        return fDatabase.getNbEvents();
    }

}
