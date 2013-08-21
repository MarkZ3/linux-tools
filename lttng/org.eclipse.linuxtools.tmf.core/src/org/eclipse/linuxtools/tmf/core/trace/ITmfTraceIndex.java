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

import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.linuxtools.tmf.core.trace.indexer.checkpoint.ITmfCheckpoint;

/**
 *
 *
 * @since 3.0
 */
public interface ITmfTraceIndex {

    /**
     * Add a checkpoint to the index
     *
     * @param checkpoint the checkpoint to add
     */
    void add(ITmfCheckpoint checkpoint);

    /**
     * Get a checkpoint by rank
     *
     * @param rank the rank to search for
     * @return the checkpoint found for the given rank
     */
    ITmfCheckpoint get(int rank);

    /**
     * Find the rank of a checkpoint
     *
     * @param checkpoint the checkpoint to search for
     * @return the rank of the checkpoint or a negative value if not found
     */
    int binarySearch(ITmfCheckpoint checkpoint);

    /**
     * Returns whether or not the index is empty
     *
     * @return true if empty false otherwise
     */
    boolean isEmpty();

    /**
     * Returns the number of checkpoints in the index
     *
     * @return the number of checkpoints
     */
    int size();

    /**
     * Dispose the index and its resources
     */
    void dispose();

    /**
     * Restore the index
     *
     * @return true on success, false on failure
     */
    public boolean isCreatedFromScratch();

    /**
     * Set trace time range to be stored in the index
     *
     * @param timeRange the time range to be stored in the index
     */
    void setTimeRange(TmfTimeRange timeRange);

    /**
     * Set the total number of events in the trace to be stored in the index
     *
     * @param nbEvents the total number of events
     */
    void setNbEvents(long nbEvents);

    /**
     * Get the trace time range stored in the index
     *
     * @return the trace time range
     */
    TmfTimeRange getTimeRange();

    /**
     * Get the total number of events in the trace stored in the index
     *
     * @return the total number of events
     */
    long getNbEvents();
}
