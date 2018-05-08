package org.cloudbus.cloudsim.scoap;


import java.util.ArrayList;

public class VMReservedType { //single_virtual_machine
    String name,region, provider, reservation_period,os;
    double mu;
    ArrayList<VMReservedPayment> VMPayments = new ArrayList<VMReservedPayment>();
    //da inserire qui vm_thresholds

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getRegion() {
        return region;
    }
    public void setRegion(String region) {
        this.region = region;
    }
    public String getProvider() {
        return provider;
    }
    public void setProvider(String provider) {
        this.provider = provider;
    }
    public String getReservation_period() {
        return reservation_period;
    }
    public void setReservation_period(String reservation_period) {
        this.reservation_period = reservation_period;
    }
    public double getMu() {
        return mu;
    }
    public void setMu(double mu) {
        this.mu = mu;
    }
    public ArrayList<VMReservedPayment> getVMPayments() {
        return VMPayments;
    }
    public void setVMPayments(ArrayList<VMReservedPayment> vMPayments) {
        VMPayments = vMPayments;
    }
    public String getOs() {
        return os;
    }
    public void setOs(String os) {
        this.os = os;
    }
}
