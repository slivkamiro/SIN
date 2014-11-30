package cz.fit.vutbr.SIN.agents;

import java.util.EnumMap;
import java.util.List;
import java.util.Queue;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.util.leap.ArrayList;
import jade.util.leap.LinkedList;

public class CrossRoadAgent extends Agent {
	// Semaphore colors
	public static final int GREEN = 6;
	public static final int RED = 7;
	
	private int semaphoreNorth = GREEN;
	private int semaphoreSouth = GREEN;
	private int semaphoreEast = RED;
	private int semaphoreWest = RED;
	
	private int CAPACITY = 1;
	
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
	
	private List<Queue<ACLMessage>> queues;
	private List< Queue<ACLMessage>> innerQueues;
	
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
		
		// Get  service
		try { Thread.sleep(1000); } catch (Exception e) {}
		getService("main-control");
		
		// Periodically send statistics
		addBehaviour(new TickerBehaviour(this, 5000){

			@Override
			protected void onTick() {
				switchColor();
			}
			
		});
				
		addBehaviour(new CrossroadControlBehaviour());
	}
	
	private void switchColor () {
		semaphoreNorth =  (semaphoreNorth == RED) ? GREEN : RED;
		semaphoreSouth =  (semaphoreSouth == RED) ? GREEN : RED;
		semaphoreWest  =  (semaphoreWest == RED) ? GREEN : RED;
		semaphoreEast  =  (semaphoreEast == RED) ? GREEN : RED;
	
		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		msg.addReceiver(mainControlService);
		msg.setContent("SEM_SWITCH");
		send(msg);
		
		// DEBUG
		if (semaphoreNorth == GREEN){
			System.out.println("[XROAD] SOUTH-NORTH opened");
		}
		else {
			System.out.println("[XROAD] EAST-WEST opened");
		}
	}
	
	private void initQueues() {
		// Queues on semaphores - TODO: refator semaphores to array, remake
		// direction constant in car agent to enum and simplify semaph. methods
		qN = new java.util.LinkedList<ACLMessage>();
		qS = new java.util.LinkedList<ACLMessage>();
		qE = new java.util.LinkedList<ACLMessage>();
		qW = new java.util.LinkedList<ACLMessage>();
		
		queues =  new java.util.ArrayList<Queue<ACLMessage>>();
		queues.add(qN);
		queues.add(qS);
		queues.add(qE);
		queues.add(qW);
		
		// inner crossroad queues by direction ( for now there is only one lane for direction)
		qNin = new java.util.LinkedList<ACLMessage>();
		qSin = new java.util.LinkedList<ACLMessage>();
		qEin = new java.util.LinkedList<ACLMessage>();
		qWin = new java.util.LinkedList<ACLMessage>();
	
		innerQueues =  new java.util.ArrayList<Queue<ACLMessage>>();
		innerQueues.add(qNin);
		innerQueues.add(qSin);
		innerQueues.add(qEin);
		innerQueues.add(qWin);

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
				AID sender = msg.getSender();
				if (msg.getPerformative() == ACLMessage.REQUEST) {
					System.out.println("request received");
					// Parse request
					String[] msgContent = msg.getContent().split(" ");
					if (msgContent[0].equals("GET_SEM")) {
						// Get semaphore light
						Integer s = Integer.parseInt(msgContent[1]);
						Integer semLight = getLightOf(s);
						if (semLight == RED) {
							debugLog(sender, "Added to queue");
							addToSemaphoreQueue(s, reply);
						} else {
							// check inner state of crossroad
							if (!isSemaphoreEmpty(s) || !isInnerQueueForDirectionEmpty(s)) {
								debugLog(sender, "Added to queue");
								addToSemaphoreQueue(s, reply);
							}
							else {
								//TODO: we dont care what is in inner queue - we only need to know its full
								debugLog(sender, "Moved to inner queue");
								ACLMessage tmp = queues.get(s).poll();
								addToInnerDirectionQueue(s, tmp);
								reply.setPerformative(ACLMessage.CONFIRM);
								myAgent.send(reply);						
							}							
						}
					}
					else if (msgContent[0].equals("IN_CR")) {
						Integer src = Integer.parseInt(msgContent[1]);
						Integer dst = Integer.parseInt(msgContent[2]);
						debugLog(sender, dirToStr(src)+" -> "+dirToStr(dst)+ " entered inner queue");
						if (isDstForward(src,dst) || isDstRight(src,dst)) {
							reply.setPerformative(ACLMessage.CONFIRM);
							myAgent.send(reply);
							// remove from inner front
							debugLog(sender, "Removed from inner queue");
							innerQueues.get(src).poll();
						}
						else { // turning left
							debugLog(sender, " going left");
//							check oppsite inner queue
//							prepare message go = ked uz tam nejaka je tak tu pustime 
							reply.setPerformative(ACLMessage.CONFIRM);
							myAgent.send(reply);
							// remove from inner front
							debugLog(sender, "Removed from inner queue");
							innerQueues.get(src).poll();
						}
					}
				}
				else { // DEBUG branch
					System.out.println("XROAD received not a request: " + msg.getContent());
				}
			}
			
			
			// Standard crossroad controll
			simulateCrossroad();	
			
			
		}
		
	}
	
	private void simulateCrossroad() {
	
		if ( semaphoreNorth == GREEN ) {
			// NORTH - SOUTH open
			simulate(CarAgent.NORTH);
			simulate(CarAgent.SOUTH);
		}
		else {
			// EAST - WEST open
			simulate(CarAgent.EAST);
			simulate(CarAgent.WEST);
		}
	}
	
	private void simulate(Integer s) {
		if (!isSemaphoreEmpty(s) && isInnerQueueAvailable(s)) {
			debugLog(" Adding car to inner queue from direction " +  dirToStr(s));
			//addToSemaphoreQueue(s, reply);
		}
	}
	
	private boolean isInnerQueueAvailable(Integer s) {
		return innerQueues.get(s).size() <= CAPACITY;
	}
	
	private String dirToStr(Integer s) {
		if ( s == CarAgent.NORTH ) {
			return "NORTH";
		} else if (s == CarAgent.SOUTH) {
			return "SOUTH";
		} else if (s == CarAgent.WEST) {
			return "WEST";
		} else if (s == CarAgent.EAST) {
			return "EAST";
		}
		
		return "UNKNOWN";
	}
	
	void debugLog(AID aid, String msg) {
		System.out.println("[XROAD] " + aid + ":" + msg);
	}
	
	void debugLog(String msg) {
		System.out.println("[XROAD]: " + msg);
	}
	
	private boolean isDstForward(Integer src, Integer dst) {
		if (src == CarAgent.NORTH && dst == CarAgent.SOUTH ||
			src == CarAgent.SOUTH && dst == CarAgent.NORTH ||
			src == CarAgent.WEST && dst == CarAgent.EAST ||
			src == CarAgent.EAST && dst == CarAgent.WEST ) {
				return true;
			}
			else { return false; }
		}
	
	private boolean isDstRight(Integer src, Integer dst) {
		if (src == CarAgent.NORTH && dst == CarAgent.WEST ||
			src == CarAgent.SOUTH && dst == CarAgent.EAST ||
			src == CarAgent.WEST && dst == CarAgent.SOUTH ||
			src == CarAgent.EAST && dst == CarAgent.NORTH ) {
			return true;
		}
		else { return false; }
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
