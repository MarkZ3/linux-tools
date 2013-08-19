package org.eclipse.linuxtools.tmf.core.trace;

import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.linuxtools.tmf.core.trace.indexer.checkpoint.ITmfCheckpoint;

/**
 *
 *
 * @since 3.0
 */
public interface ITmfIndex {

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
    public boolean restore();

    /**
     *
     * @param timeRange
     */
    void setTimeRange(TmfTimeRange timeRange);

    void setNbEvents(long nbEvents);

    TmfTimeRange getTimeRange();

    long getNbEvents();
}
