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


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.linuxtools.internal.tmf.core.IndexHelper;

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

    @Override
    public Long getLocationInfo() {
        return (Long) super.getLocationInfo();
    }

    /**
     * @throws IOException
     * @since 3.0
     */
    @Override
    public void serialize(OutputStream stream) throws IOException {
        IndexHelper.writeLong(stream, getLocationInfo().longValue());

    }

    /**
     * @throws IOException
     * @since 3.0
     */
    @Override
    public void serialize(InputStream stream) throws IOException {
    }

    /**
     * @since 3.0
     */
    public static ITmfLocation newAndserialize(InputStream stream) throws IOException {
        long longLocation = IndexHelper.readLong(stream);
        return new TmfLongLocation(longLocation);
    }

}
