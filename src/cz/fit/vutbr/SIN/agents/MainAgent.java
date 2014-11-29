package cz.fit.vutbr.SIN.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

import java.util.ArrayList;
import java.util.List;

import cz.fit.vutbr.SIN.gui.MainWindow;

public class MainAgent extends Agent {
	
	// Semaphore colors
	public static final int GREEN = 6;
	public static final int RED = 7;
	
	private int semaphoreNorth = GREEN;
	private int semaphoreSouth = GREEN;
	private int semaphoreEast = RED;
	private int semaphoreWest = RED;
	
	// Queues on semaphores
	private List<AID> qN;
	private List<AID> qS;
	private List<AID> qE;
	private List<AID> qW;
	
	// These flags says if there is something in crossroad in two directions
	private boolean centerFull = false;
	private boolean direction1 = false;
	private boolean direction2 = false;
	
	private MainWindow gui;
	
	private AgentContainer carAgentsContainer;
	
	private List<String> events;
	
	protected void setup() {
		
		gui = new MainWindow(this);
		events = new ArrayList<String>();
		
		Profile p = new ProfileImpl();
		carAgentsContainer = Runtime.instance().createAgentContainer(p);
		
		// Register the crossroad service in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("crossroad-control");
		sd.setName("CrossRoad-service");
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
				if(msg != null) {
					//System.out.println("MainAgent: message recieved");
					events.add(msg.getContent());
					gui.appendEvents(events);
					events.clear();
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
		spawnCars(num,"");
	}

	public void spawnCars(final int num, final String prefix) {
		addBehaviour(new OneShotBehaviour() {

			@Override
			public void action() {
				for (int i = 0; i < num; i++) {
					try {						
						String[] direction = { myAgent.getAID().getLocalName(),
												(int) (Math.random()*4+1)+"",
												(int) (Math.random()*4+1)+""
												};			
						AgentController agent = carAgentsContainer.createNewAgent(prefix+"#"+i, CarAgent.class.getCanonicalName(), direction);
						agent.start();
						events.add("MainAgent: car "+prefix+"#"+i+"dispatched\n");
					} catch (StaleProxyException e) {
						events.add("MainAgent: mhd car "+prefix+"#"+i+"failed to dispatch\n");
						e.printStackTrace();
					}
				}
			}
			
		});
		
		
	}

	public void startSimulation() {
		//addBehaviour(new CrossroadControlBehaviour());
		
	}
	
	private class CrossroadControlBehaviour extends Behaviour {

		@Override
		public void action() {
			ACLMessage msg = myAgent.receive();
			if(msg != null) {
				ACLMessage reply = msg.createReply();
				if(msg.getPerformative() == ACLMessage.REQUEST) {
					// Parse request
					String[] msgContent = msg.getContent().split(" ");
					if(msgContent[0].equals("GET_SEM")) {
						// Get semaphore light
						Integer s = Integer.parseInt(msgContent[1]);
						Integer semLight = getLightOf(s);
						if(semLight == RED) {
							//addToSemaphoreQueue(s,reply);
						} else {
							// send green
						}
					}
				}
			}
			
		}

		@Override
		public boolean done() {
			// TODO Auto-generated method stub
			return false;
		}
		
	}

	public Integer getLightOf(Integer s) {
		if(s == CarAgent.NORTH) {
			return semaphoreNorth;
		} else if(s == CarAgent.SOUTH) {
			return semaphoreSouth;
		} else if(s == CarAgent.WEST) {
			return semaphoreWest;
		} else {
			return semaphoreEast;
		}
	}

}
