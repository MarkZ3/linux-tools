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

package org.eclipse.linuxtools.internal.lttng2.core.live.relayd.connector.lttngviewerCommands;

/**
 * LTTng Relay Daemon API. needs a tcp connection, API is copied from BSD
 * licensed implementation here :
 * http://git.lttng.org/?p=lttng-tools.git;a=blob;
 * f=src/bin/lttng-relayd/lttng-viewer-abi.h
 *
 * @author Matthew Khouzam
 * @since 3.0
 */
public interface LTTngViewerCommands {

    /** Maximum path name length */
    final static int LTTNG_VIEWER_PATH_MAX = 4096;
    /** Maximum name length */
    final static int LTTNG_VIEWER_NAME_MAX = 255;
    /** Maximum host name length */
    final static int LTTNG_VIEWER_HOST_NAME_MAX = 64;
    /** New stream in the trace */
    public static final int NEW_STREAM = (1 << 1);
    /** New metadata in the trace */
    public static final int NEW_METADATA = (1 << 0);

}
