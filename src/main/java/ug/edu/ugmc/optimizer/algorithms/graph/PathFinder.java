package ug.edu.ugmc.optimizer.algorithms.graph;

import ug.edu.ugmc.optimizer.graph.CustomGraph;
import ug.edu.ugmc.optimizer.graph.CustomGraph.EdgeNode;
import ug.edu.ugmc.optimizer.datastructures.heap.CustomPriorityQueue;

/**
 * Pathfinding engine for the UGMC Optimizer.
 * Executes shortest-path algorithms over the CustomGraph network.
 */
public class PathFinder {

    // Universal Parameter assigned to Naakie
    private static final int EMERGENCY_PENALTY_BASE = 22387455;

    /**
     * Executes Dijkstra's Algorithm to find the shortest travel time between two wards.
     * Integrates Group A's CustomPriorityQueue for O(V + E log V) efficiency.
     * 
     * @param graph The hospital network graph.
     * @param startNode The starting location ID (e.g., "ER").
     * @param targetNode The destination location ID (e.g., "ICU").
     * @return The minimum total weight (travel time) to reach the target, or -1 if unreachable.
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
}