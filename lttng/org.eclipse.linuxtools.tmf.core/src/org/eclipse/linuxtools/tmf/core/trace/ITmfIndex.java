package org.eclipse.linuxtools.tmf.core.trace;

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
}
