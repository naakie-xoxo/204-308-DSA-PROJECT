package ug.edu.ugmc.optimizer.datastructures.trees;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests 41-60: balanced indices for hospital records (Somuah, Group A).
 *
 * <p>These extend the central 40-test suite rather than replacing any of it. Tests 1-40 cover
 * the linear structures, queues, heap, hash table, BST, sorting, searching, and graph work;
 * neither balanced tree had coverage there. Numbering continues from 40 so the two suites can
 * be read together in the final report.
 *
 * <p>Each structure gets normal, boundary, and invalid-input cases, plus a randomized
 * stress case that re-checks the structural invariants directly. The random keys come from a
 * fixed seed so a failure is always reproducible.
 */
class BalancedTreeTest {

    /**
     * Deterministic key generator. Uses the project's universal RANDOM_SEED (index 22040372)
     * with a small linear congruential recurrence, so the stress tests below shuffle the same
     * way on every machine and no {@code java.util} class is pulled into the assessed suite.
     */
    private static final class SeededKeys {
        private static final long RANDOM_SEED = 22040372L;
        private long state = RANDOM_SEED;

        private int next(int bound) {
            state = (state * 6364136223846793005L) + 1442695040888963407L;
            int value = (int) (state >>> 33);
            return Math.floorMod(value, bound);
        }
    }

    // ---------------------------------------------------------------------
    // Red-black tree
    // ---------------------------------------------------------------------

    // Test 41: Normal - index a record and read it back.
    @Test
    void testRedBlackPutAndGet() {
        RedBlackTree<String, String> index = new RedBlackTree<>();
        index.put("PAT-1042", "Ward B");
        index.put("PAT-1007", "ICU");
        index.put("PAT-2210", "Pharmacy");

        assertEquals("Ward B", index.get("PAT-1042"));
        assertEquals("ICU", index.get("PAT-1007"));
        assertEquals("Pharmacy", index.get("PAT-2210"));
        assertEquals(3, index.size());
    }

    // Test 42: Normal - in-order traversal returns keys in sorted order.
    @Test
    void testRedBlackInorderIsSorted() {
        RedBlackTree<Integer, String> index = new RedBlackTree<>();
        index.put(50, "A");
        index.put(30, "B");
        index.put(70, "C");
        index.put(20, "D");
        index.put(40, "E");

        assertEquals("20 30 40 50 70", index.inorderTraversal());
        assertEquals(20, index.min());
        assertEquals(70, index.max());
    }

    // Test 43: Boundary - ascending insertion, the input that degenerates a plain BST into a
    // linked list. The red-black tree must stay within the 2*log2(n+1) height bound.
    @Test
    void testRedBlackStaysBalancedOnSortedInput() {
        RedBlackTree<Integer, Integer> index = new RedBlackTree<>();
        int recordCount = 1023;
        for (int i = 1; i <= recordCount; i++) {
            index.put(i, i);
        }

        int worstCaseHeight = (int) (2 * (Math.log(recordCount + 1) / Math.log(2)));
        assertTrue(
                index.height() <= worstCaseHeight,
                "Height " + index.height() + " exceeded the red-black bound of " + worstCaseHeight);
        assertTrue(index.validate(), "Red-black invariants broken after sorted insertion");
        assertEquals(recordCount, index.size());
        assertTrue(index.getTotalRotations() > 0, "Sorted input must force rebalancing rotations");
    }

    // Test 44: Boundary - re-inserting a key updates the record instead of duplicating it.
    @Test
    void testRedBlackDuplicateKeyUpdatesValue() {
        RedBlackTree<String, String> index = new RedBlackTree<>();
        index.put("REQ-001", "Pending");
        index.put("REQ-001", "Dispatched");

        assertEquals("Dispatched", index.get("REQ-001"));
        assertEquals(1, index.size());
        assertEquals("REQ-001", index.inorderTraversal());
    }

    // Test 45: Boundary - an empty index reports empty and traverses to an empty string.
    @Test
    void testRedBlackEmptyIndex() {
        RedBlackTree<String, String> index = new RedBlackTree<>();

        assertTrue(index.isEmpty());
        assertEquals(0, index.size());
        assertEquals(-1, index.height());
        assertEquals("", index.inorderTraversal());
        assertTrue(index.validate());
    }

    // Test 46: Invalid - null keys are rejected on both write and read.
    @Test
    void testRedBlackNullKeyThrows() {
        RedBlackTree<String, String> index = new RedBlackTree<>();

        assertThrows(IllegalArgumentException.class, () -> index.put(null, "Ward A"));
        assertThrows(IllegalArgumentException.class, () -> index.get(null));
    }

    // Test 47: Invalid - min/max on an empty index is a programming error, not a null result.
    @Test
    void testRedBlackMinMaxOnEmptyThrows() {
        RedBlackTree<Integer, String> index = new RedBlackTree<>();

        assertThrows(IllegalStateException.class, index::min);
        assertThrows(IllegalStateException.class, index::max);
    }

    // Test 48: Normal - inclusive range query, the ordered lookup a hash table cannot serve.
    @Test
    void testRedBlackRangeSearch() {
        RedBlackTree<Integer, String> index = new RedBlackTree<>();
        for (int id = 10; id <= 100; id += 10) {
            index.put(id, "Record " + id);
        }

        assertArrayEquals(new Integer[] {30, 40, 50, 60}, index.rangeSearch(30, 60));
        assertArrayEquals(new Integer[] {50}, index.rangeSearch(50, 50));
        assertArrayEquals(new Integer[] {}, index.rangeSearch(101, 200));
    }

    // Test 49: Invalid - a reversed range is rejected rather than silently returning nothing.
    @Test
    void testRedBlackInvalidRangeThrows() {
        RedBlackTree<Integer, String> index = new RedBlackTree<>();
        index.put(10, "A");

        assertThrows(IllegalArgumentException.class, () -> index.rangeSearch(60, 30));
        assertThrows(IllegalArgumentException.class, () -> index.rangeSearch(null, 30));
    }

    // Test 50: Boundary - invariants survive a large randomized workload with duplicates.
    @Test
    void testRedBlackInvariantsUnderRandomLoad() {
        RedBlackTree<Integer, Integer> index = new RedBlackTree<>();
        SeededKeys keys = new SeededKeys();
        int insertions = 5000;

        for (int i = 0; i < insertions; i++) {
            int key = keys.next(2000); // Range below the insert count, so duplicates occur.
            index.put(key, i);
        }

        assertTrue(index.validate(), "Red-black invariants broken under randomized load");
        assertTrue(index.size() <= 2000);

        Object[] sorted = index.keysInOrder();
        assertEquals(index.size(), sorted.length);
        for (int i = 1; i < sorted.length; i++) {
            assertTrue(
                    ((Integer) sorted[i - 1]) < ((Integer) sorted[i]),
                    "Traversal is not strictly ascending");
        }
        for (Object key : sorted) {
            assertTrue(index.contains((Integer) key));
            assertNotNull(index.get((Integer) key));
        }
    }

    // ---------------------------------------------------------------------
    // B-tree
    // ---------------------------------------------------------------------

    // Test 51: Boundary - the minimum degree is the one derived from index 22018389, not a
    // hardcoded constant. (22018389 % 4) + 3 = 4.
    @Test
    void testBTreeMinimumDegreeDerivedFromIndexNumber() {
        assertEquals((22018389 % 4) + 3, BTree.MIN_DEGREE);
        assertEquals(4, BTree.MIN_DEGREE);
        assertEquals(7, BTree.MAX_KEYS);
        assertEquals(3, BTree.MIN_KEYS);
        assertEquals(8, BTree.MAX_CHILDREN);
    }

    // Test 52: Normal - index a record and read it back.
    @Test
    void testBTreeInsertAndSearch() {
        BTree<String, String> index = new BTree<>();
        index.insert("PAT-1042", "Ward B");
        index.insert("PAT-1007", "ICU");
        index.insert("PAT-2210", "Pharmacy");

        assertEquals("Ward B", index.search("PAT-1042"));
        assertEquals("ICU", index.search("PAT-1007"));
        assertEquals(3, index.size());
        assertTrue(index.validate());
    }

    // Test 53: Boundary - a node holds up to MAX_KEYS without splitting, and the very next
    // insertion splits the root and grows the tree by exactly one level.
    @Test
    void testBTreeSplitsExactlyAtCapacity() {
        BTree<Integer, String> index = new BTree<>();
        for (int i = 1; i <= BTree.MAX_KEYS; i++) {
            index.insert(i, "Record " + i);
        }

        assertEquals(0, index.getSplitCount(), "Filling a node to capacity must not split it");
        assertEquals(0, index.height(), "Seven keys still fit in a single root node");

        index.insert(BTree.MAX_KEYS + 1, "Overflow record");

        assertEquals(1, index.getSplitCount(), "The eighth key must trigger exactly one split");
        assertEquals(1, index.height(), "The split promotes a median into a new root level");
        assertTrue(index.validate());
        assertEquals(BTree.MAX_KEYS + 1, index.size());
    }

    // Test 54: Normal - in-order traversal across a multi-level tree returns sorted keys.
    @Test
    void testBTreeInorderIsSorted() {
        BTree<Integer, String> index = new BTree<>();
        int[] arrivals = {50, 30, 70, 20, 40, 60, 80, 10, 90, 25, 35, 45, 55, 65, 75, 85};
        for (int key : arrivals) {
            index.insert(key, "Record " + key);
        }

        assertEquals(
                "10 20 25 30 35 40 45 50 55 60 65 70 75 80 85 90", index.inorderTraversal());
        assertEquals(10, index.min());
        assertEquals(90, index.max());
        assertTrue(index.validate());
    }

    // Test 55: Boundary - ascending insertion keeps every leaf at the same depth and keeps the
    // tree far shallower than the red-black tree over the same keys.
    @Test
    void testBTreeStaysShallowOnSortedInput() {
        BTree<Integer, Integer> index = new BTree<>();
        RedBlackTree<Integer, Integer> comparison = new RedBlackTree<>();
        int recordCount = 10000;
        for (int i = 1; i <= recordCount; i++) {
            index.insert(i, i);
            comparison.put(i, i);
        }

        assertTrue(index.validate(), "B-tree invariants broken after sorted insertion");
        assertEquals(recordCount, index.size());
        assertTrue(
                index.height() < comparison.height(),
                "B-tree height " + index.height() + " should beat red-black height "
                        + comparison.height());
        assertTrue(index.getSplitCount() > 0, "Ten thousand records must force node splits");
    }

    // Test 56: Boundary - re-inserting a key updates the record instead of duplicating it,
    // including for a key that has already been promoted into an internal node.
    @Test
    void testBTreeDuplicateKeyUpdatesValue() {
        BTree<Integer, String> index = new BTree<>();
        for (int i = 1; i <= 20; i++) {
            index.insert(i, "Pending " + i);
        }
        int sizeBefore = index.size();

        index.insert(4, "Dispatched");

        assertEquals("Dispatched", index.search(4));
        assertEquals(sizeBefore, index.size(), "Updating a key must not change the record count");
        assertTrue(index.validate());
    }

    // Test 57: Boundary - an empty index reports empty and traverses to an empty string.
    @Test
    void testBTreeEmptyIndex() {
        BTree<String, String> index = new BTree<>();

        assertTrue(index.isEmpty());
        assertEquals(0, index.size());
        assertEquals(-1, index.height());
        assertEquals("", index.inorderTraversal());
        assertTrue(index.validate());
    }

    // Test 58: Invalid - null keys are rejected, missing keys return null rather than throwing.
    @Test
    void testBTreeNullKeyThrowsAndMissingKeyReturnsNull() {
        BTree<String, String> index = new BTree<>();
        index.insert("REQ-001", "Pending");

        assertThrows(IllegalArgumentException.class, () -> index.insert(null, "Ward A"));
        assertThrows(IllegalArgumentException.class, () -> index.search(null));
        assertNull(index.search("REQ-UNKNOWN"));
        assertFalse(index.contains("REQ-UNKNOWN"));
    }

    // Test 59: Invalid - min/max on an empty index is a programming error, not a null result.
    @Test
    void testBTreeMinMaxOnEmptyThrows() {
        BTree<Integer, String> index = new BTree<>();

        assertThrows(IllegalStateException.class, index::min);
        assertThrows(IllegalStateException.class, index::max);
    }

    // Test 60: Boundary - both balanced indices must agree key-for-key on the same randomized
    // workload. Either structure can back the hospital record index, so they have to be
    // interchangeable in what they return.
    @Test
    void testBothIndicesAgreeUnderRandomLoad() {
        RedBlackTree<Integer, Integer> redBlack = new RedBlackTree<>();
        BTree<Integer, Integer> bTree = new BTree<>();
        SeededKeys keys = new SeededKeys();
        int insertions = 5000;

        for (int i = 0; i < insertions; i++) {
            int key = keys.next(2000);
            redBlack.put(key, i);
            bTree.insert(key, i);
        }

        assertTrue(redBlack.validate());
        assertTrue(bTree.validate());
        assertEquals(redBlack.size(), bTree.size());
        assertEquals(redBlack.inorderTraversal(), bTree.inorderTraversal());
        assertArrayEquals(redBlack.keysInOrder(), bTree.keysInOrder());
        assertEquals(redBlack.min(), bTree.min());
        assertEquals(redBlack.max(), bTree.max());

        for (Object key : redBlack.keysInOrder()) {
            Integer id = (Integer) key;
            assertEquals(redBlack.get(id), bTree.search(id), "Indices disagree on key " + id);
        }
    }
}
