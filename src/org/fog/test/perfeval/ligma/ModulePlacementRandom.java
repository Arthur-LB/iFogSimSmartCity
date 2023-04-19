package org.fog.test.perfeval.ligma;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.FogDevice;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacement;

public class ModulePlacementRandom extends ModulePlacement {

    private ModuleMapping moduleMapping;

    @Override
    protected void mapModules() {
        List<FogDevice> devices = getFogDevices();
        Map<String, List<String>> mapping = moduleMapping.getModuleMapping();
        for(String deviceName : mapping.keySet()){
            FogDevice device = getDeviceByName(deviceName);
            for(String moduleName : mapping.get(deviceName)){

                AppModule module = getApplication().getModuleByName(moduleName);
                if(module == null)
                    continue;
                // We try to create modules on devices and
                // if it is not possible (ex: not enough RAM), we try on another random one
                while(!createModuleInstanceOnDevice(module, device)) {
                    device = devices.get((int)(Math.random() * devices.size()));
                }
                //getModuleInstanceCountMap().get(device.getId()).put(moduleName, mapping.get(deviceName).get(moduleName));
            }
        }
    }

    public ModulePlacementRandom(List<FogDevice> fogDevices, Application application,
                                  ModuleMapping moduleMapping){
        this.setFogDevices(fogDevices);
        this.setApplication(application);
        this.setModuleMapping(moduleMapping);
        this.setModuleToDeviceMap(new HashMap<String, List<Integer>>());
        this.setDeviceToModuleMap(new HashMap<Integer, List<AppModule>>());
        this.setModuleInstanceCountMap(new HashMap<Integer, Map<String, Integer>>());
        for(FogDevice device : getFogDevices())
            getModuleInstanceCountMap().put(device.getId(), new HashMap<String, Integer>());
        // Place remaining modules into datacenters
        this.placeRemainingModules();
        // map all modules
        mapModules();
    }

    public void placeRemainingModules(){
        ArrayList<String> modulesToPlace = getModulesToPlace(
                this.getModuleMapping().getModuleMapping().values().stream()
                        .reduce(new ArrayList<>(), (a, b) -> {
                            a.addAll(b);
                            return a;
                        }));
        List<FogDevice> devices = getFogDevices();
        for (String module : modulesToPlace) {
            int index = (int)(Math.random() * devices.size());
            moduleMapping.addModuleToDevice(module, devices.get(index).getName());
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

    public ModuleMapping getModuleMapping() {
        return moduleMapping;
    }
    public void setModuleMapping(ModuleMapping moduleMapping) {
        this.moduleMapping = moduleMapping;
    }


}
