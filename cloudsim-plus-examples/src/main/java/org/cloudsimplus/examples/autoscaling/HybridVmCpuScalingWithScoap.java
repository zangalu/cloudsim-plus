package org.cloudsimplus.examples.autoscaling;

import ch.qos.logback.classic.Level;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.ResourceProvisionerSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.scoap.*;
import org.cloudbus.cloudsim.util.ResourceLoader;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudsimplus.autoscaling.*;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudsimplus.examples.scoap.ScoapVmScalingExample;
import org.cloudsimplus.listeners.EventInfo;
import org.cloudsimplus.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;


import static java.util.Comparator.comparingDouble;


public class HybridVmCpuScalingWithScoap
{
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
    private Threshold currentThreshold;
    private ArrayList<Threshold> thresholds;
    private VmAllocationStrategy allocationStrategy;

    public static void main(String[] args)
    {
        new HybridVmCpuScalingWithScoap();
    }

    /**
     * Default constructor that builds the simulation scenario and starts the simulation.
     */
    private HybridVmCpuScalingWithScoap()
    {
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
        loadThresholds();

        loadWorkloadTraceInQueue();

        //initial allocation of cloulets and VMs
        createCloudletListsAndVmsFromTrace();
        simulation.terminateAt(SIMULATION_TIME);
        simulation.start();


        printSimulationResults();
    }

    private void loadThresholds()
    {

        ArrayList<VMReservedType> typeOfReservedVMs = new ArrayList<VMReservedType>();

        //Different kind of VMs that have to be reserved(OnDemand).
        ArrayList<VMOnDemand> ondemandVMs = new ArrayList<VMOnDemand>();

        /* Theoretic values of thresholds fetched from XML file */
        thresholds = new ArrayList<>();

        //Ask the XMLparser to populate reservedVMs and Payments for the reservedVms
        ScoapXMLparser.XMLReserved(typeOfReservedVMs, thresholds, "workload/scoap/");
        //Ask the XMLparser to populate singleVMs and thresholds, parsing the XML OnDemand file.
        ScoapXMLparser.XMLOnDemand(ondemandVMs, thresholds, "workload/scoap/");
        int isteresiLength = 3;

        //assign le deactivation thresholds
        for (int i = 0; i < (thresholds.size() - isteresiLength); i++)
        {
            if (i < isteresiLength)
            {
                thresholds.get(i).setDeactivationWorkload(0);
            }

            thresholds.get(i + isteresiLength).setDeactivationWorkload(thresholds.get(i).getWorkLoad());
        }
        for (Threshold ts : thresholds)
        {
            System.out.println("Soglia con id:" + ts.getId() + "e workload teorico:" + ts.getWorkLoad() + " e deactivation :" + ts.getDeactivationWorkload());
        }

        currentThreshold = thresholds.get(2);
    }

    /**
     * Shows updates every time the simulation clock advances.
     *
     * @param evt information about the event happened (that for this Listener is just the simulation time)
     */
    private void onClockTickListener(EventInfo evt)
    {


        int currentRequests = createCloudletListsAndVmsFromTrace();

        System.out.println("VM EXECUTION SIZE --> " + broker0.getVmExecList().size());
        System.out.println("VM WAITING SIZE --> " + broker0.getVmWaitingList().size());


        arrivalGenerator.getInstantWorkload();

        System.out.println("REQUEST QUEUE SIZE --> " + currentRequests);


        Log.setLevel(Level.INFO);
        broker0.getVmExecList().forEach(vm -> {
            System.out.printf(
                "\t\tTime %6.1f: Vm %d CPU Usage: %6.2f%% (%2d vCPUs. Running Cloudlets: #%02d) Cloudlet waiting: %s History Entries: %d\n",
                evt.getTime(), vm.getId(), vm.getCpuPercentUsage() * 100.0,
                vm.getNumberOfPes(),
                vm.getCloudletScheduler().getCloudletExecList().size(),
                vm.getBroker().getCloudletWaitingList(),
                vm.getUtilizationHistory().getHistory().size());
        });

        long timeInterval = 100;
        try
        {
            Thread.sleep(timeInterval);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }

    }

    private void printSimulationResults()
    {
        final List<Cloudlet> finishedCloudlets = broker0.getCloudletFinishedList();
        final Comparator<Cloudlet> sortByVmId = comparingDouble(c -> c.getVm().getId());
        final Comparator<Cloudlet> sortByStartTime = comparingDouble(Cloudlet::getExecStartTime);
        finishedCloudlets.sort(sortByVmId.thenComparing(sortByStartTime));

        new CloudletsTableBuilder(finishedCloudlets).build();
    }

    /**
     * Creates a Datacenter and its Hosts.
     */
    private void createDatacenter()
    {
        for (int i = 0; i < HOSTS; i++)
        {
            hostList.add(createHost());
        }

        datacenter = new DatacenterSimple(simulation, hostList, new VmAllocationPolicySimple());
        datacenter.setSchedulingInterval(SCHEDULING_INTERVAL);

    }

    private Host createHost()
    {
        List<Pe> peList = new ArrayList<>(HOST_PES);
        for (int i = 0; i < HOST_PES; i++)
        {
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
     * Load workload Trace in a Queue using Arrival Generator class
     */
    private void loadWorkloadTraceInQueue()
    {

        String WORKLOAD_FILENAME = "trace10seconds.txt";
        final InputStream workloadFile = ResourceLoader.getInputStream(ScoapVmScalingExample.class, "workload/scoap/" + WORKLOAD_FILENAME);
        BufferedReader br = new BufferedReader(new InputStreamReader(workloadFile));

        String sCurrentLine;
        try
        {
            while ((sCurrentLine = br.readLine()) != null)
            {
                arrivalGenerator.setWorkload(sCurrentLine);
            }
        }
        catch (IOException e)
        {
            System.out.print("Impossible to load the workload log file. The specified file-name does not exist");
        }
    }


    private int createCloudletListsAndVmsFromTrace()
    {
        int currentRequests = arrivalGenerator.getRequests(SIMULATION_TIME).size();

        if (currentRequests > 0)
        {

            if (null == allocationStrategy)
            {
                allocationStrategy = new DefaultVmAllocationStrategy();
            }
            currentThreshold = allocationStrategy.createAndSubmitVmsAndCloudlets(broker0, cloudletList, hiddleVmsList, vmList, arrivalGenerator, thresholds, currentThreshold, simulation, SIMULATION_TIME);
        }
        return currentRequests;
    }

}
