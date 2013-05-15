/*******************************************************************************
 * Copyright (c) 2005, 2012 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     QNX - Initial API and implementation
 *******************************************************************************/
package org.eclipse.linuxtools.tmf.core.trace.index;

import java.io.IOException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.linuxtools.internal.tmf.core.Activator;

/**
 * @author Doug Schaefer
 */
@SuppressWarnings("javadoc")
public class DBStatus extends Status {
	/**
	 * @param exception
	 */
	public DBStatus(IOException exception) {
		super(IStatus.ERROR, Activator.PLUGIN_ID, 0, "IOException", exception); //$NON-NLS-1$
	}

	public DBStatus(String msg) {
		super(IStatus.ERROR, Activator.PLUGIN_ID, 0, "Error", null); //$NON-NLS-1$
	}
}
