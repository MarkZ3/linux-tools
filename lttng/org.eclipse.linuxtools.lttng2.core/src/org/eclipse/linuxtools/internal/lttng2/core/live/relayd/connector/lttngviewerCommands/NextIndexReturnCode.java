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
 * get next index return code (hope it's viewer_index_ok)
 * @since 3.0
 */
public enum NextIndexReturnCode implements IBaseCommand {
    /** Index is available. */
    VIEWER_INDEX_OK(1),
    /** Index not yet available. */
    VIEWER_INDEX_RETRY(2),
    /** Index closed (trace destroyed). */
    VIEWER_INDEX_HUP(3),
    /** Unknown error. */
    VIEWER_INDEX_ERR(4),
    /** Inactive stream beacon. */
    VIEWER_INDEX_INACTIVE(5),
    /** End of index file. */
    VIEWER_INDEX_EOF(6);

    private final int code;

    private NextIndexReturnCode(int c) {
        code = c;
    }

    @Override
    public int getCommand() {
        return code;
    }
}