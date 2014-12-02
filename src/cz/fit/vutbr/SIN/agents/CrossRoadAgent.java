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
	
	// Information needed for giving way
	private ACLMessage leftTurn = null;
	private Integer waitingIn = null;
	
	private AID mainControlService;

	// direction where MHD was spotted - no mhd == -1
	Integer mhdAppearance = -1;
	
	// default semaphore timeout
	long semTimeout = 15000;

    // time since last color switch
	long lastSwitchTime = System.currentTimeMillis();
	
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
		getService("main-control");
		
		// Periodically send statistics
		addBehaviour(new CyclicBehaviour(){

			@Override
			public void action() {
				long currentTime = System.currentTimeMillis();
				if (currentTime - lastSwitchTime >= semTimeout){
					switchColor();
				}
			}
			
		});
				
		addBehaviour(new CrossroadControlBehaviour());

		// mhd handler
		addBehaviour(new CyclicBehaviour() {
			@Override
			public void action() {
				if (mhdAppearance >= 0){
					if (getLightOf(mhdAppearance) != GREEN){
						switchColor();
						mhdAppearance = -1;
					}
				}
			}
		});

		
		// North traffic
		addBehaviour(new CyclicBehaviour() {

			@Override
			public void action() {
				if (semaphoreNorth == GREEN 
						&& !isSemaphoreEmpty(CarAgent.NORTH) 
						&& isInnerQueueAvailable(CarAgent.NORTH)) {
					debugLog(" Adding car to inner queue from direction " +  dirToStr(CarAgent.NORTH));
					ACLMessage reply = queues.get(CarAgent.NORTH).poll();
					addToInnerDirectionQueue(CarAgent.NORTH, reply);
					reply.setPerformative(ACLMessage.CONFIRM);
					myAgent.send(reply);
					sendStatusToMainControl();
				}
				if(waitingIn == CarAgent.NORTH) {
					myAgent.send(leftTurn);
					innerQueues.get(waitingIn).poll();
					debugLog("Removed from inner queue "+dirToStr(waitingIn));
					leftTurn = null;
					waitingIn = null;
				}
			}
			
		});
		// South traffic
		addBehaviour(new CyclicBehaviour() {

			@Override
			public void action() {
				if (semaphoreSouth == GREEN 
						&& !isSemaphoreEmpty(CarAgent.SOUTH) 
						&& isInnerQueueAvailable(CarAgent.SOUTH)) {
					debugLog(" Adding car to inner queue from direction " +  dirToStr(CarAgent.SOUTH));
					ACLMessage reply = queues.get(CarAgent.SOUTH).poll();
					addToInnerDirectionQueue(CarAgent.SOUTH, reply);
					reply.setPerformative(ACLMessage.CONFIRM);
					myAgent.send(reply);
					sendStatusToMainControl();
				}
				if(waitingIn == CarAgent.SOUTH) {
					myAgent.send(leftTurn);
					innerQueues.get(waitingIn).poll();
					debugLog("Removed from inner queue "+dirToStr(waitingIn));
					leftTurn = null;
					waitingIn = null;
				}
			}
			
		});
		// West traffic
		addBehaviour(new CyclicBehaviour() {

			@Override
			public void action() {
				if (semaphoreWest == GREEN 
						&& !isSemaphoreEmpty(CarAgent.WEST) 
						&& isInnerQueueAvailable(CarAgent.WEST)) {
					debugLog(" Adding car to inner queue from direction " +  dirToStr(CarAgent.WEST));
					ACLMessage reply = queues.get(CarAgent.WEST).poll();
					addToInnerDirectionQueue(CarAgent.WEST, reply);
					reply.setPerformative(ACLMessage.CONFIRM);
					myAgent.send(reply);
					sendStatusToMainControl();
				}
				if(waitingIn == CarAgent.WEST) {
					myAgent.send(leftTurn);
					innerQueues.get(waitingIn).poll();
					leftTurn = null;
					waitingIn = null;
				}
			}
			
		});
		// East traffic
		addBehaviour(new CyclicBehaviour() {

			@Override
			public void action() {
				if (semaphoreEast == GREEN 
						&& !isSemaphoreEmpty(CarAgent.EAST) && isInnerQueueAvailable(CarAgent.EAST)) {
					debugLog(" Adding car to inner queue from direction " +  dirToStr(CarAgent.EAST));
					ACLMessage reply = queues.get(CarAgent.EAST).poll();
					addToInnerDirectionQueue(CarAgent.EAST, reply);
					reply.setPerformative(ACLMessage.CONFIRM);
					myAgent.send(reply);
					sendStatusToMainControl();
				}
				if(waitingIn == CarAgent.EAST) {
					myAgent.send(leftTurn);
					innerQueues.get(waitingIn).poll();
					leftTurn = null;
					waitingIn = null;
				}
			}
			
		});
		
		// status reporting to MainAgent
		addBehaviour(new CyclicBehaviour() {

			@Override
			public void action() {
				sendStatusToMainControl();
			}
		});
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
		
		// update last switch time
		lastSwitchTime = System.currentTimeMillis();
	
		// DEBUG
		if (semaphoreNorth == GREEN){
			System.out.println("[XROAD] SOUTH-NORTH opened");
		}
		else {
			System.out.println("[XROAD] EAST-WEST opened");
		}
	}
	
	private void initQueues() {
		// Queues on semaphores
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
			while(result.length == 0)
				result = DFService.search(this, template);
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
				//System.out.println("[XROAD] received: " + msg.getContent() + " from " + msg.getSender().getLocalName());
				ACLMessage reply = msg.createReply();
				AID sender = msg.getSender();
				if (msg.getPerformative() == ACLMessage.REQUEST) {
					// Parse request
					String[] msgContent = msg.getContent().split(" ");
					if (msgContent[0].equals("GET_SEM")) {
						// Get semaphore light
						Integer s = Integer.parseInt(msgContent[1]);
						Integer semLight = getLightOf(s);
						if (sender.getLocalName().contains("MHD")){
							   // handle MHD appearance
							   mhdAppearance = s;
							}
						if (semLight == RED) {
							debugLog(sender, "Added to queue");
							addToSemaphoreQueue(s, reply);
						} else {
							// GREEN
							if (!isSemaphoreEmpty(s) || !isInnerQueueAvailable(s)) {
								debugLog(sender, "Added to queue");
								addToSemaphoreQueue(s, reply);
							}
							else {
								// queue is empty, and inner queue has free capacity
								//TODO: we dont care what is in inner queue - we only need to know if it's full
								debugLog(sender, "Moved to inner queue");
								addToInnerDirectionQueue(s, reply);
								reply.setPerformative(ACLMessage.CONFIRM);
								myAgent.send(reply);						
							}							
						}
					}
					else if (msgContent[0].equals("IN_CR")) {
						// TODO match car from inner queue to sender, we are serving only if it is head of queue
						Integer src = Integer.parseInt(msgContent[1]);
						Integer dst = Integer.parseInt(msgContent[2]);
						
						debugLog(sender, dirToStr(src)+" -> "+dirToStr(dst)+ " entered inner queue");
						sendToMainControl("CAR_DIRECT " + src+" "+dst);
						
						try { Thread.sleep(1000); } catch (Exception e) {}
						
						if (isDstForward(src,dst) || isDstRight(src,dst)) {
							reply.setPerformative(ACLMessage.CONFIRM);
							myAgent.send(reply);
							debugLog(sender, "Removed from inner queue");
							innerQueues.get(src).poll();
						}
						else { 
							// turning left TODO
							debugLog(sender, " Going left");
//							check oppsite inner queue
							Integer opposite = getOppositeDirection(src);
							reply.setPerformative(ACLMessage.CONFIRM);
							// something is going in oposite direction
							if (!isInnerQueueForDirectionEmpty(opposite)) {
//								if someone is waiting to turn left pull him too
								if (leftTurn != null) {
									myAgent.send(reply);
									debugLog(sender, "Removed from inner queue");
									innerQueues.get(src).poll();
									myAgent.send(leftTurn);
									debugLog("Removed opposite from inner queue");
									innerQueues.get(opposite).poll();
									leftTurn = null;
									waitingIn = null;
								} else {
									// wait till someone pull you out
									leftTurn = reply;
									waitingIn = src;
								}
							} else {
								// free to go
								myAgent.send(reply);
								debugLog(sender, "Removed from inner queue");
								innerQueues.get(src).poll();
							}
						}
					}
				}
				else { // DEBUG branch
					//System.out.println("XROAD received smth else then request: " + msg.getContent());
				}
			}
			
			
			// Standard crossroad control
			//simulateCrossroad();		
			
		}
		
	}// CrossRoadControlBehaviour
	
	private Integer getOppositeDirection(Integer src) {
		if(src < 2) {
			return src == CarAgent.NORTH ? CarAgent.SOUTH : CarAgent.NORTH;
		} else {
			return src == CarAgent.EAST ? CarAgent.WEST : CarAgent.EAST;
		}
	}
	
	private void sendStatusToMainControl() {
		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		msg.addReceiver(mainControlService);
		String content = "UPDATE_Q "
				+ queues.get(CarAgent.NORTH).size() + " "
				+ queues.get(CarAgent.SOUTH).size() + " "
				+ queues.get(CarAgent.WEST).size() + " "
				+ queues.get(CarAgent.EAST).size();
		msg.setContent(content);
		send(msg);
	}
	
	private void sendToMainControl(String str) {
		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		msg.addReceiver(mainControlService);
		msg.setContent(str);
		send(msg);
	}
	
	public static String dirToStr(Integer s) {
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
		System.out.println("[XROAD] " + aid.getLocalName() + ":" + msg);
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
		return queues.get(s).isEmpty();
	}
	
	private void addToSemaphoreQueue(Integer s, ACLMessage reply) {
		queues.get(s).add(reply);
	}
	
	private boolean isInnerQueueForDirectionEmpty(Integer s) {
	return innerQueues.get(s).isEmpty();	
	}
	
	private void addToInnerDirectionQueue(Integer s, ACLMessage reply){
		innerQueues.get(s).add(reply);	

	}
	
	private boolean isInnerQueueAvailable(Integer s) {
		//System.out.println("[XROAD] Used capacity of inner " +s+ ": " + innerQueues.get(s).size());
		return innerQueues.get(s).size() < CAPACITY;
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
