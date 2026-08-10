package ug.edu.ugmc.optimizer.datastructures.hashing;

/**
 * Custom Hash Table data structure for the UGMC Optimizer.
 * Engineered for O(1) lookups of Patient IDs and ServiceRequests.
 * Implements Separate Chaining for collision resolution.
 */
public class CustomHashTable<K, V> {

    // Universal Parameter assigned to Precious
    private static final int BASE_CAPACITY = (22176813 % 97) + 50;

    /**
     * Internal node to handle Key-Value mapping for Separate Chaining.
     */
    private static class HashNode<K, V> {
        K key;
        V value;
        HashNode<K, V> next;

        public HashNode(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    private HashNode<K, V>[] buckets;
    private int size;
    
    // Tracking Requirement for Module 10 Empirical Lab
    private int collisionCount;

    @SuppressWarnings("unchecked")
    public CustomHashTable() {
        // Enforcing the calculated base capacity
        this.buckets = new HashNode[BASE_CAPACITY];
        this.size = 0;
        this.collisionCount = 0;
    }

    /**
     * Generates a positive array index for the given key.
     */
    private int getBucketIndex(K key) {
        int hashCode = key.hashCode();
        int index = hashCode % buckets.length;
        return index < 0 ? index * -1 : index;
    }

    /**
     * Inserts a Key-Value pair into the Hash Table.
     * Updates the value if the key already exists.
     * 
     * @param key The identifier (e.g., Patient ID)
     * @param value The object to store (e.g., ServiceRequest)
     */
    public void put(K key, V value) {
        if (key == null) throw new IllegalArgumentException("Key cannot be null");

        int bucketIndex = getBucketIndex(key);
        HashNode<K, V> head = buckets[bucketIndex];

        // Check if key already exists, if so, update value
        HashNode<K, V> current = head;
        while (current != null) {
            if (current.key.equals(key)) {
                current.value = value;
                return;
            }
            current = current.next;
        }

        // Insert new node at the head of the chain
        HashNode<K, V> newNode = new HashNode<>(key, value);
        if (head != null) {
            // If the bucket is not empty, a collision has explicitly occurred
            collisionCount++;
            newNode.next = head;
        }
        buckets[bucketIndex] = newNode;
        size++;
    }

    /**
     * Retrieves the value associated with the given key.
     * 
     * @param key The identifier to search for
     * @return The associated value, or null if not found
     */
    public V get(K key) {
        if (key == null) return null;

        int bucketIndex = getBucketIndex(key);
        HashNode<K, V> head = buckets[bucketIndex];

        // Traverse the chain
        while (head != null) {
            if (head.key.equals(key)) {
                return head.value;
            }
            head = head.next;
        }
        return null; // Key not found
    }

    /**
     * Removes the Key-Value pair from the Hash Table.
     * 
     * @param key The identifier to remove
     */
    public void remove(K key) {
        if (key == null) return;

        int bucketIndex = getBucketIndex(key);
        HashNode<K, V> head = buckets[bucketIndex];
        HashNode<K, V> prev = null;

        while (head != null) {
            if (head.key.equals(key)) {
                if (prev != null) {
                    // Node is in the middle or end of the chain
                    prev.next = head.next;
                } else {
                    // Node is the head of the chain
                    buckets[bucketIndex] = head.next;
                }
                size--;
                return;
            }
            prev = head;
            head = head.next;
        }
    }

    /**
     * Returns the exact number of collisions that have occurred.
     */
    public int getCollisionCount() {
        return collisionCount;
    }

    public int getSize() {
        return size;
    }
    
    public boolean isEmpty() {
        return size == 0;
    }
}