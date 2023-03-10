package org.fog.placement;

import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;

import java.util.ArrayList;
import java.util.List;

/**
 * le nombre de centre de données est 5, le nombre de RFOG est 10, le nombre de LFOG est 20,
 * le nombre de passerelles est 100 et le nombre de capteurs gérés par une passerelle est 10.
 *  Les capteurs génèrent les données toutes les secondes.
 *  Le nombre d'instances de services est 1000.
 *  Le nombre d'instances de services consommateurs par chaque donnée est 1 (choisi
 * aléatoirement parmi l'ensemble des instances de services).
 *  Le taux de la reproduction des données est fixé par 10% (la relation entre les données d'entrée
 * et les données de sorite d'une instance de service).
 *  La quantité de traitement demandée pour une donnée est calculée aléatoirement de 10 à 100
 * MI.
 *  Les performances de traitement demandées par une instance de service sont calculées
 * aléatoirement de 100 à 1000 MIPS.
 *  La quantité de RAM demandée par une instance de service est calculée aléatoirement de 100
 * à 1000 Mb.
 *  Les performances des nœuds physiques et les latences réseaux sont illustrées dans les
 * tableaux 1 et 2 donnés ci-dessous :
 * Tableau 1 : Performances des noeuds physiques
 * Vitesse de traitement (MIPS) RAM (Mb)
 * Centre de données 1000 1000*1000
 * RFOG 500 10*1000
 * LFOG 200 1000
 * Passerelle 100 1000
 * Tableau 2 : Latences réseaux entre les nœuds physiques
 * Nœud source Nœud destinataire Latence réseau (ms)
 * capteur Passerelle 10
 * Passerelle LFOG 50
 * LFOG RFOG 100
 * RFOG Centre de données 1000
 * Centre de données Centre de données 1000
 *  La durée maximale de la simulation est une heure (1000x60x60).
 *  Le nombre de simulations est 10.
 *  Les résultats présentés pour chaque algorithme sont la moyenne, le maximum et le minimum
 * de la latence du système.1000
 */
public class SmartCityConstants {
    // Configuration variables
    public static final int NUM_DATA_CENTERS = 5;
    public static final int DATA_CENTER_MIPS = 1000;
    public static final int DATA_CENTER_RAM = 1000 * 1000;
    
    public static final int NUM_RFOG = 10;
    public static final int RFOG_MIPS = 500;
    public static final int RFOG_RAM = 10 * 1000;
    public static final int NUM_LFOG = 20;
    public static final int LFOG_MIPS = 200;
    public static final int LFOG_RAM = 1000;
    public static final int NUM_GATEWAYS = 100;
    public static final int GATEWAY_MIPS = 100;
    public static final int GATEWAY_RAM = 1000;
    public static final int NUM_SENSORS_PER_GATEWAY = 10;
    public static final int NUM_SERVICE_INSTANCES = 1000; // 1 service = 1 gateway with 10 sensors
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
    public static final int DATACENTER_TO_DATACENTER_LATENCY = 1000;
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