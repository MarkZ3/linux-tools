package org.eclipse.linuxtools.tmf.sample.ui;

import org.eclipse.linuxtools.tmf.core.signal.TmfSignalHandler;
import org.eclipse.linuxtools.tmf.core.signal.TmfTraceSelectedSignal;
import org.eclipse.linuxtools.tmf.ui.views.TmfView;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.swtchart.Chart;
import org.swtchart.ISeries.SeriesType;

public class SampleView extends TmfView {

	private static final String SERIES_NAME = "Series";
	private static final String Y_AXIS_TITLE = "Signal";
	private static final String X_AXIS_TITLE = "Time";
	private static final String VIEW_ID = "org.eclipse.linuxtools.tmf.sample.ui.view";
	private Chart chart;
	
	public SampleView() {
		super(VIEW_ID);
	}

	@Override
	public void createPartControl(Composite parent) {
		chart = new Chart(parent, SWT.BORDER);
		chart.getTitle().setVisible(false);
		chart.getAxisSet().getXAxis(0).getTitle().setText(X_AXIS_TITLE);
		chart.getAxisSet().getYAxis(0).getTitle().setText(Y_AXIS_TITLE);
		chart.getSeriesSet().createSeries(SeriesType.LINE, SERIES_NAME);
	}
	
	@TmfSignalHandler
	public void traceSelected(final TmfTraceSelectedSignal signal) {
		
	}

	@Override
	public void setFocus() {
	}

}
