/*******************************************************************************
 * Copyright (c) 2013 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Marc-Andre Laperle - Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.internal.tmf.core.trace.indexer;

import org.eclipse.linuxtools.tmf.core.trace.indexer.checkpoint.ITmfCheckpoint;

/**
 * A visitor that searches for a specific checkpoint
 */
public class BTreeCheckpointVisitor implements IBTreeVisitor {
    int rank = -1;
    private ITmfCheckpoint found;
    private ITmfCheckpoint search;
    boolean exactFound = false;

    /**
     * Constructs the checkpoint visitor
     *
     * @param search the checkpoint to search for
     */
    public BTreeCheckpointVisitor(ITmfCheckpoint search) {
        this.search = search;
    }

    @Override
    public int compare(ITmfCheckpoint currentCheckpoint) {
        int compareTo = currentCheckpoint.compareTo(search);
        if (compareTo <= 0 && !exactFound) {
            rank = currentCheckpoint.getRank();
            found = currentCheckpoint;
            if (compareTo == 0) {
                exactFound = true;
            }
        }
        return compareTo;
    }

    /**
     * Return the found checkpoint
     *
     * @return the found checkpoint
     */
    public ITmfCheckpoint getCheckpoint() {
        return found;
    }
}