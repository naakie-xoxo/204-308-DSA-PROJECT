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

    /**
     * Immutable result of a Dijkstra search.  The route is ordered from source
     * to target and is empty when the target is unreachable.
     */
    public static final class ShortestPathResult {
        private final int distance;
        private final String[] route;

        private ShortestPathResult(int distance, String[] route) {
            this.distance = distance;
            this.route = route;
        }

        /** @return minimum travel time, or {@code -1} if no route exists */
        public int getDistance() {
            return distance;
        }

        /**
         * Returns a copy of the source-to-target route so callers cannot alter
         * the result stored by the pathfinder.
         *
         * @return hospital location IDs in route order, or an empty array when
         *         the target is unreachable
         */
        public String[] getRoute() {
            String[] routeCopy = new String[route.length];
            for (int i = 0; i < route.length; i++) {
                routeCopy[i] = route[i];
            }
            return routeCopy;
        }
    }

    /**
     * Executes Dijkstra's Algorithm to find the shortest travel time between two
     * wards.
     * Integrates the project's custom priority queue for O((V + E) log V)
     * efficiency.  Edge weights are used exactly as stored in the graph; route
     * costs must not be changed by algorithm-specific penalties.
     * 
     * @param graph      The hospital network graph.
     * @param startNode  The starting location ID (e.g., "ER").
     * @param targetNode The destination location ID (e.g., "ICU").
     * @return the minimum total weight (travel time), or {@code -1} when the
     *         target is unreachable
     * @throws IllegalArgumentException if the graph or location IDs are invalid,
     *         or if the graph contains a negative edge
     * @throws ArithmeticException if a reachable route cannot be represented by
     *         the method's integer return type
     */
    public static int dijkstra(CustomGraph graph, String startNode, String targetNode) {
        return dijkstraWithRoute(graph, startNode, targetNode).getDistance();
    }

    /**
     * Executes Dijkstra's algorithm and returns both the shortest travel time
     * and the corresponding source-to-target route.
     *
     * @param graph hospital road network
     * @param startNode starting hospital location ID
     * @param targetNode destination hospital location ID
     * @return shortest-path result; unreachable targets have distance
     *         {@code -1} and an empty route
     * @throws IllegalArgumentException if the graph or location IDs are invalid,
     *         or if the graph contains a negative edge
     * @throws ArithmeticException if a reachable route cannot be represented by
     *         the result's integer distance
     */
    public static ShortestPathResult dijkstraWithRoute(
            CustomGraph graph, String startNode, String targetNode) {
        requireGraph(graph);
        requireLocationId(startNode, "startNode");
        requireLocationId(targetNode, "targetNode");

        int numNodes = graph.getNumNodes();
        int startIndex = graph.getIndex(startNode);
        int targetIndex = graph.getIndex(targetNode);

        if (startIndex == -1 || targetIndex == -1) {
            throw new IllegalArgumentException("Start or target node does not exist in the graph.");
        }

        validateDijkstraEdges(graph, numNodes);

        long[] distances = new long[numNodes];
        boolean[] visited = new boolean[numNodes];
        int[] predecessor = new int[numNodes];

        // Long.MAX_VALUE is a sentinel only; it is never used in arithmetic.
        for (int i = 0; i < numNodes; i++) {
            distances[i] = Long.MAX_VALUE;
            predecessor[i] = -1;
        }

        distances[startIndex] = 0;

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

            if (currentIndex < 0 || currentIndex >= numNodes) {
                throw new IllegalStateException("Priority queue contained an unknown graph node.");
            }

            // Stale entries are expected after a successful relaxation.  The
            // first extraction for a node is its final shortest distance.
            if (visited[currentIndex]) {
                continue;
            }
            visited[currentIndex] = true;

            if (currentIndex == targetIndex) {
                return new ShortestPathResult(
                        Math.toIntExact(distances[targetIndex]),
                        reconstructRoute(graph, predecessor, startIndex, targetIndex));
            }

            EdgeNode neighbor = graph.getNeighbors(currentIndex);
            while (neighbor != null) {
                int neighborIndex = neighbor.destinationIndex;

                if (neighborIndex < 0 || neighborIndex >= numNodes) {
                    throw new IllegalStateException("Graph contains an invalid edge destination.");
                }

                if (!visited[neighborIndex]) {
                    // Negative weights are rejected before the search.  Exact
                    // addition prevents a large travel-time sum from wrapping.
                    long newDist = Math.addExact(distances[currentIndex], neighbor.weight);

                    if (newDist < distances[neighborIndex]) {
                        distances[neighborIndex] = newDist;
                        predecessor[neighborIndex] = currentIndex;
                        pq.insert(graph.getNodeName(neighborIndex), newDist);
                    }
                }
                neighbor = neighbor.next;
            }
        }

        return new ShortestPathResult(-1, new String[0]);
    }

    private static String[] reconstructRoute(
            CustomGraph graph, int[] predecessor, int startIndex, int targetIndex) {
        int routeLength = 1;
        int currentIndex = targetIndex;

        while (currentIndex != startIndex) {
            currentIndex = predecessor[currentIndex];
            if (currentIndex < 0) {
                throw new IllegalStateException("Shortest-path predecessor chain is incomplete.");
            }
            routeLength++;
        }

        String[] route = new String[routeLength];
        currentIndex = targetIndex;
        for (int position = routeLength - 1; position >= 0; position--) {
            route[position] = graph.getNodeName(currentIndex);
            currentIndex = predecessor[currentIndex];
        }
        return route;
    }

    private static void validateDijkstraEdges(CustomGraph graph, int numNodes) {
        for (int source = 0; source < numNodes; source++) {
            EdgeNode edge = graph.getNeighbors(source);
            while (edge != null) {
                if (edge.destinationIndex < 0 || edge.destinationIndex >= numNodes) {
                    throw new IllegalStateException("Graph contains an invalid edge destination.");
                }
                if (edge.weight < 0) {
                    throw new IllegalArgumentException(
                            "Dijkstra requires non-negative edge weights; found " + edge.weight + ".");
                }
                edge = edge.next;
            }
        }
    }

    private static void requireLocationId(String locationId, String parameterName) {
        if (locationId == null || locationId.trim().isEmpty()) {
            throw new IllegalArgumentException(parameterName + " cannot be null or blank.");
        }
    }

    /**
     * Calculates the minimum spanning-tree cost using Kruskal's algorithm.
     * The implementation reuses the project's custom {@link DisjointSet} for
     * cycle detection and primitive arrays for edge storage.
     *
     * @param graph weighted undirected hospital graph
     * @return total MST cost, {@code 0} for an empty graph, or {@code -1} when
     *         the graph is disconnected
     * @throws IllegalArgumentException if {@code graph} is null
     */
    public static int kruskalMSTCost(CustomGraph graph) {
        requireGraph(graph);
        int n = graph.getNumNodes();

        if (n == 0) {
            return 0;
        }

        int edgeCount = 0;
        for (int u = 0; u < n; u++) {
            EdgeNode neighbor = graph.getNeighbors(u);
            while (neighbor != null) {
                if (u < neighbor.destinationIndex) {
                    edgeCount++;
                }
                neighbor = neighbor.next;
            }
        }

        int[] source = new int[edgeCount];
        int[] destination = new int[edgeCount];
        int[] weight = new int[edgeCount];

        int edgeIndex = 0;
        for (int u = 0; u < n; u++) {
            EdgeNode neighbor = graph.getNeighbors(u);
            while (neighbor != null) {
                if (u < neighbor.destinationIndex) {
                    source[edgeIndex] = u;
                    destination[edgeIndex] = neighbor.destinationIndex;
                    weight[edgeIndex] = neighbor.weight;
                    edgeIndex++;
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
        requireGraph(graph);
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

    private static void requireGraph(CustomGraph graph) {
        if (graph == null) {
            throw new IllegalArgumentException("Graph cannot be null.");
        }
    }
}
