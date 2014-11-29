package cz.fit.vutbr.SIN.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import cz.fit.vutbr.SIN.agents.MainAgent;
import javax.swing.JScrollPane;

public class MainWindow extends JFrame {

	private MainAgent mainAgent;
	private JTextField numCars;
	private JTextField numMhd;
	private JTextArea eventsArea;
	
	public MainWindow(MainAgent a) {
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
		eventsArea.setColumns(20);
		scrollPane.setViewportView(eventsArea);
		
		mainAgent = a;
		
	}
	
	public void appendEvents(List<String> events) {
		for(String event : events) {
			eventsArea.setText(eventsArea.getText()+event);
		}
		eventsArea.repaint();
	}

	public void showGui() {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int centerX = (int)screenSize.getWidth() / 2;
		int centerY = (int)screenSize.getHeight() / 2;
		setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2);
		this.setSize(new Dimension(750,450));
		super.setVisible(true);
		
	}
}
