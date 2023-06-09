package org.fog.placement;

import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.FogDevice;

import java.util.*;

public class ModulePlacementASC extends ModulePlacement{
	@Override
	protected void mapModules() {
		Map<String, List<String>> mapping = moduleMapping.getModuleMapping();
		List<FogDevice> fogDevices = getFogDevices();
		// Sort the fog devices in ascending order of their available MIPS
		fogDevices.sort(Comparator.comparingDouble(FogDevice::getRatePerMips));

		// Sort the service instances in ascending order of their processing requirements (MI)
		Application app = getApplication();
		List<AppModule> appModules = app.getModules();
		appModules.sort(Comparator.comparingDouble(AppModule::getMips));
		// Place the service instances one by one in the fog nodes in sorted order
		for (AppModule appModule : appModules) {
			FogDevice selectedDevice = null;
			for (FogDevice fogDevice : fogDevices) {
				if (fogDevice.getRatePerMips() >= appModule.getMips()) {
					selectedDevice = fogDevice;
					break;
				}
			}
			if (selectedDevice != null) {
				moduleMapping.addModuleToDevice(appModule.getName(), selectedDevice.getName());
			} else {
				// No suitable fog node found for the service instance
				System.out.println("No suitable fog node found for appModule instance: " + appModule.getName());
			}
		}
	}

	public ModulePlacementASC(List<FogDevice> fogDevices, Application application,
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
	
}
