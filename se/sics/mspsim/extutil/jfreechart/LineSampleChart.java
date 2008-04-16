package se.sics.mspsim.extutil.jfreechart;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DefaultXYItemRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import se.sics.mspsim.cli.AbstractWindowDataHandler;


public class LineSampleChart extends AbstractWindowDataHandler {

  JPanel panel;
  private XYSeriesCollection dataset;
  private XYSeries dataSeries;
  
  public LineSampleChart() {
    NumberAxis domain = new NumberAxis("Index");
    NumberAxis range = new NumberAxis("Value");
    XYPlot xyplot = new XYPlot();
    xyplot.setDomainAxis(domain);
    xyplot.setRangeAxis(range);
    // xyplot.setBackgroundPaint(Color.black);
    xyplot.setDataset(dataset = new XYSeriesCollection());

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
    
    dataSeries = new XYSeries("-");
    dataSeries.setMaximumItemCount(200);
    dataset.addSeries(dataSeries);
  }
  
  public JComponent getComponent() {
    return panel;
  }

  @Override
  public void lineRead(String line) {
    String parts[] = line.trim().split(" ");
    dataSeries.clear();
    for (int i = 0; i < parts.length; i++) {
      dataSeries.add(i, atoi(parts[i]));
    }
    panel.repaint();
  }

  @Override
  public void setProperty(int index, String param, String[] args) {
    if (index != 0) {
      throw new IndexOutOfBoundsException("Illegal index: " + index);
    }
    if ("label".equals(param)) {
      System.out.println("setting label to: " + args[0]);
      dataSeries.setKey(args[0]);
    }
    panel.repaint();
  }  
  
}
