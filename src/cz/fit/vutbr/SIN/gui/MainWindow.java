package cz.fit.vutbr.SIN.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.NoninvertibleTransformException;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import cz.fit.vutbr.SIN.agents.CarAgent;
import cz.fit.vutbr.SIN.agents.MainAgent;

public class MainWindow extends JFrame {

	private MainAgent mainAgent;
	private JTextField numCars;
	private JTextField numMhd;
	
	private JTextArea eventsArea;
	
	private JPanel crossroadPanel;
	
	private Color NS = Color.GREEN;
	private Color WE = Color.RED;
	
	private JLabel lblNorthQ;
	private JLabel lblSouthQ;
	private JLabel lblWestQ;
	private JLabel lblEastQ;
	
	private JLabel sourceNorth;
	private JLabel sourceSouth;
	private JLabel sourceWest;
	private JLabel sourceEast;
	
	private ImageIcon iconNS;
	private ImageIcon iconNW;
	private ImageIcon iconNE;
	private ImageIcon iconSN;
	private ImageIcon iconSW;
	private ImageIcon iconSE;
	private ImageIcon iconEW;
	private ImageIcon iconES;
	private ImageIcon iconEN;
	private ImageIcon iconWE;
	private ImageIcon iconWS;
	private ImageIcon iconWN;
	
	public MainWindow(MainAgent a) {
		
		iconNS = new ImageIcon(getClass().getResource("resources/NS.png"));
		iconNW = new ImageIcon(getClass().getResource("resources/NW.png"));
		iconNE = new ImageIcon(getClass().getResource("resources/NE.png"));
		iconSN = new ImageIcon(getClass().getResource("resources/SN.png"));
		iconSW = new ImageIcon(getClass().getResource("resources/SW.png"));
		iconSE = new ImageIcon(getClass().getResource("resources/SE.png"));
		iconES = new ImageIcon(getClass().getResource("resources/ES.png"));
		iconEW = new ImageIcon(getClass().getResource("resources/EW.png"));
		iconEN = new ImageIcon(getClass().getResource("resources/EN.png"));
		iconWS = new ImageIcon(getClass().getResource("resources/WS.png"));
		iconWN = new ImageIcon(getClass().getResource("resources/WN.png"));
		iconWE = new ImageIcon(getClass().getResource("resources/WE.png"));
		
		setTitle("Super Duper krizovatka");
		getContentPane().setLayout(new BorderLayout(0, 0));
		
		JSplitPane splitPane = new JSplitPane();
		splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		splitPane.setDividerLocation(30);
		getContentPane().add(splitPane, BorderLayout.CENTER);
		
		JPanel panel = new JPanel();
		splitPane.setLeftComponent(panel);
		panel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		JLabel lblCars = new JLabel("Cars:");
		panel.add(lblCars);
		
		numCars = new JTextField();
		panel.add(numCars);
		numCars.setColumns(10);
		
		JLabel lblMhd = new JLabel("MHD:");
		panel.add(lblMhd);
		
		numMhd = new JTextField();
		panel.add(numMhd);
		numMhd.setColumns(10);
		
		JButton btnRun = new JButton("Run!");
		panel.add(btnRun);
		btnRun.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				mainAgent.spawnCars(Integer.parseInt(numCars.getText()));
				mainAgent.spawnCars(Integer.parseInt(numMhd.getText()),"MHD_");
				mainAgent.startSimulation();
				
			}
			
		});
		
		JPanel panel_1 = new JPanel();
		splitPane.setRightComponent(panel_1);
		panel_1.setLayout(new BorderLayout(0, 0));
		
		JScrollPane scrollPane = new JScrollPane();
		panel_1.add(scrollPane, BorderLayout.EAST);
		
		eventsArea = new JTextArea();
		eventsArea.setEditable(false);
		eventsArea.setColumns(25);
		scrollPane.setViewportView(eventsArea);
		
		crossroadPanel = new JPanel(){
			
			private void drawArrow(Graphics2D g2, int x1,int y1,int x2,int y2) {
				// length of the line
				double length = Math.sqrt((x1-x2)*(x1-x2) + (y1-y2)*(y1-y2));
				// angle with x
				double angle = Math.atan2(y2-y1, x2-x1);
				
				// arrow
				GeneralPath path = new GeneralPath();
				path.moveTo((float)length, 0);
				path.lineTo((float)length - 10, -5);
				path.lineTo((float)length - 7, 0);
				path.lineTo((float)length - 10, 5);
				path.lineTo((float)length, 0);
				path.closePath();
				
				try {
					AffineTransform af = AffineTransform.getTranslateInstance(x1, y1);
					af.concatenate(AffineTransform.getRotateInstance(angle));
					g2.transform(af);
					
					Area area = new Area(path);
					g2.fill(area);
				
					af.invert();
					g2.transform(af);
				} catch (NoninvertibleTransformException e) {
					// TODO: what to do here?
					e.printStackTrace();
				}
			}
			
			@Override
			public void paintComponent(Graphics g) {
				super.paintComponent(g);

				Graphics2D g2d = (Graphics2D) g;
				
				// drawing route lines
				g2d.drawLine(0, 200, 180, 200);
				drawArrow(g2d,180,200,0,200);
				g2d.drawLine(0, 220, 80, 220);
				// queue on W semaphore
				g2d.draw(new Ellipse2D.Double(80,210,20,20));
				g2d.drawLine(100, 220, 180, 220);
				drawArrow(g2d, 100, 220, 180, 220);
				
				g2d.drawLine(300, 200, 380, 200);
				drawArrow(g2d, 380, 200, 300, 200);
				// queue on E semaphore
				g2d.draw(new Ellipse2D.Double(380,190,20,20));
				g2d.drawLine(400, 200, 500, 200);
				g2d.drawLine(300, 220, 500, 220);
				drawArrow(g2d, 300, 220, 500, 220);
				
				g2d.drawLine(230,0,230,80);
				// queue on N semaphore
				g2d.draw(new Ellipse2D.Double(220,80,20,20));
				g2d.drawLine(230, 100, 230, 150);
				drawArrow(g2d,230, 100, 230, 150);
				g2d.drawLine(250, 0, 250, 150);
				drawArrow(g2d,250, 150, 250, 0);
				
				g2d.drawLine(230,270,230,420);
				drawArrow(g2d,230,270,230,420);
				g2d.drawLine(250,270,250,320);
				drawArrow(g2d,250,320,250,270);
				// queue on S semaphore
				g2d.draw(new Ellipse2D.Double(240, 320, 20, 20));
				g2d.drawLine(250, 340, 250, 420);
				
				// draw semaphores
				g2d.setColor(WE);
				// WE semaphores
				g2d.fillRect(180, 170, 20, 80);
				g2d.fillRect(280, 170, 20, 80);
				g2d.setColor(NS);
				// NS semaphores
				g2d.fillRect(200, 150, 80, 20);
				g2d.fillRect(200, 250, 80, 20);
			}
		};
		panel_1.add(crossroadPanel, BorderLayout.CENTER);
		crossroadPanel.setLayout(null);
		
		lblNorthQ = new JLabel("0");
		lblNorthQ.setBounds(225, 80, 20, 20);
		crossroadPanel.add(lblNorthQ);
		
		lblSouthQ = new JLabel("0");
		lblSouthQ.setBounds(245, 320, 20, 20);
		crossroadPanel.add(lblSouthQ);
		
		lblWestQ = new JLabel("0");
		lblWestQ.setBounds(85, 210, 20, 20);
		crossroadPanel.add(lblWestQ);
		
		lblEastQ = new JLabel("0");
		lblEastQ.setBounds(385, 190, 20, 20);
		crossroadPanel.add(lblEastQ);
		
		sourceNorth = new JLabel("");
		sourceNorth.setBounds(200, 170, 40, 40);
		crossroadPanel.add(sourceNorth);
		
		sourceEast = new JLabel("");
		sourceEast.setBounds(240,170,40,40);
		crossroadPanel.add(sourceEast);
		
		sourceSouth = new JLabel("");
		sourceSouth.setBounds(240,210,40,40);
		crossroadPanel.add(sourceSouth);
		
		sourceWest = new JLabel("");
		sourceWest.setBounds(200,210,40,40);
		crossroadPanel.add(sourceWest);
		
		mainAgent = a;
		
	}
	
	public void switchSemaphores() {
			NS = NS == Color.GREEN ? Color.RED : Color.GREEN;
			WE = WE == Color.GREEN ? Color.RED : Color.GREEN;
			sourceNorth.setIcon(null);
			sourceSouth.setIcon(null);
			sourceWest.setIcon(null);
			sourceEast.setIcon(null);
			crossroadPanel.repaint();
	}
	
	public void updateQueue(Integer source,Integer val) {
		if (source == CarAgent.EAST) {
			lblEastQ.setText(val.toString());
		} else if (source == CarAgent.WEST) {
			lblWestQ.setText(val.toString());
		} else if (source == CarAgent.NORTH) {
			lblNorthQ.setText(val.toString());
		} else {
			lblSouthQ.setText(val.toString());
		}
	}
	
	public void setArrowOnTo(Integer source, Integer destination) {
		
		if (source == CarAgent.NORTH) {
			if (destination == CarAgent.EAST) {
				sourceNorth.setIcon(iconNE);
			} else if(destination == CarAgent.WEST) {
				sourceNorth.setIcon(iconNW);
			} else {
				sourceNorth.setIcon(iconNS);
			}
		} else if (source == CarAgent.SOUTH) {
			if (destination == CarAgent.EAST) {
				sourceSouth.setIcon(iconSE);
			} else if(destination == CarAgent.WEST) {
				sourceSouth.setIcon(iconSW);
			} else {
				sourceSouth.setIcon(iconSN);
			}
		} else if (source == CarAgent.EAST) {
			if (destination == CarAgent.NORTH) {
				sourceEast.setIcon(iconEN);
			} else if(destination == CarAgent.SOUTH) {
				sourceEast.setIcon(iconES);
			} else {
				sourceEast.setIcon(iconEW);
			}
		} else if (source == CarAgent.WEST) {
			if (destination == CarAgent.NORTH) {
				sourceWest.setIcon(iconWN);
			} else if(destination == CarAgent.SOUTH) {
				sourceWest.setIcon(iconWS);
			} else {
				sourceWest.setIcon(iconWE);
			}
		}
		
	}

	public void appendEvents(List<String> events) {
		for (String event : events) {
			eventsArea.setText(eventsArea.getText()+event);
		}
		eventsArea.repaint();
	}

	public void showGui() {
		this.setSize(new Dimension(802,502));
		super.setVisible(true);
		
	}
}
