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
 * The LTTng command
 * @since 3.0
 */
public class ViewerCommand implements IFixedSize, IRelayCommand {
    /**
     * data size following this header, you normally attach a payload that
     * one, in bytes
     */
    public long data_size;
    /** enum lttcomm_relayd_command */
    public Command cmd;
    /** command version */
    public int cmd_version;

    @Override
    public byte[] getBytes() {
        byte data[] = new byte[size()];
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.putLong(data_size);
        bb.putInt(cmd.getCommand());
        bb.putInt(cmd_version);
        return data;
    }

    @Override
    public int size() {
        return (Long.SIZE + Integer.SIZE + Integer.SIZE) / 8;
    }
}