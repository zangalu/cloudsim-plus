package org.cloudsimplus.examples.autoscaling;

import ch.qos.logback.classic.Level;
import org.apache.commons.lang3.StringUtils;
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
import org.cloudsimplus.examples.scoap.ScoapVmScalingExample;
import org.cloudsimplus.listeners.EventInfo;
import org.cloudsimplus.util.Log;
import java.io.*;
import java.util.*;

public class HybridVmCpuScalingWithScoap
{

    private static final String ALLOCATION_STRATEGY = "allocationStrategy";
    private static final String CONFIGURATION_NAME ="scoap_example_simulation.properties" ;

    private static final int SCHEDULING_INTERVAL = 1;
    private static final int HOSTS = 1;
    private static final int HOST_PES = 32;
    private static final int VMS = 1;
    public static final String WORKLOAD_SCOAP_PATH = "workload/scoap/";

    public static final String SIMULATION_TIME_PROPERTY = "simulationTime";

    private int SIMULATION_TIME;

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
    private Map<String,String> simulationConfigMap;

    private int inMemoryTraceBatchSize = 10000000;

    private String allocationStrategy_config;

    private ArrayList<VMOnDemand> ondemandVMs;

    private ArrayList<VMReservedType> reservedVMs;
    private ScoapStatistics statistics;

    private int workLoadSize;

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

        final InputStreamReader reader = new InputStreamReader(ResourceLoader.getInputStream(HybridVmCpuScalingWithScoap.class, CONFIGURATION_NAME));
        readConfigFile(reader);
        statistics = new ScoapStatistics();

        SIMULATION_TIME=Integer.valueOf(simulationConfigMap.get(SIMULATION_TIME_PROPERTY));

        hostList = new ArrayList<>(HOSTS);
        vmList = new ArrayList<>(VMS);
        hiddleVmsList = new ArrayList<>();
        cloudletList = new ArrayList<>(CLOUDLETS);
        arrivalGenerator = new ExponentialRequestsArrivalGenerator(simulationConfigMap);
        simulation = new CloudSim();
        // dinamic allocation of resources (Vms and Cloudlets)
        simulation.addOnClockTickListener(this::onClockTickListener);

        createDatacenter();
        broker0 = new DatacenterBrokerSimple(simulation);

        loadThresholds();

        loadMoreWorkloadBatchesOfTraceInMemory();

        //initial allocation of cloulets and VMs
        createCloudletListsAndVmsFromTrace();
        simulation.terminateAt(SIMULATION_TIME);
        simulation.start();

        if(arrivalGenerator.workloadQueue()==0){
            populateStatistics();
            printSimulationResults(broker0);
            System.out.println("------FINE------");
            simulation.terminateAt(1.0);
        }

    }

    private void populateStatistics()
    {
        statistics.setAllocationStrategy(simulationConfigMap.get(ALLOCATION_STRATEGY));
        statistics.setCost_AVG(allocationStrategy.getTotalCost()/(SIMULATION_TIME*Integer.valueOf(simulationConfigMap.get(
            AbstractVmAllocationStrategy.SIMULATION_SCALE_FACTOR_PROPERTY))));
        statistics.setPeak_Max(arrivalGenerator.getRequestPeak());
        statistics.setPeak_AVG(workLoadSize/simulation.clock());
        statistics.setIsteresi(Integer.valueOf(simulationConfigMap.get(AbstractVmAllocationStrategy.ISTERESI_PROPERTY)));
        statistics.setSlowstart(Boolean.valueOf(simulationConfigMap.get(AbstractVmAllocationStrategy.SLOW_START_PROPERTY)));
        statistics.setTotalPackets(arrivalGenerator.getTotalPackets());
    }

    private void loadThresholds()
    {

        reservedVMs = new ArrayList<VMReservedType>();

        //Different kind of VMs that have to be reserved(OnDemand).
        ondemandVMs = new ArrayList<VMOnDemand>();

        /* Theoretic values of thresholds fetched from XML file */
        thresholds = new ArrayList<>();

        //Ask the XMLparser to populate reservedVMs and Payments for the reservedVms
        ScoapXMLparser.XMLReserved(reservedVMs, thresholds, WORKLOAD_SCOAP_PATH);
        //Ask the XMLparser to populate singleVMs and thresholds, parsing the XML OnDemand file.
        ScoapXMLparser.XMLOnDemand(ondemandVMs, thresholds, WORKLOAD_SCOAP_PATH);
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


        createCloudletListsAndVmsFromTrace();

        System.out.println("VM EXECUTION SIZE --> " + broker0.getVmExecList().size());
        System.out.println("VM WAITING SIZE --> " + broker0.getVmWaitingList().size());
        System.out.println("TOTAL COST --> "+allocationStrategy.getTotalCost());
        System.out.println("RECONFIGURATIONS --> "+statistics.getReconfiguration_AVG());

        arrivalGenerator.getInstantWorkload();

        Log.setLevel(Level.INFO);
        broker0.getVmExecList().forEach(vm -> {
            System.out.printf(
                "\t\tTime %6.1f: Vm %d (%2d vCPUs. Running Cloudlets: #%02d) Cloudlet waiting: %s History Entries: %d\n",
                arrivalGenerator.getCurrentTime()/60, vm.getId(),
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

    public void printSimulationResults(DatacenterBroker broker0)
    {
        printResults(broker0);
    }

    private void printResults(DatacenterBroker broker0)
    {
        /*final List<Cloudlet> finishedCloudlets = broker0.getCloudletFinishedList();
        final Comparator<Cloudlet> sortByVmId = comparingDouble(c -> c.getVm().getId());
        final Comparator<Cloudlet> sortByStartTime = comparingDouble(Cloudlet::getExecStartTime);
        finishedCloudlets.sort(sortByVmId.thenComparing(sortByStartTime));

        new CloudletsTableBuilder(finishedCloudlets).build();*/


        try
        {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("simulation_result")));
            out.println("-----End of Simulation with simulation strategy ["+statistics.getAllocationStrategy()+"] booting time:"+statistics.getBooting()+" isteresi: "+statistics.getIsteresi()+" timeout:"+statistics.getTimeout());
            //out.println("awt media: "+" min: "+" Max: ");
            out.println("AWG peak: "+statistics.getPeak_AVG()+" Max: "+statistics.getPeak_Max());
            out.println("AVG cost: "+statistics.getCost_AVG()+" Min: "+statistics.getCost_Min()+" Max : "+statistics.getCost_Max());
            out.println("total cost "+ allocationStrategy.getTotalCost());
            out.println("total packets "+ statistics.getTotalPackets());
            out.println("reconfigurations AVG: "+statistics.getReconfiguration_AVG());
            //out.println("availab media: "+" Min: "+" Max: ");
            out.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }



       /* PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("risultato_simulaz_"+(int)averageBootingTime+"_"+isteresiLength+"_"+(int)firstDeactivationTimeOut+".txt", true)));
        out.println("-----fine simulazione con booting time:"+averageBootingTime+" isteresi: "+isteresiLength+" timeout:"+firstDeactivationTimeOut);
        out.println("awt media: "+String.format("%.4f",totAwt/iterazioni)+" min: "+String.format("%.4f",minAwt)+" Max: "+String.format("%.4f",maxAwt));
        out.println("peak medio: "+String.format("%.2f",totPeak/iterazioni)+" Min : "+String.format("%.2f",minPeak)+ " Max : "+String.format("%.2f",maxPeak));
        out.println("cost medio: "+String.format("%.2f",totCost/iterazioni)+" Min: "+String.format("%.2f",minCost)+" Max : "+String.format("%.2f",maxCost));
        out.println("reconfigurations medio: "+(totReconf/iterazioni)+" Min: "+minReconf+" Max reconfigurations: "+maxReconf);
        out.println("availab media: "+String.format("%.4f",totAvailability/iterazioni)+" Min: "+String.format("%.4f",minAvailability)+" Max: "+String.format("%.4f",maxAvailability));
        out.close();*/
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
        return getHost(HOST_PES,hostList);
    }

    public Host getHost(int hostPes, List<Host> hostList){
        List<Pe> peList = new ArrayList<>(hostPes);
        for (int i = 0; i < hostPes; i++)
        {
            peList.add(new PeSimple(50000, new PeProvisionerSimple()));
        }

        final long ram = 2000000; //in Megabytes
        final long bw = 1000000; //in Megabytes
        final long storage = 100000000; //in Megabites/s
        final int id = hostList.size();
        return new HostSimple(ram, bw, storage, peList)
            .setRamProvisioner(new ResourceProvisionerSimple())
            .setBwProvisioner(new ResourceProvisionerSimple())
            .setVmScheduler(new VmSchedulerTimeShared());
    }

    /**
     * Load workload Trace in a Queue using Arrival Generator class
     */
    private void loadMoreWorkloadBatchesOfTraceInMemory()
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
            workLoadSize=arrivalGenerator.workloadQueue();
        }
        catch (IOException e)
        {
            System.out.print("Impossible to load the workload log file. The specified file-name does not exist");
        }
    }


    private void createCloudletListsAndVmsFromTrace()
    {
        int currentRequests = arrivalGenerator.getRequests().size();

        if (currentRequests > 0)
        {
            if (null == allocationStrategy)
            {
                allocationStrategy_config=simulationConfigMap.get(ALLOCATION_STRATEGY);
                if(StringUtils.isNotBlank(allocationStrategy_config))
                {
                    switch (allocationStrategy_config)
                    {
                        case "default":
                            allocationStrategy = new DefaultVmAllocationStrategy(simulationConfigMap, ondemandVMs, statistics);
                            break;

                        case "deactivationTimeout":
                            allocationStrategy = new DeactivationTimeoutVmAllocationStrategy(simulationConfigMap, ondemandVMs, statistics);
                            break;

                        case "hysteresis":
                            allocationStrategy = new HysteresisVmAllocationStrategy(simulationConfigMap, ondemandVMs, statistics);
                            break;

                        case "hybrid":
                            allocationStrategy = new HybridVmAllocationStrategy(simulationConfigMap, ondemandVMs, statistics);
                            break;

                        default:
                            System.out.println(
                                "You must specify a valid allocationStrategy on scoap_example_simulation.properties");
                    }
                }
            }
            if(allocationStrategy != null)
            {
                currentThreshold = allocationStrategy.createAndSubmitVmsAndCloudlets(broker0, cloudletList, hiddleVmsList, vmList, arrivalGenerator, thresholds, currentThreshold, simulation, SIMULATION_TIME);
            }else{
                System.out.println("error while reading allocationStrategy");
                simulation.terminate();
            }



            }

        System.out.println(" \n");
        System.out.println("CURRENT TIME in Hours --> "+arrivalGenerator.getCurrentTime()/60);
        System.out.println(" \n");

            System.out.println("WORKLOAD QUEUE SIZE --> "+arrivalGenerator.workloadQueue()+" !!!!!");

    }


    public void readConfigFile(final InputStreamReader streamReader) {

        try(BufferedReader reader = new BufferedReader(streamReader)) {
            String nextLine;
            String localConfig;
            simulationConfigMap = new HashMap<>();
            while ((nextLine = reader.readLine()) != null) {
                localConfig = parseLine(nextLine);
                if(localConfig.contains("#"))
                {
                    continue;
                }
                String[] key_value = localConfig.split("=");
                if(key_value.length>1)
                {
                    simulationConfigMap.put(key_value[0], key_value[1]);
                }
            }
        } catch (IOException e) {
            System.out.println("empty Value for configuration");
        }
    }

    /**
     * Gets a line from a file and parses its fields, considering that all fields have the same type.
     *
     * @param nodeLine the line to be parsed
     *
     * @return true if any field was parsed, false otherwise
     */
    private String parseLine(String nodeLine){
        final StringTokenizer tokenizer = new StringTokenizer(nodeLine);

        String token="";
        for (int i = 0; tokenizer.hasMoreElements() ; i++) {
            token = tokenizer.nextToken();
        }

        return token;
    }
}
