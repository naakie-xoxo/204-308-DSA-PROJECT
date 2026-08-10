package ug.edu.ugmc.optimizer.datastructures.linear;
 @SuppressWarnings("unchecked")
public class DynamicArray <T>{ 
    private T[] array;
    private int size;
    private int capacity;

    // Constructor to initialize the dynamic array with default capacity
   
    public DynamicArray() {
        this.capacity = 10; // Initial capacity
        this.size = 0;
        this.array = (T[]) new Object[capacity];
    }

    // insert method to add an element to the dynamic array
    public void insert(T element) {
        if (size == capacity) {
            resize();
        }
        array[size] = element;
        size++;
    }

    // get method to retrieve an element at a specific index
    public T get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        return array[index];
    }

    // setmethod to set an element at a specific index
    public void set(int index, T element) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        array[index] = element;
    }

    // remove method to remove an element at a specific index
    public void remove(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        for (int i = index; i < size - 1; i++) {
            array[i] = array[i + 1];
        }
        array[size - 1] = null; // Clear the last element
        size--;
    }

    // return the current size of the dynamic array
    public int size() {
        return size;
    }

    // resize method to increase the capacity of the dynamic array
    private void resize() {
        capacity += ((22027250 % 50) + 10);
        T[] newArray = (T[]) new Object[capacity];
        
        // Copy all existing elements into the new array
        for (int i = 0; i < size; i++) {
            newArray[i] = array[i];
        } 
        
        // Reassign the array reference AFTER the copy loop finishes
        array = newArray;
    }

    
}