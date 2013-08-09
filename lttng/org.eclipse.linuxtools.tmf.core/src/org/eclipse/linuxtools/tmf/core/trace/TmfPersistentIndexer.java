package org.eclipse.linuxtools.tmf.core.trace;

/**
 * @since 3.0
 */
@SuppressWarnings("javadoc")
public class TmfPersistentIndexer extends TmfCheckpointIndexer {

    /**
     * @param trace
     */
    public TmfPersistentIndexer(ITmfTrace trace) {
        super(trace);
    }

    /**
     * @param trace
     * @param interval
     */
    public TmfPersistentIndexer(ITmfTrace trace, int interval) {
        super(trace, interval);
    }

    @Override
    protected ITmfIndex createIndex(ITmfTrace trace) {
        return new TmfPersistentIndex(trace);
    }
}
