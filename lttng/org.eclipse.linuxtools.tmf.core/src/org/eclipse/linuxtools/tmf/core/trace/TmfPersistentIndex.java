package org.eclipse.linuxtools.tmf.core.trace;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.linuxtools.tmf.core.trace.index.BTree;
import org.eclipse.linuxtools.tmf.core.trace.index.ChunkCache;
import org.eclipse.linuxtools.tmf.core.trace.index.Database;
import org.eclipse.linuxtools.tmf.core.trace.index.IBTreeComparator;
import org.eclipse.linuxtools.tmf.core.trace.index.IBTreeVisitor;

/**
 * @author emalape
 * @since 3.0
 *
 */
public class TmfPersistentIndex implements ITmfIndex {

    Database fDatabase;
    BTree fCheckpointTree;
    ITmfTrace fTrace;
    int size = 0;

    private final String INDEX_FILE_NAME = "index.ht"; //$NON-NLS-1$
    int version = 2;

    /**
     *
     */
    public static final int VERSION_NUMBER_OFFSET = Database.DATA_AREA;
    /**
     *
     */
    public static final int FIRST_CHECKPOINT_OFFSET = VERSION_NUMBER_OFFSET + 4;

    public static final int CHECKPOINT_TREE_OFFSET = FIRST_CHECKPOINT_OFFSET + 4;

    /**
     * @param trace
     *
     */
    public TmfPersistentIndex(ITmfTrace trace) {
        fTrace = trace;
        createDatabase();
    }

    /**
     * @author emalape
     *
     */
    public static class CheckpointBTreeComparator implements IBTreeComparator {
        final private Database db;
        final private ITmfTrace fTrace;

        /**
         * @param database
         * @param trace
         */
        public CheckpointBTreeComparator(Database database, ITmfTrace trace) {
            db= database;
            fTrace = trace;
        }
        @Override
        public int compare(long record1, long record2) throws CoreException {
            ITmfCheckpoint aCheckpoint1 = fTrace.restoreCheckPoint(db, record1);
            ITmfCheckpoint aCheckpoint2 = fTrace.restoreCheckPoint(db, record2);
            return aCheckpoint1.compareTo(aCheckpoint2);
        }
    }

    private void createDatabase() {
        try {
            File indexFile = getIndexFile();
//            if (!indexFile.exists()) {
//                indexFile.createNewFile();
//            }
            fDatabase = new Database(indexFile, new ChunkCache(), version, false);
            if (fDatabase.getVersion() != version) {
                fDatabase.clear(version);
            } else {
                try {
                    int i = 0;
                    long rec = fDatabase.getRecPtr(FIRST_CHECKPOINT_OFFSET);
                    while (rec != 0) {
                        rec= fDatabase.getRecPtr(rec);
                        ++i;
                    }
                    size = i;
                    if (fCheckpointTree == null) {
                        fCheckpointTree= new BTree(fDatabase, CHECKPOINT_TREE_OFFSET, new CheckpointBTreeComparator(fDatabase, fTrace));
                    }
                } catch (CoreException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        } catch (CoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private File getIndexFile() {
        String directory = TmfTraceManager.getSupplementaryFileDir(fTrace);
        return new File(directory + INDEX_FILE_NAME);
    }

    @Override
    public void dispose() {
        try {
            fDatabase.close();
        } catch (CoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void addToList(final long listRecord, long record) throws CoreException {
        final Database db= fDatabase;
        final long nextRec= db.getRecPtr(listRecord);
        db.putRecPtr(record + 0, nextRec);
        db.putRecPtr(listRecord, record);
    }

    @Override
    public void add(ITmfCheckpoint checkpoint) {
        try {
            long record = checkpoint.serialize(fDatabase);
            fCheckpointTree.insert(record);
            addToList(FIRST_CHECKPOINT_OFFSET, record);
            size++;
        } catch (CoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        //fCheckpoints.add(checkpoint);
    }

    @Override
    public ITmfCheckpoint get(int checkpoint) {
        try {
            long rec= fDatabase.getRecPtr(FIRST_CHECKPOINT_OFFSET);
            int i = 0;
            while (rec != 0 && i != checkpoint) {
                rec= fDatabase.getRecPtr(rec);
                ++i;
            }

            return fTrace.restoreCheckPoint(fDatabase, rec);

        } catch (CoreException e) {
//            CCorePlugin.log(e);
        }

        return null;

       //fDatabase.get
        //return fCheckpoints.get(checkpoint);
    }

    class CheckPointTreeVisitor implements IBTreeVisitor {

        ITmfCheckpoint fCheckpoint;
        final private Database fDb;
        final private ITmfTrace fTrace;
        private boolean fFound = false;

        public CheckPointTreeVisitor(ITmfCheckpoint checkpoint, Database db, ITmfTrace trace) {
            fCheckpoint = checkpoint;
            this.fDb = db;
            fTrace = trace;
        }

        @Override
        public int compare(long record) throws CoreException {
            ITmfCheckpoint restoreCheckPoint = fTrace.restoreCheckPoint(fDb, record);
            return restoreCheckPoint.compareTo(fCheckpoint);
        }

        @Override
        public boolean visit(long record) throws CoreException {
            if (record == 0) {
                return true;
            }

            fFound = true;
            fCheckpoint = fTrace.restoreCheckPoint(fDb, record);
            return false;
        }

        boolean isFound() {
            return fFound;
        }

        ITmfCheckpoint getCheckpoint() {
            return fCheckpoint;
        }

    }

    @Override
    public int binarySearch(ITmfCheckpoint checkpoint) {
        System.out.println("searching for: " + checkpoint);
        CheckPointTreeVisitor visitor = new CheckPointTreeVisitor(checkpoint, fDatabase, fTrace);
        try {
            fCheckpointTree.accept(visitor);
        } catch (CoreException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        if (visitor.isFound()) {
            System.out.println("found!!");
        }

        try {
            long rec= fDatabase.getRecPtr(FIRST_CHECKPOINT_OFFSET);
            int i = 0;
            while (rec != 0) {
                ITmfCheckpoint aCheckpoint = fTrace.restoreCheckPoint(fDatabase, rec);
                if (aCheckpoint.compareTo(checkpoint) == 0) {
                    return i;
                }
                rec= fDatabase.getRecPtr(rec);
                ++i;
            }
        } catch (CoreException e) {
            e.printStackTrace();
        }

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
