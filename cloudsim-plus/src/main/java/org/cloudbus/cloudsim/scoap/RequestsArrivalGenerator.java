package org.cloudbus.cloudsim.scoap;

import java.util.Queue;

public interface RequestsArrivalGenerator
{
    Queue<Double> getRequests();

    void setWorkload(String workloadLine);

    double getInstantWorkload();

    double allocateRequest();

    int workloadQueue();
}
