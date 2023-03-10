package org.fog.placement;

import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.FogDevice;
import org.fog.test.perfeval.SmartCity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModulePlacementRandom extends ModulePlacement{

	private ModuleMapping moduleMapping;

	@Override
	protected void mapModules() {
		Application application = getApplication();
		for(AppModule module : application.getModules()){
			// Find the fog device with the highest processing capacity
			FogDevice selectedDevice = null;
			List<FogDevice> fogDevices = getFogDevices();
			int random = (int) (Math.random() * fogDevices.size());
			selectedDevice = fogDevices.get(random);

			// Add the module to the selected fog device
			moduleMapping.addModuleToDevice(module.getName(), selectedDevice.getName());
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
		mapModules();
	}
	
	
	public ModuleMapping getModuleMapping() {
		return moduleMapping;
	}
	public void setModuleMapping(ModuleMapping moduleMapping) {
		this.moduleMapping = moduleMapping;
	}

	
}
