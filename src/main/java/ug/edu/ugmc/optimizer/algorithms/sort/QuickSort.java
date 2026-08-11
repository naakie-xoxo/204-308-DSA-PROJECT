package ug.edu.ugmc.optimizer.algorithms.sort;

import ug.edu.ugmc.optimizer.datastructures.linear.DynamicArray;

public class QuickSort {

    // Universal Global Parameter derived from Index 22389307
    private static final int STUDENT_INDEX = 22389307;
    private static final int SUBARRAY_CUTOFF = (STUDENT_INDEX % 10) + 5; // Evaluates to 12

    /**
     *  Sorting elements using QuickSort in ascending order[cite: 1].
     *
     * @param requests Custom DynamicArray containing Integer objects[cite: 1]
     */
    public static void sort(DynamicArray<Integer> requests) {
        if (requests == null || requests.size() <= 1) {
            return;
        }
        quickSort(requests, 0, requests.size() - 1);
    }

    private static void quickSort(DynamicArray<Integer> requests, int low, int high) {
        // Optimization: Use Insertion Sort for sub-arrays smaller than or equal to cutoff
        if (high - low + 1 <= SUBARRAY_CUTOFF) {
            insertionSort(requests, low, high);
            return;
        }

        if (low < high) {
            int pivotIndex = partition(requests, low, high);
            quickSort(requests, low, pivotIndex - 1);
            quickSort(requests, pivotIndex + 1, high);
        }
    }

    private static int partition(DynamicArray<Integer> requests, int low, int high) {
        // Pick the rightmost element as the pivot
        Integer pivot = requests.get(high);
        int i = low - 1;

        for (int j = low; j < high; j++) {
            Integer current = requests.get(j);
            // Sorting by ascending order[cite: 1]
            if (current <= pivot) {
                i++;
                swap(requests, i, j);
            }
        }
        swap(requests, i + 1, high);
        return i + 1;
    }

    private static void swap(DynamicArray<Integer> requests, int i, int j) {
        Integer temp = requests.get(i);
        requests.set(i, requests.get(j));
        requests.set(j, temp);
    }

    private static void insertionSort(DynamicArray<Integer> requests, int left, int right) {
        for (int i = left + 1; i <= right; i++) {
            Integer key = requests.get(i);
            int j = i - 1;

            while (j >= left && requests.get(j) > key) {
                requests.set(j + 1, requests.get(j));
                j--;
            }
            requests.set(j + 1, key);
        }
    }
}