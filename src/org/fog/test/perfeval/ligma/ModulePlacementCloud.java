package org.fog.test.perfeval.ligma;

import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.FogDevice;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacement;
import org.fog.test.perfeval.SmartCityPkg.SmartCityConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModulePlacementCloud extends ModulePlacement {

    private ModuleMapping moduleMapping;

    @Override
    protected void mapModules() {
        Map<String, List<String>> mapping = moduleMapping.getModuleMapping();
        for(String deviceName : mapping.keySet()){
            FogDevice device = getDeviceByName(deviceName);
            for(String moduleName : mapping.get(deviceName)){

                AppModule module = getApplication().getModuleByName(moduleName);
                if(module == null)
                    continue;
                createModuleInstanceOnDevice(module, device);
                //getModuleInstanceCountMap().get(device.getId()).put(moduleName, mapping.get(deviceName).get(moduleName));
            }
        }
    }

    public ModulePlacementCloud(List<FogDevice> fogDevices, Application application,
                                ModuleMapping moduleMapping){
        this.setFogDevices(fogDevices);
        this.setApplication(application);
        this.setModuleMapping(moduleMapping);
        this.setModuleToDeviceMap(new HashMap<>());
        this.setDeviceToModuleMap(new HashMap<>());
        this.setModuleInstanceCountMap(new HashMap<>());
        for(FogDevice device : getFogDevices()) {
            getModuleInstanceCountMap().put(device.getId(), new HashMap<>());
        }
        // Place remaining modules into datacenters
        this.placeRemainingModules();
        // map all modules
        this.mapModules();
    }


    public ModuleMapping getModuleMapping() {
        return moduleMapping;
    }
    public void setModuleMapping(ModuleMapping moduleMapping) {
        this.moduleMapping = moduleMapping;
    }

    public void placeRemainingModules(){
        ArrayList<String> modulesToPlace = getModulesToPlace(
                this.getModuleMapping().getModuleMapping().values().stream()
                .reduce(new ArrayList<>(), (a, b) -> {
                    a.addAll(b);
                    return a;
                }));
        ArrayList<FogDevice> datacenters_list = this.findDeviceStartingWith(SmartCityConstants.datacenterPrefix);
        for (String module : modulesToPlace){
            int index = (int)(Math.random() * datacenters_list.size());
            moduleMapping.addModuleToDevice(module, datacenters_list.get(index).getName());
        }
    }

    private ArrayList<String> getModulesToPlace(List<String> placedModules){
        Application app = getApplication();
        ArrayList<String> modulesToPlace = new ArrayList<String>();
        for(AppModule module : app.getModules()){
            if(!placedModules.contains(module.getName()))
                modulesToPlace.add(module.getName());
        }
        return modulesToPlace;
    }

    public ArrayList<FogDevice> findDeviceStartingWith(String searchString) {
        ArrayList<FogDevice> devices = new ArrayList<FogDevice>();
        for (FogDevice device : getFogDevices()) {
            String deviceName = device.getName();
            if(deviceName.startsWith(searchString)){
                devices.add(device);
            }
        }
        return devices;
    }
}
