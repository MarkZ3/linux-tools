package org.eclipse.linuxtools.tmf.core.trace;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.linuxtools.tmf.core.trace.index.ChunkCache;
import org.eclipse.linuxtools.tmf.core.trace.index.Database;

/**
 * @author emalape
 * @since 3.0
 *
 */
public class TmfPersistentIndex implements ITmfIndex {

    Database fDatabase;
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

    /**
     * @param trace
     *
     */
    public TmfPersistentIndex(ITmfTrace trace) {
        fTrace = trace;
        createDatabase();
    }

    private void createDatabase() {
        try {
            fDatabase = new Database(getIndexFile(), new ChunkCache(), version, false);
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

    @Override
    public int binarySearch(ITmfCheckpoint checkpoint) {
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
