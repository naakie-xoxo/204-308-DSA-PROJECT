package ug.edu.ugmc.optimizer.datastructures.linear;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DynamicArrayTest {
    private DynamicArray<Integer> array;

    @BeforeEach
    void setUp() {
        array = new DynamicArray<>();
    }

    // Test 1: Normal Insertion
    @Test
    void testInsertAndGetNormal() {
        array.insert(10);
        array.insert(20);
        assertEquals(10, array.get(0));
        assertEquals(20, array.get(1));
    }

    // Test 2: Boundary - Resize Trigger
    @Test
    void testResizeBoundary() {
        for (int i = 0; i < 15; i++) { // Assuming default capacity is 10
            array.insert(i);
        }
        assertEquals(14, array.get(14));
        assertEquals(15, array.size());
    }

    // Test 3: Normal - Remove and Shift
    @Test
    void testRemoveAndShift() {
        array.insert(1); array.insert(2); array.insert(3);
        array.remove(1); // Remove middle element
        assertEquals(3, array.get(1)); // 3 should shift left
        assertEquals(2, array.size());
    }

    // Test 4: Invalid - Out of Bounds
    @Test
    void testOutOfBoundsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> array.get(0));
        array.insert(5);
        assertThrows(IndexOutOfBoundsException.class, () -> array.get(5));
    }
}

class SinglyLinkedListTest {
    private SinglyLinkedList<String> list;

    @BeforeEach
    void setUp() {
        list = new SinglyLinkedList<>();
    }

    // Test 5: Normal - Add First and Last
    @Test
    void testAddFirstAndLast() {
        list.addFirst("Ward A");
        list.addLast("ER");
        assertEquals("Ward A", list.get(0));
        assertEquals("ER", list.get(1));
    }

    // Test 6: Boundary - Insert After
    @Test
    void testInsertAfter() {
        list.addLast("Pharmacy");
        list.addLast("ICU");
        list.insertAfter("Pharmacy", "Lab");
        assertEquals("Lab", list.get(1));
    }

    // Test 7: Invalid - Remove from Empty
    @Test
    void testRemoveFromEmptyThrowsException() {
        assertThrows(IllegalStateException.class, () -> list.remove("NonExistent"));
    }

    // Test 8: Normal - Custom Iterator
    @Test
    void testIteratorTraversal() {
        list.addLast("A"); list.addLast("B");
        CustomIterator<String> iterator = list.iterator();
        assertTrue(iterator.hasNext());
        assertEquals("A", iterator.next());
        assertEquals("B", iterator.next());
        assertFalse(iterator.hasNext());
    }
}