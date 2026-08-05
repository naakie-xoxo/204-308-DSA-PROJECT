package ug.edu.ugmc.optimizer.algorithms.sort;

import ug.edu.ugmc.optimizer.datastructures.linear.DynamicArray;
import ug.edu.ugmc.optimizer.models.ServiceRequest;

public class QuickSort {

    // Universal Global Parameter derived from Index 22389307
    private static final int STUDENT_INDEX = 22389307;
    private static final int SUBARRAY_CUTOFF = (STUDENT_INDEX % 10) + 5; // Evaluates to 12

    /**
     *  Sorting service requests by urgency score using QuickSort.
     * Higher urgency (5) comes before lower urgency (1).
     *
     * @param requests Custom DynamicArray containing ServiceRequest objects
     */
    public static void sort(DynamicArray requests) {
        if (requests == null || requests.size() <= 1) {
            return;
        }
        quickSort(requests, 0, requests.size() - 1);
    }

    private static void quickSort(DynamicArray requests, int low, int high) {
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

    private static int partition(DynamicArray requests, int low, int high) {
        // Pick the rightmost element as  the pivot
        ServiceRequest pivot = (ServiceRequest) requests.get(high);
        int i = low - 1;

        for (int j = low; j < high; j++) {
            ServiceRequest current = (ServiceRequest) requests.get(j);
            // Sorting by descending urgency
            if (current.getUrgency() >= pivot.getUrgency()) {
                i++;
                swap(requests, i, j);
            }
        }
        swap(requests, i + 1, high);
        return i + 1;
    }

    private static void swap(DynamicArray requests, int i, int j) {
        Object temp = requests.get(i);
        requests.set(i, requests.get(j));
        requests.set(j, temp);
    }

    private static void insertionSort(DynamicArray requests, int left, int right) {
        for (int i = left + 1; i <= right; i++) {
            ServiceRequest key = (ServiceRequest) requests.get(i);
            int j = i - 1;

            while (j >= left && ((ServiceRequest) requests.get(j)).getUrgency() < key.getUrgency()) {
                requests.set(j + 1, requests.get(j));
                j--;
            }
            requests.set(j + 1, key);
        }
    }
}
