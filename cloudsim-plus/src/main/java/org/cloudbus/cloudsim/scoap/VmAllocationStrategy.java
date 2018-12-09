package org.cloudbus.cloudsim.scoap;

import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.Simulation;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudsimplus.listeners.CloudletVmEventInfo;

import java.util.ArrayList;
import java.util.List;

public interface VmAllocationStrategy
{
    Threshold createAndSubmitVmsAndCloudlets(DatacenterBroker broker, List<Cloudlet> cloudletList, List<Vm> hiddleVmsList, List<Vm> vmList, RequestsArrivalGenerator arrivalGenerator, ArrayList<Threshold> thresholds, Threshold currentThreshold, Simulation simulation, int simulationTime);

    void calculateOnDemandCost(CloudletVmEventInfo event, Vm vm, Threshold currentThreshold,
        RequestsArrivalGenerator arrivalGenerator);

    void destroyIdleVMs(CloudletVmEventInfo event, DatacenterBroker broker, List<Vm> hiddleVmsList,
        Threshold currentThreshold, Simulation simulation,
        RequestsArrivalGenerator arrivalGenerator);

    double getTotalCost();
}
