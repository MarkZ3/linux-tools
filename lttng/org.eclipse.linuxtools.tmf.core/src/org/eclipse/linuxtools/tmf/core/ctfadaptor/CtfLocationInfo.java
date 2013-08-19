/*******************************************************************************
 * Copyright (c) 2012, 2013 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Matthew Khouzam - Initial API and implementation
 *******************************************************************************/
package org.eclipse.linuxtools.tmf.core.ctfadaptor;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.eclipse.linuxtools.internal.tmf.core.IndexHelper;

/**
 * The data object to go in a {@link CtfLocation}.
 *
 * @author Matthew Khouzam
 * @since 2.0
 */
public class CtfLocationInfo implements Comparable<CtfLocationInfo> {

    private long timestamp;
    private long index;

    /**
     * @param ts
     *            Timestamp
     * @param index
     *            Index of this event (if there are N elements with the same
     *            timestamp, which one is it.)
     */
    public CtfLocationInfo(long ts, long index) {
        this.timestamp = ts;
        this.index = index;
    }

    /**
     * @return The timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * @return The index of the element
     */
    public long getIndex() {
        return index;
    }

    // ------------------------------------------------------------------------
    // Object
    // ------------------------------------------------------------------------

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + (int) (index ^ (index >>> 32));
        result = (prime * result) + (int) (timestamp ^ (timestamp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof CtfLocationInfo)) {
            return false;
        }
        CtfLocationInfo other = (CtfLocationInfo) obj;
        if (index != other.index) {
            return false;
        }
        if (timestamp != other.timestamp) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Element [" + timestamp + '/' + index + ']'; //$NON-NLS-1$
    }

    // ------------------------------------------------------------------------
    // Comparable
    // ------------------------------------------------------------------------

    @Override
    public int compareTo(CtfLocationInfo other) {
        if (this.timestamp > other.getTimestamp()) {
            return 1;
        }
        if (this.timestamp < other.getTimestamp()) {
            return -1;
        }
        if (this.index > other.getIndex()) {
            return 1;
        }
        if (this.index < other.getIndex()) {
            return -1;
        }
        return 0;
    }

    /**
     * @param stream
     * @throws IOException
     * @since 3.0
     */
    public void serializeOut(ByteBuffer bufferOut) throws IOException {
        IndexHelper.writeLong(bufferOut, timestamp);
        IndexHelper.writeLong(bufferOut, index);

    }

    /**
     * @param stream
     * @throws IOException
     * @since 3.0
     */
    public void serializeIn(ByteBuffer bufferIn) throws IOException {
        timestamp = IndexHelper.readLong(bufferIn);
        index = IndexHelper.readLong(bufferIn);

    }
}
