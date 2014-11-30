package cz.fit.vutbr.SIN.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class CarAgent extends Agent {
	
	public enum CarState {
		BEFORE_SEMAPHORE,
		IN_CROSSROAD,
		OVER_CROSSROAD
	}
	
	// Directions
	public static final Integer NORTH = 0;
	public static final Integer SOUTH = 1;
	public static final Integer WEST = 2;
	public static final Integer EAST = 3;
	
	private CarState state;
	private Integer source;
	private Integer destination;
	private AID crossroadControlService;
	private AID guiService;
	
	private long creationTime;

	protected void setup() {
				
		source = Integer.parseInt((String) this.getArguments()[0]);
		destination = Integer.parseInt((String) this.getArguments()[1]);
		
		state = CarState.BEFORE_SEMAPHORE;
		creationTime = System.currentTimeMillis();
		
		// Get CrossRoad control service
		getService("crossroad-control");
		
		// Get gui service
		getService("main-control");
		
		//Testing behaviour
		/*addBehaviour(new TickerBehaviour(this,1500) {

			@Override
			protected void onTick() {
				nextState();
				
			}
			
		});
		*/
		addBehaviour(new RoutingBehaviour());
		
	}
	
	// Testing method
	public void nextState() {
		if(state == CarState.BEFORE_SEMAPHORE) {
			state = CarState.IN_CROSSROAD;
		} else if (state == CarState.IN_CROSSROAD) {
			state = CarState.OVER_CROSSROAD;
		}
	}
	
	private void getService( String serviceType) {

		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType(serviceType);
		template.addServices(sd);
		try {
			DFAgentDescription[] result = DFService.search(this, template);
			if (serviceType.equals("main-control"))
				guiService = result[0].getName();
			else
				throw new Exception();
		} catch (FIPAException fe) {
			fe.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private class StatusChangeBehaviour extends Behaviour {
		
		private long waitTime;
		
		public StatusChangeBehaviour(long waitTime) {
			super();
			this.waitTime = waitTime;
		}

		@Override
		public void action() {
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.addReceiver(guiService);
			switch(state) {
				case BEFORE_SEMAPHORE:
					msg.setContent("STATUS "+myAgent.getLocalName()
							+":_"+source+"->"+destination
							+"_"+state+"\n");
					break;
				case IN_CROSSROAD:
					msg.setContent("STATUS "+myAgent.getLocalName()
							+":_"+source+"->"+destination
							+"_"+state
							+"_waited:"+waitTime+"s\n");
					break;
				case OVER_CROSSROAD:
					msg.setContent("STATUS "+myAgent.getLocalName()
							+":_"+source+"->"+destination
							+"_"+state
							+"_lived:"+waitTime+"s\n");
					break;
			}
			
			send(msg);
			
		}

		@Override
		public boolean done() {
			// TODO Auto-generated method stub
			return true;
		}
		
	}
	
	private class RoutingBehaviour extends Behaviour {
		
		private boolean messageSent = false;
		// Testing purpose
		//private CarState inState = state;
		
		private long startWait;
		
		private MessageTemplate mt;

		@Override
		public void action() {
			switch(state) {
				case BEFORE_SEMAPHORE:
					if(messageSent) {
						ACLMessage reply = myAgent.receive(mt);
						if(reply != null) {
							// CrossroadControlService inform about semaphore light
							if (reply.getPerformative() == ACLMessage.INFORM) {
								messageSent = false;
							}
						} else {
							block();
						}
					} else {
						myAgent.addBehaviour(new StatusChangeBehaviour(0));
						// Send request about semaphore light
						ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
						req.addReceiver(crossroadControlService);
						req.setContent("GET_SEM "+source);
						req.setConversationId("semaphore-state");
						// Unique value - also inform about public transport
						req.setReplyWith("REQ-"+myAgent.getLocalName()+"-"+System.currentTimeMillis());
						myAgent.send(req);
						// Prepare the template to get proposals
						mt = MessageTemplate.and(MessageTemplate.MatchConversationId("semaphore-state"),
								MessageTemplate.MatchInReplyTo(req.getReplyWith()));
						startWait = System.currentTimeMillis();
						messageSent = true;
					}
					break;
				case IN_CROSSROAD:
					/*if(inState != state) {
						messageSent = false;
						inState = state;
					}*/
					// tell about your intentions and wait for confirmation`
					if(messageSent) {
						ACLMessage reply = myAgent.receive(mt);
						if (reply != null) {
							if(reply.getPerformative() == ACLMessage.CONFIRM) {
								state = CarState.OVER_CROSSROAD;
								messageSent = false;
							}
						} else {
							block();
						}
					} else {
						long totalWait = (System.currentTimeMillis()-startWait)/1000;
						myAgent.addBehaviour(new StatusChangeBehaviour(totalWait));
						ACLMessage query = new ACLMessage(ACLMessage.REQUEST);
						query.addReceiver(crossroadControlService);
						query.setContent("IN_CR "+source+" "+destination);
						query.setConversationId("in-crossroad-decision");
						query.setReplyWith("QUERY-"+myAgent.getLocalName()+"-"+System.currentTimeMillis());
						myAgent.send(query);
						mt = MessageTemplate.and(MessageTemplate.MatchConversationId("in-crossroad-decision"),
								MessageTemplate.MatchInReplyTo(query.getReplyWith()));
						messageSent = true;
					}
					break;
				default:
					state = CarState.OVER_CROSSROAD;
					long totalTime = (System.currentTimeMillis()-creationTime)/1000;
					myAgent.addBehaviour(new StatusChangeBehaviour(totalTime));
			}
		}

		@Override
		public boolean done() {
			return state == CarState.OVER_CROSSROAD;
		}
		
	}
	
}
