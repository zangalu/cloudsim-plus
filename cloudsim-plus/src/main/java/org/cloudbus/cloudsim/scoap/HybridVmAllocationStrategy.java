package org.cloudbus.cloudsim.scoap;

import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.Simulation;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudsimplus.listeners.CloudletVmEventInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HybridVmAllocationStrategy extends AbstractVmAllocationStrategy
{

    int isteresi;
    private double deactivationTime;

    public HybridVmAllocationStrategy(Map<String, String> simulationConfigMap,
        ArrayList<VMOnDemand> ondemandVMs)
    {
        super(simulationConfigMap, ondemandVMs);
        isteresi = Integer.valueOf(simulationConfigMap.get(ISTERESI_PROPERTY));
        deactivationTime = Double.valueOf(simulationConfigMap.get(DEACTIVATION_TIME_PROPERTY));
    }

    public Threshold createAndSubmitVmsAndCloudlets(DatacenterBroker broker, List<Cloudlet> cloudletList, List<Vm> hiddleVmsList, List<Vm> vmList, RequestsArrivalGenerator arrivalGenerator, ArrayList<Threshold> thresholds, Threshold currentThreshold, Simulation simulation, int simulationTime) {
        return super.createAndSubmitVmsAndCloudlets(broker,cloudletList,hiddleVmsList,vmList, arrivalGenerator,thresholds,currentThreshold,simulation,simulationTime);
    }


    @Override
    public Threshold thresholdStrategy(ArrayList<Threshold> planThresholds, final Threshold currentThreshold,
        RequestsArrivalGenerator arrivalGenerator, Simulation simulation) {

        //assegno le deactivation thresholds
        for(int i=0;i<(planThresholds.size()-isteresi);i++)
        {
            if(i<isteresi){
                planThresholds.get(i).deactivationWorkload=0;
            }

            planThresholds.get(i+isteresi).deactivationWorkload=planThresholds.get(i).WorkLoad;
        }
        /*for( Threshold ts: planThresholds){
            System.out.println("Soglia con id:"+ts.Id+ "e workload teorico:"+ts.WorkLoad+" e deactivation :"+ts.deactivationWorkload);
        }*/


        currentThreshold.deactivationTime= simulation.clock()+this.deactivationTime;
        return super.thresholdStrategy(planThresholds,currentThreshold,arrivalGenerator, simulation);
    }


    @Override
    public void destroyHiddleVms(CloudletVmEventInfo event, DatacenterBroker broker, List<Vm> hiddleVmsList,
        Threshold currentThreshold, Simulation simulation)
    {
        //decido se spegnere VMs dopo il loro deactivation time in base all'arrival rate attuale
        if(simulation.clock()>=currentThreshold.deactivationTime)
        {
            super.destroyHiddleVms(event, broker, hiddleVmsList, currentThreshold, simulation);
        }
    }

    @Override
    public Vm findVmToRemove(List<Vm> hiddleVmsList)
    {
        return null;
    }
}
