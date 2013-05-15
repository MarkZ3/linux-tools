package org.eclipse.linuxtools.tmf.core.trace;

import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.linuxtools.tmf.core.trace.indexer.checkpoint.ITmfCheckpoint;

/**
 * @since 3.0
 */
@SuppressWarnings("javadoc")
public interface ITmfIndex {

    /**
     * @param checkpoint
     */
    void add(ITmfCheckpoint checkpoint);

    /**
     * @param checkpoint
     * @return
     */
    ITmfCheckpoint get(int checkpoint);

    /**
     * @param checkpoint
     * @return
     */
    int binarySearch(ITmfCheckpoint checkpoint);

    /**
     * @return
     */
    boolean isEmpty();

    /**
     * @return
     */
    int size();

    void dispose();

    public boolean restore();

    void setTimeRange(TmfTimeRange timeRange);

    void setNbEvents(long nbEvents);

    TmfTimeRange getTimeRange();

    long getNbEvents();
}
