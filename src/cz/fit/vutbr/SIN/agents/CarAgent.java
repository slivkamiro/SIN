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
		CONT_LEFT,
		CONT_RIGHT_STRAIGHT,
		OVER_CROSSROAD
	}
	
	// Directions
	public static final Integer NORTH = 1;
	public static final Integer SOUTH = 2;
	public static final Integer WEST = 3;
	public static final Integer EAST = 4;
	
	private CarState state;
	private Integer source;
	private Integer destination;
	private AID crossroadControlService;

	protected void setup() {
		
		final String mainAgentID = (String) this.getArguments()[0];
		System.out.println(getAID().getLocalName()+" agent know about "+mainAgentID);
		
		source = Integer.parseInt((String) this.getArguments()[1]);
		destination = Integer.parseInt((String) this.getArguments()[2]);
		
		state = CarState.BEFORE_SEMAPHORE;
		
		// Get CrossRoad control service
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType("crossroad-control");
		template.addServices(sd);
		try {
			DFAgentDescription[] result = DFService.search(this, template);
			crossroadControlService = result[0].getName();
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
		
		// Periodically send statistics
		addBehaviour(new TickerBehaviour(this, 1000){

			@Override
			protected void onTick() {
				ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
				msg.addReceiver(new AID(mainAgentID,AID.ISLOCALNAME));
				msg.setContent(myAgent.getLocalName()+": "+state+"\n");
				send(msg);
			}
			
		});
		
		//addBehaviour(new RoutingBehaviour());
		
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
								Integer light = Integer.parseInt(reply.getContent());
								state = light == MainAgent.GREEN ? CarState.IN_CROSSROAD : CarState.BEFORE_SEMAPHORE;
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
					if(!turningLeft(source,destination)) {
						state = CarState.CONT_RIGHT_STRAIGHT;
					} else {
						state = CarState.CONT_LEFT;						
					}
					break;
				case CONT_LEFT:
					// Find out if is it free to go
					if(messageSent) {
						ACLMessage reply = myAgent.receive(mt);
						if(reply != null) {
							if(reply.getPerformative() == ACLMessage.CONFIRM) {
								state = CarState.OVER_CROSSROAD;
							}
						}
					} else {
						ACLMessage query = new ACLMessage(ACLMessage.QUERY_IF);
						query.addReceiver(crossroadControlService);
						query.setContent("TURN_LEFT_SAFE "+source);
						query.setConversationId("turning-left");
						query.setReplyWith("QUERY-"+myAgent.getLocalName()+"-"+System.currentTimeMillis());
						myAgent.send(query);
						messageSent = true;
						mt = MessageTemplate.and(MessageTemplate.MatchConversationId("turning-left"),
								MessageTemplate.MatchInReplyTo(query.getReplyWith()));
					}
					break;
				default:
					state = CarState.OVER_CROSSROAD;
			}
		}

		private boolean turningLeft(Integer source, Integer destination) {
			return (source == SOUTH && destination == WEST) ||
					(source == EAST && destination == SOUTH) ||
					(source == NORTH && destination == EAST) ||
					(source == WEST && destination == NORTH);
		}

		@Override
		public boolean done() {
			return state == CarState.OVER_CROSSROAD;
		}
		
	}
	
}