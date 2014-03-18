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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.linuxtools.ctf.core.trace.CTFReaderException;
import org.eclipse.linuxtools.ctf.core.trace.Metadata;
import org.eclipse.linuxtools.ctf.core.trace.Stream;
import org.eclipse.linuxtools.ctf.core.trace.StreamInput;
import org.eclipse.linuxtools.internal.lttng2.core.Activator;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.AttachSessionResponse;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.LTTngViewerCommands;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.SessionResponse;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.StreamResponse;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.TracePacketResponse;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfTrace;

/**
 * Consumer of the relay d
 *
 * @author Matthew Khouzam
 * @since 3.0
 */
public class LttngRelaydConsumer {

    private Job fConsumerJob;
    private final String fAddress;
    private final int fPort;
    private final int fSession;
    private final CtfTmfTrace fCtfTrace;
    private Map<Long, File> fStreams;

    /**
     * Start a lttng consumer
     *
     * @param address
     *            the ip address in string format
     * @param port
     *            the port, an integer
     * @param session
     *            the session id
     * @param ctfTrace
     *            the parent trace
     */
    public LttngRelaydConsumer(String address, final int port, final int session, final CtfTmfTrace ctfTrace) {
        fAddress = address;
        fPort = port;
        fSession = session;
        fCtfTrace = ctfTrace;
        fStreams = new TreeMap<>();
        for (Stream s : fCtfTrace.getCTFTrace().getStreams()) {
            for (StreamInput si : s.getStreamInputs()) {
                fStreams.put(si.getStream().getId(), new File(si.getPath()));
            }
        }

        fConsumerJob = new Job("RelayD consumer") { //$NON-NLS-1$

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try (Socket connection = new Socket(fAddress, fPort)) {

                    ILttngRelaydConnector relayd = LttngRelaydConnectorFactory.connect(connection);
                    List<SessionResponse> sessions = relayd.getSessions();
                    AttachSessionResponse attachedSession = relayd.attachToSession(sessions.get(fSession));

                    while (!monitor.isCanceled()) {

                        List<StreamResponse> attachedStreams = Arrays.asList(attachedSession.stream_list);
                        for (StreamResponse stream : attachedStreams) {

                            TracePacketResponse packet = relayd.getNextPacket(stream);
                            // more streams
                            if ((packet.flags & LTTngViewerCommands.NEW_STREAM) == LTTngViewerCommands.NEW_STREAM) {
                                List<StreamResponse> newStreams = relayd.getNewStreams();
                                for (StreamResponse streamToAdd : newStreams) {

                                    File f = new File(fCtfTrace.getPath() + File.separator + streamToAdd.path_name + streamToAdd.channel_name);
                                    // touch the file
                                    f.setLastModified(System.currentTimeMillis());
                                    fStreams.put(Long.valueOf(streamToAdd.id), f);
                                    fCtfTrace.getCTFTrace().addStream(streamToAdd.id, f);

                                }

                            }
                            // more metadata
                            if ((packet.flags & LTTngViewerCommands.NEW_METADATA) == LTTngViewerCommands.NEW_METADATA) {

                                String metaData = relayd.getMetadata(attachedSession);
                                (new Metadata(ctfTrace.getCTFTrace())).parseTextFragment(metaData);
                            }

                            try (FileOutputStream fos = new FileOutputStream(fStreams.get(stream.id), true)) {
                                fos.write(packet.data);
                                monitor.worked(1);
                            }
                        }

                    }

                } catch (IOException | CTFReaderException e) {
                    Activator.getDefault().logError("Error during live trace reading", e); //$NON-NLS-1$
                }
                return null;
            }
        };
        fConsumerJob.schedule();
    }

}
