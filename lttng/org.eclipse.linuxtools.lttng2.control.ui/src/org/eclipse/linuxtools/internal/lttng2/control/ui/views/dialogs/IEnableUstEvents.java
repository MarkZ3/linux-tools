/**********************************************************************
 * Copyright (c) 2012, 2013 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Bernd Hufmann - Initial API and implementation
 *   Bernd Hufmann - Updated for support of LTTng Tools 2.1
 **********************************************************************/
package org.eclipse.linuxtools.internal.lttng2.control.ui.views.dialogs;

import java.util.List;

/**
 * <p>
 * Interface for providing information about UST events to be enabled.
 * </p>
 *
 * @author Bernd Hufmann
 */
public interface IEnableUstEvents {

    // ------------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------------

    /**
     * @return a flag whether the tracepoints shall be configured.
     */
    boolean isTracepoints();

    /**
     * @return a flag indicating whether all tracepoints shall be enabled or not.
     */
    boolean isAllTracePoints();

    /**
     * @return a list of event names to be enabled.
     */
    List<String> getEventNames();


}
