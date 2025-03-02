package org.fog.test.perfeval;

import org.apache.commons.math3.special.Gamma;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Vm;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.fog.test.perfeval.Constants.*;

public class Common {
    public static final Random random = new Random();

    public static void applyFADS(int[] predator, int index, double CF, List<int[]> predators , List<? extends Vm> vmList ) {
        double FADS = 0.2; // Threshold for FADS, adjust as needed
        double r = random.nextDouble(); // Generate a random number between 0 and 1
        int numVMs = vmList.size(); // Number of VMs
        int XL = 0; // Lower boundary of VM index (assuming VM indices start from 0)
        int XU = numVMs - 1; // Upper boundary of VM index

        if (r < FADS) {
            // If random number is within FADS threshold, modify position slightly
            double randFactor = random.nextDouble(); // Generate another random number
            predator[index] += CF * (XL + randFactor * (XU - XL)); // Adjust position using CF and boundaries
        } else {
            // If random number exceeds FADS threshold, make a more significant positional change
            int randomPredatorIndex = random.nextInt(predators.size()); // Pick a random predator
            int[] randomPredator = predators.get(randomPredatorIndex); // Get the random predator
            // Calculate new position based on random predator and current predator
            predator[index] = randomPredator[index] + (int)((FADS * (1 - r) + r) * (randomPredator[index] - predator[index]));
        }

        // Ensure the updated position is within the VM boundaries
        predator[index] = Math.max(XL, Math.min(predator[index], XU)); // Clamp the position to valid VM indices
    }


    public static double gamma(double x) {
        return Math.exp(Gamma.logGamma(x));
    }


    public static double calculateCF(int currentIteration, int maxIteration) {
        return (1 - (double)currentIteration / maxIteration) * (2 * (double)currentIteration / maxIteration);
    }

    public static double[][] calculateTaskExecutionTimes(List<? extends Cloudlet> cloudletList, List<? extends Vm> vmList) {

        int numTasks = cloudletList.size();
        int numVMs = vmList.size();

        double[][] taskExecutionTimes = new double[numTasks][numVMs];

        for (int taskIdx = 0; taskIdx < numTasks; taskIdx++) {
            Cloudlet task = cloudletList.get(taskIdx);
            long taskLength = task.getCloudletLength(); // Task length in MI

            for (int vmIdx = 0; vmIdx < numVMs; vmIdx++) {
                Vm vm = vmList.get(vmIdx);
                double vmMips = vm.getMips(); // VM MIPS in MIPS

                // Calculate task execution time on the VM
                double executionTime = (double) taskLength / vmMips;
                taskExecutionTimes[taskIdx][vmIdx] = executionTime;
            }
        }

        return taskExecutionTimes;
    }

    public static double calculateTaskCompletionTime(int taskId, int assignedVM, double[][] taskExecutionTimes) {
        return taskExecutionTimes[taskId][assignedVM];
    }

    public static double evaluateFitness(int[] predator, List<? extends Cloudlet> cloudletList, List<? extends Vm> vmList) {
        if (cloudletList != null && vmList != null) {
            double[][] taskExecutionTimes = calculateTaskExecutionTimes(cloudletList, vmList);
            double makespan = calculateMakespan(predator, taskExecutionTimes , cloudletList, vmList);
            double totalEnergyConsumption = calculateTotalEnergyConsumption(predator, taskExecutionTimes , makespan , vmList);

            return calculateFitness(totalEnergyConsumption, makespan);
        }
        return 0;

    }

    public static double calculateTotalEnergyConsumption(int[] predator, double[][] taskExecutionTimes, double makespan, List<? extends Vm> vmList) {
        double totalEnergyConsumption = 0;
        for (int vmId = 0; vmId < vmList.size(); vmId++) {
            for (int task = 0; task < predator.length; task++) {
                if (predator[task] == vmId) {
                    double executionTime = taskExecutionTimes[task][vmId];
                    double mips = vmList.get(vmId).getMips();
                    double energy = calculateTaskEnergyConsumption(mips, executionTime,makespan);
                    totalEnergyConsumption += energy;
                }
            }
        }
        return totalEnergyConsumption;
    }

    public static double calculateMakespan(int[] predator, double[][] taskExecutionTimes, List<? extends Cloudlet> cloudletList, List<? extends Vm> vmList) {

        double makespan = 0;
//        System.out.println("predator: "+ Arrays.toString(predator));
        // Get the number of tasks and VMs
        int numTasks = cloudletList.size();
        int numVMs = vmList.size();

        // Initialize an array to store completion times for each VM
        double[] completionTimes = new double[numVMs];

        // Initialize the completion times for each VM to 0
        for (int i = 0; i < numVMs; i++) {
            completionTimes[i] = 0;
        }

        // Calculate completion times for each task
        for (int task = 0; task < numTasks; task++) {
            int assignedVM = predator[task]; // Get the VM assigned to the task
            double executionTime = taskExecutionTimes[task][assignedVM]; // Get task execution time on the assigned VM

            // Calculate task completion time on the assigned VM
            double taskCompletionTime = completionTimes[assignedVM] + executionTime;

            // Update the completion time for the assigned VM
            completionTimes[assignedVM] = taskCompletionTime;

            // Update the makespan if the task completion time is greater
            if (taskCompletionTime > makespan) {
                makespan = taskCompletionTime;
            }
        }

        return makespan;
    }


    public static double calculateTaskEnergyConsumption(double mips, double executionTime, double makespan) {
        double K = 1e-8;
        double S = 0.6; // Factor of energy consumed in the active state

        double idleTime = makespan - executionTime; // Idle time for the VM

        double energyActive = executionTime * K * Math.pow(mips, 2);
        double energyIdle = idleTime * K * Math.pow(mips, 2);

        return (energyActive + energyIdle) * S;
    }

    public static double calculateFlowTime(int[] predator, double[][] taskExecutionTimes) {
        double totalFlowTime = 0;

        // Get the number of tasks
        int numTasks = predator.length;

        for (int task = 0; task < numTasks; task++) {
            int assignedVM = predator[task]; // Get the VM assigned to the task

            // Calculate task completion time on the assigned VM
            double taskCompletionTime = calculateTaskCompletionTime(task, assignedVM, taskExecutionTimes);

            // Flow time is the same as completion time since release times are at 0

            // Add flow time to the total flow time
            totalFlowTime += taskCompletionTime;
        }

        return totalFlowTime;
    }


    public static double calculateCO2Emission(double totalEnergy) {
        double totalCO2Emission = 0.0;
        for (int i = 0; i < EMISSION_FACTORS.length; i++) {
            double energyFromSource = totalEnergy * Constants.shareOfEnergySources[i];
            double emissionFromSource = energyFromSource * EMISSION_FACTORS[i] * CARBON_TO_CO2_RATIO;
            totalCO2Emission += emissionFromSource;
        }
        return totalCO2Emission;
    }

    public static double calculateFitness(double energyConsumption, double makespan) {
        return ALPHA * energyConsumption + (1 - ALPHA) * makespan;
    }

    public static void reportResults(int[] bestSolution , List<? extends Cloudlet> cloudletList, List<? extends Vm> vmList) {
        if (bestSolution == null) {
            System.out.println("No solution found.");
            return;
        }
        double fitness = evaluateFitness(bestSolution , cloudletList, vmList);
        double[][] taskExecutionTimes = calculateTaskExecutionTimes(cloudletList, vmList);

        double makespan = calculateMakespan(bestSolution, taskExecutionTimes, cloudletList, vmList);
        System.out.println();
        System.out.println("***********************************************************");
        double totalEnergyConsumption = calculateTotalEnergyConsumption(bestSolution, taskExecutionTimes, makespan, vmList);
        System.out.println("***********************************************************");
        System.out.println();
        double totalFlowTime = calculateFlowTime(bestSolution, taskExecutionTimes);
        double co2Emission = calculateCO2Emission(totalEnergyConsumption);
        System.out.println();
        System.out.println("Results for the best solution:");
        System.out.println();
        System.out.println("Best Solution: " + Arrays.toString(bestSolution));
        System.out.println("fitness: " + fitness );
        System.out.println("Makespan: " + makespan + " time units");
        System.out.println("Total Energy Consumption: " + totalEnergyConsumption + " energy units");
        System.out.println("Total Flow Time: " + totalFlowTime + " time units");
        System.out.println("CO2 Emissions: " + co2Emission + " emission units");
        System.out.println();
        System.out.println();
    }
}
