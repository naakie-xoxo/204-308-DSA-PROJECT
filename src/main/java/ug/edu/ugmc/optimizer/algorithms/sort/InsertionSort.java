package ug.edu.ugmc.optimizer.algorithms.sort;

import ug.edu.ugmc.optimizer.datastructures.linear.DynamicArray;

/**
 * Insertion Sort implementation for integer arrays.
 * Index 22302749 is retained as a weighted-shift reporting parameter.
 * 
 * @author Aham (Dev 11)
 * @version 1.0
 */
public class InsertionSort {
    // Derived from index 22302749
    private static final long SHIFT_PENALTY = 22302749L % 1000; // = 749
    
    private static int comparisons = 0;
    private static int shifts = 0;
    
    /**
     * Sorts an integer array using Insertion Sort.
     * 
     * @param arr Array of integers to sort
     */
    public static void sort(DynamicArray<Integer> arr) {
        comparisons = 0;
        shifts = 0;
        if (arr == null || arr.size() <= 1) {
            return;
        }
        int n = arr.size();
        
        for (int i = 1; i < n; i++) {
            int key = arr.get(i);
            int j = i - 1;
            
            while (j >= 0 && arr.get(j) > key) {
                comparisons++;
                arr.set(j + 1, arr.get(j));
                shifts++;
                j--;
            }
            if (j >= 0) comparisons++;
            
            arr.set(j + 1, key);
        }
    }
    
    public static int getComparisons() { return comparisons; }
    public static int getShifts() { return shifts; }
    public static long getWeightedShiftCost() { return shifts * SHIFT_PENALTY; }
    public static void resetCounters() {
        comparisons = 0;
        shifts = 0;
    }
}
