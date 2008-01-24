package se.sics.mspsim.extutil.jfreechart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JWindow;
import javax.swing.Timer;

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

import se.sics.mspsim.util.DataSource;

public class DataSourceSampler implements ActionListener {

  private int interval = 1000;
  private Timer timer;
  private ArrayList<DataSource> sources = new ArrayList<DataSource>();

  private TimeSeries test;
  private TimeSeriesCollection dataset;
  
  public DataSourceSampler() {
    timer = new Timer(interval, this);
    test = new TimeSeries("Data", Millisecond.class);
    test.setMaximumItemCount(30000);
    dataset = new TimeSeriesCollection();
    dataset.addSeries(test);
//    timer.start();
  }
  
  public void addDataSource(DataSource source) {
    sources.add(source);
  }
  
  public void removeDataSource(DataSource source) {
    sources.remove(source);
  }
  
  public void setInterval(int intMsek) {
    interval = intMsek;
    timer.setDelay(interval);
  }

  private void sampleAll() {
    if (sources.size() > 0) {
      DataSource[] srcs = (DataSource[]) sources.toArray(new DataSource[0]);    
      for (int i = 0; i < srcs.length; i++) {
        int val = srcs[i].getValue();
      
      }
    }
    test.add(new Millisecond(), Math.random());
  }

  public void actionPerformed(ActionEvent arg0) {
    System.out.println("Scheduled for sampling...");
    sampleAll();
  }
  
  
  public static void main(String[] args) {
    DataSourceSampler samp = new DataSourceSampler();
    DateAxis domain = new DateAxis("Time");
    NumberAxis range = new NumberAxis("Memory");
    XYPlot xyplot = new XYPlot();
    xyplot.setDataset(samp.dataset);
    xyplot.setDomainAxis(domain);
    xyplot.setRangeAxis(range);
    xyplot.setBackgroundPaint(Color.black);
    
    XYItemRenderer renderer = new DefaultXYItemRenderer();
    renderer.setSeriesPaint(0, Color.red);
    renderer.setSeriesPaint(1, Color.green);
    renderer.setBaseStroke(
        new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL)
    );
    xyplot.setRenderer(renderer);
    
    domain.setAutoRange(true);
    domain.setLowerMargin(0.0);
    domain.setUpperMargin(0.0);
    domain.setTickLabelsVisible(true);
    range.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
    JFreeChart chart = new JFreeChart(
        "Memory Usage",
        JFreeChart.DEFAULT_TITLE_FONT,
        xyplot,true);
    ChartPanel chartPanel = new ChartPanel(chart);
    JFrame jw = new JFrame("test");
    jw.add(chartPanel);
    jw.setBounds(100, 100, 400, 200);
    jw.setVisible(true);
        
  }
  
}
