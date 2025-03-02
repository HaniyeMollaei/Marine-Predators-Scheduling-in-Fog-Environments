package org.fog.test.perfeval;

public class Constants {
    public static final int NO_OF_VMS = 60; // number of Cloudlets;
    public static final int NO_OF_VMS2 = 12; // number of Cloudlets;
    public static final int[] CLOUDLET_LENGTH1 = {100, 200, 300, 400, 500, 600, 700, 800, 900, 1000, 1200, 1500};
    public static final int[] CLOUDLET_LENGTH2 = {600};
    public static final int MAX_ITERATION = 1500; // Number of Particles.
    public static final int PREDATORS_NO = 10; // Number of Particles.
    public static final double[] EMISSION_FACTORS = {0.5825, 0.7476, 0.4435, 0.0}; // oil, coal, natural gas, non-fossil
    public static final double CARBON_TO_CO2_RATIO = 44.0 / 12.0;
    public static final double[] shareOfEnergySources = {0.25, 0.25, 0.25, 0.25}; // Example distribution
    public static final double ALPHA = 0.8; // Weight for energy consumption
    public static final int FI = 60;
    public static final int PR = 300;
    public static final int PIT = 800;


}
