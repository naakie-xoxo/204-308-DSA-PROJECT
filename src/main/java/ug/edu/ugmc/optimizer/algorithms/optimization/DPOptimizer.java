package ug.edu.ugmc.optimizer.algorithms.optimization;

/**
 * Handles dynamic programming optimizations for the UGMC Optimizer.
 * Engineered to maximize resource allocation under strict hospital constraints.
 */
public class DPOptimizer {

    // Universal Parameter assigned to Naakie
    private static final int BASE_INDEX = 22384696;

    /**
     * Purpose: 0/1 Knapsack algorithm to maximize emergency response value within a strict capacity.
     * Hospital Use Case: Selecting the optimal combination of medical supplies or service requests 
     *                    to load into an ambulance without exceeding its payload limit.
     * 
     * @param weights Array of resource weights/costs (e.g., equipment weight, time required)
     * @param values Array of urgency/life-saving values associated with each request
     * @param capacity The maximum weight/budget the ambulance or team can carry
     * @return The maximum value achievable
     * 
     * Complexity: 
     * Time: O(N * W) where N is the number of items and W is the capacity.
     * Space: O(N * W) to store the tabulation matrix in memory.
     */
    public static int knapsack(int[] weights, int[] values, int capacity) {
        // Edge Case 1: Null or empty input arrays
        if (weights == null || values == null || weights.length == 0 || values.length == 0) {
            return 0;
        }

        // Edge Case 2: Zero or negative capacity (satisfies testDPZeroCapacity)
        if (capacity <= 0) {
            return 0;
        }

        // Parameter Usage: Enforce the strict maximum budget constraint derived from index
        // Prevents the system from processing requests that exceed the hospital's hard physical limits
        int maxBudget = (BASE_INDEX % 500) + 100; 
        int effectiveCapacity = Math.min(capacity, maxBudget);

        int n = weights.length;
        
        // Internal Representation: 2D array where rows represent items and columns represent capacity
        int[][] dp = new int[n + 1][effectiveCapacity + 1];

        // Bottom-up Tabulation Matrix Construction
        for (int i = 1; i <= n; i++) {
            for (int w = 1; w <= effectiveCapacity; w++) {
                
                // If the current item's weight fits within the current capacity column 'w'
                if (weights[i - 1] <= w) {
                    // Evaluate the Choice: Include the item OR Exclude the item
                    dp[i][w] = Math.max(
                        values[i - 1] + dp[i - 1][w - weights[i - 1]], // Include
                        dp[i - 1][w]                                   // Exclude
                    );
                } else {
                    // Overweight: The item is too heavy for the current capacity 'w', must exclude
                    dp[i][w] = dp[i - 1][w];
                }
            }
        }

        // The optimal global maximum mathematically settles in the bottom-right corner
        return dp[n][effectiveCapacity];
    }
}