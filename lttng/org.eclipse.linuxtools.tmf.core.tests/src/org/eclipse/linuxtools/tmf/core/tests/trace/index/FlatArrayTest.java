package org.eclipse.linuxtools.tmf.core.tests.trace.index;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.linuxtools.tmf.core.trace.FlatArray;
import org.eclipse.linuxtools.tmf.core.trace.indexer.checkpoint.ITmfCheckpoint;
import org.eclipse.linuxtools.tmf.core.trace.indexer.checkpoint.TmfCheckpoint;
import org.eclipse.linuxtools.tmf.core.trace.location.TmfLongLocation;
import org.eclipse.linuxtools.tmf.tests.stubs.trace.TmfTraceStub;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FlatArrayTest {

    private TmfTraceStub fTrace;
    File file = new File("ranks.ht");

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

    @Test
    public void testInsertAlot() {
        FlatArray farr = new FlatArray(file, fTrace);
        long old = System.currentTimeMillis();
        final int TRIES = 5000000;
        for (int i = 0; i < TRIES; i++) {
            TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(12345 + i), new TmfLongLocation(123456L + i));
            checkpoint.setRank(i);
            farr.insert(checkpoint);
        }

        farr.dispose();
        System.out.println("Write time: " + (System.currentTimeMillis() - old));

        ArrayList<Integer> list = new ArrayList<Integer>();
        for (int i = 0; i < TRIES; i++) {
            Random rand = new Random();
            list.add(rand.nextInt(TRIES));
        }

        old = System.currentTimeMillis();
        farr = new FlatArray(file, fTrace);

        for (int i = 0; i < TRIES; i++) {
            Integer randomCheckpoint = list.get(i);
            TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(12345 + randomCheckpoint), new TmfLongLocation(123456L + randomCheckpoint));

            ITmfCheckpoint fileCheckpoint = farr.get(randomCheckpoint);
//            assertEquals(randomCheckpoint.intValue(), fileCheckpoint.getRank());
            assertEquals(checkpoint, fileCheckpoint);
            if (i % 1000 == 0) {
                System.out.println("Progress: " + (float)i / TRIES * 100);
            }
        }
        System.out.println("Rank read time: " + (System.currentTimeMillis() - old));


        for (int i = 0; i < TRIES; i++) {
            Integer randomCheckpoint = list.get(i);
            TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(12345 + randomCheckpoint), new TmfLongLocation(123456L + randomCheckpoint));

            ITmfCheckpoint found = farr.binarySearch2(checkpoint);
            assertEquals(randomCheckpoint.intValue(), found.getRank());
            assertEquals(checkpoint, found);
            if (i % 1000 == 0) {
                System.out.println("Progress: " + (float)i / TRIES * 100);
            }
        }
        System.out.println("Rank read time: " + (System.currentTimeMillis() - old));

        farr.dispose();
    }

}
