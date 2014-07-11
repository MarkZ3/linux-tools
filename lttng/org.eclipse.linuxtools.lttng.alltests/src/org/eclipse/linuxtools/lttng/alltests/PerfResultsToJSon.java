/*******************************************************************************
 * Copyright (c) 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Marc-Andre Laperle - Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.lttng.alltests;

import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.eclipse.test.internal.performance.InternalDimensions;
import org.eclipse.test.internal.performance.PerformanceTestPlugin;
import org.eclipse.test.internal.performance.db.DB;
import org.eclipse.test.internal.performance.db.Scenario;
import org.eclipse.test.internal.performance.db.SummaryEntry;
import org.eclipse.test.internal.performance.db.TimeSeries;
import org.eclipse.test.internal.performance.db.Variations;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

/**
 * Convert results from the database to JSON suitable for display
 */
public class PerfResultsToJSon {

    private static final String BUILD_DATE_FORMAT = "yyyyMMdd-HHmm";

    private static final String OVERVIEW_CHART_FILE_NAME = "chart_overview";
    private static final String CHART_FILE_NAME = "chart";
    private static final String CHART_FILE_NAME_EXTENSION = ".json";

    /**
     * Convert results from the database to JSON suitable for display
     *
     * @throws JSONException
     *             JSON error
     * @throws IOException
     *             io error
     */
    @Test
    public void parseResults() throws JSONException, IOException {
        Variations variations = PerformanceTestPlugin.getVariations();

        String scenarioPattern = "%"; //$NON-NLS-1$

        String seriesKey = PerformanceTestPlugin.BUILD;
        List<String> builds = new ArrayList<>();
        DB.queryDistinctValues(builds, seriesKey, variations, scenarioPattern);

        Scenario[] scenarios = DB.queryScenarios(variations, scenarioPattern, seriesKey, null);
        System.out.println("Converting results for " + scenarios.length + " Scenarios"); //$NON-NLS-1$

        for (int i = 0; i < scenarios.length; i++) {
            JSONArray rootScenario = new JSONArray();
            rootScenario.put(createSerie(scenarios[i], variations, scenarios[i].getScenarioName()));
            try (FileWriter fw = new FileWriter(CHART_FILE_NAME + i + CHART_FILE_NAME_EXTENSION)) {
                fw.write(rootScenario.toString());
            }
        }

        JSONArray rootSummary = new JSONArray();
        SummaryEntry[] querySummaries = DB.querySummaries(variations, scenarioPattern);
        for (SummaryEntry entry : querySummaries) {
            if (entry.isGlobal) {
                for (int i = 0; i < scenarios.length; i++) {
                    Scenario s = scenarios[i];
                    if (s.getScenarioName().equals(entry.scenarioName)) {
                        rootSummary.put(createSerie(s, variations, entry.shortName));
                    }
                }
            }
        }
        try (FileWriter fw = new FileWriter(OVERVIEW_CHART_FILE_NAME + CHART_FILE_NAME_EXTENSION)) {
            fw.write(rootSummary.toString(4));
        }

        generateMetaData(variations);
    }

    private void generateMetaData(Variations variations) throws JSONException {
        JSONObject rootMetadata = new JSONObject();
        rootMetadata.put("osjvm", createOsJvm(variations));
    }

    private JSONObject createOsJvm(Variations variations) throws JSONException {
        JSONObject osjvm = new JSONObject();
        List<String> configs = new ArrayList<>();
        String key = PerformanceTestPlugin.CONFIG;
        String scenarioPattern = "%"; //$NON-NLS-1$
        Variations v = new Variations();
        v.setProperty("%", "%");

        DB.queryDistinctValues(configs, key, v, scenarioPattern);

        int i = 1;
        for (String config : configs) {
            List<String> vms = new ArrayList<>();
            key = "jvm";
            v = new Variations();
            v.setProperty("config", config);

            List<String> jvms = new ArrayList<>();
            DB.queryDistinctValues(configs, key, v, scenarioPattern);
            for (String jvm : jvms) {
                JSONObject osjvmItem = new JSONObject();
                osjvm.put(Integer.toString(i), osjvmItem);
                i++;
            }
        }


        // TODO Auto-generated method stub
        return osjvm;
    }

    /**
     * This main can be run from within Eclipse provided everything is on the
     * class path.
     *
     * @param args
     *            the arguments
     * @throws JSONException
     *             JSON error
     * @throws IOException
     *             io error
     */
    public static void main(String[] args) throws JSONException, IOException {
        new PerfResultsToJSon().parseResults();
    }

    private static JSONObject createSerie(Scenario s, Variations variations, String shortName) throws JSONException {
        JSONObject o = new JSONObject();
        o.putOpt("key", shortName);
        o.putOpt("values", createPoints(s, variations));
        return o;
    }

    private static JSONArray createPoints(Scenario s, Variations variations) throws JSONException {
        // Can be uncommented to see raw dump
        // s.dump(System.out, PerformanceTestPlugin.BUILD);

        String[] builds = DB.querySeriesValues(s.getScenarioName(), variations, PerformanceTestPlugin.BUILD);
        Date[] dates = new Date[builds.length];
        for (int i = 0; i < builds.length; i++) {
            dates[i] = parseBuildDate(builds[i]);
        }

        TimeSeries timeSeries = s.getTimeSeries(InternalDimensions.CPU_TIME);
        JSONArray dataPoints = new JSONArray();
        for (int i = 0; i < dates.length; i++) {
            JSONArray point = new JSONArray();
            point.put(dates[i].getTime());
            point.put(timeSeries.getValue(i));
            dataPoints.put(point);
        }
        return dataPoints;
    }

    private static Date parseBuildDate(String build) {
        SimpleDateFormat f = new SimpleDateFormat(BUILD_DATE_FORMAT);
        Date date;
        try {
            date = f.parse(build);
        } catch (ParseException e) {
            return null;
        }
        return date;
    }
}
