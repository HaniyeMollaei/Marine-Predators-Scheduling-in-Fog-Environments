package org.fog.test.perfeval;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Vm;

import java.util.*;

import static org.fog.test.perfeval.Common.*;
import static org.fog.test.perfeval.Constants.*;

public class MPABroker extends DatacenterBroker {
    private final List<int[]> predators; // List of predators (solutions)
    private final Random random;

    private final Map<int[], Double> predatorFitnessMap = new HashMap<>();


    public MPABroker(String name) throws Exception {
        super(name);
        this.predators = new ArrayList<>();
        this.random = new Random();

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

        double bestFitness = Double.MAX_VALUE; // For minimization problem; use Double.MIN_VALUE for maximization
        int[] bestSolution = null;

        // Main loop of the MPA algorithm
        for (int iter = 0; iter < MAX_ITERATION; iter++) {
            // Evaluate fitness of each predator
            for (int[] predator : predators) {
                double fitness = evaluateFitness(predator, cloudletList, vmList);
                predatorFitnessMap.put(predator, fitness);
                // Check if the current solution is better than the best known solution
                if (fitness  < bestFitness) { // Use > for maximization
                    bestFitness = fitness;
                    bestSolution = predator.clone(); // Store the best solution
                }
            }

            // Update predators based on MPA algorithm
            updatePredators(predators , iter);

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
        reportResults(solution , cloudletList, vmList);
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
            System.out.println("   - fitness:  "+ evaluateFitness(predator, cloudletList , vmList) );
            System.out.println();
        }
    }

    /*
    *  private void initializePredators() {
        int numTasks = cloudletList.size();
        int numVMs = vmList.size();
        System.out.println("numVMs: " + numVMs);

        for (int i = 0; i < PREDATORS_NO; i++) {
            int[] predator = new int[numTasks];
            for (int j = 0; j < numTasks; j++) {
                double rand = random.nextDouble(); // Generate a random number between 0 and 1
                int XL = 0; // Assuming 0 as the minimum VM index
                int XU = numVMs - 1; // Assuming numVMs - 1 as the maximum VM index
                predator[j] = XL + (int)(rand * (XU - XL)); // Assign a VM based on MPA initialization formula
            }
            predators.add(predator);
            System.out.println("predator" + i + " :  " + Arrays.toString(predator));
            System.out.println("   - fitness:  " + evaluateFitness(predator));
            System.out.println();
        }
    }
*/

    protected void updatePredators(List<int[]> predators, int currentIteration) {
        double CF = calculateCF(currentIteration, MAX_ITERATION);

        for (int[] predator : predators) {
            if (currentIteration < MAX_ITERATION / 3) {
                // Exploration phase
                for (int i = 0; i < predator.length; i++) {
                    predator[i] = performBrownianMotion(predator, i);
                }
            } else if (currentIteration < 2 * MAX_ITERATION / 3) {
                // Balancing phase
                for (int i = 0; i < predator.length; i++) {
                    if (i < predator.length / 2) {
                        predator[i] = performBrownianMotion(predator, i);
                    } else {
                        predator[i] = performLevyFlight(predator, i);
                    }
                }
            } else {
                // Exploitation phase
                for (int i = 0; i < predator.length; i++) {
                    predator[i] = performLevyFlight(predator, i);
                }
            }

            // Apply FADS logic for each predator
            for (int i = 0; i < predator.length; i++) {
                applyFADS(predator, i, CF);
            }
        }
    }

    private void applyFADS(int[] predator, int index, double CF) {
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
    public int clamp(int value) {
        return Math.max(0, Math.min(Objects.equals(getName(), "MPA_Broker2") ? NO_OF_VMS2-1 :NO_OF_VMS-1, value));
    }


}
