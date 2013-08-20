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

import org.eclipse.linuxtools.internal.tmf.core.trace.index.BTree;
import org.eclipse.linuxtools.internal.tmf.core.trace.index.BTreeCheckpointVisitor;
import org.eclipse.linuxtools.internal.tmf.core.trace.index.IBTreeVisitor;
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
 * Tests for the BTree class
 */
public class BTreeTest {

    private static final int CHECKPOINTS_INSERT_NUM = 50000;
    private TmfTraceStub fTrace;
    private File fFile = new File("index.ht");
    BTree fBTree = null;
    private final int DEGREE = 15;

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
        if (fBTree != null) {
            fBTree.dispose();
        }
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
        fBTree = new BTree(DEGREE, fFile, fTrace);
        assertTrue(fFile.exists());
        assertTrue(fBTree.isCreatedFromScratch());
    }

    /**
     * Test constructing a new BTree, existing file
     */
    @Test
    public void testConstructorExistingFile() {
        fBTree = new BTree(DEGREE, fFile, fTrace);
        assertTrue(fFile.exists());
        fBTree.dispose();

        fBTree = new BTree(5, fFile, fTrace);
        assertFalse(fBTree.isCreatedFromScratch());
        fBTree.dispose();
    }

    /**
     * Test a new BTree is considered created from scratch and vice versa
     */
    @Test
    public void testIsCreatedFromScratch() {
        fBTree = new BTree(DEGREE, fFile, fTrace);
        assertTrue(fBTree.isCreatedFromScratch());
        fBTree.dispose();

        fBTree = new BTree(5, fFile, fTrace);
        assertFalse(fBTree.isCreatedFromScratch());
        fBTree.dispose();
    }

    /**
     * Test a single insertion
     */
    @Test
    public void testInsert() {
        fBTree = new BTree(DEGREE, fFile, fTrace);
        TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(12345), new TmfLongLocation(123456L));
        fBTree.insert(checkpoint);

        BTreeCheckpointVisitor treeVisitor = new BTreeCheckpointVisitor(checkpoint);
        fBTree.accept(treeVisitor);
        assertEquals(0, treeVisitor.getRank());
        fBTree.dispose();
    }

    /**
     * Test many checkpoint insertions.
     * Make sure they can be found after re-opening the file
     */
    @Test
    public void testInsertAlot() {
        fBTree = new BTree(DEGREE, fFile, fTrace);
        for (int i = 0; i < CHECKPOINTS_INSERT_NUM; i++) {
            TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(12345 + i), new TmfLongLocation(123456L + i));
            checkpoint.setRank(i);
            fBTree.insert(checkpoint);
        }

        fBTree.dispose();

        boolean random = true;
        ArrayList<Integer> list = new ArrayList<Integer>();
        for (int i = 0; i < CHECKPOINTS_INSERT_NUM; i++) {
            if (random) {
                Random rand = new Random();
                list.add(rand.nextInt(CHECKPOINTS_INSERT_NUM));
            } else {
                list.add(i);
            }
        }

        fBTree = new BTree(DEGREE, fFile, fTrace);

        for (int i = 0; i < CHECKPOINTS_INSERT_NUM; i++) {
            Integer randomCheckpoint = list.get(i);
            TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(12345 + randomCheckpoint), new TmfLongLocation(123456L + randomCheckpoint));
            BTreeCheckpointVisitor treeVisitor = new BTreeCheckpointVisitor(checkpoint);
            fBTree.accept(treeVisitor);
            assertEquals(randomCheckpoint.intValue(), treeVisitor.getRank());
            assertEquals(checkpoint, treeVisitor.getCheckpoint());
        }

        fBTree.dispose();
    }

    /**
     * Test setSize, size
     */
    @Test
    public void testSetGetSize() {
        fBTree = new BTree(DEGREE, fFile, fTrace);
        assertEquals(0, fBTree.size());
        int expected = 1234;
        fBTree.setSize(expected);
        assertEquals(expected, fBTree.size());
        fBTree.dispose();
    }

    /**
     * Tests that accepts find the correct checkpoint and ends with a perfect match
     */
    @Test
    public void testAccept() {
        fBTree = new BTree(DEGREE, fFile, fTrace);
        for (int i = 0; i < CHECKPOINTS_INSERT_NUM; i++) {
            TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(i), new TmfLongLocation((long)i));
            fBTree.insert(checkpoint);
        }

        final TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(123), new TmfLongLocation(123L));

        class TestVisitor implements IBTreeVisitor {
            public int fLastCompare = 0;
            ITmfCheckpoint fFoundCheckpoint;

            @Override
            public int compare(ITmfCheckpoint checkRec) {
                fLastCompare = checkRec.compareTo(checkpoint);
                if (fLastCompare == 0) {
                    fFoundCheckpoint = checkRec;
                }
                return fLastCompare;
            }
        }
        final TestVisitor t = new TestVisitor();

        fBTree.accept(t);

        assertEquals(checkpoint, t.fFoundCheckpoint);
        assertEquals(0, t.fLastCompare);
        fBTree.dispose();
    }

    /**
     * Tests that accepts find the correct checkpoint when the time stamp is between checkpoints
     */
    @Test
    public void testAcceptFindInBetween() {
        fBTree = new BTree(DEGREE, fFile, fTrace);
        for (int i = 0; i < CHECKPOINTS_INSERT_NUM; i++) {
            TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(2 * i), new TmfLongLocation((long)2 * i));
            fBTree.insert(checkpoint);
        }

        final TmfCheckpoint searchedCheckpoint = new TmfCheckpoint(new TmfTimestamp(123), new TmfLongLocation(123L));
        final TmfCheckpoint expectedCheckpoint = new TmfCheckpoint(new TmfTimestamp(122), new TmfLongLocation(122L));

        BTreeCheckpointVisitor v = new BTreeCheckpointVisitor(searchedCheckpoint);
        fBTree.accept(v);

        assertEquals(expectedCheckpoint, v.getCheckpoint());
        fBTree.dispose();
    }

    /**
     * Test setTimeRange, getTimeRange
     */
    @Test
    public void testSetGetTimeRange() {
        fBTree = new BTree(DEGREE, fFile, fTrace);
        TmfTimeRange timeRange = new TmfTimeRange(new TmfTimestamp(0), new TmfTimestamp(100));
        fBTree.setTimeRange(timeRange);
        assertEquals(timeRange, fBTree.getTimeRange());
        fBTree.dispose();
    }

    /**
     * Test setNbEvents, getNbEvents
     */
    @Test
    public void testSetGetNbEvents() {
        fBTree = new BTree(DEGREE, fFile, fTrace);
        int expected = 12345;
        fBTree.setNbEvents(expected);
        assertEquals(expected, fBTree.getNbEvents());
        fBTree.dispose();
    }

    /**
     * Test delete
     */
    @Test
    public void testDelete() {
        fBTree = new BTree(DEGREE, fFile, fTrace);
        assertTrue(fFile.exists());
        fBTree.delete();
        assertFalse(fFile.exists());
    }

    /**
     * Test version change
     * @throws IOException can throw this
     */
    @Test
    public void testVersionChange() throws IOException {
        fBTree = new BTree(DEGREE, fFile, fTrace);
        fBTree.dispose();
        RandomAccessFile f = new RandomAccessFile(fFile, "rw");
        f.writeInt(-1);
        f.close();

        fBTree = new BTree(DEGREE, fFile, fTrace);
        assertTrue(fBTree.isCreatedFromScratch());
        fBTree.dispose();
    }
}
