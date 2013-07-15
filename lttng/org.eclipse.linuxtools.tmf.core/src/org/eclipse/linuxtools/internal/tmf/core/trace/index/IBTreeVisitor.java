package org.eclipse.linuxtools.internal.tmf.core.trace.index;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.linuxtools.tmf.core.trace.ITmfCheckpoint;

/**
 * @since 3.0
 */
public interface IBTreeVisitor {
    /**
     * Visit a given record and return whether to continue or not.

     * @return <code>true</code> to continue the visit, <code>false</code> to abort it.
     * @throws CoreException
     */
    public abstract boolean visit(BTreeKey key);

    public abstract boolean visit(ITmfCheckpoint key);
}
