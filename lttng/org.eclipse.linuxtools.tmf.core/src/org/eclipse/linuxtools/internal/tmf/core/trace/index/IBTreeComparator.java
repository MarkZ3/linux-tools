package org.eclipse.linuxtools.internal.tmf.core.trace.index;


/**
 *
 */
public interface IBTreeComparator {
	/**
	 * Compare two records. Used for insert.
	 */
	public abstract int compare(long record1, long record2);
}
