package ug.edu.ugmc.optimizer.datastructures.queues;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class QueuesTestSuite {

    @Test
    public void testStackAuditLogDepthConstraint() {
        CustomStack<String> auditStack = new CustomStack<>();
        // Push more items than the explicit max capacity limit (121 elements)
        for (int i = 1; i <= 130; i++) {
            auditStack.push("Event " + i);
        }
        // Size should clamp perfectly to your derived index depth parameter limit
        assertEquals(121, auditStack.size());
        // The earliest elements (1 through 9) should have been dropped
        assertEquals("Event 130", auditStack.peek());
    }

    @Test
    public void testCircularQueueBufferLimitOverflow() {
        CustomQueue<Integer> requestQueue = new CustomQueue<>();
        // Fill up to Maron's explicit parameter capacity limit (56 elements)
        for (int i = 0; i < 56; i++) {
            requestQueue.enqueue(i);
        }
        assertTrue(requestQueue.isFull());
        // Assert that the 57th element throws an overflow exception
        assertThrows(IllegalStateException.class, () -> requestQueue.enqueue(999));
    }

    @Test
    public void testDequeBoundaryInsertions() {
        CustomDeque<String> urgentDeque = new CustomDeque<>();
        urgentDeque.addRear("Request_Normal_1");
        urgentDeque.addFront("Request_Urgent_Ambulance");
        
        assertEquals("Request_Urgent_Ambulance", urgentDeque.peekFront());
        assertEquals("Request_Normal_1", urgentDeque.peekRear());
    }
}
