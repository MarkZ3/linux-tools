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

    // private static IPreferenceStore fPreferenceStore;
    private static String fTimestampPattern;
    private static String fIntervalPattern;

    private String fDatimeFormat;
    private String fDateFormat;
    private String fTimeFormat;
    private String fSSecFormat;

    private String fDateFieldSep = "-"; //$NON-NLS-1$
    private String fTimeFieldSep = ":"; //$NON-NLS-1$
    private String fSSecFieldSep = " "; //$NON-NLS-1$

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    public static void init() {
        IEclipsePreferences defaultPreferences = getDefaultPreferences();
        defaultPreferences.put(TmfTimePreferences.DATIME, DATIME_DEFAULT);
        defaultPreferences.put(TmfTimePreferences.SUBSEC, SUBSEC_DEFAULT);
        defaultPreferences.put(TmfTimePreferences.DATE_DELIMITER, DATE_DELIMITER_DEFAULT);
        defaultPreferences.put(TmfTimePreferences.TIME_DELIMITER, TIME_DELIMITER_DEFAULT);
        defaultPreferences.put(TmfTimePreferences.SSEC_DELIMITER, SSEC_DELIMITER_DEFAULT);
        defaultPreferences.put(TmfTimePreferences.TIME_ZONE, TIME_ZONE_DEFAULT);

        // Create the singleton and initialize format preferences
        getInstance();
    }

    private static IEclipsePreferences getDefaultPreferences() {
        return DefaultScope.INSTANCE.getNode(Activator.PLUGIN_ID);
    }

    public static synchronized TmfTimePreferences getInstance() {
        if (fPreferences == null) {
            fPreferences = new TmfTimePreferences();
        }
        return fPreferences;
    }

    /**
     * Local constructor
     */
    private TmfTimePreferences() {
        initPatterns();
        setTimePattern(fTimestampPattern, getTimeZone());
    }

    // ------------------------------------------------------------------------
    // Getters/Setters
    // ------------------------------------------------------------------------

    /**
     * @return the timestamp pattern
     */
    public static String getTimePattern() {
        return fTimestampPattern;
    }

    /**
     * Sets the timestamp, timezone and updates TmfTimestampFormat
     *
     * @param timePattern
     *            the new timestamp pattern
     * @param timeZone
     *            the new time zone
     */
    public static void setTimePattern(String timePattern, TimeZone timeZone) {
        fTimestampPattern = timePattern;
        TmfTimestampFormat.setDefaultTimeFormat(fTimestampPattern, timeZone);
        TmfTimestampFormat.setDefaultIntervalFormat(fIntervalPattern);
    }

    /**
     * Update the Date field separator
     *
     * @param pattern
     *            the Date field separator
     */
    public void setDateFieldSep(String pattern) {
        fDateFieldSep = pattern;
    }

    /**
     * Update the Time field separator
     *
     * @param pattern
     *            the Time field separator
     */
    public void setTimeFieldSep(String pattern) {
        fTimeFieldSep = pattern;
    }

    /**
     * Update the Subseconds field separator
     *
     * @param pattern
     *            the Subseconds field separator
     */
    public void setSSecFieldSep(String pattern) {
        fSSecFieldSep = pattern;
    }

    /**
     * Update the Date/Time format
     *
     * @param pattern
     *            the Date/Time format
     */
    public void setDateTimeFormat(String pattern) {
        fDatimeFormat = pattern;
        if (fDatimeFormat == null) {
            fDatimeFormat = DEFAULT_TIME_PATTERN;
        }
        int index = fDatimeFormat.indexOf(' ');
        if (index != -1) {
            fDateFormat = fDatimeFormat.substring(0, fDatimeFormat.indexOf(' ') + 1);
            fTimeFormat = fDatimeFormat.substring(fDateFormat.length());
        } else {
            fDateFormat = ""; //$NON-NLS-1$
            fTimeFormat = fDatimeFormat;
        }
    }

    /**
     * Update the Subseconds format
     *
     * @param pattern
     *            the Subseconds format
     */
    public void setSSecFormat(String pattern) {
        fSSecFormat = pattern;
    }

    /**
     * Get the time zone
     *
     * @return the time zone
     */
    public TimeZone getTimeZone() {
        IPreferencesService prefs = Platform.getPreferencesService();
        return TimeZone.getTimeZone(prefs.get(TIME_ZONE, TimeZone.getDefault().getID(), null));
    }

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------

    public void initPatterns() {
        IPreferencesService prefs = Platform.getPreferencesService();
        setDateTimeFormat(prefs.get(DATIME, DATIME_DEFAULT, null));

        fSSecFormat = prefs.get(SUBSEC, SUBSEC_DEFAULT, null);
        fDateFieldSep = prefs.get(DATE_DELIMITER, DATE_DELIMITER_DEFAULT, null);
        fTimeFieldSep = prefs.get(TIME_DELIMITER, TIME_DELIMITER_DEFAULT, null);
        fSSecFieldSep = prefs.get(SSEC_DELIMITER, SSEC_DELIMITER_DEFAULT, null);

        updatePatterns();
    }

    public void updatePatterns() {
        String dateFmt = fDateFormat.replaceAll("-", fDateFieldSep); //$NON-NLS-1$
        String timeFmt = fTimeFormat.replaceAll(":", fTimeFieldSep); //$NON-NLS-1$
        String ssecFmt = fSSecFormat.replaceAll(" ", fSSecFieldSep); //$NON-NLS-1$

        fTimestampPattern = dateFmt + timeFmt + "." + ssecFmt; //$NON-NLS-1$
        fIntervalPattern = "TTT." + ssecFmt; //$NON-NLS-1$
    }

    public void setDefaults() {
        setDateTimeFormat(TmfTimePreferences.DATIME_DEFAULT);
        setSSecFormat(TmfTimePreferences.SUBSEC_DEFAULT);
        setDateFieldSep(TmfTimePreferences.DATE_DELIMITER_DEFAULT);
        setTimeFieldSep(TmfTimePreferences.TIME_DELIMITER_DEFAULT);
        setSSecFieldSep(TmfTimePreferences.SSEC_DELIMITER_DEFAULT);
        updatePatterns();
    }

}
