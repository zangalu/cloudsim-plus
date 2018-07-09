package org.cloudbus.cloudsim.brokers;

import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.Simulation;
import org.cloudbus.cloudsim.core.events.SimEvent;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudsimplus.listeners.DatacenterBrokerEventInfo;
import org.cloudsimplus.listeners.EventListener;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A class that implements the Null Object Design Pattern for {@link DatacenterBroker}
 * class.
 *
 * @author Manoel Campos da Silva Filho
 * @see DatacenterBroker#NULL
 */
final class DatacenterBrokerNull implements DatacenterBroker {
    @Override public int compareTo(SimEntity o) {
        return 0;
    }
    @Override public SimEntity setState(State state) { return this; }
    @Override public boolean isStarted() {
        return false;
    }
    @Override public boolean isAlive() { return false; }
    @Override public boolean isFinished() { return false; }
    @Override public Simulation getSimulation() {
        return Simulation.NULL;
    }
    @Override public SimEntity setSimulation(Simulation simulation) {
        return this;
    }
    @Override public void processEvent(SimEvent ev) {/**/}
    @Override public void schedule(SimEntity dest, double delay, int tag) {/**/}
    @Override public void run() {/**/}
    @Override public void start() {/**/}
    @Override public int getId() {
        return -1;
    }
    @Override public String getName() {
        return "";
    }
    @Override public boolean bindCloudletToVm(Cloudlet cloudlet, Vm vm) {
        return false;
    }
    @Override public <T extends Cloudlet> List<T> getCloudletWaitingList() {
        return Collections.emptyList();
    }
    @Override public <T extends Cloudlet> List<T> getCloudletFinishedList() {
        return Collections.emptyList();
    }
    @Override public Vm getWaitingVm(int index) {
        return Vm.NULL;
    }
    @Override public <T extends Vm> List<T> getVmWaitingList() {
        return Collections.emptyList();
    }
    @Override public <T extends Vm> List<T> getVmExecList() {
        return Collections.emptyList();
    }
    @Override public <T extends Vm> List<T> getVmCreatedList() { return Collections.EMPTY_LIST; }
    @Override public boolean isThereWaitingCloudlets() { return false; }
    @Override public void shutdownEntity() {/**/}
    @Override public SimEntity setName(String newName) throws IllegalArgumentException { return this; }
    @Override public void setDatacenterSupplier(Supplier<Datacenter> datacenterSupplier) {/**/}
    @Override public void setFallbackDatacenterSupplier(Supplier<Datacenter> fallbackDatacenterSupplier) {/**/}
    @Override public void setVmMapper(Function<Cloudlet, Vm> vmMapper) {/**/}
    @Override public Set<Cloudlet> getCloudletCreatedList() { return Collections.EMPTY_SET; }
    @Override public DatacenterBroker addOnVmsCreatedListener(EventListener<DatacenterBrokerEventInfo> listener) { return this; }
    @Override public DatacenterBroker addOneTimeOnVmsCreatedListener(EventListener<DatacenterBrokerEventInfo> listener) { return this; }
    @Override public Function<Vm, Double> getVmDestructionDelayFunction() { return vm -> 0.0; }
    @Override public DatacenterBroker setVmDestructionDelayFunction(Function<Vm, Double> function) { return this; }
    @Override public Vm defaultVmMapper(Cloudlet cloudlet) { return Vm.NULL; }
    @Override public void setVmComparator(Comparator<Vm> comparator) {/**/}
    @Override public void setCloudletComparator(Comparator<Cloudlet> comparator) {/**/}
    @Override public void setLog(boolean log) {/**/}
    @Override public void submitCloudlet(Cloudlet cloudlet) {/**/}
    @Override public void submitCloudletList(List<? extends Cloudlet> list) {/**/}
    @Override public void submitCloudletList(List<? extends Cloudlet> list, double submissionDelay) {/**/}
    @Override public void submitCloudletList(List<? extends Cloudlet> list, Vm vm) {/**/}
    @Override public void submitCloudletList(List<? extends Cloudlet> list, Vm vm, double submissionDelay) {/**/}
    @Override public void submitVm(Vm vm) {/**/}
    @Override public void submitVmList(List<? extends Vm> list) {/**/}
    @Override public void submitVmList(List<? extends Vm> list, double submissionDelay) {/**/}
}
