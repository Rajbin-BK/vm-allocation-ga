
import java.util.Arrays;
import java.util.Random;


public class VMAllocationGA {

    private static final int VM_COUNT = 7; //no. of VMS
    private static final int CSP_COUNT = 8; //number of CSP
    private static final int GENERATIONS = 100;
    private static final int CHROMOSOME_LENGTH = 3; //representing the 3 bits for the CSP
    private static final double MUTATION_RATE = 0.1;
    private static final Random random = new Random();

    private static final double[] CSP_COST =        {5   ,6  , 5.5  , 6.2  , 4.8   , 6.5  , 7,    4.6};
    private static final double[] CSP_RELIABILITY = {0.93 ,0.96, 0.95, 0.97, 0.91, 0.98, 0.99, 0.89};
    private static final double[] CSP_LATENCY =     {0.60, 0.53, 0.57, 0.50, 0.62, 0.46, 0.42, 0.68};

    private static final double WEIGHT_COST = 0.3;
    private static final double WEIGHT_RELIABILITY = 0.05;
    private static final double WEIGHT_LATENCY = 0.65;

    public static void main(String[] args) {
        double[] normalizedCSPCost = normalizeCosts(CSP_COST);
        String[] population = initializePopulation(VM_COUNT);
    
        for (int generation = 0; generation < GENERATIONS; generation++) {
            // Removed the printing inside the loop to only print after the final generation
            double[] adjustedLatency = adjustLatencyBasedOnAllocations(population, CSP_LATENCY);
            double[] fitnessScores = calculatePopulationFitness(population, normalizedCSPCost, adjustedLatency);
    
            for (int i = 0; i < VM_COUNT; i += 2) { // Process in pairs
                String[] parents = selectParents(population, fitnessScores);
                String[] offspring = crossover(parents[0], parents[1]);
                mutate(offspring, MUTATION_RATE);
    
                double[] offspringFitness = calculatePopulationFitness(offspring, normalizedCSPCost, adjustedLatency);
                double parentsFitnessSum = fitnessScores[i] + fitnessScores[Math.min(i + 1, VM_COUNT - 1)];
                double offspringFitnessSum = offspringFitness[0] + offspringFitness[1];
    
                if (offspringFitnessSum < parentsFitnessSum) {
                    population[i] = offspring[0];
                    if (i + 1 < VM_COUNT) {
                        population[i + 1] = offspring[1];
                    }
                }
            }
        }
    
        // After all generations, process and print the final allocation details
        double totalCost = 0;
        double totalLatency = 0;
        double totalfailureRate=1;
        


        System.out.println("\nAfter the Final Generation:\n");
        for (int i = 0; i < VM_COUNT; i++) {
            int cspIndex = binaryToDecimal(population[i]);
            double cost = CSP_COST[cspIndex]; // Original cost, not normalized
            double latency = CSP_LATENCY[cspIndex]; // Original latency
            double reliability = CSP_RELIABILITY[cspIndex];
            double failureRate = 1.0 -reliability;      //inverse of the reliabilities
            totalCost += cost;
            totalLatency += latency;
            totalfailureRate *=failureRate;

            // Calculate and print details for each VM, including fitness
            double fitness = calculateFitness(population[i], normalizedCSPCost, adjustLatencyBasedOnAllocations(population, CSP_LATENCY));


            System.out.println("VM " + (i + 1) + " allocated to CSP " + (cspIndex + 1) +
                    ", Fitness: " + fitness +
                    ", Cost: " + cost +
                    ", Latency: " + latency +
                    ", Reliability: " + reliability );
        }
        double totalRelaibilites= 1-totalfailureRate;
        // Calculate the overall fitness of the final population
        double[] finalFitnessScores = calculatePopulationFitness(population, normalizedCSPCost, adjustLatencyBasedOnAllocations(population, CSP_LATENCY));
        double overallFitness = Arrays.stream(finalFitnessScores).average().orElse(Double.NaN);
            
        System.out.println("\nOverall Average Fitness: " + overallFitness +
                    "\nTotal Cost: " + totalCost + 
                    ",\nTotal Latency: " + totalLatency + 
                    " \nTotal Reliabilites: " + totalRelaibilites +".\n" );
          
    }

    private static String[] initializePopulation(int count) {
        String[] population = new String[count];
        for (int i = 0; i < count; i++) {
            population[i] = generateRandomAllocation();
        }
        return population;
    }
    
    private static double[] normalizeCosts(double[] costs) {
        double maxCost = Arrays.stream(costs).max().getAsDouble();
        return Arrays.stream(costs).map(cost -> cost / maxCost).toArray();
    }
    
    private static String generateRandomAllocation() {
        int cspIndex = random.nextInt(CSP_COUNT);
        return String.format("%" + CHROMOSOME_LENGTH + "s", Integer.toBinaryString(cspIndex)).replace(' ', '0');
    }
    
    private static double[] adjustLatencyBasedOnAllocations(String[] population, double[] baseLatency) {
        double[] adjustedLatency = Arrays.copyOf(baseLatency, baseLatency.length);
        int[] vmCounts = new int[CSP_COUNT];
        for (String alloc : population) {
            int index = binaryToDecimal(alloc);
            vmCounts[index]++;
        }
        for (int i = 0; i < CSP_COUNT; i++) {
            adjustedLatency[i] += (vmCounts[i] - 1) * 0.1; // Increase latency for each additional VM
        }
        return adjustedLatency;
    }
    
    private static double[] calculatePopulationFitness(String[] population, double[] normalizedCosts, double[] adjustedLatency) {
        return Arrays.stream(population)
                     .mapToDouble(chromosome -> calculateFitness(chromosome, normalizedCosts, adjustedLatency))
                     .toArray();
    }
    
    private static String[] selectParents(String[] population, double[] fitnessScores) {
        String[] parents = new String[2];
        double totalFitness = Arrays.stream(fitnessScores).sum();
        double[] cumulativeProbabilities = new double[VM_COUNT];
        cumulativeProbabilities[0] = fitnessScores[0] / totalFitness;
        for (int i = 1; i < VM_COUNT; i++) {
            cumulativeProbabilities[i] = cumulativeProbabilities[i - 1] + (fitnessScores[i] / totalFitness);
        }
        for (int i = 0; i < 2; i++) {
            double randomValue = random.nextDouble();
            for (int j = 0; j < VM_COUNT; j++) {
                if (randomValue <= cumulativeProbabilities[j]) {
                    parents[i] = population[j];
                    break;
                }
            }
        }
        return parents;
    }
    
    
    private static String[] crossover(String parent1, String parent2) {
        int crossoverPoint = random.nextInt(CHROMOSOME_LENGTH - 1) + 1; // Avoiding crossover at the ends
        String offspring1 = parent1.substring(0, crossoverPoint) + parent2.substring(crossoverPoint);
        String offspring2 = parent2.substring(0, crossoverPoint) + parent1.substring(crossoverPoint);
        return new String[]{offspring1, offspring2};
    }
    
    
    private static void mutate(String[] offspring, double rate) {
        for (int i = 0; i < offspring.length; i++) {
            char[] chars = offspring[i].toCharArray();
            for (int j = 0; j < chars.length; j++) {
                if (random.nextDouble() < rate) {
                    chars[j] = chars[j] == '0' ? '1' : '0'; // Flipping the bit
                }
            }
            offspring[i] = new String(chars);
        }
    }
    
    
    private static double calculateFitness(String chromosome, double[] normalizedCosts, double[] adjustedLatency) {
        int cspIndex = binaryToDecimal(chromosome);
        double cost = normalizedCosts[cspIndex];
        double reliability = CSP_RELIABILITY[cspIndex];
        double latency = adjustedLatency[cspIndex];
        // Fitness function considering cost, reliability, and latency.
        // Lower fitness values are better. Adjust formula as needed.
        return WEIGHT_COST * cost + WEIGHT_RELIABILITY * (1 - reliability) + WEIGHT_LATENCY * latency;
    }
    
    
    private static int binaryToDecimal(String binary) {
        return Integer.parseInt(binary, 2);
    }
    
}
