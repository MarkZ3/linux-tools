package mytmfview;

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
import org.eclipse.linuxtools.tmf.ui.views.TmfView;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.swtchart.Chart;
import org.swtchart.IAxis;
import org.swtchart.ISeries;
import org.swtchart.ISeries.SeriesType;
import org.swtchart.Range;

public class MyCpuView extends TmfView {

	private Chart chart;

	public MyCpuView() {
		super("Cpu view");
	}

	@Override
	public void createPartControl(Composite parent) {
		chart = new Chart(parent, SWT.BORDER);
		IAxis xAxis = chart.getAxisSet().getXAxis(0);
		xAxis.getTitle().setText("Time");
		IAxis yAxis = chart.getAxisSet().getYAxis(0);
		yAxis.getTitle().setText("Signal");

		ISeries createSeries = chart.getSeriesSet().createSeries(
				SeriesType.LINE, "Series");
		createSeries.getLabel().setVisible(false);
	}

	@TmfSignalHandler
	public void traceSelected(final TmfTraceSelectedSignal signal) {
		TmfEventRequest req = new TmfEventRequest(CtfTmfEvent.class,
				TmfTimeRange.ETERNITY, TmfEventRequest.ALL_DATA,
				ExecutionType.BACKGROUND) {
			ArrayList<Double> xValues = new ArrayList<Double>();
			ArrayList<Double> yValues = new ArrayList<Double>();
			private double maxFoo = -1;
			private double minFoo = Double.MAX_VALUE;
			private double maxX = -Double.MAX_VALUE;
			private double minX = Double.MAX_VALUE;

			@Override
			public void handleData(ITmfEvent data) {
				super.handleData(data);
				final CtfTmfEvent event = (CtfTmfEvent) data;
				if (event != null) {
					ITmfEventField field = data.getContent().getField("foo");
					if (field != null) {
						Double foo = (Double) field.getValue();
						yValues.add(foo);
						double xValue = (double) data.getTimestamp().getValue();
						xValues.add(xValue);
						minFoo = Math.min(minFoo, foo);
						maxFoo = Math.max(maxFoo, foo);
						minX = Math.min(minX, xValue);
						maxX = Math.max(maxX, xValue);
					}
				}
			}

			@Override
			public void handleSuccess() {
				if (xValues.isEmpty() || yValues.isEmpty()) {
					return;
				}
				
				final double x[] = toArray(xValues);
				final double y[] = toArray(yValues);

				Display.getDefault().asyncExec(new Runnable() {

					@Override
					public void run() {
						chart.getSeriesSet().getSeries()[0].setXSeries(x);
						chart.getSeriesSet().getSeries()[0].setYSeries(y);
						double test = (maxX - minX) / 1;
						System.out.println("maxx: " + maxX + " minx: " + minX);
						System.out.println("maxFoo: " + maxFoo + " minFoo: " + minFoo);
						chart.getAxisSet().getXAxis(0).setRange(new Range(minX, maxX));
						chart.getAxisSet().getYAxis(0).setRange(new Range(minFoo, maxFoo));
						chart.getAxisSet().adjustRange();
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
		// chart.setFocus();
	}

}
