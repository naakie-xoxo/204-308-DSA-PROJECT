 
package ug.edu.ugmc.optimizer.datastructures.queues;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QueueAndStackTest {

    // Test 9: Stack - Normal Push/Pop (Audit Trail)
    @Test
    void testStackPushPop() {
        CustomStack<String> stack = new CustomStack<>();
        stack.push("Action1");
        stack.push("Action2");
        assertEquals("Action2", stack.pop());
        assertEquals("Action1", stack.peek());
    }

    // Test 10: Stack - Invalid Pop Empty
    @Test
    void testPopEmptyStack() {
        CustomStack<String> stack = new CustomStack<>();
        assertThrows(IllegalStateException.class, stack::pop);
    }

    // Test 11: Queue - FIFO Dispatch
    @Test
    void testQueueFIFO() {
        CustomQueue<String> queue = new CustomQueue<>();
        queue.enqueue("Req1");
        queue.enqueue("Req2");
        assertEquals("Req1", queue.dequeue());
        assertEquals("Req2", queue.peek());
    }

    // Test 12: Circular Queue - Wrap Around Boundary
    @Test
    void testCircularQueueWrapAround() {
        // Assume capacity of 3 derived from index 22384306 requirements
        CircularQueue<Integer> cq = new CircularQueue<>(3);
        cq.enqueue(1); cq.enqueue(2); cq.enqueue(3);
        cq.dequeue(); // Frees up index 0
        cq.enqueue(4); // Should wrap to index 0
        assertEquals(2, cq.dequeue());
        assertEquals(3, cq.dequeue());
        assertEquals(4, cq.dequeue()); // Verifies successful wrap
    }

    // Test 13: Circular Queue - Full Exception
    @Test
    void testCircularQueueFull() {
        CircularQueue<Integer> cq = new CircularQueue<>(2);
        cq.enqueue(1); cq.enqueue(2);
        assertThrows(IllegalStateException.class, () -> cq.enqueue(3));
    }

    // Test 14: Deque - Urgent Dispatch
    @Test
    void testDequeUrgentInsert() {
        CustomDeque<String> deque = new CustomDeque<>();
        deque.addRear("NormalReq");
        deque.addFront("UrgentReq"); // Bypasses the line
        assertEquals("UrgentReq", deque.removeFront());
    }
}
