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
package org.cloudsimplus.autoscaling;

import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.util.Log;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudsimplus.listeners.VmHostEventInfo;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * <p>
 * A {@link ConfigurableHorizontalVmScalingSimple} implementation that allows defining the condition
 * to identify an overloaded or underloaded VM, based on any desired criteria, such as
 * current RAM, CPU and/or Bandwidth utilization.
 * A {@link DatacenterBroker} monitors the VMs that have
 * an HorizontalVmScaling object in order to create or destroy VMs on demand.
 * </p>
 *
 * <br>
 * <p>The overload condition has to be defined
 * by providing a {@link Predicate} using the {@link #setOverloadPredicate(Predicate)} method.
 * The underload condition has to be defined
 * by providing a {@link Predicate} using the {@link #setUnderloadPredicate(Predicate)} method.
 * Check the {@link ConfigurableHorizontalVmScaling} documentation for details on how to enable horizontal down scaling
 * using the {@link DatacenterBroker}.
 * </p>
 *
 * @author Luca Zangari
 * @since CloudSim Plus 2.3
 * @see ConfigurableHorizontalVmScalingSimple
 */
public class ConfigurableHorizontalVmScalingSimple extends VmScalingAbstract implements ConfigurableHorizontalVmScaling {
    private Supplier<Vm> vmSupplier;

    /**
     * The last number of cloudlet creation requests
     * received by the broker. This is not related to the VM,
     * but the overall cloudlets creation requests.
     */
    private long cloudletCreationRequests;

    private Predicate<Vm> overloadPredicate;
    private Predicate<Vm> underloadPredicate;

    public ConfigurableHorizontalVmScalingSimple(){
        super();
        this.overloadPredicate = FALSE_PREDICATE;
        this.underloadPredicate = FALSE_PREDICATE;
        this.vmSupplier = () -> Vm.NULL;
    }

    @Override
    public Supplier<Vm> getVmSupplier() {
        return vmSupplier;
    }

    @Override
    public final HorizontalVmScaling setVmSupplier(final Supplier<Vm> supplier) {
        Objects.requireNonNull(supplier);
        this.vmSupplier = supplier;
        return this;
    }

    @Override
    public Predicate<Vm> getOverloadPredicate() {
        return overloadPredicate;
    }

    @Override
    public VmScaling setOverloadPredicate(final Predicate<Vm> predicate) {
        Objects.requireNonNull(predicate);
        this.overloadPredicate = predicate;
        return this;
    }


    @Override
    protected boolean requestUpScaling(final double time) {
        if(!haveNewCloudletsArrived()){
            return false;
        }

        final double vmCpuUsagePercent = getVm().getCpuPercentUsage() * 100;
        //final Vm newVm = getVmSupplier().get();
       // Log.printFormattedLine(
       //     "\t%.2f: %s%d: Requesting creation of Vm %d to receive new Cloudlets in order to balance load of Vm %d. Vm %d CPU usage is %.2f%%",
       //     time, getClass().getSimpleName(), getVm().getId(), newVm.getId(), getVm().getId(), getVm().getId(), vmCpuUsagePercent);
        //getVm().getBroker().submitVm(newVm);

        cloudletCreationRequests = getVm().getBroker().getCloudletCreatedList().size();
        return true;
    }

    /**
     * Checks if new Cloudlets were submitted to the broker since the last
     * time this method was called.
     * @return
     */
    private boolean haveNewCloudletsArrived(){
        return getVm().getBroker().getCloudletCreatedList().size() > cloudletCreationRequests;
    }

    @Override
    public final boolean requestUpScalingIfPredicateMatches(final VmHostEventInfo evt) {
        if(!isTimeToCheckPredicate(evt.getTime())) {
            return false;
        }

        setLastProcessingTime(evt.getTime());
        return overloadPredicate.test(getVm()) && requestUpScaling(evt.getTime());
    }

    @Override
    public boolean requestDownScalingIfPredicateMatches(VmHostEventInfo evt) {
        if(!isTimeToCheckPredicate(evt.getTime())) {
            return false;
        }

        setLastProcessingTime(evt.getTime());
        return underloadPredicate.test(getVm()) && requestDownScaling(evt.getTime());
    }

    @Override
    public Predicate<Vm> getUnderloadPredicate() {
        return underloadPredicate;
    }

    @Override
    public VmScaling setUnderloadPredicate(Predicate<Vm> predicate) {
        Objects.requireNonNull(predicate);
        this.underloadPredicate = predicate;
        return this;
    }

    @Override
    public boolean requestDownScaling(final double time) {
        if(haveNewCloudletsArrived()){
            return false;
        }

        Log.printFormattedLine(
            "\t%.2f: %s%d: Requesting destruction of Vm %d in order to balance load.",
            time, getClass().getSimpleName(), getVm().getId());

        getVm().getBroker().getSimulation().sendNow(getVm().getBroker(), getVm().getBroker(),CloudSimTags.VM_DESTROY, getVm());

        return true;
    }
}
