package org.eclipse.linuxtools.internal.tmf.core.trace.index;

import org.eclipse.linuxtools.tmf.core.trace.indexer.checkpoint.ITmfCheckpoint;

/**
 *
 */
public interface IBTreeVisitor {

    public abstract int compare(ITmfCheckpoint checkRec);
}
