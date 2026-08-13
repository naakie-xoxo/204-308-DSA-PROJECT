package ug.edu.ugmc.optimizer.algorithms.optimization;

/**
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
public final class GreedyOptimizer {

    private GreedyOptimizer() {
    }

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
        if (weights == null || values == null) {
            throw new IllegalArgumentException("Weights and values cannot be null.");
        }
        if (weights.length != values.length) {
            throw new IllegalArgumentException(
                    "Weights and values must contain the same number of requests.");
        }
        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity cannot be negative.");
        }
        if (weights.length == 0 || capacity == 0) {
            return 0;
        }

        for (int index = 0; index < weights.length; index++) {
            if (weights[index] <= 0) {
                throw new IllegalArgumentException(
                        "Request weight must be positive at index " + index + ".");
            }
            if (values[index] < 0) {
                throw new IllegalArgumentException(
                        "Request value cannot be negative at index " + index + ".");
            }
        }

        int n = weights.length;
        int[] indices = new int[n];
        double[] ratios = new double[n];

        // Step 1: Calculate Value-to-Weight ratios
        for (int i = 0; i < n; i++) {
            indices[i] = i;
            ratios[i] = (double) values[i] / weights[i];
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

    /**
     * Greedy approach to Job Scheduling (Shortest Job First).
     * Hospital Use Case: Minimizing total patient waiting time by treating the fastest cases first.
     */
    public static int[] scheduleJobs(int[] processingTimes) {
        if (processingTimes == null) {
            throw new IllegalArgumentException("Processing times cannot be null.");
        }

        int[] schedule = copy(processingTimes);
        for (int index = 0; index < schedule.length; index++) {
            if (schedule[index] <= 0) {
                throw new IllegalArgumentException(
                        "Processing time must be positive at index " + index + ".");
            }
        }
        
        // Inline selection sort to arrange times in ascending order
        for (int i = 0; i < schedule.length - 1; i++) {
            int minIdx = i;
            for (int j = i + 1; j < schedule.length; j++) {
                if (schedule[j] < schedule[minIdx]) {
                    minIdx = j;
                }
            }
            int temp = schedule[minIdx];
            schedule[minIdx] = schedule[i];
            schedule[i] = temp;
        }
        
        return schedule;
    }

    /**
     * Greedy approach to Coin Change.
     * Hospital Use Case: Pharmacy billing returning the minimum number of coins/bills.
     */
    public static int coinChange(int[] coins, int amount) {
        if (coins == null) {
            throw new IllegalArgumentException("Coin denominations cannot be null.");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative.");
        }
        if (amount == 0) {
            return 0;
        }
        if (coins.length == 0) {
            return -1;
        }

        int[] denominations = copy(coins);
        for (int index = 0; index < denominations.length; index++) {
            if (denominations[index] <= 0) {
                throw new IllegalArgumentException(
                        "Coin denomination must be positive at index " + index + ".");
            }
        }
        
        // Sort descending (assuming coins might be unsorted)
        for (int i = 0; i < denominations.length - 1; i++) {
            int maxIdx = i;
            for (int j = i + 1; j < denominations.length; j++) {
                if (denominations[j] > denominations[maxIdx]) {
                    maxIdx = j;
                }
            }
            int temp = denominations[maxIdx];
            denominations[maxIdx] = denominations[i];
            denominations[i] = temp;
        }
        
        int coinCount = 0;
        for (int coin : denominations) {
            if (amount == 0) break;
            if (coin <= amount) {
                coinCount += (amount / coin);
                amount = amount % coin;
            }
        }
        
        // If amount is not 0, it means exact change couldn't be made
        return amount == 0 ? coinCount : -1;
    }

    private static int[] copy(int[] source) {
        int[] result = new int[source.length];
        for (int index = 0; index < source.length; index++) {
            result[index] = source[index];
        }
        return result;
    }
}
