package ug.edu.ugmc.optimizer.datastructures.disjointset;

/**
 * Disjoint Set (Union-Find) data structure for the UGMC Optimizer.
 * Engineered to track network connectivity between hospital locations 
 * in near-constant time using Path Compression and Union by Rank.
 */
public class DisjointSet {

    // Internal representation using parallel arrays
    private int[] parent;
    private int[] rank;

    /**
     * Purpose: Initializes the Disjoint Set.
     * Complexity: O(N) Time, O(N) Space.
     * 
     * @param size The number of discrete elements (e.g., hospital locations).
     */
    public DisjointSet(int size) {
        parent = new int[size];
        rank = new int[size];
        
        // Initially, every element is in its own set (makeSet)
        for (int i = 0; i < size; i++) {
            makeSet(i);
        }
    }

    /**
     * Purpose: Creates a new set consisting of a single element.
     * Complexity: O(1) Time.
     * 
     * @param i The element identifier.
     */
    public void makeSet(int i) {
        parent[i] = i; // Node is its own parent (root)
        rank[i] = 0;   // Initial rank is 0
    }

    /**
     * Purpose: Finds the representative (root) of the set containing element i.
     * Implements Path Compression to flatten the tree structure.
     * Complexity: O(α(N)) amortized Time.
     * 
     * @param i The element to find.
     * @return The root identifier of the set.
     */
    public int find(int i) {
        // If i is not the root of its own set
        if (parent[i] != i) {
            // Path compression: recursively find the root and attach i directly to it
            parent[i] = find(parent[i]); 
        }
        return parent[i];
    }

    /**
     * Purpose: Merges the sets containing elements x and y.
     * Implements Union by Rank to keep trees shallow.
     * Complexity: O(α(N)) amortized Time.
     * 
     * @param x The first element.
     * @param y The second element.
     */
    public void union(int x, int y) {
        int rootX = find(x);
        int rootY = find(y);

        // If they are already in the same set, do nothing
        if (rootX != rootY) {
            // Union by rank: attach the shorter tree under the taller tree
            if (rank[rootX] > rank[rootY]) {
                parent[rootY] = rootX;
            } else if (rank[rootX] < rank[rootY]) {
                parent[rootX] = rootY;
            } else {
                // If ranks are equal, pick one as root and increment its rank
                parent[rootY] = rootX;
                rank[rootX]++;
            }
        }
    }

    /**
     * Purpose: Checks if two elements are in the same set.
     * Hospital Use Case: Verifying if two departments share the same routing network.
     * 
     * @param x The first element.
     * @param y The second element.
     * @return True if connected, false otherwise.
     */
    public boolean connected(int x, int y) {
        return find(x) == find(y);
    }
}