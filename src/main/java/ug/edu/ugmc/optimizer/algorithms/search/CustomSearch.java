package ug.edu.ugmc.optimizer.algorithms.search;

public class CustomSearch { 

    /**
     * Purpose: Finds a target by checking every element sequentially.
     * Hospital Use Case: Locating a newly admitted patient in an unsorted daily log.
     * Complexity: O(N) Time, O(1) Space.
     */
    public static int linearSearch(int[] arr, int target) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == target) {
                return i; // Target found, return index
            }
        }
        return -1; // Target not found
    }

    /**
     * Purpose: Finds a target by repeatedly halving the search space.
     * Hospital Use Case: Rapidly looking up a staff ID in a sorted master directory.
     * Complexity: O(log N) Time for search, O(1) Space.
     */
    public static int binarySearch(int[] arr, int target) {
        // REQUIRED EDGE CASE: Invalid Precondition Check
        // Explicitly reject unsorted arrays to satisfy project brief Section 10.
        for (int i = 0; i < arr.length - 1; i++) {
            if (arr[i] > arr[i + 1]) {
                throw new IllegalStateException("Array must be sorted prior to Binary Search.");
            }
        }

        int left = 0;
        int right = arr.length - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2; // Prevents integer overflow

            if (arr[mid] == target) {
                return mid; // Target found
            } else if (arr[mid] < target) {
                left = mid + 1; // Search right half
            } else {
                right = mid - 1; // Search left half
            }
        }
        
        return -1; // Target not found
    }
}