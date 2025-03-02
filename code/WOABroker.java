package org.fog.test.perfeval;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Vm;

import java.util.*;

import static org.fog.test.perfeval.Constants.*;
import static org.fog.test.perfeval.Common.*;

public class WOABroker extends DatacenterBroker {
    private final List<int[]> whales; // List of whales (solutions)
    private final Random random;
    private final Map<int[], Double> whaleFitnessMap = new HashMap<>();
    private int[] bestSolution;
    private double bestFitness;

    public WOABroker(String name) throws Exception {
        super(name);
        this.whales = new ArrayList<>();
        this.random = new Random();
        this.bestFitness = Double.MAX_VALUE;
        this.bestSolution = null;
    }

    @Override
    public void submitCloudletList(List<? extends Cloudlet> list) {
        super.submitCloudletList(list);
    }

    @Override
    public void submitVmList(List<? extends Vm> list) {
        super.submitVmList(list);
    }

    protected void submitCloudlets() {
        initializeWhales();

        // Main loop of the WOA algorithm
        for (int iter = 0; iter < MAX_ITERATION; iter++) {
            // Evaluate fitness of each whale
            for (int[] whale : whales) {
                double fitness = evaluateFitness(whale, cloudletList, vmList);
                whaleFitnessMap.put(whale, fitness);
                if (fitness < bestFitness) {
                    bestFitness = fitness;
                    bestSolution = whale.clone();
                }
            }

            // Update whales based on WOA algorithm
            updateWhales(whales, iter);

            // Optional: check for convergence or stopping criteria
        }

        // After the loop, submit cloudlets based on the best solution found
        if (bestSolution != null) {
            submitCloudletsBasedOnSolution(bestSolution);
        }
    }

    private void submitCloudletsBasedOnSolution(int[] solution) {
        // Map each cloudlet to a VM based on the solution and submit them
        for (int i = 0; i < cloudletList.size(); i++) {
            int vmId = solution[i];
            bindCloudletToVm(cloudletList.get(i).getCloudletId(), vmId);
        }
        reportResults(solution, cloudletList, vmList);
        super.submitCloudlets();
    }

    private void initializeWhales() {
        int numTasks = cloudletList.size();
        int numVMs = vmList.size();

        for (int i = 0; i < PREDATORS_NO; i++) {
            int[] whale = new int[numTasks];
            for (int j = 0; j < numTasks; j++) {
                whale[j] = random.nextInt(numVMs); // Assign a random VM to each task
            }
            whales.add(whale);
        }
    }

    protected void updateWhales(List<int[]> whales, int currentIteration) {
        double a = 2.0 - currentIteration * (2.0 / MAX_ITERATION); // Decreases linearly from 2 to 1
        double a2 = -1 + currentIteration * ((-1) / MAX_ITERATION); // Decreases from -1 to -2

        for (int[] whale : whales) {
            double r1 = random.nextDouble(); // Random number in [0,1)
            double r2 = random.nextDouble(); // Random number in [0,1)
            double A = 2 * a * r1 - a; // Equation (2.3) in the paper
            double C = 2 * r2; // Equation (2.4) in the paper
            double b = 1; // Defines shape of the spiral
            double l = (a2 - 1) * random.nextDouble() + 1; // Equation (2.5) in the paper

            for (int i = 0; i < whale.length; i++) {
                double p = random.nextDouble();
                if (p < 0.5) {
                    if (Math.abs(A) < 1) {
                        int randomWhaleIndex = random.nextInt(whales.size());
                        int[] randomWhale = whales.get(randomWhaleIndex);
                        whale[i] = updatePosition(whale, randomWhale, A, C, i);
                    } else {
                        whale[i] = updatePosition(whale, bestSolution, A, C, i);
                    }
                } else {
                    whale[i] = updateSpiralPosition(whale, bestSolution, b, l, i);
                }
                whale[i] = Math.max(0, Math.min(whale[i], vmList.size() - 1)); // Ensure the position is within bounds
            }
        }
    }

    private int updatePosition(int[] whale, int[] referenceWhale, double A, double C, int index) {
        // Simulates the encircling prey behavior or exploration
        return referenceWhale[index] - (int) (A * Math.abs(C * referenceWhale[index] - whale[index]));
    }

    private int updateSpiralPosition(int[] whale, int[] referenceWhale, double b, double l, int index) {
        // Simulates the spiral-shaped path towards the prey
        double distance = Math.abs(referenceWhale[index] - whale[index]);
        return (int) (distance * Math.exp(b * l) * Math.cos(l * 2 * Math.PI) + referenceWhale[index]);
    }

}
