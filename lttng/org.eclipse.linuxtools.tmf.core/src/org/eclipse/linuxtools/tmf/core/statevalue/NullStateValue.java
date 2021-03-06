/*******************************************************************************
 * Copyright (c) 2012, 2013 Ericsson
 * Copyright (c) 2010, 2011 École Polytechnique de Montréal
 * Copyright (c) 2010, 2011 Alexandre Montplaisir <alexandre.montplaisir@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.core.statevalue;

/**
 * A state value that contains no particular value. It is sometimes needed over
 * a "null" reference, since we avoid NPE's this way.
 *
 * It can also be read either as a String ("nullValue") or an Integer (-1).
 *
 * @version 1.0
 * @author Alexandre Montplaisir
 */
final class NullStateValue extends TmfStateValue {

    @Override
    public Type getType() {
        return Type.NULL;
    }

    @Override
    public boolean isNull() {
        return true;
    }

    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public String toString() {
        return "nullValue"; //$NON-NLS-1$
    }
}
