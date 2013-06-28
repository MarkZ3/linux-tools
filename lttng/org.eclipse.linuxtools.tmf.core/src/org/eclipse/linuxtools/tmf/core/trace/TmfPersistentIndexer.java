package org.eclipse.linuxtools.tmf.core.trace;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.eclipse.linuxtools.internal.tmf.core.IndexHelper;
import org.eclipse.linuxtools.tmf.core.signal.TmfTraceUpdatedSignal;
import org.eclipse.linuxtools.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimeRange;


/**
 * @since 3.0
 */
@SuppressWarnings("javadoc")
public class TmfPersistentIndexer extends TmfCheckpointIndexer {

    private final String INDEX_FILE_NAME = "index.ht"; //$NON-NLS-1$
    private final long INDEX_VERSION = 1;

    /**
     * @param trace
     */
    public TmfPersistentIndexer(ITmfTrace trace) {
        super(trace);
    }

    @Override
    public void dispose() {
        super.dispose();

        persistIndex();
    }

    @Override
    protected ITmfIndex createIndex(ITmfTrace trace) {
        return new TmfPersistentIndex(trace);
    }

    private void persistIndex() {
//        if (!fTraceIndex.isEmpty()) {
//            File htFile = getIndexFile();
//            try {
//                BufferedOutputStream buff = new BufferedOutputStream(new FileOutputStream(htFile));
//                IndexHelper.writeLong(buff, INDEX_VERSION);
//                IndexHelper.writeInt(buff, fTraceIndex.size());
//                for (int index = 0; index < fTraceIndex.size(); ++index) {
//                    ITmfCheckpoint checkpoint= fTraceIndex.get(index);
//                    checkpoint.serialize(buff);
//                }
//                buff.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
    }

    private File getIndexFile() {
        String directory = TmfTraceManager.getSupplementaryFileDir(fTrace);
        return new File(directory + INDEX_FILE_NAME);
    }

    @Override
    public void buildIndex(long offset, TmfTimeRange range, boolean waitForCompletion) {
        File indexFile = getIndexFile();
        if (indexFile.exists()) {
            if (restoreIndex(indexFile)) {
                return;
            }
        }

        super.buildIndex(offset, range, waitForCompletion);
        if (waitForCompletion) {
            persistIndex();
        }
    }

    private boolean restoreIndex(File indexFile) {
        return fTraceIndex.size() > 1;
//        BufferedInputStream buff;
//        try {
//            buff = new BufferedInputStream(new FileInputStream(indexFile));
//            long version = IndexHelper.readLong(buff);
//            if (version != INDEX_VERSION) {
//                indexFile.delete();
//                return false;
//            }
//
//            int size = IndexHelper.readInt(buff);
//            for (int i = 0; i < size; ++i) {
//                ITmfCheckpoint checkPoint = fTrace.restoreCheckPoint(buff);
//                fTraceIndex.add(checkPoint);
//            }
//
//            ITmfTimestamp startTimestamp = fTraceIndex.get(0).getTimestamp();
//            ITmfTimestamp endTimestamp = fTraceIndex.get(fTraceIndex.size() - 1).getTimestamp();
//            fTrace.broadcast(new TmfTraceUpdatedSignal(fTrace, fTrace, new TmfTimeRange(startTimestamp, endTimestamp)));
//
//            return true;
//
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return false;
    }

    /**
     * @param trace
     * @param interval
     */
    public TmfPersistentIndexer(ITmfTrace trace, int interval) {
        super(trace, interval);
    }
}
