package cloudsim.simulation;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.VmSchedulerSpaceShared;


import java.io.FileWriter;
import java.util.*;

public class VmAllocationComparison {
    public static void main(String[] args) {
        try {
            System.out.println("\n===== RUNNING TIME-SHARED VM SCHEDULING SIMULATION =====\n");
            long timeSharedStart = System.currentTimeMillis();
            List<Cloudlet> timeSharedResults = runSimulation("TimeShared", true);
            long timeSharedEnd = System.currentTimeMillis();
            double timeSharedRuntime = (timeSharedEnd - timeSharedStart) / 1000.0;

            System.out.println("\n===== RUNNING SPACE-SHARED VM SCHEDULING SIMULATION =====\n");
            long spaceSharedStart = System.currentTimeMillis();
            List<Cloudlet> spaceSharedResults = runSimulation("SpaceShared", false);
            long spaceSharedEnd = System.currentTimeMillis();
            double spaceSharedRuntime = (spaceSharedEnd - spaceSharedStart) / 1000.0;

            generateComparisonReport(timeSharedResults, spaceSharedResults, timeSharedRuntime, spaceSharedRuntime);
            System.out.println("\nAll simulations completed. Reports generated.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<Cloudlet> runSimulation(String name, boolean isTimeShared) throws Exception {
        CloudSim.init(1, Calendar.getInstance(), false);
        Datacenter datacenter = createDatacenter(name + "_Datacenter", isTimeShared);
        DatacenterBroker broker = new DatacenterBroker(name + "_Broker");

        List<Vm> vmlist = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            CloudletScheduler scheduler = isTimeShared ?
                    new CloudletSchedulerTimeShared() : new CloudletSchedulerSpaceShared();
            Vm vm = new Vm(i, broker.getId(), 1000, 1, 512, 1000, 1000, "Xen", scheduler);
            vmlist.add(vm);
        }

        List<Cloudlet> cloudletList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Cloudlet cloudlet = new Cloudlet(i, 40000, 1, 300, 300,
                    new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull());
            cloudlet.setUserId(broker.getId());
            cloudletList.add(cloudlet);
        }

        broker.submitVmList(vmlist);
        broker.submitCloudletList(cloudletList);

        CloudSim.startSimulation();
        List<Cloudlet> finishedCloudlets = broker.getCloudletReceivedList();
        CloudSim.stopSimulation();

        generateSimulationLog(name, finishedCloudlets, vmlist);
        return finishedCloudlets;
    }

    private static Datacenter createDatacenter(String name, boolean isTimeShared) throws Exception {
        List<Host> hostList = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            List<Pe> peList = new ArrayList<>();
            peList.add(new Pe(0, new PeProvisionerSimple(1000)));

            VmScheduler vmScheduler = isTimeShared ?
                    new VmSchedulerTimeShared(peList) : new VmSchedulerSpaceShared(peList);

            Host host = new Host(i, new RamProvisionerSimple(2048),
                    new BwProvisionerSimple(10000), 1000000, peList, vmScheduler);
            hostList.add(host);
        }

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                "x86", "Linux", "Xen", hostList,
                10.0, 3.0, 0.05, 0.1, 0.1);

        return new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), new LinkedList<>(), 0);
    }

    private static void generateSimulationLog(String strategy, List<Cloudlet> cloudlets, List<Vm> vms) {
        try (FileWriter writer = new FileWriter(strategy + "_simulation_log.txt")) {
            writer.write("====== " + strategy + " VM SCHEDULING SIMULATION LOG ======\n\n");
            writer.write("SIMULATION CONFIGURATION:\n");
            writer.write("- Scheduling Strategy: " + strategy + "\n");
            writer.write("- Number of VMs: " + vms.size() + "\n");
            writer.write("- Number of Cloudlets: " + cloudlets.size() + "\n\n");

            writer.write("CLOUDLET EXECUTION RESULTS:\n");
            writer.write(String.format("%-10s %-10s %-10s %-15s %-15s %-15s\n",
                    "Cloudlet", "Status", "VM ID", "Time", "Start Time", "Finish Time"));

            double totalExecTime = 0;
            int successCount = 0;

            for (Cloudlet cl : cloudlets) {
                boolean success = cl.getStatus() == Cloudlet.SUCCESS;
                if (success) {
                    totalExecTime += cl.getActualCPUTime();
                    successCount++;
                }
                writer.write(String.format("%-10d %-10s %-10d %-15.2f %-15.2f %-15.2f\n",
                        cl.getCloudletId(),
                        success ? "SUCCESS" : "FAILED",
                        cl.getVmId(),
                        cl.getActualCPUTime(),
                        cl.getExecStartTime(),
                        cl.getFinishTime()));
            }

            writer.write("\nPERFORMANCE SUMMARY:\n");
            writer.write("- Successful Cloudlets: " + successCount + "/" + cloudlets.size() + "\n");
            if (successCount > 0) {
                writer.write("- Avg Execution Time: " + (totalExecTime / successCount) + "\n");
                writer.write("- Makespan: " + getMaxFinishTime(cloudlets) + "\n");
            }
            System.out.println("Simulation log generated: " + strategy + "_simulation_log.txt");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void generateComparisonReport(List<Cloudlet> timeShared, List<Cloudlet> spaceShared,
                                                 double timeSharedRuntime, double spaceSharedRuntime) {
        try (FileWriter writer = new FileWriter("scheduling_comparison_report.txt")) {
            writer.write("====== VM SCHEDULING STRATEGY COMPARISON REPORT ======\n\n");

            writer.write("RUNTIME COMPARISON:\n");
            writer.write("- Time-Shared Runtime: " + timeSharedRuntime + "s\n");
            writer.write("- Space-Shared Runtime: " + spaceSharedRuntime + "s\n");
            writer.write("- Faster: " + (timeSharedRuntime < spaceSharedRuntime ? "Time-Shared" : "Space-Shared") + "\n\n");

            double tsMakespan = getMaxFinishTime(timeShared);
            double ssMakespan = getMaxFinishTime(spaceShared);
            double tsAvg = getAverageExecutionTime(timeShared);
            double ssAvg = getAverageExecutionTime(spaceShared);

            writer.write("PERFORMANCE METRICS:\n");
            writer.write("- Time-Shared Makespan: " + tsMakespan + "\n");
            writer.write("- Space-Shared Makespan: " + ssMakespan + "\n");
            writer.write("- Time-Shared Avg Exec Time: " + tsAvg + "\n");
            writer.write("- Space-Shared Avg Exec Time: " + ssAvg + "\n\n");

            writer.write("CONCLUSION:\n");
            writer.write("Based on makespan: " + (tsMakespan < ssMakespan ? "Time-Shared" : "Space-Shared") + " is better\n");
            writer.write("Based on avg exec time: " + (tsAvg < ssAvg ? "Time-Shared" : "Space-Shared") + " is better\n\n");

            writer.write("RECOMMENDATION:\n");
            if (tsMakespan < ssMakespan && tsAvg < ssAvg) {
                writer.write("✅ Use Time-Shared for best results.\n");
            } else if (ssMakespan < tsMakespan && ssAvg < tsAvg) {
                writer.write("✅ Use Space-Shared for best results.\n");
            } else {
                writer.write("🎯 Choose based on priority:\n");
                writer.write("- Time-Shared → better average execution time.\n");
                writer.write("- Space-Shared → better makespan.\n");
            }

            System.out.println("Comparison report generated: scheduling_comparison_report.txt");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static double getMaxFinishTime(List<Cloudlet> list) {
        return list.stream().mapToDouble(Cloudlet::getFinishTime).max().orElse(0);
    }

    private static double getAverageExecutionTime(List<Cloudlet> list) {
        return list.stream()
                .filter(c -> c.getStatus() == Cloudlet.SUCCESS)
                .mapToDouble(Cloudlet::getActualCPUTime)
                .average().orElse(0);
    }
}
