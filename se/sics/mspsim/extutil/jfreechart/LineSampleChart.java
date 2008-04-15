package se.sics.mspsim.extutil.jfreechart;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.Date;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DefaultXYItemRenderer;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import se.sics.mspsim.cli.WindowDataHandler;

public class LineSampleChart implements WindowDataHandler {

  JPanel panel;
  private TimeSeriesCollection dataset;
  private TimeSeries timeSeries;
  
  public LineSampleChart() {
    DateAxis domain = new DateAxis("Time");
    NumberAxis range = new NumberAxis("Value");
    XYPlot xyplot = new XYPlot();
    xyplot.setDomainAxis(domain);
    xyplot.setRangeAxis(range);
    // xyplot.setBackgroundPaint(Color.black);
    xyplot.setDataset(dataset = new TimeSeriesCollection());

    DefaultXYItemRenderer renderer = new DefaultXYItemRenderer();
    renderer.setSeriesPaint(0, Color.black);
    renderer.setSeriesShapesVisible(0, false);
    xyplot.setRenderer(renderer);

    domain.setAutoRange(true);
    domain.setLowerMargin(0.0);
    domain.setUpperMargin(0.0);

    domain.setTickLabelsVisible(true);
    range.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
    JFreeChart chart = new JFreeChart("Test",
        JFreeChart.DEFAULT_TITLE_FONT, xyplot, true);
    ChartPanel chartPanel = new ChartPanel(chart);
    panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.setPreferredSize(new Dimension(400, 200));
    panel.add(chartPanel, BorderLayout.CENTER);
    
    timeSeries = new TimeSeries("-", Millisecond.class);
    timeSeries.setMaximumItemCount(200);
    dataset.addSeries(timeSeries);
  }
  
  public JComponent getJComponent() {
    return panel;
  }

  @Override
  public void lineRead(String line) {
    System.out.println("Got line to: " + line);
    String parts[] = line.trim().split(" ");
    timeSeries.clear();
    for (int i = 0; i < parts.length; i++) {
      timeSeries.add(new Millisecond(new Date(i)), Integer.parseInt(parts[i]));      
    }
    panel.repaint();
  }
}
