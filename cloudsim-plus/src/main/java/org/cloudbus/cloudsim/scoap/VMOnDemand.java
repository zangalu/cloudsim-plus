package org.cloudbus.cloudsim.scoap;

public class VMOnDemand {
    String name;
    double mu, running_payment;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public double getMu() {
        return mu;
    }
    public void setMu(double mu) {
        this.mu = mu;
    }
    public double getRunning_payment() {
        return running_payment;
    }
    public void setRunning_payment(double running_payment) {
        this.running_payment = running_payment;
    }

}
