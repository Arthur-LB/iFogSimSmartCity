package org.fog.test.perfeval;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.placement.Controller;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacementEdgewards;
import org.fog.placement.ModulePlacementMapping;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;

/**
 * Simulation setup for case study 1 - EEG Beam Tractor Game
 * @author Harshit Gupta
 *
 */
public class SmartHome {
	static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
	static List<Sensor> sensors = new ArrayList<Sensor>();
	static List<Actuator> actuators = new ArrayList<Actuator>();
	
	
	static double d1_TRANSMISSION_TIME = 60;
	static double d2_TRANSMISSION_TIME = 10;
	static double d3_TRANSMISSION_TIME = 10;
	
	//static double EEG_TRANSMISSION_TIME = 10;
	
	public static void main(String[] args) {

		Log.printLine("Starting Smart_Home...");

		try {
			Log.disable();
			int num_user = 1; // number of cloud users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false; // mean trace events

			CloudSim.init(num_user, calendar, trace_flag);

			String appId = "Smart_Home"; // identifier of the application
			
			FogBroker broker = new FogBroker("broker");
			
			Application application = createApplication(appId, broker.getId());
			application.setUserId(broker.getId());
			
			createFogDevices(broker.getId(), appId);
			
			ModuleMapping moduleMapping = ModuleMapping.createModuleMapping(); // initializing a module mapping
			
			
			moduleMapping.addModuleToDevice("ServiceFog", "Fog1");
			moduleMapping.addModuleToDevice("ServiceBox", "Fog2");
			moduleMapping.addModuleToDevice("ServiceMobile", "Fog3");
			
			
			
			Controller controller = new Controller("master-controller", fogDevices, sensors, actuators);
			
			controller.submitApplication(application, 0, new ModulePlacementMapping(fogDevices, application, moduleMapping));

			TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());

			CloudSim.startSimulation();

			CloudSim.stopSimulation();

			Log.printLine("Smart_Home finished!");
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
		FogDevice fog1 = createFogDevice("Fog1", 2800, 4000, 10000, 10000, 1, 0.0, 107.339, 83.4333); // creates the fog device Cloud at the apex of the hierarchy with level=0
		fog1.setParentId(-1);
		
		FogDevice fog2 = createFogDevice("Fog2", 2800, 4000, 10000, 10000, 1, 0.0, 107.339, 83.4333); // creates the fog device Cloud at the apex of the hierarchy with level=0
		fog2.setParentId(fog1.getId());
		fog2.setUplinkLatency(5.0);
		
		FogDevice fog3 = createFogDevice("Fog3", 2800, 4000, 10000, 10000, 1, 0.0, 107.339, 83.4333); // creates the fog device Cloud at the apex of the hierarchy with level=0
		fog3.setParentId(fog2.getId());
		fog3.setUplinkLatency(1.0);

		fogDevices.add(fog1);fogDevices.add(fog2);fogDevices.add(fog3);

		Sensor camera = new Sensor("camera", "d1", userId, appId, new DeterministicDistribution(d1_TRANSMISSION_TIME)); // inter-transmission time of EEG sensor follows a deterministic distribution
		sensors.add(camera);
		camera.setGatewayDeviceId(fog2.getId());
		camera.setLatency(2.0); 
		
		
		Sensor temp = new Sensor("temperature", "d2", userId, appId, new DeterministicDistribution(d2_TRANSMISSION_TIME)); // inter-transmission time of EEG sensor follows a deterministic distribution
		sensors.add(temp);
		temp.setGatewayDeviceId(fog2.getId());
		temp.setLatency(1.0); 
		
		Sensor pres = new Sensor("presence", "d3", userId, appId, new DeterministicDistribution(d3_TRANSMISSION_TIME)); // inter-transmission time of EEG sensor follows a deterministic distribution
		sensors.add(pres);
		pres.setGatewayDeviceId(fog2.getId());
		pres.setLatency(1.0); 
		
		for(FogDevice dev : fogDevices)
			System.out.println("Name:"+dev.getName()+"  id:"+dev.getId());

		
	}

	
	
	
	
	/**
	 * Creates a vanilla fog device
	 * @param nodeName name of the device to be used in simulation
	 * @param mips MIPS
	 * @param ram RAM
	 * @param upBw uplink bandwidth
	 * @param downBw downlink bandwidth
	 * @param level hierarchy level of the device
	 * @param ratePerMips cost rate per MIPS used
	 * @param busyPower
	 * @param idlePower
	 * @return
	 */
	private static FogDevice createFogDevice(String nodeName, long mips,
			int ram, long upBw, long downBw, int level, double ratePerMips, double busyPower, double idlePower) {
		
		List<Pe> peList = new ArrayList<Pe>();

		// 3. Create PEs and add these into a list.
		peList.add(new Pe(0, new PeProvisionerOverbooking(mips))); // need to store Pe id and MIPS Rating

		int hostId = FogUtils.generateEntityId();
		long storage = 1000000; // host storage
		int bw = 10000;

		PowerHost host = new PowerHost(
				hostId,
				new RamProvisionerSimple(ram),
				new BwProvisionerOverbooking(bw),
				storage,
				peList,
				new StreamOperatorScheduler(peList),
				new FogLinearPowerModel(busyPower, idlePower)
			);

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
		LinkedList<Storage> storageList = new LinkedList<Storage>(); // we are not adding SAN
													// devices by now

		FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
				arch, os, vmm, host, time_zone, cost, costPerMem,
				costPerStorage, costPerBw);

		FogDevice fogdevice = null;
		try {
			fogdevice = new FogDevice(nodeName, characteristics, 
					new AppModuleAllocationPolicy(hostList), storageList, 10, upBw, downBw, 0, ratePerMips);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		fogdevice.setLevel(level);
		return fogdevice;
	}

	/**
	 * Function to create the EEG Tractor Beam game application in the DDF model. 
	 * @param appId unique identifier of the application
	 * @param userId identifier of the user of the application
	 * @return
	 */
	@SuppressWarnings({"serial" })
	private static Application createApplication(String appId, int userId){
		
		Application application = Application.createApplication(appId, userId); // creates an empty application model (empty directed graph)
		
		/*
		 * Adding modules (vertices) to the application model (directed graph)
		 */
		application.addAppModule("ServiceFog", 10); // adding module Client to the application model
		application.addAppModule("ServiceBox", 10); // adding module Concentration Calculator to the application model
		application.addAppModule("ServiceMobile", 10); // adding module Connector to the application model
		
		/*
		 * Connecting the application modules (vertices) in the application model (directed graph) with edges
		 */
		
		
		application.addAppEdge("camera", "ServiceBox", 1000, 1500, "d1", Tuple.UP, AppEdge.SENSOR); 
		application.addAppEdge("temperature", "ServiceBox", 10, 1000, "d2", Tuple.UP, AppEdge.SENSOR); 
		application.addAppEdge("presence", "ServiceBox", 1, 700, "d3", Tuple.UP, AppEdge.SENSOR); 
		
		application.addAppEdge("ServiceBox", "ServiceMobile", 1, 500, "d4", Tuple.DOWN, AppEdge.MODULE);
		
		application.addAppEdge("ServiceBox", "ServiceFog", 1000, 1500, "d5", Tuple.UP, AppEdge.MODULE);
		application.addAppEdge("ServiceBox", "ServiceFog", 10, 1000, "d6", Tuple.UP, AppEdge.MODULE);
		application.addAppEdge("ServiceBox", "ServiceFog", 1, 700, "d7", Tuple.UP, AppEdge.MODULE);
		
	
		/*
		 * Defining the input-output relationships (represented by selectivity) of the application modules. 
		 */
		
		application.addTupleMapping("ServiceBox", "d1", "d4", new FractionalSelectivity(1)); 
		application.addTupleMapping("ServiceBox", "d2", "d4", new FractionalSelectivity(1)); 
		application.addTupleMapping("ServiceBox", "d3", "d4", new FractionalSelectivity(1)); 
		
		application.addTupleMapping("ServiceBox", "d1", "d5", new FractionalSelectivity(1)); 
		application.addTupleMapping("ServiceBox", "d2", "d6", new FractionalSelectivity(1)); 
		application.addTupleMapping("ServiceBox", "d3", "d7", new FractionalSelectivity(1)); 
		
		
		/*
		 * Defining application loops to monitor the latency of. 
		 * Here, we add only one loop for monitoring : EEG(sensor) -> Client -> Concentration Calculator -> Client -> DISPLAY (actuator)
		 */
		final AppLoop loop1 = new AppLoop(new ArrayList<String>(){{add("camera");add("temperature");add("presence");add("Fog1");add("Fog2");add("Fog3");}});
		List<AppLoop> loops = new ArrayList<AppLoop>(){{add(loop1);}};
		application.setLoops(loops);
		
		return application;
	}
}