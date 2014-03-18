/**********************************************************************
 * Copyright (c) 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Matthew Khouzam - Initial implementation and API
 *   Marc-Andre Laperle - Initial implementation and API
 **********************************************************************/

package org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Get response of viewer stream
 * @since 3.0
 */
public class StreamResponse implements IFixedSize, IRelayResponse {
    /**
     * id of the stream
     */
    public long id;
    /**
     * the id of the trace in ctf. Should be a uuid???
     */
    public long ctf_trace_id;
    /**
     * metadata flag, do we have more
     */
    public int metadata_flag;
    /**
     * the path
     */
    public byte path_name[] = new byte[LTTngViewerCommands.LTTNG_VIEWER_PATH_MAX];
    /**
     * The channel, traditionally channel0
     */
    public byte channel_name[] = new byte[LTTngViewerCommands.LTTNG_VIEWER_NAME_MAX];

    @Override
    public void populate(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.BIG_ENDIAN);
        id = bb.getLong();
        ctf_trace_id = bb.getLong();
        metadata_flag = bb.getInt();
        bb.get(path_name, 0, LTTngViewerCommands.LTTNG_VIEWER_PATH_MAX);
        bb.get(channel_name, 0, LTTngViewerCommands.LTTNG_VIEWER_NAME_MAX);
    }

    @Override
    public int size() {
        return (Long.SIZE + Long.SIZE + Integer.SIZE) / 8 + LTTngViewerCommands.LTTNG_VIEWER_PATH_MAX + LTTngViewerCommands.LTTNG_VIEWER_NAME_MAX;
    }
}