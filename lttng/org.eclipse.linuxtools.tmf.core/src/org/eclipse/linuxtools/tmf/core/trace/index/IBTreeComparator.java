/*******************************************************************************
 * Copyright (c) 2005, 2009 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     QNX - Initial API and implementation
 *******************************************************************************/
package org.eclipse.linuxtools.tmf.core.trace.index;

import org.eclipse.core.runtime.CoreException;

/**
 * @author Doug Schaefer
 * @since 3.0
 */
@SuppressWarnings("javadoc")
public interface IBTreeComparator {
	/**
	 * Compare two records. Used for insert.
	 */
	public abstract int compare(long record1, long record2) throws CoreException;
}
