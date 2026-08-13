package ug.edu.ugmc.optimizer.algorithms.sort;

import ug.edu.ugmc.optimizer.datastructures.linear.DynamicArray;

public class QuickSort {

    private static final int STUDENT_INDEX = 22389307;
    private static final int RANDOM_SEED = 22040372;
    private static final int SUBARRAY_CUTOFF = (STUDENT_INDEX % 10) + 5;

    /**
     * Sorting elements using QuickSort in ascending order.
     *
     * @param values custom DynamicArray containing Integer objects
     */
    public static void sort(DynamicArray<Integer> values) {
        if (values == null || values.size() <= 1) {
            return;
        }
        quickSort(values, 0, values.size() - 1, new PivotGenerator(RANDOM_SEED));
    }

    private static void quickSort(
            DynamicArray<Integer> values,
            int low,
            int high,
            PivotGenerator pivots) {
        if (low < high) {
            if (high - low + 1 <= SUBARRAY_CUTOFF) {
                insertionSort(values, low, high);
            } else {
                int pivotIndex = partition(values, low, high, pivots);
                quickSort(values, low, pivotIndex - 1, pivots);
                quickSort(values, pivotIndex + 1, high, pivots);
            }
        }
    }

    private static int partition(
            DynamicArray<Integer> values,
            int low,
            int high,
            PivotGenerator pivots) {
        int selectedPivot = low + pivots.nextInt(high - low + 1);
        swap(values, selectedPivot, high);
        Integer pivot = values.get(high);
        int i = low - 1;

        for (int j = low; j < high; j++) {
            if (values.get(j) <= pivot) {
                i++;
                swap(values, i, j);
            }
        }
        swap(values, i + 1, high);
        return i + 1;
    }

    private static void swap(DynamicArray<Integer> values, int i, int j) {
        Integer temp = values.get(i);
        values.set(i, values.get(j));
        values.set(j, temp);
    }

    private static void insertionSort(DynamicArray<Integer> values, int left, int right) {
        for (int i = left + 1; i <= right; i++) {
            Integer key = values.get(i);
            int j = i - 1;

            while (j >= left && values.get(j) > key) {
                values.set(j + 1, values.get(j));
                j--;
            }
            values.set(j + 1, key);
        }
    }

    /** Small deterministic pseudo-random generator; no library collections are used. */
    private static final class PivotGenerator {
        private int state;

        private PivotGenerator(int seed) {
            state = seed;
        }

        private int nextInt(int bound) {
            state = state * 1103515245 + 12345;
            return (state & Integer.MAX_VALUE) % bound;
        }
    }
}
