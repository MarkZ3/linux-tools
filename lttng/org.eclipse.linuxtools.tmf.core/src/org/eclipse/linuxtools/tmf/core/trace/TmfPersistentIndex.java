package org.eclipse.linuxtools.tmf.core.trace;

import java.io.File;

import org.eclipse.linuxtools.internal.tmf.core.trace.index.BTree;
import org.eclipse.linuxtools.internal.tmf.core.trace.index.IBTreeVisitor;

/**
 * @author emalape
 * @since 3.0
 *
 */
@SuppressWarnings("javadoc")
public class TmfPersistentIndex implements ITmfIndex {

    static boolean sDEBUG_LOCKS= true; // initialized in the PDOMManager, because IBM needs PDOM independent of runtime plugin.

    private BTree fDatabase;

    private final String INDEX_FILE_NAME = "index.ht"; //$NON-NLS-1$

    /**
     * @param trace
     *
     */
    public TmfPersistentIndex(ITmfTrace trace) {
        fDatabase = new BTree(8, getIndexFile(trace), trace);
    }

    private File getIndexFile(ITmfTrace trace) {
        String directory = TmfTraceManager.getSupplementaryFileDir(trace);
        return new File(directory + INDEX_FILE_NAME);
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

    class RankVisitor implements IBTreeVisitor {
        ITmfCheckpoint found = null;
        private int search;

        public RankVisitor(int search) {
            this.search = search;
        }

        @Override
        public boolean visit(ITmfCheckpoint key) {
            if (key == null) {
                return true;
            }
            found = key;
            return false;
        }

        @Override
        public int compare(ITmfCheckpoint checkRec) {
            return Integer.valueOf(checkRec.getRank()).compareTo(search);
        }

        public ITmfCheckpoint getCheckpoint() {
            return found;
        }
    }

    @Override
    public ITmfCheckpoint get(int checkpoint) {
        System.out.println("Searching for: " + checkpoint);
        RankVisitor v = new RankVisitor(checkpoint);
        long oldTime = System.currentTimeMillis();
        fDatabase.accept(v);
        ITmfCheckpoint checkpoint2 = v.getCheckpoint();
        System.out.println("Found: " + checkpoint2.getTimestamp() + "(" + (System.currentTimeMillis() - oldTime) + ")");
        return checkpoint2;
    }

    class Visitor implements IBTreeVisitor {
        int rank = -1;
        private ITmfCheckpoint search;
        boolean exactFound = false;

        public Visitor(ITmfCheckpoint search) {
            this.search = search;
        }

        @Override
        public boolean visit(ITmfCheckpoint key) {
            if (key == null) {
                return true;
            }
            return false;
        }

        @Override
        public int compare(ITmfCheckpoint checkRec) {
            int compareTo = checkRec.compareTo(search);
            if (compareTo <= 0 && !exactFound) {
                rank = checkRec.getRank();
                if (compareTo == 0) {
                    exactFound = true;
                }
            }
            return compareTo;
        }

        public int getRank() {
            return rank;
        }
    }

    @Override
    public int binarySearch(ITmfCheckpoint checkpoint) {
        System.out.println("Searching for: " + checkpoint.getTimestamp());
        Visitor v = new Visitor(checkpoint);
        long oldTime = System.currentTimeMillis();
        fDatabase.accept(v);
        int rank = v.getRank();
        System.out.println("Found: " + rank + "(" + (System.currentTimeMillis() - oldTime) + ")");
        return rank;
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
    public boolean restore() {
        return fDatabase.isExisted();
    }

}
