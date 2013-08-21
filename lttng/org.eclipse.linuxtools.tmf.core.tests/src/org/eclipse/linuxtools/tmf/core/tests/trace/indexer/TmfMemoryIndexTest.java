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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Random;

import org.eclipse.linuxtools.internal.tmf.core.trace.indexer.TmfMemoryIndex;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.linuxtools.tmf.core.trace.indexer.checkpoint.ITmfCheckpoint;
import org.eclipse.linuxtools.tmf.core.trace.indexer.checkpoint.TmfCheckpoint;
import org.eclipse.linuxtools.tmf.core.trace.location.TmfLongLocation;
import org.junit.Test;

/**
 * Test for the TmfMemoryIndex class
 *
 * @author Marc-Andre Laperle
 */
public class TmfMemoryIndexTest extends AbstractCheckpointCollectionTest {

    private TmfMemoryIndex fMemoryIndex;

    /**
     * Test a single insertion
     */
    @Test
    public void testInsert() {
        fMemoryIndex = createCollection();
        TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(12345), new TmfLongLocation(123456L), 0);
        fMemoryIndex.insert(checkpoint);

        ITmfCheckpoint indexCheckpoint = fMemoryIndex.get(0);
        assertEquals(checkpoint, indexCheckpoint);

        long found = fMemoryIndex.binarySearch(checkpoint);
        assertEquals(0, found);

        fMemoryIndex.dispose();
    }

    /**
     * Test many checkpoint insertions.
     * Make sure they can be found after inserting
     */
    @Test
    public void testInsertAlot() {
        fMemoryIndex = createCollection();
        for (int i = 0; i < CHECKPOINTS_INSERT_NUM; i++) {
            TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(12345 + i), new TmfLongLocation(123456L + i), i);
            fMemoryIndex.insert(checkpoint);
        }

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

        for (int i = 0; i < CHECKPOINTS_INSERT_NUM; i++) {
            Integer randomCheckpoint = list.get(i);
            TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(12345 + randomCheckpoint), new TmfLongLocation(123456L + randomCheckpoint), 0);

            ITmfCheckpoint indexCheckpoint = fMemoryIndex.get(randomCheckpoint);
            assertEquals(checkpoint, indexCheckpoint);
        }

        for (int i = 0; i < CHECKPOINTS_INSERT_NUM; i++) {
            Integer randomCheckpoint = list.get(i);
            TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(12345 + randomCheckpoint), new TmfLongLocation(123456L + randomCheckpoint), 0);

            long found = fMemoryIndex.binarySearch(checkpoint);
            assertEquals(randomCheckpoint.intValue(), found);
        }

        fMemoryIndex.dispose();
    }

    /**
     * Test many checkpoint insertions using the same timestamp.
     */
    @Test
    public void testInsertSameTimestamp() {
        fMemoryIndex = createCollection();
        for (int i = 0; i < CHECKPOINTS_INSERT_NUM; i++) {
            TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(12345), new TmfLongLocation(123456L + i), i);
            fMemoryIndex.insert(checkpoint);
        }

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

        for (int i = 0; i < CHECKPOINTS_INSERT_NUM; i++) {
            Integer randomCheckpoint = list.get(i);
            TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(12345), new TmfLongLocation(123456L + randomCheckpoint), 0);

            ITmfCheckpoint indexCheckpoint = fMemoryIndex.get(randomCheckpoint);
            assertEquals(checkpoint, indexCheckpoint);
        }

        for (int i = 0; i < CHECKPOINTS_INSERT_NUM; i++) {
            Integer randomCheckpoint = list.get(i);
            TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(12345), new TmfLongLocation(123456L + randomCheckpoint), 0);

            long found = fMemoryIndex.binarySearch(checkpoint);
            assertEquals(randomCheckpoint.intValue(), found);
        }

        fMemoryIndex.dispose();
    }

    /**
     * Tests that binarySearch find the correct checkpoint and ends with a perfect match
     */
    @Test
    public void testBinarySearch() {
        fMemoryIndex = createCollection();
        for (long i = 0; i < CHECKPOINTS_INSERT_NUM; i++) {
            TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(i), new TmfLongLocation(i), 0);
            fMemoryIndex.insert(checkpoint);
        }

        TmfCheckpoint expectedCheckpoint = new TmfCheckpoint(new TmfTimestamp(122), new TmfLongLocation(122L), 0);
        int expectedRank = 122;

        long rank = fMemoryIndex.binarySearch(expectedCheckpoint);
        ITmfCheckpoint found = fMemoryIndex.get(rank);

        assertEquals(expectedRank, rank);
        assertEquals(found, expectedCheckpoint);
        fMemoryIndex.dispose();
    }

    /**
     * Tests that binarySearch find the correct checkpoint when the time stamp is between checkpoints
     */
    @Test
    public void testBinarySearchFindInBetween() {
        fMemoryIndex = createCollection();
        for (long i = 0; i < CHECKPOINTS_INSERT_NUM; i++) {
            TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(2 * i), new TmfLongLocation(2 * i), 0);
            fMemoryIndex.insert(checkpoint);
        }

        TmfCheckpoint searchedCheckpoint = new TmfCheckpoint(new TmfTimestamp(123), new TmfLongLocation(123L), 0);
        TmfCheckpoint expectedCheckpoint = new TmfCheckpoint(new TmfTimestamp(122), new TmfLongLocation(122L), 0);
        int expectedInsertionPoint = 61;
        int expectedRank = -(expectedInsertionPoint + 2);

        long rank = fMemoryIndex.binarySearch(searchedCheckpoint);
        assertEquals(expectedRank, rank);

        ITmfCheckpoint found = fMemoryIndex.get(expectedInsertionPoint);

        assertEquals(found, expectedCheckpoint);
        fMemoryIndex.dispose();
    }

    /**
     * Tests that binarySearch finds the correct checkpoint when searching for a checkpoint with a null location.
     * It should return the previous checkpoint from the first checkpoint that matches the timestamp.
     */
    @Test
    public void testFindInBetweenSameTimestamp() {
        fMemoryIndex = createCollection();
        int checkpointNum = 0;
        for (; checkpointNum < 100; checkpointNum++) {
            TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(0), new TmfLongLocation((long)checkpointNum), checkpointNum);
            fMemoryIndex.insert(checkpoint);
        }

        for (; checkpointNum < 200; checkpointNum++) {
            TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(1), new TmfLongLocation((long)checkpointNum), checkpointNum);
            fMemoryIndex.insert(checkpoint);
        }

        final TmfCheckpoint searchedCheckpoint = new TmfCheckpoint(new TmfTimestamp(1), null, 0);
        final TmfCheckpoint expectedCheckpoint = new TmfCheckpoint(new TmfTimestamp(0), new TmfLongLocation(99L), 0);

        int expectedInsertionPoint = 99;
        int expectedRank = -(expectedInsertionPoint + 2);

        long rank = fMemoryIndex.binarySearch(searchedCheckpoint);
        assertEquals(expectedRank, rank);
        ITmfCheckpoint found = fMemoryIndex.get(expectedInsertionPoint);

        assertEquals(expectedRank, rank);
        assertEquals(found, expectedCheckpoint);
        fMemoryIndex.dispose();
    }

    /**
     * Test dispose
     */
    @Test
    public void testDispose() {
        fMemoryIndex = createCollection();
        fMemoryIndex.dispose();
        assertTrue(fMemoryIndex.isEmpty());
    }

    @Override
    protected TmfMemoryIndex createCollection() {
        return new TmfMemoryIndex(getTrace());
    }
}
