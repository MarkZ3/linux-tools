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
import static org.junit.Assert.assertNotNull;

import java.util.Map;
import java.util.TimeZone;

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.linuxtools.internal.tmf.core.Activator;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimePreferences;
import org.junit.Test;
import org.osgi.service.prefs.BackingStoreException;

@SuppressWarnings("javadoc")
public class TmfTimePreferencesTest {

    private static final String TIME_PATTERN = "HH:mm:ss.SSS CCC NNN";

    @Test
    public void testInit() {
        assertEquals(DefaultScope.INSTANCE.getNode(Activator.PLUGIN_ID).get(TmfTimePreferences.DATIME, null), TmfTimePreferences.TIME_HOUR_FMT);
    }

    @Test
    public void testGetInstance() {
        assertNotNull(TmfTimePreferences.getInstance());
    }

    @Test
    public void testGetTimePattern() {
        assertEquals(TIME_PATTERN, TmfTimePreferences.getInstance().getTimePattern());
    }

    @Test
    public void testGetIntervalPattern() {
        assertEquals(TIME_PATTERN, TmfTimePreferences.getInstance().getTimePattern());
    }

    @Test
    public void testGetTimeZone() {
        assertEquals(TimeZone.getDefault(), TmfTimePreferences.getInstance().getTimeZone());
    }

    @Test
    public void testGetPreferenceMap() {
        Map<String, String> defaultPreferenceMap = TmfTimePreferences.getInstance().getPreferenceMap(true);
        assertEquals(TmfTimePreferences.TIME_HOUR_FMT, defaultPreferenceMap.get(TmfTimePreferences.DATIME));

        String testValue = TmfTimePreferences.TIME_HOUR_FMT + "foo";
        IEclipsePreferences node = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
        node.put(TmfTimePreferences.DATIME, testValue);
        try {
            node.flush();
        } catch (BackingStoreException e) {
        }
        Map<String, String> preferenceMap = TmfTimePreferences.getInstance().getPreferenceMap(false);
        assertEquals(testValue, preferenceMap.get(TmfTimePreferences.DATIME));
        defaultPreferenceMap = TmfTimePreferences.getInstance().getPreferenceMap(true);
        assertEquals(TmfTimePreferences.TIME_HOUR_FMT, defaultPreferenceMap.get(TmfTimePreferences.DATIME));
    }

    @Test
    public void testComputeTimePattern() {
        assertEquals(TIME_PATTERN, TmfTimePreferences.getInstance().computeTimePattern(TmfTimePreferences.getInstance().getPreferenceMap(false)));
    }

}
