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

package org.eclipse.linuxtools.tmf.core.trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.linuxtools.tmf.core.trace.indexer.checkpoint.ITmfCheckpoint;

/**
 * @since 3.0
 */
public class TmfMemoryIndex implements ITmfTraceIndex {

    List<ITmfCheckpoint> fCheckpoints;

    /**
     * @param trace
     *
     */
    public TmfMemoryIndex(ITmfTrace trace) {
        fCheckpoints = new ArrayList<ITmfCheckpoint>();
    }

    @Override
    public void dispose() {
        fCheckpoints.clear();
    }

    @Override
    public void add(ITmfCheckpoint checkpoint) {
        fCheckpoints.add(checkpoint);
    }

    @Override
    public ITmfCheckpoint get(int checkpoint) {
        return fCheckpoints.get(checkpoint);
    }

    @Override
    public int binarySearch(ITmfCheckpoint checkpoint) {
        return Collections.binarySearch(fCheckpoints, checkpoint);
    }

    @Override
    public boolean isEmpty() {
        return fCheckpoints.isEmpty();
    }

    @Override
    public int size() {
        return fCheckpoints.size();
    }

    @Override
    public boolean restore() {
        return false;
    }

    @Override
    public void setTimeRange(TmfTimeRange timeRange) {
    }

    @Override
    public void setNbEvents(long nbEvents) {
    }

    @Override
    public TmfTimeRange getTimeRange() {
        return null;
    }

    @Override
    public long getNbEvents() {
        return 0;
    }
}
