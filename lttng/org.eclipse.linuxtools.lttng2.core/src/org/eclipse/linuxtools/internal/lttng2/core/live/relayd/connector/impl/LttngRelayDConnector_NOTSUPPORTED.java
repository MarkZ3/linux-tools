/**********************************************************************
 * Copyright (c) 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Matthew Khouzam - Initial implementation
 **********************************************************************/

package org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.impl;

import java.io.IOException;
import java.util.List;

import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.ILttngRelaydConnector;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.AttachSessionResponse;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.IndexResponse;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.SessionResponse;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.StreamResponse;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.TracePacketResponse;

/**
 * Unsupported version of the relay daemon
 *
 * @author Matthew Khouzam
 *
 */
public class LttngRelayDConnector_NOTSUPPORTED implements ILttngRelaydConnector {

    @Override
    public List<SessionResponse> getSessions() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public AttachSessionResponse attachToSession(SessionResponse lttng_viewer_session) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getMetadata(AttachSessionResponse attachedSession) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public TracePacketResponse getNextPacket(StreamResponse stream) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public TracePacketResponse getPacketFromStream(IndexResponse index, long id) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<StreamResponse> getNewStreams() throws IOException {
        throw new UnsupportedOperationException();
    }

}
