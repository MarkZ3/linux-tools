/*******************************************************************************
 * Copyright (c) 2009, 2013 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Francois Chouinard - Initial API and implementation
 *   Francois Chouinard - Updated as per TMF Trace Model 1.0
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.core.trace;

import java.util.Collections;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.linuxtools.tmf.core.component.ITmfDataProvider;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;
import org.eclipse.linuxtools.tmf.core.statesystem.ITmfStateSystem;
import org.eclipse.linuxtools.tmf.core.statistics.ITmfStatistics;
import org.eclipse.linuxtools.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimeRange;

/**
 * The event stream structure in TMF. In its basic form, a trace has:
 * <ul>
 * <li> an associated Eclipse resource
 * <li> a path to its location on the file system
 * <li> the type of the events it contains
 * <li> the number of events it contains
 * <li> the time range (span) of the events it contains
 * </ul>
 * Concrete ITmfTrace classes have to provide a parameter-less constructor and
 * an initialization method (<i>initTrace</i>) if they are to be opened from
 * the Project View. Also, a validation method (<i>validate</i>) has to be
 * provided to ensure that the trace is of the correct type.
 * <p>
 * A trace can be accessed simultaneously from multiple threads by various
 * application components. To avoid obvious multi-threading issues, the trace
 * uses an ITmfContext as a synchronization aid for its read operations.
 * <p>
 * A proper ITmfContext can be obtained by performing a seek operation on the
 * trace. Seek operations can be performed for a particular event (by rank or
 * timestamp) or for a plain trace location.
 * <p>
 * <b>Example 1</b>: Process a whole trace
 * <pre>
 * ITmfContext context = trace.seekEvent(0);
 * ITmfEvent event = trace.getNext(context);
 * while (event != null) {
 *     processEvent(event);
 *     event = trace.getNext(context);
 * }
 * </pre>
 * <b>Example 2</b>: Process 50 events starting from the 1000th event
 * <pre>
 * int nbEventsRead = 0;
 * ITmfContext context = trace.seekEvent(1000);
 * ITmfEvent event = trace.getNext(context);
 * while (event != null && nbEventsRead < 50) {
 *     nbEventsRead++;
 *     processEvent(event);
 *     event = trace.getNext(context);
 * }
 * </pre>
 * <b>Example 3</b>: Process the events between 2 timestamps (inclusive)
 * <pre>
 * ITmfTimestamp startTime = ...;
 * ITmfTimestamp endTime = ...;
 * ITmfContext context = trace.seekEvent(startTime);
 * ITmfEvent event = trace.getNext(context);
 * while (event != null && event.getTimestamp().compareTo(endTime) <= 0) {
 *     processEvent(event);
 *     event = trace.getNext(context);
 * }
 * </pre>
 * A trace is also an event provider so it can process event requests
 * asynchronously (and coalesce compatible, concurrent requests).
 * <p>
 * </pre>
 * <b>Example 4</b>: Process a whole trace (see ITmfEventRequest for variants)
 * <pre>
 * ITmfRequest request = new TmfEventRequest&lt;MyEventType&gt;(MyEventType.class) {
 *     &#64;Override
 *     public void handleData(MyEventType event) {
 *         super.handleData(event);
 *         processEvent(event);
 *     }
 *     &#64;Override
 *     public void handleCompleted() {
 *         finish();
 *         super.handleCompleted();
 *     }
 * };
 *
 * fTrace.handleRequest(request);
 * if (youWant) {
 *     request.waitForCompletion();
 * }
 * </pre>
 *
 * @version 1.0
 * @author Francois Chouinard
 *
 * @see ITmfContext
 * @see ITmfEvent
 * @see ITmfTraceIndexer
 * @see ITmfEventParser
 */
public interface ITmfTrace extends ITmfDataProvider {

    // ------------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------------

    /**
     * The default trace cache size
     */
    public static final int DEFAULT_TRACE_CACHE_SIZE = 1000;

    // ------------------------------------------------------------------------
    // Initializers
    // ------------------------------------------------------------------------

    /**
     * Initialize a newly instantiated "empty" trace object. This is used to
     * properly parameterize an ITmfTrace instantiated with its parameterless
     * constructor.
     * <p>
     * Typically, the parameterless constructor will provide the block size
     * and its associated parser and indexer.
     *
     * @param resource the trace resource
     * @param path the trace path
     * @param type the trace event type
     * @throws TmfTraceException If we couldn't open the trace
     */
    public void initTrace(IResource resource, String path, Class<? extends ITmfEvent> type) throws TmfTraceException;

    /**
     * Validate that the trace is of the correct type.
     *
     * @param project the eclipse project
     * @param path the trace path
     *
     * @return true if trace is valid
     * @since 2.0
     */
    public IStatus validate(IProject project, String path);

    // ------------------------------------------------------------------------
    // Basic getters
    // ------------------------------------------------------------------------

    /**
     * @return the trace event type
     */
    public Class<? extends ITmfEvent> getEventType();

    /**
     * @return the associated trace resource
     */
    public IResource getResource();

    /**
     * @return the trace path
     */
    public String getPath();

    /**
     * @return the trace cache size
     */
    public int getCacheSize();

    /**
     * @return The statistics provider for this trace
     * @since 2.0
     */
    public ITmfStatistics getStatistics();

    /**
     * Return the map of state systems associated with this trace.
     *
     * This view should be read-only (implementations should use
     * {@link Collections#unmodifiableMap}).
     *
     * @return The map of state systems
     * @since 2.0
     */
    public Map<String, ITmfStateSystem> getStateSystems();

    /**
     * If a state system is not build by the trace itself, it's possible to
     * register it if it comes from another source. It will then be accessible
     * with {@link #getStateSystems} normally.
     *
     * @param id
     *            The unique ID to assign to this state system. In case of
     *            conflicting ID's, the new one will overwrite the previous one
     *            (default Map behavior).
     * @param ss
     *            The already-built state system
     * @since 2.0
     */
    public void registerStateSystem(String id, ITmfStateSystem ss);

    /**
     * Index the trace. Depending on the trace type, this could be done at the
     * constructor or initTrace phase too, so this could be implemented as a
     * no-op.
     *
     * @param waitForCompletion
     *            Should we block the caller until indexing is finished, or not.
     * @since 2.0
     */
    public void indexTrace(boolean waitForCompletion);

    // ------------------------------------------------------------------------
    // Trace characteristics getters
    // ------------------------------------------------------------------------

    /**
     * @return the number of events in the trace
     */
    public long getNbEvents();

    /**
     * @return the trace time range
     * @since 2.0
     */
    public TmfTimeRange getTimeRange();

    /**
     * @return the timestamp of the first trace event
     * @since 2.0
     */
    public ITmfTimestamp getStartTime();

    /**
     * @return the timestamp of the last trace event
     * @since 2.0
     */
    public ITmfTimestamp getEndTime();

    /**
     * @return the streaming interval in ms (0 if not a streaming trace)
     */
    public long getStreamingInterval();

    // ------------------------------------------------------------------------
    // Trace positioning getters
    // ------------------------------------------------------------------------

    /**
     * @return the current trace location
     */
    public ITmfLocation getCurrentLocation();

    /**
     * Returns the ratio (proportion) corresponding to the specified location.
     *
     * @param location a trace specific location
     * @return a floating-point number between 0.0 (beginning) and 1.0 (end)
     */
    public double getLocationRatio(ITmfLocation location);

    // ------------------------------------------------------------------------
    // SeekEvent operations (returning a trace context)
    // ------------------------------------------------------------------------

    /**
     * Position the trace at the specified (trace specific) location.
     * <p>
     * A null location is interpreted as seeking for the first event of the
     * trace.
     * <p>
     * If not null, the location requested must be valid otherwise the returned
     * context is undefined (up to the implementation to recover if possible).
     * <p>
     * @param location the trace specific location
     * @return a context which can later be used to read the corresponding event
     */
    public ITmfContext seekEvent(ITmfLocation location);

    /**
     * Position the trace at the 'rank'th event in the trace.
     * <p>
     * A rank <= 0 is interpreted as seeking for the first event of the
     * trace.
     * <p>
     * If the requested rank is beyond the last trace event, the context
     * returned will yield a null event if used in a subsequent read.
     *
     * @param rank the event rank
     * @return a context which can later be used to read the corresponding event
     */
    public ITmfContext seekEvent(long rank);

    /**
     * Position the trace at the first event with the specified timestamp. If
     * there is no event with the requested timestamp, a context pointing to
     * the next chronological event is returned.
     * <p>
     * A null timestamp is interpreted as seeking for the first event of the
     * trace.
     * <p>
     * If the requested timestamp is beyond the last trace event, the context
     * returned will yield a null event if used in a subsequent read.
     *
     * @param timestamp the timestamp of desired event
     * @return a context which can later be used to read the corresponding event
     * @since 2.0
     */
    public ITmfContext seekEvent(ITmfTimestamp timestamp);

    /**
     * Position the trace at the event located at the specified ratio in the
     * trace file.
     * <p>
     * The notion of ratio (0.0 <= r <= 1.0) is trace specific and left
     * voluntarily vague. Typically, it would refer to the event proportional
     * rank (arguably more intuitive) or timestamp in the trace file.
     *
     * @param ratio the proportional 'rank' in the trace
     * @return a context which can later be used to read the corresponding event
     */
    public ITmfContext seekEvent(double ratio);

    /**
     * Returns the initial range offset
     *
     * @return the initial range offset
     * @since 2.0
     */
    public ITmfTimestamp getInitialRangeOffset();
}
