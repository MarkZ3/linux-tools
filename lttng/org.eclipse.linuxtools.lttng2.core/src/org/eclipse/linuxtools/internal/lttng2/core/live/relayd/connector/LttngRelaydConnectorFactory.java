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

package org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.impl.LttngRelayDConnector_2_4;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.impl.LttngRelayDConnector_NOTSUPPORTED;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.ViewerCommand;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.Command;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.ConnectResponse;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.ConnectionType;

/**
 * LTTng RelayD connector factory
 *
 * @author Matthew Khouzam
 * @since 3.0
 */
public abstract class LttngRelaydConnectorFactory {

    /**
     * Create a connection to a relayd
     *
     * @param myConnection
     *            a connection to the relayd
     *
     * @return A relayd connector or Null
     * @throws IOException
     *             caused by invalid sockets
     */
    public static ILttngRelaydConnector connect(Socket myConnection) throws IOException {
        DataOutputStream outNet;
        DataInputStream inNet;

        inNet = new DataInputStream(myConnection.getInputStream());
        outNet = new DataOutputStream(myConnection.getOutputStream());

        ViewerCommand connectCommand = new ViewerCommand();
        ConnectResponse connectPayload = new ConnectResponse();
        connectCommand.cmd = Command.VIEWER_CONNECT;
        connectCommand.data_size = connectPayload.size();

        outNet.write(connectCommand.getBytes());
        outNet.flush();

        connectPayload.major = 2;
        connectPayload.minor = 4;
        connectPayload.type = ConnectionType.VIEWER_CLIENT_COMMAND;
        connectPayload.viewer_session_id = 0;
        outNet.write(connectPayload.getBytes());
        outNet.flush();

        byte data[] = new byte[connectPayload.size()];
        inNet.readFully(data, 0, connectPayload.size());
        connectPayload.populate(data);
        switch (connectPayload.major) {
        case 1:
            return new LttngRelayDConnector_NOTSUPPORTED();
        case 2:
            switch (connectPayload.minor) {
            case 0:
            case 1:
            case 2:
            case 3:
                return new LttngRelayDConnector_NOTSUPPORTED();
            case 4:
            default:
                return new LttngRelayDConnector_2_4(inNet, outNet);
            }
        default:
            break;
        }
        return null;
    }
}
