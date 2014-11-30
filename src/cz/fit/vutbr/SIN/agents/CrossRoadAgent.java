package cz.fit.vutbr.SIN.agents;

import java.util.EnumMap;
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
import jade.util.leap.LinkedList;

public class CrossRoadAgent extends Agent {
	// Semaphore colors
	public static final int GREEN = 6;
	public static final int RED = 7;
	
	private int semaphoreNorth = GREEN;
	private int semaphoreSouth = GREEN;
	private int semaphoreEast = RED;
	private int semaphoreWest = RED;
	
	// Queues on semaphores - TODO: refator semaphores to array, remake
	// direction constant in car agent to enum and simplify semaph. methods
	private Queue<ACLMessage> qN;
	private Queue<ACLMessage> qS;
	private Queue<ACLMessage> qE;
	private Queue<ACLMessage> qW;
		
	// inner crossroad queues by direction ( for now there is only one lane for direction)
	private Queue<ACLMessage> qNin;
	private Queue<ACLMessage> qSin;
	private Queue<ACLMessage> qEin;
	private Queue<ACLMessage> qWin;
	
	private EnumMap<CarAgent.Direction, Queue<ACLMessage>> queues;
	private EnumMap<CarAgent.Direction, Queue<ACLMessage>> innerQueues;
	
	// These flags says if there is something in crossroad in two directions
	private boolean centerFull = false;
	private boolean direction1 = false;
	private boolean direction2 = false;
	
	private AID mainControlService;

	protected void setup() {
		
		initQueues();
		
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
	
	private void initQueues() {
		// Queues on semaphores - TODO: refator semaphores to array, remake
		// direction constant in car agent to enum and simplify semaph. methods
		qN = new java.util.LinkedList<ACLMessage>();
		qS = new java.util.LinkedList<ACLMessage>();
		qE = new java.util.LinkedList<ACLMessage>();
		qW = new java.util.LinkedList<ACLMessage>();
		
		queues =  new EnumMap<CarAgent.Direction, Queue<ACLMessage>>(CarAgent.Direction.class);
		queues.put(CarAgent.Direction.NORTH, qN);
		queues.put(CarAgent.Direction.SOUTH, qS);
		queues.put(CarAgent.Direction.EAST, qE);
		queues.put(CarAgent.Direction.WEST, qW);
		
		// inner crossroad queues by direction ( for now there is only one lane for direction)
		qNin = new java.util.LinkedList<ACLMessage>();
		qSin = new java.util.LinkedList<ACLMessage>();
		qEin = new java.util.LinkedList<ACLMessage>();
		qWin = new java.util.LinkedList<ACLMessage>();
	
		innerQueues =  new EnumMap<CarAgent.Direction, Queue<ACLMessage>>(CarAgent.Direction.class);
		queues.put(CarAgent.Direction.NORTH, qNin);
		queues.put(CarAgent.Direction.SOUTH, qSin);
		queues.put(CarAgent.Direction.EAST, qEin);
		queues.put(CarAgent.Direction.WEST, qWin);

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
							if (!isSemaphoreEmpty(s) || !isInnerQueueForDirectionEmpty(s)) {
								addToSemaphoreQueue(s, reply);
							}
							else {
								addToInnerDirectionQueue(s, reply);
								reply.setPerformative(ACLMessage.CONFIRM);
								myAgent.send(reply);						
							}							
						}
					}
//					else if (msgContent[0].equals("IN_CR")) {
//						if (msgContent[1].equals("IN_CR")
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
	
	private boolean isSemaphoreEmpty(Integer s) {
//		if ( s == CarAgent.NORTH ) {
//			return qN.isEmpty();
//		} else if (s == CarAgent.SOUTH) {
//			return qS.isEmpty();
//		} else if (s == CarAgent.WEST) {
//			return qW.isEmpty();
//		} else if (s == CarAgent.EAST) {
//			return qE.isEmpty();
//		}		
		
		return queues.get(s).isEmpty();
	}
	
	private void addToSemaphoreQueue(Integer s, ACLMessage reply) {
//		if ( s == CarAgent.NORTH ) {
//			qN.add(reply);
//		} else if (s == CarAgent.SOUTH) {
//			qS.add(reply);
//		} else if (s == CarAgent.WEST) {
//			qW.add(reply);
//		} else if (s == CarAgent.EAST) {
//			qE.add(reply);
//		}
		
		queues.get(s).add(reply);
	}
	
	private boolean isInnerQueueForDirectionEmpty(Integer s) {
//		if ( s == CarAgent.NORTH ) {
//			return qNin.isEmpty();
//		} else if (s == CarAgent.SOUTH) {
//			return qSin.isEmpty();
//		} else if (s == CarAgent.WEST) {
//			return qWin.isEmpty();
//		} else if (s == CarAgent.EAST) {
//			return qEin.isEmpty();
//		}		

		return innerQueues.get(s).isEmpty();	
	}
	
	private void addToInnerDirectionQueue(Integer s, ACLMessage reply){
//		if ( s == CarAgent.NORTH ) {
//			qNin.add(reply);
//		} else if (s == CarAgent.SOUTH) {
//			qSin.add(reply);
//		} else if (s == CarAgent.WEST) {
//			qWin.add(reply);
//		} else if (s == CarAgent.EAST) {
//			qEin.add(reply);
//		}

		innerQueues.get(s).add(reply);	

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
