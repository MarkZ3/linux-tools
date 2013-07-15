package org.eclipse.linuxtools.tmf.core.trace;

import java.io.File;

import org.eclipse.linuxtools.internal.tmf.core.trace.index.BTree;

/**
 * @author emalape
 * @since 3.0
 *
 */
@SuppressWarnings("javadoc")
public class TmfPersistentIndex implements ITmfIndex {

    static boolean sDEBUG_LOCKS= true; // initialized in the PDOMManager, because IBM needs PDOM independent of runtime plugin.

    private BTree fDatabase;
//    private BTree fCheckpointTree;
//    private BTree fCheckpointRankTree;
    private ITmfTrace fTrace;
    private int size = 0;

    private final String INDEX_FILE_NAME = "index.ht"; //$NON-NLS-1$
    private int version = 2;

    /**
     * @param trace
     *
     */
    public TmfPersistentIndex(ITmfTrace trace) {

        fTrace = trace;
        createDatabase();
    }

    private void createDatabase() {
        fDatabase = new BTree();
    }

    private File getIndexFile() {
        String directory = TmfTraceManager.getSupplementaryFileDir(fTrace);
        return new File(directory + INDEX_FILE_NAME);
    }

    @Override
    public void dispose() {

    }

    @Override
    public void add(ITmfCheckpoint checkpoint) {
        fDatabase.insert(checkpoint);
        size++;
    }

    @Override
    public ITmfCheckpoint get(int checkpoint) {

        return null;

    }

    @Override
    public int binarySearch(ITmfCheckpoint checkpoint) {

        return -1;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public int size() {
        return size;
    }

}
