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
 * Get viewer session response to command
 *
 * @since 3.0
 */
public class SessionResponse implements IRelayResponse, IFixedSize {
    /** id of the session */
    public long id;
    /** live timer */
    public int live_timer;
    /** number of clients */
    public int clients;
    /** number streams */
    public int streams;
    /** Hostname, like 'localhost' */
    public byte hostname[] = new byte[LTTngViewerCommands.LTTNG_VIEWER_HOST_NAME_MAX];
    /** Session name, like 'streaming session'*/
    public byte session_name[] = new byte[LTTngViewerCommands.LTTNG_VIEWER_NAME_MAX];

    @Override
    public void populate(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.BIG_ENDIAN);
        id = bb.getLong();
        live_timer = bb.getInt();
        clients = bb.getInt();
        streams = bb.getInt();

        bb.get(hostname, 0, hostname.length);
        bb.get(session_name, 0, session_name.length);
    }

    @Override
    public int size() {
        return LTTngViewerCommands.LTTNG_VIEWER_HOST_NAME_MAX + LTTngViewerCommands.LTTNG_VIEWER_NAME_MAX + (Long.SIZE + Integer.SIZE + Integer.SIZE + Integer.SIZE) / 8;
    }

}