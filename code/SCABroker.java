package org.fog.test.perfeval;

import org.cloudbus.cloudsim.DatacenterBroker;

import java.util.*;
import static org.fog.test.perfeval.Common.*;

public class SCABroker extends DatacenterBroker {
    private final List<int[]> agents; // List of agents (solutions)
    private final Random random;
    private final Map<int[], Double> agentFitnessMap = new HashMap<>();
    private int[] bestSolution;
    private double bestFitness;

    public SCABroker(String name) throws Exception {
        super(name);
        this.agents = new ArrayList<>();
        this.random = new Random();
        this.bestFitness = Double.MAX_VALUE;
        this.bestSolution = null;
    }

    @Override
    protected void submitCloudlets() {
        initializeAgents();

        // Main loop of the SCA algorithm
        for (int iter = 0; iter < Constants.MAX_ITERATION; iter++) {
            // Update the position of search agents using Sine and Cosine functions
            updateAgents(iter);

            // Evaluate fitness of each agent
            for (int[] agent : agents) {
                double fitness = evaluateFitness(agent, cloudletList, vmList);
                agentFitnessMap.put(agent, fitness);
                if (fitness < bestFitness) {
                    bestFitness = fitness;
                    bestSolution = agent.clone();
                }
            }
        }

        // After the loop, submit cloudlets based on the best solution found
        if (bestSolution != null) {
            submitCloudletsBasedOnSolution(bestSolution);
        }
    }

    private void initializeAgents() {
        int numTasks = cloudletList.size();
        int numVMs = vmList.size();

        for (int i = 0; i < Constants.PREDATORS_NO; i++) {
            int[] agent = new int[numTasks];
            for (int j = 0; j < numTasks; j++) {
                agent[j] = random.nextInt(numVMs); // Assign a random VM to each task
            }
            agents.add(agent);
        }
    }

    private void updateAgents(int currentIteration) {
        double a = 2;
        double r1 = a - currentIteration * ((a) / Constants.MAX_ITERATION);  // r1 decreases linearly from a to 0

        for (int[] agent : agents) {
            for (int j = 0; j < agent.length; j++) {
                double r2 = (2 * Math.PI) * random.nextDouble();
                double r3 = 2 * random.nextDouble();
                double r4 = random.nextDouble();

                if (r4 < 0.5) {
                    agent[j] = agent[j] + (int)(r1 * Math.sin(r2) * Math.abs(r3 * bestSolution[j] - agent[j]));
                } else {
                    agent[j] = agent[j] + (int)(r1 * Math.cos(r2) * Math.abs(r3 * bestSolution[j] - agent[j]));
                }
                agent[j] = clamp(agent[j], 0, vmList.size() - 1);  // Ensure the agent position is within valid bounds
            }
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void submitCloudletsBasedOnSolution(int[] solution) {
        for (int i = 0; i < cloudletList.size(); i++) {
            int vmId = solution[i];
            bindCloudletToVm(cloudletList.get(i).getCloudletId(), vmId);
        }
        super.submitCloudlets();
    }


}
