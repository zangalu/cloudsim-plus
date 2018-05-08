package org.cloudbus.cloudsim.scoap;


import java.util.ArrayList;

public class Threshold {

    Integer Id;
    double WorkLoad, activationTime, arrivalRateValue, deactivationTime, deactivationWorkload;

    ArrayList<SingleVM> VMsToActivate = new ArrayList<SingleVM>();

    public Integer getId() {
        return Id;
    }

    public void setId(Integer id) {
        Id = id;
    }

    public double getWorkLoad() {
        return WorkLoad;
    }

    public void setWorkLoad(double workLoad) {
        WorkLoad = workLoad;
    }

    public double getActivationTime() {
        return activationTime;
    }

    public void setActivationTime(double activationTime) {
        this.activationTime = activationTime;
    }

    public double getArrivalRateValue() {
        return arrivalRateValue;
    }

    public void setArrivalRateValue(double arrivalRateValue) {
        this.arrivalRateValue = arrivalRateValue;
    }

    public double getDeactivationTime() {
        return deactivationTime;
    }

    public void setDeactivationTime(double deactivationTime) {
        this.deactivationTime = deactivationTime;
    }

    public double getDeactivationWorkload() {
        return deactivationWorkload;
    }

    public void setDeactivationWorkload(double deactivationWorkload) {
        this.deactivationWorkload = deactivationWorkload;
    }

    public ArrayList<SingleVM> getVMsToActivate() {
        return VMsToActivate;
    }

    public void setVMsToActivate(ArrayList<SingleVM> vMsToActivate) {
        VMsToActivate = vMsToActivate;
    }

}

