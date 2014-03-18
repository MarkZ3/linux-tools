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
 * VIEWER_LIST_SESSIONS payload.
 * @since 3.0
 */
public class ListSessionsResponse implements IRelayResponse,IFixedSize {
    /** number of sessions*/
    public int sessions_count;
    /** the list of sessions */
    public SessionResponse session_list[];

    @Override
    public void populate(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.BIG_ENDIAN);
        sessions_count = bb.getInt();
        session_list = new SessionResponse[sessions_count];
    }

    @Override
    public int size() {
        return 4 + ((session_list == null) ? 0 : (session_list.length * (new SessionResponse()).size()));
    }

}