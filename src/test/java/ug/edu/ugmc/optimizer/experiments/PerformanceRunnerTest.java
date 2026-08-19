package ug.edu.ugmc.optimizer.experiments;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import ug.edu.ugmc.optimizer.algorithms.search.CustomSearch;
import ug.edu.ugmc.optimizer.algorithms.sort.InsertionSort;
import ug.edu.ugmc.optimizer.algorithms.sort.QuickSort;
import ug.edu.ugmc.optimizer.datastructures.linear.DynamicArray;

/** Contract tests for the empirical lab's assessed scales and parameters. */
class PerformanceRunnerTest {

    @Test
    void exposesTheSixExactRequiredScales() {
        assertArrayEquals(
                new int[] {100, 500, 1_000, 5_000, 10_000, 50_000},
                PerformanceRunner.getDataScales());
    }

    @Test
    void presortedBinarySearchKeepsThePureLogarithmicLookupApi() {
        DynamicArray<Integer> ids = new DynamicArray<>(5);
        for (int id = 0; id < 5; id++) {
            ids.insert(id);
        }

        assertEquals(4, CustomSearch.binarySearchPresorted(ids, 4));
        assertEquals(-1, CustomSearch.binarySearchPresorted(ids, 9));
        assertThrows(IllegalArgumentException.class,
                () -> CustomSearch.binarySearchPresorted(null, 1));
    }

    @Test
    void insertionPenaltyAndQuickSortCutoffAreObservable() {
        DynamicArray<Integer> descending = new DynamicArray<>(4);
        descending.insert(4);
        descending.insert(3);
        descending.insert(2);
        descending.insert(1);
        InsertionSort.sort(descending);

        assertEquals(6, InsertionSort.getShiftPenaltyApplications());
        assertEquals(6L * 749L, InsertionSort.getWeightedShiftCost());

        DynamicArray<Integer> quickInput = new DynamicArray<>(10);
        for (int value = 10; value > 0; value--) {
            quickInput.insert(value);
        }
        QuickSort.sort(quickInput);

        assertEquals(12, QuickSort.getSubarrayCutoff());
        assertTrue(QuickSort.getInsertionFallbackCount() > 0);
    }
}
