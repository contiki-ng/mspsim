package se.sics.mspsim.cli;

import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import se.sics.mspsim.extutil.jfreechart.LineChart;
import se.sics.mspsim.extutil.jfreechart.LineSampleChart;

public class WindowTarget implements LineListener {

  private JFrame window;
  private String targetName;
  // Default in the current version - TODO: replace with better
  private JTextArea jta = new JTextArea(40,40);
  private WindowDataHandler dataHandler = null;

  public WindowTarget(String name) {
    window = new JFrame(name);
    window.getContentPane().add(jta);
    window.pack();
    window.setVisible(true);
    targetName = name;
  }

  @Override
  public void lineRead(final String line) {    
    if (line != null && window != null) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          handleLine(line);
        }
      });
    }
  }

  private void handleLine(String line) {
    if (line.startsWith("#!")) {
      line = line.substring(2);
      String[] parts = CommandParser.parseLine(line);
      String cmd = parts[0];
      if ("bounds".equals(cmd)) {
        try {
          window.setBounds(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
              Integer.parseInt(parts[3]), Integer.parseInt(parts[4]));
        } catch (Exception e) {
          System.err.println("Could not set bounds: " + line);
        }
      } else if ("title".equals(cmd)) {
        String args = CommandParser.toString(parts, 1, parts.length);
        window.setTitle(args);
        if (dataHandler != null) {
          dataHandler.setProperty("title", new String[] {args});
        }
      } else if ("type".equals(cmd)) {
        if ("line-sample".equals(parts[1])) {
          dataHandler = new LineSampleChart();
        } else if ("line".equals(parts[1])) {
          dataHandler = new LineChart();
        } else {
          System.err.println("Unknown window data handler type: " + parts[1]);
        }
        if (dataHandler != null) {
          System.out.println("Replacing window data handler! " + parts[1] + " " + dataHandler);
          window.getContentPane().removeAll();
          window.getContentPane().add(dataHandler.getComponent());
          String title = window.getTitle();
          if (title != null) {
            // Set title for the new data handler
            dataHandler.setProperty("title", new String[] { title });
          }
          window.repaint();
        }
      } else if (dataHandler != null) {
        dataHandler.handleCommand(parts);
      }
    } else if (!line.startsWith("#")){
      if (dataHandler != null) {
        dataHandler.lineRead(line);
      } else {
        jta.append(line + '\n');
      }
    }
  }

  public void close() {
    // TODO Notify all the currently active "streams" of lines to this windows data-handlers
    window.setVisible(false);
    window.dispose();
    window.removeAll();
    window = null;
  }

  public String getName() {
    return targetName;
  }
}
