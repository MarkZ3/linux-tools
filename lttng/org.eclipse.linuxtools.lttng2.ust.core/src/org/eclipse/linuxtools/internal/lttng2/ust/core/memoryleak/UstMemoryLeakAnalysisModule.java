/*******************************************************************************
 * Copyright (c) 2014 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation
 *   Guilliano Molaire - Provide the requirements of the analysis
 *******************************************************************************/

package org.eclipse.linuxtools.internal.lttng2.ust.core.memoryleak;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.linuxtools.internal.lttng2.ust.core.memoryusage.UstMemoryStrings;
import org.eclipse.linuxtools.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.linuxtools.tmf.core.filter.ITmfFilter;
import org.eclipse.linuxtools.tmf.core.request.ITmfEventRequest;
import org.eclipse.linuxtools.tmf.core.request.ITmfEventRequest.ExecutionType;
import org.eclipse.linuxtools.tmf.core.request.TmfEventRequest;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimeRange;

/**
 * This analysis build a state system from the libc memory instrumentation on a
 * UST trace
 *
 * @author Marc-Andre Laperle
 * @since 3.1
 */
public class UstMemoryLeakAnalysisModule extends TmfAbstractAnalysisModule {

    /**
     * Analysis ID, it should match that in the plugin.xml file
     */
    public static String ID = "org.eclipse.linuxtools.lttng2.ust.analysis.memory.leak"; //$NON-NLS-1$

    // Address to event
    private Map<Long, ITmfEvent> events;
    private Set<ITmfEvent> leaks;
    private static final Long ZERO = Long.valueOf(0);

    private TmfEventRequest fTmfEventRequest;

    @Override
    protected boolean executeAnalysis(IProgressMonitor monitor) throws TmfAnalysisException {
        events = new HashMap<>();
        fTmfEventRequest = new TmfEventRequest(ITmfEvent.class, TmfTimeRange.ETERNITY,
                0, ITmfEventRequest.ALL_DATA, ExecutionType.BACKGROUND) {

            @Override
            public void handleData(ITmfEvent event) {
                // TODO Auto-generated method stub
                super.handleData(event);

                String name = event.getType().getName();
                switch (name) {
                case UstMemoryStrings.MALLOC: {
                    Long ptr = (Long) event.getContent().getField(UstMemoryStrings.FIELD_PTR).getValue();
                    if (ZERO.equals(ptr)) {
                        return;
                    }

                    events.put(ptr, event);
                }
                    break;
                case UstMemoryStrings.FREE: {
                    Long ptr = (Long) event.getContent().getField(UstMemoryStrings.FIELD_PTR).getValue();
                    if (ZERO.equals(ptr)) {
                        return;
                    }

                    events.remove(ptr);
                }
                    break;
                case UstMemoryStrings.CALLOC: {
                    Long ptr = (Long) event.getContent().getField(UstMemoryStrings.FIELD_PTR).getValue();
                    if (ZERO.equals(ptr)) {
                        return;
                    }
                    events.put(ptr, event);
                }
                    break;
                default:
                    break;
                }
            }

        };
        getTrace().sendRequest(fTmfEventRequest);
        try {
            fTmfEventRequest.waitForCompletion();
            leaks = new HashSet<>();
            for (ITmfEvent event : events.values()) {
                leaks.add(event);
            }
            events.clear();
            System.out.println("leaks: " + leaks.size());
        } catch (InterruptedException e) {
            return false;
        }
        return true;
    }

    @Override
    protected void canceling() {
        fTmfEventRequest.cancel();
    }

    public ITmfFilter getFilter() {
        return new ITmfFilter() {

            @Override
            public boolean matches(ITmfEvent event) {
                boolean contains = leaks.contains(event);
                return contains;
            }
        };
    }
}