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
 * VIEWER_ATTACH_SESSION payload.
 * @since 3.0
 */
public class AttachSessionRequest implements IFixedSize, IRelayCommand {
    /** the id of a session */
    public long session_id;
    /** unused for now */
    public long offset;
    /** enum lttng_viewer_seek */
    public SeekCommand seek;

    @Override
    public int size() {
        return (Long.SIZE + Integer.SIZE + Integer.SIZE + Integer.SIZE) / 8;
    }

    @Override
    public byte[] getBytes() {
        byte data[] = new byte[size()];
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.putLong(session_id);
        bb.putLong(offset);
        bb.putInt(seek.getCommand());
        return data;
    }
}