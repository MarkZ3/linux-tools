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
 * the index
 * @since 3.0
 */
public class IndexResponse implements IRelayResponse, IFixedSize {
    /** the offset */
    public long offset;
    /** packet_size */
    public long packet_size;
    /** the content size - how much of the packet is used*/
    public long content_size;
    /** timestamp of the beginning of the packet */
    public long timestamp_begin;
    /** timestamp of the end of the packet */
    public long timestamp_end;
    /** number of discarded events BEFORE this packet */
    public long events_discarded;
    /** the stream id */
    public long stream_id;
    /** the status */
    public NextIndexReturnCode status;
    /** whether there are new streams or metadata */
    public int flags;

    @Override
    public void populate(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.BIG_ENDIAN);
        offset = bb.getLong();
        packet_size = bb.getLong();
        content_size = bb.getLong();
        timestamp_begin = bb.getLong();
        timestamp_end = bb.getLong();
        events_discarded = bb.getLong();
        stream_id = bb.getLong();

        status = NextIndexReturnCode.values()[bb.getInt() - 1];
        flags = bb.getInt();
    }

    @Override
    public int size() {
        return (Long.SIZE * 7 + Integer.SIZE * 2) / 8;
    }
}