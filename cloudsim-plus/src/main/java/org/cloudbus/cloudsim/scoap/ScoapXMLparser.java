package org.cloudbus.cloudsim.scoap;

import org.cloudbus.cloudsim.util.ResourceLoader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.io.File;

import static org.cloudbus.cloudsim.util.ResourceLoader.getInputStream;

public class ScoapXMLparser {
    private static final String VM_ON_DEMAND = "vmOnDemand_eu-west-1_linux_.xml";
    //private static final String PATH = "resources/output/";

    private static final String VM_RESERVED = "vmReserved_eu-west-1_linux_c1-xlarge.xml";

    public static void XMLOnDemand(ArrayList<VMOnDemand> singleVMs, ArrayList<Threshold> thresholds, String path) {

        VMOnDemand newVM;
        Threshold newThreshold;
        SingleVM newSingleVM;

        try {

            final InputStream fXmlFile = ResourceLoader.getInputStream(ScoapXMLparser.class, path+VM_ON_DEMAND);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);

            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("single_virtual_machine");

            //System.out.println(nList.getLength());


            //Fetch single VMS
            for (int temp = 0; temp < nList.getLength(); temp++) {

                Node nNode = nList.item(temp);

                newVM = new VMOnDemand();

                //System.out.println("\nCurrent Element :" + nNode.getNodeName());

                if (nNode.getNodeType() == Node.ELEMENT_NODE) {

                    Element eElement = (Element) nNode;
                    //System.out.println("Nome VM : " +eElement.getElementsByTagName("name").item(0).getTextContent() +" con MU="+ eElement.getElementsByTagName("mu").item(0).getTextContent());

                    newVM.setName(eElement.getElementsByTagName("name").item(0).getTextContent());
                    newVM.setMu(Double.parseDouble(eElement.getElementsByTagName("mu").item(0).getTextContent()));

                    NodeList nl = eElement.getElementsByTagName("vm_payment");

                    for(int x = 0;x < nl.getLength();x++)
                    {
                        Node child_node = nl.item(x);
                        if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element child_eElement = (Element) child_node;

                            //System.out.println("running payment:" + child_eElement.getAttribute("running_payment"));
                            newVM.setRunning_payment(Double.parseDouble(child_eElement.getAttribute("running_payment")));
                        }

                    }

                }
                singleVMs.add(newVM);
            }



            //Fetch the THRESHOLDS

            nList = doc.getElementsByTagName("threshold");
            //System.out.println(nList.getLength());

            for (int temp = 0; temp < nList.getLength(); temp++) {

                newThreshold = new Threshold();

                Node nNode = nList.item(temp);

                //System.out.println("\nCurrent Element :" + nNode.getNodeName());

                if (nNode.getNodeType() == Node.ELEMENT_NODE) {

                    Element eElement = (Element) nNode;

                    //System.out.println("Soglia : " + eElement.getAttribute("workload"));
                    newThreshold.setWorkLoad(73.38+Double.parseDouble(eElement.getAttribute("workload")));
                    newThreshold.setId(Integer.decode(eElement.getAttribute("id")));
                    //System.out.println("Macchine da attivare:");
                    NodeList nl = eElement.getElementsByTagName("on_demand");

                    for(int x = 0;x < nl.getLength();x++)
                    {
                        newSingleVM = new SingleVM();
                        Node child_node = nl.item(x);
                        if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element child_eElement = (Element) child_node;

                            //System.out.println("N°:" + child_eElement.getAttribute("num")+" di tipo:"+ child_eElement.getAttribute("vm_name"));
                            newSingleVM.setNum(Integer.decode(child_eElement.getAttribute("num")));
                            newSingleVM.setVm_name(child_eElement.getAttribute("vm_name"));
                            newThreshold.getVMsToActivate().add(newSingleVM);
                        }

                    }

                }
                thresholds.add(newThreshold);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void XMLReserved(ArrayList<VMReservedType> typeOfReservedVMs,ArrayList<Threshold> thresholds, String path){

        VMReservedType newVMReserved;
        Threshold newThreshold;
        VMReservedPayment vm_payment;

        try {
            final InputStream fXmlFile = ResourceLoader.getInputStream(ScoapXMLparser.class, path+VM_RESERVED);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);

            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("single_virtual_machine");
            for (int temp = 0; temp < nList.getLength(); temp++) {

                Node nNode = nList.item(temp);

                newVMReserved = new VMReservedType();

                if (nNode.getNodeType() == Node.ELEMENT_NODE) {

                    Element eElement = (Element) nNode;

                    newVMReserved.setName(eElement.getElementsByTagName("name").item(0).getTextContent());
                    newVMReserved.setRegion(eElement.getElementsByTagName("region").item(0).getTextContent());
                    newVMReserved.setProvider(eElement.getElementsByTagName("provider").item(0).getTextContent());
                    newVMReserved.setReservation_period(eElement.getElementsByTagName("reservation_period").item(0).getTextContent());
                    newVMReserved.setOs(eElement.getElementsByTagName("os").item(0).getTextContent());
                    newVMReserved.setMu(Double.parseDouble(eElement.getElementsByTagName("mu").item(0).getTextContent()));

                    NodeList nl = eElement.getElementsByTagName("vm_payment");

                    for(int x = 0;x < nl.getLength();x++)
                    {
                        vm_payment = new VMReservedPayment();

                        Node child_node = nl.item(x);
                        if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element child_eElement = (Element) child_node;

                            //System.out.println("running payment:" + child_eElement.getAttribute("running_payment"));
                            vm_payment.setRunning_payment(Double.parseDouble(child_eElement.getAttribute("running_payment")));
                            vm_payment.setPeriodic_payment(Double.parseDouble(child_eElement.getAttribute("periodic_payment")));
                            vm_payment.setReservation_type(child_eElement.getAttribute("reservation_type"));
                        }
                        newVMReserved.getVMPayments().add(vm_payment);

                    }
                    typeOfReservedVMs.add(newVMReserved);
                }
            }
            nList = doc.getElementsByTagName("reserved");
            for (int temp = 0; temp < nList.getLength(); temp++) {

                Node nNode = nList.item(temp);

                newThreshold =new Threshold();

                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    newThreshold.setWorkLoad(Double.parseDouble(eElement.getAttribute("workload")));
                    newThreshold.setId(Integer.decode(eElement.getAttribute("id")));
                    SingleVM newVM = new SingleVM();
                    newVM.setNum(1);
                    newVM.setVm_name("c1-xlarge");
                    newVM.setR(1);
                    newVM.setReservationType(eElement.getAttribute("reservation_type"));
                    newThreshold.getVMsToActivate().add(newVM);
                }
                thresholds.add(newThreshold);
            }


        }catch (Exception e) {
            e.printStackTrace();
        }
    }

}

