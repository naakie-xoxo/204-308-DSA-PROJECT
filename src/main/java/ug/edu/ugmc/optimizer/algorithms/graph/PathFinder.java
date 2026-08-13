package ug.edu.ugmc.optimizer.algorithms.graph;

import ug.edu.ugmc.optimizer.graph.CustomGraph;
import ug.edu.ugmc.optimizer.graph.CustomGraph.EdgeNode;
import ug.edu.ugmc.optimizer.datastructures.heap.CustomPriorityQueue;
import ug.edu.ugmc.optimizer.datastructures.disjointset.DisjointSet;

/**
 * Pathfinding engine for the UGMC Optimizer.
 * Executes shortest-path algorithms over the CustomGraph network.
 */
public class PathFinder {

    // Universal Parameter assigned to Naakie
    private static final int EMERGENCY_PENALTY_BASE = 22387455;

    /**
     * Executes Dijkstra's Algorithm to find the shortest travel time between two
     * wards.
     * Integrates Group A's CustomPriorityQueue for O(V + E log V) efficiency.
     * 
     * @param graph      The hospital network graph.
     * @param startNode  The starting location ID (e.g., "ER").
     * @param targetNode The destination location ID (e.g., "ICU").
     * @return The minimum total weight (travel time) to reach the target, or -1 if
     *         unreachable.
     */
    public static int dijkstra(CustomGraph graph, String startNode, String targetNode) {
        int numNodes = graph.getNumNodes();
        int[] distances = new int[numNodes];
        boolean[] visited = new boolean[numNodes];

        // Initialize all distances to "infinity"
        for (int i = 0; i < numNodes; i++) {
            distances[i] = Integer.MAX_VALUE;
        }

        int startIndex = graph.getIndex(startNode);
        int targetIndex = graph.getIndex(targetNode);

        if (startIndex == -1 || targetIndex == -1) {
            throw new IllegalArgumentException("Start or target node does not exist in the graph.");
        }

        // Distance to the starting point is always 0
        distances[startIndex] = 0;

        // Initialize Tawiah Kwaku's Priority Queue
        CustomPriorityQueue pq = new CustomPriorityQueue();
        pq.insert(startNode, 0);

        while (true) {
            String currentNode;
            try {
                // Extract the node with the lowest distance
                currentNode = pq.extractHighestPriority();
            } catch (IllegalStateException e) {
                // Queue is empty, meaning all reachable nodes are processed
                break;
            }

            int currentIndex = graph.getIndex(currentNode);

            // Skip if we have already found the absolute shortest path to this node
            if (visited[currentIndex]) {
                continue;
            }
            visited[currentIndex] = true;

            // Early exit if we reached our destination
            if (currentIndex == targetIndex) {
                return distances[targetIndex];
            }

            // Traverse the Adjacency List for the current node
            EdgeNode neighbor = graph.getNeighbors(currentIndex);
            while (neighbor != null) {
                int neighborIndex = neighbor.destinationIndex;

                if (!visited[neighborIndex]) {
                    // Apply Naakie's congestion penalty derived from the universal parameter
                    int congestionPenalty = (EMERGENCY_PENALTY_BASE % 3);
                    int newDist = distances[currentIndex] + neighbor.weight + congestionPenalty;

                    // Relaxation step
                    if (newDist < distances[neighborIndex]) {
                        distances[neighborIndex] = newDist;
                        pq.insert(graph.getNodeName(neighborIndex), newDist);
                    }
                }
                neighbor = neighbor.next;
            }
        }

        // Return -1 if the target node is completely isolated/unreachable
        return -1;
    }

    public static int kruskalMSTCost(CustomGraph graph) {
        int n = graph.getNumNodes();

        if (n == 0) {
            return 0;
        }

        int maxEdges = n * (n - 1) / 2;

        int[] source = new int[maxEdges];
        int[] destination = new int[maxEdges];
        int[] weight = new int[maxEdges];

        int edgeCount = 0;

        for (int u = 0; u < n; u++) {
            EdgeNode neighbor = graph.getNeighbors(u);

            while (neighbor != null) {
                int v = neighbor.destinationIndex;

                if (u < v) {
                    source[edgeCount] = u;
                    destination[edgeCount] = v;
                    weight[edgeCount] = neighbor.weight;
                    edgeCount++;
                }

                neighbor = neighbor.next;
            }
        }

        // Sort edges by weight using a simple insertion sort
        for (int i = 1; i < edgeCount; i++) {
            int currentSource = source[i];
            int currentDestination = destination[i];
            int currentWeight = weight[i];

            int j = i - 1;

            while (j >= 0 && weight[j] > currentWeight) {
                source[j + 1] = source[j];
                destination[j + 1] = destination[j];
                weight[j + 1] = weight[j];
                j--;
            }

            source[j + 1] = currentSource;
            destination[j + 1] = currentDestination;
            weight[j + 1] = currentWeight;
        }

        DisjointSet disjointSet = new DisjointSet(n);

        int totalCost = 0;
        int edgesUsed = 0;

        for (int i = 0; i < edgeCount && edgesUsed < n - 1; i++) {
            int u = source[i];
            int v = destination[i];

            if (!disjointSet.connected(u, v)) {
                disjointSet.union(u, v);
                totalCost += weight[i];
                edgesUsed++;
            }
        }

        if (edgesUsed != n - 1) {
            return -1;
        }

        return totalCost;
    }

    /**
     * Prim's Minimum Spanning Tree algorithm.
     *
     * @param graph the weighted graph
     * @return total weight of the MST, or -1 if the graph is disconnected
     */
    public static int primMSTCost(CustomGraph graph) {
        int n = graph.getNumNodes();

        if (n == 0) {
            return 0;
        }

        boolean[] inMST = new boolean[n];
        int[] minEdge = new int[n];

        for (int i = 0; i < n; i++) {
            minEdge[i] = Integer.MAX_VALUE;
        }

        minEdge[0] = 0;
        int totalCost = 0;

        for (int count = 0; count < n; count++) {
            int u = -1;

            // Find the unvisited vertex with the smallest edge
            for (int i = 0; i < n; i++) {
                if (!inMST[i] && (u == -1 || minEdge[i] < minEdge[u])) {
                    u = i;
                }
            }

            // No reachable vertex means the graph is disconnected
            if (u == -1 || minEdge[u] == Integer.MAX_VALUE) {
                return -1;
            }

            inMST[u] = true;
            totalCost += minEdge[u];

            // Update the cheapest connection for every unvisited vertex
            for (int v = 0; v < n; v++) {
                int weight = graph.getMatrixWeight(u, v);

                if (!inMST[v] && weight >= 0 && weight < minEdge[v]) {
                    minEdge[v] = weight;
                }
            }
        }

        return totalCost;
    }

    /**
     * Kruskal's Minimum Spanning Tree algorithm.
     *
     * Uses the custom DisjointSet implementation to detect cycles.
     *
     * @param graph the weighted graph
     * @return total weight of the MST, or -1 if the graph is disconnected
     */

}