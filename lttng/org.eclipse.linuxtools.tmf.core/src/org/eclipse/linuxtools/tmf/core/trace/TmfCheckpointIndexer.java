/*******************************************************************************
 * Copyright (c) 2012, 2013 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Francois Chouinard - Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.core.trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.linuxtools.internal.tmf.core.Messages;
import org.eclipse.linuxtools.tmf.core.component.TmfDataProvider;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.request.ITmfDataRequest;
import org.eclipse.linuxtools.tmf.core.request.ITmfEventRequest;
import org.eclipse.linuxtools.tmf.core.request.TmfDataRequest;
import org.eclipse.linuxtools.tmf.core.request.TmfEventRequest;
import org.eclipse.linuxtools.tmf.core.signal.TmfTraceUpdatedSignal;
import org.eclipse.linuxtools.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimeRange;

/**
 * A simple indexer that manages the trace index as an array of trace
 * checkpoints. Checkpoints are stored at fixed intervals (event rank) in
 * ascending timestamp order.
 * <p>
 * The goal being to access a random trace event reasonably fast from the user's
 * standpoint, picking the right interval value becomes a trade-off between speed
 * and memory usage (a shorter inter-event interval is faster but requires more
 * checkpoints).
 * <p>
 * Locating a specific checkpoint is trivial for both rank (rank % interval) and
 * timestamp (bsearch in the array).
 *
 * @version 1.0
 * @author Francois Chouinard
 *
 * @see ITmfTrace
 * @see ITmfEvent
 */
public class TmfCheckpointIndexer implements ITmfTraceIndexer {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    /** The event trace to index */
    protected final ITmfTrace fTrace;

    /** The interval between checkpoints */
    private final int fCheckpointInterval;

    /** The event trace to index */
    private boolean fIsIndexing;

    /**
     * The trace index. It is composed of checkpoints taken at intervals of
     * fCheckpointInterval events.
     */
    protected final List<ITmfCheckpoint> fTraceIndex;

    /**
     * The indexing request
     */
    private ITmfEventRequest fIndexingRequest = null;

    // ------------------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------------------

    /**
     * Basic constructor that uses the default trace block size as checkpoints
     * intervals
     *
     * @param trace the trace to index
     */
    public TmfCheckpointIndexer(final ITmfTrace trace) {
        this(trace, TmfDataProvider.DEFAULT_BLOCK_SIZE);
    }

    /**
     * Full trace indexer
     *
     * @param trace the trace to index
     * @param interval the checkpoints interval
     */
    public TmfCheckpointIndexer(final ITmfTrace trace, final int interval) {
        fTrace = trace;
        fCheckpointInterval = interval;
        fTraceIndex = new ArrayList<ITmfCheckpoint>();
        fIsIndexing = false;
    }

    @Override
    public void dispose() {
        if ((fIndexingRequest != null) && !fIndexingRequest.isCompleted()) {
            fIndexingRequest.cancel();
            fTraceIndex.clear();
        }
    }

    // ------------------------------------------------------------------------
    // ITmfTraceIndexer - isIndexing
    // ------------------------------------------------------------------------

    @Override
    public boolean isIndexing() {
        return fIsIndexing;
    }

    // ------------------------------------------------------------------------
    // ITmfTraceIndexer - buildIndex
    // ------------------------------------------------------------------------

    /**
     * @since 2.0
     */
    @Override
    public void buildIndex(final long offset, final TmfTimeRange range, final boolean waitForCompletion) {

        // Don't do anything if we are already indexing
        synchronized (fTraceIndex) {
            if (fIsIndexing) {
                return;
            }
            fIsIndexing = true;
        }

        // The monitoring job
        final Job job = new Job("Indexing " + fTrace.getName() + "...") { //$NON-NLS-1$ //$NON-NLS-2$
            @Override
            protected IStatus run(final IProgressMonitor monitor) {
                monitor.beginTask("", IProgressMonitor.UNKNOWN); //$NON-NLS-1$
                while (!monitor.isCanceled()) {
                    try {
                        long prevNbEvents = fTrace.getNbEvents();
                        Thread.sleep(250);
                        long nbEvents = fTrace.getNbEvents();
                        setName(Messages.TmfCheckpointIndexer_Indexing + ' ' + fTrace.getName() + " (" + nbEvents + ")"); //$NON-NLS-1$ //$NON-NLS-2$
                        // setName doesn't refresh the UI, setTaskName does
                        long rate = (nbEvents - prevNbEvents) * 4;
                        monitor.setTaskName(rate + " " + Messages.TmfCheckpointIndexer_EventsPerSecond); //$NON-NLS-1$
                    } catch (final InterruptedException e) {
                        return Status.OK_STATUS;
                    }
                }
                monitor.done();
                return Status.OK_STATUS;
            }
        };
        job.schedule();

        // Build a background request for all the trace data. The index is
        // updated as we go by readNextEvent().
        fIndexingRequest = new TmfEventRequest(ITmfEvent.class,
                range, offset, TmfDataRequest.ALL_DATA, fCheckpointInterval, ITmfDataRequest.ExecutionType.BACKGROUND)
        {
            @Override
            public void handleData(final ITmfEvent event) {
                super.handleData(event);
                if (event != null) {
                    // Update the trace status at regular intervals
                    if ((getNbRead() % fCheckpointInterval) == 0) {
                        updateTraceStatus();
                    }
                }
            }

            @Override
            public void handleSuccess() {
                updateTraceStatus();
            }

            @Override
            public void handleCompleted() {
                job.cancel();
                super.handleCompleted();
                fIsIndexing = false;
            }

            private void updateTraceStatus() {
                if (fTrace.getNbEvents() > 0) {
                    signalNewTimeRange(fTrace.getStartTime(), fTrace.getEndTime());
                }
            }
        };

        // Submit the request and wait for completion if required
        fTrace.sendRequest(fIndexingRequest);
        if (waitForCompletion) {
            try {
                fIndexingRequest.waitForCompletion();
            } catch (final InterruptedException e) {
            }
        }
    }

    /**
     * Notify the interested parties that the trace time range has changed
     *
     * @param startTime the new start time
     * @param endTime the new end time
     */
    private void signalNewTimeRange(final ITmfTimestamp startTime, final ITmfTimestamp endTime) {
        fTrace.broadcast(new TmfTraceUpdatedSignal(fTrace, fTrace, new TmfTimeRange(startTime, endTime)));
    }

    // ------------------------------------------------------------------------
    // ITmfTraceIndexer - updateIndex
    // ------------------------------------------------------------------------

    /**
     * @since 2.0
     */
    @Override
    public synchronized void updateIndex(final ITmfContext context, final ITmfTimestamp timestamp) {
        if ((context.getRank() % fCheckpointInterval) == 0) {
            // Determine the table position
            final long position = context.getRank() / fCheckpointInterval;
            // Add new entry at proper location (if empty)
            if (fTraceIndex.size() == position) {
                fTraceIndex.add(new TmfCheckpoint(timestamp, context.getLocation()));
            }
        }
    }

    // ------------------------------------------------------------------------
    // ITmfTraceIndexer - seekIndex
    // ------------------------------------------------------------------------

    /**
     * @since 2.0
     */
    @Override
    public synchronized ITmfContext seekIndex(final ITmfTimestamp timestamp) {

        // A null timestamp indicates to seek the first event
        if (timestamp == null) {
            return fTrace.seekEvent(0);
        }

        // Find the checkpoint at or before the requested timestamp.
        // In the very likely event that the timestamp is not at a checkpoint
        // boundary, bsearch will return index = (- (insertion point + 1)).
        // It is then trivial to compute the index of the previous checkpoint.
        int index = Collections.binarySearch(fTraceIndex, new TmfCheckpoint(timestamp, null));
        if (index < 0) {
            index = Math.max(0, -(index + 2));
        } else {
            // If timestamp was in the list, use previous index to be able to find the
            // first event with the same timestamp before the checkpoint
            index = Math.max(0, index - 1);
        }

        // Position the trace at the checkpoint
        return restoreCheckpoint(index);
    }

    @Override
    public ITmfContext seekIndex(final long rank) {

        // A rank < 0 indicates to seek the first event
        if (rank < 0) {
            return fTrace.seekEvent(0);
        }

        // Find the checkpoint at or before the requested rank.
        final int index = (int) rank / fCheckpointInterval;

        // Position the trace at the checkpoint
        return restoreCheckpoint(index);
    }

    /**
     * Position the trace at the given checkpoint
     *
     * @param checkpoint the checkpoint index
     * @return the corresponding context
     */
    private ITmfContext restoreCheckpoint(final int checkpoint) {
        ITmfLocation location = null;
        int index = 0;
        synchronized (fTraceIndex) {
            if (!fTraceIndex.isEmpty()) {
                index = checkpoint;
                if (index >= fTraceIndex.size()) {
                    index = fTraceIndex.size() - 1;
                }
                location = fTraceIndex.get(index).getLocation();
            }
        }
        final ITmfContext context = fTrace.seekEvent(location);
        context.setRank((long) index * fCheckpointInterval);
        return context;
    }

    // ------------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------------

    /**
     * @return the trace index
     */
    protected List<ITmfCheckpoint> getTraceIndex() {
        return fTraceIndex;
    }

}
