package org.fog.placement;

import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;

import java.util.ArrayList;
import java.util.List;

public class SmartCityConstants {
    // Configuration variables
    public static final int NUM_DATA_CENTERS = 5;
    public static final int NUM_RFOG = 10;
    public static final int NUM_LFOG = 20;
    public static final int NUM_GATEWAYS = 100;
    public static final int NUM_SENSORS_PER_GATEWAY = 10;
    public static final int NUM_SERVICE_INSTANCES = 1000;
    public static final int NUM_CONSUMERS_PER_DATA = 1;
    public static final double REPRODUCTION_RATE = 0.1;
    public static final int DATA_PROCESSING_REQUIREMENT_MIN = 10;
    public static final int DATA_PROCESSING_REQUIREMENT_MAX = 100;
    public static final int SERVICE_PROCESSING_REQUIREMENT_MIN = 100;
    public static final int SERVICE_PROCESSING_REQUIREMENT_MAX = 1000;
    public static final int SERVICE_RAM_REQUIREMENT_MIN = 100;
    public static final int SERVICE_RAM_REQUIREMENT_MAX = 1000;
    public static final int SENSOR_TO_GATEWAY_LATENCY = 10;
    public static final int GATEWAY_TO_LFOG_LATENCY = 50;
    public static final int LFOG_TO_RFOG_LATENCY = 100;
    public static final int RFOG_TO_DATACENTER_LATENCY = 1000;
    public static final int SIMULATION_TIME = 1000 * 60 * 60; // 1 hour in seconds
    public static final int NUM_SIMULATIONS = 10;

    // Declare FogDevices, Sensors and Actuators
    public static List<FogDevice> datacenters = new ArrayList<FogDevice>();
    public static List<FogDevice> rfogs = new ArrayList<FogDevice>();
    public static List<FogDevice> lfogs = new ArrayList<FogDevice>();
    public static List<FogDevice> gateways = new ArrayList<FogDevice>();
    public static List<Sensor> sensors = new ArrayList<Sensor>();
    public static List<Actuator> actuators = new ArrayList<Actuator>();

    public static String datacenterPrefix = "DC-";
    public static String rfogPrefix = "RFOG-";
    public static String lfogPrefix = "LFOG-";
    public static String gatewayPrefix = "HGW-";
    public static String sensorPrefix = "s-";
}