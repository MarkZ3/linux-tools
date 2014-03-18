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
 * Response to getpacket command
 *
 * @since 3.0
 */
public class TracePacketResponse implements IRelayResponse {
    /** Enum lttng_viewer_get_packet_return_code */
    public GetPacketReturnCode status;
    /** length of the packet */
    public int len;
    /** flags: is there new metadata or new streams? */
    public int flags;
    /** the packet */
    public byte[] data;

    @Override
    public void populate(byte[] input) {
        ByteBuffer bb = ByteBuffer.wrap(input);
        bb.order(ByteOrder.BIG_ENDIAN);
        status = GetPacketReturnCode.values()[bb.getInt() - 1];
        len = bb.getInt();
        flags = bb.getInt();
    }
}