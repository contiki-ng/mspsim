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

  private int interval = 100;
  private Timer timer;
  private ArrayList<TimeSource> sources = new ArrayList<TimeSource>();

//  private TimeSeries test;
//  private TimeSeries test2;
//  private TimeSeriesCollection dataset;  
  
  public DataSourceSampler() {
    timer = new Timer(interval, this);
//    test = new TimeSeries("Data", Millisecond.class);
//    test.setMaximumItemAge(30000);
//    test2 = new TimeSeries("Data 2", Millisecond.class);
//    test2.setMaximumItemAge(30000);
////    test2.setMaximumItemCount(30000);
//    dataset = new TimeSeriesCollection();
//    dataset.addSeries(test);
//    dataset.addSeries(test2);
    timer.start();
  }
  
  public TimeSource addDataSource(DataSource source, TimeSeries ts) {
    TimeSource times = new TimeSource(source, ts);
    sources.add(times);
    return times;
  }
  
  public void removeDataSource(TimeSource source) {
    sources.remove(source);
  }
  
  public void setInterval(int intMsek) {
    interval = intMsek;
    timer.setDelay(interval);
  }

  private void sampleAll() {
    if (sources.size() > 0) {
      TimeSource[] srcs = (TimeSource[]) sources.toArray(new TimeSource[0]);    
      for (int i = 0; i < srcs.length; i++) {
        srcs[i].update();
      }
    }
    
//    test.add(new Millisecond(), Math.random() * 100);
//    test2.add(new Millisecond(), Math.random() * 100);
  }

  public void actionPerformed(ActionEvent arg0) {
    sampleAll();
  }
  
  
//  public static void main(String[] args) {
//    DataSourceSampler samp = new DataSourceSampler();
//    DateAxis domain = new DateAxis("Time");
//    NumberAxis range = new NumberAxis("Memory");
//    XYPlot xyplot = new XYPlot();
//    xyplot.setDataset(samp.dataset);
//    xyplot.setDomainAxis(domain);
//    xyplot.setRangeAxis(range);
//    xyplot.setBackgroundPaint(Color.black);
//    
//    XYItemRenderer renderer = new DefaultXYItemRenderer();
//    renderer.setSeriesPaint(0, Color.red);
//    renderer.setSeriesPaint(1, Color.green);
//    renderer.setBaseStroke(
//        new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL)
//    );
//    xyplot.setRenderer(renderer);
//    
//    domain.setAutoRange(true);
//    domain.setLowerMargin(0.0);
//    domain.setUpperMargin(0.0);
//    domain.setTickLabelsVisible(true);
//    range.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
//    JFreeChart chart = new JFreeChart(
//        "Memory Usage",
//        JFreeChart.DEFAULT_TITLE_FONT,
//        xyplot,true);
//    ChartPanel chartPanel = new ChartPanel(chart);
//    JFrame jw = new JFrame("test");
//    jw.add(chartPanel);
//    jw.setBounds(100, 100, 400, 200);
//    jw.setVisible(true);
//        
//  }
  
  class TimeSource {

    private DataSource dataSource;
    private TimeSeries timeSeries;

    TimeSource(DataSource ds, TimeSeries ts) {
      dataSource = ds;
      timeSeries = ts;
    }
    
    public void update() {
      timeSeries.add(new Millisecond(), dataSource.getValue());
    }
    
  }
  
}
