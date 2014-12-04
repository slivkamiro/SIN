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

	private int carCnt = 0;

	// Statistics
	private int onSemTotalCnt = 0;
	private int onSemTotalTime = 0;
	private int onSemPtCnt = 0;
	private int onSemPtTime = 0;
	private int onSemCarCnt = 0;
	private int onSemCarTime = 0;
	private int lifeTotalCnt = 0;
	private int lifeTotalTime = 0;
	private int lifePtCnt = 0;
	private int lifePtTime = 0;
	private int lifeCarCnt = 0;
	private int lifeCarTime = 0;

	@Override
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
					if(!msg.getContent().contains("UPDATE_Q")) {
						System.out.println("debug:MainAgent: "+msg.getContent());
					}
					String[] content = msg.getContent().split(" ");
					if (content[0].equals("STATUS")) {
						updateStatistics(content[1]);
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

	protected void printStats() {
		events.add("Total on semaphore wait time: "+onSemTotalTime+"s\n");
		if (onSemTotalCnt != 0) {
			events.add("Average on semaphore wait time: "+(onSemTotalTime/onSemTotalCnt)+"s\n");
		}
		events.add("Total public transportation on semaphore wait time: "+onSemPtTime+"s\n");
		if (onSemPtCnt != 0) {
			events.add("Average public transportation on semaphore wait time: "+(onSemPtTime/onSemPtCnt)+"s\n");
		}
		events.add("Total personal car on semaphore wait time: "+onSemCarTime+"s\n");
		if (onSemCarCnt != 0) {
			events.add("Average personal car on semaphore wait time: "+(onSemCarTime/onSemCarCnt)+"s\n");
		}
		events.add("Total life time: "+lifeTotalTime+"s\n");
		if (lifeTotalCnt != 0) {
			events.add("Average life time: "+(lifeTotalTime/lifeTotalCnt)+"s\n");
		}
		events.add("Total public transportation life time: "+lifePtTime+"s\n");
		if (lifePtCnt != 0) {
			events.add("Average public transportation life time: "+(lifePtTime/lifePtCnt)+"s\n");
		}
		events.add("Total personal car life time: "+lifeCarTime+"s\n");
		if (lifeCarCnt != 0) {
			events.add("Average personal car life time: "+(lifeCarTime/lifeCarCnt)+"s\n");
		}
		gui.appendEvents(events);
		events.clear();

	}

	protected void updateStatistics(String msg) {
		if (msg.contains("IN_CROSSROAD")) {
			// update on semaphore total wait time
			String seconds = msg.substring(msg.indexOf("_waited:")+"_waited:".length(),msg.length()-2);
			int wt = Integer.parseInt(seconds);
			onSemTotalCnt++;
			onSemTotalTime += wt;
			if (msg.contains("MHD")) {
				// update public transportation on semaphore wait time
				onSemPtCnt++;
				onSemPtTime += wt;
			} else {
				// update personal cars onsemaphore wait time
				onSemCarCnt++;
				onSemCarTime += wt;
			}
		} else if (msg.contains("OVER_CROSSROAD")) {
			// update total life in crossroad
			String seconds = msg.substring(msg.indexOf("_lived:")+"_lived:".length(),msg.length()-2);
			int lt = Integer.parseInt(seconds);
			lifeTotalCnt++;
			lifeTotalTime += lt;
			if (msg.contains("MHD")) {
				// update pt total life
				lifePtCnt++;
				lifePtTime += lt;
			} else {
				// personal car total life
				lifeCarCnt++;
				lifeCarTime += lt;
			}
		}

	}

	@Override
	public void takeDown() {
		try {
			DFService.deregister(this);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}

		gui.dispose();

	}

	public void spawnCars(int num) {
		spawnCars(num,"CAR_");
	}

	public void spawnCars(int num, final String prefix) {
		num += carCnt;
		for(; carCnt < num; carCnt++) {
			final Integer n = new Integer(carCnt);
			addBehaviour(new WakerBehaviour(this, (long) (Math.random()*50000+1)) {

				@Override
				public void onWake() {
					try {
						int src = (int) (Math.random()*4);
						int dst = (int) (Math.random()*4);

						if (dst == src) {
							dst = (dst+1) % 4;
						}
						String[] direction = { src+"", dst+"" };
						AgentController agent = carAgentsContainer.createNewAgent(prefix+"#"+n, CarAgent.class.getCanonicalName(), direction);
						agent.start();
						Calendar cal = Calendar.getInstance();
						cal.getTime();
						SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
						events.add(sdf.format(cal.getTime())+" :"+prefix+"#"+n+" dispatched\n");
						gui.appendEvents(events);
						events.clear();
					} catch (StaleProxyException e) {
						events.add(prefix+"#"+n+"failed to dispatch\n");
						gui.appendEvents(events);
						events.clear();
						e.printStackTrace();
					}
				}

			});
		}


	}

	public void startSimulation() {
		// Stats must be planed after 66 seconds because last car can be planned 50 secondes after start
		// and it can come to crossroad just when red light turn ed on and that takes 15 seconds to pass
		// it takes one second to get through crossroad
		addBehaviour(new WakerBehaviour(this,100000) {
			@Override
			public void onWake(){
				printStats();
			}

		});
	}

}
