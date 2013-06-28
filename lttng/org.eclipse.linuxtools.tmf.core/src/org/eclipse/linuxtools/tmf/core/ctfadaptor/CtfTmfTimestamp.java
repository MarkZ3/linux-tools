/*******************************************************************************
 * Copyright (c) 2012, 2013 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Matthew Khouzam - Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.core.ctfadaptor;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.linuxtools.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.linuxtools.tmf.core.trace.index.Database;

/**
 * The CTF adapter for the TMF timestamp. It's basically the same as a
 * TmfTimestamp, but the scale is always nanoseconds, and the precision is 0.
 *
 * @version 1.2
 * @author Matthew khouzam
 */
public final class CtfTmfTimestamp extends TmfTimestamp {

    /**
     * Constructor for CtfTmfTimestamp.
     *
     * @param timestamp
     *            The timestamp value (in nanoseconds)
     */
    public CtfTmfTimestamp(long timestamp) {
        super(timestamp, ITmfTimestamp.NANOSECOND_SCALE, 0);
    }

    private CtfTmfTimestamp() {
    }

    /**
     * @throws CoreException
     * @since 3.0
     */
    public static CtfTmfTimestamp newAndSerialize(Database db, long rec) throws CoreException {
        CtfTmfTimestamp c = new CtfTmfTimestamp();
        c.serialize(db, rec);
        return c;
    }

}
