/*******************************************************************************
 * Copyright (c) 2009, 2012 Ericsson
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.linuxtools.tmf.core.trace.index.Database;

/**
 * The generic trace location in TMF.
 * <p>
 * An ITmfLocation is the equivalent of a random-access file position, holding
 * enough information to allow the positioning of the trace 'pointer' to read an
 * arbitrary event.
 * <p>
 * This location is trace-specific, must be comparable and immutable.
 *
 * @version 2.0
 * @author Francois Chouinard
 */
public interface ITmfLocation {

    // ------------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------------

    /**
     * Returns the concrete trace location information
     *
     * @return the location information
     * @since 2.0
     */
    Comparable<?> getLocationInfo();

    /**
     * @param db
     * @return the record where the location lives
     * @throws CoreException
     * @since 3.0
     */
    long serialize(Database db) throws CoreException;
    /**
     * @param stream
     * @throws IOException
     * @since 3.0
     */
    void serialize(Database db, long rec) throws CoreException;

}
