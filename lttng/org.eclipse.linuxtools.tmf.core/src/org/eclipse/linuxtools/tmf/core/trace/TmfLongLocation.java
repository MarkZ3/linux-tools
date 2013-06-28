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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.linuxtools.tmf.core.trace.index.Database;

/**
 * A concrete implementation of TmfLocation based on Long:s
 *
 * @author Francois Chouinard
 * @since 2.0
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
     * @since 3.0
     */
    @Override
    public long serialize(Database db) throws CoreException {
        long record = db.malloc(8);
        db.putLong(record, getLocationInfo().longValue());
        return record;
    }

    /**
     * @since 3.0
     */
    @Override
    public void serialize(Database db, long rec) throws CoreException {

    }

}
