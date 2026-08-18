package ug.edu.ugmc.optimizer.algorithms.search;

import ug.edu.ugmc.optimizer.datastructures.linear.DynamicArray;

public class CustomSearch {

    /**
     * Purpose: Finds a target by checking every element sequentially.
     * Hospital Use Case: Locating a newly admitted patient in an unsorted daily log.
     * Complexity: O(N) Time, O(1) Space.
     */
    public static int linearSearch(DynamicArray<Integer> arr, int target) {
        for (int i = 0; i < arr.size(); i++) {
            if (arr.get(i) == target) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Purpose: Finds a target by repeatedly halving the search space.
     * Hospital Use Case: Rapidly looking up a staff ID in a sorted master directory.
     * Complexity: O(log N) Time for search, O(1) Space.
     */
    public static int binarySearch(DynamicArray<Integer> arr, int target) {
        // REQUIRED EDGE CASE: Invalid Precondition Check
        for (int i = 0; i < arr.size() - 1; i++) {
            if (arr.get(i) > arr.get(i + 1)) {
                throw new IllegalStateException("Array must be sorted prior to Binary Search.");
            }
        }

        return binarySearchPresorted(arr, target);
    }

    /**
     * Performs the pure O(log N) lookup after the caller has established that
     * the data is sorted. The empirical lab validates and loads the structure
     * before starting its timer, so setup work is not mislabeled as search time.
     *
     * @param arr sorted custom dynamic array
     * @param target ID to locate
     * @return target index or {@code -1} when absent
     */
    public static int binarySearchPresorted(DynamicArray<Integer> arr, int target) {
        if (arr == null) {
            throw new IllegalArgumentException("Array cannot be null.");
        }

        int left = 0;
        int right = arr.size() - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            int midVal = arr.get(mid);
            if (midVal == target) {
                return mid;
            } else if (midVal < target) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        return -1;
    }
}
