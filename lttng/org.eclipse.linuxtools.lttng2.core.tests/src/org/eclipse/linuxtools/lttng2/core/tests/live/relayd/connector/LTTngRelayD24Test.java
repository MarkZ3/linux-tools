/**********************************************************************
 * Copyright (c) 2013 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Matthew Khouzam - Initial implementation
 **********************************************************************/
package org.eclipse.linuxtools.lttng2.core.tests.live.relayd.connector;

import static org.junit.Assert.assertNotNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;

import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.ILttngRelaydConnector;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.LttngRelaydConnectorFactory;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.AttachSessionResponse;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.Command;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.ConnectResponse;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.GetNextIndex;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.IndexResponse;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.NextIndexReturnCode;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.SessionResponse;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.StreamResponse;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.ViewerCommand;
import org.junit.Test;

/**
 * Unit tests for lttng-relayd. It actually allows us to test the API.
 *
 * @author Matthew Khouzam
 *
 */
public class LTTngRelayD24Test {

    static final String address = "127.0.0.1"; // change me //$NON-NLS-1$
    static final int port = 5433;



    private static void getPackets(AttachSessionResponse attachedSession, Socket connection, ILttngRelaydConnector relayD ) throws IOException {
        int numPacketsReceived = 0;
        DataOutputStream fOutNet = new DataOutputStream(connection.getOutputStream());
        DataInputStream fInNet = new DataInputStream(connection.getInputStream());
        while (numPacketsReceived < 100) {
            for (StreamResponse stream : attachedSession.stream_list) {
                if (stream.metadata_flag != 1) {
                    ViewerCommand connectCommand = new ViewerCommand();
                    ConnectResponse connectPayload = new ConnectResponse();
                    connectCommand.cmd = Command.VIEWER_GET_NEXT_INDEX;
                    connectCommand.data_size = connectPayload.size();
                    fOutNet.write(connectCommand.getBytes());
                    fOutNet.flush();

                    GetNextIndex indexRequest = new GetNextIndex();
                    indexRequest.stream_id = stream.id;
                    fOutNet.write(indexRequest.getBytes());
                    fOutNet.flush();

                    IndexResponse indexReply = new IndexResponse();
                    byte[] data = new byte[indexReply.size()];
                    fInNet.readFully(data, 0, indexReply.size());
                    indexReply.populate(data);

                    // Nothing else supported for now

                    if (indexReply.status == NextIndexReturnCode.VIEWER_INDEX_OK) {
                        if (relayD.getPacketFromStream(indexReply, stream.id) != null) {
                            numPacketsReceived++;
                        }
                    }
                }
            }
        }
    }


    /**
     * Test a connection
     *
     * @throws IOException
     *             network timeout?
     */
    @Test
    public void testViewerConnection() throws IOException {
        InetAddress addr = InetAddress.getByName(address);
        try (Socket connection = new Socket(addr, port)) {
            ILttngRelaydConnector relayD = LttngRelaydConnectorFactory.connect(connection);
            List<SessionResponse> sessions = relayD.getSessions();
            SessionResponse lttng_viewer_session = sessions.get(0);
            AttachSessionResponse attachedSession = relayD.attachToSession(lttng_viewer_session);
            String metaData = relayD.getMetadata(attachedSession);
            assertNotNull(metaData);

            getPackets(attachedSession,connection, relayD);
        }
    }
}
