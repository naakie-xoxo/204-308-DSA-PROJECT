package ug.edu.ugmc.optimizer.datastructures.hashing;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HashTableTest {

    // Test 19: Normal - Put and Get
    @Test
    void testPutAndGet() {
        CustomHashTable<String, String> table = new CustomHashTable<>();
        table.put("REQ001", "Pending");
        assertEquals("Pending", table.get("REQ001"));
    }

    // Test 20: Boundary - Hash Collision Handling
    @Test
    void testHashCollision() {
        // Force a collision if map uses simple modulus
        CustomHashTable<Integer, String> table = new CustomHashTable<>(5);
        table.put(1, "A");
        table.put(6, "B"); // Collides with 1 if size is 5
        assertEquals("A", table.get(1));
        assertEquals("B", table.get(6));
    }

    // Test 21: Invalid - Missing Key
    @Test
    void testGetMissingKeyReturnsNull() {
        CustomHashTable<String, String> table = new CustomHashTable<>();
        assertNull(table.get("UNKNOWN_ID"));
    }

    // Test 22: Tree - BST Inorder Traversal (Sorted output)
    @Test
    void testBSTInorder() {
        BinarySearchTree bst = new BinarySearchTree();
        bst.insert(50); bst.insert(30); bst.insert(70);
        assertEquals("30 50 70", bst.inorderTraversal().trim());
    }
    
    // Test 23: Tree - BST Search
    @Test
    void testBSTSearchFoundAndNotFound() {
        BinarySearchTree bst = new BinarySearchTree();
        bst.insert(101);
        assertTrue(bst.search(101));
        assertFalse(bst.search(999));
    }
}