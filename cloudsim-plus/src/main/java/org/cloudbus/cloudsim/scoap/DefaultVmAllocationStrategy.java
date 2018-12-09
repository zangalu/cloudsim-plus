package org.cloudbus.cloudsim.scoap;

import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.core.Simulation;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudsimplus.listeners.CloudletVmEventInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DefaultVmAllocationStrategy extends AbstractVmAllocationStrategy
{
    public DefaultVmAllocationStrategy(Map<String, String> simulationConfigMap,
        ArrayList<VMOnDemand> ondemandVMs, ScoapStatistics statistics)
    {
        super(simulationConfigMap, ondemandVMs, statistics);
    }

    @Override
    public void destroyIdleVMs(CloudletVmEventInfo event, DatacenterBroker broker, List<Vm> hiddleVmsList,
        Threshold currentThreshold, Simulation simulation,
        RequestsArrivalGenerator arrivalGenerator) {

       super.destroyIdleVMs(event,broker,hiddleVmsList,currentThreshold,simulation, arrivalGenerator);
    }

    @Override
    public Vm findVmToRemove(List<Vm> hiddleVmsList) {
        return hiddleVmsList.stream().filter(vm -> vm.getMips() == VM_XLARGE_MIPS || vm.getMips() == VM_LARGE_MIPS || vm.getMips() == VM_MID_MIPS || vm.getMips() == VM_SMALL_MIPS).findFirst().get();
    }

}
