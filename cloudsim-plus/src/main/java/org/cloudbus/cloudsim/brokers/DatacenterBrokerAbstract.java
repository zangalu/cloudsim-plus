/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */
package org.cloudbus.cloudsim.brokers;

import java.util.*;

import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.events.SimEvent;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterCharacteristics;
import org.cloudbus.cloudsim.util.Log;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.core.*;
import org.cloudsimplus.listeners.DatacenterToVmEventInfo;
import org.cloudsimplus.listeners.VmToCloudletEventInfo;
import org.cloudbus.cloudsim.lists.VmList;

/**
 * An abstract class to be used as base for implementing a {@link DatacenterBroker}.
 *
 * @author Rodrigo N. Calheiros
 * @author Anton Beloglazov
 * @author Manoel Campos da Silva Filho
 */
public abstract class DatacenterBrokerAbstract extends CloudSimEntity implements DatacenterBroker {
    /**
     * @see #getLastSelectedVm()
     */
    private Vm lastSelectedVm = Vm.NULL;

    /**
     * @see #getVmsWaitingList()
     */
    private List<Vm> vmsWaitingList;
    /**
     * @see #getVmsCreatedList()
     */
    private List<Vm> vmsCreatedList;
    /**
     * @see #getCloudletsWaitingList()
     */
    private List<Cloudlet> cloudletsWaitingList;
    /**
     * @see #getCloudletsFinishedList()
     */
    private List<Cloudlet> cloudletsFinishedList;

	/**
	 * @see #getCloudletsCreated()
	 */
    private int cloudletsCreated;
    /**
     * @see #getVmCreationRequests()
     */
    private int vmCreationRequests;
    /**
     *@see #getVmCreationAcks()
     */
    private int vmCreationAcks;
    /**
     * @see #getVmsDestroyed()
     */
    private int vmsDestroyed;
    /**
     * @see #getDatacenterIdsList()
     */
    private List<Integer> datacenterIdsList;
    /**
     * @see #getDatacenterRequestedIdsList()
     */
    private List<Integer> datacenterRequestedIdsList;
    /**
     * @see #getVmsToDatacentersMap()
     */
    private Map<Integer, Integer> vmsToDatacentersMap;
    /**
     * @see #getDatacenterCharacteristicsMap()
     */
    private Map<Integer, DatacenterCharacteristics> datacenterCharacteristicsMap;

    /**
     * Creates a new DatacenterBroker object.
     *
     * @param simulation The CloudSim instance that represents the simulation the Entity is related to
     * @post $none
     */
    public DatacenterBrokerAbstract(CloudSim simulation) {
        super(simulation);
        this.getSimulation().incrementNumberOfUsers();

        this.vmsWaitingList = new ArrayList<>();
        this.vmsCreatedList = new ArrayList<>();
        this.cloudletsWaitingList = new ArrayList<>();
        this.cloudletsFinishedList = new ArrayList<>();

        cloudletsCreated = 0;
        vmCreationRequests = 0;
        vmCreationAcks = 0;
        vmsDestroyed = 0;

        setDatacenterIdsList(new LinkedList<>());
        datacenterRequestedIdsList = new ArrayList<>();
        this.vmsToDatacentersMap = new HashMap<>();
        this.datacenterCharacteristicsMap = new HashMap<>();
    }

    @Override
    public void submitVmList(List<? extends Vm> list) {
        setSimulationInstanceForSubmittedVms(list);
        getVmsWaitingList().addAll(list);
    }

    private void setSimulationInstanceForSubmittedVms(List<? extends Vm> list) {
        for(Vm vm: list){
            vm.setSimulation(this.getSimulation());
        }
    }

    @Override
    public void submitCloudletList(List<? extends Cloudlet> list) {
        setSimulationInstanceForSubmittedCloudlets(list);
        getCloudletsWaitingList().addAll(list);
    }

    private void setSimulationInstanceForSubmittedCloudlets(List<? extends Cloudlet> list) {
        for(Cloudlet cloudlet: list){
            cloudlet.setSimulation(this.getSimulation());
        }
    }

    @Override
    public void submitCloudletList(List<? extends Cloudlet> list, double submissionDelay) {
        setDelayForSubmittedCloudlets(list, submissionDelay);
        submitCloudletList(list);
    }

    private void setDelayForSubmittedCloudlets(List<? extends Cloudlet> list, double submissionDelay) {
        for(Cloudlet cloudlet: list) {
            cloudlet.setSubmissionDelay(submissionDelay);
        }
    }

    @Override
    public boolean bindCloudletToVm(Cloudlet cloudlet, Vm vm) {
        if(!getCloudletsWaitingList().contains(cloudlet)){
            return false;
        }

        cloudlet.setVm(vm);
        return true;
    }

    @Override
    public void processEvent(SimEvent ev) {
        switch (ev.getTag()) {
            // Datacenter characteristics request
            case CloudSimTags.DATACENTER_CHARACTERISTICS_REQUEST:
                processDatacenterCharacteristicsRequest(ev);
            break;
            // Datacenter characteristics response
            case CloudSimTags.DATACENTER_CHARACTERISTICS:
                processDatacenterCharacteristicsResponse(ev);
            break;
            // VM Creation response
            case CloudSimTags.VM_CREATE_ACK:
                processVmCreateResponseFromDatacenter(ev);
            break;
            // A finished cloudlet returned
            case CloudSimTags.CLOUDLET_RETURN:
                processCloudletReturn(ev);
            break;
            // if the simulation finishes
            case CloudSimTags.END_OF_SIMULATION:
                shutdownEntity();
            break;
            // other unknown tags are processed by this method
            default:
                processOtherEvent(ev);
            break;
        }
    }

    /**
     * Process the response of a request for the characteristics of a Datacenter
     * and then requests the creation of waiting VMs in the received Datacenter.
     *
     * @param ev a CloudSimEvent object
     * @pre ev != $null
     * @post $none
     */
    protected void processDatacenterCharacteristicsResponse(SimEvent ev) {
        DatacenterCharacteristics characteristics = (DatacenterCharacteristics) ev.getData();
        getDatacenterCharacteristicsMap().put(characteristics.getId(), characteristics);
        if (getDatacenterCharacteristicsMap().size() == getDatacenterIdsList().size()) {
            this.datacenterRequestedIdsList = new ArrayList<>();
            requestDatacenterToCreateWaitingVms(selectDatacenterForWaitingVms());
        }
    }

    /**
     * Process a request for the characteristics of a Datacenter
     * and gets the list of available Datacenters.
     *
     * @param ev a CloudSimEvent object
     * @pre ev != $null
     * @post $none
     */
    protected void processDatacenterCharacteristicsRequest(SimEvent ev) {
        setDatacenterIdsList(getSimulation().getDatacenterIdsList());
        this.datacenterCharacteristicsMap = new HashMap<>();
        Log.printConcatLine(getSimulation().clock(), ": ", getName(), ": Cloud Datacenter List received with ", getDatacenterIdsList().size(), " switches(s)");
        for (Integer datacenterId : getDatacenterIdsList()) {
            sendNow(datacenterId, CloudSimTags.DATACENTER_CHARACTERISTICS, getId());
        }
    }

    /**
     * Process the ack received from a Datacenter to a broker's request for
     * creation of a Vm in that Datacenter.
     *
     * @param ev a CloudSimEvent object
     * @return true if the VM was created successfully, false otherwise
     * @pre ev != null
     * @post $none
     */
    protected boolean processVmCreateResponseFromDatacenter(SimEvent ev) {
        int[] data = (int[]) ev.getData();
        int datacenterId = data[0];
        int vmId = data[1];
        int result = data[2];
        boolean vmCreatedAndLocated = false;
        vmCreationAcks++;

        //if the VM was sucessfully created in the requested Datacenter
        if (result == CloudSimTags.TRUE) {
            vmCreatedAndLocated = processSuccessVmCreationInDatacenter(vmId, datacenterId);
        } else {
            processFailedVmCreationInDatacenter(vmId, datacenterId);
        }

        // all the requested VMs have been created
        if (getVmsCreatedList().size() == getVmsWaitingList().size() - getVmsDestroyed()) {
            requestDatacentersToCreateWaitingCloudlets();
        } else if (getVmCreationRequests() == getVmCreationAcks()) {
            requestCreationOfWaitingVmsToNextDatacenter();
        }

        return vmCreatedAndLocated;
    }

    /**
     * After the response (ack) of all VM creation request were received
     * but not all VMs could be created (what means some
     * acks informed about Vm creation failures), try to find
     * another switches to request the creation of the VMs
     * in the waiting list.
     */
    protected void requestCreationOfWaitingVmsToNextDatacenter() {
        final int nextDatacenterId = selectFallbackDatacenterForWaitingVms();
        if (nextDatacenterId != -1) {
            requestDatacenterToCreateWaitingVms(nextDatacenterId);
            return;
        }
        // all datacenters already queried
        if (getVmsCreatedList().size() > 0) {
            // if some VMs were created
            requestDatacentersToCreateWaitingCloudlets();
        } else {
            // no VMs created. abort
            Log.printFormattedLine("%.2f: %s: %s", getSimulation().clock(), getName(), "none of the required VMs could be created. Aborting");
            finishExecution();
        }
    }

    /**
     * Process a response from a switches informing that it was able to
     * create the VM requested by the broker.
     *
     * @param vmId id of the Vm that succeeded to be created inside the Datacenter
     * @param datacenterId id of the Datacenter where the request to create
     * the Vm succeeded
     * @return true if the created VM was found in broker VM list, false otherwise
     */
    protected boolean processSuccessVmCreationInDatacenter(int vmId, int datacenterId) {
        getVmsToDatacentersMap().put(vmId, datacenterId);
        Vm vm = VmList.getById(getVmsWaitingList(), vmId);
        boolean vmLocated = vm != Vm.NULL;
        if (vmLocated) {
            /**
             * @todo @author manoelcampos It should remove the created VM from the waiting list.
             */
            getVmsCreatedList().add(vm);
            Log.printConcatLine(
                getSimulation().clock(), ": ", getName(),
                ": VM #", vmId, " has been created in Datacenter #", datacenterId, ", Host #",
                VmList.getById(getVmsCreatedList(), vmId).getHost().getId());
        } else {
            Log.printFormattedLine("The request to create Vm %d was not processed because the Vm was not found in the waiting list.", vmId);
        }
        return vmLocated;
    }

    /**
     * Process a response from a switches informing that it was NOT able to
     * create the VM requested by the broker.
     *
     * @param vmId id of the Vm that failed to be created inside the Datacenter
     * @param datacenterId id of the Datacenter where the request to create
     * the Vm failed
     */
    protected void processFailedVmCreationInDatacenter(int vmId, int datacenterId) {
        Vm vm = VmList.getById(getVmsWaitingList(), vmId);
        if (vm != Vm.NULL) {
            Datacenter datacenter = datacenterCharacteristicsMap.get(datacenterId).getDatacenter();
            DatacenterToVmEventInfo info =
                    new DatacenterToVmEventInfo(getSimulation().clock(), datacenter, vm);
            vm.getOnVmCreationFailureListener().update(info);
        }
        Log.printConcatLine(getSimulation().clock(), ": ", getName(), ": Creation of VM #", vmId, " failed in Datacenter #", datacenterId);
    }

    /**
     * Processes the end of execution of a given cloudlet inside a Vm.
     *
     * @param ev The cloudlet that has just finished to execute
     * @pre ev != $null
     * @post $none
     */
    protected void processCloudletReturn(SimEvent ev) {
        Cloudlet cloudlet = (Cloudlet) ev.getData();
        getCloudletsFinishedList().add(cloudlet);
        notifyCloudletFinishListener(cloudlet);
        Log.printFormattedLine("%.1f: %s: %s %d received", getSimulation().clock(), getName(), cloudlet.getClass().getSimpleName(), cloudlet.getId());
        cloudletsCreated--;
        if (getCloudletsWaitingList().isEmpty() && cloudletsCreated == 0) {
            // all cloudlets executed
            Log.printConcatLine(getSimulation().clock(), ": ", getName(), ": All Cloudlets executed. Finishing...");
            destroyVms();
            finishExecution();
        } else if (hasMoreCloudletsToBeExecuted()) {
            /* All the cloudlets sent have finished. It means that some bounded
            cloudlet are waiting their VMs to be created*/
            destroyVms();
            requestDatacenterToCreateWaitingVms(selectDatacenterForWaitingVms());
        }
    }

    @Override
    public boolean hasMoreCloudletsToBeExecuted() {
        return getCloudletsWaitingList().size() > 0 && cloudletsCreated == 0;
    }

    protected void notifyCloudletFinishListener(Cloudlet cloudlet) {
        VmToCloudletEventInfo info =
                new VmToCloudletEventInfo(getSimulation().clock(), cloudlet.getVm(), cloudlet);
        cloudlet.getOnCloudletFinishEventListener().update(info);
    }

    /**
     * Process non-default received events that aren't processed by the {@link #processEvent(SimEvent)} method.
     * This method should be overridden by subclasses if they really want to process new defined
     * events.
     *
     * @param ev a CloudSimEvent object
     * @pre ev != null
     * @post $none
     */
    protected void processOtherEvent(SimEvent ev) {
        if (Objects.isNull(ev)) {
            Log.printConcatLine(getName(), ".processOtherEvent(): ", "Error - an event is null.");
            return;
        }
        Log.printConcatLine(getName(), ".processOtherEvent(): Error - event unknown by this DatacenterBroker.");
    }

    /**
     * <p>Request a switches to create the VM in the
     * {@link #getVmsWaitingList() VM waiting list}.</p>
     *
     * <p>This method is called after the list of available Datacenters
     * is received.</p>
     *
     * @param datacenterId id of the switches to request the VMs creation
     * @pre $none
     * @post $none
     * @see #submitVmList(java.util.List)
     */
    protected void requestDatacenterToCreateWaitingVms(int datacenterId) {
        int requestedVms = 0;
        String datacenterName = getSimulation().getEntityName(datacenterId);
        for (Vm vm : getVmsWaitingList()) {
            if (!getVmsToDatacentersMap().containsKey(vm.getId())) {
                Log.printLine(getSimulation().clock() + ": " + getName() + ": Trying to Create VM #" + vm.getId() + " in " + datacenterName);
                sendNow(datacenterId, CloudSimTags.VM_CREATE_ACK, vm);
                requestedVms++;
            }
        }
        getDatacenterRequestedIdsList().add(datacenterId);
        this.vmCreationRequests += requestedVms;
        vmCreationAcks = 0;
    }

    /**
     * <p>Request Datacenters to create the Cloudlets in the
     * {@link #getCloudletsWaitingList() Cloudlets waiting list}.
     * If there aren't available VMs to host all cloudlets,
     * the creation of some ones will be postponed.</p>
     *
     * <p>This method is called after all submitted VMs are created
     * in some Datacenter.</p>
     *
     * @pre $none
     * @post $none
     * @see #submitCloudletList(java.util.List)
     * @todo @author manoelcampos Where is checked if the Vm to where
     * a cloudlet was submitted has the required resources?
     */
    protected void requestDatacentersToCreateWaitingCloudlets() {
        List<Cloudlet> successfullySubmitted = new ArrayList<>();
        for (Cloudlet cloudlet : getCloudletsWaitingList()) {
            lastSelectedVm = selectVmForWaitingCloudlet(cloudlet);
            if (getLastSelectedVm() == Vm.NULL) {
                // vm was not created
                Log.printConcatLine(getSimulation().clock(), ": ", getName(), ": Postponing execution of cloudlet ", cloudlet.getId(), ": bounded VM not available");
                continue;
            }
            Log.printFormattedLine("%.2f: %s: Sending %s %d to VM #%d", getSimulation().clock(), getName(), cloudlet.getClass().getSimpleName(), cloudlet.getId(), getLastSelectedVm().getId());
            cloudlet.setVm(lastSelectedVm);
            send(getVmsToDatacentersMap().get(getLastSelectedVm().getId()),
                    cloudlet.getSubmissionDelay(), CloudSimTags.CLOUDLET_SUBMIT, cloudlet);
            cloudletsCreated++;
            successfullySubmitted.add(cloudlet);
        }
        // remove created cloudlets from waiting list
        getCloudletsWaitingList().removeAll(successfullySubmitted);
        /*sets the last selected VM to null so that the next
        time cloudlets are requested to be created, the VM selection will
        restarting from the first VM.*/
        lastSelectedVm = Vm.NULL;
    }

    /**
     * Destroy all created broker's VMs.
     *
     * @pre $none
     * @post $none
     */
    protected void destroyVms() {
        for (Vm vm : getVmsCreatedList()) {
            Log.printConcatLine(getSimulation().clock(), ": " + getName(), ": Destroying VM #", vm.getId());
            sendNow(getVmsToDatacentersMap().get(vm.getId()), CloudSimTags.VM_DESTROY, vm);
        }
        getVmsCreatedList().clear();
    }

    /**
     * Send an internal event communicating the end of the simulation.
     *
     * @pre $none
     * @post $none
     */
    protected void finishExecution() {
        sendNow(getId(), CloudSimTags.END_OF_SIMULATION);
    }

    @Override
    public void shutdownEntity() {
        Log.printConcatLine(getName(), " is shutting down...");
    }

    @Override
    public void startEntity() {
        Log.printConcatLine(getName(), " is starting...");
        schedule(getId(), 0, CloudSimTags.DATACENTER_CHARACTERISTICS_REQUEST);
    }

    @Override
    public <T extends Vm> List<T> getVmsWaitingList() {
        return (List<T>) vmsWaitingList;
    }

    @Override
    public Vm getWaitingVm(final int index) {
        if (index >= 0 && index < vmsWaitingList.size()) {
            return vmsWaitingList.get(index);
        }
        return Vm.NULL;
    }

    @Override
    public <T extends Cloudlet> List<T> getCloudletsWaitingList() {
        return (List<T>) cloudletsWaitingList;
    }

    @Override
    public <T extends Cloudlet> List<T> getCloudletsFinishedList() {
        return (List<T>) cloudletsFinishedList;
    }

    @Override
    public <T extends Vm> List<T> getVmsCreatedList() {
        return (List<T>) vmsCreatedList;
    }

    /**
     * Gets a Vm at a given index from the {@link #getVmsCreatedList() list of created VMs}.
     *
     * @param vmIndex the index where a VM has to be got from the created VM list
     * @return the VM at the given index or {@link Vm#NULL} if the index is invalid
     */
    protected Vm getVmFromCreatedList(int vmIndex) {
        return vmIndex >= 0 && vmIndex < vmsCreatedList.size() ? vmsCreatedList.get(vmIndex) : Vm.NULL;
    }

    /**
     * Gets the number of VM creation requests.
     *
     * @return the number of VM creation requests
     */
    protected int getVmCreationRequests() {
        return vmCreationRequests;
    }

    /**
     * Gets the number of acknowledges (ACKs) received from Datacenters
     * in response to requests to create VMs.
     * The number of acks doesn't mean the number of created VMs,
     * once Datacenters can respond informing that a Vm could not be created.
     *
     * @return the number vm creation acks
     */
    protected int getVmCreationAcks() {
        return vmCreationAcks;
    }

    /**
     * Gets the number of destroyed VMs.
     *
     * @return the number of vms destroyed
     */
    protected int getVmsDestroyed() {
        return vmsDestroyed;
    }

    /**
     * Gets the id's list of available datacenters.
     *
     * @return the switches id's list
     */
    protected List<Integer> getDatacenterIdsList() {
        return datacenterIdsList;
    }

    /**
     * Sets the id's list of available datacenters.
     *
     * @param datacenterIdsList the new switches id's list
     */
    protected final void setDatacenterIdsList(List<Integer> datacenterIdsList) {
        this.datacenterIdsList = datacenterIdsList;
    }

    /**
     * Gets the VM to Datacenter map, where each key is a VM id and each value is
     * the id of the switches where the VM is placed.
     *
     * @return the VM to Datacenter map
     */
    protected Map<Integer, Integer> getVmsToDatacentersMap() {
        return vmsToDatacentersMap;
    }

    /**
     * Gets the switches characteristics map where each key is a switches id and
     * each value is its characteristics.
     *
     * @return the switches characteristics map
     */
    protected Map<Integer, DatacenterCharacteristics> getDatacenterCharacteristicsMap() {
        return datacenterCharacteristicsMap;
    }

    /**
     * Gets the list of datacenters where was requested to place VMs.
     *
     * @return the switches requested id's list
     */
    protected List<Integer> getDatacenterRequestedIdsList() {
        return datacenterRequestedIdsList;
    }

    /**
     *
     * @return latest VM selected to run a cloudlet.
     */
    protected Vm getLastSelectedVm() {
        return lastSelectedVm;
    }

	/**
	 * Gets the total number of cloudlets created inside some Vm.
	 */
	protected int getCloudletsCreated() {
		return cloudletsCreated;
	}
}
