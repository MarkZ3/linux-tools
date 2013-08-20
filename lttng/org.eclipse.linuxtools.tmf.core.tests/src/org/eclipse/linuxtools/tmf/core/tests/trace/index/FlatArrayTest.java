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

package org.eclipse.linuxtools.tmf.core.tests.trace.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Random;

import org.eclipse.linuxtools.internal.tmf.core.trace.index.FlatArray;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.linuxtools.tmf.core.trace.indexer.checkpoint.ITmfCheckpoint;
import org.eclipse.linuxtools.tmf.core.trace.indexer.checkpoint.TmfCheckpoint;
import org.eclipse.linuxtools.tmf.core.trace.location.TmfLongLocation;
import org.eclipse.linuxtools.tmf.tests.stubs.trace.TmfTraceStub;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the FlatArray class
 */
public class FlatArrayTest {

    private static final int CHECKPOINTS_INSERT_NUM = 5000;
    private TmfTraceStub fTrace;
    private File fFile = new File("index.ht");
    FlatArray fFlatArray = null;

    /**
     * Setup the test. Make sure the index is deleted.
     */
    @Before
    public void setUp() {
        fTrace = new TmfTraceStub();
        if (fFile.exists()) {
            fFile.delete();
        }
    }

    /**
     * Tear down the test. Make sure the index is deleted.
     */
    @After
    public void tearDown() {
        fTrace.dispose();
        fTrace = null;
        if (fFile.exists()) {
            fFile.delete();
        }
    }

    /**
     * Test constructing a new BTree
     */
    @Test
    public void testConstructor() {
        fFlatArray = new FlatArray(fFile, fTrace);
        assertTrue(fFile.exists());
        assertTrue(fFlatArray.isCreatedFromScratch());
    }

    /**
     * Test constructing a new BTree, existing file
     */
    @Test
    public void testConstructorExistingFile() {
        fFlatArray = new FlatArray(fFile, fTrace);
        assertTrue(fFile.exists());
        fFlatArray.dispose();

        fFlatArray = new FlatArray(fFile, fTrace);
        assertFalse(fFlatArray.isCreatedFromScratch());
        fFlatArray.dispose();
    }

    /**
     * Test a new BTree is considered created from scratch and vice versa
     */
    @Test
    public void testIsCreatedFromScratch() {
        fFlatArray = new FlatArray(fFile, fTrace);
        assertTrue(fFlatArray.isCreatedFromScratch());
        fFlatArray.dispose();

        fFlatArray = new FlatArray(fFile, fTrace);
        assertFalse(fFlatArray.isCreatedFromScratch());
        fFlatArray.dispose();
    }

    /**
     * Test a single insertion
     */
    @Test
    public void testInsert() {
        FlatArray flatArray = new FlatArray(fFile, fTrace);
        TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(12345), new TmfLongLocation(123456L));
        flatArray.insert(checkpoint);

        ITmfCheckpoint fFileCheckpoint = flatArray.get(0);
        assertEquals(checkpoint, fFileCheckpoint);


        int found = flatArray.binarySearch(checkpoint);
        assertEquals(0, found);

        flatArray.dispose();
    }

    /**
     * Test many checkpoint insertions.
     * Make sure they can be found after re-opening the fFile
     */
    @Test
    public void testInsertAlot() {
        FlatArray flatArray = new FlatArray(fFile, fTrace);
        for (int i = 0; i < CHECKPOINTS_INSERT_NUM; i++) {
            TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(12345 + i), new TmfLongLocation(123456L + i));
            checkpoint.setRank(i);
            flatArray.insert(checkpoint);
        }

        flatArray.dispose();

        boolean random = false;
        ArrayList<Integer> list = new ArrayList<Integer>();
        for (int i = 0; i < CHECKPOINTS_INSERT_NUM; i++) {
            if (random) {
                Random rand = new Random();
                list.add(rand.nextInt(CHECKPOINTS_INSERT_NUM));
            } else {
                list.add(i);
            }
        }

        flatArray = new FlatArray(fFile, fTrace);

        for (int i = 0; i < CHECKPOINTS_INSERT_NUM; i++) {
            Integer randomCheckpoint = list.get(i);
            TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(12345 + randomCheckpoint), new TmfLongLocation(123456L + randomCheckpoint));

            ITmfCheckpoint fFileCheckpoint = flatArray.get(randomCheckpoint);
            assertEquals(checkpoint, fFileCheckpoint);
        }


        for (int i = 0; i < CHECKPOINTS_INSERT_NUM; i++) {
            Integer randomCheckpoint = list.get(i);
            TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(12345 + randomCheckpoint), new TmfLongLocation(123456L + randomCheckpoint));

            int found = flatArray.binarySearch(checkpoint);
            assertEquals(randomCheckpoint.intValue(), found);
        }

        flatArray.dispose();
    }

    /**
     * Tests that binarySearch find the correct checkpoint and ends with a perfect match
     */
    @Test
    public void testBinarySearch() {
        FlatArray flatArray = new FlatArray(fFile, fTrace);
        for (long i = 0; i < CHECKPOINTS_INSERT_NUM; i++) {
            TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(i), new TmfLongLocation(i));
            flatArray.insert(checkpoint);
        }

        TmfCheckpoint expectedCheckpoint = new TmfCheckpoint(new TmfTimestamp(122), new TmfLongLocation(122L));
        int expectedRank = 122;

        int rank = flatArray.binarySearch(expectedCheckpoint);
        ITmfCheckpoint found = flatArray.get(rank);

        assertEquals(expectedRank, rank);
        assertEquals(found, expectedCheckpoint);
        flatArray.dispose();
    }
    /**
     * Tests that binarySearch find the correct checkpoint when the time stamp is between checkpoints
     */
    @Test
    public void testBinarySearchFindInBetween() {
        FlatArray flatArray = new FlatArray(fFile, fTrace);
        for (long i = 0; i < CHECKPOINTS_INSERT_NUM; i++) {
            TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(2 * i), new TmfLongLocation(2 * i));
            flatArray.insert(checkpoint);
        }

        TmfCheckpoint searchedCheckpoint = new TmfCheckpoint(new TmfTimestamp(123), new TmfLongLocation(123L));
        TmfCheckpoint expectedCheckpoint = new TmfCheckpoint(new TmfTimestamp(122), new TmfLongLocation(122L));
        int expectedRank = 61;

        int rank = flatArray.binarySearch(searchedCheckpoint);
        ITmfCheckpoint found = flatArray.get(rank);

        assertEquals(expectedRank, rank);
        assertEquals(found, expectedCheckpoint);
        flatArray.dispose();
    }

    /**
     * Test setTimeRange, getTimeRange
     */
    @Test
    public void testSetGetTimeRange() {
        fFlatArray = new FlatArray(fFile, fTrace);
        TmfTimeRange timeRange = new TmfTimeRange(new TmfTimestamp(0), new TmfTimestamp(100));
        fFlatArray.setTimeRange(timeRange);
        assertEquals(timeRange, fFlatArray.getTimeRange());
        fFlatArray.dispose();
    }

    /**
     * Test setNbEvents, getNbEvents
     */
    @Test
    public void testSetGetNbEvents() {
        fFlatArray = new FlatArray(fFile, fTrace);
        int expected = 12345;
        fFlatArray.setNbEvents(expected);
        assertEquals(expected, fFlatArray.getNbEvents());
        fFlatArray.dispose();
    }

    /**
     * Test setSize, size
     */
    @Test
    public void testSetGetSize() {
        fFlatArray = new FlatArray(fFile, fTrace);
        assertEquals(0, fFlatArray.size());
        int expected = 1234;
        fFlatArray.setSize(expected);
        assertEquals(expected, fFlatArray.size());
        fFlatArray.dispose();
    }

    /**
     * Test delete
     */
    @Test
    public void testDelete() {
        fFlatArray = new FlatArray(fFile, fTrace);
        assertTrue(fFile.exists());
        fFlatArray.delete();
        assertFalse(fFile.exists());
    }

    /**
     * Test version change
     * @throws IOException can throw this
     */
    @Test
    public void testVersionChange() throws IOException {
        fFlatArray = new FlatArray(fFile, fTrace);
        fFlatArray.dispose();
        RandomAccessFile f = new RandomAccessFile(fFile, "rw");
        f.writeInt(-1);
        f.close();

        fFlatArray = new FlatArray(fFile, fTrace);
        assertTrue(fFlatArray.isCreatedFromScratch());
        fFlatArray.dispose();
    }

}
