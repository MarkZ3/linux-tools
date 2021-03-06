/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - Jeff Briggs, Henry Hughes, Ryan Morse
 *******************************************************************************/

package org.eclipse.linuxtools.systemtap.graphingapi.core.structures;

public class DataPoint {
	public DataPoint() {
		this(0, 0);
	}
	
	public DataPoint(double x, double y) {
		this.x = x;
		this.y = y;
	}
	
	public double x;
	public double y;
}
