package org.cloudbus.cloudsim.scoap;

public class VMReservedPayment { //vm_payment in vmreserved
    double periodic_payment, running_payment;
    String reservation_type;

    public double getPeriodic_payment() {
        return periodic_payment;
    }
    public void setPeriodic_payment(double periodic_payment) {
        this.periodic_payment = periodic_payment;
    }
    public double getRunning_payment() {
        return running_payment;
    }
    public void setRunning_payment(double running_payment) {
        this.running_payment = running_payment;
    }
    public String getReservation_type() {
        return reservation_type;
    }
    public void setReservation_type(String reservation_type) {
        this.reservation_type = reservation_type;
    }
}

