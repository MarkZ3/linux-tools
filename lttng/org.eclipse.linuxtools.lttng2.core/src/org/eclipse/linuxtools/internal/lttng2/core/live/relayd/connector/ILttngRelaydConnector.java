/**********************************************************************
 * Copyright (c) 2013-2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Matthew Khouzam - Initial implementation
 **********************************************************************/

package org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector;

import java.io.IOException;
import java.util.List;

import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.AttachSessionResponse;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.CreateSessionResponse;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.IndexResponse;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.SessionResponse;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.StreamResponse;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.TracePacketResponse;

/**
 * Connector for LTTng RelayD
 *
 * @author Matthew Khouzam
 * @since 3.0
 *
 */
public interface ILttngRelaydConnector {

    /**
     * Gets a session list
     * @return
     * @throws IOException
     */
    public List<SessionResponse> getSessions() throws IOException;

    public CreateSessionResponse createSession() throws IOException;

    public AttachSessionResponse attachToSession(SessionResponse lttng_viewer_session) throws IOException;

    public String getMetadata(AttachSessionResponse attachedSession) throws IOException;

    public IndexResponse getNextIndex(StreamResponse stream) throws IOException;

    public TracePacketResponse getNextPacket(StreamResponse stream) throws IOException;

    public TracePacketResponse getPacketFromStream(IndexResponse index, long id) throws IOException;

    public List<StreamResponse> getNewStreams() throws IOException;
}
