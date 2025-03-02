package org.fog.test.perfeval;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import java.text.DecimalFormat;
import java.util.*;

import static org.fog.test.perfeval.Constants.CLOUDLET_LENGTH1;
import static org.fog.test.perfeval.Constants.CLOUDLET_LENGTH2;

public class TaskSchedulingSimulation {

    public static void main(String[] args) {

        try {
            // Step 1: Initialize the CloudSim package.
            int numUser = 1;
            Calendar calendar = Calendar.getInstance();
            boolean traceFlag = false;

            CloudSim.init(numUser, calendar, traceFlag);

            // Step 2: Create Datacenters
            Datacenter datacenter0 = createDatacenter();

            // Step 3: Create Broker
            DatacenterBroker broker = createMPABroker("MPA_Broker");
            int brokerId = broker != null ? broker.getId() : 0;

            // Step 4: Create VMs
            List<Vm> vmList = createVM(brokerId);
            // Submit VM list to the broker
            if (broker != null) {
                broker.submitVmList(vmList);
            }

            // Step 5: Create Cloudlets
            List<Cloudlet> cloudletList = createCloudlet(brokerId);
            // Submit cloudlet list to the broker
            if (broker != null) {
                broker.submitCloudletList(cloudletList);
            }

            // Step 6: Start the simulation
            CloudSim.startSimulation();

            // Step 7: Stop the simulation
            CloudSim.stopSimulation();

            // Step 8: Print results
            List<Cloudlet> resultList = broker.getCloudletReceivedList();
//            System.out.println("resultList0: "+ resultList.size());
//            printCloudletList(resultList , vmList);




            CloudSim.init(numUser, calendar, traceFlag);
            Datacenter datacenter1 = createDatacenter();

            // Step 4: Create VMs
            DatacenterBroker broker2 = createMPABroker("MPA_Broker2");
            int brokerId2 = broker != null ? broker.getId() : 0;

            // Step 4: Create VMs
            List<Vm> vmList2 = createVM2(brokerId2);
            // Submit VM list to the broker
            if (broker2 != null) {
                broker2.submitVmList(vmList2);
            }

            // Step 5: Create Cloudlets
            List<Cloudlet> cloudletList2 = createCloudlet2(brokerId2);
            // Submit cloudlet list to the broker
            if (broker2 != null) {
                broker2.submitCloudletList(cloudletList2);
            }


            // Step 6: Start the simulation
            CloudSim.startSimulation();

            // Step 7: Stop the simulation
            CloudSim.stopSimulation();

            // Step 8: Print results
            List<Cloudlet> resultList2 = broker2.getCloudletReceivedList();
            System.out.println("resultList1: "+ resultList2.size());

            printCloudletList(resultList2 , vmList2);


        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Unwanted errors happen");
        }
    }


    private static List<Cloudlet> createCloudlet(int brokerId) {
        List<Cloudlet> cloudletList = new ArrayList<Cloudlet>();
        int pesNumber = 1;
        long fileSize = 300;
        long outputSize = 300;
        int i = 0;

        for (int length : CLOUDLET_LENGTH1) {
            Cloudlet cloudlet = new Cloudlet(i, length, pesNumber, fileSize, outputSize, new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull());
            cloudlet.setUserId(brokerId);
            cloudletList.add(cloudlet);
            i++;
        }

        return cloudletList;
    }

    private static List<Cloudlet> createCloudlet2(int brokerId) {
        List<Cloudlet> cloudletList = new ArrayList<Cloudlet>();
        int pesNumber = 1;
        long fileSize = 300;
        long outputSize = 300;
        int i = 0;

        for (int length : CLOUDLET_LENGTH2) {
            Cloudlet cloudlet = new Cloudlet(i, length, pesNumber, fileSize, outputSize, new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull());
            cloudlet.setUserId(brokerId);
            cloudletList.add(cloudlet);
            i++;
        }

        return cloudletList;
    }
    private static List<Vm> createVM(int brokerId) {
        List<Vm> vmList = new ArrayList<Vm>();

        long size = 10000; // image size (MB)
        int ram = 512; // vm memory (MB)
        int pesNumber = 1; // number of CPUs
        String vmm = "Xen";
        Random random = new Random();

        for (int vmId = 0; vmId < Constants.NO_OF_VMS; vmId++) {
            long bw = 1000 + random.nextInt(9001);
            int mips = vmId < Constants.NO_OF_VMS / 2 ? 2000 : 4000;
            Vm vm = new Vm(vmId, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
//            vm.setCostPerSec(400);
            vmList.add(vm);
        }

        return vmList;
    }
    private static List<Vm> createVM2(int brokerId) {
        List<Vm> vmList = new ArrayList<Vm>();

        long size = 10000; // image size (MB)
        int ram = 512; // vm memory (MB)
        int pesNumber = 1; // number of CPUs
        String vmm = "Xen";
        Random random = new Random();

        for (int vmId = 0; vmId < Constants.NO_OF_VMS2; vmId++) {
            long bw = 1000 + random.nextInt(9001);
            int mips = vmId < Constants.NO_OF_VMS2 / 2 ? 2000 : 4000;
            Vm vm = new Vm(vmId, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
//            vm.setCostPerSec(400);
            vmList.add(vm);
        }

        return vmList;
    }

    private static Datacenter createDatacenter() {
        // Here are the steps to create a Datacenter:
        // 1. We need to create a list to store our machine
        List<Host> hostList = new ArrayList<Host>();

        // 2. A Machine contains one or more PEs or CPUs/Cores.
        // In this example, it will have only one core.
        List<Pe> peList = new ArrayList<Pe>();

        int mips = 180000;

        // 3. Create PEs and add these into the list.
        peList.add(new Pe(0, new PeProvisionerSimple(mips))); // need to store Pe id and MIPS Rating

        // 4. Create Hosts with its id and list of PEs and add them to the list of machines
        int hostId = 0;
        int ram = 32768; // host memory (MB)
        long storage = 10000000; // host storage
        int bw = 100000;

        hostList.add(
                new Host(
                        hostId,
                        new RamProvisionerSimple(ram),
                        new BwProvisionerSimple(bw),
                        storage,
                        peList,
                        new VmSchedulerTimeShared(peList)
                )
        ); // This is our first machine

        // 5. Create a DatacenterCharacteristics object that stores the properties of a data center: architecture, OS, list of Machines, allocation policy: time- or space-shared, time zone and its price (G$/Pe time unit).
        String arch = "x86"; // system architecture
        String os = "Linux"; // operating system
        String vmm = "Xen";
        double time_zone = 10.0; // time zone this resource located
        double cost = 3.0; // the cost of using processing in this resource
        double costPerMem = 0.05; // the cost of using memory in this resource
        double costPerStorage = 0.1;  // the cost of using storage in this resource
        double costPerBw = 0.1; // the cost of using bw in this resource

        LinkedList<Storage> storageList = new LinkedList<Storage>(); // we are not adding SAN devices by now

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);

        // 6. Finally, we need to create a PowerDatacenter object.
        Datacenter datacenter = null;
        try {
            datacenter = new Datacenter("Datacenter_0", characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return datacenter;
    }

    private static DatacenterBroker createMPABroker(String name) {
        DatacenterBroker broker = null;
        try {
            broker = new MPABroker(name);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return broker;
    }

    private static void printCloudletList(List<Cloudlet> list, List<Vm> vms) {
        System.out.println("Cloudlets:");
        for (int i = 0; i < list.size(); i++) {
            System.out.print(list.get(i).getVmId()+ ", ");
        }
        System.out.println();
        System.out.println("Vms:");
        for (int i = 0; i < vms.size(); i++) {
            System.out.print(vms.get(i).getId()+ ", ");
        }
        int size = list.size();
        Cloudlet cloudlet;

        String indent = "\t";
        System.out.println();
        System.out.println("========== OUTPUT ==========");
        Log.printLine("Cloudlet ID" + indent +indent+ "STATUS" +indent+
                indent + "Data center ID" + indent+
                indent + "VM ID" +
                indent + indent + "Time" +
                indent + "Start Time" +
                indent + "Finish Time"+
                indent +  indent + "Length" +
                indent +  indent + "Vm mips"+
                indent +  indent + "Vm bw");

        DecimalFormat dft = new DecimalFormat("###.##");
        for (int i = 0; i < size; i++) {
            cloudlet = list.get(i);
            System.out.print(indent + cloudlet.getCloudletId() + indent + indent);

            if (cloudlet.getStatus() == Cloudlet.SUCCESS) {
                System.out.println(indent+"SUCCESS" + indent+
                        indent + indent + dft.format(cloudlet.getResourceId()) + indent+
                        indent + indent + indent + dft.format(cloudlet.getVmId()) +
                        indent + indent+indent + dft.format(cloudlet.getActualCPUTime()) +
                        indent + indent + dft.format(cloudlet.getExecStartTime()) +
                        indent + indent + indent + dft.format(cloudlet.getFinishTime())+
                        indent + indent + dft.format(cloudlet.getCloudletLength())+
                        indent + indent + indent + dft.format(vms.get(cloudlet.getVmId()).getMips())+
                        indent + indent + dft.format(vms.get(cloudlet.getVmId()).getBw())
                );
            }
        }
//        exportCloudletList(list , vms);
//        double makespan = bcalcMakespan(list);
//        Log.printLine("Makespan using RoundRobin: " + makespan);
    }

}
