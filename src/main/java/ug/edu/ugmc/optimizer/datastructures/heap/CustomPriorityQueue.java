package ug.edu.ugmc.optimizer.datastructures.heap;

public class CustomPriorityQueue {

    private static class Node {
        String value;
        int priority;

        Node(String value, int priority) {
            this.value = value;
            this.priority = priority;
        }
    }

    private static final int CAPACITY = 137;
    private Node[] heap;
    private int size;

    public CustomPriorityQueue() {
        heap = new Node[CAPACITY];
        size = 0;
    }

    public void insert(String value, int priority) {
        if (size == heap.length) {
            throw new IllegalStateException("Priority Queue is full");
        }

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
