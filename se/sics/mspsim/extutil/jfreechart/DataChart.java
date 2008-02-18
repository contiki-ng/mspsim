package se.sics.mspsim.extutil.jfreechart;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DefaultXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import se.sics.mspsim.chip.CC2420;
import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.ui.WindowUtils;
import se.sics.mspsim.util.DataSource;
import se.sics.mspsim.util.OperatingModeStatistics;
import se.sics.mspsim.util.StackMonitor;

@SuppressWarnings("serial")
public class DataChart extends JPanel {

  private TimeSeriesCollection dataset;
  
  public DataChart(String title, String yaxis) {
    DateAxis domain = new DateAxis("Time");
    NumberAxis range = new NumberAxis(yaxis);
    XYPlot xyplot = new XYPlot();
    xyplot.setDomainAxis(domain);
    xyplot.setRangeAxis(range);
 // xyplot.setBackgroundPaint(Color.black);
    xyplot.setDataset(dataset = new TimeSeriesCollection());

    DefaultXYItemRenderer renderer = new DefaultXYItemRenderer();
    renderer.setSeriesPaint(0, Color.red);
    renderer.setSeriesPaint(1, Color.green);
    renderer.setSeriesPaint(2, Color.blue);
    renderer.setSeriesPaint(3, Color.black);
//    renderer.setBaseStroke(
//        new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL)
//    );
    renderer.setSeriesShapesVisible(0, false);
    renderer.setSeriesShapesVisible(1, false);
    renderer.setSeriesShapesVisible(2, false);
    renderer.setSeriesShapesVisible(3, false);
    xyplot.setRenderer(renderer);
    
    domain.setAutoRange(true);
    domain.setLowerMargin(0.0);
    domain.setUpperMargin(0.0);

    domain.setTickLabelsVisible(true);
    range.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
    JFreeChart chart = new JFreeChart(title, 
        JFreeChart.DEFAULT_TITLE_FONT, xyplot, true);
    ChartPanel chartPanel = new ChartPanel(chart);
    setLayout(new BorderLayout());
    setPreferredSize(new Dimension(400, 200));
    add(chartPanel, BorderLayout.CENTER);
  }

  public void addTimeSeries(TimeSeries ts) {
    dataset.addSeries(ts);
  }
  
  private JFrame openFrame(String name) {
    JFrame jw = new JFrame(name);
    jw.add(this);
    WindowUtils.restoreWindowBounds(name, jw);
    WindowUtils.addSaveOnShutdown(name, jw);
//     jw.setBounds(100, 100, 400, 200);
    return jw;
  }
  
  public void setupStackFrame(MSP430 cpu) {
    JFrame jw = openFrame("Stack Monitor");
    StackMonitor sm = new StackMonitor(cpu);
    DataSourceSampler dss = new DataSourceSampler(cpu);
    TimeSeries ts = new TimeSeries("Max Stack", Millisecond.class);
    ts.setMaximumItemCount(200);
    addTimeSeries(ts);
    dss.addDataSource(sm.getMaxSource(), ts);
    ts = new TimeSeries("Stack", Millisecond.class);
    ts.setMaximumItemCount(200);
    addTimeSeries(ts);
    dss.addDataSource(sm.getSource(), ts);
    jw.setVisible(true);
  }
  
  public DataSourceSampler setupChipFrame(MSP430 cpu) {
    JFrame jw = openFrame("Duty-Cycle Monitor");
    DataSourceSampler dss = new DataSourceSampler(cpu);
    dss.setInterval(50);
    jw.setVisible(true);
    return dss;
  }

  public void addDataSource(DataSourceSampler dss, String name, DataSource src) {
    TimeSeries ts = new TimeSeries(name, Millisecond.class);
    ts.setMaximumItemCount(200);
    addTimeSeries(ts);
    dss.addDataSource(src, ts);
  }  
}
