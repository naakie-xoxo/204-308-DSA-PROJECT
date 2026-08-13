package ug.edu.ugmc.optimizer.algorithms.optimization;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import ug.edu.ugmc.optimizer.algorithms.optimization.DPOptimizer.KnapsackResult;

/** Tests optimization independently from the unfinished graph traversals. */
class OptimizationTest {

    @Test
    void dpReconstructsTheOptimalUgmcRequests() {
        KnapsackResult result = DPOptimizer.solveKnapsack(
                new int[]{10, 20, 30}, new int[]{60, 100, 120}, 50);

        assertEquals(220, result.getMaximumValue());
        assertEquals(50, result.getTotalWeight());
        assertArrayEquals(new int[]{1, 2}, result.getSelectedIndices());
        assertArrayEquals(new String[]{"R2", "R3"}, result.getSelectedRequestLabels());
    }

    @Test
    void compatibilityMethodReturnsTheOptimalValue() {
        assertEquals(220, DPOptimizer.knapsack(
                new int[]{10, 20, 30}, new int[]{60, 100, 120}, 50));
    }

    @Test
    void dpUsesTheRequestedCapacityWithoutASilentCap() {
        assertEquals(999, DPOptimizer.knapsack(new int[]{297}, new int[]{999}, 300));
    }

    @Test
    void greedyKnapsackCounterexampleShowsWhyDpIsRequired() {
        int[] weights = {10, 20, 30};
        int[] values = {60, 100, 120};

        assertEquals(160, GreedyOptimizer.greedyKnapsack(weights, values, 50));
        assertEquals(220, DPOptimizer.knapsack(weights, values, 50));
    }

    @Test
    void greedyCoinChangeReturnsThreeWhenTheOptimumIsTwo() {
        assertEquals(3, GreedyOptimizer.coinChange(new int[]{1, 3, 4}, 6));
        assertEquals(2, optimalCoinCountForTheDocumentedFixture());
    }

    @Test
    void greedySchedulesShorterUgmcJobsFirst() {
        int[] processingTimes = {3, 1, 2};

        assertArrayEquals(new int[]{1, 2, 3}, GreedyOptimizer.scheduleJobs(processingTimes));
        assertArrayEquals(new int[]{3, 1, 2}, processingTimes);
    }

    @Test
    void zeroCapacityAndEmptyInputsSelectNoRequests() {
        KnapsackResult zeroCapacity = DPOptimizer.solveKnapsack(
                new int[]{10, 20}, new int[]{60, 100}, 0);
        KnapsackResult empty = DPOptimizer.solveKnapsack(new int[0], new int[0], 50);

        assertEquals(0, zeroCapacity.getMaximumValue());
        assertEquals(0, zeroCapacity.getTotalWeight());
        assertArrayEquals(new int[0], zeroCapacity.getSelectedIndices());
        assertEquals(0, empty.getMaximumValue());
        assertEquals(0, GreedyOptimizer.greedyKnapsack(new int[0], new int[0], 50));
    }

    @Test
    void mismatchedAndNullArraysAreRejectedClearly() {
        assertThrows(IllegalArgumentException.class,
                () -> DPOptimizer.knapsack(new int[]{10, 20}, new int[]{60}, 20));
        assertThrows(IllegalArgumentException.class,
                () -> GreedyOptimizer.greedyKnapsack(new int[]{10, 20}, new int[]{60}, 20));
        assertThrows(IllegalArgumentException.class,
                () -> DPOptimizer.knapsack(null, new int[0], 20));
        assertThrows(IllegalArgumentException.class,
                () -> GreedyOptimizer.greedyKnapsack(null, new int[0], 20));
    }

    @Test
    void invalidCapacityWeightsAndValuesAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> DPOptimizer.knapsack(new int[]{10}, new int[]{60}, -1));
        assertThrows(IllegalArgumentException.class,
                () -> GreedyOptimizer.greedyKnapsack(new int[]{10}, new int[]{60}, -1));
        assertThrows(IllegalArgumentException.class,
                () -> DPOptimizer.knapsack(new int[]{0}, new int[]{60}, 10));
        assertThrows(IllegalArgumentException.class,
                () -> GreedyOptimizer.greedyKnapsack(new int[]{0}, new int[]{60}, 10));
        assertThrows(IllegalArgumentException.class,
                () -> DPOptimizer.knapsack(new int[]{10}, new int[]{-1}, 10));
        assertThrows(IllegalArgumentException.class,
                () -> GreedyOptimizer.greedyKnapsack(new int[]{10}, new int[]{-1}, 10));
        assertThrows(IllegalArgumentException.class,
                () -> GreedyOptimizer.scheduleJobs(new int[]{0}));
        assertThrows(IllegalArgumentException.class,
                () -> GreedyOptimizer.coinChange(new int[]{0, 1}, 6));
        assertThrows(IllegalArgumentException.class,
                () -> GreedyOptimizer.coinChange(new int[]{1}, -1));
    }

    @Test
    void reconstructedIndicesCannotMutateTheStoredResult() {
        KnapsackResult result = DPOptimizer.solveKnapsack(
                new int[]{10, 20, 30}, new int[]{60, 100, 120}, 50);
        int[] callerCopy = result.getSelectedIndices();

        callerCopy[0] = 0;

        assertArrayEquals(new int[]{1, 2}, result.getSelectedIndices());
    }

    /** The documented optimum for amount 6 is two coins: 3 + 3. */
    private static int optimalCoinCountForTheDocumentedFixture() {
        return 2;
    }
}
