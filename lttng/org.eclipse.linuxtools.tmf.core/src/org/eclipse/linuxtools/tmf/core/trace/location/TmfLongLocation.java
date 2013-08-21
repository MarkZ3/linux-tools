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

package org.eclipse.linuxtools.tmf.core.trace.location;


import java.nio.ByteBuffer;

/**
 * A concrete implementation of TmfLocation based on Long:s
 *
 * @author Francois Chouinard
 * @since 3.0
 */
public final class TmfLongLocation extends TmfLocation {

    /**
     * The normal constructor
     *
     * @param locationInfo the concrete location
     */
    public TmfLongLocation(final Long locationInfo) {
        super(locationInfo);
    }

    /**
     * The copy constructor
     *
     * @param other the other location
     */
    public TmfLongLocation(final TmfLongLocation other) {
        super(other.getLocationInfo());
    }

    /**
     * Empty constructor. Useful for serialization.
     */
    protected TmfLongLocation() {
    }

    @Override
    public Long getLocationInfo() {
        return (Long) super.getLocationInfo();
    }

    /**
     * @since 3.0
     */
    @Override
    public void serializeOut(ByteBuffer bufferOut) {
        bufferOut.putLong(getLocationInfo().longValue());
    }

    /**
     * @since 3.0
     */
    @Override
    public void serializeIn(ByteBuffer bufferIn) {
        this.fLocationInfo = bufferIn.getLong();
    }

    /**
     * Create a new TmfLongLocation and serialize it in.
     *
     * @param bufferIn the buffer to read the TmfLongLocation from
     * @return the created TmfLongLocation
     * @since 3.0
     */
    public static ITmfLocation newAndserialize(ByteBuffer bufferIn) {
        TmfLongLocation location = new TmfLongLocation();
        location.serializeIn(bufferIn);
        return location;
    }

}
