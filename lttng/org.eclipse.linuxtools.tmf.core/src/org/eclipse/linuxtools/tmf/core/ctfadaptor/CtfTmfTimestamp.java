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

import java.nio.ByteBuffer;

import org.eclipse.linuxtools.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimestamp;

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
     * Create a new CtfTmfTimestamp and serialize it in.
     *
     * @param bufferIn the buffer to read the time stamp from
     * @return the created time stamp
     * @since 3.0
     */
    public static CtfTmfTimestamp newAndSerialize(ByteBuffer bufferIn) {
        CtfTmfTimestamp c = new CtfTmfTimestamp();
        c.serializeIn(bufferIn);
        return c;
    }

}
