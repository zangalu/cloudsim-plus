package org.cloudbus.cloudsim.scoap;

import java.util.Queue;

public interface RequestsArrivalGenerator
{
    Queue<Double> getRequests();

    void setWorkload(String workloadLine);

    double getInstantWorkload();

    double allocateRequest();

    int workloadQueue();

    double getRequestPeak();

    double getRequestsAvg();

    int getTotalPackets();

    double getCurrentTime();
}
