package ug.edu.ugmc.optimizer.datastructures.trees;

/**
 * B-tree index over hospital records, the disk-oriented counterpart to {@link RedBlackTree}.
 *
 * <p>Both structures answer the same ordered queries in O(log n). They differ in shape: the
 * red-black tree stores one key per node and grows tall, while this tree packs up to
 * {@value #MAX_KEYS} keys into every node and stays very shallow. That matters once hospital
 * records outgrow memory, because tree height is the number of page reads a lookup costs.
 * With a minimum degree of {@value #MIN_DEGREE}, roughly 50,000 records fit in a tree of
 * height 5 or less.
 *
 * <p>Balance is maintained by node splitting on the way down: before descending into any
 * child that is already full, that child is split and its median key is promoted into the
 * parent. Because a node is never allowed to overflow, insertion needs only one downward
 * pass and never has to back up. Growth therefore happens at the root, which is why every
 * leaf stays at exactly the same depth.
 *
 * <p>Structural invariants for a tree of minimum degree t:
 *
 * <ol>
 *   <li>Every node holds at most 2t-1 = {@value #MAX_KEYS} keys, in ascending order.</li>
 *   <li>Every node except the root holds at least t-1 = {@value #MIN_KEYS} keys.</li>
 *   <li>An internal node holding k keys has exactly k+1 children.</li>
 *   <li>Keys in child i fall strictly between key i-1 and key i of the parent.</li>
 *   <li>All leaves sit at the same depth.</li>
 * </ol>
 *
 * <p>Complexity: {@code search}, {@code insert}, {@code contains}, {@code min}, {@code max}
 * are O(t * log_t n) worst case. {@code inorderTraversal} and {@code keysInOrder} are O(n).
 * Space is O(n). {@link #validate()} re-checks all five invariants in O(n).
 *
 * <p>No {@code java.util} collection is used; only plain arrays and nodes written here.
 *
 * @param <K> ordered key type, typically a record ID
 * @param <V> stored record type
 */
public class BTree<K extends Comparable<K>, V> {

    /**
     * Index number 22018389 (Somuah), assigned as this project's B-tree minimum degree
     * parameter. The brief requires structural constants to be derived from team index
     * numbers rather than chosen arbitrarily.
     */
    private static final int SOMUAH_INDEX = 22018389;

    /**
     * Minimum degree t, derived as (22018389 % 4) + 3 = 4 per the assigned parameter mapping.
     * Every node therefore holds 3 to 7 keys and an internal node has 4 to 8 children.
     */
    public static final int MIN_DEGREE = (SOMUAH_INDEX % 4) + 3;

    /** Maximum keys per node, 2t-1 = 7. A node reaching this count must split before reuse. */
    public static final int MAX_KEYS = (2 * MIN_DEGREE) - 1;

    /** Minimum keys per non-root node, t-1 = 3. */
    public static final int MIN_KEYS = MIN_DEGREE - 1;

    /** Maximum children per internal node, 2t = 8. */
    public static final int MAX_CHILDREN = 2 * MIN_DEGREE;

    /**
     * One B-tree page. Keys, values, and children are fixed-size arrays sized by the degree,
     * so a node never reallocates; only {@code numKeys} moves.
     */
    private static final class Node<K, V> {
        private final K[] keys;
        private final V[] values;
        private final Node<K, V>[] children;
        private int numKeys;
        private final boolean leaf;

        @SuppressWarnings("unchecked")
        private Node(boolean leaf) {
            this.leaf = leaf;
            this.keys = (K[]) new Comparable[MAX_KEYS];
            this.values = (V[]) new Object[MAX_KEYS];
            this.children = new Node[MAX_CHILDREN];
            this.numKeys = 0;
        }

        private boolean isFull() {
            return numKeys == MAX_KEYS;
        }
    }

    private Node<K, V> root;
    private int size;

    // Empirical-lab counters, reported alongside the red-black tree's rotation counts.
    private long splitCount;
    private long comparisons;

    /**
     * Inserts a record, or replaces the value if the key is already indexed.
     *
     * @param key   record ID; must not be null
     * @param value record payload; may be null
     * @throws IllegalArgumentException if {@code key} is null
     */
    public void insert(K key, V value) {
        if (key == null) {
            throw new IllegalArgumentException("B-tree keys cannot be null.");
        }

        // Handled up front so the proactive-split descent below never has to reason about a
        // duplicate that has already been promoted into an internal node.
        Node<K, V> existing = findNode(key);
        if (existing != null) {
            existing.values[indexOfKey(existing, key)] = value;
            return;
        }

        if (root == null) {
            root = new Node<>(true);
            root.keys[0] = key;
            root.values[0] = value;
            root.numKeys = 1;
            size++;
            return;
        }

        // The only way the tree gets taller: a full root splits into a new single-key root.
        // Because this happens at the top, every leaf still ends up at the same depth.
        if (root.isFull()) {
            Node<K, V> newRoot = new Node<>(false);
            newRoot.children[0] = root;
            splitChild(newRoot, 0);
            root = newRoot;
        }

        insertNonFull(root, key, value);
        size++;
    }

    /**
     * Inserts into a subtree whose root is guaranteed not to be full.
     *
     * <p>In a leaf the key is placed directly. In an internal node the correct child is
     * located, split first if it is full, and then descended into. The "not full" guarantee
     * is what makes a single downward pass sufficient.
     */
    private void insertNonFull(Node<K, V> node, K key, V value) {
        int i = node.numKeys - 1;

        if (node.leaf) {
            // Shift larger keys one slot right, then drop the new key into the gap.
            while (i >= 0) {
                comparisons++;
                if (key.compareTo(node.keys[i]) >= 0) {
                    break;
                }
                node.keys[i + 1] = node.keys[i];
                node.values[i + 1] = node.values[i];
                i--;
            }
            node.keys[i + 1] = key;
            node.values[i + 1] = value;
            node.numKeys++;
            return;
        }

        while (i >= 0) {
            comparisons++;
            if (key.compareTo(node.keys[i]) >= 0) {
                break;
            }
            i--;
        }
        int childIndex = i + 1;

        if (node.children[childIndex].isFull()) {
            splitChild(node, childIndex);
            // The split promoted a median into this node; the target child may now be the
            // new right sibling instead.
            comparisons++;
            if (key.compareTo(node.keys[childIndex]) > 0) {
                childIndex++;
            }
        }

        insertNonFull(node.children[childIndex], key, value);
    }

    /**
     * Splits the full child at {@code childIndex} into two nodes of t-1 keys each and
     * promotes its median key into {@code parent}.
     *
     * <p>With t = {@value #MIN_DEGREE} a full child holds 7 keys. Keys 0-2 stay on the left,
     * key 3 is promoted, and keys 4-6 move to a new right sibling, leaving both halves at the
     * legal minimum of {@value #MIN_KEYS}. The parent must not be full when this is called,
     * which the descent in {@link #insertNonFull} guarantees. Runs in O(t).
     */
    private void splitChild(Node<K, V> parent, int childIndex) {
        Node<K, V> full = parent.children[childIndex];
        Node<K, V> sibling = new Node<>(full.leaf);
        sibling.numKeys = MIN_KEYS;

        // Upper half of the keys moves to the new sibling.
        for (int j = 0; j < MIN_KEYS; j++) {
            sibling.keys[j] = full.keys[j + MIN_DEGREE];
            sibling.values[j] = full.values[j + MIN_DEGREE];
            full.keys[j + MIN_DEGREE] = null;
            full.values[j + MIN_DEGREE] = null;
        }

        // Internal nodes carry their matching children across too.
        if (!full.leaf) {
            for (int j = 0; j < MIN_DEGREE; j++) {
                sibling.children[j] = full.children[j + MIN_DEGREE];
                full.children[j + MIN_DEGREE] = null;
            }
        }

        K medianKey = full.keys[MIN_KEYS];
        V medianValue = full.values[MIN_KEYS];
        full.keys[MIN_KEYS] = null;
        full.values[MIN_KEYS] = null;
        full.numKeys = MIN_KEYS;

        // Open a slot in the parent for the new sibling and the promoted median.
        for (int j = parent.numKeys; j >= childIndex + 1; j--) {
            parent.children[j + 1] = parent.children[j];
        }
        parent.children[childIndex + 1] = sibling;

        for (int j = parent.numKeys - 1; j >= childIndex; j--) {
            parent.keys[j + 1] = parent.keys[j];
            parent.values[j + 1] = parent.values[j];
        }
        parent.keys[childIndex] = medianKey;
        parent.values[childIndex] = medianValue;
        parent.numKeys++;

        splitCount++;
    }

    /**
     * Looks up a record by ID.
     *
     * @return the stored value, or null if the key is absent or was stored with a null value
     * @throws IllegalArgumentException if {@code key} is null
     */
    public V search(K key) {
        Node<K, V> node = findNode(key);
        return node == null ? null : node.values[indexOfKey(node, key)];
    }

    /**
     * Reports whether a record ID is indexed. Unlike {@link #search}, this distinguishes an
     * absent key from a key stored with a null value.
     *
     * @throws IllegalArgumentException if {@code key} is null
     */
    public boolean contains(K key) {
        return findNode(key) != null;
    }

    /** Walks down from the root, scanning each node's keys, until the key is found. */
    private Node<K, V> findNode(K key) {
        if (key == null) {
            throw new IllegalArgumentException("B-tree keys cannot be null.");
        }
        Node<K, V> current = root;
        while (current != null) {
            int i = 0;
            while (i < current.numKeys) {
                comparisons++;
                if (key.compareTo(current.keys[i]) <= 0) {
                    break;
                }
                i++;
            }
            if (i < current.numKeys && key.compareTo(current.keys[i]) == 0) {
                return current;
            }
            if (current.leaf) {
                return null;
            }
            current = current.children[i];
        }
        return null;
    }

    private int indexOfKey(Node<K, V> node, K key) {
        for (int i = 0; i < node.numKeys; i++) {
            if (key.compareTo(node.keys[i]) == 0) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the smallest indexed key, found by walking the leftmost spine.
     *
     * @throws IllegalStateException if the index is empty
     */
    public K min() {
        if (root == null) {
            throw new IllegalStateException("Cannot read the minimum key of an empty index.");
        }
        Node<K, V> current = root;
        while (!current.leaf) {
            current = current.children[0];
        }
        return current.keys[0];
    }

    /**
     * Returns the largest indexed key, found by walking the rightmost spine.
     *
     * @throws IllegalStateException if the index is empty
     */
    public K max() {
        if (root == null) {
            throw new IllegalStateException("Cannot read the maximum key of an empty index.");
        }
        Node<K, V> current = root;
        while (!current.leaf) {
            current = current.children[current.numKeys];
        }
        return current.keys[current.numKeys - 1];
    }

    /** Number of indexed records. */
    public int size() {
        return size;
    }

    /** True when no record has been indexed. */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Edge-count height; -1 for an empty index, 0 for a single-node tree. Because all leaves
     * are at the same depth, following the leftmost spine is enough to measure it.
     */
    public int height() {
        if (root == null) {
            return -1;
        }
        int levels = 0;
        Node<K, V> current = root;
        while (!current.leaf) {
            current = current.children[0];
            levels++;
        }
        return levels;
    }

    /**
     * Returns the indexed keys in ascending order as a single space-separated string, matching
     * the traversal format used by {@link RedBlackTree}. Empty index returns "".
     */
    public String inorderTraversal() {
        StringBuilder builder = new StringBuilder();
        appendInorder(root, builder);
        return builder.toString().trim();
    }

    /**
     * In-order walk for a multi-key node: child 0, key 0, child 1, key 1, ..., last child.
     */
    private void appendInorder(Node<K, V> node, StringBuilder builder) {
        if (node == null) {
            return;
        }
        for (int i = 0; i < node.numKeys; i++) {
            if (!node.leaf) {
                appendInorder(node.children[i], builder);
            }
            builder.append(node.keys[i]).append(' ');
        }
        if (!node.leaf) {
            appendInorder(node.children[node.numKeys], builder);
        }
    }

    /**
     * Returns every key in ascending order.
     *
     * <p>Plain array rather than a {@code java.util.List} so the assessed structure stays free
     * of built-in collections. The declared element type is {@code Object} rather than
     * {@code K} on purpose: Java erases K at runtime, so this method cannot create a genuine
     * {@code K[]}, and declaring one would let {@code String[] ids = index.keysInOrder()}
     * compile and then fail with a ClassCastException. Cast the elements, not the array.
     */
    public Object[] keysInOrder() {
        Object[] keys = new Object[size];
        fillInorder(root, keys, 0);
        return keys;
    }

    private int fillInorder(Node<K, V> node, Object[] target, int nextSlot) {
        if (node == null) {
            return nextSlot;
        }
        for (int i = 0; i < node.numKeys; i++) {
            if (!node.leaf) {
                nextSlot = fillInorder(node.children[i], target, nextSlot);
            }
            target[nextSlot++] = node.keys[i];
        }
        if (!node.leaf) {
            nextSlot = fillInorder(node.children[node.numKeys], target, nextSlot);
        }
        return nextSlot;
    }

    /**
     * Re-checks all five B-tree invariants from scratch, including that every leaf sits at
     * the same depth.
     *
     * <p>Kept in production code rather than the test folder so the console demonstration can
     * prove balance to the examiner after a live insertion. O(n).
     *
     * @return true when the structure is a valid B-tree of minimum degree {@value #MIN_DEGREE}
     */
    public boolean validate() {
        if (root == null) {
            return size == 0;
        }
        // The root is the one node allowed fewer than MIN_KEYS, but it must hold at least one.
        if (root.numKeys < 1 || root.numKeys > MAX_KEYS) {
            return false;
        }
        return checkSubtree(root, null, null, 0, leafDepth()) && countKeys(root) == size;
    }

    /** Depth of the leftmost leaf, used as the reference every other leaf must match. */
    private int leafDepth() {
        int depth = 0;
        Node<K, V> current = root;
        while (!current.leaf) {
            current = current.children[0];
            depth++;
        }
        return depth;
    }

    private boolean checkSubtree(
            Node<K, V> node, K exclusiveLow, K exclusiveHigh, int depth, int expectedLeafDepth) {

        if (node.numKeys > MAX_KEYS) {
            return false; // Invariant 1.
        }
        if (node != root && node.numKeys < MIN_KEYS) {
            return false; // Invariant 2.
        }

        // Keys ascending within the node, and inside the range this subtree may occupy.
        for (int i = 0; i < node.numKeys; i++) {
            if (node.keys[i] == null) {
                return false;
            }
            if (i > 0 && node.keys[i - 1].compareTo(node.keys[i]) >= 0) {
                return false;
            }
            if (exclusiveLow != null && node.keys[i].compareTo(exclusiveLow) <= 0) {
                return false; // Invariant 4.
            }
            if (exclusiveHigh != null && node.keys[i].compareTo(exclusiveHigh) >= 0) {
                return false; // Invariant 4.
            }
        }

        if (node.leaf) {
            return depth == expectedLeafDepth; // Invariant 5.
        }

        // Invariant 3: k keys means exactly k+1 children, and no stragglers beyond that.
        for (int i = 0; i <= node.numKeys; i++) {
            if (node.children[i] == null) {
                return false;
            }
        }
        for (int i = node.numKeys + 1; i < MAX_CHILDREN; i++) {
            if (node.children[i] != null) {
                return false;
            }
        }

        for (int i = 0; i <= node.numKeys; i++) {
            K childLow = i == 0 ? exclusiveLow : node.keys[i - 1];
            K childHigh = i == node.numKeys ? exclusiveHigh : node.keys[i];
            if (!checkSubtree(node.children[i], childLow, childHigh, depth + 1, expectedLeafDepth)) {
                return false;
            }
        }
        return true;
    }

    private int countKeys(Node<K, V> node) {
        if (node == null) {
            return 0;
        }
        int total = node.numKeys;
        if (!node.leaf) {
            for (int i = 0; i <= node.numKeys; i++) {
                total += countKeys(node.children[i]);
            }
        }
        return total;
    }

    /** Node splits performed so far; recorded for the empirical efficiency lab. */
    public long getSplitCount() {
        return splitCount;
    }

    /** Key comparisons made by {@code insert}, {@code search}, and {@code contains}. */
    public long getComparisons() {
        return comparisons;
    }

    /** Zeroes the empirical counters so a benchmark run can measure one workload cleanly. */
    public void resetCounters() {
        splitCount = 0;
        comparisons = 0;
    }
}
