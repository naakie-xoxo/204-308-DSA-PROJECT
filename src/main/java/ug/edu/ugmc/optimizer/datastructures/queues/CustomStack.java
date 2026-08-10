package ug.edu.ugmc.optimizer.datastructures.queues;

/**
 * Custom Stack Implementation for Undo Log and Audit Trail.
 * Accommodates individual accountability tracking for the joint project.
 * 
 * Derived from index number 22401371 (Kingsley) to satisfy the Stack Audit Log Depth
 * requirement: (22401371 % 100) + 50 = 71 + 50 = 121 maximum depth limit.
 */
public class CustomStack<T> {

    // Parameter derived explicitly from index number 22401371
    private static final int MAX_AUDIT_DEPTH = (22401371 % 100) + 50; // Evaluates to 121

    private Node<T> top;
    private int size;

    private static class Node<E> {
        E data;
        Node<E> next;

        Node(E data) {
            this.data = data;
        }
    }

    public CustomStack() {
        this.top = null;
        this.size = 0;
    }

    /**
     * Pushes an element onto the top of the stack.
     * Enforces the MAX_AUDIT_DEPTH constraint by dropping the bottom-most element if exceeded.
     */
    public void push(T item) {
        if (item == null) {
            throw new IllegalArgumentException("Cannot push null item onto the audit stack.");
        }

        Node<T> newNode = new Node<>(item);
        newNode.next = top;
        top = newNode;
        size++;

        // Rule check: Drop oldest event if audit log capacity is exceeded
        if (size > MAX_AUDIT_DEPTH) {
            dropOldestElement();
        }
    }

    /**
     * Removes and returns the top element of the stack.
     */
    public T pop() {
        if (isEmpty()) {
            throw new IllegalStateException("Audit Stack underflow: The undo trail is empty.");
        }
        T data = top.data;
        top = top.next;
        size--;
        return data;
    }

    /**
     * Returns the top element without removing it.
     */
    public T peek() {
        if (isEmpty()) {
            throw new IllegalStateException("Audit Stack is empty: Cannot peek.");
        }
        return top.data;
    }

    public boolean isEmpty() {
        return top == null;
    }

    public int size() {
        return size;
    }

    /**
     * Helper method to purge the oldest element at the bottom of the stack 
     * to honor the fixed audit depth boundary.
     */
    private void dropOldestElement() {
        if (top == null) return;
        
        // If there's only one element, just clear it
        if (top.next == null) {
            top = null;
            size = 0;
            return;
        }

        // Traverse down to find the second-to-last node
        Node<T> current = top;
        while (current.next != null && current.next.next != null) {
            current = current.next;
        }
        
        // Sever the link to drop the absolute oldest element
        current.next = null;
        size--;
    }
}
