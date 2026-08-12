package ug.edu.ugmc.optimizer.algorithms.graph;

import ug.edu.ugmc.optimizer.datastructures.queues.CustomQueue;
import ug.edu.ugmc.optimizer.datastructures.queues.CustomStack;
import ug.edu.ugmc.optimizer.graph.CustomGraph;
import ug.edu.ugmc.optimizer.graph.CustomGraph.EdgeNode;

/**
 * Graph traversal engine for the UGMC Optimizer.
 * Finds reachable hospital wards (nodes) from a starting location
 * using Breadth-First Search and Depth-First Search.
 *
 * Role context: locating all hospital wards reachable from the Emergency Room
 * so dispatch can decide whether a critical patient must travel farther.
 *
 * Universal Parameter: 22394896.
 * MAX_TRAVERSAL_DEPTH is derived as (22394896 % 15) + 5 to guard against
 * infinite recursion in disconnected or cyclic network zones.
 */
public class GraphTraversal {

    /** Hard upper bound on traversal depth to bound work on disconnected graphs. */
    public static final int MAX_TRAVERSAL_DEPTH = (22394896 % 15) + 5; // Evaluates to 6

    /** Marker entry pushed onto the DFS stack so the depth travels with each node. */
    private static final class DfsFrame {
        final int nodeIndex;
        final int depth;

        DfsFrame(int nodeIndex, int depth) {
            this.nodeIndex = nodeIndex;
            this.depth = depth;
        }
    }

    /**
     * Determines whether {@code targetNode} is reachable from {@code startNode}
     * using a Breadth-First Search that respects {@link #MAX_TRAVERSAL_DEPTH}.
     *
     * BFS expands the graph in rings around the start, so once {@code targetNode}
     * is dequeued, we know it was reached via the fewest edges — useful for
     * identifying the closest available ward.
     *
     * @param graph       the hospital network graph
     * @param startNode   ID of the starting ward (e.g., "ER")
     * @param targetNode  ID of the destination ward
     * @return true if target is reachable within the depth budget, false otherwise
     */
    public static boolean bfsReachability(CustomGraph graph, String startNode, String targetNode) {
        int startIndex = validateAndResolve(graph, startNode, targetNode);
        int targetIndex = graph.getIndex(targetNode);

        if (startIndex == targetIndex) {
            return true;
        }

        boolean[] visited = new boolean[graph.getNumNodes()];
        // Parallel depth array so BFS can also enforce MAX_TRAVERSAL_DEPTH
        // (BFS has no implicit depth, so we track it explicitly).
        int[] depth = new int[graph.getNumNodes()];
        // CustomQueue is the merged FIFO primitive from Group A (capacity 56).
        CustomQueue<Integer> queue = new CustomQueue<>();
        queue.enqueue(startIndex);
        visited[startIndex] = true;
        depth[startIndex] = 0;

        while (!queue.isEmpty()) {
            int current = queue.dequeue();

            // Depth cap: do not expand any node whose depth has already hit the budget.
            if (depth[current] >= MAX_TRAVERSAL_DEPTH) {
                continue;
            }

            EdgeNode neighbor = graph.getNeighbors(current);
            while (neighbor != null) {
                int next = neighbor.destinationIndex;
                if (!visited[next]) {
                    visited[next] = true;
                    depth[next] = depth[current] + 1;
                    if (next == targetIndex) {
                        // Even if target is found, honor the depth cap — if it's
                        // beyond MAX_TRAVERSAL_DEPTH it is not considered reachable.
                        return depth[next] <= MAX_TRAVERSAL_DEPTH;
                    }
                    try {
                        queue.enqueue(next);
                    } catch (IllegalStateException overflow) {
                        // CustomQueue capacity hit — treat as exhausted frontier and halt gracefully.
                        return false;
                    }
                }
                neighbor = neighbor.next;
            }
        }
        return false;
    }

    /**
     * Determines whether {@code targetNode} is reachable from {@code startNode}
     * using a Depth-First Search that strictly enforces {@link #MAX_TRAVERSAL_DEPTH}.
     *
     * The depth cap protects against runaway recursion in disconnected or cyclic
     * sub-graphs: once a stack frame reaches the cap, its outgoing edges are
     * never pushed, so the algorithm always terminates.
     *
     * @param graph       the hospital network graph
     * @param startNode   ID of the starting ward (e.g., "ER")
     * @param targetNode  ID of the destination ward
     * @return true if target is reachable within the depth budget, false otherwise
     */
    public static boolean dfsReachability(CustomGraph graph, String startNode, String targetNode) {
        int startIndex = validateAndResolve(graph, startNode, targetNode);
        int targetIndex = graph.getIndex(targetNode);

        if (startIndex == targetIndex) {
            return true;
        }

        boolean[] visited = new boolean[graph.getNumNodes()];
        // CustomStack carries DfsFrame(nodeIndex, depth) so depth travels with each entry.
        CustomStack<DfsFrame> stack = new CustomStack<>();
        stack.push(new DfsFrame(startIndex, 0));
        visited[startIndex] = true;

        while (!stack.isEmpty()) {
            DfsFrame frame = stack.pop();
            int current = frame.nodeIndex;

            if (current == targetIndex) {
                return true;
            }

            // Depth cap: refuse to expand any node whose depth has already hit the budget.
            if (frame.depth >= MAX_TRAVERSAL_DEPTH) {
                continue;
            }

            EdgeNode neighbor = graph.getNeighbors(current);
            while (neighbor != null) {
                int next = neighbor.destinationIndex;
                if (!visited[next]) {
                    visited[next] = true;
                    stack.push(new DfsFrame(next, frame.depth + 1));
                }
                neighbor = neighbor.next;
            }
        }
        return false;
    }

    /**
     * Collects every ward ID reachable from {@code startNode} within
     * {@link #MAX_TRAVERSAL_DEPTH} using BFS. Returns the set in the order
     * each node was first discovered (level-order).
     *
     * @param graph     the hospital network graph
     * @param startNode ID of the starting ward (e.g., "ER")
     * @return list of reachable node IDs in BFS discovery order
     */
    public static java.util.List<String> reachableNodesBFS(CustomGraph graph, String startNode) {
        java.util.List<String> reachable = new java.util.ArrayList<>();
        int startIndex = graph.getIndex(startNode);
        if (startIndex == -1) {
            return reachable;
        }

        boolean[] visited = new boolean[graph.getNumNodes()];
        // Track depth per node to enforce the cap. BFS by itself has no implicit
        // depth, so we maintain a parallel array and skip expanding any node
        // whose depth would exceed the budget.
        int[] depth = new int[graph.getNumNodes()];

        CustomQueue<Integer> queue = new CustomQueue<>();
        queue.enqueue(startIndex);
        visited[startIndex] = true;
        depth[startIndex] = 0;
        reachable.add(graph.getNodeName(startIndex));

        while (!queue.isEmpty()) {
            int current = queue.dequeue();
            if (depth[current] >= MAX_TRAVERSAL_DEPTH) {
                continue;
            }
            EdgeNode neighbor = graph.getNeighbors(current);
            while (neighbor != null) {
                int next = neighbor.destinationIndex;
                if (!visited[next]) {
                    visited[next] = true;
                    depth[next] = depth[current] + 1;
                    reachable.add(graph.getNodeName(next));
                    try {
                        queue.enqueue(next);
                    } catch (IllegalStateException overflow) {
                        return reachable; // graceful halt on bounded queue overflow
                    }
                }
                neighbor = neighbor.next;
            }
        }
        return reachable;
    }

    /**
     * Resolves start and target IDs to indices, throwing a clear error
     * if either is unknown to the graph.
     */
    private static int validateAndResolve(CustomGraph graph, String startNode, String targetNode) {
        int startIndex = graph.getIndex(startNode);
        int targetIndex = graph.getIndex(targetNode);
        if (startIndex == -1 || targetIndex == -1) {
            throw new IllegalArgumentException(
                "Start or target node does not exist in the graph. start='" + startNode +
                "', target='" + targetNode + "'.");
        }
        return startIndex;
    }
}