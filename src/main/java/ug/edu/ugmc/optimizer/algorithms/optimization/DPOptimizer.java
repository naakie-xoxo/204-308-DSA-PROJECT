package ug.edu.ugmc.optimizer.algorithms.optimization;

/**
 * Dynamic-programming optimization for selecting indivisible UGMC service
 * requests under a fixed resource-capacity constraint.
 */
public final class DPOptimizer {

    private DPOptimizer() {
    }

    /**
     * Computes the maximum value obtainable with 0/1 Knapsack.
     *
     * <p>This compatibility method keeps the original integer-returning API
     * used by the console UI and empirical comparisons. Call
     * {@link #solveKnapsack(int[], int[], int)} when the selected requests are
     * also required for a trace table or live demonstration.</p>
     *
     * @param weights positive resource costs for the requests
     * @param values non-negative urgency or service values for the requests
     * @param capacity available resource capacity
     * @return the maximum achievable value
     * @throws IllegalArgumentException if an input contract is violated
     */
    public static int knapsack(int[] weights, int[] values, int capacity) {
        return solveKnapsack(weights, values, capacity).getMaximumValue();
    }

    /**
     * Solves 0/1 Knapsack and reconstructs the exact selected requests.
     *
     * <p>The DP table has {@code weights.length + 1} rows and
     * {@code capacity + 1} columns. Its size is based on the requested
     * capacity; no hidden student-index cap changes the problem being solved.</p>
     *
     * @param weights positive resource costs for the requests
     * @param values non-negative urgency or service values for the requests
     * @param capacity available resource capacity, which must not be negative
     * @return an immutable result containing value, weight, and chosen indices
     * @throws IllegalArgumentException if arrays are null, have different
     *                                  lengths, contain invalid entries, or the
     *                                  capacity is negative
     */
    public static KnapsackResult solveKnapsack(int[] weights, int[] values, int capacity) {
        validateInput(weights, values, capacity);

        int itemCount = weights.length;
        if (itemCount == 0 || capacity == 0) {
            return new KnapsackResult(0, 0, new int[0]);
        }

        int[][] table = new int[itemCount + 1][capacity + 1];

        for (int item = 1; item <= itemCount; item++) {
            int itemWeight = weights[item - 1];
            int itemValue = values[item - 1];

            for (int currentCapacity = 0;
                    currentCapacity <= capacity;
                    currentCapacity++) {
                int excludeValue = table[item - 1][currentCapacity];
                if (itemWeight > currentCapacity) {
                    table[item][currentCapacity] = excludeValue;
                } else {
                    int includeValue = itemValue
                            + table[item - 1][currentCapacity - itemWeight];
                    table[item][currentCapacity] = Math.max(includeValue, excludeValue);
                }
            }
        }

        int selectedCount = countSelectedItems(table, weights, capacity);
        int[] selectedIndices = new int[selectedCount];
        int selectedPosition = selectedCount - 1;
        int remainingCapacity = capacity;
        int totalWeight = 0;

        for (int item = itemCount; item > 0; item--) {
            if (table[item][remainingCapacity] != table[item - 1][remainingCapacity]) {
                int selectedIndex = item - 1;
                selectedIndices[selectedPosition--] = selectedIndex;
                totalWeight += weights[selectedIndex];
                remainingCapacity -= weights[selectedIndex];
            }
        }

        return new KnapsackResult(
                table[itemCount][capacity], totalWeight, selectedIndices);
    }

    private static int countSelectedItems(int[][] table, int[] weights, int capacity) {
        int count = 0;
        int remainingCapacity = capacity;

        for (int item = weights.length; item > 0; item--) {
            if (table[item][remainingCapacity] != table[item - 1][remainingCapacity]) {
                count++;
                remainingCapacity -= weights[item - 1];
            }
        }
        return count;
    }

    private static void validateInput(int[] weights, int[] values, int capacity) {
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
    }

    /** Immutable 0/1 Knapsack outcome suitable for UI and trace-table output. */
    public static final class KnapsackResult {
        private final int maximumValue;
        private final int totalWeight;
        private final int[] selectedIndices;

        private KnapsackResult(int maximumValue, int totalWeight, int[] selectedIndices) {
            this.maximumValue = maximumValue;
            this.totalWeight = totalWeight;
            this.selectedIndices = copy(selectedIndices);
        }

        public int getMaximumValue() {
            return maximumValue;
        }

        public int getTotalWeight() {
            return totalWeight;
        }

        /**
         * Returns zero-based indices into the supplied weights and values arrays.
         */
        public int[] getSelectedIndices() {
            return copy(selectedIndices);
        }

        /**
         * Returns one-based request labels used in UGMC traces, such as R2.
         */
        public String[] getSelectedRequestLabels() {
            String[] labels = new String[selectedIndices.length];
            for (int index = 0; index < selectedIndices.length; index++) {
                labels[index] = "R" + (selectedIndices[index] + 1);
            }
            return labels;
        }

        private static int[] copy(int[] source) {
            int[] result = new int[source.length];
            for (int index = 0; index < source.length; index++) {
                result[index] = source[index];
            }
            return result;
        }
    }
}
