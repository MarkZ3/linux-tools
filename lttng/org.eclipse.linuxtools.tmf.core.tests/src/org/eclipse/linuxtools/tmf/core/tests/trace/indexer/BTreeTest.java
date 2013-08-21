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

package org.eclipse.linuxtools.tmf.core.tests.trace.indexer;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Random;

import org.eclipse.linuxtools.internal.tmf.core.trace.indexer.BTree;
import org.eclipse.linuxtools.internal.tmf.core.trace.indexer.BTreeCheckpointVisitor;
import org.eclipse.linuxtools.internal.tmf.core.trace.indexer.IBTreeVisitor;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.linuxtools.tmf.core.trace.indexer.ITmfPersistentlyIndexable;
import org.eclipse.linuxtools.tmf.core.trace.indexer.checkpoint.ITmfCheckpoint;
import org.eclipse.linuxtools.tmf.core.trace.indexer.checkpoint.TmfCheckpoint;
import org.eclipse.linuxtools.tmf.core.trace.location.TmfLongLocation;
import org.junit.Test;

/**
 * Tests for the BTree class
 *
 * @author Marc-Andre Laperle
 */
public class BTreeTest extends AbstractCheckpointCollectionTest {

    private final int DEGREE = 15;
    private BTree fBTree;

    @Override
    protected BTree createCollection() {
        BTree bTree = new BTree(DEGREE, getFile(), (ITmfPersistentlyIndexable)getTrace());
        fCheckpointCollection = bTree;
        return bTree;
    }

    /**
     * Test a single insertion
     */
    @Test
    public void testInsert() {
        fBTree = createCollection();
        TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(12345), new TmfLongLocation(123456L), 0);
        fBTree.insert(checkpoint);

        BTreeCheckpointVisitor treeVisitor = new BTreeCheckpointVisitor(checkpoint);
        fBTree.accept(treeVisitor);
        assertEquals(0, treeVisitor.getCheckpoint().getCheckpointRank());
        assertEquals(checkpoint, treeVisitor.getCheckpoint());
    }

    @Override
    public boolean isPersistableCollection() {
        return true;
    }

    /**
     * Test many checkpoint insertions. Make sure they can be found after
     * re-opening the file
     */
    @Test
    public void testInsertAlot() {
        fBTree = createCollection();
        for (int i = 0; i < CHECKPOINTS_INSERT_NUM; i++) {
            TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(12345 + i), new TmfLongLocation(123456L + i), i);
            fBTree.insert(checkpoint);
        }

        fBTree.setIndexComplete();
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

        fBTree = createCollection();

        for (int i = 0; i < CHECKPOINTS_INSERT_NUM; i++) {
            Integer randomCheckpoint = list.get(i);
            TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(12345 + randomCheckpoint), new TmfLongLocation(123456L + randomCheckpoint), 0);
            BTreeCheckpointVisitor treeVisitor = new BTreeCheckpointVisitor(checkpoint);
            fBTree.accept(treeVisitor);
            assertEquals(randomCheckpoint.intValue(), treeVisitor.getCheckpoint().getCheckpointRank());
            assertEquals(checkpoint, treeVisitor.getCheckpoint());
        }
    }

    /**
     * Test many checkpoint insertions using the same timestamp. Make sure they
     * can be found after re-opening the file
     */
    @Test
    public void testInsertSameTimestamp() {
        fBTree = createCollection();
        for (int i = 0; i < CHECKPOINTS_INSERT_NUM; i++) {
            TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(12345), new TmfLongLocation(123456L + i), i);
            fBTree.insert(checkpoint);
        }

        fBTree.setIndexComplete();
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

        fBTree = createCollection();

        for (int i = 0; i < CHECKPOINTS_INSERT_NUM; i++) {
            Integer randomCheckpoint = list.get(i);
            TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(12345), new TmfLongLocation(123456L + randomCheckpoint), 0);
            BTreeCheckpointVisitor treeVisitor = new BTreeCheckpointVisitor(checkpoint);
            fBTree.accept(treeVisitor);
            assertEquals(randomCheckpoint.intValue(), treeVisitor.getCheckpoint().getCheckpointRank());
            assertEquals(checkpoint, treeVisitor.getCheckpoint());
        }
    }

    /**
     * Tests that accepts find the correct checkpoint and ends with a perfect
     * match
     */
    @Test
    public void testAccept() {
        fBTree = createCollection();
        for (int i = 0; i < CHECKPOINTS_INSERT_NUM; i++) {
            TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(i), new TmfLongLocation((long) i), 0);
            fBTree.insert(checkpoint);
        }

        final TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(123), new TmfLongLocation(123L), 0);

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
    }

    /**
     * Tests that accepts find the correct checkpoint when the time stamp is
     * between checkpoints
     */
    @Test
    public void testAcceptFindInBetween() {
        fBTree = createCollection();
        for (int i = 0; i < CHECKPOINTS_INSERT_NUM; i++) {
            TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(2 * i), new TmfLongLocation((long) 2 * i), i);
            fBTree.insert(checkpoint);
        }

        final TmfCheckpoint searchedCheckpoint = new TmfCheckpoint(new TmfTimestamp(123), new TmfLongLocation(123L), 123);
        final TmfCheckpoint expectedCheckpoint = new TmfCheckpoint(new TmfTimestamp(122), new TmfLongLocation(122L), 122);

        BTreeCheckpointVisitor v = new BTreeCheckpointVisitor(searchedCheckpoint);
        fBTree.accept(v);

        int expectedInsertionPoint = 61;
        int expectedRank = -(expectedInsertionPoint + 2);

        assertEquals(expectedRank, v.getCheckpointRank());

        assertEquals(expectedCheckpoint, v.getCheckpoint());
    }

    /**
     * Tests that accepts finds the correct checkpoint when searching for a
     * checkpoint with a null location. It should return the previous checkpoint
     * from the first checkpoint that matches the timestamp.
     */
    @Test
    public void testAcceptFindInBetweenSameTimestamp() {
        fBTree = createCollection();
        int checkpointNum = 0;
        for (; checkpointNum < 100; checkpointNum++) {
            TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(0), new TmfLongLocation((long) checkpointNum), checkpointNum);
            fBTree.insert(checkpoint);
        }

        for (; checkpointNum < 200; checkpointNum++) {
            TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(1), new TmfLongLocation((long) checkpointNum), checkpointNum);
            fBTree.insert(checkpoint);
        }

        final TmfCheckpoint searchedCheckpoint = new TmfCheckpoint(new TmfTimestamp(1), null, 0);
        final TmfCheckpoint expectedCheckpoint = new TmfCheckpoint(new TmfTimestamp(0), new TmfLongLocation(99L), 0);

        BTreeCheckpointVisitor v = new BTreeCheckpointVisitor(searchedCheckpoint);
        fBTree.accept(v);

        int expectedInsertionPoint = 99;
        int expectedRank = -(expectedInsertionPoint + 2);

        assertEquals(expectedRank, v.getCheckpointRank());

        assertEquals(expectedCheckpoint, v.getCheckpoint());
    }

    /**
     * Test setSize, size
     */
    @Override
    @Test
    public void testSetGetSize() {
        fBTree = createCollection();
        assertEquals(0, fBTree.size());
        int expected = CHECKPOINTS_INSERT_NUM;
        for (int i = 0; i < expected; ++i) {
            fBTree.insert(new TmfCheckpoint(new TmfTimestamp(0), new TmfLongLocation(0L), 0));
            fBTree.setSize(fBTree.size() + 1);
        }
        assertEquals(expected, fBTree.size());
    }

}
