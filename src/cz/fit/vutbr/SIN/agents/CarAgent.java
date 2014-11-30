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

	protected void setup() {
				
		source = Integer.parseInt((String) this.getArguments()[0]);
		destination = Integer.parseInt((String) this.getArguments()[1]);
		
		state = CarState.BEFORE_SEMAPHORE;
		
		// Get CrossRoad control service
		getService("crossroad-control");
		
		// Get gui service
		getService("main-control");
		
		// Periodically send statistics
		addBehaviour(new TickerBehaviour(this, 5000){

			@Override
			protected void onTick() {
				ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
				msg.addReceiver(guiService);
				msg.setContent("STATUS "+myAgent.getLocalName()+":"+state+"\n");
				send(msg);
			}
			
		});
		
		//addBehaviour(new RoutingBehaviour());
		
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
	
	private class RoutingBehaviour extends Behaviour {
		
		private boolean messageSent = false;
		
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
						}
					} else {
						// Send request about semaphore light
						ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
						req.addReceiver(crossroadControlService);
						req.setContent("GET_SEM "+source);
						req.setConversationId("semaphore-state");
						// Unique value - also inform about public transport
						req.setReplyWith("REQ-"+myAgent.getLocalName()+"-"+System.currentTimeMillis());
						myAgent.send(req);
						messageSent = true;
						// Prepare the template to get proposals
						mt = MessageTemplate.and(MessageTemplate.MatchConversationId("semaphore-state"),
								MessageTemplate.MatchInReplyTo(req.getReplyWith()));
					}
					break;
				case IN_CROSSROAD:
					// tell about your intentions and wait for confirmation`
					if(messageSent) {
						ACLMessage reply = myAgent.receive(mt);
						if (reply != null) {
							if(reply.getPerformative() == ACLMessage.CONFIRM) {
								state = CarState.OVER_CROSSROAD;
							}
						}
					} else {
						ACLMessage query = new ACLMessage(ACLMessage.REQUEST);
						query.addReceiver(crossroadControlService);
						query.setContent("IN_CR "+source+" "+destination);
						query.setConversationId("in-crossroad-decision");
						query.setReplyWith("QUERY-"+myAgent.getLocalName()+"-"+System.currentTimeMillis());
						myAgent.send(query);
						messageSent = true;
						mt = MessageTemplate.and(MessageTemplate.MatchConversationId("in-crossroad-decision"),
								MessageTemplate.MatchInReplyTo(query.getReplyWith()));
					}
					break;
				default:
					state = CarState.OVER_CROSSROAD;
			}
		}

		@Override
		public boolean done() {
			return state == CarState.OVER_CROSSROAD;
		}
		
	}
	
}