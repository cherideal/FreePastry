package rice.visualization;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import rice.visualization.render.*;
import rice.pastry.dist.*;

public class VisualizationFrame extends JFrame {
  
  protected Visualization visualization;
    
  protected PastryRingPanel pastryRingPanel;

  protected PastryNodePanel pastryNodePanel;
  
  protected InformationPanel informationPanel;
  
  protected VisualizationFrame(Visualization visualization) {
    super("Pastry Network Visualization"); 
    
    this.visualization = visualization;
    
    GridBagLayout layout = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
    
    getContentPane().setLayout(layout);
    
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    
    ViewRendererFactory factory = new ViewRendererFactory();
    factory.addRenderer(new TableViewRenderer(visualization));
    factory.addRenderer(new KeyValueListViewRenderer(visualization));
    factory.addRenderer(new LineGraphViewRenderer(visualization));
    factory.addRenderer(new PieChartViewRenderer(visualization));
        
    pastryRingPanel = new PastryRingPanel(visualization);
    c.gridx = 0;
    c.gridy = 0;
    layout.setConstraints(pastryRingPanel, c);
    getContentPane().add(pastryRingPanel);
    
    c = new GridBagConstraints();
    
    pastryNodePanel = new PastryNodePanel(visualization, factory);
    c.gridx = 0;
    c.gridy = 1;
    c.gridwidth = 2;
    layout.setConstraints(pastryNodePanel, c);
    getContentPane().add(pastryNodePanel); 
    
    c = new GridBagConstraints();
    
    informationPanel = new InformationPanel(visualization);
    c.gridx = 1;
    c.gridy = 0;
    layout.setConstraints(informationPanel, c);
    getContentPane().add(informationPanel); 
    
    //Display the window.
    pack();
    setVisible(true);
  }
  
  public void nodeHighlighted(DistNodeHandle node) {
    pastryRingPanel.nodeHighlighted(node);
  }
  
  public void nodeSelected(DistNodeHandle node) {
    informationPanel.nodeSelected(node);
    pastryRingPanel.nodeSelected(node);
    pastryNodePanel.nodeSelected(node);
  }
}