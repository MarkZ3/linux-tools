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
 * Command received, needs a populate to fill the data
 * @since 3.0
 */
public interface IRelayResponse {
    /**
     * Populate the class from a byte array
     *
     * @param data
     *            the byte array containing the streamed command
     */
    public void populate(byte[] data);
}