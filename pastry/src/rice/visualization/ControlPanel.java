/*
 * Created on Mar 12, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package rice.visualization;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import rice.environment.logging.Logger;
import rice.pastry.dist.DistNodeHandle;
import rice.visualization.client.UpdateJarResponse;
import rice.visualization.data.*;

/**
 * @author jeffh
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ControlPanel extends JPanel implements ActionListener {
  
  protected Visualization visualization;

  JButton selectJarsButton;

  JPanel centerPanel;
  JList fileList; 
  JButton removeJarsButton;
  JButton debugCommandButton;
  GridBagLayout centerPanelLayout;

  JTextField commandLineField;  
  JButton updateJarsButton;

  JFileChooser chooser;
  DefaultListModel selectedFiles;
  protected Logger logger;
  
  /**
   * 
   */
  public ControlPanel(Visualization visualization) {
    this.visualization = visualization;
    logger = visualization.getEnvironment().getLogManager().getLogger(ControlPanel.class, null);

    setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Node Control"),
                                               BorderFactory.createEmptyBorder(5,5,5,5)));
    this.setLayout(new BorderLayout());
  
    centerPanel = new JPanel();
    centerPanelLayout = new GridBagLayout();
    centerPanel.setLayout(centerPanelLayout);

    selectedFiles = new DefaultListModel();
/*
    selectedFiles.addElement("one");
    selectedFiles.addElement("one afffasfdadsdafj;flkjafds;lajfasd asdf fadsfa falasdjfads fjas");
    selectedFiles.addElement("one asdfa");
    selectedFiles.addElement("one erwqrqw");
    selectedFiles.addElement("one yert");
    selectedFiles.addElement("oneasfdae3q");
    selectedFiles.addElement("one qwerqrew");
    selectedFiles.addElement("onesdajd");
    selectedFiles.addElement("oneqwer");
*/
    fileList = new JList(selectedFiles);

    selectJarsButton = new JButton("Add Jars/props");
    removeJarsButton = new JButton("Remove Jars");
    debugCommandButton = new JButton("Debug Command");
    updateJarsButton = new JButton("Update and Restart");
    updateJarsButton.setEnabled(false);
      
    JScrollPane jarsPane = new JScrollPane();
    jarsPane.getViewport().setView(fileList); 

    commandLineField = new JTextField();
    commandLineField.setBorder(
      BorderFactory.createCompoundBorder(
      BorderFactory.createTitledBorder("Command Line"),
      BorderFactory.createEmptyBorder(5,5,5,5))
    );

    int gridY = 0;

    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.BOTH;
    c.weightx = 1.0;
    c.weighty = 0.0;
    c.gridx = 0;
    
    c.gridy = gridY++;
    centerPanelLayout.setConstraints(selectJarsButton, c);
    centerPanel.add(selectJarsButton);

    c.gridy = gridY++;
    c.weighty = 4.0;
    centerPanelLayout.setConstraints(jarsPane, c);
    centerPanel.add(jarsPane);
    c.weighty = 0.0;

    c.gridy = gridY++;
    centerPanelLayout.setConstraints(removeJarsButton, c);
    centerPanel.add(removeJarsButton);

//    c.gridy = gridY++;
//    centerPanelLayout.setConstraints(commandLineField, c);
//    centerPanel.add(commandLineField);

    c.gridy = gridY++;
    centerPanelLayout.setConstraints(updateJarsButton, c);
    centerPanel.add(updateJarsButton);

    c.gridy = gridY++;
    centerPanelLayout.setConstraints(debugCommandButton, c);
    centerPanel.add(debugCommandButton);

    add(centerPanel,BorderLayout.CENTER);                  
  
    selectJarsButton.addActionListener(this);
    updateJarsButton.addActionListener(this);
    removeJarsButton.addActionListener(this);
    debugCommandButton.addActionListener(this);

    chooser = new JFileChooser();
    VizFileFilter filter = new VizFileFilter();
    chooser.setFileFilter(filter);
    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    chooser.setApproveButtonText("Add");
    chooser.setMultiSelectionEnabled(true);

  }

  class VizFileFilter extends FileFilter {
    public boolean accept(File arg0) {      
      return ((arg0.getName().endsWith(".jar")) ||
              (arg0.getName().endsWith(".params")));
    }

    public String getDescription() {
      return "Jar/Param Files";    
    }
  }

  public Dimension getPreferredSize() {
    return new Dimension(InformationPanel.INFORMATION_PANEL_WIDTH, InformationPanel.INFORMATION_PANEL_HEIGHT*2);
  }

  public void nodeSelected(Node node, Data data) {
    if (node != null) {
      updateJarsButton.setEnabled(true);      
    } else {
      updateJarsButton.setEnabled(false);
    }
  }

  public void actionPerformed(ActionEvent arg0) {
//    System.outt.println(arg0.getSource());
    if (arg0.getSource() == selectJarsButton) {
      int returnVal = chooser.showOpenDialog(this);
      if(returnVal == JFileChooser.APPROVE_OPTION) {
        addFiles(chooser.getSelectedFiles());
      }      
    }

    if (arg0.getSource() == removeJarsButton) {
      Object[] o = fileList.getSelectedValues();
      if (logger.level <= Logger.INFO) logger.log(
          "o.length"+o.length);
      if (o.length > 0) {
        removeFiles(o);
      } else {
        // TODO: Tell user to select files to delete
      }
    }

    if (arg0.getSource() == updateJarsButton) {
      int resp = JOptionPane.showConfirmDialog(this,"Are you sure you want to restart this node?","Restart Node",JOptionPane.YES_NO_OPTION);
      if (resp == JOptionPane.YES_OPTION) {
        int size = selectedFiles.getSize();
        File[] files = new File[size];
        for (int i = 0; i < size; i++) {
          files[i] = (File)selectedFiles.get(i);
        }
        String text = commandLineField.getText();
        if (text.equals("")) {
          text = null;
        }
        UpdateJarResponse ujr = visualization.updateJar(files,text);
        if (ujr.success()) {
          switch(ujr.getResponse()) {
            case UpdateJarResponse.OK:
              JOptionPane.showMessageDialog(this, "Transfer Successful, Restarting...",
                       "Success", JOptionPane.INFORMATION_MESSAGE);                  
              break;
            case UpdateJarResponse.FILE_COPY_NOT_ALLOWED:
              JOptionPane.showMessageDialog(this, "Not Allowed To Update Jars", "Error", JOptionPane.ERROR_MESSAGE);          
              break;
            case UpdateJarResponse.NEW_EXEC_NOT_ALLOWED:
              JOptionPane.showMessageDialog(this, "Not Allowed To Change Execution Command, Restarting...", "Warning", JOptionPane.WARNING_MESSAGE);          
              break;
          }
        } else {
          JOptionPane.showMessageDialog(this, ujr.getException(), "Error", JOptionPane.ERROR_MESSAGE);
        }      
      }
    }

    if (arg0.getSource() == debugCommandButton) {
      visualization.openDebugConsole();
    }
  }
  
  public void addFiles(File[] newFiles) {
    if (newFiles == null) return;
    for (int i = 0; i < newFiles.length; i++) {
      if (!selectedFiles.contains(newFiles[i])) {
        selectedFiles.addElement(newFiles[i]);    
      }
    }
  }

  public void removeFiles(Object[] newFiles) {
    if (logger.level <= Logger.INFO) logger.log(
        "removeFiles()");
    if (newFiles == null) return;
    for (int i = 0; i < newFiles.length; i++) {
      selectedFiles.removeElement((File)newFiles[i]);    
    }    
  }
}
