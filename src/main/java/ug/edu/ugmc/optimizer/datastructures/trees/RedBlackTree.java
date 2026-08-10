package ug.edu.ugmc.optimizer.datastructures.trees;

/**
 * Self-balancing red-black tree used as the primary ordered index for hospital records.
 *
 * <p>A hash table gives O(1) point lookup but cannot answer ordered questions. This index
 * exists so the system can also ask "list every record between two IDs" and "give me the
 * smallest/largest key" in guaranteed O(log n) time, which is what ward and admission
 * reports need.
 *
 * <p>The implementation is the classic (CLRS) red-black tree, not the left-leaning variant,
 * so both left and right rotations appear explicitly. The five invariants maintained are:
 *
 * <ol>
 *   <li>Every node is either red or black.</li>
 *   <li>The root is black.</li>
 *   <li>Null leaves count as black.</li>
 *   <li>A red node never has a red child (no two reds in a row).</li>
 *   <li>Every path from a node down to a null leaf contains the same number of black nodes.</li>
 * </ol>
 *
 * <p>Invariants 4 and 5 together bound the height at 2*log2(n+1), which is what makes the
 * O(log n) worst case hold. {@link #validate()} re-checks all five and is used by the tests.
 *
 * <p>Complexity: {@code put}, {@code get}, {@code contains}, {@code min}, {@code max} are
 * O(log n) worst case. {@code inorderTraversal} and {@code keysInOrder} are O(n).
 * {@code rangeSearch} is O(log n + k) for k reported keys. Space is O(n).
 *
 * <p>No {@code java.util} collection is used; only plain arrays and nodes written here.
 *
 * @param <K> ordered key type, typically a record ID
 * @param <V> stored record type
 */
public class RedBlackTree<K extends Comparable<K>, V> {

    private static final boolean RED = true;
    private static final boolean BLACK = false;

    /** A single record slot in the index. */
    private static final class Node<K, V> {
        private K key;
        private V value;
        private Node<K, V> left;
        private Node<K, V> right;
        private Node<K, V> parent;
        private boolean color;

        private Node(K key, V value, Node<K, V> parent) {
            this.key = key;
            this.value = value;
            this.parent = parent;
            this.color = RED; // New nodes start red so invariant 5 is never broken on insert.
        }
    }

    private Node<K, V> root;
    private int size;

    // Empirical-lab counters. The final report compares these against the hash table's
    // collision count, so they are exposed rather than kept private to the tests.
    private long leftRotations;
    private long rightRotations;
    private long recolorings;
    private long comparisons;

    /**
     * Inserts a record, or replaces the value if the key is already indexed.
     *
     * @param key   record ID; must not be null
     * @param value record payload; may be null
     * @throws IllegalArgumentException if {@code key} is null
     */
    public void put(K key, V value) {
        if (key == null) {
            throw new IllegalArgumentException("Red-black tree keys cannot be null.");
        }

        if (root == null) {
            root = new Node<>(key, value, null);
            root.color = BLACK; // Invariant 2.
            recolorings++;
            size++;
            return;
        }

        // Standard BST descent, remembering the parent so the new node can be linked back.
        Node<K, V> current = root;
        Node<K, V> parent = null;
        int direction = 0;
        while (current != null) {
            comparisons++;
            direction = key.compareTo(current.key);
            if (direction == 0) {
                current.value = value; // Update in place; shape and colors are untouched.
                return;
            }
            parent = current;
            current = direction < 0 ? current.left : current.right;
        }

        Node<K, V> inserted = new Node<>(key, value, parent);
        if (direction < 0) {
            parent.left = inserted;
        } else {
            parent.right = inserted;
        }
        size++;

        insertFixup(inserted);
    }

    /**
     * Restores the red-black invariants after a red node has been attached.
     *
     * <p>Only invariant 4 can be broken here, and only between the new node and its parent.
     * Three cases handle it, mirrored for the left and right side:
     *
     * <ul>
     *   <li>Case 1 — red uncle: recolor parent and uncle black, grandparent red, then repeat
     *       from the grandparent. This pushes the violation two levels up.</li>
     *   <li>Case 2 — black uncle, node is the "inner" child: rotate the parent so the three
     *       nodes form a straight line, reducing to case 3.</li>
     *   <li>Case 3 — black uncle, node is the "outer" child: recolor and rotate the
     *       grandparent once. This terminates the loop.</li>
     * </ul>
     *
     * <p>At most two rotations ever occur per insertion, so the fixup is O(log n) from the
     * case-1 walk upward and O(1) in rotations.
     */
    private void insertFixup(Node<K, V> node) {
        while (isRed(node.parent)) {
            Node<K, V> parent = node.parent;
            Node<K, V> grandparent = parent.parent;
            // grandparent is never null here: parent is red, so parent is not the root.

            if (parent == grandparent.left) {
                Node<K, V> uncle = grandparent.right;
                if (isRed(uncle)) {
                    parent.color = BLACK;
                    uncle.color = BLACK;
                    grandparent.color = RED;
                    recolorings += 3;
                    node = grandparent;
                } else {
                    if (node == parent.right) {
                        node = parent;
                        rotateLeft(node);
                        parent = node.parent;
                        grandparent = parent.parent;
                    }
                    parent.color = BLACK;
                    grandparent.color = RED;
                    recolorings += 2;
                    rotateRight(grandparent);
                }
            } else {
                Node<K, V> uncle = grandparent.left;
                if (isRed(uncle)) {
                    parent.color = BLACK;
                    uncle.color = BLACK;
                    grandparent.color = RED;
                    recolorings += 3;
                    node = grandparent;
                } else {
                    if (node == parent.left) {
                        node = parent;
                        rotateRight(node);
                        parent = node.parent;
                        grandparent = parent.parent;
                    }
                    parent.color = BLACK;
                    grandparent.color = RED;
                    recolorings += 2;
                    rotateLeft(grandparent);
                }
            }
        }

        // Case 1 can leave the root red; forcing it black adds one to every path's black
        // count, so invariant 5 stays satisfied.
        if (isRed(root)) {
            root.color = BLACK;
            recolorings++;
        }
    }

    /**
     * Rotates left around {@code pivot}: its right child moves up and takes its place.
     * In-order position of every key is preserved. Runs in O(1).
     */
    private void rotateLeft(Node<K, V> pivot) {
        Node<K, V> riser = pivot.right;
        pivot.right = riser.left;
        if (riser.left != null) {
            riser.left.parent = pivot;
        }
        riser.parent = pivot.parent;
        if (pivot.parent == null) {
            root = riser;
        } else if (pivot == pivot.parent.left) {
            pivot.parent.left = riser;
        } else {
            pivot.parent.right = riser;
        }
        riser.left = pivot;
        pivot.parent = riser;
        leftRotations++;
    }

    /**
     * Rotates right around {@code pivot}: its left child moves up and takes its place.
     * Exact mirror of {@link #rotateLeft}. Runs in O(1).
     */
    private void rotateRight(Node<K, V> pivot) {
        Node<K, V> riser = pivot.left;
        pivot.left = riser.right;
        if (riser.right != null) {
            riser.right.parent = pivot;
        }
        riser.parent = pivot.parent;
        if (pivot.parent == null) {
            root = riser;
        } else if (pivot == pivot.parent.right) {
            pivot.parent.right = riser;
        } else {
            pivot.parent.left = riser;
        }
        riser.right = pivot;
        pivot.parent = riser;
        rightRotations++;
    }

    private boolean isRed(Node<K, V> node) {
        return node != null && node.color == RED;
    }

    /**
     * Looks up a record by ID.
     *
     * @return the stored value, or null if the key is absent or was stored with a null value
     * @throws IllegalArgumentException if {@code key} is null
     */
    public V get(K key) {
        Node<K, V> node = findNode(key);
        return node == null ? null : node.value;
    }

    /**
     * Reports whether a record ID is indexed. Unlike {@link #get}, this distinguishes an
     * absent key from a key stored with a null value.
     *
     * @throws IllegalArgumentException if {@code key} is null
     */
    public boolean contains(K key) {
        return findNode(key) != null;
    }

    private Node<K, V> findNode(K key) {
        if (key == null) {
            throw new IllegalArgumentException("Red-black tree keys cannot be null.");
        }
        Node<K, V> current = root;
        while (current != null) {
            comparisons++;
            int direction = key.compareTo(current.key);
            if (direction == 0) {
                return current;
            }
            current = direction < 0 ? current.left : current.right;
        }
        return null;
    }

    /**
     * Returns the smallest indexed key.
     *
     * @throws IllegalStateException if the index is empty
     */
    public K min() {
        if (root == null) {
            throw new IllegalStateException("Cannot read the minimum key of an empty index.");
        }
        Node<K, V> current = root;
        while (current.left != null) {
            current = current.left;
        }
        return current.key;
    }

    /**
     * Returns the largest indexed key.
     *
     * @throws IllegalStateException if the index is empty
     */
    public K max() {
        if (root == null) {
            throw new IllegalStateException("Cannot read the maximum key of an empty index.");
        }
        Node<K, V> current = root;
        while (current.right != null) {
            current = current.right;
        }
        return current.key;
    }

    /** Number of indexed records. */
    public int size() {
        return size;
    }

    /** True when no record has been indexed. */
    public boolean isEmpty() {
        return size == 0;
    }

    /** Edge-count height of the tree; -1 for an empty index, 0 for a single node. */
    public int height() {
        return heightOf(root);
    }

    private int heightOf(Node<K, V> node) {
        if (node == null) {
            return -1;
        }
        int left = heightOf(node.left);
        int right = heightOf(node.right);
        return 1 + Math.max(left, right);
    }

    /**
     * Number of black nodes on any root-to-leaf path, excluding the null leaves. Invariant 5
     * guarantees this is the same for every path, so it is well defined. Returns 0 when empty.
     */
    public int blackHeight() {
        int count = 0;
        Node<K, V> current = root;
        while (current != null) {
            if (current.color == BLACK) {
                count++;
            }
            current = current.left;
        }
        return count;
    }

    /**
     * Returns the indexed keys in ascending order as a single space-separated string, matching
     * the traversal format the rest of the team's tree tests expect. Empty index returns "".
     */
    public String inorderTraversal() {
        StringBuilder builder = new StringBuilder();
        appendInorder(root, builder);
        return builder.toString().trim();
    }

    private void appendInorder(Node<K, V> node, StringBuilder builder) {
        if (node == null) {
            return;
        }
        appendInorder(node.left, builder);
        builder.append(node.key).append(' ');
        appendInorder(node.right, builder);
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
        nextSlot = fillInorder(node.left, target, nextSlot);
        target[nextSlot++] = node.key;
        return fillInorder(node.right, target, nextSlot);
    }

    /**
     * Returns every key in {@code [low, high]} in ascending order, both bounds inclusive.
     *
     * <p>This is the operation that justifies keeping a balanced tree alongside the hash
     * table: subtrees that cannot contain a match are skipped, so the cost is O(log n + k)
     * for k reported keys rather than the O(n) scan a hash table would force.
     *
     * <p>Returns {@code Object[]} for the erasure reason explained on {@link #keysInOrder()}.
     *
     * @throws IllegalArgumentException if either bound is null, or if {@code low > high}
     */
    public Object[] rangeSearch(K low, K high) {
        if (low == null || high == null) {
            throw new IllegalArgumentException("Range bounds cannot be null.");
        }
        if (low.compareTo(high) > 0) {
            throw new IllegalArgumentException(
                    "Invalid range: low bound " + low + " is greater than high bound " + high + ".");
        }

        int matches = countRange(root, low, high);
        Object[] found = new Object[matches];
        collectRange(root, low, high, found, 0);
        return found;
    }

    private int countRange(Node<K, V> node, K low, K high) {
        if (node == null) {
            return 0;
        }
        int total = 0;
        boolean aboveLow = node.key.compareTo(low) >= 0;
        boolean belowHigh = node.key.compareTo(high) <= 0;
        if (aboveLow) {
            total += countRange(node.left, low, high);
        }
        if (aboveLow && belowHigh) {
            total++;
        }
        if (belowHigh) {
            total += countRange(node.right, low, high);
        }
        return total;
    }

    private int collectRange(Node<K, V> node, K low, K high, Object[] target, int nextSlot) {
        if (node == null) {
            return nextSlot;
        }
        boolean aboveLow = node.key.compareTo(low) >= 0;
        boolean belowHigh = node.key.compareTo(high) <= 0;
        if (aboveLow) {
            nextSlot = collectRange(node.left, low, high, target, nextSlot);
        }
        if (aboveLow && belowHigh) {
            target[nextSlot++] = node.key;
        }
        if (belowHigh) {
            nextSlot = collectRange(node.right, low, high, target, nextSlot);
        }
        return nextSlot;
    }

    /**
     * Re-checks all five red-black invariants plus BST ordering from scratch.
     *
     * <p>Kept in production code rather than the test folder so the console demonstration can
     * prove balance to the examiner after a live insertion. O(n).
     *
     * @return true when the structure is a valid red-black tree
     */
    public boolean validate() {
        if (root == null) {
            return size == 0;
        }
        if (isRed(root)) {
            return false; // Invariant 2.
        }
        return checkSubtree(root, null, null) >= 0;
    }

    /**
     * Verifies one subtree and returns its black height, or -1 if any invariant fails.
     * Bounds are the exclusive key range this subtree must stay within, which is how BST
     * ordering is checked globally rather than only between adjacent nodes.
     */
    private int checkSubtree(Node<K, V> node, K exclusiveLow, K exclusiveHigh) {
        if (node == null) {
            return 1; // Null leaves are black (invariant 3).
        }

        if (exclusiveLow != null && node.key.compareTo(exclusiveLow) <= 0) {
            return -1;
        }
        if (exclusiveHigh != null && node.key.compareTo(exclusiveHigh) >= 0) {
            return -1;
        }

        if (isRed(node) && (isRed(node.left) || isRed(node.right))) {
            return -1; // Invariant 4.
        }

        if (node.left != null && node.left.parent != node) {
            return -1; // Parent pointers must agree, or rotations would corrupt the tree.
        }
        if (node.right != null && node.right.parent != node) {
            return -1;
        }

        int leftBlackHeight = checkSubtree(node.left, exclusiveLow, node.key);
        if (leftBlackHeight < 0) {
            return -1;
        }
        int rightBlackHeight = checkSubtree(node.right, node.key, exclusiveHigh);
        if (rightBlackHeight < 0) {
            return -1;
        }
        if (leftBlackHeight != rightBlackHeight) {
            return -1; // Invariant 5.
        }

        return leftBlackHeight + (node.color == BLACK ? 1 : 0);
    }

    /** Left rotations performed so far; recorded for the empirical efficiency lab. */
    public long getLeftRotations() {
        return leftRotations;
    }

    /** Right rotations performed so far; recorded for the empirical efficiency lab. */
    public long getRightRotations() {
        return rightRotations;
    }

    /** Total rotations performed so far. */
    public long getTotalRotations() {
        return leftRotations + rightRotations;
    }

    /** Individual color changes applied so far; the cheap half of rebalancing. */
    public long getRecolorings() {
        return recolorings;
    }

    /** Key comparisons made by {@code put}, {@code get}, and {@code contains}. */
    public long getComparisons() {
        return comparisons;
    }

    /** Zeroes the empirical counters so a benchmark run can measure one workload cleanly. */
    public void resetCounters() {
        leftRotations = 0;
        rightRotations = 0;
        recolorings = 0;
        comparisons = 0;
    }
}
