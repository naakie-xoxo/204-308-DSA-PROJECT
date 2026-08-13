package ug.edu.ugmc.optimizer.datastructures.linear;

public class SinglyLinkedList<T> { 
    //custom node class
    private class Node {
        T data;
        Node next;

        Node(T data) {
            this.data = data;
            this.next = null;
        }
    }

    private Node head;
    private int size;

    //addFrist method to add an element at the beginning of the linked list
    public void addFirst(T element) {
        Node newNode = new Node(element);
        newNode.next = head;
        head = newNode;
        size++;
    }

    //addLast method to add an element at the end of the linked list
    public void addLast(T element) {
        Node newNode = new Node(element);
        if (head == null) {
            head = newNode;
        } else {
            Node current = head;
           
            while (current.next != null) {
                current = current.next;
            }
            current.next = newNode;
        }
        size++;
    }

    //get element at a specific index in the linked list
    public T get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        Node current = head;
        for (int i = 0; i < index; i++) {
            current = current.next;
        }
        return current.data;
    }

    //insertAfter method to insert an element after a specific index in the linked list
    public void insertAfter(T target, T element) {
        Node current =head;
        while (current != null && !current.data.equals(target)) {
            current = current.next;
        }
        if (current ==null){
            throw new IllegalArgumentException("Target element not found in the list");
        }
        Node newNode = new Node(element);
        newNode.next = current.next;
        current.next = newNode;
        size++;
    }

    //remove method to remove an element at a specific index in the linked list
    public void remove(T element) {
        if (head == null) {
            throw new IllegalStateException("List is empty");
        }
        if (head.data.equals(element)) {
            head = head.next;
            size--;
            return;
        } 
        Node current = head;
        while (current.next != null && !current.next.data.equals(element)) {
            current = current.next;
        }
        if (current.next == null) {
            throw new IllegalArgumentException("Element not found in the list");
        }
        current.next = current.next.next;
        size--;
    }   

    //custom iterator method to return an iterator for the linked list
    public CustomIterator<T> iterator() {

        return new LinkedListIterator(); 

    }

    private class LinkedListIterator implements CustomIterator<T> {
            private Node current = head;

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new IndexOutOfBoundsException("No more elements in the list");
                }
                T data = current.data;
                current = current.next;
                return data;
            }
        };
    }
    
