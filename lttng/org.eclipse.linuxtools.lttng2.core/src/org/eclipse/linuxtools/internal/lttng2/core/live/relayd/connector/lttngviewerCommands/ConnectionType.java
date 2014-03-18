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

/**
 * get viewer connection type
 * @since 3.0
 */
public enum ConnectionType implements IBaseCommand {
    /** Command sent */
    VIEWER_CLIENT_COMMAND(1),
    /** Notification sent */
    VIEWER_CLIENT_NOTIFICATION(2);
    private final int code;

    private ConnectionType(int c) {
        code = c;
    }

    @Override
    public int getCommand() {
        return code;
    }

}