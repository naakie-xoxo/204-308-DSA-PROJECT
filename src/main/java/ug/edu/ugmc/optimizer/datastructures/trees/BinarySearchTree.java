package ug.edu.ugmc.optimizer.datastructures.trees;

public class BinarySearchTree {
    private Node root;

    private static class Node {
        int value;
        Node left, right;
        Node(int value) { this.value = value; }
    }

    public void insert(int value) {
        root = insertRec(root, value);
    }

    private Node insertRec(Node node, int value) {
        if (node == null) return new Node(value);
        if (value < node.value) node.left = insertRec(node.left, value);
        else if (value > node.value) node.right = insertRec(node.right, value);
        return node;
    }

    public boolean search(int value) {
        return searchRec(root, value);
    }

    private boolean searchRec(Node node, int value) {
        if (node == null) return false;
        if (value == node.value) return true;
        return value < node.value ? searchRec(node.left, value) : searchRec(node.right, value);
    }

    public String inorderTraversal() {
        StringBuilder sb = new StringBuilder();
        inorderRec(root, sb);
        return sb.toString().trim();
    }

    private void inorderRec(Node node, StringBuilder sb) {
        if (node == null) return;
        inorderRec(node.left, sb);
        sb.append(node.value).append(" ");
        inorderRec(node.right, sb);
    }
}