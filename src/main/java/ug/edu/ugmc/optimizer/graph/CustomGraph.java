package ug.edu.ugmc.optimizer.graph;

/**
 * Custom Graph data structure for the UGMC Optimizer.
 * Maintains both Adjacency Matrix and Adjacency List representations 
 * to satisfy strict DSA project requirements.
 */
public class CustomGraph {

    private int maxNodes;
    private int numNodes;
    private String[] nodeNames;
    
    // Representation 1: Adjacency Matrix
    private int[][] adjacencyMatrix;
    
    // Representation 2: Adjacency List
    private EdgeNode[] adjacencyList;

    /**
     * Inner class to act as the Linked List node for the Adjacency List.
     * Built entirely from scratch.
     */
    public static class EdgeNode {
        public int destinationIndex;
        public int weight;
        public EdgeNode next;

        public EdgeNode(int destinationIndex, int weight, EdgeNode next) {
            this.destinationIndex = destinationIndex;
            this.weight = weight;
            this.next = next;
        }
    }

    /**
     * Initializes the graph with a fixed maximum capacity.
     * 
     * @param maxNodes The maximum number of hospital locations.
     */
    public CustomGraph(int maxNodes) {
        this.maxNodes = maxNodes;
        this.numNodes = 0;
        this.nodeNames = new String[maxNodes];
        this.adjacencyMatrix = new int[maxNodes][maxNodes];
        this.adjacencyList = new EdgeNode[maxNodes];

        // Initialize matrix with -1 to represent no edge (since weights must be > 0)
        for (int i = 0; i < maxNodes; i++) {
            for (int j = 0; j < maxNodes; j++) {
                adjacencyMatrix[i][j] = -1;
            }
        }
    }

    /**
     * Adds a new hospital location to the graph.
     * Complexity: O(1) Time.
     * 
     * @param id The String ID of the location.
     */
    public void addNode(String id) {
        if (numNodes >= maxNodes) {
            throw new IllegalStateException("Graph capacity reached.");
        }
        nodeNames[numNodes] = id;
        numNodes++;
    }

    /**
     * Internal O(V) mapping from String ID to Integer Index.
     */
    public int getIndex(String id) {
        for (int i = 0; i < numNodes; i++) {
            if (nodeNames[i] != null && nodeNames[i].equals(id)) {
                return i;
            }
        }
        return -1;
    }

    public String getNodeName(int index) {
        if (index >= 0 && index < numNodes) {
            return nodeNames[index];
        }
        return null;
    }

    /**
     * Adds an undirected edge to both the Matrix and the List representations.
     * Hospital Use Case: Connecting two locations with a weighted road.
     * Complexity: O(V) Time (due to index lookup), O(1) Insertion.
     * 
     * @param source The starting location ID.
     * @param destination The ending location ID.
     * @param weight The road condition weight or travel time.
     */
    public void addEdge(String source, String destination, int weight) {
        int srcIdx = getIndex(source);
        int destIdx = getIndex(destination);

        if (srcIdx == -1 || destIdx == -1) {
            throw new IllegalArgumentException("Cannot add edge: Node not found.");
        }

        // 1. Update Adjacency Matrix (Undirected)
        adjacencyMatrix[srcIdx][destIdx] = weight;
        adjacencyMatrix[destIdx][srcIdx] = weight;

        // 2. Update Adjacency List (Insert at head for O(1) time)
        adjacencyList[srcIdx] = new EdgeNode(destIdx, weight, adjacencyList[srcIdx]);
        adjacencyList[destIdx] = new EdgeNode(srcIdx, weight, adjacencyList[destIdx]);
    }

    // Getters required for algorithms
    public int getNumNodes() {
        return numNodes;
    }

    public EdgeNode getNeighbors(int nodeIndex) {
        return adjacencyList[nodeIndex];
    }

    public int getMatrixWeight(int srcIdx, int destIdx) {
        return adjacencyMatrix[srcIdx][destIdx];
    }
}