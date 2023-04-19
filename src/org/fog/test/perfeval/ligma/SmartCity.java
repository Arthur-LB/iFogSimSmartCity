package org.fog.test.perfeval.ligma;

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
import org.fog.placement.ModulePlacementRandom;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;

import java.util.*;

public class SmartCity {
    static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
    static List<Sensor> sensors = new ArrayList<Sensor>();
    static List<Actuator> actuators = new ArrayList<Actuator>();
    static int nbOfDatacenter = 5;
    static int nbOfRFOGperDatacenter = 2;
    static int nbOfLFOGperRFOG = 2;
    static int nbOfGatewayPerLFOG = 5;
    static int nbOfSensorPerGateway = 10;
    public static final float leftLatencyDC = 1000;
    public static final float rightLatencyDC = 1000;

    public static final float LatencyDCToRFOG = 1000;
    public static final float LatencyRFOGToLFOG = 100;
    public static final float LatencyLFOGToHGW = 50;
    public static final double LatencyHGWToSNR = 10;
    public static final float LatencyHGWToACT = 10;


    /* infrastructure */
    public static int nb_HGW = 100; //5 HGW per LFOG
    public static final int nb_LFOG = 20; //2 LFOG per RFOG
    public static final int nb_RFOG = 10; //2 RFOG per DC
    public static final int nb_DC = 5; //

    public static final int nb_SnrPerHGW = 10;
    public static final int nb_ActPerHGW = 1;
    private static final int Nb_Service = 1000;
    static String placement_algo = "random";

    /* SNR periodic samples in ms*/
    public static int SNR_TRANSMISSION_TIME = 1000;

    public static void main(String[] args) {

        Log.printLine("Starting Smart City...");

        try {
            Log.disable();
            int num_user = 1; // number of cloud users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false; // mean trace events

            CloudSim.init(num_user, calendar, trace_flag);

            String appId = "Smart_City"; // identifier of the application

            // 2-  La création de l'entité brocker.
            FogBroker broker = new FogBroker("broker");

            // 3- La création des nœuds de Fog ainsi que les capteurs.
            createFogDevices(broker.getId(), appId);
            createSensors(broker.getId(), appId);
            //printDevices();

            // 4- La création de l'application (les éléments logiques) du scénario :
            Application application = createApplication(appId, broker.getId());
            application.setUserId(broker.getId());

            // 5- La création du catalogue des emplacements des services (ModuleMapping).
            ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();

            // 6- L'implémentation de l'algorithme de placement de services
            int i = 0;
            for (FogDevice device : fogDevices) {
                // on place les 100 premiers services sur les gateway
                if (device.getName().startsWith("gw")) {
                    moduleMapping.addModuleToDevice("service-" + i, device.getName());
                    //System.out.println("Map service-" + i+ " with " + device.getName());
                    i++;
                }
            }

            // 7- La création de l'entité controller.
            Controller controller = new Controller("master-controller", fogDevices, sensors, actuators);

            ModuleMapping moduleMappingTotal = null;

            // Placement des devices restants en fonction de l'algo
            switch (placement_algo) {
                case "cloud":
                    System.out.println("Cloud Placement");
                    ModulePlacementCloud modulePlacementCloud = new ModulePlacementCloud(fogDevices, application, moduleMapping);
                    controller.submitApplication(application, 0, modulePlacementCloud);
                    moduleMappingTotal = modulePlacementCloud.getModuleMapping();
                    break;
                case "random":
                    System.out.println("Random Placement");
                    org.fog.placement.ModulePlacementRandom modulePlacementRandom = new ModulePlacementRandom(fogDevices, application, moduleMapping);
                    controller.submitApplication(application, 0, modulePlacementRandom);
                    moduleMappingTotal = modulePlacementRandom.getModuleMapping();
                    break;
                case "fog1":
                    System.out.println("Fog1 Placement : order by MI & MIPS asc");
                    ModulePlacementFog modulePlacementFog = new ModulePlacementFog(fogDevices, application, moduleMapping, ModulePlacementFog.TRI_ASC);
                    controller.submitApplication(application, 0, modulePlacementFog);
                    moduleMappingTotal = modulePlacementFog.getModuleMapping();
                    break;
                case "fog2":
                    System.out.println("Fog2 Placement : order by MI & MIPS desc");
                    ModulePlacementFog modulePlacementFog2 = new ModulePlacementFog(fogDevices, application, moduleMapping, ModulePlacementFog.TRI_DESC);
                    controller.submitApplication(application, 0, modulePlacementFog2);
                    moduleMappingTotal = modulePlacementFog2.getModuleMapping();
                    break;
            }


            // 8- Le calcul des latences existant entre les nœuds de Fog.
            System.out.println("Latencies computation...");
            TopologicalGraph graph = computeTopologicalGraph(fogDevices);
            new DelayMatrix_Float(graph, false);

            TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());
            //printAllToAllLatencies();
            System.out.println("LATENCE GLOBALE : " + computeLatency(application, moduleMappingTotal));

            // 9- Le lancement de la simulation.
            CloudSim.startSimulation();
            // 10- L'arrêt de la simulation.
            CloudSim.stopSimulation();

            Log.printLine("Smart Home finished!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Unwanted errors happen");
        }
    }

    private static float computeLatency(Application app, ModuleMapping moduleMapping) {
        List<AppLoop> loops = app.getLoops();
        float latencySUM = 0.0f;

        // Parcourir tous les scénarios
        for (AppLoop loop : loops
        ) {
            FogDevice src = null;
            // Parcourir tous les modules
            for (String module : loop.getModules()
            ) {
                Map<String, List<String>> mapping = moduleMapping.getModuleMapping();
                if (!loop.isStartModule(module)) {
                    FogDevice dest = fogDevices.stream().filter(e -> e.getName() == getKey(mapping, module)).findFirst().orElse(null);
                    if (src != null) {
                        float latency = DelayMatrix_Float.getFastestLink(src.getId(), dest.getId());
                        System.out.println("Latency from " + src.getName() + " To " + dest.getName() + " = " + latency);
                        latencySUM = Float.sum(latencySUM, latency);
                    }
                    src = dest;
                }
            }
        }
        return latencySUM;
    }


    private static <K, V> String getKey(Map<String, List<String>> map, String value) {
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            if (entry.getValue().contains(value)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static FogDevice createFogDevice(String nodeName, long mips, int ram, long upBw, long downBw, int level, double ratePerMips, double busyPower, double idlePower) {

        List<Pe> peList = new ArrayList<Pe>();

        // 3. Create PEs and add these into a list.
        peList.add(new Pe(0, new PeProvisionerOverbooking(mips)));

        int hostId = FogUtils.generateEntityId();

        long storage = 10000000; // host storage
        int bw = 1000000000;

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

        FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(arch, os, vmm, host, time_zone, cost, costPerMem, costPerStorage, costPerBw);

        int right = getRight(nodeName);
        int left = getleft(nodeName);

        FogDevice fogdevice = null;
        try {
            fogdevice = new FogDevice(nodeName, characteristics, new AppModuleAllocationPolicy(hostList), storageList,
                    right, left, getRightLatency(nodeName, right), getLeftLatency(nodeName, left), 10, upBw, downBw, 0, ratePerMips);
        } catch (Exception e) {
            e.printStackTrace();
        }

        fogdevice.setLevel(level);
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
            if ((nb_DC > 1) && (fogId < (nb_DC - 1))) {
                return fogId + 1 + 3;
            }
        }
        return -1;
    }

    /**
     * Creates the fog devices in the physical topology of the simulation.
     *
     * @param userId
     * @param appId
     */
    private static void createFogDevices(int userId, String appId) {
        for (int i=0; i<nbOfDatacenter; i++){
            FogDevice datacenter = createFogDevice("dc-"+i, 1000, 1000*1000, 100, 10000, 0, 0.01, 16*103, 16*83.25);
            datacenter.setParentId(-1);
            fogDevices.add(datacenter);
            //datacenter.set
            for(int j=0;j<nbOfRFOGperDatacenter;j++) {
                addRFOG(nbOfRFOGperDatacenter*i+j, userId, appId, datacenter.getId());
            }
        }
    }

    private static void addRFOG(int id, int userId, String appId, int parentId){
        FogDevice rfog = createFogDevice("rfog-"+id, 500, 10*1000, 10000, 10000, 1, 0.0, 107.339, 83.4333);
        rfog.setParentId(parentId);
        rfog.setUplinkLatency(LatencyDCToRFOG);
        fogDevices.add(rfog);
        for(int i=0;i<nbOfLFOGperRFOG;i++){
            addLFOG(nbOfLFOGperRFOG*id+i, userId, appId, rfog.getId());
        }
    }

    private static void addLFOG(int id, int userId, String appId, int parentId){
        FogDevice lfog = createFogDevice("lfog-"+id, 200, 1000, 1000, 1000, 2, 0.0, 107.339, 83.4333);
        lfog.setParentId(parentId);
        lfog.setUplinkLatency(LatencyRFOGToLFOG);
        fogDevices.add(lfog);
        for(int i=0;i<nbOfGatewayPerLFOG;i++){
            addGw(nbOfGatewayPerLFOG*id+i, userId, appId, lfog.getId());
        }
    }

    private static void addGw(int id, int userId, String appId, int parentId){
        FogDevice gateway = createFogDevice("gw-"+id, 100, 1000, 1000, 1000, 3, 0.0, 107.339, 83.4333);
        gateway.setParentId(parentId);
        gateway.setUplinkLatency(LatencyLFOGToHGW);
        fogDevices.add(gateway);
    }


    static void createSensors(int userId, String appId) {
        int sensor_idx = 0;
        for (FogDevice device : fogDevices) {
            if (device.getName().startsWith("gw")) {
                for (int i = 0; i < nbOfSensorPerGateway; i++) {
                    Sensor sensor = new Sensor("sensor-" + sensor_idx, "data", userId, appId, new DeterministicDistribution(1));
                    sensor.setGatewayDeviceId(device.getId());
                    sensor.setLatency((double) LatencyHGWToSNR);
                    sensors.add(sensor);
                    sensor_idx++;
                }
            }
        }
    }

    private static int generateRand(int min, int max) {
        Random random = new Random();
        return min + random.nextInt(max - min);
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
                    link = new TopologicalLink(fogDevice.getId() - 3, (int) key - 3, childMap.get(key).floatValue(), (float) 30000);
                    graph.addLink(link);
                }
            }


            /* ADD Right Link to Graph */
            if (fogDevice.getRightId() != -1) {
                link = new TopologicalLink(fogDevice.getId() - 3, fogDevice.getRightId() - 3, fogDevice.getRightLatency(), 30000);
                graph.addLink(link);
            }
        }

        //System.out.println(graph.toString());

        return graph;


    }

    private static void printDevices() {
        // System.out.println("\nFog devices : ");
        for (FogDevice fogdev : fogDevices) {
            System.out.println(fogdev.getName() + "  idEntity = " + fogdev.getId() + " up= " + fogdev.getParentId() + " left =" + fogdev.getLeftId() + " leftLatency = " + fogdev.getLeftLatency() + " right =" + fogdev.getRightId() + " rightLatency=" + fogdev.getRightLatency() + " children = " + fogdev.getChildrenIds() + " childrenLatencies =" + fogdev.getChildToLatencyMap() + " Storage = " + fogdev.getVmAllocationPolicy().getHostList().get(0).getStorage() + " |	");
        }

        // System.out.println("\nSensors : ");
        for (Sensor snr : sensors) {
            System.out.println(snr.getName() + "  HGW_ID = " + snr.getGatewayDeviceId() + " TupleType = " + snr.getTupleType() + " Latency = " + snr.getLatency() + " |	");
        }
        // System.out.println("\nActuators : ");
        for (Actuator act : actuators) {
            System.out.println(act.getName() + " GW_ID = " + act.getGatewayDeviceId() + " Act_Type= " + act.getActuatorType() + " Latency = " + act.getLatency() + " |	");
        }
        System.out.println("\n");

    }

    private static void printAllToAllLatencies() {
        // System.out.println("\nFog devices : ");
        for (FogDevice src : fogDevices) {
            for (FogDevice dest : fogDevices) {
                System.out.println("Latency from " + src.getName() + " To " + dest.getName() + " = " + DelayMatrix_Float.getFastestLink(src.getId(), dest.getId()));
            }
        }
    }

    /**
     * Function to create the EEG Tractor Beam game application in the DDF model.
     *
     * @param appId  unique identifier of the application
     * @param userId identifier of the user of the application
     * @return
     */
    @SuppressWarnings({"serial"})
    private static Application createApplication(String appId, int userId) {

        Application application = Application.createApplication(appId, userId); // creates an empty application model (empty directed graph)

        // a- ajout des services
        for (int i=0; i<Nb_Service; i++){
            application.addAppModule("service-"+i, generateRand(100, 1000), generateRand(100, 1000));
            // c- ajout des taux de production des données (de sortie) en fonction du nombre des données traitées (d'entrée)
            application.addTupleMapping("service-" + i, "data", "data", new FractionalSelectivity(0.1));
        }

        // b- ajout des dépendances de données entre les services
        for (int i = 0; i < Nb_Service; i++) {
            if (i < 100) {
                application.addAppEdge("data", "service-" + i, 10, 15, "data", Tuple.UP, AppEdge.SENSOR);
            } else {
                for (int j = 0; j < Nb_Service; j++) {
                    application.addAppEdge("service-" + i, "service-" + j, 1000, 1500, "data", Tuple.UP, AppEdge.MODULE);
                    application.addAppEdge("service-" + i, "service-" + j, 1000, 1500, "data", Tuple.DOWN, AppEdge.MODULE);
                }
            }
        }
//        for (FogDevice enfant : fogDevices) {
//
//            fogDevices.stream().filter(e -> e.getId() == enfant.getParentId())
//                    .findFirst().ifPresent(
//                            parent -> application.addAppEdge("service-" + enfant.getId(), "service-" + parent.getId(), 1000, 1500, "data", Tuple.UP, AppEdge.MODULE)
//                    );
//        }
        //for (Sensor sensor : sensors) {
        //    fogDevices.stream().filter(e -> e.getId() == sensor.getGatewayDeviceId())
        //            .findFirst().ifPresent(
        //                    gateway -> application.addAppEdge(sensor.getTupleType(), "service-" + gateway.getId(), 1000, 1500, "data", Tuple.UP, AppEdge.SENSOR)
        //            );
        //}

        // d- définition de l'application loop pour la mesure des latences dans les entités physiques.
        final AppLoop loop1 = new AppLoop(new ArrayList<>() {{
            add("sensor-0");
            add("service-1");
            add("service-257");
            add("service-976");
        }});
        List<AppLoop> loops = new ArrayList<>() {{
            add(loop1);
        }};
        application.setLoops(loops);

        return application;
    }
}
