package org.cloudbus.cloudsim.scoap;

import umontreal.iro.lecuyer.randvar.ExponentialGen;
import umontreal.iro.lecuyer.rng.MRG32k3a;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class ExponentialRequestsArrivalGenerator implements RequestsArrivalGenerator
{

    private static final String ARRIVAL_GENERATOR_BATCH_SIZE_PROPERTY = "arrivalGeneratorBatchSize";
    private static final String REQUEST_WEIGHT = "requestWeight";

    private Queue<Double> requests;
    private Queue<Integer> workLoadQueue;
    private int batcheSize;
    private double currentTime;
    private double lambdaSwitchTimestamp;
    private double nextArrival;
    private double timeStamp;
    private double workload;
    private Integer totalRequests;
    private Integer requestsInInterval;
    private double requestsPeakMax;
    private Integer requestsAvg;
    private Integer requestWeight;

    

    public ExponentialRequestsArrivalGenerator(Map<String,String> simulationConfigMap){
        currentTime=0.0;
        lambdaSwitchTimestamp=0.0;
        totalRequests=0;
        workLoadQueue = new LinkedList<>();
        batcheSize= Integer.valueOf(simulationConfigMap.get(ARRIVAL_GENERATOR_BATCH_SIZE_PROPERTY));
        requestWeight= Integer.valueOf(simulationConfigMap.get(REQUEST_WEIGHT));
    }

    @Override
    public Queue<Double> getRequests()
    {
        requests = new LinkedList<>();
        double interval=10.0;
        double arrivalRate;
        int scaleWorkload=10;


        //number of requests to read from the workload at every iteration
        workload=0;

        for(int i=0; i<(workLoadQueue.size()/batcheSize); i++)
        {
            if(workLoadQueue.peek()!= null){
                double lambda = workLoadQueue.poll();
                totalRequests += Double.valueOf(lambda).intValue();
                if(lambda==0){
                    lambda=1;
                }
                arrivalRate=lambda/interval;

                if(arrivalRate>requestsPeakMax)
                {
                    requestsPeakMax = arrivalRate;
                }

                ExponentialGen genArr = new ExponentialGen(new MRG32k3a(),
                    arrivalRate); //new exponential with new lambda
                double nextDouble = genArr.nextDouble();

                timeStamp=currentTime;
                nextArrival=nextDouble;
                currentTime+=nextArrival;


                //workload += nextDouble*scaleWorkload;
                //requests.add(nextDouble*requestWeight);

                workload += nextDouble;
                requests.add(workload*requestWeight);


            }
            if(workLoadQueue.size()/batcheSize == 1 && batcheSize>1){
                batcheSize--;
            }
        }
        return requests;
    }


   /* @Override
    public Queue<Double> getRequests()
    {
        double interval=10.0;//Interval in seconds between two values in the log file.
        RandomVariateGen genArr;
        double arrivalRate;


        //number of requests to read from the workload at every iteration
        workload=0;

        //for(int i=0; i<(workLoadQueue.size()/(simulationTime*interval)); i++)
        while(workLoadQueue.size() > 100)
        {
            //TODO RIGUARDARE FATTORI DI SCALA (TEMPO REALE E DI SIMULAZIONE DEVONO ESSERE SCORRELATI)
            if(workLoadQueue.peek()!= null){

                //double requestLoad  =workLoadQueue.poll()/interval;
                //workload +=requestLoad*10/simulationTime;

                lambda += workLoadQueue.poll();
                System.out.println(workLoadQueue.size());

                if(lambda==0){
                    lambda=1;
                }
                arrivalRate=lambda/interval;

                genArr = new ExponentialGen(new MRG32k3a(), arrivalRate); //new exponential with new lambda
                double nextDouble = genArr.nextDouble();

                requests.add(nextDouble);
            }
        }
        return requests;
    }*/


    @Override
    public void setWorkload(String currentLine){
        Integer lambda = Integer.parseInt(currentLine);
        if (lambda == 0) {
            lambda = 1;
        }

        workLoadQueue.add(lambda);
    }

    @Override
    public double getInstantWorkload()
    {
        return workload*10;
    }

    @Override
    public double allocateRequest()
    {
        if(requests.isEmpty()){
            System.out.println("REQUEST LIST IS EMPTY");
            return 0.0;
        }
        return requests.poll();
    }

    @Override
    public int workloadQueue()
    {
        return workLoadQueue.size();
    }

    @Override
    public double getRequestPeak()
    {
        return this.requestsPeakMax;
    }

    @Override
    public double getRequestsAvg()
    {
        return this.requestsAvg;
    }

    @Override
    public int getTotalPackets()
    {
        return totalRequests;
    }

    @Override
    public double getCurrentTime()
    {
        return currentTime;
    }

}
