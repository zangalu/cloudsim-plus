package org.cloudbus.cloudsim.scoap;

import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.Simulation;
import org.cloudbus.cloudsim.provisioners.ResourceProvisioner;
import org.cloudbus.cloudsim.provisioners.ResourceProvisionerSimple;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudsimplus.autoscaling.ConfigurableHorizontalVmScaling;
import org.cloudsimplus.autoscaling.ConfigurableHorizontalVmScalingSimple;
import org.cloudsimplus.listeners.CloudletVmEventInfo;

import java.util.*;
import java.util.stream.IntStream;

public abstract class AbstractVmAllocationStrategy implements VmAllocationStrategy
{

    public static final String VM_MID_PES_PROPERTY = "vm_mid_pes";
    public static final String VM_LARGE_PES_PROPERTY = "vm_large_pes";
    public static final String VM_SMALL_PES_PROPERTY = "vm_small_pes";

    public static final String VM_SMALL_RAM_PROPERTY = "vm_small_ram_MB";
    public static final String VM_MID_RAM_PROPERTY = "vm_mid_ram_MB";
    public static final String VM_LARGE_RAM_PROPERTY = "vm_large_ram_MB";

    public static final String VM_SMALL_MIPS_PROPERTY = "vm_small_MIPS";
    public static final String VM_MID_MIPS_PROPERTY = "vm_mid_MIPS";
    public static final String VM_LARGE_MIPS_PROPERTY = "vm_large_MIPS";

    public static final String ISTERESI_PROPERTY= "isteresi_sec";
    public static final String DEACTIVATION_TIME_PROPERTY="deactivationTime_sec";

    private int numberOfCreatedCloudlets;
    private int createdVmsProgressionID;
    private long VM_SMALL_PES;
    private long VM_MID_PES;
    private long VM_LARGE_PES;
    private int VM_SMALL_RAM;
    private int VM_MID_RAM;
    private int VM_LARGE_RAM;
    private long VM_SMALL_MIPS;
    private long VM_MID_MIPS;
    private long VM_LARGE_MIPS;
    ResourceProvisioner resourceProvisioner;

    private int mipsCapacity;


    AbstractVmAllocationStrategy(Map<String,String> simulationConfigMap){

        VM_SMALL_PES = Integer.valueOf(simulationConfigMap.get(VM_SMALL_PES_PROPERTY));
        VM_MID_PES = Integer.valueOf(simulationConfigMap.get(VM_MID_PES_PROPERTY));
        VM_LARGE_PES = Integer.valueOf(simulationConfigMap.get(VM_LARGE_PES_PROPERTY));

        VM_SMALL_RAM = Integer.valueOf(simulationConfigMap.get(VM_SMALL_RAM_PROPERTY));
        VM_MID_RAM = Integer.valueOf(simulationConfigMap.get(VM_MID_RAM_PROPERTY));
        VM_LARGE_RAM = Integer.valueOf(simulationConfigMap.get(VM_LARGE_RAM_PROPERTY));

        VM_SMALL_MIPS = Integer.valueOf(simulationConfigMap.get(VM_SMALL_MIPS_PROPERTY));
        VM_MID_MIPS = Integer.valueOf(simulationConfigMap.get(VM_MID_MIPS_PROPERTY));
        VM_LARGE_MIPS = Integer.valueOf(simulationConfigMap.get(VM_LARGE_MIPS_PROPERTY));

    }

    @Override
    public Threshold createAndSubmitVmsAndCloudlets(DatacenterBroker broker, List<Cloudlet> cloudletList, List<Vm> hiddleVmsList, List<Vm> vmList, RequestsArrivalGenerator arrivalGenerator, ArrayList<Threshold> thresholds, Threshold currentThreshold, Simulation simulation, int simulationTime) {

        List<Vm> newVmList = new ArrayList<>();
        List<Cloudlet> newCloudletList = new ArrayList<>();
        Vm newAvailableVm;
        Threshold newThreshold = currentThreshold;

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

        else {

            newThreshold = thresholdStrategy(thresholds, currentThreshold, arrivalGenerator, simulation);
            double totalCapacity = sumCapacity(broker);
            if(totalCapacity<newThreshold.getWorkLoad() && hiddleVmsList.size()==0){
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
            if(totalCapacity>newThreshold.getWorkLoad()){
                destroyHiddleVms(null, broker, hiddleVmsList, currentThreshold, simulation);
            }
        }
        return newThreshold;

    }

    private Cloudlet createCloudlet(Vm vm, RequestsArrivalGenerator arrivalGenerator, DatacenterBroker broker, List<Vm> hiddleVmsList, Threshold currentThreshold, Simulation simulation, int simulationTime) {

        long length = 0; //in Million Structions (MI)

        while ( (vm.getMips()*100 - length )>0 && arrivalGenerator.allocateRequest()>0){

            length += arrivalGenerator.getRequests().poll();

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

    public Threshold thresholdStrategy(ArrayList<Threshold> planThresholds, final Threshold currentThreshold,
        RequestsArrivalGenerator arrivalGenerator, Simulation simulation) {

        Threshold localThreshold = currentThreshold;
        OptionalInt indexOpt = findThrehsoldIndex(planThresholds, localThreshold);

        int index = indexOpt.getAsInt();
        System.out.println("INDICE PRIMA -->"+index);

        if(index < planThresholds.size()-1 && arrivalGenerator.getInstantWorkload() > currentThreshold.getWorkLoad()){
            localThreshold = planThresholds.get(index+1);
        }
        else if (index>0 && arrivalGenerator.getInstantWorkload() < currentThreshold.getWorkLoad())
        {
            localThreshold = planThresholds.get(index-1);
        }

        index = findThrehsoldIndex(planThresholds, localThreshold).getAsInt();
        System.out.println("INDICE DOPO -->"+index);
        System.out.println("Instant workload"  +arrivalGenerator.getInstantWorkload());
        System.out.println("Current threshold "+localThreshold.getWorkLoad());
        return localThreshold;

    }

    private OptionalInt findThrehsoldIndex(ArrayList<Threshold> planThresholds, Threshold currentThreshold)
    {
        return IntStream.range(0, planThresholds.size())
            .filter(i -> currentThreshold.equals(planThresholds.get(i)))
            .findFirst();
    }


    private List<Vm> createListOfScalableVms(){

        int numberOfVmsToCreate =1;

        return createListOfHorizontalScalableVms(numberOfVmsToCreate);

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

        final Vm vm = new VmSimple(id,VM_MID_MIPS, VM_MID_PES)
            .setRam(VM_MID_RAM).setBw(1000).setSize(10000)
            .setCloudletScheduler(new CloudletSchedulerSpaceShared());

        vm.getUtilizationHistory().enable();
        return vm;
    }

    public void destroyHiddleVms(CloudletVmEventInfo event, DatacenterBroker broker, List<Vm> hiddleVmsList, Threshold currentThreshold, Simulation simulation){
        if(resourceProvisioner == null)
        {
            resourceProvisioner = new ResourceProvisionerSimple();
        }

        if (null != event) {

            resourceProvisioner = new ResourceProvisionerSimple();
            resourceProvisioner.deallocateResourceForVm(event.getVm());

            broker.setVmDestructionDelayFunction(vm -> 1.0);

            System.out.println("Destroying VM at execution termination " + event.getVm());
        }

        for(Vm vm : broker.getVmWaitingList()) {
            hiddleVmsList.add(vm);
        }

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

    public abstract Vm findVmToRemove(List<Vm> hiddleVmsList);
}
