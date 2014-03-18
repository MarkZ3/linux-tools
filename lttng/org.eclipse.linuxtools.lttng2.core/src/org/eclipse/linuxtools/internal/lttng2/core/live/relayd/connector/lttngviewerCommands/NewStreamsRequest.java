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
 * LTTNG_VIEWER_GET_NEW_STREAMS payload.
 * @since 3.0
 */
public class NewStreamsRequest implements IFixedSize, IRelayCommand {
    /** session ID */
    long session_id;

    @Override
    public byte[] getBytes() {
        byte data[] = new byte[size()];
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.putLong(session_id);
        return data;
    }

    @Override
    public int size() {
        return Long.SIZE / 8;
    }
}