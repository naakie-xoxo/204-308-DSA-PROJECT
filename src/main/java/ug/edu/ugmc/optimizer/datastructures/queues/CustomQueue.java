package ug.edu.ugmc.optimizer.datastructures.queues;

/**
 * Custom Circular Queue Implementation for FIFO Service Request Processing.
 * Accommodates individual accountability tracking for the joint project.
 * 
 * Derived from index number 22384306 (Maron) to establish the strict circular 
 * buffer capacity framework layout: (22384306 % 100) + 50 = 6 + 50 = 56 capacity.
 */
public class CustomQueue<T> {

    // Parameter derived explicitly from index number 22384306
    private static final int BUFFER_LIMIT = (22384306 % 100) + 50; // Evaluates to 56

    private final T[] array;
    private int front;
    private int rear;
    private int size;

    @SuppressWarnings("unchecked")
    public CustomQueue() {
        // Safe casting array allocation because raw built-in Java arrays are allowed for backing
        this.array = (T[]) new Object[BUFFER_LIMIT];
        this.front = 0;
        this.rear = -1;
        this.size = 0;
    }

    /**
     * Adds an item to the rear of the circular queue using wrap-around handling.
     */
    public void enqueue(T item) {
        if (item == null) {
            throw new IllegalArgumentException("Cannot enqueue a null service request.");
        }
        if (isFull()) {
            throw new IllegalStateException("Circular Queue Overflow: Buffer limit of " + BUFFER_LIMIT + " hit.");
        }

        // Handle wrap-around index pointer arithmetic cleanly
        rear = (rear + 1) % BUFFER_LIMIT;
        array[rear] = item;
        size++;
    }

    /**
     * Removes and returns the front item of the circular queue.
     */
    public T dequeue() {
        if (isEmpty()) {
            throw new IllegalStateException("Circular Queue Underflow: No requests to dispatch.");
        }

        T item = array[front];
        array[front] = null; // Clear reference for garbage collection
        
        // Advance front pointer using wrap-around index manipulation
        front = (front + 1) % BUFFER_LIMIT;
        size--;
        return item;
    }

    /**
     * Returns the front element without removing it.
     */
    public T peek() {
        if (isEmpty()) {
            throw new IllegalStateException("Circular Queue is empty: Cannot peek.");
        }
        return array[front];
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean isFull() {
        return size == BUFFER_LIMIT;
    }

    public int size() {
        return size;
    }
    
    public int getCapacity() {
        return BUFFER_LIMIT;
    }
}
