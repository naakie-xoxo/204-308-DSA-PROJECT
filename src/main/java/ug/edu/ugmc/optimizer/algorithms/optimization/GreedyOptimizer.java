package ug.edu.ugmc.optimizer.algorithms.optimization;

/**
----
 * The Greedy algorithm attempts to find the optimal solution by sorting 
 * hospital requests by their Value-to-Weight ratio (highest first).
 * 
 * Mathematical Counterexample (Local Maxima vs Global Maxima):
 * Consider an ambulance with Capacity = 50.
 * Request 1: Weight = 10, Value = 60  (Ratio = 6.0)
 * Request 2: Weight = 20, Value = 100 (Ratio = 5.0)
 * Request 3: Weight = 30, Value = 120 (Ratio = 4.0)
 * 
 * 1. Greedy Execution: Takes Req 1 (Highest ratio). Remaining capacity = 40.
 * 2. Greedy Execution: Takes Req 2. Remaining capacity = 20.
 * 3. Greedy Execution: Rejects Req 3 (Weight 30 > 20).
 * GREEDY TOTAL VALUE = 160.
 * 
 * OPTIMAL DP EXECUTION: Ignores Req 1. Takes Req 2 + Req 3.
 * OPTIMAL TOTAL VALUE = 220 (Exactly fits Capacity 50).
 * 
 * Conclusion: Because hospital requests cannot be fractionally divided (0/1 constraint), 
 * the Greedy choice traps the system in a local maximum, leaving wasted capacity.
 */
public class GreedyOptimizer {

    /**
     * Executes the Greedy heuristic for resource assignment.
     * Matches the DPOptimizer signature to allow direct benchmarking in the Empirical Lab.
     * 
     * @param weights Array of resource weights/costs
     * @param values Array of urgency/life-saving values
     * @param capacity Maximum weight/budget available
     * @return The sub-optimal total value achieved by the greedy heuristic
     */
    public static int greedyKnapsack(int[] weights, int[] values, int capacity) {
        if (weights == null || values == null || weights.length == 0 || values.length == 0) {
            return 0;
        }

        int n = weights.length;
        int[] indices = new int[n];
        double[] ratios = new double[n];

        // Step 1: Calculate Value-to-Weight ratios
        for (int i = 0; i < n; i++) {
            indices[i] = i;
            // Prevent division by zero if a weight is 0
            ratios[i] = weights[i] == 0 ? Double.MAX_VALUE : (double) values[i] / weights[i];
        }

        // Step 2: Sort indices by ratio in descending order (Highest ratio first)
        // Using an inline Selection Sort to strictly avoid java.util.Arrays/Collections
        for (int i = 0; i < n - 1; i++) {
            int maxIdx = i;
            for (int j = i + 1; j < n; j++) {
                if (ratios[indices[j]] > ratios[indices[maxIdx]]) {
                    maxIdx = j;
                }
            }
            // Swap indices
            int tempIdx = indices[i];
            indices[i] = indices[maxIdx];
            indices[maxIdx] = tempIdx;
        }

        // Step 3: Iterate through the sorted ratios and pack the capacity
        int totalValue = 0;
        int currentWeight = 0;

        for (int i = 0; i < n; i++) {
            int itemIdx = indices[i];
            
            // If the item fits in the remaining capacity, take it entirely
            if (currentWeight + weights[itemIdx] <= capacity) {
                currentWeight += weights[itemIdx];
                totalValue += values[itemIdx];
            }
        }

        return totalValue;
    }
}