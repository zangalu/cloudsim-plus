package org.cloudbus.cloudsim.scoap;

import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.core.Simulation;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudsimplus.listeners.CloudletVmEventInfo;

import java.util.List;
import java.util.Map;

public class DefaultVmAllocationStrategy extends AbstractVmAllocationStrategy
{
    public DefaultVmAllocationStrategy(Map<String, String> simulationConfigMap)
    {
        super(simulationConfigMap);
    }

    @Override
    public void destroyHiddleVms(CloudletVmEventInfo event, DatacenterBroker broker, List<Vm> hiddleVmsList, Threshold currentThreshold, Simulation simulation) {

       super.destroyHiddleVms(event,broker,hiddleVmsList,currentThreshold,simulation);
    }

    @Override
    public Vm findVmToRemove(List<Vm> hiddleVmsList) {
        return hiddleVmsList.stream().filter(vm -> vm.getMips() == 50).findFirst().get();
    }

}
