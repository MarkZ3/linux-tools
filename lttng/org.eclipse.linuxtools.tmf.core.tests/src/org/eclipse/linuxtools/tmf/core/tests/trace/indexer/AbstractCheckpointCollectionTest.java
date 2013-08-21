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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.eclipse.linuxtools.internal.tmf.core.trace.indexer.ICheckpointCollection;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.indexer.checkpoint.TmfCheckpoint;
import org.eclipse.linuxtools.tmf.core.trace.location.TmfLongLocation;
import org.eclipse.linuxtools.tmf.tests.stubs.trace.TmfTraceStub;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Common code for ICheckpointCollection test classes
 *
 * @author Marc-Andre Laperle
 */
public abstract class AbstractCheckpointCollectionTest {

    private static final String INDEX_FILE_NAME = "checkpoint.idx"; //$NON-NLS-1$

    /**
     * The number of checkpoints to be inserted in insert tests
     */
    protected static final int CHECKPOINTS_INSERT_NUM = 50000;
    /**
     * The collection being tested
     */
    protected ICheckpointCollection fCheckpointCollection = null;

    private TmfTraceStub fTrace;
    private File fFile = new File(INDEX_FILE_NAME);

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
        if (fCheckpointCollection != null) {
            fCheckpointCollection.dispose();
        }
        if (fFile.exists()) {
            fFile.delete();
        }
    }

    /**
     * Get the trace being tested.
     *
     * @return the trace being tested.
     */
    public ITmfTrace getTrace() {
        return fTrace;
    }

    /**
     * Returns whether or not the collection is persisted to disk
     *
     * @return true if the collection is persisted to disk, false otherwise
     */
    public boolean isPersistableCollection() {
        return false;
    }

    /**
     * Get the file used for the index being tested.
     *
     * @return the file used for the index being tested.
     */
    public File getFile() {
        return fFile;
    }

    /**
     * Test constructing a new checkpoint collection
     */
    @Test
    public void testConstructor() {
        fCheckpointCollection = createCollection();
        if (isPersistableCollection()) {
            assertTrue(fFile.exists());
        }
        assertTrue(fCheckpointCollection.isCreatedFromScratch());
        fCheckpointCollection.dispose();
    }

    /**
     * Test constructing a new checkpoint collection, existing file
     */
    @Test
    public void testConstructorExistingFile() {
        if (isPersistableCollection()) {
            fCheckpointCollection = createCollection();
            assertTrue(fFile.exists());
            fCheckpointCollection.setIndexComplete();
            fCheckpointCollection.dispose();

            fCheckpointCollection = createCollection();
            assertFalse(fCheckpointCollection.isCreatedFromScratch());
            fCheckpointCollection.dispose();
        }
    }

    /**
     * Test that a new checkpoint collection is considered created from scratch
     * and vice versa
     */
    @Test
    public void testIsCreatedFromScratch() {
        fCheckpointCollection = createCollection();
        assertTrue(fCheckpointCollection.isCreatedFromScratch());
        fCheckpointCollection.setIndexComplete();
        fCheckpointCollection.dispose();

        if (isPersistableCollection()) {
            fCheckpointCollection = createCollection();
            assertFalse(fCheckpointCollection.isCreatedFromScratch());
            fCheckpointCollection.dispose();
        }
    }

    /**
     * Test setTimeRange, getTimeRange
     */
    @Test
    public void testSetGetTimeRange() {
        if (isPersistableCollection()) {
            fCheckpointCollection = createCollection();
            TmfTimeRange timeRange = new TmfTimeRange(new TmfTimestamp(0), new TmfTimestamp(100));
            fCheckpointCollection.setTimeRange(timeRange);
            assertEquals(timeRange, fCheckpointCollection.getTimeRange());
            fCheckpointCollection.dispose();
        }
    }

    /**
     * Create a collection for the test
     *
     * @return the collection
     */
    abstract protected ICheckpointCollection createCollection();

    /**
     * Test setNbEvents, getNbEvents
     */
    @Test
    public void testSetGetNbEvents() {
        if (isPersistableCollection()) {
            fCheckpointCollection = createCollection();
            int expected = 12345;
            fCheckpointCollection.setNbEvents(expected);
            assertEquals(expected, fCheckpointCollection.getNbEvents());
            fCheckpointCollection.dispose();
        }
    }

    /**
     * Test setSize, size
     */
    @Test
    public void testSetGetSize() {
        fCheckpointCollection = createCollection();
        assertEquals(0, fCheckpointCollection.size());
        int expected = CHECKPOINTS_INSERT_NUM;
        for (int i = 0; i < expected; ++i) {
            fCheckpointCollection.insert(new TmfCheckpoint(new TmfTimestamp(0), new TmfLongLocation(0L), 0));
        }
        assertEquals(expected, fCheckpointCollection.size());
        fCheckpointCollection.dispose();
    }

    /**
     * Test delete
     */
    @Test
    public void testDelete() {
        if (isPersistableCollection()) {
            fCheckpointCollection = createCollection();
            assertTrue(fFile.exists());
            fCheckpointCollection.delete();
            assertFalse(fFile.exists());
        }
    }

    /**
     * Test version change
     *
     * @throws IOException
     *             can throw this
     */
    @Test
    public void testVersionChange() throws IOException {
        fCheckpointCollection = createCollection();
        fCheckpointCollection.setIndexComplete();
        fCheckpointCollection.dispose();
        RandomAccessFile f = new RandomAccessFile(fFile, "rw");
        f.writeInt(-1);
        f.close();

        fCheckpointCollection = createCollection();
        assertTrue(fCheckpointCollection.isCreatedFromScratch());
        fCheckpointCollection.dispose();
    }
}
