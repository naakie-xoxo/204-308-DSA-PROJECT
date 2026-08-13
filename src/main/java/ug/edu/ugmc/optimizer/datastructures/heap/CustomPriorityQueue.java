package ug.edu.ugmc.optimizer.datastructures.heap;

/**
 * A min-priority queue implemented with a binary heap.
 *
 * <p>The queue grows as needed so graph algorithms are not limited by an
 * arbitrary number of relaxations.  Priorities are stored as {@code long}s to
 * keep path calculations from wrapping when a large hospital network is
 * loaded.</p>
 */
public class CustomPriorityQueue {

    private static class Node {
        String value;
        long priority;

        Node(String value, long priority) {
            this.value = value;
            this.priority = priority;
        }
    }

    private static final int INITIAL_CAPACITY = 16;
    private Node[] heap;
    private int size;

    public CustomPriorityQueue() {
        heap = new Node[INITIAL_CAPACITY];
        size = 0;
    }

    /** Inserts a value with an integer priority. */
    public void insert(String value, int priority) {
        insert(value, (long) priority);
    }

    /** Inserts a value with a long priority without narrowing or wrapping it. */
    public void insert(String value, long priority) {
        if (value == null) {
            throw new IllegalArgumentException("Priority queue values cannot be null.");
        }
        ensureCapacity();
        heap[size] = new Node(value, priority);
        bubbleUp(size);
        size++;
    }

    public String extractHighestPriority() {
        if (size == 0) {
            throw new IllegalStateException("Priority Queue is empty");
        }

        String result = heap[0].value;
        heap[0] = heap[size - 1];
        size--;
        sinkDown(0);

        return result;
    }

    private void bubbleUp(int index) {
        while (index > 0) {
            int parent = (index - 1) / 2;

            if (heap[index].priority >= heap[parent].priority) {
                break;
            }

            swap(index, parent);
            index = parent;
        }
    }

    private void ensureCapacity() {
        if (size < heap.length) {
            return;
        }

        int newCapacity = heap.length > Integer.MAX_VALUE / 2
                ? Integer.MAX_VALUE
                : heap.length * 2;
        if (newCapacity <= heap.length) {
            throw new IllegalStateException("Priority Queue capacity exhausted.");
        }

        Node[] expanded = new Node[newCapacity];
        for (int i = 0; i < size; i++) {
            expanded[i] = heap[i];
        }
        heap = expanded;
    }

    private void sinkDown(int index) {
        while (true) {
            int left = 2 * index + 1;
            int right = 2 * index + 2;
            int smallest = index;

            if (left < size && heap[left].priority < heap[smallest].priority) {
                smallest = left;
            }

            if (right < size && heap[right].priority < heap[smallest].priority) {
                smallest = right;
            }

            if (smallest == index) {
                break;
            }

            swap(index, smallest);
            index = smallest;
        }
    }

    private void swap(int i, int j) {
        Node temp = heap[i];
        heap[i] = heap[j];
        heap[j] = temp;
    }
}
