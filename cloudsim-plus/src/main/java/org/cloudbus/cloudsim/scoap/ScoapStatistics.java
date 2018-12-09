package org.cloudbus.cloudsim.scoap;

public class ScoapStatistics
{
    private String allocationStrategy;
    private int booting;
    private int isteresi;
    private boolean slowstart;
    private int timeout;
    private int totalPackets;

    private double cost_AVG;
    private double cost_Min;
    private double cost_Max;

    private double peak_AVG;
    private double peak_Min;
    private double peak_Max;

    private double reconfiguration_AVG;
    private double reconfiguration_Min;

    public ScoapStatistics()
    {
        allocationStrategy="default";
        booting=0;
        isteresi=0;
        slowstart=false;
        timeout =0;
        totalPackets =0;

        cost_AVG = 0.0;
        cost_Min = 0.0;
        cost_Max = 0.0;

        peak_AVG = 0.0;
        peak_Min = 0.0;
        peak_Max = 0.0;

        reconfiguration_AVG = 0.0;
        reconfiguration_Min = 0.0;
    }

    public double getCost_AVG()
    {
        return cost_AVG;
    }

    public void setCost_AVG(double cost_AVG)
    {
        this.cost_AVG = cost_AVG;
    }

    public double getCost_Min()
    {
        return cost_Min;
    }

    public void setCost_Min(double cost_Min)
    {
        this.cost_Min = cost_Min;
    }

    public double getCost_Max()
    {
        return cost_Max;
    }

    public void setCost_Max(double cost_Max)
    {
        this.cost_Max = cost_Max;
    }

    public double getPeak_AVG()
    {
        return peak_AVG;
    }

    public void setPeak_AVG(double peak_AVG)
    {
        this.peak_AVG = peak_AVG;
    }

    public double getPeak_Min()
    {
        return peak_Min;
    }

    public void setPeak_Min(double peak_Min)
    {
        this.peak_Min = peak_Min;
    }

    public double getPeak_Max()
    {
        return peak_Max;
    }

    public void setPeak_Max(double peak_Max)
    {
        this.peak_Max = peak_Max;
    }

    public double getReconfiguration_AVG()
    {
        return reconfiguration_AVG;
    }

    public void setReconfiguration_AVG(double reconfiguration_AVG)
    {
        this.reconfiguration_AVG = reconfiguration_AVG;
    }

    public double getReconfiguration_Min()
    {
        return reconfiguration_Min;
    }

    public void setReconfiguration_Min(double reconfiguration_Min)
    {
        this.reconfiguration_Min = reconfiguration_Min;
    }

    public int getBooting()
    {
        return booting;
    }

    public void setBooting(int booting)
    {
        this.booting = booting;
    }

    public int getIsteresi()
    {
        return isteresi;
    }

    public void setIsteresi(int isteresi)
    {
        this.isteresi = isteresi;
    }

    public boolean isSlowstart()
    {
        return slowstart;
    }

    public void setSlowstart(boolean slowstart)
    {
        this.slowstart = slowstart;
    }

    public int getTimeout()
    {
        return timeout;
    }

    public void setTimeout(int timeout)
    {
        this.timeout = timeout;
    }

    public String getAllocationStrategy()
    {
        return allocationStrategy;
    }

    public void setAllocationStrategy(String allocationStrategy)
    {
        this.allocationStrategy = allocationStrategy;
    }

    public int getTotalPackets()
    {
        return totalPackets;
    }

    public void setTotalPackets(int totalPackets)
    {
        this.totalPackets = totalPackets;
    }


           /* out.println("awt media: "+" min: "+" Max: ");
            out.println("peak medio: "+" Min : "+ " Max : ");
            out.println("cost medio: "+" Min: "+" Max : ");
            out.println("toatal cost"+ allocationStrategy.getTotalCost());
            out.println("reconfigurations medio: "+" Min: ");
            out.println("availab media: "+" Min: "+" Max: ");*/
}
