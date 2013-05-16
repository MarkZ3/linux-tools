/*******************************************************************************
 * Copyright (c) 2012, 2013 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Francois Chouinard - Initial API and implementation
 *     Marc-Andre Laperle - Add time zone preference
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.core.timestamp;

import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.linuxtools.internal.tmf.core.Activator;

/**
 * TMF Time format preferences
 *
 * @author Francois Chouinard
 * @version 1.0
 * @since 2.0
 */
@SuppressWarnings("javadoc")
public class TmfTimePreferences {

    // ------------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------------

    public static final String DEFAULT_TIME_PATTERN = "HH:mm:ss.SSS_CCC_NNN"; //$NON-NLS-1$

    static final String TIME_FORMAT_PREF = "org.eclipse.linuxtools.tmf.core.prefs.time.format"; //$NON-NLS-1$
    public static final String DATIME = TIME_FORMAT_PREF + ".datime"; //$NON-NLS-1$
    public static final String SUBSEC = TIME_FORMAT_PREF + ".subsec"; //$NON-NLS-1$
    public static final String TIME_ZONE = TIME_FORMAT_PREF + ".timezone"; //$NON-NLS-1$

    public static final String DATE_DELIMITER = TIME_FORMAT_PREF + ".date.delimiter"; //$NON-NLS-1$
    public static final String TIME_DELIMITER = TIME_FORMAT_PREF + ".time.delimiter"; //$NON-NLS-1$
    public static final String SSEC_DELIMITER = TIME_FORMAT_PREF + ".ssec.delimiter"; //$NON-NLS-1$

    public static final String DATE_YEAR_FMT = "yyyy-MM-dd HH:mm:ss"; //$NON-NLS-1$
    public static final String DATE_YEAR2_FMT = "yy-MM-dd HH:mm:ss"; //$NON-NLS-1$
    public static final String DATE_MONTH_FMT = "MM-dd HH:mm:ss"; //$NON-NLS-1$
    public static final String DATE_DAY_FMT = "dd HH:mm:ss"; //$NON-NLS-1$
    public static final String DATE_JDAY_FMT = "DDD HH:mm:ss"; //$NON-NLS-1$
    public static final String DATE_NO_FMT = "HH:mm:ss"; //$NON-NLS-1$

    public static final String TIME_HOUR_FMT = "HH:mm:ss"; //$NON-NLS-1$
    public static final String TIME_MINUTE_FMT = "mm:ss"; //$NON-NLS-1$
    public static final String TIME_SECOND_FMT = "ss"; //$NON-NLS-1$
    public static final String TIME_ELAPSED_FMT = "TTT"; //$NON-NLS-1$
    public static final String TIME_NO_FMT = ""; //$NON-NLS-1$

    public static final String SUBSEC_MILLI_FMT = "SSS"; //$NON-NLS-1$
    public static final String SUBSEC_MICRO_FMT = "SSS CCC"; //$NON-NLS-1$
    public static final String SUBSEC_NANO_FMT = "SSS CCC NNN"; //$NON-NLS-1$
    public static final String SUBSEC_NO_FMT = ""; //$NON-NLS-1$

    public static final String DELIMITER_NONE = ""; //$NON-NLS-1$
    public static final String DELIMITER_SPACE = " "; //$NON-NLS-1$
    public static final String DELIMITER_PERIOD = "."; //$NON-NLS-1$
    public static final String DELIMITER_COMMA = ","; //$NON-NLS-1$
    public static final String DELIMITER_DASH = "-"; //$NON-NLS-1$
    public static final String DELIMITER_UNDERLINE = "_"; //$NON-NLS-1$
    public static final String DELIMITER_COLON = ":"; //$NON-NLS-1$
    public static final String DELIMITER_SEMICOLON = ";"; //$NON-NLS-1$
    public static final String DELIMITER_SLASH = "/"; //$NON-NLS-1$
    public static final String DELIMITER_DQUOT = "\""; //$NON-NLS-1$

    private static final String DATIME_DEFAULT = TIME_HOUR_FMT;
    private static final String SUBSEC_DEFAULT = SUBSEC_NANO_FMT;
    private static final String DATE_DELIMITER_DEFAULT = DELIMITER_DASH;
    private static final String TIME_DELIMITER_DEFAULT = DELIMITER_COLON;
    private static final String SSEC_DELIMITER_DEFAULT = DELIMITER_SPACE;
    private static final String TIME_ZONE_DEFAULT = TimeZone.getDefault().getID();
    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    private static TmfTimePreferences fPreferences;

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    public static void init() {
        IEclipsePreferences defaultPreferences = DefaultScope.INSTANCE.getNode(Activator.PLUGIN_ID);
        defaultPreferences.put(TmfTimePreferences.DATIME, DATIME_DEFAULT);
        defaultPreferences.put(TmfTimePreferences.SUBSEC, SUBSEC_DEFAULT);
        defaultPreferences.put(TmfTimePreferences.DATE_DELIMITER, DATE_DELIMITER_DEFAULT);
        defaultPreferences.put(TmfTimePreferences.TIME_DELIMITER, TIME_DELIMITER_DEFAULT);
        defaultPreferences.put(TmfTimePreferences.SSEC_DELIMITER, SSEC_DELIMITER_DEFAULT);
        defaultPreferences.put(TmfTimePreferences.TIME_ZONE, TIME_ZONE_DEFAULT);

        // Create the singleton and update default formats
        getInstance();
    }

    public static synchronized TmfTimePreferences getInstance() {
        if (fPreferences == null) {
            fPreferences = new TmfTimePreferences();
            TmfTimestampFormat.updateDefaultFormats();
        }
        return fPreferences;
    }

    /**
     * Local constructor
     */
    private TmfTimePreferences() {
    }

    // ------------------------------------------------------------------------
    // Getters/Setters
    // ------------------------------------------------------------------------

    /**
     * @return the timestamp pattern
     */
    public String getTimePattern() {
        return computeTimePattern(getPreferenceMap(false));
    }

    public String getIntervalPattern() {
        return computeIntervalPattern(getPreferenceMap(false));
    }

    /**
     * Get the time zone
     *
     * @return the time zone
     */
    public TimeZone getTimeZone() {
        IPreferencesService prefs = Platform.getPreferencesService();
        return TimeZone.getTimeZone(prefs.getString(Activator.PLUGIN_ID, TIME_ZONE, TimeZone.getDefault().getID(), null));
    }

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------

    public Map<String, String> getPreferenceMap(boolean defaultValues) {
        Map<String, String> prefsMap = new HashMap<String, String>();
        IEclipsePreferences prefs = defaultValues ? DefaultScope.INSTANCE.getNode(Activator.PLUGIN_ID) : InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
        prefToMap(prefs, prefsMap, SUBSEC, SUBSEC_DEFAULT);
        prefToMap(prefs, prefsMap, TIME_DELIMITER, TIME_DELIMITER_DEFAULT);
        prefToMap(prefs, prefsMap, SSEC_DELIMITER, SSEC_DELIMITER_DEFAULT);
        prefToMap(prefs, prefsMap, DATIME, DATIME_DEFAULT);
        prefToMap(prefs, prefsMap, DATE_DELIMITER, DATE_DELIMITER_DEFAULT);
        return prefsMap;
    }

    private static String computeIntervalPattern(Map<String, String> prefsMap) {
        String ssecFmt = computeSubSecFormat(prefsMap);
        return "TTT." + ssecFmt; //$NON-NLS-1$
    }

    private static String computeSubSecFormat(Map<String, String> prefsMap) {
        String sSecFormat = prefsMap.get(SUBSEC);
        String sSecFieldSep = prefsMap.get(SSEC_DELIMITER);
        String ssecFmt = sSecFormat.replaceAll(" ", sSecFieldSep); //$NON-NLS-1$
        return ssecFmt;
    }

    private static void prefToMap(IEclipsePreferences node, Map<String, String> prefsMap, String key, String defaultValue) {
        prefsMap.put(key, node.get(key, defaultValue));
    }

    public String computeTimePattern(Map<String, String> prefsMap) {
        String dateTimeFormat = prefsMap.get(DATIME);
        if (dateTimeFormat == null) {
            dateTimeFormat = DEFAULT_TIME_PATTERN;
        }

        String dateFormat;
        String timeFormat;
        int index = dateTimeFormat.indexOf(' ');
        if (index != -1) {
            dateFormat = dateTimeFormat.substring(0, dateTimeFormat.indexOf(' ') + 1);
            timeFormat = dateTimeFormat.substring(dateFormat.length());
        } else {
            dateFormat = ""; //$NON-NLS-1$
            timeFormat = dateTimeFormat;
        }

        String dateFieldSep = prefsMap.get(DATE_DELIMITER);
        String timeFieldSep = prefsMap.get(TIME_DELIMITER);
        String dateFmt = dateFormat.replaceAll("-", dateFieldSep); //$NON-NLS-1$
        String timeFmt = timeFormat.replaceAll(":", timeFieldSep); //$NON-NLS-1$

        String ssecFmt = computeSubSecFormat(prefsMap);
        return dateFmt + timeFmt + "." + ssecFmt; //$NON-NLS-1$;
    }

}
