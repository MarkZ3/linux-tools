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

import org.eclipse.linuxtools.internal.tmf.core.trace.indexer.FlatArray;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.linuxtools.tmf.core.trace.indexer.ITmfPersistentlyIndexable;
import org.eclipse.linuxtools.tmf.core.trace.indexer.checkpoint.ITmfCheckpoint;
import org.eclipse.linuxtools.tmf.core.trace.indexer.checkpoint.TmfCheckpoint;
import org.eclipse.linuxtools.tmf.core.trace.location.TmfLongLocation;
import org.junit.Test;

/**
 * Tests for the FlatArray class
 *
 * @author Marc-Andre Laperle
 */
public class FlatArrayTest extends AbstractCheckpointCollectionTest {

    private FlatArray fFlatArray;

    @Override
    protected FlatArray createCollection() {
        FlatArray flatArray = new FlatArray(getFile(), (ITmfPersistentlyIndexable)getTrace());
        fCheckpointCollection = flatArray;
        return flatArray;
    }

    @Override
    public boolean isPersistableCollection() {
        return true;
    }

    /**
     * Test a single insertion
     */
    @Test
    public void testInsert() {
        fFlatArray = createCollection();
        TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(12345), new TmfLongLocation(123456L), 0);
        fFlatArray.insert(checkpoint);

        ITmfCheckpoint fFileCheckpoint = fFlatArray.get(0);
        assertEquals(checkpoint, fFileCheckpoint);

        long found = fFlatArray.binarySearch(checkpoint);
        assertEquals(0, found);

        fFlatArray.dispose();
    }

    /**
     * Test many checkpoint insertions. Make sure they can be found after
     * re-opening the fFile
     */
    @Test
    public void testInsertAlot() {
        fFlatArray = createCollection();
        for (int i = 0; i < CHECKPOINTS_INSERT_NUM; i++) {
            TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(12345 + i), new TmfLongLocation(123456L + i), i);
            fFlatArray.insert(checkpoint);
        }
        fFlatArray.setIndexComplete();
        fFlatArray.dispose();

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

        fFlatArray = createCollection();

        for (int i = 0; i < CHECKPOINTS_INSERT_NUM; i++) {
            Integer randomCheckpoint = list.get(i);
            TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(12345 + randomCheckpoint), new TmfLongLocation(123456L + randomCheckpoint), 0);

            ITmfCheckpoint fFileCheckpoint = fFlatArray.get(randomCheckpoint);
            assertEquals(checkpoint, fFileCheckpoint);
        }

        for (int i = 0; i < CHECKPOINTS_INSERT_NUM; i++) {
            Integer randomCheckpoint = list.get(i);
            TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(12345 + randomCheckpoint), new TmfLongLocation(123456L + randomCheckpoint), 0);

            long found = fFlatArray.binarySearch(checkpoint);
            assertEquals(randomCheckpoint.intValue(), found);
        }

        fFlatArray.dispose();
    }

    /**
     * Test many checkpoint insertions using the same timestamp. Make sure they
     * can be found after re-opening the fFile
     */
    @Test
    public void testInsertSameTimestamp() {
        fFlatArray = createCollection();
        for (int i = 0; i < CHECKPOINTS_INSERT_NUM; i++) {
            TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(12345), new TmfLongLocation(123456L + i), i);
            fFlatArray.insert(checkpoint);
        }
        fFlatArray.setIndexComplete();
        fFlatArray.dispose();

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

        fFlatArray = createCollection();

        for (int i = 0; i < CHECKPOINTS_INSERT_NUM; i++) {
            Integer randomCheckpoint = list.get(i);
            TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(12345), new TmfLongLocation(123456L + randomCheckpoint), 0);

            ITmfCheckpoint fFileCheckpoint = fFlatArray.get(randomCheckpoint);
            assertEquals(checkpoint, fFileCheckpoint);
        }

        for (int i = 0; i < CHECKPOINTS_INSERT_NUM; i++) {
            Integer randomCheckpoint = list.get(i);
            TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(12345), new TmfLongLocation(123456L + randomCheckpoint), 0);

            long found = fFlatArray.binarySearch(checkpoint);
            assertEquals(randomCheckpoint.intValue(), found);
        }

        fFlatArray.dispose();
    }

    /**
     * Tests that binarySearch find the correct checkpoint and ends with a
     * perfect match
     */
    @Test
    public void testBinarySearch() {
        fFlatArray = createCollection();
        for (long i = 0; i < CHECKPOINTS_INSERT_NUM; i++) {
            TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(i), new TmfLongLocation(i), 0);
            fFlatArray.insert(checkpoint);
        }

        TmfCheckpoint expectedCheckpoint = new TmfCheckpoint(new TmfTimestamp(122), new TmfLongLocation(122L), 0);
        int expectedRank = 122;

        long rank = fFlatArray.binarySearch(expectedCheckpoint);
        ITmfCheckpoint found = fFlatArray.get(rank);

        assertEquals(expectedRank, rank);
        assertEquals(found, expectedCheckpoint);
        fFlatArray.dispose();
    }

    /**
     * Tests that binarySearch find the correct checkpoint when the time stamp
     * is between checkpoints
     */
    @Test
    public void testBinarySearchFindInBetween() {
        fFlatArray = createCollection();
        for (long i = 0; i < CHECKPOINTS_INSERT_NUM; i++) {
            TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(2 * i), new TmfLongLocation(2 * i), 0);
            fFlatArray.insert(checkpoint);
        }

        TmfCheckpoint searchedCheckpoint = new TmfCheckpoint(new TmfTimestamp(123), new TmfLongLocation(123L), 0);
        TmfCheckpoint expectedCheckpoint = new TmfCheckpoint(new TmfTimestamp(122), new TmfLongLocation(122L), 0);
        int expectedInsertionPoint = 61;
        int expectedRank = -(expectedInsertionPoint + 2);

        long rank = fFlatArray.binarySearch(searchedCheckpoint);
        assertEquals(expectedRank, rank);
        ITmfCheckpoint found = fFlatArray.get(expectedInsertionPoint);

        assertEquals(expectedRank, rank);
        assertEquals(found, expectedCheckpoint);
        fFlatArray.dispose();
    }

    /**
     * Tests that binarySearch finds the correct checkpoint when searching for a
     * checkpoint with a null location. It should return the previous checkpoint
     * from the first checkpoint that matches the timestamp.
     */
    @Test
    public void testFindInBetweenSameTimestamp() {
        fFlatArray = createCollection();
        int checkpointNum = 0;
        for (; checkpointNum < 100; checkpointNum++) {
            TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(0), new TmfLongLocation((long) checkpointNum), checkpointNum);
            fFlatArray.insert(checkpoint);
        }

        for (; checkpointNum < 200; checkpointNum++) {
            TmfCheckpoint checkpoint = new TmfCheckpoint(new TmfTimestamp(1), new TmfLongLocation((long) checkpointNum), checkpointNum);
            fFlatArray.insert(checkpoint);
        }

        final TmfCheckpoint searchedCheckpoint = new TmfCheckpoint(new TmfTimestamp(1), null, 0);
        final TmfCheckpoint expectedCheckpoint = new TmfCheckpoint(new TmfTimestamp(0), new TmfLongLocation(99L), 0);

        int expectedInsertionPoint = 99;
        int expectedRank = -(expectedInsertionPoint + 2);

        long rank = fFlatArray.binarySearch(searchedCheckpoint);
        assertEquals(expectedRank, rank);
        ITmfCheckpoint found = fFlatArray.get(expectedInsertionPoint);

        assertEquals(expectedRank, rank);
        assertEquals(found, expectedCheckpoint);
        fFlatArray.dispose();
    }

}
