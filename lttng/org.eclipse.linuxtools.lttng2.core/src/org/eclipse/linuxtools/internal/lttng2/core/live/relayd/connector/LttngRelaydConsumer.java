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
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.linuxtools.ctf.core.trace.CTFReaderException;
import org.eclipse.linuxtools.ctf.core.trace.CTFTrace;
import org.eclipse.linuxtools.ctf.core.trace.Metadata;
import org.eclipse.linuxtools.ctf.core.trace.Stream;
import org.eclipse.linuxtools.ctf.core.trace.StreamInput;
import org.eclipse.linuxtools.internal.lttng2.core.Activator;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.AttachReturnCode;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.AttachSessionResponse;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.CreateSessionResponse;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.CreateSessionReturnCode;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.GetPacketReturnCode;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.IndexResponse;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.LTTngViewerCommands;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.NextIndexReturnCode;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.SessionResponse;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.StreamResponse;
import org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands.TracePacketResponse;
import org.eclipse.linuxtools.tmf.core.project.model.TmfTraceType;
import org.eclipse.linuxtools.tmf.core.project.model.TraceTypeHelper;
import org.eclipse.linuxtools.tmf.core.signal.TmfSignalHandler;
import org.eclipse.linuxtools.tmf.core.signal.TmfSignalManager;
import org.eclipse.linuxtools.tmf.core.signal.TmfSignalThrottler;
import org.eclipse.linuxtools.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.linuxtools.tmf.core.signal.TmfTraceRangeUpdatedSignal;
import org.eclipse.linuxtools.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.linuxtools.tmf.ctf.core.CtfTmfTimestamp;
import org.eclipse.linuxtools.tmf.ctf.core.CtfTmfTrace;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfOpenTraceHelper;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfProjectElement;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfProjectRegistry;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfTraceElement;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfTraceFolder;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfTraceTypeUIUtils;

/**
 * Consumer of the relay d
 *
 * @author Matthew Khouzam
 * @since 3.0
 */
public class LttngRelaydConsumer {

    private static final String ENCODING_UTF_8 = "UTF-8"; //$NON-NLS-1$
    private static final String LTTNG_KERNEL_TRACE_TYPE = "org.eclipse.linuxtools.lttng2.kernel.tracetype"; //$NON-NLS-1$
    private static final String GENERIC_CTF_TRACE_TYPE = "org.eclipse.linuxtools.tmf.ui.type.ctf"; //$NON-NLS-1$
    private Job fConsumerJob;
    private final String fAddress;
    private final int fPort;
    private final String fSessionName;
    private CtfTmfTrace fCtfTmfTrace;
    private CTFTrace fCtfTrace;
    private Map<Long, File> fStreams;
    private boolean fInitialized;
    protected IProject fProject;
    private long fTimestampEnd;
    protected TmfSignalThrottler fSignalThrottler;

    /**
     * Start a lttng consumer
     *
     * @param address
     *            the ip address in string format
     * @param port
     *            the port, an integer
     * @param sessionName
     *            the session name
     * @param ctfTrace
     *            the parent trace
     * @param project
     */
    public LttngRelaydConsumer(String address, final int port, final String sessionName, IProject project) {
        fAddress = address;
        fPort = port;
        fSessionName = sessionName;
        fStreams = new TreeMap<>();
        fInitialized = false;
        fProject = project;
        fTimestampEnd = 0;

        TmfSignalManager.register(this);

        fConsumerJob = new Job("RelayD consumer") { //$NON-NLS-1$

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try (Socket connection = new Socket(fAddress, fPort)) {

                    ILttngRelaydConnector relayd = LttngRelaydConnectorFactory.connect(connection);
                    List<SessionResponse> sessions = relayd.getSessions();
                    SessionResponse selectedSession = null;
                    for (SessionResponse session : sessions) {
                        String asessionName = nullTerminatedByteArrayToString(session.session_name);

                        //System.out.println(asessionName);
                        if (asessionName.equals(fSessionName)) {
                            selectedSession = session;
                            break;
                        }
                    }

                    if (selectedSession == null) {
                        Activator.getDefault().logError("Error, live session not found"); //$NON-NLS-1$
                        return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Error, live session not found");
                    }

                    CreateSessionResponse createSession = relayd.createSession();
                    if (createSession.status != CreateSessionReturnCode.LTTNG_VIEWER_CREATE_SESSION_OK) {
                        return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Error, could not create viewer session, error code: " + createSession.status.toString());
                    }
                    //createSession

                    AttachSessionResponse attachedSession = relayd.attachToSession(selectedSession);
                    if (attachedSession.status != AttachReturnCode.VIEWER_ATTACH_OK) {
                        return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Error, could not attach to session, error code: " + attachedSession.status.toString());
                    }

                    if (!monitor.isCanceled()) {
                        String metadata = relayd.getMetadata(attachedSession);
                        if (metadata == null) {
                            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Error, trace has no metadata.");
                        }

                        //System.out.println("metadata: " + metadata);
                    }

                    while (!monitor.isCanceled()) {

                        List<StreamResponse> attachedStreams = Arrays.asList(attachedSession.stream_list);
                        boolean active = true;
                        for (StreamResponse stream : attachedStreams) {
                            if (!fInitialized) {
                                initializeTraceResource(stream);
                                fCtfTrace = fCtfTmfTrace.getCTFTrace();
                                fSignalThrottler = new TmfSignalThrottler(fCtfTmfTrace, 5000);
                                fInitialized = true;
                            }

                            if (stream.metadata_flag != 1) {
                                TracePacketResponse packet = null;
                                IndexResponse indexReply = relayd.getNextIndex(stream);
                                if (indexReply.status == NextIndexReturnCode.VIEWER_INDEX_OK) {
                                    packet = relayd.getPacketFromStream(indexReply, stream.id);
                                    long nanoTimeStamp = fCtfTrace.timestampCyclesToNanos(indexReply.timestamp_end);
                                    if (nanoTimeStamp > fTimestampEnd) {
                                        CtfTmfTimestamp endTime = new CtfTmfTimestamp(nanoTimeStamp);
                                        TmfTimeRange range = new TmfTimeRange(fCtfTmfTrace.getStartTime(), endTime);
                                        TmfTraceRangeUpdatedSignal signal = new TmfTraceRangeUpdatedSignal(LttngRelaydConsumer.this, fCtfTmfTrace, range);
                                        ITmfTimestamp delta = endTime.getDelta(new CtfTmfTimestamp(fTimestampEnd));
                                        if (delta.getValue() > 10000000) {
                                            System.out.println("new range: " + range);
//                                          fSignalThrottler.queue(signal);
                                          fCtfTmfTrace.broadcastAsync(signal);
                                        }
                                        //- new CtfTmfTimestamp(nanoTimeStamp);
                                        fTimestampEnd = nanoTimeStamp;
                                    }
                                    active = true;
                                } else if (indexReply.status == NextIndexReturnCode.VIEWER_INDEX_HUP) {
                                    fCtfTmfTrace.setLive(false);
                                    TmfTraceRangeUpdatedSignal signal = new TmfTraceRangeUpdatedSignal(LttngRelaydConsumer.this, fCtfTmfTrace, new TmfTimeRange(fCtfTmfTrace.getStartTime(), new CtfTmfTimestamp(fTimestampEnd)));
                                    fCtfTmfTrace.broadcastAsync(signal);
                                    fSignalThrottler.dispose();
                                    return Status.OK_STATUS;
                                } else if (indexReply.status != NextIndexReturnCode.VIEWER_INDEX_RETRY) {
                                    //System.out.println(indexReply.status);
                                    active = true;
                                }

                                //TracePacketResponse packet = relayd.getNextPacket(stream);
                                if (packet != null && packet.status == GetPacketReturnCode.VIEWER_GET_PACKET_OK) {
                                    // more streams
                                    if ((packet.flags & LTTngViewerCommands.NEW_STREAM) == LTTngViewerCommands.NEW_STREAM) {
                                        List<StreamResponse> newStreams = relayd.getNewStreams();
                                        for (StreamResponse streamToAdd : newStreams) {

                                            File f = new File(fCtfTmfTrace.getPath() + File.separator + streamToAdd.path_name + streamToAdd.channel_name);
                                            // touch the file
                                            f.setLastModified(System.currentTimeMillis());
                                            fStreams.put(Long.valueOf(streamToAdd.id), f);
                                            fCtfTrace.addStream(streamToAdd.id, f);

                                        }

                                    }
                                    // more metadata
                                    if ((packet.flags & LTTngViewerCommands.NEW_METADATA) == LTTngViewerCommands.NEW_METADATA) {
                                        String metaData = relayd.getMetadata(attachedSession);
                                        (new Metadata(fCtfTrace)).parseTextFragment(metaData);
                                    }

                                    // try (FileOutputStream fos = new
                                    // FileOutputStream(fStreams.get(stream.id),
                                    // true)) {
                                    // fos.write(packet.data);
                                    // monitor.worked(1);
                                    // }
                                }
                            }
                        }

                        if (!active) {
                            TmfTraceRangeUpdatedSignal signal = new TmfTraceRangeUpdatedSignal(LttngRelaydConsumer.this, fCtfTmfTrace, new TmfTimeRange(new CtfTmfTimestamp(fTimestampEnd), new CtfTmfTimestamp(fTimestampEnd)));
                            fCtfTmfTrace.broadcastAsync(signal);
                        }
                    }

                } catch (IOException | CTFReaderException | CoreException e) {
                    Activator.getDefault().logError("Error during live trace reading", e); //$NON-NLS-1$
                    fSignalThrottler.dispose();
                    return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Error during live trace reading." + e.getMessage());
                }

                fSignalThrottler.dispose();
                return Status.OK_STATUS;
            }
        };
        fConsumerJob.setSystem(true);
        fConsumerJob.schedule();
    }

    private static String nullTerminatedByteArrayToString(byte[] byteArray) throws UnsupportedEncodingException {
        // Find length of null terminated string
        int length = 0;
        for (; length < byteArray.length && byteArray[length] != 0; length++) {
        }

        String asessionName = new String(byteArray, 0, length, ENCODING_UTF_8);
        return asessionName;
    }

    private void initializeTraceResource(StreamResponse stream) throws UnsupportedEncodingException, CoreException {
        String pathName = nullTerminatedByteArrayToString(stream.path_name);
        //System.out.println(pathName);
        IFolder folder = fProject.getFolder(TmfTraceFolder.TRACE_FOLDER_NAME);
        IFolder traceFolder = folder.getFolder(fSessionName);
        Path location = new Path(pathName);
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IStatus result = workspace.validateLinkLocation(folder, location);
        if (result.isOK()) {
            traceFolder.createLink(location, IResource.REPLACE, new NullProgressMonitor());
        } else {
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, result.getMessage()));
        }

        TraceTypeHelper traceTypeToSet = null;
        if (new Path(pathName).lastSegment().equals("kernel")) {
            traceTypeToSet = TmfTraceType.getInstance().getTraceType(LTTNG_KERNEL_TRACE_TYPE);
        } else {
            traceTypeToSet = TmfTraceType.getInstance().getTraceType(GENERIC_CTF_TRACE_TYPE);
        }
        IStatus ret = TmfTraceTypeUIUtils.setTraceType(traceFolder, traceTypeToSet);
        if (ret.isOK()) {
            ret = TmfOpenTraceHelper.openTraceFromProject(fProject.getName(), fSessionName);
        }

        final TmfProjectElement projectElement = TmfProjectRegistry.getProject(fProject, true);
        final TmfTraceFolder tracesFolder = projectElement.getTracesFolder();
        final List<TmfTraceElement> traces = tracesFolder.getTraces();
        TmfTraceElement found = null;
        for (TmfTraceElement candidate : traces) {
            if (candidate.getName().equals(fSessionName)) {
                found = candidate;
            }
        }

        if (found == null) {
            throw new IllegalStateException("Could not find CtfTmfTrace");
        }

        while (fCtfTmfTrace == null) {
            fCtfTmfTrace = (CtfTmfTrace) found.getTrace();
            try {
                Thread.sleep(10);
                //System.out.println("sleeping");
            } catch (InterruptedException e) {
                throw new IllegalStateException("Could not find CtfTmfTrace");
            }
        }

        fCtfTmfTrace.setLive(true);

        for (Stream s : fCtfTmfTrace.getCTFTrace().getStreams()) {
            for (StreamInput si : s.getStreamInputs()) {
                fStreams.put(si.getStream().getId(), new File(si.getPath()));
            }
        }
    }

    /**
     * Listen to trace closed so that we can stop the relayd job
     *
     * @param signal
     */
    @TmfSignalHandler
    public void traceOpened(TmfTraceClosedSignal signal) {
        if (signal.getTrace() == fCtfTmfTrace) {
            fConsumerJob.cancel();
        }
    }

}