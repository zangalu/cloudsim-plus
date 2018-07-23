package org.cloudbus.cloudsim.scoap;

import java.util.LinkedList;
import java.util.Queue;

public class ExponentialRequestsArrivalGenerator implements RequestsArrivalGenerator
{

    Queue queue;
    private Queue<Double> requests;
    private Queue<Integer> workLoadQueue;
    double currentTime, lambdaSwitchTimestamp, nextArrival,timeStamp, workload;
    Integer totalRequests, requestsInInterval, lambda;

    public ExponentialRequestsArrivalGenerator(){
        currentTime=0.0;
        lambdaSwitchTimestamp=0.0;
        totalRequests=0;
        lambda=0;
        workLoadQueue = new LinkedList<>();
    }

    @Override
    public Queue<Double> getRequests(int simulationTime)
    {
        requests = new LinkedList<>();
        double interval=10.0;

        //number of requests to read from the workload at every iteration
        workload=0;
        for(int i=0; i<(workLoadQueue.size()/(simulationTime*interval)); i++)
        {
            if(workLoadQueue.peek()!= null){
                double requestLoad  =workLoadQueue.poll()/interval;
                workload +=requestLoad*10/simulationTime;
                requests.add(requestLoad);

            }
        }
        return requests;
    }

    @Override
    public void setWorkload(String currentLine){
        lambda = Integer.parseInt(currentLine);
        if (lambda == 0) {
            lambda = 1;
        }

        workLoadQueue.add(lambda);
    }

    @Override
    public double getInstantWorkload()
    {

        System.out.println("WORKLOAD QUEUE SIZE --> "+workLoadQueue.size()+" !!!!!");
        System.out.println("WORKLOAD  --> "+workload+" !!!!!");
        return workload;
    }

    @Override
    public double allocateRequest()
    {
        return requests.poll();
    }
}
