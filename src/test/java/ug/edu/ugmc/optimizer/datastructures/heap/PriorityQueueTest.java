package ug.edu.ugmc.optimizer.datastructures.heap;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PriorityQueueTest {

    // Test 15: MinHeap - Extract Min
    @Test
    void testHeapExtractMin() {
        MinHeap heap = new MinHeap();
        heap.insert(5); heap.insert(1); heap.insert(3);
        assertEquals(1, heap.extractMin());
        assertEquals(3, heap.extractMin());
    }

    // Test 16: Priority Queue - Urgency Dispatch
    @Test
    void testPriorityQueueDispatch() {
        CustomPriorityQueue pq = new CustomPriorityQueue();
        pq.insert("Mild Pain", 3);
        pq.insert("Cardiac Arrest", 1); // Lower number = higher urgency
        assertEquals("Cardiac Arrest", pq.extractHighestPriority());
    }

    // Test 17: Boundary - Single Element
    @Test
    void testHeapSingleElement() {
        MinHeap heap = new MinHeap();
        heap.insert(10);
        assertEquals(10, heap.extractMin());
        assertTrue(heap.isEmpty());
    }
    
    // Test 18: Invalid - Extract from Empty
    @Test
    void testExtractEmptyThrowsException() {
        MinHeap heap = new MinHeap();
        assertThrows(IllegalStateException.class, heap::extractMin);
    }
}