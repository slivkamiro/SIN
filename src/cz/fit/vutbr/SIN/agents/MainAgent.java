package cz.fit.vutbr.SIN.agents;

import jade.core.Agent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import cz.fit.vutbr.SIN.gui.MainWindow;

@SuppressWarnings("serial")
public class MainAgent extends Agent {
	
	private MainWindow gui;
	
	private AgentContainer carAgentsContainer;
	
	private List<String> events;
	
	protected void setup() {
		
		gui = new MainWindow(this);
		events = new ArrayList<String>();
		
		//crossRoadAgent = 
		
		Profile p = new ProfileImpl();
		carAgentsContainer = Runtime.instance().createAgentContainer(p);
		
		// Register the gui service in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("main-control");
		sd.setName("Main-service");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		
		addBehaviour(new CyclicBehaviour() {

			@Override
			public void action() {
				//System.out.println("MainAgent: listening");
				MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
				ACLMessage msg = myAgent.receive(mt);
				if (msg != null) {
					//System.out.println("MainAgent: message recieved");
					String[] content = msg.getContent().split(" ");
					if (content[0].equals("STATUS")) {
						events.add(content[1].replaceAll("_", " "));
						gui.appendEvents(events);
						events.clear();
					} else if (content[0].equals("SEM_SWITCH")) {
						gui.switchSemaphores();
					} else if (content[0].equals("UPDATE_Q")) {
						gui.updateQueue(CarAgent.NORTH, Integer.parseInt(content[1]));
						gui.updateQueue(CarAgent.SOUTH, Integer.parseInt(content[2]));
						gui.updateQueue(CarAgent.WEST, Integer.parseInt(content[3]));
						gui.updateQueue(CarAgent.EAST, Integer.parseInt(content[4]));
					} else if (content[0].equals("CAR_DIRECT")) {
						gui.setArrowOnTo(Integer.parseInt(content[1]), Integer.parseInt(content[2]));
					}
				} else {
					block();
				}
				
			}
			
		});
		gui.showGui();
	}
	
	public void takeDown() {
		try {
			DFService.deregister(this);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
		
		gui.dispose();
		
	}

	public void spawnCars(int num) {
		spawnCars(num,"Car");
	}

	public void spawnCars(final int num, final String prefix) {
		for(int i = 0; i < num; i++) {
			final Integer n = new Integer(i);
			addBehaviour(new WakerBehaviour(this, (long) (Math.random()*50000+1)) {
	
				@Override
				public void onWake() {
					try {						
	//						String[] direction = { myAgent.getAID().getLocalName(),
	//												(int) (Math.random()*4+1)+"",
	//												(int) (Math.random()*4+1)+""
	//												};		
						// DEBUG - NORT SOUTH should be green initialy
						int src = (int) (Math.random()*2+1);
						int dst = (int) (Math.random()*4+1);
						if (dst == src) {
							dst = dst == 4 ? dst-1 : dst+1;
						}
						String[] direction = { src+"", dst+"" };			
						AgentController agent = carAgentsContainer.createNewAgent(prefix+"#"+n, CarAgent.class.getCanonicalName(), direction);
						agent.start();
						Calendar cal = Calendar.getInstance();
						cal.getTime();
						SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
						events.add("MainAgent "+sdf.format(cal.getTime())+" :"+prefix+"#"+n+" dispatched\n");
					} catch (StaleProxyException e) {
						events.add("MainAgent: mhd car "+prefix+"#"+n+"failed to dispatch\n");
						e.printStackTrace();
					}
				}
				
			});
		}
		
		
	}

	public void startSimulation() {
		//addBehaviour(new CrossroadControlBehaviour());
		
	}

}
