package org.eclipse.linuxtools.tmf.core.tests.trace.index;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.linuxtools.internal.tmf.core.trace.index.BTree;
import org.eclipse.linuxtools.internal.tmf.core.trace.index.IBTreeVisitor;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.linuxtools.tmf.core.trace.ITmfCheckpoint;
import org.eclipse.linuxtools.tmf.core.trace.TmfCheckpoint;
import org.eclipse.linuxtools.tmf.core.trace.TmfLongLocation;
import org.eclipse.linuxtools.tmf.tests.stubs.trace.TmfTraceStub;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BTreeTest {

    private TmfTraceStub fTrace;
    File file = new File("index.ht");

    @Before
    public void setUp() {
        fTrace = new TmfTraceStub();
        if (file.exists()) {
            file.delete();
        }
    }

    @After
    public void tearDown() {
        fTrace.dispose();
        fTrace = null;
        if (file.exists()) {
            file.delete();
        }
    }

    class Visitor implements IBTreeVisitor {
        int rank = -1;
        private ITmfCheckpoint search;
        private ITmfCheckpoint found;
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
                found = checkRec;
                if (compareTo == 0) {
                    exactFound = true;
                }
            }
            return compareTo;
        }

        public int getRank() {
            return rank;
        }

        public ITmfCheckpoint getFound() {
            return found;
        }
    }

    @Test
    public void testInsert() {
        BTree bTree = new BTree(8, file, fTrace);
        TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(12345), new TmfLongLocation(123456L));
        bTree.insert(checkpoint);

        Visitor treeVisitor = new Visitor(checkpoint);
        bTree.accept(treeVisitor);
        assertEquals(0, treeVisitor.getRank());
        bTree.dispose();
    }

    @Test
    public void testInsertAlot() {
        BTree bTree = new BTree(8, file, fTrace);
        ArrayList<ITmfCheckpoint> list = new ArrayList<ITmfCheckpoint>();
        final int TRIES = 5000;
        for (int i = 0; i < TRIES; i++) {
            TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(12345 + i), new TmfLongLocation(123456L + i));
            checkpoint.setRank(i);
            bTree.insert(checkpoint);
            list.add(checkpoint);
        }

        bTree.dispose();
        bTree = new BTree(8, file, fTrace);

        for (int i = 0; i < TRIES; i++) {
            Visitor treeVisitor = new Visitor(list.get(i));
            bTree.accept(treeVisitor);
            assertEquals(i, treeVisitor.getRank());
            assertEquals(list.get(i), treeVisitor.getFound());
        }

        bTree.dispose();
    }

}
