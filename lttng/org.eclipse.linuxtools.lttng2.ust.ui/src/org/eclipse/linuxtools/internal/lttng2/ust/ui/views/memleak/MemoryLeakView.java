package org.eclipse.linuxtools.internal.lttng2.ust.ui.views.memleak;

import org.eclipse.linuxtools.internal.lttng2.ust.core.memoryleak.UstMemoryLeakAnalysisModule;
import org.eclipse.linuxtools.tmf.core.filter.ITmfFilter;
import org.eclipse.linuxtools.tmf.core.signal.TmfSignalHandler;
import org.eclipse.linuxtools.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.linuxtools.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.linuxtools.tmf.core.signal.TmfTraceSelectedSignal;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.ui.viewers.events.TmfEventsTable;
import org.eclipse.linuxtools.tmf.ui.views.TmfView;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/**
 * Memory Leak View
 *
 * @author Matthew Khouzam
 */
public class MemoryLeakView extends TmfView {

    /** ID string */
    public static final String ID = "org.eclipse.linuxtools.lttng2.ust.memoryleak"; //$NON-NLS-1$
    private MemoryLeakEventsTable fEventsTable;
    ITmfTrace fTrace;
    private UstMemoryLeakAnalysisModule fModule;

    private static class MemoryLeakEventsTable extends TmfEventsTable {
        public MemoryLeakEventsTable(Composite parent, int i) {
            super(parent, i);
        }

        @Override
        public void applyFilter(ITmfFilter filter) {
            super.applyFilter(filter);
        }
    }

    /**
     * Constructor
     */
    public MemoryLeakView() {
        super(Messages.MemoryLeakView_Title);
    }

    @Override
    public void createPartControl(Composite parent) {
        fEventsTable = new MemoryLeakEventsTable(parent, 100);
        ITmfTrace trace = getActiveTrace();
        if (trace != null) {
            loadTrace(trace);
        }
    }

    private void loadTrace(ITmfTrace trace) {
        fTrace = trace;
        Thread thread = new Thread() {
            @Override
            public void run() {
                initializeDataSource();
                if (fModule != null) {
                    fModule.waitForCompletion();
                } else {
                    return;
                }
                Display.getDefault().asyncExec(new Runnable() {

                    @Override
                    public void run() {
                        if (!fEventsTable.getTable().isDisposed()) {
                            fEventsTable.setTrace(fTrace, false);
                            fEventsTable.applyFilter(fModule.getFilter());
                        }
                    }
                });
            }
        };
        thread.start();
    }

    private void initializeDataSource() {
        if (fTrace != null) {
            fModule = fTrace.getAnalysisModuleOfClass(UstMemoryLeakAnalysisModule.class, UstMemoryLeakAnalysisModule.ID);
            if (fModule == null) {
                return;
            }
            fModule.schedule();
        }
    }

    /**
     * Signal handler for handling of the trace opened signal.
     *
     * @param signal
     *            The trace opened signal {@link TmfTraceOpenedSignal}
     */
    @TmfSignalHandler
    public void traceOpened(TmfTraceOpenedSignal signal) {
        fTrace = signal.getTrace();
        loadTrace(fTrace);
    }

    /**
     * Signal handler for handling of the trace selected signal.
     *
     * @param signal
     *            The trace selected signal {@link TmfTraceSelectedSignal}
     */
    @TmfSignalHandler
    public void traceSelected(TmfTraceSelectedSignal signal) {
        if (fTrace != signal.getTrace()) {
            fTrace = signal.getTrace();
            loadTrace(fTrace);
        }
    }

    /**
     * Signal handler for handling of the trace closed signal.
     *
     * @param signal
     *            The trace closed signal {@link TmfTraceClosedSignal}
     */
    @TmfSignalHandler
    public void traceClosed(TmfTraceClosedSignal signal) {

        if (signal.getTrace() != fTrace) {
            return;
        }

        fTrace = null;
        fEventsTable.setTrace(null, false);
    }

    @Override
    public void setFocus() {
        fEventsTable.setFocus();
    }

    @Override
    public void dispose() {
        super.dispose();
        fEventsTable.dispose();
    }
}
