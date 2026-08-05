package ug.edu.ugmc.optimizer.algorithms.sort;

// Import Group A's custom structure and model
import ug.edu.ugmc.optimizer.datastructures.linear.DynamicArray;
import ug.edu.ugmc.optimizer.models.ServiceRequest;

public class MergeSort {


    private static final int STUDENT_INDEX = 22389307;

    private static final int SUBARRAY_CUTOFF = (STUDENT_INDEX % 10) +5 ;

    /**
     *  Sorting service requests by urgency score using MergeSort.
     * Higher urgency (5) comes before lower urgency (1).
     *
     * @param requests Custom DynamicArray containing ServiceRequest objects
     */

    public static void sort(DynamicArray requests) {
        if (requests == null || requests.size() <= 1) {
            return;
        }
        mergeSort(requests, 0, requests.size() - 1);
    }

    private static void mergeSort(DynamicArray requests, int left, int right) {
        // Optimization: Switch to Insertion Sort when sub-array size <= SUBARRAY_CUTOFF (12)
        if (right - left + 1 <= SUBARRAY_CUTOFF) {
            insertionSort(requests, left, right);
            return;
        }

        if (left < right) {
            int mid = left + (right - left) / 2;

            // Divide
            mergeSort(requests, left, mid);
            mergeSort(requests, mid + 1, right);

            // Conquer / Merge
            merge(requests, left, mid, right);
        }
    }

    private static void merge(DynamicArray requests, int left, int mid, int right) {
        int leftSize = mid - left + 1;
        int rightSize = right - mid;

        ServiceRequest[] leftArray = new ServiceRequest[leftSize];
        ServiceRequest[] rightArray = new ServiceRequest[rightSize];

        for (int i = 0; i < leftSize; i++) {
            leftArray[i] = (ServiceRequest) requests.get(left + i);
        }
        for (int j = 0; j < rightSize; j++) {
            rightArray[j] = (ServiceRequest) requests.get(mid + 1 + j);
        }

        int i = 0, j = 0;
        int k = left;

        while (i < leftSize && j < rightSize) {
            // Primary sort key = urgency level in descending order
            if (leftArray[i].getUrgency() >= rightArray[j].getUrgency()) {
                requests.set(k, leftArray[i]);
                i++;
            } else {
                requests.set(k, rightArray[j]);
                j++;
            }
            k++;
        }

        while (i < leftSize) {
            requests.set(k, leftArray[i]);
            i++;
            k++;
        }

        while (j < rightSize) {
            requests.set(k, rightArray[j]);
            j++;
            k++;
        }
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



