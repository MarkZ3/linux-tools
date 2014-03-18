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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.ILttngRelaydConnector;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.AttachSessionRequest;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.AttachSessionResponse;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.Command;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.ConnectResponse;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.GetMetadata;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.GetNextIndex;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.GetPacket;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.GetPacketReturnCode;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.IndexResponse;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.ListSessionsResponse;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.MetadataPacketResponse;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.NewStreamsResponse;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.NewStreamsReturnCode;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.NextIndexReturnCode;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.SeekCommand;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.SessionResponse;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.StreamResponse;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.TracePacketResponse;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.ViewerCommand;

/**
 * Lttng 2.4 implementation
 *
 * @author Matthew Khouzam
 *
 */
public class LttngRelayDConnector_2_4 implements ILttngRelaydConnector {
    private final DataInputStream fInNet;
    private final DataOutputStream fOutNet;

    /**
     * Constructor needs two network streams
     *
     * @param inNet
     *            network incoming data
     * @param outNet
     *            network outgoing data
     */
    public LttngRelayDConnector_2_4(DataInputStream inNet, DataOutputStream outNet) {
        fInNet = inNet;
        fOutNet = outNet;
    }

    @Override
    public List<SessionResponse> getSessions() throws IOException {
        ViewerCommand listSessionsCmd = new ViewerCommand();
        listSessionsCmd.cmd = Command.VIEWER_LIST_SESSIONS;
        listSessionsCmd.data_size = 0;

        fOutNet.write(listSessionsCmd.getBytes());
        byte[] data = new byte[4096];
        fInNet.readFully(data, 0, 4);

        ListSessionsResponse listingResponse = new ListSessionsResponse();
        listingResponse.populate(data);

        List<SessionResponse> temp = new ArrayList<>();
        for (int i = 0; i < listingResponse.sessions_count; i++) {
            SessionResponse viewer = new SessionResponse();
            fInNet.readFully(data, 0, viewer.size());
            viewer.populate(data);
            temp.add(viewer);
        }

        return temp;
    }

    @Override
    public AttachSessionResponse attachToSession(SessionResponse lttng_viewer_session) throws IOException {
        ViewerCommand listSessionsCmd = new ViewerCommand();
        listSessionsCmd.cmd = Command.VIEWER_ATTACH_SESSION;
        fOutNet.write(listSessionsCmd.getBytes());

        AttachSessionRequest attachRequest = new AttachSessionRequest();
        attachRequest.session_id = lttng_viewer_session.id;
        attachRequest.seek = SeekCommand.VIEWER_SEEK_BEGINNING;
        fOutNet.write(attachRequest.getBytes());
        fOutNet.flush();

        byte[] data = new byte[8];
        fInNet.readFully(data, 0, 8);

        AttachSessionResponse attachResponse = new AttachSessionResponse();
        attachResponse.populate(data);

        List<StreamResponse> temp = new ArrayList<>();
        for (int i = 0; i < attachResponse.streams_count; i++) {
            StreamResponse stream = new StreamResponse();
            byte[] streamData = new byte[stream.size()];
            fInNet.readFully(streamData, 0, stream.size());
            stream.populate(streamData);
            temp.add(stream);
        }
        attachResponse.stream_list = temp.toArray(new StreamResponse[0]);

        return attachResponse;
    }

    @Override
    public String getMetadata(AttachSessionResponse attachedSession) throws IOException {

        for (StreamResponse stream : attachedSession.stream_list) {
            if (stream.metadata_flag == 1) {
                issueCommand(Command.VIEWER_GET_METADATA);

                GetMetadata metadataRequest = new GetMetadata();
                metadataRequest.stream_id = stream.id;
                fOutNet.write(metadataRequest.getBytes());
                fOutNet.flush();

                MetadataPacketResponse metaDataPacket = new MetadataPacketResponse();
                final int BUFFER_SIZE = 12;
                byte[] data = new byte[BUFFER_SIZE];
                fInNet.readFully(data, 0, BUFFER_SIZE);
                metaDataPacket.populate(data);

                metaDataPacket.data = new byte[(int) metaDataPacket.len];
                fInNet.readFully(metaDataPacket.data, 0, (int) metaDataPacket.len);
                String strMetadata = new String(metaDataPacket.data);
                return strMetadata;
            }
        }

        return null;
    }

    @Override
    public TracePacketResponse getPacketFromStream(IndexResponse index, long id) throws IOException {

        issueCommand(Command.VIEWER_GET_PACKET);

        GetPacket packetRequest = new GetPacket();
        packetRequest.len = (int) (index.packet_size / 8);
        packetRequest.offset = index.offset;
        packetRequest.stream_id = id;
        fOutNet.write(packetRequest.getBytes());
        fOutNet.flush();

        TracePacketResponse tracePacket = new TracePacketResponse();
        final int BUFFER_SIZE = 12;
        byte[] data = new byte[BUFFER_SIZE];
        fInNet.readFully(data, 0, BUFFER_SIZE);
        tracePacket.populate(data);

        if (tracePacket.status == GetPacketReturnCode.VIEWER_GET_PACKET_OK) {
            tracePacket.data = new byte[tracePacket.len];
            fInNet.readFully(tracePacket.data, 0, tracePacket.len);
            return tracePacket;
        }

        return null;
    }

    @Override
    public TracePacketResponse getNextPacket(StreamResponse stream) throws IOException {
        TracePacketResponse packet = null;

        issueCommand(Command.VIEWER_GET_NEXT_INDEX);

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
            packet = getPacketFromStream(indexReply, stream.id);
        }
        return packet;
    }

    @Override
    public List<StreamResponse> getNewStreams() throws IOException {

        Command viewerGetNewStreams = Command.VIEWER_GET_NEW_STREAMS;

        issueCommand(viewerGetNewStreams);

        NewStreamsResponse newStreamReply = new NewStreamsResponse();

        final int PACKET_SIZE = 8;

        byte[] data = new byte[PACKET_SIZE];
        fInNet.readFully(data, 0, PACKET_SIZE);
        newStreamReply.populate(data);

        if (newStreamReply.status.equals(NewStreamsReturnCode.LTTNG_VIEWER_NEW_STREAMS_OK)) {
            newStreamReply.stream_list = new StreamResponse[newStreamReply.streams_count];
            for (int i = 0; i < newStreamReply.streams_count; i++) {
                newStreamReply.stream_list[i] = new StreamResponse();
                byte[] streamData = new byte[newStreamReply.stream_list[i].size()];
                fInNet.readFully(streamData, 0, newStreamReply.stream_list[i].size());
                newStreamReply.stream_list[i].populate(streamData);
            }
        }

        return Arrays.asList(newStreamReply.stream_list);
    }

    private void issueCommand(Command command) throws IOException {
        ConnectResponse connectPayload = new ConnectResponse();
        ViewerCommand connectCommand = new ViewerCommand();
        connectCommand.cmd = command;
        connectCommand.data_size = connectPayload.size();
        fOutNet.write(connectCommand.getBytes());
        fOutNet.flush();
    }

}
