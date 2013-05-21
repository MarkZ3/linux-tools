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

package org.eclipse.linuxtools.tmf.core.tests.event;

import static org.junit.Assert.assertEquals;

import java.util.TimeZone;

import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimePreferences;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimestampFormat;
import org.junit.Test;

/**
 * Test suite for the TmfTimestampFormat class.
 */
@SuppressWarnings("javadoc")
public class TmfTimestampFormatTest {

    private static final String TEST_PATTERN = "HH:mm:ss.SSS";
    private static final TimeZone TEST_TIME_ZONE = TimeZone.getTimeZone(TimeZone.getAvailableIDs(0)[0]);

    private final TmfTimestampFormat tsf1 = new TmfTimestampFormat(TEST_PATTERN);
    private final TmfTimestampFormat tsf2 = new TmfTimestampFormat(TEST_PATTERN, TEST_TIME_ZONE);

    @Test
    public void testDefaultConstructor() {
        // The default should be loaded
        TmfTimestampFormat ts0 = new TmfTimestampFormat();
        assertEquals("toPattern", "HH:mm:ss.SSS CCC NNN", ts0.toPattern());
    }

    @Test
    public void testValueConstructor() {
        assertEquals("toPattern", TEST_PATTERN, tsf1.toPattern());
    }

    @Test
    public void testValueTimeZoneConstructor() {
        assertEquals("toPattern", TEST_PATTERN, tsf2.toPattern());
        assertEquals("getTimeZone", TEST_TIME_ZONE, tsf2.getTimeZone());
    }

    @Test
    public void testUpdateDefaultFormats() {
        TmfTimestampFormat.updateDefaultFormats();
        assertEquals(TmfTimestampFormat.getDefaulTimeFormat().toPattern(), TmfTimePreferences.getInstance().getTimePattern());
        assertEquals(TmfTimestampFormat.getDefaulIntervalFormat().toPattern(), TmfTimePreferences.getInstance().getIntervalPattern());
    }

    @Test
    public void testGetDefaulTimeFormat() {
        assertEquals(TmfTimestampFormat.getDefaulTimeFormat().toPattern(), TmfTimePreferences.getInstance().getTimePattern());
    }

    @Test
    public void testGetDefaulIntervalFormat() {
        assertEquals(TmfTimestampFormat.getDefaulIntervalFormat().toPattern(), TmfTimePreferences.getInstance().getIntervalPattern());
    }

    @Test
    public void testApplyPattern() {

    }
}
