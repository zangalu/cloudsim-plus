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
package org.cloudsimplus.examples.scoap;

import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.Simulation;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.distributions.ContinuousDistribution;
import org.cloudbus.cloudsim.distributions.ExponentialDistr;
import org.cloudbus.cloudsim.distributions.UniformDistr;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.ResourceProvisionerSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.scoap.*;
import org.cloudbus.cloudsim.util.ResourceLoader;
import org.cloudbus.cloudsim.util.SwfWorkloadFileReader;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelPlanetLab;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudsimplus.autoscaling.HorizontalVmScaling;
import org.cloudsimplus.autoscaling.HorizontalVmScalingSimple;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudsimplus.listeners.EventInfo;
import org.cloudsimplus.listeners.EventListener;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Comparator.comparingDouble;

/**
 * An example that balances load by dynamically creating VMs,
 * according to the arrival of request from workload file.
 * Cloudlets are {@link #createNewCloudlets(EventInfo) dynamically created and submitted to the broker
 * at specific time intervals}.
 *
 * <p>A {@link HorizontalVmScalingSimple}
 * is set to each {@link #createListOfScalableVms(int) initially created VM},
 * that will check at {@link #SCHEDULING_INTERVAL specific time intervals}
 * if the VM {@link #isVmOverloaded(Vm) is overloaded or not} to then
 * request the creation of a new VM to attend arriving Cloudlets.</p>
 *
 * <p>The example uses CloudSim Plus {@link EventListener} feature
 * to enable monitoring the simulation and dynamically creating objects such as Cloudlets and VMs.
 * It relies on
 * <a href="http://www.oracle.com/webfolder/technetwork/tutorials/obe/java/Lambda-QuickStart/index.html">Java 8 Lambda Expressions</a>
 * to set a Listener for the {@link Simulation#addOnClockTickListener(EventListener) onClockTick event}.
 * That Listener gets notified every time the simulation clock advances and then creates and submits new Cloudlets.
 * </p>
 *
 * <p>The {@link DatacenterBroker} is accountable to perform horizontal down scaling.
 * The down scaling is enabled by setting a {@link Function} using the {@link DatacenterBroker#setVmDestructionDelayFunction(Function)}.
 * This Function defines the time the broker has to wait to destroy a VM after it becomes idle.
 * If no Function is set, the broker just destroys VMs after all running Cloudlets are finished
 * and there is no Cloudlet waiting to be created.
 * </p>
 *
 * @author Manoel Campos da Silva Filho
 * @since CloudSim Plus 1.0
 */
public class ScoapVmScalingExample {
    /**
     * The interval in which the Datacenter will schedule events.
     * As lower is this interval, sooner the processing of VMs and Cloudlets
     * is updated and you will get more notifications about the simulation execution.
     * However, that also affect the simulation performance.
     *
     * <p>A large schedule interval, such as 15, will make that just
     * at every 15 seconds the processing of VMs is updated. If a VM is overloaded, just
     * after this time the creation of a new one will be requested
     * by the VM's {@link HorizontalVmScaling Horizontal Scaling} mechanism.</p>
     *
     * <p>If this interval is defined using a small value, you may get
     * more dynamically created VMs than expected.
     * Accordingly, this value has to be trade-off.
     * For more details, see {@link Datacenter#getSchedulingInterval()}.</p>
    */
    private static final int SCHEDULING_INTERVAL = 5;

    /**
     * The interval to request the creation of new Cloudlets.
     */
    private static final int CLOUDLETS_CREATION_INTERVAL = SCHEDULING_INTERVAL * 2;
    private static final String WORKLOAD_FILENAME = "trace10seconds.txt";
    private static final int hysteresisLeght = 3;
    /**
     * Defines the maximum number of cloudlets to be created
     * from the given workload file.
     * The value -1 indicates that every job inside the workload file
     * will be created as one cloudlet.
     */
    private int maximumNumberOfCloudletsToCreateFromTheWorkloadFile = -1;
    private static final int CLOUDLETS_MIPS = 10000;

    private static final int HOSTS = 50;
    private static final int HOST_PES = 32;
    private static final int VMS = 4;
    private static final int CLOUDLETS = 6;
    private final CloudSim simulation;
    private DatacenterBroker broker0;
    private List<Host> hostList;
    private List<Vm> vmList;
    private List<Cloudlet> cloudletList;
    private double arrivalRate=0.0;

    /**
     * Different lengths that will be randomly assigned to created Cloudlets.
     */
    private static final long[] CLOUDLET_LENGTHS = {2000, 4000, 10000, 16000, 2000, 30000, 20000};
    private ContinuousDistribution rand;

    private int createdCloudlets;
    private int createsVms;

    public static void main(String[] args) {
        new ScoapVmScalingExample();
    }

    /**
     * Default constructor that builds the simulation scenario and starts the simulation.
     */
    public ScoapVmScalingExample() {
        /*You can remove the seed parameter to get a dynamic one, based on current computer time.
        * With a dynamic seed you will get different results at each simulation run.*/
        final long seed = 1;
        rand = new UniformDistr(0, CLOUDLET_LENGTHS.length, seed);
        hostList = new ArrayList<>(HOSTS);
        vmList = new ArrayList<>(VMS);
        cloudletList = new ArrayList<>(CLOUDLETS);

        simulation = new CloudSim();


        ArrayList<VMReservedType> typeOfReservedVMs = new ArrayList<VMReservedType>();

        //Different kind of VMs that have to be reserved(OnDemand).
        ArrayList<VMOnDemand> ondemandVMs = new ArrayList<VMOnDemand>();

        /* Theoretic values of thresholds fetched from XML file */
        ArrayList<Threshold> thresholds= new ArrayList<Threshold>();

        //Ask the XMLparser to populate reservedVMs and Payments for the reservedVms
        ScoapXMLparser.XMLReserved(typeOfReservedVMs, thresholds,"workload/scoap/");
        //Ask the XMLparser to populate singleVMs and thresholds, parsing the XML OnDemand file.
        ScoapXMLparser.XMLOnDemand(ondemandVMs, thresholds,"workload/scoap/");


        //check reading from  xml
        for (Threshold t : thresholds){
            System.out.println("Threshold ID: "+t.getId()+". Active when workload: "+t.getWorkLoad()+". To activate: ");
            for (SingleVM vm : t.getVMsToActivate()){
                System.out.println("N°: "+vm.getNum()+" of type: "+vm.getVm_name());
            }
        }
        //allocate deactivation thresholds
        for(int i=0;i<(thresholds.size()-hysteresisLeght);i++)
        {
            if(i<hysteresisLeght){
                thresholds.get(i).setDeactivationWorkload(0);
            }

            thresholds.get(i+hysteresisLeght).setDeactivationWorkload(thresholds.get(i).getWorkLoad());
        }
        for( Threshold ts: thresholds){
            System.out.println("Threshold with id:"+ts.getId()+ " and theoretic workload :"+ts.getWorkLoad()+" and deactivation :"+ts.getDeactivationWorkload());
        }

        //read WORKLOAD FILE LINE BY LINE AND ASSIGN THE RESOURCES
        /*final InputStream workloadFile = ResourceLoader.getInputStream(ScoapVmScalingExample.class, "workload/scoap/" + WORKLOAD_FILENAME);
        UtilizationModelPlanetLab utilizationFromLog = new UtilizationModelPlanetLab(new InputStreamReader(workloadFile), 10,10000);
        double utilization = utilizationFromLog.getUtilization(10);
        System.out.println("Utilization !!!!! :"+utilization);
*/

        final InputStream workloadFile = ResourceLoader.getInputStream(ScoapVmScalingExample.class, "workload/scoap/" + WORKLOAD_FILENAME);
        BufferedReader br= new BufferedReader(new InputStreamReader(workloadFile));
        double currentTime=0.0;

        double lambda=0.0;
        //Interval in seconds between two values in the log file.
        double interval=10.0;
        String sCurrentLine;
        try {

            //Arrivals arrivals_generator = new Arrivals (this,queue);

            //This cycle is analyzing every row in the log file.
            while ((sCurrentLine = br.readLine()) != null) {

                lambda=Integer.parseInt(sCurrentLine);
                if(lambda==0){
                    lambda=1;
                }
                arrivalRate=lambda/interval;

                // ?? not sure if needed in Cloudsim
                //currentTime=arrivals_generator.newSimulation(lambda,interval, null);



                // if arrival rate > threshold.workload activate new bucket

                // create new cloudlets to handle more requests
                // readjust VMs according to plan

                // else if rate <threshold.workload deactivate bucket
                // readjust Vms according to plan







            }
        }
        catch (IOException e){
            System.out.print("Impossible to load the workload log file. The specified file-name does not exist");
        }
            simulation.addOnClockTickListener(this::createCloudletsFromWorkloadFile);

        createDatacenter();
        broker0 = new DatacenterBrokerSimple(simulation);

        /*
         * Defines the Vm Destruction Delay Function as a lambda expression
         * so that the broker will wait 10 seconds before destroying an idle VM.
         * By commenting this line, no down scaling will be performed
         * and idle VMs will be destroyed just after all running Cloudlets
         * are finished and there is no waiting Cloudlet. */
        broker0.setVmDestructionDelayFunction(vm -> 10.0);

        vmList.addAll(createListOfScalableVms(VMS));

        createCloudletList();
        broker0.submitVmList(vmList);
        broker0.submitCloudletList(cloudletList);

        simulation.start();

        printSimulationResults();
    }


    private void createCloudletsFromWorkloadFile(EventInfo eventInfo) {
        try {
            final String fileName = "workload/scoap/" + WORKLOAD_FILENAME;
            SwfWorkloadFileReader reader = SwfWorkloadFileReader.getInstance(fileName, CLOUDLETS_MIPS);
            reader.setMaxLinesToRead(maximumNumberOfCloudletsToCreateFromTheWorkloadFile);
            this.cloudletList = reader.generateWorkload();
        }catch (Exception e)
        {
            System.out.println("File not present!");
        }
    }


    private void printSimulationResults() {
        List<Cloudlet> finishedCloudlets = broker0.getCloudletFinishedList();
        Comparator<Cloudlet> sortByVmId = comparingDouble(c -> c.getVm().getId());
        Comparator<Cloudlet> sortByStartTime = comparingDouble(c -> c.getExecStartTime());
        finishedCloudlets.sort(sortByVmId.thenComparing(sortByStartTime));

        new CloudletsTableBuilder(finishedCloudlets).build();
    }

    private void createCloudletList() {
        for (int i = 0; i < CLOUDLETS; i++) {
            cloudletList.add(createCloudlet());
        }
    }

    /**
     * Creates new Cloudlets at every {@link #CLOUDLETS_CREATION_INTERVAL} seconds, up to the 50th simulation second.
     * A reference to this method is set as the {@link EventListener}
     * to the {@link Simulation#addOnClockTickListener(EventListener)}.
     * The method is called every time the simulation clock advances.
     *
     * @param eventInfo the information about the OnClockTick event that has happened
     */
    private void createNewCloudlets(EventInfo eventInfo) {
        final long time = (long) eventInfo.getTime();
        if (time % CLOUDLETS_CREATION_INTERVAL == 0 && time <= 50) {
            final int numberOfCloudlets = 4;
            System.out.printf("\t#Creating %d Cloudlets at time %d.", numberOfCloudlets, time);
            List<Cloudlet> newCloudlets = new ArrayList<>(numberOfCloudlets);
            for (int i = 0; i < numberOfCloudlets; i++) {
                Cloudlet cloudlet = createCloudlet();
                cloudletList.add(cloudlet);
                newCloudlets.add(cloudlet);
            }

            broker0.submitCloudletList(newCloudlets);
        }
    }

    /**
     * Creates a Datacenter and its Hosts.
     */
    private void createDatacenter() {
        for (int i = 0; i < HOSTS; i++) {
            hostList.add(createHost());
        }

        Datacenter dc0 = new DatacenterSimple(simulation, hostList, new VmAllocationPolicySimple());
        dc0.setSchedulingInterval(SCHEDULING_INTERVAL);
    }

    private Host createHost() {
        List<Pe> peList = new ArrayList<>(HOST_PES);
        for (int i = 0; i < HOST_PES; i++) {
            peList.add(new PeSimple(1000, new PeProvisionerSimple()));
        }

        final long ram = 2048; // in Megabytes
        final long storage = 1000000; // in Megabytes
        final long bw = 10000; //in Megabits/s
        return new HostSimple(ram, bw, storage, peList)
            .setRamProvisioner(new ResourceProvisionerSimple())
            .setBwProvisioner(new ResourceProvisionerSimple())
            .setVmScheduler(new VmSchedulerTimeShared());
    }

    /**
     * Creates a list of initial VMs in which each VM is able to scale horizontally
     * when it is overloaded.
     *
     * @param numberOfVms number of VMs to create
     * @return the list of scalable VMs
     * @see #createHorizontalVmScaling(Vm)
     */
    private List<Vm> createListOfScalableVms(final int numberOfVms) {
        List<Vm> newList = new ArrayList<>(numberOfVms);
        for (int i = 0; i < numberOfVms; i++) {
            Vm vm = createVm();
            createHorizontalVmScaling(vm);
            newList.add(vm);
        }

        return newList;
    }

    /**
     * Creates a {@link HorizontalVmScaling} object for a given VM.
     *
     * @param vm the VM for which the Horizontal Scaling will be created
     * @see #createListOfScalableVms(int)
     */
    private void createHorizontalVmScaling(Vm vm) {
        HorizontalVmScaling horizontalScaling = new HorizontalVmScalingSimple();
        horizontalScaling
             .setVmSupplier(this::createVm)
             .setOverloadPredicate(this::thresholdsToBeActivated);
        vm.setHorizontalScaling(horizontalScaling);
    }

    /**
     * A {@link Predicate} that checks if a given VM is overloaded or not,
     * based on upper CPU utilization threshold.
     * A reference to this method is assigned to each {@link HorizontalVmScaling} created.
     *
     * @param vm the VM to check if it is overloaded
     * @return true if the VM is overloaded, false otherwise
     * @see #createHorizontalVmScaling(Vm)
     */
    private boolean isVmOverloaded(Vm vm) {
        return vm.getCpuPercentUsage() > 0.7;
    }


    private boolean thresholdsToBeActivated(Vm vm){
        //return vm.getCurrentAllocatedBw() < arrivalRate;
        //TODO
        return true;
    }



    /**
     * Creates a Vm object.
     *
     * @return the created Vm
     */
    private Vm createVm() {
        final int id = createsVms++;
        return new VmSimple(id, 1000, 2)
            .setRam(512).setBw(10).setSize(10000)
            .setCloudletScheduler(new CloudletSchedulerTimeShared());
    }

    private Cloudlet createCloudlet() {
        final int id = createdCloudlets++;
        //randomly selects a length for the cloudlet
        final long length = CLOUDLET_LENGTHS[(int) rand.sample()];
        UtilizationModel utilization = new UtilizationModelFull();
        return new CloudletSimple(id, length, 2)
            .setFileSize(1024)
            .setOutputSize(1024)
            .setUtilizationModel(utilization);
    }
}
