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
 * VIEWER_GET_PACKET payload.
 * @since 3.0
 */
public class GetPacket implements IRelayCommand, IFixedSize {
    /** the stream Id*/
    public long stream_id;
    /** the offset */
    public long offset;
    /** the length of the packet */
    public int len;

    @Override
    public byte[] getBytes() {
        byte data[] = new byte[size()];
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.putLong(stream_id);
        bb.putLong(offset);
        bb.putInt(len);
        return data;
    }

    @Override
    public int size() {
        return (Long.SIZE + Long.SIZE + Integer.SIZE) / 8;
    }
}