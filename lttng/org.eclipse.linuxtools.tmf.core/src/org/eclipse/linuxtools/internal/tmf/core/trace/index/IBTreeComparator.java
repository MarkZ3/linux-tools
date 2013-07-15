package org.eclipse.linuxtools.internal.tmf.core.trace.index;

import org.eclipse.core.runtime.CoreException;

/**
 * @since 3.0
 */
public interface IBTreeComparator {
	/**
	 * Compare two records. Used for insert.
	 * @since 3.0
	 */
	public abstract int compare(long record1, long record2);
}
