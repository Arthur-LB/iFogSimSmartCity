package org.fog.test.perfeval.SmartCityPkg;

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

import static org.fog.test.perfeval.SmartCityPkg.SmartCityConstants.*;



/**
 * Simulation setup for case study 1 - EEG Beam Tractor Game
 * @author Harshit Gupta
 *
 */
public class SmartCity {

	
	public static void main(String[] args) {

		Log.printLine("Starting SmartCity...");

		try {
			Log.disable();
			int num_user = 1; // number of cloud users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false; // mean trace events

			// argument 1: nom de l'algorithme de placement
			String placement = args[0];
			// arugment 2: log all ?
			boolean logAll = args.length > 1 && args[1].equals("true");


			CloudSim.init(num_user, calendar, trace_flag);

			String appId = "smart_city"; // identifier of the application

			// création du broker
			FogBroker broker = new FogBroker("broker");

			// creation noeds fog et des capteurs
			createFogDevices(broker.getId(), appId);
			createSensors(broker.getId(), appId);
			printDevices();
			//createActuators(broker.getId(), appId);

			// création de l'application et de ses modules du scénario demandé
			Application application = createApplication(appId, broker.getId());
			application.setUserId(broker.getId());

			// création du module mapping (liens entre modules et noeuds fog)
			ModuleMapping moduleMapping = ModuleMapping.createModuleMapping(); // initializing a module mapping

			// création de la map des placements
			Map<String, Class<? extends ModulePlacement>> modulePlacementMap = new HashMap<>(
					Map.of("CLOUD", ModulePlacementOnlyCloud.class,
							"RANDOM", ModulePlacementRandom.class,
							"ASC", ModulePlacementASC.class,
							"DESC", ModulePlacementDSC.class)
			);

			// String output pour prévenir le placement
			Map<String, String> placementMap = new HashMap<String, String>(
					Map.of("CLOUD", "Cloud Placement, place les modules sur le cloud.",
							"RANDOM", "Random Placement, place les modules aléatoirement.",
							"ASC", "ASC Placement, place les modules selon l'ordre croissant.",
							"DESC", "DESC Placement, place les modules selon l'ordre décroissant.")
			);

			// --- Algorithmes de placement ---
			// Premièrement on place les 100 premiers services sur les différentes gateway
			int serviceId = 0;
			for (FogDevice fogdevice : fogDevices) {
				if (fogdevice.getName().startsWith(gatewayPrefix)) {
					moduleMapping.addModuleToDevice("service-" + serviceId, fogdevice.getName());
					serviceId++;
				}
			}


			// création du controller
			Controller controller = new Controller("master-controller", fogDevices, sensors, 
					actuators);

			// Initialisation de la variable pour le placement

			// Placement selon l'algorithme demandé
			ModulePlacement modulePlacement = modulePlacementMap
					.get(placement)
					.getDeclaredConstructor(List.class, Application.class, ModuleMapping.class)
					.newInstance(fogDevices, application, moduleMapping); // Récupération de l'algorithme de placement
			if (modulePlacement == null) {
				throw new IllegalArgumentException("Placement algorithm not found");
			}
			controller.submitApplication(application, 0, modulePlacement);
			ModuleMapping moduleMappingWithAlgorithm = modulePlacement.getModuleMapping();

			System.out.println(placementMap.get(placement)); // Affichage du placement

			// Calcule du temps de simulation
			System.out.println("Calcul du temps de simulation");
			TopologicalGraph graph = computeTopologicalGraph(fogDevices); // Créé le graphe topologique des noeuds fog
			new DelayMatrix_Float(graph, false); // Créé la matrice de délais (pour le calcul du temps de simulation)

			TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());
			float global_latency = computeLatency(application, moduleMappingWithAlgorithm); // Calcul du temps de latence global
			System.out.println("Temps de latence global : " + global_latency + " ms");
			if (logAll) {
				printAllToAllLatencies();
			}
			System.out.println("Starting simulation...");

			CloudSim.startSimulation(); // Lancement de la simulation

			CloudSim.stopSimulation(); // Arrêt de la simulation

			Log.printLine("Smart City Finished !");
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Unwanted errors happen");
		}
	}

	/**
	 * Creates the fog devices in the physical topology of the simulation.
	 *
	 * @param userId
	 * @param appId
	 */
	private static void createFogDevices(int userId, String appId) {
		for (int i=0; i<NUM_DATA_CENTERS; i++){
			FogDevice datacenter = createFogDevice(SmartCityConstants.datacenterPrefix + i, DATA_CENTER_MIPS, DATA_CENTER_RAM, 100, 10000, 0, 0.01, 16*103, 16*83.25);
			datacenter.setParentId(-1);
			fogDevices.add(datacenter);
			datacenters.add(datacenter);
			for(int j=0;j<NUM_RFOG_PER_DC;j++) {
				addRFOG(NUM_RFOG_PER_DC*i+j, userId, appId, datacenter.getId());
			}
		}
	}

	/**
	 * Creates the RFOG in the physical topology of the simulation for the given datacenter.
	 * @param id
	 * @param userId
	 * @param appId
	 * @param parentId
	 */
	private static void addRFOG(int id, int userId, String appId, int parentId){
		FogDevice rfog = createFogDevice(SmartCityConstants.rfogPrefix+id, RFOG_MIPS, RFOG_RAM, 10000, 10000, 1, 0.0, 107.339, 83.4333);
		rfog.setParentId(parentId);
		rfog.setUplinkLatency(RFOG_TO_DATACENTER_LATENCY);
		fogDevices.add(rfog);
		rfogs.add(rfog);
		for(int i=0;i<NUM_LFOG_PER_RFOG;i++){
			addLFOG(NUM_LFOG_PER_RFOG*id+i, userId, appId, rfog.getId());
		}
	}

	/**
	 * Creates the LFOG in the physical topology of the simulation for the given RFOG.
	 * @param id
	 * @param userId
	 * @param appId
	 * @param parentId
	 */
	private static void addLFOG(int id, int userId, String appId, int parentId){
		FogDevice lfog = createFogDevice(SmartCityConstants.lfogPrefix+id, LFOG_MIPS, LFOG_RAM, 1000, 1000, 2, 0.0, 107.339, 83.4333);
		lfog.setParentId(parentId);
		lfog.setUplinkLatency(LFOG_TO_RFOG_LATENCY);
		fogDevices.add(lfog);
		lfogs.add(lfog);
		for(int i=0;i<NUM_GATEWAYS_PER_LFOG;i++){
			addGw(NUM_GATEWAYS_PER_LFOG*id+i, userId, appId, lfog.getId());
		}
	}

	/**
	 * Creates the gateway in the physical topology of the simulation for the given LFOG.
	 * @param id
	 * @param userId
	 * @param appId
	 * @param parentId
	 */
	private static void addGw(int id, int userId, String appId, int parentId){
		FogDevice gateway = createFogDevice(SmartCityConstants.gatewayPrefix+id, GATEWAY_MIPS, GATEWAY_RAM, 1000, 1000, 3, 0.0, 107.339, 83.4333);
		gateway.setParentId(parentId);
		gateway.setUplinkLatency(GATEWAY_TO_LFOG_LATENCY);
		fogDevices.add(gateway);
		gateways.add(gateway);
	}

	/**
	 * Creates a fog sensor in the physical topology of the simulation.
	 * @param userId
	 * @param appId
	 */
	static void createSensors(int userId, String appId) {
		int sensor_id = 0;
		for (FogDevice device : fogDevices) {
			if (device.getName().startsWith(gatewayPrefix)) {
				for (int i = 0; i < NUM_SENSORS_PER_GATEWAY; i++) {
					Sensor sensor = new Sensor(SmartCityConstants.sensorPrefix + sensor_id, SENSOR_TUPLE_TYPE, userId, appId, new DeterministicDistribution(1));
					sensor.setGatewayDeviceId(device.getId());
					sensor.setLatency((double) SENSOR_TO_GATEWAY_LATENCY);
					sensors.add(sensor);
					sensor_id++;
				}
			}
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

		assert fogdevice != null;
		fogdevice.setLevel(level);
		return fogdevice;
	}


	private static float getRightLatency(String nodeName, int right) {
		if ((nodeName.startsWith(datacenterPrefix)) && (right != -1))
			return rightLatencyDC;
		return -1;
	}


	private static float getLeftLatency(String nodeName, int left) {
		if ((nodeName.startsWith(datacenterPrefix)) && (left != -1))
			return leftLatencyDC;
		return -1;
	}


	private static int getleft(String nodeName) {
		int fogId;
		if ((nodeName.startsWith(datacenterPrefix))) {
			fogId = Integer.parseInt(nodeName.substring(2));
			if (fogId > 0) {
				return fogId - 1 + 3;
			}
		}
		return -1;
	}


	private static int getRight(String nodeName) {
		int fogId;
		if ((nodeName.startsWith(datacenterPrefix))) {
			fogId = Integer.parseInt(nodeName.substring(2));
			if ((NUM_DATA_CENTERS > 1) && (fogId < (NUM_DATA_CENTERS - 1))) {
				return fogId + 1 + 3;
			}
		}
		return -1;
	}



	public static TopologicalGraph computeTopologicalGraph(List<FogDevice> fogDevices) {

		TopologicalGraph graph = new TopologicalGraph();

		TopologicalNode node = null;
		TopologicalLink link = null;
		System.out.println("Graph construction...");

		for (FogDevice fogDevice : fogDevices) {

			node = new TopologicalNode(fogDevice.getId() - 3, fogDevice.getName(), 0, 0);
			graph.addNode(node);

			/* ADD children nodes */
			if (fogDevice.getChildrenIds() != null) {
				Map<Integer, Double> childMap = fogDevice.getChildToLatencyMap();
				for (Integer key : childMap.keySet()) {
					if(key -3 < 0)
						System.out.println("key: " + key + " fogDevice.getId(): " + fogDevice.getId());

					link = new TopologicalLink(fogDevice.getId() - 3, (int) key - 3, childMap.get(key).floatValue(), (float) 30000);
					graph.addLink(link);
				}
			}


			/* ADD Right Link to Graph */
			if (fogDevice.getRightId() != -1) {
				if(fogDevice.getRightId() - 3 < 0) fogDevice.setRightId(fogDevice.getRightId() + 3);

				link = new TopologicalLink(fogDevice.getId() - 3, fogDevice.getRightId() - 3, fogDevice.getRightLatency(), 30000);
				graph.addLink(link);
			}
		}

		//System.out.println(graph.toString());

		return graph;


	}

	/**
	 * Prints the Cloudlet objects
	 */
	private static void printDevices() {
		for (FogDevice fogdev : fogDevices) {
			System.out.println(fogdev.getName() + "  idEntity = " + fogdev.getId() + " up= " + fogdev.getParentId() + " left =" + fogdev.getLeftId() + " leftLatency = " + fogdev.getLeftLatency() + " right =" + fogdev.getRightId() + " rightLatency=" + fogdev.getRightLatency() + " children = " + fogdev.getChildrenIds() + " childrenLatencies =" + fogdev.getChildToLatencyMap() + " Storage = " + fogdev.getVmAllocationPolicy().getHostList().get(0).getStorage() + " |	");
		}

		for (Sensor snr : sensors) {
			System.out.println(snr.getName() + "  HGW_ID = " + snr.getGatewayDeviceId() + " TupleType = " + snr.getTupleType() + " Latency = " + snr.getLatency() + " |	");
		}

		for (Actuator act : actuators) {
			System.out.println(act.getName() + " GW_ID = " + act.getGatewayDeviceId() + " Act_Type= " + act.getActuatorType() + " Latency = " + act.getLatency() + "");
		}

	}

	/**
	 * Function to print the latency between all the fog devices.
	 */
	private static void printAllToAllLatencies() {
		for (FogDevice src : fogDevices) {
			for (FogDevice dest : fogDevices) {
				System.out.println("Latency from "+src.getName()+" To "+dest.getName()+" = " + DelayMatrix_Float.getFastestLink(src.getId(), dest.getId()));
			}
		}
	}

	private static <K, V> String getKeyMappingOfModule(Map<String, List<String>> mapping, String module) {
		for (Map.Entry<String, List<String>> entry : mapping.entrySet()) {
			if (entry.getValue().contains(module)) {
				return entry.getKey();
			}
		}
		return null;
	}

	/**
	 * Function to print the latency between all the fog devices.
	 * @param app
	 * @param moduleMapping
	 * @return the total latency of the application
	 */
	private static Float computeLatency(Application app, ModuleMapping moduleMapping) {
		List<AppLoop> loops = app.getLoops(); // Récupérer tous les scénarios de l'application (scénario = type de communication)
		float totalLatency = 0.0f; // Somme des latences

		// Parcourir tous les scénarios
		for (AppLoop loop : loops) {
			FogDevice src = null; // Source du scénario
			// Parcourir tous les modules
			for (String module : loop.getModules()) {
				Map<String, List<String>> mapping = moduleMapping.getModuleMapping(); // Récupérer le mapping des modules
				if (!loop.isStartModule(module)) { // Si le module n'est pas le module de départ on calcule la latence (le module de départ n'a pas de source)

					FogDevice dest = fogDevices.stream().filter(
							(e) -> Objects.equals(e.getName(), getKeyMappingOfModule(mapping, module))
					).findFirst().orElse(null);

					if (src != null && dest != null) { // Si la source n'est pas null on calcule la latence entre la source et la destination
						float latency = DelayMatrix_Float.getFastestLink(src.getId(), dest.getId()); // Récupérer la latence entre la source et la destination
						System.out.println("Latency from " + src.getName() + " To " + dest.getName() + " = " + latency); // Afficher la latence
						totalLatency += latency; // Ajouter la latence à la somme des latences
					}
					src = dest; // La source devient la destination
				}
			}
		}
		return totalLatency; // Retourner la somme des latences
	}

	/**
	 * Function to create the application model (Application) of the simulation.
	 * @param appId unique identifier of the application
	 * @param userId identifier of the user of the application
	 * @return the application model
	 */
	@SuppressWarnings({"serial" })
	private static Application createApplication(String appId, int userId){
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

		Random rand = new Random();

		/*
		 * Adding services (vertices) to the application model (directed graph)
		 */
		for(int i=0; i < NUM_SERVICE_INSTANCES ; i++){
			application.addAppModule("service-"+i, rand.nextInt(100, 1000), rand.nextInt(100, 1000));
			// Définis la probabilité de sélection d'un tuple pour ce module
			application.addTupleMapping("service-" + i, SENSOR_TUPLE_TYPE, SENSOR_TUPLE_TYPE, new FractionalSelectivity(0.1));
		}

		/*
		 * Connecting the application modules (vertices) in the application model (directed graph) with edges
		 */
		for (int i = 0; i < NUM_SERVICE_INSTANCES; i++) {
			if (i < 100) { // Les 100 premiers modules sont des capteurs
				application.addAppEdge(SENSOR_TUPLE_TYPE, "service-" + i, 10, 15, SENSOR_TUPLE_TYPE, Tuple.UP, AppEdge.SENSOR);
			} else {
				// Répudiation de la communication entre les modules
				for (int j = 0; j < NUM_SERVICE_INSTANCES; j++) {
					application.addAppEdge("service-" + i, "service-" + j, 1000, 1500, SENSOR_TUPLE_TYPE, Tuple.UP, AppEdge.MODULE);
					application.addAppEdge("service-" + i, "service-" + j, 1000, 1500, SENSOR_TUPLE_TYPE, Tuple.DOWN, AppEdge.MODULE);
				}
			}
		}

		/*
		 * Defining application loops to monitor the latency of. 
		 */
		final AppLoop loop1 = new AppLoop(new ArrayList<>() {{
			add("sensor-0");
			add("service-1");
			add("service-389");
			add("service-931");
		}});
		// create another loop with random service instances
		final AppLoop loop2 = new AppLoop(new ArrayList<>() {{
			add("sensor-0");
			add("service-1");
			add("service-172");
			add("service-735");
		}});
		final AppLoop loop3 = new AppLoop(new ArrayList<>() {{
			add("sensor-0");
			add("service-1");
			add("service-500");
			add("service-984");
		}});
		final AppLoop loop4 = new AppLoop(new ArrayList<>() {{
			add("sensor-0");
			add("service-1");
			add("service-72");
			add("service-325");
		}});
		final AppLoop loop5 = new AppLoop(new ArrayList<>() {{
			add("sensor-0");
			add("service-1");
			add("service-158");
			add("service-559");
		}});
		List<AppLoop> loops = new ArrayList<AppLoop>(){{add(loop1);add(loop2);add(loop3);add(loop4);add(loop5);}};
		application.setLoops(loops);
		
		return application;
	}
}