/*******************************************************************************
 * Copyright (c) 2013 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Marc-Andre Laperle - Initial API and implementation
 *******************************************************************************/
package org.eclipse.linuxtools.tmf.core.trace;

import java.io.File;

import org.eclipse.linuxtools.internal.tmf.core.trace.index.BTree;
import org.eclipse.linuxtools.internal.tmf.core.trace.index.IBTreeVisitor;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.linuxtools.tmf.core.trace.indexer.checkpoint.ITmfCheckpoint;

/**
 * @since 3.0
 *
 */
@SuppressWarnings("javadoc")
public class TmfPersistentIndex implements ITmfIndex {

    static boolean sDEBUG_LOCKS= true; // initialized in the PDOMManager, because IBM needs PDOM independent of runtime plugin.

    private BTree fDatabase;
    private FlatArray fRanks;

    private final String INDEX_FILE_NAME = "index.ht"; //$NON-NLS-1$
    private final String RANKS_FILE_NAME = "ranks.ht"; //$NON-NLS-1$

    /**
     * @param trace
     *
     */
    public TmfPersistentIndex(ITmfTrace trace) {
        fDatabase = new BTree(8, getIndexFile(trace, INDEX_FILE_NAME), trace);
        fRanks = new FlatArray(getIndexFile(trace, RANKS_FILE_NAME), trace);
    }

    private static File getIndexFile(ITmfTrace trace, String fileName) {
        String directory = TmfTraceManager.getSupplementaryFileDir(trace);
        return new File(directory + fileName);
    }

    @Override
    public void dispose() {
        fDatabase.dispose();
    }

    @Override
    public void add(ITmfCheckpoint checkpoint) {
        checkpoint.setRank(fDatabase.size());
        fDatabase.insert(checkpoint);
        fRanks.insert(checkpoint);
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
        long oldTime = System.currentTimeMillis();
        ITmfCheckpoint checkpoint2 = fRanks.get(checkpoint);
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

    @Override
    public void setTimeRange(TmfTimeRange timeRange) {
        fDatabase.setTimeRange(timeRange);
    }

    @Override
    public void setNbEvents(long nbEvents) {
        fDatabase.setNbEvents(nbEvents);
    }

    @Override
    public TmfTimeRange getTimeRange() {
        return fDatabase.getTimeRange();
    }

    @Override
    public long getNbEvents() {
        return fDatabase.getNbEvents();
    }

}
