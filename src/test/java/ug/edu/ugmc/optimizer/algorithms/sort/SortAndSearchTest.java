package ug.edu.ugmc.optimizer.algorithms.sort;

import org.junit.jupiter.api.Test;
import ug.edu.ugmc.optimizer.algorithms.search.CustomSearch;
import static org.junit.jupiter.api.Assertions.*;

class SortAndSearchTest {

    // Test 24: Selection Sort Normal
    @Test
    void testSelectionSort() {
        int[] arr = {5, 2, 9, 1};
        SelectionSort.sort(arr);
        assertArrayEquals(new int[]{1, 2, 5, 9}, arr);
    }

    // Test 25: Insertion Sort Already Sorted (Best Case)
    @Test
    void testInsertionSortAlreadySorted() {
        int[] arr = {1, 2, 3, 4};
        InsertionSort.sort(arr);
        assertArrayEquals(new int[]{1, 2, 3, 4}, arr);
    }

    // Test 26: Merge Sort Reverse Order (Worst Case)
    @Test
    void testMergeSortReverse() {
        int[] arr = {9, 7, 5, 3};
        MergeSort.sort(arr);
        assertArrayEquals(new int[]{3, 5, 7, 9}, arr);
    }

    // Test 27: Quick Sort Duplicates Boundary
    @Test
    void testQuickSortDuplicates() {
        int[] arr = {4, 1, 4, 1, 4};
        QuickSort.sort(arr); // Must use index 22040372 for random pivot
        assertArrayEquals(new int[]{1, 1, 4, 4, 4}, arr);
    }

    // Test 28: Linear Search
    @Test
    void testLinearSearch() {
        int[] arr = {10, 20, 30};
        assertEquals(1, CustomSearch.linearSearch(arr, 20));
        assertEquals(-1, CustomSearch.linearSearch(arr, 99));
    }

    // Test 29: Binary Search Normal
    @Test
    void testBinarySearchNormal() {
        int[] arr = {1, 5, 10, 15, 20};
        assertEquals(3, CustomSearch.binarySearch(arr, 15));
    }

    // Test 30: Binary Search Invalid Precondition (Unsorted Array)
    // Project Brief Section 10 requires this specific counterexample test
    @Test
    void testBinarySearchUnsortedPrecondition() {
        int[] arr = {10, 1, 5};
        assertThrows(IllegalStateException.class, () -> CustomSearch.binarySearch(arr, 5), 
            "Binary search must explicitly reject unsorted arrays per the DSA brief.");
    }
}