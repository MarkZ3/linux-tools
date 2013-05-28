package org.eclipse.linuxtools.internal.sampleview;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEvent;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfTrace;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.event.ITmfEventField;
import org.eclipse.linuxtools.tmf.core.request.ITmfDataRequest.ExecutionType;
import org.eclipse.linuxtools.tmf.core.request.TmfEventRequest;
import org.eclipse.linuxtools.tmf.core.signal.TmfSignalHandler;
import org.eclipse.linuxtools.tmf.core.signal.TmfTraceSelectedSignal;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.ui.views.TmfView;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.swtchart.Chart;
import org.swtchart.ISeries.SeriesType;
import org.swtchart.Range;

public class SampleView extends TmfView {

	private static final String SERIES_NAME = "Series";
	private static final String FIELD = "foo";
	private static final String Y_AXIS_TITLE = "Signal";
	private static final String X_AXIS_TITLE = "Time";
	private static final String VIEW_NAME = "Sample view";
	private Chart chart;

	public SampleView() {
		super(VIEW_NAME);
	}

	@Override
	public void createPartControl(Composite parent) {
		chart = new Chart(parent, SWT.BORDER);
		chart.getTitle().setVisible(false);
		chart.getAxisSet().getXAxis(0).getTitle().setText(X_AXIS_TITLE);
		chart.getAxisSet().getYAxis(0).getTitle().setText(Y_AXIS_TITLE);
		chart.getSeriesSet().createSeries(SeriesType.LINE, SERIES_NAME);
		
        ITmfTrace trace = getActiveTrace();
        if (trace != null) {
            traceSelected(new TmfTraceSelectedSignal(this, trace));
        }
	}

	@TmfSignalHandler
	public void traceSelected(final TmfTraceSelectedSignal signal) {
		TmfEventRequest req = new TmfEventRequest(CtfTmfEvent.class,
				TmfTimeRange.ETERNITY, TmfEventRequest.ALL_DATA,
				ExecutionType.BACKGROUND) {
			ArrayList<Double> xValues = new ArrayList<Double>();
			ArrayList<Double> yValues = new ArrayList<Double>();
			private double maxFieldValue = -1;
			private double minFieldValue = Double.MAX_VALUE;
			private double maxX = -Double.MAX_VALUE;
			private double minX = Double.MAX_VALUE;

			@Override
			public void handleData(ITmfEvent data) {
				super.handleData(data);
				final CtfTmfEvent event = (CtfTmfEvent) data;
				if (event != null) {
					ITmfEventField field = data.getContent().getField(FIELD);
					if (field != null) {
						Double fieldValue = (Double) field.getValue();
						yValues.add(fieldValue);
						double xValue = (double) data.getTimestamp().getValue();
						xValues.add(xValue);
						minFieldValue = Math.min(minFieldValue, fieldValue);
						maxFieldValue = Math.max(maxFieldValue, fieldValue);
						minX = Math.min(minX, xValue);
						maxX = Math.max(maxX, xValue);
					}
				}
			}

			@Override
			public void handleSuccess() {
				final double x[] = toArray(xValues);
				for (int i = 0; i < x.length; ++i) {
					x[i] -= minX;
				}
				final double y[] = toArray(yValues);

				Display.getDefault().asyncExec(new Runnable() {

					@Override
					public void run() {
						chart.getSeriesSet().getSeries()[0].setXSeries(x);
						chart.getSeriesSet().getSeries()[0].setYSeries(y);
						if (!xValues.isEmpty() && !yValues.isEmpty()) {
							System.out.println("maxx: " + maxX + " minx: " + minX);
							System.out.println("maxFoo: " + maxFieldValue + " minFoo: " + minFieldValue);
							chart.getAxisSet().getXAxis(0).setRange(new Range(0, x[x.length - 1]));
							chart.getAxisSet().getYAxis(0).setRange(new Range(minFieldValue, maxFieldValue));
							chart.getAxisSet().adjustRange();
						}
						chart.redraw();
						clearValues();

					}

				});
				super.handleSuccess();
			}

			private double[] toArray(List<Double> list) {
				double[] d = new double[list.size()];
				for (int i = 0; i < list.size(); ++i) {
					d[i] = list.get(i);
				}

				return d;
			}

			@Override
			public void handleFailure() {
				super.handleFailure();
				clearValues();
			}

			private void clearValues() {
				yValues.clear();
				xValues.clear();
			}
		};
		CtfTmfTrace fExperiment = (CtfTmfTrace) signal.getTrace();
		fExperiment.sendRequest(req);
	}

	@Override
	public void setFocus() {
		chart.setFocus();
	}

}
