/*
 * CloudSim Plus: A modern, highly-extensible and easier-to-use Framework for
 * Modeling and Simulation of Cloud Computing Infrastructures and Services.
 * http://cloudsimplus.org
 *
 *     Copyright (C) 2015-2018 Universidade da Beira Interior (UBI, Portugal) and
 *     the Instituto Federal de Educação Ciência e Tecnologia do Tocantins (IFTO, Brazil).
 *
 *     This file is part of CloudSim Plus.
 *
 *     CloudSim Plus is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     CloudSim Plus is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with CloudSim Plus. If not, see <http://www.gnu.org/licenses/>.
 */
package org.cloudsimplus.examples.autoscaling;

import ch.qos.logback.classic.Level;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.Simulation;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.ResourceProvisionerSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.resources.Processor;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.scoap.*;

import org.cloudbus.cloudsim.util.MathUtil;
import org.cloudbus.cloudsim.util.ResourceLoader;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelStochastic;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudsimplus.autoscaling.*;
import org.cloudsimplus.autoscaling.resources.ResourceScaling;
import org.cloudsimplus.autoscaling.resources.ResourceScalingGradual;
import org.cloudsimplus.autoscaling.resources.ResourceScalingInstantaneous;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudsimplus.examples.scoap.ScoapVmScalingExample;
import org.cloudsimplus.listeners.CloudletVmEventInfo;
import org.cloudsimplus.listeners.EventInfo;
import org.cloudsimplus.listeners.EventListener;
import org.cloudsimplus.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Comparator.comparingDouble;


public class HybridVmCpuScalingWithScoap {
    /**
     * The interval in which the Datacenter will schedule events.
     * As lower is this interval, sooner the processing of VMs and Cloudlets
     * is updated and you will get more notifications about the simulation execution.
     * However, it can affect the simulation performance.
     *
     * <p>For this example, a large schedule interval such as 15 will make that just
     * at every 15 seconds the processing of VMs is updated. If a VM is overloaded, just
     * after this time the creation of a new one will be requested
     * by the VM's {@link HorizontalVmScaling Horizontal Scaling} mechanism.</p>
     *
     * <p>If this interval is defined using a small value, you may get
     * more dynamically created VMs than expected. Accordingly, this value
     * has to be trade-off.
     * For more details, see {@link Datacenter#getSchedulingInterval()}.</p>
    */
    private static final int SCHEDULING_INTERVAL = 1;
    private static final int HOSTS = 1;

    private static final int HOST_PES = 32;
    private static final int VMS = 1;
    private static final int SIMULATION_TIME = 200;
    private final CloudSim simulation;
    private DatacenterBroker broker0;
    private List<Host> hostList;
    private List<Vm> vmList;
    private List<Vm> hiddleVmsList;
    private List<Cloudlet> cloudletList;
    private Datacenter datacenter;
    private RequestsArrivalGenerator arrivalGenerator;

    private static final int CLOUDLETS = 10;


    private int createdCloudlets;
    //private int[] threshold = {10, 15, 20, 15, 10};
    private Threshold currentThreshold;
    private ArrayList<Threshold> thresholds;
    private VmAllocationStrategy allocationStrategy;

    public static void main(String[] args) {
        new HybridVmCpuScalingWithScoap();
    }

    /**
     * Default constructor that builds the simulation scenario and starts the simulation.
     */
    private HybridVmCpuScalingWithScoap() {
        /*You can remove the seed to get a dynamic one, based on current computer time.
        * With a dynamic seed you will get different results at each simulation run.*/
        final long seed = 1;
        hostList = new ArrayList<>(HOSTS);
        vmList = new ArrayList<>(VMS);
        hiddleVmsList = new ArrayList<>();
        cloudletList = new ArrayList<>(CLOUDLETS);
        arrivalGenerator = new ExponentialRequestsArrivalGenerator();
        simulation = new CloudSim();
        // dinamic allocation of resources (Vms and Cloudlets)
        simulation.addOnClockTickListener(this::onClockTickListener);

        createDatacenter();
        broker0 = new DatacenterBrokerSimple(simulation);
        //broker0.setVmDestructionDelayFunction(vm -> 1.0);
        loadThresholds();

        loadWorkloadTraceIntoQueue();

        //initial allocation of cloulets and VMs
        createCloudletListsAndVmsFromTrace();
        simulation.terminateAt(SIMULATION_TIME);
        simulation.start();



        printSimulationResults();
    }

    private void loadThresholds() {

        ArrayList<VMReservedType> typeOfReservedVMs = new ArrayList<VMReservedType>();

        //Different kind of VMs that have to be reserved(OnDemand).
        ArrayList<VMOnDemand> ondemandVMs = new ArrayList<VMOnDemand>();

        /* Theoretic values of thresholds fetched from XML file */
        thresholds= new ArrayList<>();

        //Ask the XMLparser to populate reservedVMs and Payments for the reservedVms
        ScoapXMLparser.XMLReserved(typeOfReservedVMs, thresholds,"workload/scoap/");
        //Ask the XMLparser to populate singleVMs and thresholds, parsing the XML OnDemand file.
        ScoapXMLparser.XMLOnDemand(ondemandVMs, thresholds,"workload/scoap/");
        int isteresiLength = 3;

        //assign le deactivation thresholds
        for(int i=0;i<(thresholds.size()-isteresiLength);i++)
        {
            if(i<isteresiLength){
                thresholds.get(i).setDeactivationWorkload(0);
            }

            thresholds.get(i+isteresiLength).setDeactivationWorkload(thresholds.get(i).getWorkLoad());
        }
        for( Threshold ts: thresholds){
            System.out.println("Soglia con id:"+ts.getId()+ "e workload teorico:"+ts.getWorkLoad()+" e deactivation :"+ts.getDeactivationWorkload());
        }

        currentThreshold=thresholds.get(2);
    }

    /**
     * Shows updates every time the simulation clock advances.
     * @param evt information about the event happened (that for this Listener is just the simulation time)
     */
    private void onClockTickListener(EventInfo evt) {


        createCloudletListsAndVmsFromTrace();

        System.out.println("VM EXECUTION SIZE --> "+broker0.getVmExecList().size()+" !!!!!");
        System.out.println("VM WAITING SIZE --> "+broker0.getVmWaitingList().size()+" !!!!!");


        arrivalGenerator.getInstantWorkload();

        System.out.println("REQUEST QUEUE SIZE --> "+arrivalGenerator.getRequests(SIMULATION_TIME).size()+" !!!!!");


        //destrotyHiddleVms();

        Log.setLevel(Level.INFO);
        broker0.getVmExecList().forEach(vm -> {
            System.out.printf(
                "\t\tTime %6.1f: Vm %d CPU Usage: %6.2f%% (%2d vCPUs. Running Cloudlets: #%02d) Cloudlet waiting: %s History Entries: %d\n",
                evt.getTime(), vm.getId(), vm.getCpuPercentUsage()*100.0,
                vm.getNumberOfPes(),
                vm.getCloudletScheduler().getCloudletExecList().size(),
                vm.getBroker().getCloudletWaitingList(),
                vm.getUtilizationHistory().getHistory().size());
        });

        long timeInterval = 100;
        try {
            Thread.sleep(timeInterval);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private void printSimulationResults() {
        final List<Cloudlet> finishedCloudlets = broker0.getCloudletFinishedList();
        final Comparator<Cloudlet> sortByVmId = comparingDouble(c -> c.getVm().getId());
        final Comparator<Cloudlet> sortByStartTime = comparingDouble(Cloudlet::getExecStartTime);
        finishedCloudlets.sort(sortByVmId.thenComparing(sortByStartTime));

        new CloudletsTableBuilder(finishedCloudlets).build();
    }

    /**
     * Creates a Datacenter and its Hosts.
     */
    private void createDatacenter() {
        for (int i = 0; i < HOSTS; i++) {
            hostList.add(createHost());
        }

        datacenter = new DatacenterSimple(simulation, hostList, new VmAllocationPolicySimple());
        datacenter.setSchedulingInterval(SCHEDULING_INTERVAL);

    }

    private Host createHost() {
        List<Pe> peList = new ArrayList<>(HOST_PES);
        for (int i = 0; i < HOST_PES; i++) {
            peList.add(new PeSimple(50000, new PeProvisionerSimple()));
        }

        final long ram = 200000; //in Megabytes
        final long bw = 100000; //in Megabytes
        final long storage = 10000000; //in Megabites/s
        final int id = hostList.size();
        return new HostSimple(ram, bw, storage, peList)
            .setRamProvisioner(new ResourceProvisionerSimple())
            .setBwProvisioner(new ResourceProvisionerSimple())
            .setVmScheduler(new VmSchedulerTimeShared());
    }




    /**
     * Creates a list of initial VMs in which each VM is able to scale vertically
     * when it is over or underloaded.
     *
     * @param numberOfVms number of VMs to create
     * @return the list of scalable VMs
     * @see #createVerticalPeScaling()
     */
    /**
    private List<Vm> createListOfVerticalScalableVms(final int numberOfVms) {
        List<Vm> newList = new ArrayList<>(numberOfVms);
        for (int i = 0; i < numberOfVms; i++) {
            Vm vm = createVm();
            vm.setPeVerticalScaling(createVerticalPeScaling());
            newList.add(vm);
        }

        return newList;
    }
     */


    /**
     * Creates a {@link VerticalVmScaling} for scaling VM's CPU when it's under or overloaded.
     *
     * <p>Realize the lower and upper thresholds are defined inside this method by using
     * references to the methods {@link #lowerCpuUtilizationThreshold(Vm)}
     * and {@link #upperCpuUtilizationThreshold(Vm)}.
     * These methods enable defining thresholds in a dynamic way
     * and even different thresholds for distinct VMs.
     * Therefore, it's a powerful mechanism.
     * </p>
     *
     * <p>
     * However, if you are defining thresholds in a static way,
     * and they are the same for all VMs, you can use a Lambda Expression
     * like below, for instance, instead of creating a new method that just returns a constant value:<br>
     * {@code verticalCpuScaling.setLowerThresholdFunction(vm -> 0.4);}
     * </p>
     *
     */
    private VerticalVmScaling createVerticalPeScaling() {
        //The percentage in which the number of PEs has to be scaled
        final double scalingFactor = 0.1;
        VerticalVmScalingSimple verticalCpuScaling = new VerticalVmScalingSimple(Processor.class, scalingFactor);

        /* By uncommenting the line below, you will see that, instead of gradually
         * increasing or decreasing the number of PEs, when the scaling object detects
         * the CPU usage is above or below the defined thresholds,
         * it will automatically calculate the number of PEs to add/remove to
         * move the VM from the over or underload condition.
        */
        //verticalCpuScaling.setResourceScaling(new ResourceScalingInstantaneous());

        /** Different from the commented line above, the line below implements a ResourceScaling using a Lambda Expression.
         * It is just an example which scales the resource twice the amount defined by the scaling factor
         * defined in the constructor.
         *
         * Realize that if the setResourceScaling method is not called, a ResourceScalingGradual will be used,
         * which scales the resource according to the scaling factor.
         * The lower and upper thresholds after this line can also be defined using a Lambda Expression.
         *
         * So, here we are defining our own {@link ResourceScaling} instead of
         * using the available ones such as the {@link ResourceScalingGradual}
         * or {@link ResourceScalingInstantaneous}.
         */
        verticalCpuScaling.setResourceScaling(vs -> 2*vs.getScalingFactor()*vs.getAllocatedResource());

        verticalCpuScaling.setLowerThresholdFunction(this::lowerCpuUtilizationThreshold);
        verticalCpuScaling.setUpperThresholdFunction(this::upperCpuUtilizationThreshold);

        return verticalCpuScaling;
    }

    /**
     * Defines the minimum CPU utilization percentage that indicates a Vm is underloaded.
     * This function is using a statically defined threshold, but it would be defined
     * a dynamic threshold based on any condition you want.
     * A reference to this method is assigned to each Vertical VM Scaling created.
     *
     * @param vm the VM to check if its CPU is underloaded.
     *        <b>The parameter is not being used internally, which means the same
     *        threshold is used for any Vm.</b>
     * @return the lower CPU utilization threshold
     * @see #createVerticalPeScaling()
     */
    private double lowerCpuUtilizationThreshold(Vm vm) {
        return 0.4;
    }

    /**
     * Defines a dynamic CPU utilization threshold that indicates a Vm is overloaded.
     * Such a threshold is the maximum CPU a VM can use before requesting vertical CPU scaling.
     * A reference to this method is assigned to each Vertical VM Scaling created.
     *
     * <p>The dynamic upper threshold is defined as 20% above the mean (mean * 1.2),
     * if there are at least 10 CPU utilization history entries.
     * That means if the CPU utilization of a VM is 20% above its mean
     * CPU utilization, it indicates the VM is overloaded.
     * If there aren't enough history entries,
     * it defines a static threshold as 70% of CPU utilization.</p>
     *
     * @param vm the VM to check if its CPU is overloaded.
     *        The parameter is not being used internally, that means the same
     *        threshold is used for any Vm.
     * @return the upper dynamic CPU utilization threshold
     * @see #createVerticalPeScaling()
     */
    private double upperCpuUtilizationThreshold(Vm vm) {
        final List<Double> history = vm.getUtilizationHistory().getHistory();
        return history.size() > 10 ? MathUtil.median(history) * 1.2 : 0.7;
    }


    private void loadWorkloadTraceIntoQueue() {

        String WORKLOAD_FILENAME = "trace10seconds.txt";
        final InputStream workloadFile = ResourceLoader.getInputStream(ScoapVmScalingExample.class, "workload/scoap/" + WORKLOAD_FILENAME);
        BufferedReader br= new BufferedReader(new InputStreamReader(workloadFile));

        String sCurrentLine;
        try {
            while ((sCurrentLine = br.readLine()) != null) {
                arrivalGenerator.setWorkload(sCurrentLine);
            }
        }
        catch (IOException e){
            System.out.print("Impossible to load the workload log file. The specified file-name does not exist");
        }
    }


    private void createCloudletListsAndVmsFromTrace() {




        if(arrivalGenerator.getRequests(SIMULATION_TIME).size()>0) {

            if(null == allocationStrategy)
            {
                allocationStrategy = new DefaultVmAllocationStrategy();
            }
            allocationStrategy.createAndSubmitVmsAndCloudlets(broker0, cloudletList, hiddleVmsList, vmList, arrivalGenerator, thresholds, currentThreshold, simulation, SIMULATION_TIME);
        }

    }


    /**
     * Dynamically creates and submits a set of VMs to the broker when
     * the first cloudlet finishes.
     * @param eventInfo information about the fired event
     */
    private void submitNewVmsAndCloudletsToBroker(CloudletVmEventInfo eventInfo) {
        final int numberOfNewVms = 2;
        final int numberOfCloudletsByVm = 4;
        Log.setLevel(ch.qos.logback.classic.Level.WARN);
        System.out.printf("\n\t#Cloudlet %d finished. Submitting %d new VMs to the broker\n",
            eventInfo.getCloudlet().getId(), numberOfNewVms);

        VmAllocationStrategy defaultAllocationStrategy = new DefaultVmAllocationStrategy();
        defaultAllocationStrategy.createAndSubmitVmsAndCloudlets(broker0, cloudletList, hiddleVmsList, vmList, arrivalGenerator, thresholds, currentThreshold, simulation, SIMULATION_TIME);



    }

    private void checkQueueAndAllocateCloudLets(CloudletVmEventInfo eventInfo){

        if(arrivalGenerator.getRequests(SIMULATION_TIME).size() > 0){
            //System.out.println("Cloudlet finish "+eventInfo.getCloudlet()+" on VM "+eventInfo.getVm().getId()+" Request queue size ---> "+requests.size());

            VmAllocationStrategy defaultAllocationStrategy = new DefaultVmAllocationStrategy();

            defaultAllocationStrategy.createAndSubmitVmsAndCloudlets(broker0, cloudletList, hiddleVmsList, vmList, arrivalGenerator, thresholds, currentThreshold, simulation, SIMULATION_TIME);
        }
    }



    /**
     * Creates a single Cloudlet with no delay, which means the Cloudlet arrival time will
     * be zero (exactly when the simulation starts).
     *
     * @param length the Cloudlet length
     * @param numberOfPes the number of PEs the Cloudlets requires
     * @return the created Cloudlet
     */
    private Cloudlet createCloudlet(final long length, final int numberOfPes) {
        return createCloudlet(length, numberOfPes, 0);
    }

    /**
     * Creates a single Cloudlet.
     *
     * @param length the length of the cloudlet to create.
     * @param numberOfPes the number of PEs the Cloudlets requires.
     * @param delay the delay that defines the arrival time of the Cloudlet at the Cloud infrastructure.
     * @return the created Cloudlet
     */
    private Cloudlet createCloudlet(final long length, final int numberOfPes, final double delay) {
        /*
        Since a VM PE isn't used by two Cloudlets at the same time,
        the Cloudlet can used 100% of that CPU capacity at the time
        it is running. Even if a CloudletSchedulerTimeShared is used
        to share the same VM PE among multiple Cloudlets,
        just one Cloudlet uses the PE at a time.
        Then it is preempted to enable other Cloudlets to use such a VM PE.
         */
        final UtilizationModel utilizationCpu = new UtilizationModelFull();

        /**
         * Since BW e RAM are shared resources that don't enable preemption,
         * two Cloudlets can't use the same portion of such resources at the same time
         * (unless virtual memory is enabled, but such a feature is not available in simulation).
         * This way, the total capacity of such resources is being evenly split among created Cloudlets.
         * If there are 10 Cloudlets, each one will use just 10% of such resources.
         * This value can be defined in different ways, as you want. For instance, some Cloudlets
         * can require more resources than other ones.
         * To enable that, you would need to instantiate specific {@link UtilizationModelDynamic} for each Cloudlet,
         * use a {@link UtilizationModelStochastic} to define resource usage randomly,
         * or use any other {@link UtilizationModel} implementation.
        */
        final UtilizationModel utilizationModelDynamic = new UtilizationModelDynamic(1.0/CLOUDLETS);
        Cloudlet cl = new CloudletSimple(length, numberOfPes);
        cl.setFileSize(1024)
            .setOutputSize(1024)
            .setUtilizationModelBw(utilizationModelDynamic)
            .setUtilizationModelRam(utilizationModelDynamic)
            .setUtilizationModelCpu(utilizationCpu)
            .setSubmissionDelay(delay);
        return cl;
    }
}
