package ug.edu.ugmc.optimizer.algorithms.sort;

import ug.edu.ugmc.optimizer.datastructures.linear.DynamicArray;

/**
 * Selection Sort implementation for integer arrays.
 * Used for testing and sorting demonstrations.
 * 
 * @author Aham (Dev 11)
 * @version 1.0
 */
public class SelectionSort {
    private static int comparisons = 0;
    private static int swaps = 0;
    
    /**
     * Sorts an integer array using Selection Sort.
     * 
     * @param arr Array of integers to sort
     */
    public static void sort(DynamicArray<Integer> arr) {
        comparisons = 0;
        swaps = 0;
        int n = arr.size();
        
        for (int i = 0; i < n - 1; i++) {
            int minIdx = i;
            for (int j = i + 1; j < n; j++) {
                comparisons++;
                if (arr.get(j) < arr.get(minIdx)) {
                    minIdx = j;
                }
            }
            if (minIdx != i) {
                swaps++;
                Integer temp = arr.get(i);
                                arr.set(i, arr.get(minIdx));
                                arr.set(minIdx, temp);
            }
        }
    }
    
    public static int getComparisons() { return comparisons; }
    public static int getSwaps() { return swaps; }
    public static void resetCounters() {
        comparisons = 0;
        swaps = 0;
    }
}