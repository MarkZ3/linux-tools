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
 * Metadata packet
 * @since 3.0
 */
public class MetadataPacketResponse implements IRelayResponse {
    /** length of the packet */
    public long len;
    /** status of the metadata */
    public GetMetadataReturnCode status;
    /** the packet */
    public byte data[];

    @Override
    public void populate(byte[] input) {
        ByteBuffer bb = ByteBuffer.wrap(input);
        bb.order(ByteOrder.BIG_ENDIAN);
        len = bb.getLong();
        status = GetMetadataReturnCode.values()[bb.getInt() - 1];
    }
}