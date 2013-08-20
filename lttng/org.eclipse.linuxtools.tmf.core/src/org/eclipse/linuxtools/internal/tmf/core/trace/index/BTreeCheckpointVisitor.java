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

package org.eclipse.linuxtools.internal.tmf.core.trace.index;

import org.eclipse.linuxtools.tmf.core.trace.indexer.checkpoint.ITmfCheckpoint;

/**
 *
 */
public class BTreeCheckpointVisitor implements IBTreeVisitor {
        int rank = -1;
        private ITmfCheckpoint found;
        private ITmfCheckpoint search;
        boolean exactFound = false;

        public BTreeCheckpointVisitor(ITmfCheckpoint search) {
            this.search = search;
        }

        @Override
        public int compare(ITmfCheckpoint checkRec) {
            int compareTo = checkRec.compareTo(search);
            if (compareTo <= 0 && !exactFound) {
                rank = checkRec.getRank();
                found = checkRec;
                if (compareTo == 0) {
                    exactFound = true;
                }
            }
            return compareTo;
        }

        /**
         *
         * @return
         */
        public int getRank() {
            return rank;
        }

        /**
         *
         * @return
         */
        public ITmfCheckpoint getCheckpoint() {
            return found;
        }
    }