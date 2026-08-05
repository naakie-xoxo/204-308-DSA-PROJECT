package ug.edu.ugmc.optimizer.datastructures.hashing;

/**
 * CustomHashTable
 * ----------------
 * A generic hash table implemented from scratch using separate chaining
 * (singly linked nodes per bucket) for collision resolution. No java.util
 * Map/HashMap classes are used — buckets are plain Object arrays of
 * hand-rolled Node references, per project constraints.
 *
 * Assigned to: Precious
 * Index number: 22176813
 *
 * Derivation of initial capacity (per Official Universal Parameter Mapping,
 * item 4, Group A):
 *      INITIAL_CAPACITY = (22176813 % 97) + 50
 *                       = 91 + 50
 *                       = 141
 *
 * Note: 141 = 3 * 47, so it is not actually prime despite the brief's
 * comment about "ensuring a prime number base." The formula is applied
 * exactly as specified in the brief; if a genuinely prime base is required
 * for the oral defence, nextPrime(141) = 149 could be substituted instead.
 * A helper (findNextPrime) is included below in case you want to swap it in.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class CustomHashTable<K, V> {

    /** Index number this class's sizing parameter is derived from. */
    private static final long ASSIGNED_INDEX_NUMBER = 22176813L;

    /** Derived initial capacity: (22176813 % 97) + 50 = 141. */
    private static final int INITIAL_CAPACITY =
            (int) (ASSIGNED_INDEX_NUMBER % 97) + 50;

    /** Resize when load factor (size / capacity) exceeds this threshold. */
    private static final double LOAD_FACTOR_THRESHOLD = 0.75;

    /** Single linked-list node holding one key-value pair in a bucket. */
    private class Node {
        final K key;
        V value;
        Node next;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
            this.next = null;
        }
    }

    private Node[] buckets;
    private int capacity;
    private int size;

    @SuppressWarnings("unchecked")
    public CustomHashTable() {
        this.capacity = INITIAL_CAPACITY;
        this.buckets = (Node[]) new CustomHashTable<?, ?>.Node[capacity];
        this.size = 0;
    }

    /**
     * Overload for testing/explicit sizing (e.g. QA forcing a small
     * capacity to exercise collision handling). Production code should
     * still use the no-arg constructor so the index-derived capacity
     * (141) is what actually ships.
     */
    @SuppressWarnings("unchecked")
    public CustomHashTable(int explicitCapacity) {
        if (explicitCapacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive.");
        }
        this.capacity = explicitCapacity;
        this.buckets = (Node[]) new CustomHashTable<?, ?>.Node[capacity];
        this.size = 0;
    }

    /**
     * Computes a bucket index for a given key using Java's hashCode(),
     * folded into a non-negative index within table bounds.
     */
    private int indexFor(K key, int tableCapacity) {
        if (key == null) {
            throw new IllegalArgumentException("Null keys are not supported.");
        }
        int hash = key.hashCode();
        // Fold to non-negative before modulo to avoid negative bucket indices.
        hash = hash ^ (hash >>> 16);
        return Math.abs(hash) % tableCapacity;
    }

    /** Inserts or updates a key-value pair. */
    public void put(K key, V value) {
        if (size + 1 > capacity * LOAD_FACTOR_THRESHOLD) {
            resize();
        }

        int idx = indexFor(key, capacity);
        Node current = buckets[idx];

        // Update existing key if present.
        while (current != null) {
            if (current.key.equals(key)) {
                current.value = value;
                return;
            }
            current = current.next;
        }

        // Insert new node at head of bucket's chain.
        Node newNode = new Node(key, value);
        newNode.next = buckets[idx];
        buckets[idx] = newNode;
        size++;
    }

    /** Retrieves the value for a key, or null if absent. */
    public V get(K key) {
        int idx = indexFor(key, capacity);
        Node current = buckets[idx];
        while (current != null) {
            if (current.key.equals(key)) {
                return current.value;
            }
            current = current.next;
        }
        return null;
    }

    /** Returns true if the key exists in the table. */
    public boolean containsKey(K key) {
        int idx = indexFor(key, capacity);
        Node current = buckets[idx];
        while (current != null) {
            if (current.key.equals(key)) {
                return true;
            }
            current = current.next;
        }
        return false;
    }

    /** Removes a key-value pair, returning true if something was removed. */
    public boolean remove(K key) {
        int idx = indexFor(key, capacity);
        Node current = buckets[idx];
        Node previous = null;

        while (current != null) {
            if (current.key.equals(key)) {
                if (previous == null) {
                    buckets[idx] = current.next;
                } else {
                    previous.next = current.next;
                }
                size--;
                return true;
            }
            previous = current;
            current = current.next;
        }
        return false;
    }

    /** Doubles table capacity and rehashes all existing entries. */
    @SuppressWarnings("unchecked")
    private void resize() {
        int newCapacity = capacity * 2;
        Node[] newBuckets = (Node[]) new CustomHashTable<?, ?>.Node[newCapacity];

        for (int i = 0; i < capacity; i++) {
            Node current = buckets[i];
            while (current != null) {
                Node next = current.next; // save before relinking
                int newIdx = indexFor(current.key, newCapacity);
                current.next = newBuckets[newIdx];
                newBuckets[newIdx] = current;
                current = next;
            }
        }

        buckets = newBuckets;
        capacity = newCapacity;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int getCapacity() {
        return capacity;
    }

    /**
     * Optional helper: finds the next prime number >= n. Not used by default
     * since the brief's formula is applied as-is, but available if the team
     * decides to enforce a genuinely prime capacity for the write-up.
     */
    public static int findNextPrime(int n) {
        if (n <= 2) {
            return 2;
        }
        int candidate = (n % 2 == 0) ? n + 1 : n;
        while (!isPrime(candidate)) {
            candidate += 2;
        }
        return candidate;
    }

    private static boolean isPrime(int n) {
        if (n < 2) {
            return false;
        }
        for (int i = 2; (long) i * i <= n; i++) {
            if (n % i == 0) {
                return false;
            }
        }
        return true;
    }
}
