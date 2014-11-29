package cz.fit.vutbr.SIN.agents;

import java.util.List;
import java.util.Queue;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

public class CrossRoadAgent extends Agent {
	// Semaphore colors
	public static final int GREEN = 6;
	public static final int RED = 7;
	
	private int semaphoreNorth = GREEN;
	private int semaphoreSouth = GREEN;
	private int semaphoreEast = RED;
	private int semaphoreWest = RED;
	
	// Queues on semaphores
	private Queue<ACLMessage> qN;
	private Queue<ACLMessage> qS;
	private Queue<ACLMessage> qE;
	private Queue<ACLMessage> qW;
	
	// inner queue
	private Queue<AID> qInner;
	
	// These flags says if there is something in crossroad in two directions
	private boolean centerFull = false;
	private boolean direction1 = false;
	private boolean direction2 = false;
	
	private AID mainControlService;

	protected void setup() {
		
		System.out.println(getAID().getLocalName()+" agent raises from hell!");
		
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
		
		// Get  services
		getService("main-control");
				
		addBehaviour(new CrossroadControlBehaviour());
	}
	
	
	private void getService( String serviceType) {

		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType(serviceType);
		template.addServices(sd);
		try {
			DFAgentDescription[] result = DFService.search(this, template);
			mainControlService = result[0].getName();
		} catch (FIPAException fe) {
			fe.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private class CrossroadControlBehaviour extends CyclicBehaviour {

		@Override
		public void action() {
			ACLMessage msg = myAgent.receive();
			if (msg != null) {
				ACLMessage reply = msg.createReply();
				if (msg.getPerformative() == ACLMessage.REQUEST) {
					// Parse request
					String[] msgContent = msg.getContent().split(" ");
					if (msgContent[0].equals("GET_SEM")) {
						// Get semaphore light
						Integer s = Integer.parseInt(msgContent[1]);
						Integer semLight = getLightOf(s);
						if (semLight == RED) {
							addToSemaphoreQueue(s, reply);
						} else {
							// check inner state of crossroad
//							if (!qS.isEmpty) enqueue qS
//							else if !q.InnerS() enqueue qS
//							else 
//								enqueque qInnerS
//								response inform message mozes is nuka							
						}
					}
//					else if (msgContent[0].equals("IN_CR")) {
//						parsujem src, dst [1], [2]
//						if (dst == ROVNO || doprava) 
//							reply GO
//						else check oppsite inner queue
//							prepare message go = ked uz tam nejaka je tak tu pustime 
//					}
				}
				else { // DEBUG branch
					System.out.println("XROAD received not a request: " + msg.getContent());
				}
			}
			
		}
		
	}
	
	private void addToSemaphoreQueue(Integer s, ACLMessage reply) {
		if ( s == CarAgent.NORTH ) {
			qN.add(reply);
		} else if (s == CarAgent.SOUTH) {
			qS.add(reply);
		} else if (s == CarAgent.WEST) {
			qW.add(reply);
		} else if (s == CarAgent.EAST) {
			qE.add(reply);
		}
	}
	
	private Integer getLightOf(Integer s) {
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
