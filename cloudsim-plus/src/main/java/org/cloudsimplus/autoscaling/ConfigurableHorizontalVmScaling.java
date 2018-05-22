package org.cloudsimplus.autoscaling;

import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudsimplus.listeners.VmHostEventInfo;

import java.util.function.Predicate;

public interface ConfigurableHorizontalVmScaling extends HorizontalVmScaling{

    /**
     * Requests a horizontal scale if the Vm is underloaded, according to the {@link #getUnderloadPredicate()} predicate.
     * The scaling is performed by deallocating VMs and submitting the request to the broker.
     *
     * <p>The time interval in which it will be checked if the Vm is underloaded
     * depends on the {@link Datacenter#getSchedulingInterval()} value.
     * Make sure to set such a value to enable the periodic overload verification.</p>
     *
     * <p><b>The method will check the need to remove a
     * VM at the time interval defined by the {@link Datacenter#getSchedulingInterval()}.
     * A VM deallocation request is only sent when the VM is underloaded and
     * cloudlets in execution have finished.
     * </b></p>
     *
     * @param evt current simulation time
     * @return {@inheritDoc}
     */

    boolean requestDownScalingIfPredicateMatches(VmHostEventInfo evt);

    /**
     * Gets a {@link Predicate} that defines when {@link #getVm() Vm} is overloaded or not,
     * that will make the Vm's {@link DatacenterBroker} to up scale the VM.
     * The up scaling is performed by creating new VMs to attend new arrived Cloudlets
     * and then balance the load.
     *
     * @return
     * @see #setUnderloadPredicate(Predicate)
     */
    Predicate<Vm> getUnderloadPredicate();


    /**
     * Sets a {@link Predicate} that defines when the {@link #getVm() Vm} is underloaded or not,
     * making the {@link DatacenterBroker} to down scale the VM.
     * The down scaling is performed by creating removing the VM and resubmitting the waiting Cloudlets to the brokers
     * in order to balance the load.
     *
     * @param predicate a predicate that checks certain conditions
     *                  to define a {@link #getVm() Vm} as underloaded.
     *                  The predicate receives the Vm that has to be checked.
     *                  Such a condition can be defined, for instance,
     *                  based on Vm's {@link Vm#getCpuPercentUsage(double)} CPU usage}
     *                  and/or any other VM resource usage.
     *                  Despite the VmScaling already is already linked to a {@link #getVm() Vm},
     *                  the Vm parameter for the {@link Predicate} enables reusing the same predicate
     *                  to detect underload of different VMs.
     * @return
     */
    VmScaling setUnderloadPredicate(Predicate<Vm> predicate);

    /**
     * Performs the actual request to scale the Vm  down,
     * depending if it is underloaded.
     * This method is automatically called by {@link #requestDownScalingIfPredicateMatches(org.cloudsimplus.listeners.VmHostEventInfo)}
     * when it is verified that the Vm is underloaded.
     *
     * @param time current simulation time
     * @return true if the request was actually sent, false otherwise
     */
    boolean requestDownScaling(final double time);

}
