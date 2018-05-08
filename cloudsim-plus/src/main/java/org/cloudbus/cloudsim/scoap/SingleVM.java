package org.cloudbus.cloudsim.scoap;


public class SingleVM {
    Integer num,r=0;
    String vm_name, reservationType;

    public Integer getNum() {
        return num;
    }
    public void setNum(Integer num) {
        this.num = num;
    }
    public Integer getR() {
        return r;
    }
    public void setR(Integer r) {
        this.r = r;
    }
    public String getVm_name() {
        return vm_name;
    }
    public void setVm_name(String vm_name) {
        this.vm_name = vm_name;
    }
    public String getReservationType() {
        return reservationType;
    }
    public void setReservationType(String reservationType) {
        this.reservationType = reservationType;
    }
}

