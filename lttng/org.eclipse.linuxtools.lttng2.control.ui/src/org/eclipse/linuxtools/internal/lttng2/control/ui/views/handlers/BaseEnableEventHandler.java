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
package org.eclipse.linuxtools.internal.lttng2.control.ui.views.handlers;

import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.window.Window;
import org.eclipse.linuxtools.internal.lttng2.control.core.model.LogLevelType;
import org.eclipse.linuxtools.internal.lttng2.control.core.model.TraceLogLevel;
import org.eclipse.linuxtools.internal.lttng2.control.ui.views.dialogs.EnableEventsDialog;
import org.eclipse.linuxtools.internal.lttng2.control.ui.views.dialogs.TraceControlDialogFactory;
import org.eclipse.linuxtools.internal.lttng2.control.ui.views.model.impl.TraceDomainComponent;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * <p>
 * Base command handler implementation to enable events.
 * </p>
 *
 * @author Bernd Hufmann
 */
public abstract class BaseEnableEventHandler extends BaseControlViewHandler {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------
    /**
     * The command execution parameter.
     */
    protected CommandParameter fParam = null;

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------

    /**
     * Enables a list of events for given parameters.
     *
     * @param param
     *            - a parameter instance with data for the command execution
     * @param eventNames
     *            - list of event names
     * @param isKernel
     *            - true if kernel domain else false
     * @param filterExpression
     *            - a filter expression
     * @param monitor
     *            - a progress monitor
     * @throws ExecutionException
     *             If the command fails for some reason
     */
    public abstract void enableEvents(CommandParameter param, List<String> eventNames, boolean isKernel, String filterExpression, IProgressMonitor monitor) throws ExecutionException;

    /**
     * Enables all syscall events.
     *
     * @param param
     *            - a parameter instance with data for the command execution
     * @param monitor
     *            - a progress monitor
     * @throws ExecutionException
     *             If the command fails for some reason
     */
    public abstract void enableSyscalls(CommandParameter param, IProgressMonitor monitor) throws ExecutionException;

    /**
     * Enables a dynamic probe.
     *
     * @param param
     *            - a parameter instance with data for the command execution
     * @param eventName
     *            - a event name
     * @param isFunction
     *            - true for dynamic function entry/return probe else false
     * @param probe
     *            - a dynamic probe information
     * @param monitor
     *            - a progress monitor
     * @throws ExecutionException
     *             If the command fails for some reason
     */
    public abstract void enableProbe(CommandParameter param, String eventName, boolean isFunction, String probe, IProgressMonitor monitor) throws ExecutionException;

    /**
     * Enables events using log level
     *
     * @param param
     *            - a parameter instance with data for the command execution
     * @param eventName
     *            - a event name
     * @param logLevelType
     *            - a log level type
     * @param level
     *            - a log level
     * @param filterExpression
     *            - a filter expression
     * @param monitor
     *            - a progress monitor
     * @throws ExecutionException
     *             If the command fails for some reason
     */
    public abstract void enableLogLevel(CommandParameter param, String eventName, LogLevelType logLevelType, TraceLogLevel level, String filterExpression, IProgressMonitor monitor) throws ExecutionException;

    /**
     * @param param
     *            - a parameter instance with data for the command execution
     * @return returns the relevant domain (null if domain is not known)
     */
    public abstract TraceDomainComponent getDomain(CommandParameter param);

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

        if (window == null) {
            return false;
        }
        fLock.lock();
        try {
            final EnableEventsDialog dialog = TraceControlDialogFactory.getInstance().getEnableEventsDialog();

            if (dialog.open() != Window.OK) {
                return null;
            }
        } finally {
            fLock.unlock();
        }
        return null;
    }
}
