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
import java.util.List;
import java.util.stream.IntStream;

public abstract class AbstractVmAllocationStrategy implements VmAllocationStrategy
{


    public static final String VM_SMALL_PES_PROPERTY = "vm_small_pes";
    public static final String VM_MID_PES_PROPERTY = "vm_mid_pes";
    public static final String VM_LARGE_PES_PROPERTY = "vm_large_pes";
    public static final String VM_XLARGE_PES_PROPERTY = "vm_xlarge_pes";

    public static final String VM_SMALL_RAM_PROPERTY = "vm_small_ram_MB";
    public static final String VM_MID_RAM_PROPERTY = "vm_mid_ram_MB";
    public static final String VM_LARGE_RAM_PROPERTY = "vm_large_ram_MB";
    public static final String VM_XLARGE_RAM_PROPERTY = "vm_xlarge_ram_MB";

    public static final String VM_SMALL_MIPS_PROPERTY = "vm_small_MIPS";
    public static final String VM_MID_MIPS_PROPERTY = "vm_mid_MIPS";
    public static final String VM_LARGE_MIPS_PROPERTY = "vm_large_MIPS";
    public static final String VM_XLARGE_MIPS_PROPERTY = "vm_large_MIPS";

    public static final String ISTERESI_PROPERTY= "isteresi_sec";
    public static final String DEACTIVATION_TIME_PROPERTY="deactivationTime_sec";

    public static final String M_1_SMALL = "m1-small";

    public static final String C_1_MEDIUM = "c1-medium";

    public static final String M_1_LARGE = "m1-large";

    public static final String C_1_XLARGE = "c1-xlarge";

    public static final String SIMULATION_SCALE_FACTOR_PROPERTY ="scale-factor";

    public static final String SLOW_START_PROPERTY ="slow-start";

    private final ArrayList<VMOnDemand> ondemandVMs;

    private int numberOfCreatedCloudlets;
    private int createdVmsProgressionID;
    public long VM_SMALL_PES;
    public long VM_MID_PES;
    public long VM_LARGE_PES;
    public long VM_XLARGE_PES;
    public int VM_SMALL_RAM;
    public int VM_MID_RAM;
    public int VM_LARGE_RAM;
    public int VM_XLARGE_RAM;
    public long VM_SMALL_MIPS;
    public long VM_MID_MIPS;
    public long VM_LARGE_MIPS;
    public long VM_XLARGE_MIPS;
    ResourceProvisioner resourceProvisioner;


    private double totalCost;

    private double running_payment_small_vm;

    private double running_payment_medium_vm;

    private double running_payment_large_vm;

    private double running_payment_xlarge_vm;

    private boolean slowStart;

    private ScoapStatistics statistics;

    private int slowStartFactor;

    AbstractVmAllocationStrategy(Map<String, String> simulationConfigMap,
        ArrayList<VMOnDemand> ondemandVMs, ScoapStatistics statistics){

        this.ondemandVMs=ondemandVMs;
        this.statistics=statistics;
        this.slowStart=Boolean.valueOf(simulationConfigMap.get(SLOW_START_PROPERTY));

        VM_SMALL_PES = Integer.valueOf(simulationConfigMap.get(VM_SMALL_PES_PROPERTY));
        VM_MID_PES = Integer.valueOf(simulationConfigMap.get(VM_MID_PES_PROPERTY));
        VM_LARGE_PES = Integer.valueOf(simulationConfigMap.get(VM_LARGE_PES_PROPERTY));
        VM_XLARGE_PES = Integer.valueOf(simulationConfigMap.get(VM_XLARGE_PES_PROPERTY));

        VM_SMALL_RAM = Integer.valueOf(simulationConfigMap.get(VM_SMALL_RAM_PROPERTY));
        VM_MID_RAM = Integer.valueOf(simulationConfigMap.get(VM_MID_RAM_PROPERTY));
        VM_LARGE_RAM = Integer.valueOf(simulationConfigMap.get(VM_LARGE_RAM_PROPERTY));
        VM_XLARGE_RAM = Integer.valueOf(simulationConfigMap.get(VM_XLARGE_RAM_PROPERTY));

        VM_SMALL_MIPS = Integer.valueOf(simulationConfigMap.get(VM_SMALL_MIPS_PROPERTY));
        VM_MID_MIPS = Integer.valueOf(simulationConfigMap.get(VM_MID_MIPS_PROPERTY));
        VM_LARGE_MIPS = Integer.valueOf(simulationConfigMap.get(VM_LARGE_MIPS_PROPERTY));
        VM_XLARGE_MIPS = Integer.valueOf(simulationConfigMap.get(VM_XLARGE_MIPS_PROPERTY));

        slowStartFactor=2;
        readCostOnDemand();
    }

    protected  void readCostOnDemand(){

        Optional<VMOnDemand> server = ondemandVMs.stream().filter(s -> s.name.equals(M_1_SMALL)).findFirst();
        running_payment_small_vm = server.get().getRunning_payment();

        server = ondemandVMs.stream().filter(s -> s.name.equals(C_1_MEDIUM)).findFirst();
        running_payment_medium_vm = server.get().getRunning_payment();

        server = ondemandVMs.stream().filter(s -> s.name.equals(M_1_LARGE)).findFirst();
        running_payment_large_vm = server.get().getRunning_payment();

        server = ondemandVMs.stream().filter(s -> s.name.equals(C_1_XLARGE)).findFirst();
        running_payment_xlarge_vm = server.get().getRunning_payment();

    };

    @Override
    public Threshold createAndSubmitVmsAndCloudlets(DatacenterBroker broker, List<Cloudlet> cloudletList, List<Vm> hiddleVmsList, List<Vm> vmList, RequestsArrivalGenerator arrivalGenerator, ArrayList<Threshold> thresholds, Threshold currentThreshold, Simulation simulation, int simulationTime) {

        Map<String, ArrayList<Vm>> newVmList = new HashMap<>();
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

            if(newAvailableVm.getMips()==VM_SMALL_MIPS){
                ArrayList<Vm> smallVM = new ArrayList<>();
                smallVM.add(newAvailableVm);
                newVmList.put(M_1_SMALL,smallVM);
            }
            if(newAvailableVm.getMips()==VM_MID_MIPS){
                ArrayList<Vm> midVM = new ArrayList<>();
                midVM.add(newAvailableVm);
                newVmList.put(C_1_MEDIUM,midVM);
            }
            if(newAvailableVm.getMips()==VM_LARGE_MIPS){
                ArrayList<Vm> largeVM = new ArrayList<>();
                largeVM.add(newAvailableVm);
                newVmList.put(M_1_LARGE,largeVM);
            }
            if(newAvailableVm.getMips()==VM_XLARGE_MIPS){
                ArrayList<Vm> xlargeVM = new ArrayList<>();
                xlargeVM.add(newAvailableVm);
                newVmList.put(C_1_XLARGE,xlargeVM);
            }
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

                int small=0;
                int mid=0;
                int large=0;
                int xlarge=0;

                for(SingleVM vmToActivate : newThreshold.getVMsToActivate()){

                    if(vmToActivate.getVm_name().equals(M_1_SMALL)){
                        small=vmToActivate.getNum();
                    }
                    if(vmToActivate.getVm_name().equals(C_1_MEDIUM)){
                        mid=vmToActivate.getNum();
                    }
                    if(vmToActivate.getVm_name().equals(M_1_LARGE)){
                        large=vmToActivate.getNum();
                    }
                    if(vmToActivate.getVm_name().equals(C_1_XLARGE)){
                        xlarge=vmToActivate.getNum();
                    }

                }

                newVmList = createListOfScalableVms(small, mid, large, xlarge);
                vmList.addAll(newVmList.get(M_1_SMALL));
                vmList.addAll(newVmList.get(C_1_MEDIUM));
                vmList.addAll(newVmList.get(M_1_LARGE));
                vmList.addAll(newVmList.get(C_1_XLARGE));


                for(Vm vm : newVmList.get(M_1_SMALL)) {
                    Cloudlet cloudlet = createCloudlet(vm, arrivalGenerator, broker, hiddleVmsList,currentThreshold, simulation, simulationTime);
                    newCloudletList.add(cloudlet);
                }

                for(Vm vm : newVmList.get(C_1_MEDIUM)) {
                    Cloudlet cloudlet = createCloudlet(vm, arrivalGenerator, broker, hiddleVmsList,currentThreshold, simulation, simulationTime);
                    newCloudletList.add(cloudlet);
                }

                for(Vm vm : newVmList.get(M_1_LARGE)) {
                    Cloudlet cloudlet = createCloudlet(vm, arrivalGenerator, broker, hiddleVmsList,currentThreshold, simulation, simulationTime);
                    newCloudletList.add(cloudlet);
                }

                for(Vm vm : newVmList.get(C_1_XLARGE)) {
                    Cloudlet cloudlet = createCloudlet(vm, arrivalGenerator, broker, hiddleVmsList,currentThreshold, simulation, simulationTime);
                    newCloudletList.add(cloudlet);
                }

                broker.submitVmList(newVmList.get(M_1_SMALL));
                broker.submitVmList(newVmList.get(C_1_MEDIUM));
                broker.submitVmList(newVmList.get(M_1_LARGE));
                broker.submitVmList(newVmList.get(C_1_XLARGE));
                broker.submitCloudletList(newCloudletList);
                cloudletList.addAll(newCloudletList);
            }
            if(totalCapacity>newThreshold.getWorkLoad()){
                destroyIdleVMs(null, broker, hiddleVmsList, currentThreshold, simulation, arrivalGenerator);
            }
        }
        return newThreshold;
    }


    /*
    Create cloudlet and adding listeners for deallocation and cost calculation
     */
    private Cloudlet createCloudlet(Vm vm, RequestsArrivalGenerator arrivalGenerator, DatacenterBroker broker, List<Vm> hiddleVmsList, Threshold currentThreshold, Simulation simulation, int simulationTime) {

        long length = 0; //in Million Structions (MI)

        while ( (vm.getMips()- length )>0 && arrivalGenerator.allocateRequest()>0){

            length += arrivalGenerator.getRequests().poll();

        }

        long fileSize = 300; //Size (in bytes) before execution
        long outputSize = 300; //Size (in bytes) after execution
        long numberOfCpuCores = vm.getNumberOfPes(); //cloudlet will use all the VM's CPU cores

        //Defines how CPU, RAM and Bandwidth resources are used
        //Sets the same utilization model for all these resources.
        UtilizationModel utilization = new UtilizationModelFull();
        currentThreshold.setActivationTime(arrivalGenerator.getCurrentTime());


        Cloudlet cloudlet = new CloudletSimple(
            numberOfCreatedCloudlets++, length, numberOfCpuCores)
            .addOnUpdateProcessingListener(event-> calculateOnDemandCost(event, vm, currentThreshold, arrivalGenerator))
            .setFileSize(fileSize)
            .setOutputSize(outputSize)
            .setUtilizationModel(utilization)
            .setVm(vm)
            .addOnFinishListener(event -> destroyIdleVMs(event, broker, hiddleVmsList, currentThreshold, simulation, arrivalGenerator));

        System.out.println(vm.getCpuPercentUsage());
        //listener in case we want to perform some action when a cloudlet finish to execute e.g. statistics
        //.addOnFinishListener(event -> checkQueueAndAllocateCloudLets(event));
        cloudlet.setSubmissionDelay(1);
        return cloudlet;
    }

    @Override
    public void calculateOnDemandCost(CloudletVmEventInfo event, Vm vm,
        Threshold currentThreshold, RequestsArrivalGenerator arrivalGenerator)
    {
        double executionTime = arrivalGenerator.getCurrentTime()-currentThreshold.getActivationTime();
        double running_payment = 0.0;

        if(vm.getMips()==VM_SMALL_MIPS){
            running_payment = running_payment_small_vm;
        }
        if(vm.getMips()==VM_MID_MIPS){
            running_payment = running_payment_medium_vm;
        }
        if(vm.getMips()==VM_LARGE_MIPS){
            running_payment = running_payment_large_vm;
        }
        if(vm.getMips()==VM_XLARGE_MIPS){
            running_payment = running_payment_xlarge_vm;
        }

        double currentVmCost =running_payment*executionTime/600;

       totalCost+=(currentVmCost);
        if(currentVmCost>statistics.getCost_Max()){
            statistics.setCost_Max(currentVmCost);
        }
        if(currentVmCost<statistics.getCost_Min()){
            statistics.setCost_Min(currentVmCost);
        }
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
        System.out.println("Instant workload -->"  +arrivalGenerator.getInstantWorkload());
        System.out.println("Current threshold "+localThreshold.getWorkLoad());
        return localThreshold;

    }

    private OptionalInt findThrehsoldIndex(ArrayList<Threshold> planThresholds, Threshold currentThreshold)
    {
        return IntStream.range(0, planThresholds.size())
            .filter(i -> currentThreshold.equals(planThresholds.get(i)))
            .findFirst();
    }


    private Map<String,ArrayList<Vm>> createListOfScalableVms(int newSmallVmNumber, int newMidVmNumber, int newLargeVmNumber, int newXlargeVmNumber){


        if(slowStart){
            newSmallVmNumber*=slowStartFactor;
            newMidVmNumber*=slowStartFactor;
            newLargeVmNumber*=slowStartFactor;
            newXlargeVmNumber*=slowStartFactor;
        }


        return createListOfHorizontalScalableVms(newSmallVmNumber, newMidVmNumber, newLargeVmNumber, newXlargeVmNumber);

    }

    /**
     * Creates a list of initial VMs in which each VM is able to scale horizontally
     * when it is overloaded.
     *
     * @param small number of small VMs to create
     * @param mid number of mid VMs to create
     * @param large number of large VMs to create
     * @return the list of scalable VMs just created
     */
    private Map<String,ArrayList<Vm>>createListOfHorizontalScalableVms(final int small, int mid, int large, int xLarge) {

        Map<String,ArrayList<Vm>> newCreatedVMs = new HashMap<>();
        ArrayList<Vm> newSmallList = new ArrayList<>(small);
        for (int i = 0; i < small; i++) {
            Vm vm = createSmallVm();
            ConfigurableHorizontalVmScaling horizontalScaling = new ConfigurableHorizontalVmScalingSimple();
            horizontalScaling.setUnderloadPredicate(this::isVmUnderloaded);
            horizontalScaling.setVmSupplier(this::createSmallVm);
            horizontalScaling.setOverloadPredicate(this::isVmOverloaded);

            vm.setHorizontalScaling(horizontalScaling);
            newSmallList.add(vm);
        }

        ArrayList<Vm> newMidList = new ArrayList<>(small);
        for (int i = 0; i < mid; i++) {
            Vm vm = createMidVm();
            ConfigurableHorizontalVmScaling horizontalScaling = new ConfigurableHorizontalVmScalingSimple();
            horizontalScaling.setUnderloadPredicate(this::isVmUnderloaded);
            horizontalScaling.setVmSupplier(this::createMidVm);
            horizontalScaling.setOverloadPredicate(this::isVmOverloaded);

            vm.setHorizontalScaling(horizontalScaling);
            newSmallList.add(vm);
        }

        ArrayList<Vm> newLargeList = new ArrayList<>(small);
        for (int i = 0; i < large; i++) {
            Vm vm = createLargeVm();
            ConfigurableHorizontalVmScaling horizontalScaling = new ConfigurableHorizontalVmScalingSimple();
            horizontalScaling.setUnderloadPredicate(this::isVmUnderloaded);
            horizontalScaling.setVmSupplier(this::createLargeVm);
            horizontalScaling.setOverloadPredicate(this::isVmOverloaded);

            vm.setHorizontalScaling(horizontalScaling);
            newSmallList.add(vm);
        }

        ArrayList<Vm> newXLargeList = new ArrayList<>(small);
        for (int i = 0; i < large; i++) {
            Vm vm = createXLargeVm();
            ConfigurableHorizontalVmScaling horizontalScaling = new ConfigurableHorizontalVmScalingSimple();
            horizontalScaling.setUnderloadPredicate(this::isVmUnderloaded);
            horizontalScaling.setVmSupplier(this::createXLargeVm);
            horizontalScaling.setOverloadPredicate(this::isVmOverloaded);

            vm.setHorizontalScaling(horizontalScaling);
            newSmallList.add(vm);
        }

        newCreatedVMs.put("m1-small",newSmallList);
        newCreatedVMs.put("c1-medium",newMidList);
        newCreatedVMs.put("m1-large",newLargeList);
        newCreatedVMs.put("c1-xlarge",newXLargeList);

        return newCreatedVMs;
    }

    /**
     * Creates a Vm object, enabling it to store
     * CPU utilization history which is used
     * to compute a dynamic threshold for CPU vertical scaling.
     *
     * @return the created Vm
     */
    private Vm createSmallVm() {
        return createVm(VM_SMALL_RAM, VM_SMALL_MIPS, VM_SMALL_PES);
    }

    private Vm createMidVm() {
        return createVm(VM_MID_RAM, VM_MID_MIPS, VM_MID_PES);
    }

    private Vm createLargeVm() {
        return createVm(VM_LARGE_RAM, VM_LARGE_MIPS, VM_LARGE_PES);
    }

    private Vm createXLargeVm() {
        return createVm(VM_XLARGE_RAM, VM_XLARGE_MIPS, VM_XLARGE_PES);
    }

    private Vm createVm(int VM_RAM, long VM_MIPS, long VM_PES) {
        final int id = createdVmsProgressionID++;

        final Vm vm = new VmSimple(id,VM_MIPS, VM_PES)
            .setRam(VM_RAM).setBw(1000).setSize(10000)
            .setCloudletScheduler(new CloudletSchedulerSpaceShared());

        vm.getUtilizationHistory().enable();
        return vm;
    }

    @Override
    public void destroyIdleVMs(CloudletVmEventInfo event, DatacenterBroker broker, List<Vm> hiddleVmsList,
        Threshold currentThreshold, Simulation simulation,
        RequestsArrivalGenerator arrivalGenerator){
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
            //calculateOnDemandCost(event, vm,currentThreshold,arrivalGenerator);
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

    @Override
    public double getTotalCost()
    {
        return totalCost;
    }

}
