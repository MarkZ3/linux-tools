package org.eclipse.linuxtools.tmf.core.tests.trace.index;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Random;

import org.eclipse.linuxtools.internal.tmf.core.trace.index.BTree;
import org.eclipse.linuxtools.internal.tmf.core.trace.index.IBTreeVisitor;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.linuxtools.tmf.core.trace.FlatArray;
import org.eclipse.linuxtools.tmf.core.trace.indexer.checkpoint.ITmfCheckpoint;
import org.eclipse.linuxtools.tmf.core.trace.indexer.checkpoint.TmfCheckpoint;
import org.eclipse.linuxtools.tmf.core.trace.location.TmfLongLocation;
import org.eclipse.linuxtools.tmf.tests.stubs.trace.TmfTraceStub;
import org.junit.After;
import org.junit.Before;

public class AllBench {

    private static final boolean reportProgress = true;
    private static ArrayList<ArrayList<Integer>> nums;
    private TmfTraceStub fTrace;
    File file = new File("index.ht");

    static int BTREE_DEGREE = 15;

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

    public void generateDataFile(ArrayList<Integer> list, int checkpointsNums) throws IOException {
        File randomDataFile = new File("data" + checkpointsNums + ".ht");
        RandomAccessFile f = new RandomAccessFile(randomDataFile, "rw");
        if (randomDataFile.exists()) {
            for (int i = 0; i < checkpointsNums; i++) {
                Random rand = new Random();
                int nextInt = rand.nextInt(checkpointsNums);
                list.add(nextInt);
                f.writeInt(nextInt);
            }
        } else {
            for (int i = 0; i < checkpointsNums; i++) {
                list.add(f.readInt());
            }
        }
        f.close();
    }

    public static void main(String[] args) throws IOException {
        int checkpointsNums [] = new int [] { 5000, 50000, 500000, 5000000 };
        nums = new ArrayList<ArrayList<Integer>>(checkpointsNums.length);

        System.out.println("DEGREE: " + BTREE_DEGREE);
        System.out.println("ALWAYS_CACHE_ROOT: " + BTree.ALWAYS_CACHE_ROOT);
        System.out.println("Cache size: " + BTree.CACHE_SIZE + "\n");

        AllBench b = new AllBench();
        b.setUp();
        for (int i = 0; i < checkpointsNums.length; i++) {
            ArrayList<Integer> list = new ArrayList<Integer>();
            b.generateDataFile(list, checkpointsNums[i]);
            nums.add(list);

            System.out.println("*** " + checkpointsNums[i] + " checkpoints ***\n");

            b.benchIt(list);
        }
        b.tearDown();
    }

    public void benchIt(ArrayList<Integer> list) {

        System.out.println("Testing BTree\n");

        testInsertAlot(list);

        System.out.println("Testing Array\n");

        testInsertAlotArray(list);
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

    public void testInsertAlot(ArrayList<Integer> list2) {
        int checkpointsNum = list2.size();

        writeCheckpoints(checkpointsNum);

        ArrayList<Integer> list = new ArrayList<Integer>();
        for (int i = 0; i < checkpointsNum; i++) {
            list.add(i);
        }

        readCheckpoints(checkpointsNum, list, false);
        readCheckpoints(checkpointsNum, list2, true);

        System.out.println();
    }

    public void testInsertAlotArray(ArrayList<Integer> list2) {
        int checkpointsNum = list2.size();

        writeCheckpointsArray(checkpointsNum);

        ArrayList<Integer> list = new ArrayList<Integer>();
        for (int i = 0; i < checkpointsNum; i++) {
            list.add(i);
        }

        readCheckpointsArray(checkpointsNum, list, false);
        readCheckpointsArray(checkpointsNum, list2, true);

        System.out.println();
    }

    private void writeCheckpoints(int checkpointsNum) {
        BTree bTree;
        {
            int REPEAT = 10;
            long time = 0;
            for (int j = 0; j < REPEAT; j++) {
                long old = System.currentTimeMillis();
                bTree = new BTree(BTREE_DEGREE, file, fTrace);
                for (int i = 0; i < checkpointsNum; i++) {
                    TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(12345 + i), new TmfLongLocation(123456L + i));
                    checkpoint.setRank(i);
                    bTree.insert(checkpoint);
                }

                time += (System.currentTimeMillis() - old);
                bTree.dispose();
                if (j != REPEAT - 1) {
                    file.delete();
                }
                if (reportProgress) {
                    System.out.print(".");
                }
            }

            System.out.println("Write time average: " + (float) time / REPEAT);
        }
    }

    private void writeCheckpointsArray(int checkpointsNum) {
        FlatArray array;
        {
            int REPEAT = 10;
            long time = 0;
            for (int j = 0; j < REPEAT; j++) {
                long old = System.currentTimeMillis();
                array = new FlatArray(file, fTrace);
                for (int i = 0; i < checkpointsNum; i++) {
                    TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(12345 + i), new TmfLongLocation(123456L + i));
                    checkpoint.setRank(i);
                    array.insert(checkpoint);
                }

                time += (System.currentTimeMillis() - old);
                array.dispose();
                if (j != REPEAT - 1) {
                    file.delete();
                }
                if (reportProgress) {
                    System.out.print(".");
                }
            }

            System.out.println("Write time average: " + (float) time / REPEAT);
        }
    }


    private void readCheckpoints(int checkpointsNum, ArrayList<Integer> list, boolean random) {
        BTree bTree;
        int REPEAT = 10;
        long time = 0;
        long cacheMisses = 0;
        for (int j = 0; j < REPEAT; j++) {
            long old = System.currentTimeMillis();
            bTree = new BTree(BTREE_DEGREE, file, fTrace);
            for (int i = 0; i < checkpointsNum; i++) {
                Integer randomCheckpoint = list.get(i);
                TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(12345 + randomCheckpoint), new TmfLongLocation(123456L + randomCheckpoint));
                Visitor treeVisitor = new Visitor(checkpoint);
                bTree.accept(treeVisitor);
                assertEquals(randomCheckpoint.intValue(), treeVisitor.getRank());
            }
            time += (System.currentTimeMillis() - old);
            cacheMisses = bTree.getCacheMisses();
            bTree.dispose();
            if (reportProgress) {
                System.out.print(".");
            }
        }

        System.out.println("Read " + (random ? "(random)" : "(linear)") + "time average: " + (float) time / REPEAT + "            (cache miss: " + cacheMisses + ")");
    }

    private void readCheckpointsArray(int checkpointsNum, ArrayList<Integer> list, boolean random) {
        FlatArray array;
        int REPEAT = 10;
        long time = 0;
        long cacheMisses = 0;
        for (int j = 0; j < REPEAT; j++) {
            long old = System.currentTimeMillis();
            array = new FlatArray(file, fTrace);
            for (int i = 0; i < checkpointsNum; i++) {
                Integer randomCheckpoint = list.get(i);
                TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(12345 + randomCheckpoint), new TmfLongLocation(123456L + randomCheckpoint));
                int found = array.binarySearch(checkpoint);
                assertEquals(randomCheckpoint.intValue(), found);
            }
            time += (System.currentTimeMillis() - old);
            cacheMisses = array.getCacheMisses();
            array.dispose();
            if (reportProgress) {
                System.out.print(".");
            }
        }

        System.out.println("Read " + (random ? "(random)" : "(linear)") + "time average: " + (float) time / REPEAT + "            (cache miss: " + cacheMisses + ")");
    }

}
