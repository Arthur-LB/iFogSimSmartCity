package org.fog.test.perfeval.ligma;

import java.util.*;

import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.FogDevice;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacement;

public class ModulePlacementFog extends ModulePlacement {

    public static final int TRI_ASC = 1;
    public static final int TRI_DESC = -1;
    private int tri;
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

    public ModulePlacementFog(List<FogDevice> fogDevices, Application application,
                              ModuleMapping moduleMapping, int tri){
        this.setFogDevices(fogDevices);
        this.setApplication(application);
        this.setModuleMapping(moduleMapping);
        this.setModuleToDeviceMap(new HashMap<String, List<Integer>>());
        this.setDeviceToModuleMap(new HashMap<Integer, List<AppModule>>());
        this.setModuleInstanceCountMap(new HashMap<Integer, Map<String, Integer>>());
        this.tri = tri;
        for(FogDevice device : getFogDevices())
            getModuleInstanceCountMap().put(device.getId(), new HashMap<String, Integer>());
        mapModules();
        placeRemainingModules();
    }

    public void placeRemainingModules(){
        ArrayList<AppModule> modulesToPlace = getModulesToPlace(
                this.getModuleMapping().getModuleMapping().values().stream()
                        .reduce(new ArrayList<>(), (a, b) -> {
                            a.addAll(b);
                            return a;
                        }));
        Collections.sort(modulesToPlace, new Comparator<AppModule>() {
            @Override
            public int compare(AppModule am1, AppModule am2) {
                int res;
                switch (tri) {
                    case TRI_ASC :
                        res = (int)(am1.getMips() - am2.getMips());
                        break;
                    case TRI_DESC :
                        res = (int) (am2.getMips() - am1.getMips());
                        break;
                    default :
                        throw new IllegalStateException("Unexpected value for sorting : " + tri);
                };
                return res;
            }
        });
        List<FogDevice> fogDevices = getFogDevices();
        Collections.sort(fogDevices, new Comparator<FogDevice>() {
            @Override
            public int compare(FogDevice d1, FogDevice d2) {
                int res;
                switch (tri) {
                    case TRI_ASC :
                        res = (int)(d1.getHost().getAvailableMips() - d2.getHost().getAvailableMips());
                        break;
                    case TRI_DESC :
                        res = (int) (d2.getHost().getAvailableMips() - d1.getHost().getAvailableMips());
                        break;
                    default :
                        throw new IllegalStateException("Unexpected value for sorting : " + tri);
                };
                return res;
            }
        });
        int modulesParDevice = modulesToPlace.size()/ fogDevices.size();
        for (int i = 0; i<modulesToPlace.size(); i++){
            int j = i;
            if (i/modulesParDevice<fogDevices.size()) {
                while (!createModuleInstanceOnDevice(modulesToPlace.get(i), fogDevices.get(j / modulesParDevice))) {j+=tri;}
            } else {
                while (!createModuleInstanceOnDevice(modulesToPlace.get(i), fogDevices.get(fogDevices.size() - 1))) {j+=tri;}
            }
        }

    }

    private ArrayList<AppModule> getModulesToPlace(List<String> placedModules){
        Application app = getApplication();
        ArrayList<AppModule> modulesToPlace = new ArrayList<>();
        for(AppModule module : app.getModules()){
            if(!placedModules.contains(module.getName()))
                modulesToPlace.add(module);
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
