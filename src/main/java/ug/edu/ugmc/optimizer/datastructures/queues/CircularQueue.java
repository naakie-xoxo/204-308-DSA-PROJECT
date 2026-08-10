package ug.edu.ugmc.optimizer.datastructures.queues;

/**
 * A bounded Circular Queue implementation for the UGMC Optimizer.
 * Engineered to handle incoming hospital service requests within a strictly enforced physical capacity.
 */
public class CircularQueue<T> {

    // Universal Parameter assigned to Maron
    private static final int BASE_INDEX = 22384306;
    
    // The strict, unchangeable array buffer limit
    private final int capacity;
    private T[] array;
    
    // Tracking pointers for modulo arithmetic
    private int front;
    private int rear;
    private int size;

    @SuppressWarnings("unchecked")
    public CircularQueue() {
        // Enforcing the strict hospital bay limit calculation
        this.capacity = (BASE_INDEX % 50) + 20;
        this.array = (T[]) new Object[capacity];
        
        this.front = 0;
        this.rear = -1;
        this.size = 0;
    }

    /**
     * Inserts an element at the rear of the queue.
     * Uses modulo arithmetic to wrap the rear pointer.
     * 
     * @param element The service request to add.
     * @throws IllegalStateException if the queue is full.
     */
    public void enqueue(T element) {
        // Edge Case Handling: Guard clause to prevent overwriting unhandled patients
        if (isFull()) {
            throw new IllegalStateException("Queue is full. Cannot overwrite unhandled patients.");
        }
        
        // Routing Logic: Wrap the rear pointer back to 0 if it hits the physical limit
        rear = (rear + 1) % capacity;
        array[rear] = element;
        size++;
    }

    /**
     * Removes and returns the element at the front of the queue.
     * Uses modulo arithmetic to wrap the front pointer.
     * 
     * @return The processed service request.
     * @throws IllegalStateException if the queue is empty.
     */
    public T dequeue() {
        if (isEmpty()) {
            throw new IllegalStateException("Queue is empty. No requests to process.");
        }
        
        T element = array[front];
        array[front] = null; // Clear the memory reference to prevent memory leaks
        
        // Routing Logic: Wrap the front pointer back to 0 if it hits the physical limit
        front = (front + 1) % capacity;
        size--;
        
        return element;
    }

    /**
     * Returns the element at the front without removing it.
     */
    public T peek() {
        if (isEmpty()) {
            throw new IllegalStateException("Queue is empty.");
        }
        return array[front];
    }

    public boolean isFull() {
        return size == capacity;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }
    
    public int getCapacity() {
        return capacity;
    }
}