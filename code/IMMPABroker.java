package org.fog.test.perfeval;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Vm;

import java.util.*;

import static org.fog.test.perfeval.Common.*;
import static org.fog.test.perfeval.Constants.*;

public class IMMPABroker extends DatacenterBroker {
    private final List<int[]> predators; // List of predators (solutions)
    private final Random random;
    private final Map<int[], Double> predatorFitnessMap = new HashMap<>();
    private int[] bestSolution;
    private double bestFitness;

    private final Map<int[], Integer> predatorFailureCountMap = new HashMap<>();

    public IMMPABroker(String name) throws Exception {
        super(name);
        this.predators = new ArrayList<>();
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
        initializePredators();

        // Main loop of the IMMPA algorithm
        for (int iter = 0; iter < Constants.MAX_ITERATION; iter++) {
            // Evaluate fitness of each predator
            for (int[] predator : predators) {
                double fitness = evaluateFitness(predator, cloudletList, vmList);
                predatorFitnessMap.put(predator, fitness);
                if (fitness < bestFitness) {
                    bestFitness = fitness;
                    bestSolution = predator.clone();
                }
            }
            rankingBasedReinitializationAndMutation();


            if (iter % Constants.PIT == 0) {
                // Periodic Re-initialization
                reInitializePredators();
            }

            // Update predators based on IMMPA algorithm
            updatePredators(predators, iter);

            // Mutation towards the best solution
            mutateTowardsBest(predators, bestSolution, Constants.PR);
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

    private void initializePredators() {
        int numTasks = cloudletList.size();
        int numVMs = vmList.size();
        System.out.println("numVMs: "+ numVMs);

        for (int i = 0; i < PREDATORS_NO; i++) {
            int[] predator = new int[numTasks];
            for (int j = 0; j < numTasks; j++) {
                predator[j] = random.nextInt(numVMs); // Assign a random VM to each task
            }
            predators.add(predator);
            System.out.println("predator"+ i + " :  "+ Arrays.toString(predator));
            System.out.println("   - fitness:  "+ evaluateFitness(predator, cloudletList, vmList) );
            System.out.println();
        }
    }

    private void reInitializePredators() {
        // Re-initialization logic here
        int numToReinitialize = predators.size() / 2; // Reinitialize half of the population

        for (int i = 0; i < numToReinitialize; i++) {
            int[] predator = predators.get(i);
            for (int j = 0; j < predator.length; j++) {
                predator[j] = random.nextInt(vmList.size()); // Assign a random VM to each task
            }
            predators.set(i, predator); // Update the predator in the population
        }
    }

    private void mutateTowardsBest(List<int[]> predators, int[] bestSolution, double probability) {
        // Mutation logic here
        for (int[] predator : predators) {
            if (random.nextDouble() < probability) {
                // Mutate this predator towards the best solution
                for (int i = 0; i < predator.length; i++) {
                    // Simple mutation: move half the distance towards the best solution
                    predator[i] += (bestSolution[i] - predator[i]) / 2;
                    // Ensure the mutated position is within valid bounds
                    predator[i] = Math.max(0, Math.min(predator[i], vmList.size() - 1));
                }
            }
        }
    }


    protected void updatePredators(List<int[]> predators, int currentIteration) {
        double CF = calculateCF(currentIteration, Constants.MAX_ITERATION);

        for (int p = 0; p < predators.size(); p++) {
            int[] predator = predators.get(p); // Get the current predator
            int[] newPredatorPosition = predator.clone(); // Clone it for updating

            if (currentIteration < Constants.MAX_ITERATION / 3) {
                // Exploration phase
                for (int i = 0; i < predator.length; i++) {
                    newPredatorPosition[i] = performBrownianMotion(predator, i);
                }
            } else if (currentIteration < 2 * Constants.MAX_ITERATION / 3) {
                // Balancing phase
                for (int i = 0; i < predator.length; i++) {
                    if (i < predator.length / 2) {
                        newPredatorPosition[i] = performBrownianMotion(predator, i);
                    } else {
                        newPredatorPosition[i] = performLevyFlight(predator, i);
                    }
                }
            } else {
                // Exploitation phase
                for (int i = 0; i < predator.length; i++) {
                    newPredatorPosition[i] = performLevyFlight(predator, i);
                }
            }

            // Apply FADS logic for each predator
            for (int i = 0; i < newPredatorPosition.length; i++) {
                applyFADS(newPredatorPosition, i, CF , predators, vmList);
            }

            // Update the predator in the list with its new position
            predators.set(p, newPredatorPosition);
        }
    }


    private void rankingBasedReinitializationAndMutation() {
        for (int[] predator : predators) {
            double currentFitness = predatorFitnessMap.get(predator);
            double previousBestFitness = bestFitness;  // This should be updated to store the best fitness of each predator individually
            int failureCount = predatorFailureCountMap.getOrDefault(predator, 0);

            if (currentFitness > previousBestFitness) {
                failureCount++;
                predatorFailureCountMap.put(predator, failureCount);
            } else {
                failureCount = 0;
                predatorFailureCountMap.put(predator, failureCount);
            }

            if (failureCount > FI) {
                double r = random.nextDouble();
                if (r < PR) {
                    // Reinitialize the predator randomly
                    for (int j = 0; j < predator.length; j++) {
                        predator[j] = random.nextInt(vmList.size());
                    }
                } else {
                    // Mutate the predator towards the best solution
                    for (int j = 0; j < predator.length; j++) {
                        predator[j] += (bestSolution[j] - predator[j]) / 2;
                        predator[j] = clamp(predator[j]);  // Ensure the value is within the valid range
                    }
                }
            }
        }
    }



    private int performBrownianMotion(int[] predator, int index) {
        double stepSize = 0.1; // Step size, adjust as needed
        double gaussian = random.nextGaussian(); // Generate a Gaussian random value
        int newPosition = (int) (predator[index] + stepSize * gaussian); // New position based on Brownian motion
        return clamp(newPosition);
    }

    private int performLevyFlight(int[] predator, int index) {
        double beta = 1.5;
        double sigma = Math.pow((gamma(1 + beta) * Math.sin(Math.PI * beta / 2)) / (gamma((1 + beta) / 2) * beta * Math.pow(2, (beta - 1) / 2)), 1 / beta);
        double u = random.nextGaussian() * sigma;
        double v = random.nextGaussian();
        double step = u / Math.pow(Math.abs(v), 1 / beta);

        int newPosition = (int) (predator[index] + step);
        return clamp(newPosition);
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(Objects.equals(getName(), "MPA_Broker2") ? NO_OF_VMS2-1 :NO_OF_VMS-1, value));
    }




}
