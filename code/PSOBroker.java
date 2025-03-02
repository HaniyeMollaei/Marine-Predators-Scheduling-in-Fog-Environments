package org.fog.test.perfeval;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Vm;

import java.util.*;


import static org.fog.test.perfeval.Common.*;
import static org.fog.test.perfeval.Constants.*;

public class PSOBroker extends DatacenterBroker {
    private final List<int[]> particles; // List of particles (solutions)
    private final List<int[]> pBest; // Personal best solutions
    private int[] gBest; // Global best solution
    private final List<int[]> velocity; // Velocity of particles
    private final Random random;

    public PSOBroker(String name) throws Exception {
        super(name);
        this.particles = new ArrayList<>();
        this.pBest = new ArrayList<>();
        this.gBest = null;
        this.velocity = new ArrayList<>();
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
        initializeParticles();

        double gBestFitness = Double.MAX_VALUE;

        // Main loop of the PSO algorithm
        for (int iter = 0; iter < MAX_ITERATION; iter++) {
            for (int i = 0; i < particles.size(); i++) {
                int[] particle = particles.get(i);
                double fitness = evaluateFitness(particle, cloudletList, vmList);

                // Update personal best
                if (fitness < evaluateFitness(pBest.get(i), cloudletList, vmList)) {
                    pBest.set(i, particle.clone());
                }

                // Update global best
                if (fitness < gBestFitness) {
                    gBestFitness = fitness;
                    gBest = particle.clone();
                }
            }

            // Update velocity and position of particles
            updateParticles(iter);

            // Optional: check for convergence or stopping criteria
        }

        // After the loop, submit cloudlets based on the best solution found
        if (gBest != null) {
            submitCloudletsBasedOnSolution(gBest);
        }
    }

    private void initializeParticles() {
        int numTasks = cloudletList.size();
        int numVMs = vmList.size();

        for (int i = 0; i < PREDATORS_NO; i++) {
            int[] particle = new int[numTasks];
            int[] vel = new int[numTasks];

            for (int j = 0; j < numTasks; j++) {
                particle[j] = random.nextInt(numVMs); // Assign a random VM to each task
                vel[j] = 0; // Initial velocity is 0
            }

            particles.add(particle);
            velocity.add(vel);
            pBest.add(particle.clone());
        }

        gBest = particles.get(0).clone();
    }

    private void updateParticles(int iter) {
        double w = 0.5; // Inertia weight
        double c1 = 1.0; // Cognitive (particle's own experience)
        double c2 = 2.0; // Social (other particles' experiences)

        for (int i = 0; i < particles.size(); i++) {
            int[] particle = particles.get(i);
            int[] vel = velocity.get(i);
            int[] pBestParticle = pBest.get(i);

            for (int j = 0; j < particle.length; j++) {
                // Update velocity
                double r1 = random.nextDouble();
                double r2 = random.nextDouble();
                vel[j] = (int) (w * vel[j] + c1 * r1 * (pBestParticle[j] - particle[j]) + c2 * r2 * (gBest[j] - particle[j]));

                // Update position
                particle[j] += vel[j];

                // Clamp position within bounds
                if (particle[j] < 0) {
                    particle[j] = 0;
                } else if (particle[j] >= vmList.size()) {
                    particle[j] = vmList.size() - 1;
                }
            }
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




}
