package org.eclipse.linuxtools.tmf.core.trace;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimeRange;


/**
 * @since 3.0
 */
public class TmfPersistentIndexer extends TmfCheckpointIndexer {

    private final String INDEX_FILE_NAME = "index.ht"; //$NON-NLS-1$

    /**
     * @param trace
     */
    public TmfPersistentIndexer(ITmfTrace trace) {
        super(trace);
    }

    @Override
    public void dispose() {
        super.dispose();

        if (!fTraceIndex.isEmpty()) {
            String directory = TmfTraceManager.getSupplementaryFileDir(fTrace);
            final File htFile = new File(directory + INDEX_FILE_NAME);
            try {
                BufferedOutputStream buff = new BufferedOutputStream(new FileOutputStream(htFile));
                for (ITmfCheckpoint checkpoint : fTraceIndex) {
                    checkpoint.serialize(buff);
                }
                buff.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public void buildIndex(long offset, TmfTimeRange range, boolean waitForCompletion) {
        // TODO Auto-generated method stub
        super.buildIndex(offset, range, waitForCompletion);

    }

    /**
     * @param trace
     * @param interval
     */
    public TmfPersistentIndexer(ITmfTrace trace, int interval) {
        super(trace, interval);
    }



}
