package org.cloudbus.cloudsim.scoap;

import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.Simulation;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudsimplus.autoscaling.ConfigurableHorizontalVmScaling;
import org.cloudsimplus.autoscaling.ConfigurableHorizontalVmScalingSimple;
import org.cloudsimplus.listeners.CloudletVmEventInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.IntStream;

public class DefaultVmAllocationStrategy implements VmAllocationStrategy
{

    private int numberOfCreatedCloudlets;
    private int createdVmsProgressionID;
    private static final int VM_PES = 8;
    private static final int VM_RAM = 1200;

    @Override
    public void createAndSubmitVmsAndCloudlets(DatacenterBroker broker, List<Cloudlet> cloudletList, List<Vm> hiddleVmsList, List<Vm> vmList, RequestsArrivalGenerator arrivalGenerator, ArrayList<Threshold> thresholds, Threshold currentThreshold, Simulation simulation, int simulationTime) {

        List<Vm> newVmList = new ArrayList<>();
        List<Cloudlet> newCloudletList = new ArrayList<>();
        Vm newAvailableVm;

        Optional<Vm> availableVm = broker.getVmWaitingList().stream().filter(vm -> vm.getCpuPercentUsage()<0.7).findAny();

        // get VM from waiting list
        if(availableVm.isPresent()) {
            System.out.println("VM Present "+availableVm.get().getId()+" with cpu usage "+availableVm.get().getCpuPercentUsage());
            newAvailableVm = availableVm.get();
            Cloudlet cloudlet = createCloudlet(newAvailableVm, arrivalGenerator, broker, hiddleVmsList, currentThreshold, simulation, simulationTime);
            cloudletList.add(cloudlet);
            newVmList.add(newAvailableVm);
            newCloudletList.add(cloudlet);
        }// get VM from hiddle list
        else if(hiddleVmsList.size()>0){
            Vm vm = hiddleVmsList.iterator().next();
            Cloudlet cloudlet = createCloudlet(vm, arrivalGenerator, broker, hiddleVmsList, currentThreshold, simulation, simulationTime);
            hiddleVmsList.remove(vm);
            cloudletList.add(cloudlet);
            broker.submitCloudlet(cloudlet);
        }
        // create new vm according to static plan
        //se workload gestito dalle VM attive < del current workload
        else if(sumCapacity(broker)<thresholdStrategy(thresholds,currentThreshold, arrivalGenerator) && hiddleVmsList.size()==0){
            System.out.println("Create new VM");
            newVmList = createListOfScalableVms();
            vmList.addAll(newVmList);
            for(Vm vm : newVmList) {
                Cloudlet cloudlet = createCloudlet(vm, arrivalGenerator, broker, hiddleVmsList,currentThreshold, simulation, simulationTime);
                newCloudletList.add(cloudlet);
            }
            broker.submitVmList(newVmList);
            broker.submitCloudletList(newCloudletList);
            cloudletList.addAll(newCloudletList);
        }

    }

    private Cloudlet createCloudlet(Vm vm, RequestsArrivalGenerator arrivalGenerator, DatacenterBroker broker, List<Vm> hiddleVmsList, Threshold currentThreshold, Simulation simulation, int simulationTime) {

        long length = 0; //in Million Structions (MI)

        while ( (vm.getMips()*100 - length )>0 && arrivalGenerator.getRequests(simulationTime).size()>0){
            length += arrivalGenerator.allocateRequest();
        }


        long fileSize = 300; //Size (in bytes) before execution
        long outputSize = 300; //Size (in bytes) after execution
        long numberOfCpuCores = vm.getNumberOfPes(); //cloudlet will use all the VM's CPU cores

        //Defines how CPU, RAM and Bandwidth resources are used
        //Sets the same utilization model for all these resources.
        UtilizationModel utilization = new UtilizationModelFull();


        Cloudlet cloudlet = new CloudletSimple(
            numberOfCreatedCloudlets++, length, numberOfCpuCores)
            .setFileSize(fileSize)
            .setOutputSize(outputSize)
            .setUtilizationModel(utilization)
            .setVm(vm)
            .addOnFinishListener(event -> destroyHiddleVms(event, broker, hiddleVmsList, currentThreshold, simulation));

        System.out.println(vm.getCpuPercentUsage());
        //listener in case we want to perform some action when a cloudlet finish to execute e.g. statistics
        //.addOnFinishListener(event -> checkQueueAndAllocateCloudLets(event));
        cloudlet.setSubmissionDelay(1);
        return cloudlet;
    }

    private double sumCapacity(DatacenterBroker broker){
        double totalCapacity = broker.getVmExecList().stream().mapToDouble(t -> t.getMips()).sum();
        System.out.println("total capacity: "+totalCapacity);
        return totalCapacity;
    }

    private double thresholdStrategy(ArrayList<Threshold> thresholds, final Threshold currentThreshold, RequestsArrivalGenerator arrivalGenerator) {

        Threshold localThreshold = currentThreshold;
        OptionalInt indexOpt = IntStream.range(0, thresholds.size())
            .filter(i -> currentThreshold.equals(thresholds.get(i)))
            .findFirst();

        int index = indexOpt.getAsInt();
        System.out.println("INDICE -->"+indexOpt.getAsInt());

        if(index < thresholds.size()-1 && arrivalGenerator.getInstantWorkload() > currentThreshold.getWorkLoad()){
            localThreshold = thresholds.get(indexOpt.getAsInt()+1);
        }
        else if (index>0  && arrivalGenerator.getInstantWorkload() < currentThreshold.getWorkLoad())
        {
            localThreshold = thresholds.get(indexOpt.getAsInt()-1);
        }

        System.out.println("Current threshold "+currentThreshold.getWorkLoad());
        return localThreshold.getWorkLoad();

    }


    private List<Vm> createListOfScalableVms(){

        int numberOfVmsToCreate =1;

        //if(requests.size()>500) {
        return createListOfHorizontalScalableVms(numberOfVmsToCreate);
        //}
        // else{
        //     return createListOfVerticalScalableVms(numberOfVmsToCreate);
        // }

    }

    /**
     * Creates a list of initial VMs in which each VM is able to scale horizontally
     * when it is overloaded.
     *
     * @param numberOfVms number of VMs to create
     * @return the list of scalable VMs
     */
    private List<Vm> createListOfHorizontalScalableVms(final int numberOfVms) {
        List<Vm> newList = new ArrayList<>(numberOfVms);
        for (int i = 0; i < numberOfVms; i++) {
            Vm vm = createVm();
            ConfigurableHorizontalVmScaling horizontalScaling = new ConfigurableHorizontalVmScalingSimple();
            horizontalScaling.setUnderloadPredicate(this::isVmUnderloaded);
            horizontalScaling.setVmSupplier(this::createVm);
            horizontalScaling.setOverloadPredicate(this::isVmOverloaded);

            vm.setHorizontalScaling(horizontalScaling);
            newList.add(vm);
        }

        return newList;
    }

    /**
     * Creates a Vm object, enabling it to store
     * CPU utilization history which is used
     * to compute a dynamic threshold for CPU vertical scaling.
     *
     * @return the created Vm
     */
    private Vm createVm() {
        final int id = createdVmsProgressionID++;

        final Vm vm = new VmSimple(id, 20, VM_PES)
            .setRam(VM_RAM).setBw(1000).setSize(10000)
            .setCloudletScheduler(new CloudletSchedulerSpaceShared());

        vm.getUtilizationHistory().enable();
        return vm;
    }

    private void destroyHiddleVms(CloudletVmEventInfo eventInfo, DatacenterBroker broker, List<Vm> hiddleVmsList, Threshold currentThreshold, Simulation simulation) {


        for(Vm vm : broker.getVmExecList()) {

            if (vm.getCpuPercentUsage() == 0.0 && vm.getCloudletScheduler().getCloudletExecList().size()==0 && vm.getCloudletScheduler().getCloudletWaitingList().size() ==0 && broker.getCloudletWaitingList().isEmpty()) {

                hiddleVmsList.add(vm);
                //vmList.remove(vm);
            }
        }
        if(hiddleVmsList.size()+broker.getVmExecList().size()>currentThreshold.getWorkLoad() && !hiddleVmsList.isEmpty()) {

            Vm vmToRemove = findVmToRemove( hiddleVmsList);
            simulation.send(broker, broker, 1, CloudSimTags.VM_DESTROY, vmToRemove);
            hiddleVmsList.remove(vmToRemove);
        }
    }

    private boolean isVmOverloaded(Vm vm) {
        return vm.getCpuPercentUsage() > 0.7;
    }

    private boolean isVmUnderloaded(Vm vm) {
        return vm.getCpuPercentUsage() == 0.2;
    }

    private Vm findVmToRemove(List<Vm> hiddleVmsList) {
        return hiddleVmsList.stream().filter(vm -> vm.getMips() == 100).findFirst().get();
    }

}
