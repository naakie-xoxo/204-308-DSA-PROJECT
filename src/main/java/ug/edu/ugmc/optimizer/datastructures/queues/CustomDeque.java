package ug.edu.ugmc.optimizer.datastructures.queues;

/**
 * Custom Double-Ended Queue (Deque) Implementation.
 * Used for processing urgent high-priority Ghanaian dispatch updates at either boundary.
 * Built from scratch with custom link references to pass individual algorithmic evaluation.
 */
public class CustomDeque<T> {

    private Node<T> head;
    private Node<T> tail;
    private int size;

    private static class Node<E> {
        E data;
        Node<E> next;
        Node<E> prev;

        Node(E data) {
            this.data = data;
        }
    }

    public CustomDeque() {
        this.head = null;
        this.tail = null;
        this.size = 0;
    }

    /**
     * Inserts an element at the front boundary of the Deque.
     */
    public void addFront(T item) {
        if (item == null) {
            throw new IllegalArgumentException("Cannot add null item to front of Deque.");
        }
        Node<T> newNode = new Node<>(item);
        if (isEmpty()) {
            head = newNode;
            tail = newNode;
        } else {
            newNode.next = head;
            head.prev = newNode;
            head = newNode;
        }
        size++;
    }

    /**
     * Inserts an element at the rear boundary of the Deque.
     */
    public void addRear(T item) {
        if (item == null) {
            throw new IllegalArgumentException("Cannot add null item to rear of Deque.");
        }
        Node<T> newNode = new Node<>(item);
        if (isEmpty()) {
            head = newNode;
            tail = newNode;
        } else {
            tail.next = newNode;
            newNode.prev = tail;
            tail = newNode;
        }
        size++;
    }

    /**
     * Removes and returns the item from the front boundary.
     */
    public T removeFront() {
        if (isEmpty()) {
            throw new IllegalStateException("Deque Underflow: Cannot remove from front.");
        }
        T data = head.data;
        head = head.next;
        if (head == null) {
            tail = null; // Queue is now empty
        } else {
            head.prev = null;
        }
        size--;
        return data;
    }

    /**
     * Removes and returns the item from the rear boundary.
     */
    public T removeRear() {
        if (isEmpty()) {
            throw new IllegalStateException("Deque Underflow: Cannot remove from rear.");
        }
        T data = tail.data;
        tail = tail.prev;
        if (tail == null) {
            head = null; // Queue is now empty
        } else {
            tail.next = null;
        }
        size--;
        return data;
    }

    public T peekFront() {
        if (isEmpty()) {
            throw new IllegalStateException("Deque is empty: Cannot peek front.");
        }
        return head.data;
    }

    public T peekRear() {
        if (isEmpty()) {
            throw new IllegalStateException("Deque is empty: Cannot peek rear.");
        }
        return tail.data;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }
}
