/*******************************************************************************
 * Copyright (c) 2009, 2013 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Francois Chouinard - Initial API and implementation
 *   Alexandre Montplaisir - Port to JUnit4, enable CTF and statistics tests
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.core.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Master test suite for TMF Core.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    TmfCorePluginTest.class,
    org.eclipse.linuxtools.tmf.core.tests.analysis.AllTests.class,
    org.eclipse.linuxtools.tmf.core.tests.component.AllTests.class,
})
public class AllTmfCoreTests {

}
