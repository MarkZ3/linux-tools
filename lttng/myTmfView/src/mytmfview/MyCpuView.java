package mytmfview;

import org.eclipse.linuxtools.tmf.ui.views.TmfView;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.swtchart.Chart;
import org.swtchart.IAxis;

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
		
	}

	@Override
	public void setFocus() {
		chart.setFocus();
	}

}
