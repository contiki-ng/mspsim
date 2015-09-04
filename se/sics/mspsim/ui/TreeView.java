package se.sics.mspsim.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.tree.DefaultMutableTreeNode;

import se.sics.mspsim.core.IOUnit;
import se.sics.mspsim.core.MSP430;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;

public class TreeView
{  
  private MSP430 cpu;
  private DefaultMutableTreeNode root=null;
  private JScrollPane scroller;
  private JPanel view;

  public javax.swing.Timer rtimer;
  public JFrame tf = null;

  public TreeView(MSP430 cpu)  	           	       	                     //Create tree in JFrame, set size and other parameters
  {  
    rtimer = new javax.swing.Timer(300, new ActionListener() {           //Refresh-Timer
      public void actionPerformed(ActionEvent evt) {                     //
        Refresh();                                                       //Calls Refresh-function periodically to ensure correct information 
        if (!tf.isVisible()) {                                           //Displayed in the frame
          rtimer.stop();                                                 //Stops the timer in case of not-visible window to ensure performance
        }                                                                // 
      }                                                                  //
    });                                                                  // 

    this.cpu = cpu;                                                      // 
    root = CreateRoot();                                                 //Fills root with information which are stored in tree nodes

    DefaultTreeModel model = new DefaultTreeModel(root);                 // 
    JTree tree = new JTree(model);                                             //


    tf = new JFrame("Components");                                       //Names frame
    tf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);                //Sets parameters
    tf.add(tree);                                                        //Adds tree to frame 
    tf.setSize(210,450);                                                 //Set parameters
    tf.setLocation(750,400);                                             //Set parameters
    tf.setVisible(true);                                                 //Makes it visible

    tree.setRootVisible(false);                                          //Makes root invisible

    view = new JPanel(new GridBagLayout());
   
    view.add(tree,GetConstraints());
    scroller = new JScrollPane(view);                      //Creates scroll bar
    tf.add(scroller);                                                    //Adds scroller to frame 

    rtimer.start();                                                      //Refresh timer runs from the beginning
  }  

  private DefaultMutableTreeNode CreateRoot() {                          //Calls function, which collects data from components and adds those data to the tree 

    DefaultMutableTreeNode root=new DefaultMutableTreeNode("Components");//

    for(Object uni: cpu.getIOUnits()){                                   //This loop enables direct access to each component and tries to gather information about them
      IOUnit ele=(IOUnit)uni;                                            //
      DefaultMutableTreeNode node=ele.getNode();                         //
      if(node!=null)                                                     //Only components with collectible information are displayed
        root.add(node);                                                  //
    }                                                                    //
    return root;                                                         //Returns root
  }                                                                      //

  private void Refresh(){                                                //Refreshes tree

    int position = scroller.getVerticalScrollBar().getValue();           //Stores scroll bar position

    DefaultTreeModel model = new DefaultTreeModel(CreateRoot());
    JTree tree = new JTree(model);
    tree.setRootVisible(false);

    for(int count = 0; count < tree.getRowCount(); count++)              //This loop prompts which rows are expanded in the obsolete tree. 
      if(((JTree)view.getComponent(0)).isExpanded(count))                                     //
        tree.expandRow(count);                                       //Expands those rows in new tree



    view.removeAll();
    view.add(tree,GetConstraints());
    view.revalidate();
    view.repaint();
    
    scroller.getVerticalScrollBar().setValue(position);                  //Sets the scroll bar to the previous position
  }
  
  private static GridBagConstraints GetConstraints(){
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridwidth = GridBagConstraints.VERTICAL;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1;    
    return gbc;
  }

  public void ReOpen(){
    tf.setVisible(true);                                                 //
    rtimer.restart();                                                    //Makes frame visible, if it's already built
  }                                                                      //restarts timer
}                                                                        //