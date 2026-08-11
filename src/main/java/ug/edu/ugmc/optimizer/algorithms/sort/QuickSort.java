package ug.edu.ugmc.optimizer.algorithms.sort;

import ug.edu.ugmc.optimizer.datastructures.linear.DynamicArray;

public class QuickSort {
    
    private static final int STUDENT_INDEX = 22389307; 
    private static final int SUBARRAY_CUTOFF = (STUDENT_INDEX % 10) + 5; 

    /**
     * Sorting elements using QuickSort in ascending order.
     *
     * @param requests Custom DynamicArray containing Integer objects
     */
    public static void sort(DynamicArray<Integer> requests) {
        if (requests == null || requests.size() <= 1) {
            return;
        }
        quickSort(requests, 0, requests.size() - 1);
    }

    private static void quickSort(DynamicArray<Integer> requests, int low, int high) {
        if (low < high) {
            if (high - low + 1 <= SUBARRAY_CUTOFF) {
                insertionSort(requests, low, high);
            } else {
                int pi = partition(requests, low, high);
                quickSort(requests, low, pi - 1);
                quickSort(requests, pi + 1, high);
            }
        }
    }

    private static int partition(DynamicArray<Integer> requests, int low, int high) {
        Integer pivot = requests.get(high);
        int i = low - 1;

        for (int j = low; j < high; j++) {
            if (requests.get(j) <= pivot) {
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
