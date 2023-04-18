package org.fog.test.perfeval;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.network.DelayMatrix_Float;
import org.cloudbus.cloudsim.network.TopologicalGraph;
import org.cloudbus.cloudsim.network.TopologicalLink;
import org.cloudbus.cloudsim.network.TopologicalNode;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.*;
import org.fog.placement.*;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;

import java.util.*;

import static org.fog.placement.SmartCityConstants.*;



/**
 * Simulation setup for case study 1 - EEG Beam Tractor Game
 * @author Harshit Gupta
 *
 */
public class SmartCity {



	
	public static void main(String[] args) {

		Log.printLine("Starting VRGame...");

		try {
			Log.disable();
			int num_user = 1; // number of cloud users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false; // mean trace events

			CloudSim.init(num_user, calendar, trace_flag);

			String appId = "vr_game"; // identifier of the application
			
			FogBroker broker = new FogBroker("broker");
			
			Application application = createApplication(appId, broker.getId());
			application.setUserId(broker.getId());
			
			createFogDevices(broker.getId(), appId);
			
			ModuleMapping moduleMapping = ModuleMapping.createModuleMapping(); // initializing a module mapping

			moduleMapping.addModuleToDevice("connector", "cloud"); // fixing all instances of the Connector module to the Cloud

			
			Controller controller = new Controller("master-controller", fogDevices, sensors, 
					actuators);
			
			controller.submitApplication(application, 0, (new ModulePlacementEdgewards(fogDevices, sensors, actuators, application, moduleMapping)));

			TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());

			CloudSim.startSimulation();

			CloudSim.stopSimulation();

			Log.printLine("VRGame finished!");
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Unwanted errors happen");
		}
	}

	/**
	 * Creates the fog devices in the physical topology of the simulation.
	 * @param userId
	 * @param appId
	 */
	private static void createFogDevices(int userId, String appId) {

		// create datacenters
		for (int i = 0; i < NUM_DATA_CENTERS; i++) {
			// name, mips, ram, upBw, downBw, level, ratePerMips, busyPower, idlePower
			FogDevice datacenter = createFogDevice(
					SmartCityConstants.datacenterPrefix + i,
					DATA_CENTER_MIPS, DATA_CENTER_RAM,
					100000, 100000,
					0, 0,
					87.53, 82.44
			);
			datacenter.setParentId(-1);
			datacenters.add(datacenter);
		}

		// create rfogs
		for (int i = 0; i < NUM_RFOG; i++) {
			FogDevice rfog = createFogDevice(
					SmartCityConstants.rfogPrefix + i,
					RFOG_MIPS, RFOG_RAM,
					10000, 10000,
					1, 0,
					87.53, 82.44
			);
			rfogs.add(rfog);
		}

		// create lfogs
		for (int i = 0; i < NUM_LFOG; i++) {
			FogDevice lfog = createFogDevice(
					SmartCityConstants.lfogPrefix + i,
					LFOG_MIPS, LFOG_RAM,
					10000, 10000,
					2, 0,
					87.53, 82.44
			);
			lfogs.add(lfog);
		}

		// create gateways
		for (int i = 0; i < NUM_GATEWAYS; i++) {
			FogDevice gateway = createFogDevice(
					SmartCityConstants.gatewayPrefix + i,
					GATEWAY_MIPS, GATEWAY_RAM,
					10000, 10000,
					3, 0,
					87.53, 82.44
			);
			gateways.add(gateway);
		}

		// create sensors
		for (int i = 0; i < NUM_SERVICE_INSTANCES; i++) {
			Sensor sensor = new Sensor(
					SmartCityConstants.sensorPrefix + i,
					"SENSOR",
					userId,
					appId,
					new DeterministicDistribution(1)
			);
			sensors.add(sensor);
		}

		// create actuators
		for (int i = 0; i < NUM_ACTUATORS_PER_GATEWAY * NUM_GATEWAYS; i++) {
			Actuator actuator = new Actuator(
					SmartCityConstants.actuatorPrefix + i,
					userId,
					appId,
					SmartCityConstants.actuatorPrefix + i
			);
			actuators.add(actuator);
		}


	}


	private static FogDevice createFogDevice(String nodeName, long mips,
											 int ram, long upBw, long downBw, int level, double ratePerMips,
											 double busyPower, double idlePower) {

		List<Pe> peList = new ArrayList<Pe>();

		// 3. Create PEs and add these into a list.
		peList.add(new Pe(0, new PeProvisionerOverbooking(mips)));

		int hostId = FogUtils.generateEntityId();

		long storage = 10000000; // host storage
		int bw = 10000;

		PowerHost host = new PowerHost(hostId, new RamProvisionerSimple(ram),
				new BwProvisionerOverbooking(bw), storage, peList,
				new StreamOperatorScheduler(peList), new FogLinearPowerModel(busyPower, idlePower));

		List<Host> hostList = new ArrayList<Host>();
		hostList.add(host);

		String arch = "x86"; // system architecture
		String os = "Linux"; // operating system
		String vmm = "Xen";
		double time_zone = 10.0; // time zone this resource located
		double cost = 3.0; // the cost of using processing in this resource
		double costPerMem = 0.05; // the cost of using memory in this resource
		double costPerStorage = 0.001; // the cost of using storage in this
		// resource
		double costPerBw = 0.0; // the cost of using bw in this resource
		LinkedList<Storage> storageList = new LinkedList<Storage>();

		FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(arch, os, vmm, host, time_zone, cost, costPerMem,costPerStorage, costPerBw);

		int right = getRight(nodeName);
		int left = getleft(nodeName);

		FogDevice fogdevice = null;
		try {
			fogdevice = new FogDevice(nodeName, characteristics,new AppModuleAllocationPolicy(hostList), storageList,
					right, left, getRightLatency(nodeName, right),getLeftLatency(nodeName, left), 10, upBw, downBw, 0,ratePerMips);
		} catch (Exception e) {
			e.printStackTrace();
		}

		fogdevice.setLevel(level);
		fogDevices.add(fogdevice);
		return fogdevice;
	}


	private static float getRightLatency(String nodeName, int right) {
		if ((nodeName.startsWith("DC")) && (right != -1))
			return rightLatencyDC;
		return -1;
	}


	private static float getLeftLatency(String nodeName, int left) {
		if ((nodeName.startsWith("DC")) && (left != -1))
			return leftLatencyDC;
		return -1;
	}


	private static int getleft(String nodeName) {
		int fogId;
		if ((nodeName.startsWith("DC"))) {
			fogId = Integer.valueOf(nodeName.substring(2));
			if (fogId > 0) {
				return fogId - 1 + 3;
			}
		}
		return -1;
	}


	private static int getRight(String nodeName) {
		int fogId;
		if ((nodeName.startsWith("DC"))) {
			fogId = Integer.valueOf(nodeName.substring(2));
			if ((NUM_DATA_CENTERS > 1) && (fogId < (NUM_DATA_CENTERS - 1))) {
				return fogId + 1 + 3;
			}
		}
		return -1;
	}



	public static TopologicalGraph computeTopologicalGraph(List<FogDevice> fogDevices){

		TopologicalGraph graph = new TopologicalGraph();

		TopologicalNode node =null;
		TopologicalLink link =null;
		System.out.println("Graph construction...");

		for(FogDevice fogDevice : fogDevices){

			node =  new TopologicalNode(fogDevice.getId()-3, fogDevice.getName(),0,0);
			graph.addNode(node);

			/* ADD cheldren nodes */
			if(fogDevice.getChildrenIds() != null){
				Map<Integer, Double> childMap = fogDevice.getChildToLatencyMap();
				for(Integer key : childMap.keySet()){
					link = new TopologicalLink(fogDevice.getId()-3,(int) key-3, childMap.get(key).floatValue() , (float)30000);
					graph.addLink(link);
				}
			}


			/* ADD Right Link to Graph */
			if(fogDevice.getRightId()!=-1){
				link = new TopologicalLink(fogDevice.getId()-3,fogDevice.getRightId()-3, fogDevice.getRightLatency(),30000);
				graph.addLink(link);
			}
		}


		//System.out.println(graph.toString());

		return graph;


	}

	private static void printDevices() {
		// System.out.println("\nFog devices : ");
		for (FogDevice fogdev : fogDevices) {
			System.out.println(fogdev.getName()+"  idEntity = "+fogdev.getId()+" up= "+fogdev.getParentId()+" left ="+fogdev.getLeftId()+" leftLatency = "+fogdev.getLeftLatency()+" right ="+fogdev.getRightId()+" rightLatency="+fogdev.getRightLatency()+" children = "+fogdev.getChildrenIds()+" childrenLatencies ="+fogdev.getChildToLatencyMap()+" Storage = "+fogdev.getVmAllocationPolicy().getHostList().get(0).getStorage()+" |	");
		}

		// System.out.println("\nSensors : ");
		for (Sensor snr : sensors) {
			System.out.println(snr.getName()+"  HGW_ID = "+snr.getGatewayDeviceId()+" TupleType = "+snr.getTupleType()+" Latency = "+snr.getLatency()+" |	");
		}
		// System.out.println("\nActuators : ");
		for (Actuator act : actuators) {
			System.out.println(act.getName()+" GW_ID = "+act.getGatewayDeviceId()+" Act_Type= "+act.getActuatorType()+" Latency = "+act.getLatency()+" |	");
		}
		System.out.println("\n");

	}

	private static void printAllToAllLatencies() {
		// System.out.println("\nFog devices : ");
		for (FogDevice src : fogDevices) {
			for (FogDevice dest : fogDevices) {
				System.out.println("Latency from "+src.getName()+" To "+dest.getName()+" = " + DelayMatrix_Float.getFastestLink(src.getId(), dest.getId()));
			}
		}
	}

	/**
	 * Function to create the application model (Application) of the simulation.
	 * @param appId unique identifier of the application
	 * @param userId identifier of the user of the application
	 * @return
	 */
	@SuppressWarnings({"serial" })
	private static Application createApplication(String appId, int userId){
		// TODO: Redo the application model with this graph :
		//Centre de données    Centre de données
		//------------------------------------------------
		//Les noeuds de fogs RFOG qui communiquent entre eux
		//-------------------------------------------------
		//Les noeuds de fogs LFOG qui communiquent entre eux
		//-------------------------------------------------
		//Les noeuds de Fog Passerelle qui communiquent avec les capteurs
		//-------------------------------------------------
		//Les capteurs qui communique avent les neouds au dessus Fog Passerelle
		
		Application application = Application.createApplication(appId, userId); // creates an empty application model (empty directed graph)
		
		/*
		 * Adding modules (vertices) to the application model (directed graph)
		 */
		application.addAppModule("amCLOUD", 10); // adding module Cloud to the application model
		application.addAppModule("amRFOG", 10); // adding module RFOG to the application model
		application.addAppModule("amLFOG", 10); // adding module LFOG to the application model
		application.addAppModule("amGATEWAYS", 10); // adding module Gateway to the application model

		/*
		 * Connecting the application modules (vertices) in the application model (directed graph) with edges
		 */
		application.addAppEdge("SENSOR", "amGATEWAYS", 1000, 1000, "SENSOR", Tuple.UP, AppEdge.SENSOR); // adding edge from Sensor to Gateway module carrying tuples of type SENSOR
		application.addAppEdge("amGATEWAYS", "amLFOG", 1000, 1000, "SENSOR", Tuple.UP, AppEdge.MODULE); // adding edge from Gateway to LFOG module carrying tuples of type SENSOR
		application.addAppEdge("amLFOG", "amRFOG", 1000, 1000, "SENSOR", Tuple.UP, AppEdge.MODULE); // adding edge from Gateway to RFOG module carrying tuples of type SENSOR
		application.addAppEdge("amLFOG", "amLFOG", 1000, 1000, "SENSOR", Tuple.UP, AppEdge.MODULE); // adding edge from Gateway to RFOG module carrying tuples of type SENSOR
		application.addAppEdge("amRFOG", "amRFOG", 1000, 1000, "SENSOR", Tuple.UP, AppEdge.MODULE); // adding edge from Gateway to RFOG module carrying tuples of type SENSOR
		application.addAppEdge("amRFOG", "amCLOUD", 1000, 1000, "SENSOR", Tuple.UP, AppEdge.MODULE); // adding edge from Gateway to Cloud module carrying tuples of type SENSOR

		/*
		 * Defining the input-output relationships (represented by selectivity) of the application modules. 
		 */
		application.addTupleMapping("amGATEWAYS", "SENSOR", "SENSOR", new FractionalSelectivity(1.0)); // 1.0 tuples of type "SENSOR" are emitted by the "SENSOR" module per incoming tuple of type "SENSOR"
		application.addTupleMapping("amLFOG", "SENSOR", "SENSOR", new FractionalSelectivity(1.0)); // 1.0 tuples of type "SENSOR" are emitted by the "SENSOR" module per incoming tuple of type "SENSOR"
		application.addTupleMapping("amRFOG", "SENSOR", "SENSOR", new FractionalSelectivity(1.0)); // 1.0 tuples of type "SENSOR" are emitted by the "SENSOR" module per incoming tuple of type "SENSOR"
		application.addTupleMapping("amCLOUD", "SENSOR", "SENSOR", new FractionalSelectivity(1.0)); // 1.0 tuples of type "SENSOR" are emitted by the "SENSOR" module per incoming tuple of type "SENSOR"

		/*
		 * Defining application loops to monitor the latency of. 
		 */
		AppLoop loop1 = new AppLoop(new ArrayList<String>(){{add("SENSOR");add("amGATEWAYS");add("amLFOG");add("amRFOG");add("amCLOUD");}});
		List<AppLoop> loops = new ArrayList<AppLoop>(){{add(loop1);}};
		application.setLoops(loops);
		
		return application;
	}
}