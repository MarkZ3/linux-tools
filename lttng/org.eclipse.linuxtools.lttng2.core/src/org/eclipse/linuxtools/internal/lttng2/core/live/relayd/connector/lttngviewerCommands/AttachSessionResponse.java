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
 * Attach session response
 * @since 3.0
 */
public class AttachSessionResponse implements IRelayResponse {
    /** enum lttng_viewer_attach_return_code */
    public AttachReturnCode status;
    /** how many streams are there */
    public int streams_count;
    /** public class lttng_viewer_stream */
    public StreamResponse stream_list[];

    @Override
    public void populate(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.BIG_ENDIAN);
        status = AttachReturnCode.values()[bb.getInt() - 1];
        streams_count = bb.getInt();
    }
}