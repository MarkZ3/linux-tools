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
 * CONNECT payload.
 * @since 3.0
 */
public class ConnectResponse implements IRelayResponse, IFixedSize, IRelayCommand {
    /** session id, counts from 1 and increments by session */
    public long viewer_session_id;
    /**
     * Major version, hint, it's 2
     */
    public int major;
    /**
     * Minor version, hint, it's 4
     */
    public int minor;
    /**
     * type of connect to {@link ConnectionType}
     */
    public ConnectionType type;

    @Override
    public int size() {
        return (Long.SIZE + Integer.SIZE + Integer.SIZE + Integer.SIZE) / 8;
    }

    @Override
    public void populate(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.BIG_ENDIAN);
        viewer_session_id = bb.getLong();
        major = bb.getInt();
        minor = bb.getInt();
        bb.getInt(); // Should not be used, see
                     // http://bugs.lttng.org/issues/728
    }

    @Override
    public byte[] getBytes() {
        byte data[] = new byte[size()];
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.putLong(viewer_session_id);
        bb.putInt(major);
        bb.putInt(minor);
        bb.putInt(type.getCommand());
        return data;
    }
}