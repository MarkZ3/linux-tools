package org.eclipse.linuxtools.tmf.core.trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @since 3.0
 */
@SuppressWarnings("javadoc")
public class TmfMemoryIndex implements ITmfIndex {

    List<ITmfCheckpoint> fCheckpoints;

    /**
     * @param trace
     *
     */
    public TmfMemoryIndex(ITmfTrace trace) {
        fCheckpoints = new ArrayList<ITmfCheckpoint>();
    }

    @Override
    public void dispose() {
        fCheckpoints.clear();
    }

    @Override
    public void add(ITmfCheckpoint checkpoint) {
        fCheckpoints.add(checkpoint);
    }

    @Override
    public ITmfCheckpoint get(int checkpoint) {
        return fCheckpoints.get(checkpoint);
    }

    @Override
    public int binarySearch(ITmfCheckpoint checkpoint) {
        return Collections.binarySearch(fCheckpoints, checkpoint);
    }

    @Override
    public boolean isEmpty() {
        return fCheckpoints.isEmpty();
    }

    @Override
    public int size() {
        return fCheckpoints.size();
    }

    public boolean restore() {
        return false;
    }
}
