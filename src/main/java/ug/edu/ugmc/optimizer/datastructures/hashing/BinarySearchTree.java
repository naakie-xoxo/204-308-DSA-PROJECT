package ug.edu.ugmc.optimizer.datastructures.hashing;

/**
 * BinarySearchTree
 * -----------------
 * Minimal integer BST supporting insert, search, and inorder traversal.
 *
 * NOTE: This class was added only to satisfy HashTableTest's tree tests
 * (testBSTInorder, testBSTSearchFoundAndNotFound), which reference
 * BinarySearchTree with no import — meaning the test expects it in the
 * same package as the hash table. It isn't tied to any of the 15
 * index-derived parameters (that's Somuah's B-Tree, index 22018389, a
 * different structure). Flag this with whoever owns the actual BST/tree
 * assignment — this file should probably move to its own
 * ug.edu.ugmc.optimizer.datastructures.trees package once ownership is
 * confirmed, rather than living next to the hash table permanently.
 */
public class BinarySearchTree {

    private class Node {
        int value;
        Node left;
        Node right;

        Node(int value) {
            this.value = value;
        }
    }

    private Node root;

    public void insert(int value) {
        root = insertRec(root, value);
    }

    private Node insertRec(Node node, int value) {
        if (node == null) {
            return new Node(value);
        }
        if (value < node.value) {
            node.left = insertRec(node.left, value);
        } else if (value > node.value) {
            node.right = insertRec(node.right, value);
        }
        // Duplicate values are ignored.
        return node;
    }

    public boolean search(int value) {
        return searchRec(root, value);
    }

    private boolean searchRec(Node node, int value) {
        if (node == null) {
            return false;
        }
        if (value == node.value) {
            return true;
        }
        return value < node.value
                ? searchRec(node.left, value)
                : searchRec(node.right, value);
    }

    /** Returns space-separated inorder (sorted) traversal, e.g. "30 50 70 ". */
    public String inorderTraversal() {
        StringBuilder sb = new StringBuilder();
        inorderRec(root, sb);
        return sb.toString();
    }

    private void inorderRec(Node node, StringBuilder sb) {
        if (node == null) {
            return;
        }
        inorderRec(node.left, sb);
        sb.append(node.value).append(" ");
        inorderRec(node.right, sb);
    }
}
