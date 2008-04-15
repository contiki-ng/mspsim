package se.sics.mspsim.cli;

import javax.swing.JFrame;
import javax.swing.JTextArea;

import se.sics.mspsim.extutil.jfreechart.LineSampleChart;

public class WindowTarget implements LineListener {

  private JFrame window;
  private String targetName;
  private JTextArea jta = new JTextArea(20,20);
  private WindowDataHandler dataHandler = null;
  
  public WindowTarget(String name) {
    window = new JFrame(name);
    window.setVisible(true);
    window.getContentPane().add(jta);
    targetName = name;
  }
  
  @Override
  public void lineRead(String line) {
    // TODO Auto-generated method stub
    if (line != null && line.startsWith("#!")) {
      line = line.substring(2);
      String[] parts = line.split(" ");
      String cmd = parts[0];
      if ("bounds".equals(cmd)) {
        try {
          window.setBounds(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
              Integer.parseInt(parts[3]), Integer.parseInt(parts[4]));
        } catch (Exception e) {
          System.err.println("Cound not set bounds: " + line);
        }
      } else if ("title".equals(cmd)) {
        window.setTitle(parts[1]);
      } else if ("type".equals(cmd)) {
        if ("line-sample".equals(parts[1])) {
          dataHandler = new LineSampleChart();
        }
        if (dataHandler != null) {
          System.out.println("Replacing window data handler! " + parts[1] + " " + dataHandler);
          window.getContentPane().removeAll();
          window.getContentPane().add(dataHandler.getJComponent());
        }
      }
    } else {
      if (dataHandler != null) {
        dataHandler.lineRead(line);
      } else {
        jta.append(line + '\n');
      }
    }
    //    jta.set
  }

  public void close() {
    // Notify all the currently active "streams" of lines to this windows
    // data-handlers
    window.setVisible(false);
    window.removeAll();
    window = null;
  }

  public String getName() {
    // TODO Auto-generated method stub
    return targetName;
  }  
}
