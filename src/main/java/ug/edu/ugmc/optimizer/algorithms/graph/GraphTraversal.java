package ug.edu.ugmc.optimizer.algorithms.graph;

import ug.edu.ugmc.optimizer.datastructures.linear.DynamicArray;
import ug.edu.ugmc.optimizer.datastructures.queues.CustomQueue;
import ug.edu.ugmc.optimizer.datastructures.queues.CustomStack;
import ug.edu.ugmc.optimizer.graph.CustomGraph;
import ug.edu.ugmc.optimizer.graph.CustomGraph.EdgeNode;

/**
 * Breadth-first and depth-first traversal utilities for the UGMC hospital road
 * network. All traversal state is held in project-owned data structures or
 * primitive arrays; no Java collection implementation is used.
 */
public final class GraphTraversal {

    private static final int MAX_TRAVERSAL_DEPTH = (22394896 % 15) + 5;

    private GraphTraversal() {
        // Utility class.
    }

    /** Carries a node's hop count through the BFS frontier. */
    private static final class BfsFrame {
        private final int nodeIndex;
        private final int depth;

        private BfsFrame(int nodeIndex, int depth) {
            this.nodeIndex = nodeIndex;
            this.depth = depth;
        }
    }

    /**
     * Keeps one DFS frame per active path level. Advancing {@code nextNeighbor}
     * before pushing a child lets the iterative algorithm backtrack correctly
     * without placing every sibling on the stack at once.
     */
    private static final class DfsFrame {
        private final int nodeIndex;
        private final int depth;
        private EdgeNode nextNeighbor;

        private DfsFrame(int nodeIndex, int depth, EdgeNode nextNeighbor) {
            this.nodeIndex = nodeIndex;
            this.depth = depth;
            this.nextNeighbor = nextNeighbor;
        }
    }

    /**
     * Chains the merged fixed-capacity queues so a wide hospital level is not
     * silently truncated when its frontier contains more than 56 locations.
     * Every traversal node is still enqueued in a {@link CustomQueue}.
     */
    private static final class QueueSegment<T> {
        private final CustomQueue<T> queue = new CustomQueue<>();
        private QueueSegment<T> next;
    }

    /** FIFO adapter over one or more project-owned queue segments. */
    private static final class TraversalQueue<T> {
        private QueueSegment<T> head;
        private QueueSegment<T> tail;

        private void enqueue(T item) {
            if (tail == null) {
                head = new QueueSegment<>();
                tail = head;
            } else if (tail.queue.isFull()) {
                tail.next = new QueueSegment<>();
                tail = tail.next;
            }
            tail.queue.enqueue(item);
        }

        private T dequeue() {
            if (isEmpty()) {
                throw new IllegalStateException("Traversal queue is empty.");
            }

            T item = head.queue.dequeue();
            if (head.queue.isEmpty()) {
                head = head.next;
                if (head == null) {
                    tail = null;
                }
            }
            return item;
        }

        private boolean isEmpty() {
            return head == null;
        }
    }

    /**
     * Traverses reachable hospital locations in breadth-first discovery order.
     * The starting location has depth {@code 0}; locations at depth
     * {@link #MAX_TRAVERSAL_DEPTH} are included but are not expanded.
     *
     * <p>Time complexity: O(V + E), where V and E are the locations and roads
     * examined within the depth limit. Space complexity: O(V).</p>
     *
     * @param graph UGMC hospital road network
     * @param startNode ID of the location at which traversal begins
     * @return location IDs in exact BFS discovery order
     * @throws IllegalArgumentException if the graph is null or the starting ID
     *         is null, blank, or absent from the graph
     */
    public static DynamicArray<String> bfsTraversal(CustomGraph graph, String startNode) {
        int startIndex = validateAndResolve(graph, startNode, "startNode");
        int nodeCount = graph.getNumNodes();
        boolean[] visited = new boolean[nodeCount];
        DynamicArray<String> traversalOrder = new DynamicArray<>();
        TraversalQueue<BfsFrame> frontier = new TraversalQueue<>();

        visited[startIndex] = true;
        traversalOrder.insert(graph.getNodeName(startIndex));
        frontier.enqueue(new BfsFrame(startIndex, 0));

        while (!frontier.isEmpty()) {
            BfsFrame current = frontier.dequeue();
            if (current.depth >= MAX_TRAVERSAL_DEPTH) {
                continue;
            }

            EdgeNode neighbor = graph.getNeighbors(current.nodeIndex);
            while (neighbor != null) {
                int neighborIndex = requireValidNeighbor(neighbor, nodeCount);
                if (!visited[neighborIndex]) {
                    visited[neighborIndex] = true;
                    traversalOrder.insert(graph.getNodeName(neighborIndex));
                    frontier.enqueue(new BfsFrame(neighborIndex, current.depth + 1));
                }
                neighbor = neighbor.next;
            }
        }

        return traversalOrder;
    }

    /**
     * Traverses reachable hospital locations in iterative depth-first order.
     * The explicit {@link CustomStack} contains only the active patrol path, so
     * its size is bounded by {@code MAX_TRAVERSAL_DEPTH + 1}.
     *
     * <p>Time complexity: O(V + E), where V and E are the locations and roads
     * examined within the depth limit. Space complexity: O(V).</p>
     *
     * @param graph UGMC hospital road network
     * @param startNode ID of the location at which traversal begins
     * @return location IDs in exact DFS discovery order
     * @throws IllegalArgumentException if the graph is null or the starting ID
     *         is null, blank, or absent from the graph
     */
    public static DynamicArray<String> dfsTraversal(CustomGraph graph, String startNode) {
        int startIndex = validateAndResolve(graph, startNode, "startNode");
        int nodeCount = graph.getNumNodes();
        boolean[] visited = new boolean[nodeCount];
        DynamicArray<String> traversalOrder = new DynamicArray<>();
        CustomStack<DfsFrame> stack = new CustomStack<>();

        visited[startIndex] = true;
        traversalOrder.insert(graph.getNodeName(startIndex));
        stack.push(new DfsFrame(startIndex, 0, graph.getNeighbors(startIndex)));

        while (!stack.isEmpty()) {
            DfsFrame current = stack.peek();

            if (current.depth >= MAX_TRAVERSAL_DEPTH || current.nextNeighbor == null) {
                stack.pop();
                continue;
            }

            EdgeNode neighbor = current.nextNeighbor;
            current.nextNeighbor = neighbor.next;
            int neighborIndex = requireValidNeighbor(neighbor, nodeCount);

            if (!visited[neighborIndex]) {
                visited[neighborIndex] = true;
                traversalOrder.insert(graph.getNodeName(neighborIndex));
                stack.push(new DfsFrame(
                        neighborIndex,
                        current.depth + 1,
                        graph.getNeighbors(neighborIndex)));
            }
        }

        return traversalOrder;
    }

    /** Alias retaining the conventional short BFS method name. */
    public static DynamicArray<String> bfs(CustomGraph graph, String startNode) {
        return bfsTraversal(graph, startNode);
    }

    /** Alias retaining the conventional short DFS method name. */
    public static DynamicArray<String> dfs(CustomGraph graph, String startNode) {
        return dfsTraversal(graph, startNode);
    }

    /** Compatibility alias for callers that request all BFS-reachable IDs. */
    public static DynamicArray<String> reachableNodesBFS(CustomGraph graph, String startNode) {
        return bfsTraversal(graph, startNode);
    }

    /** Returns whether the target is reachable by BFS within the depth limit. */
    public static boolean bfsReachability(CustomGraph graph, String startNode, String targetNode) {
        validateAndResolve(graph, targetNode, "targetNode");
        return contains(bfsTraversal(graph, startNode), targetNode);
    }

    /** Returns whether the target is reachable by DFS within the depth limit. */
    public static boolean dfsReachability(CustomGraph graph, String startNode, String targetNode) {
        validateAndResolve(graph, targetNode, "targetNode");
        return contains(dfsTraversal(graph, startNode), targetNode);
    }

    private static int validateAndResolve(CustomGraph graph, String nodeId, String parameterName) {
        if (graph == null) {
            throw new IllegalArgumentException("Graph cannot be null.");
        }
        if (nodeId == null || nodeId.trim().isEmpty()) {
            throw new IllegalArgumentException(parameterName + " cannot be null or blank.");
        }

        int nodeIndex = graph.getIndex(nodeId);
        if (nodeIndex < 0) {
            throw new IllegalArgumentException(
                    parameterName + " does not exist in the graph: " + nodeId + ".");
        }
        return nodeIndex;
    }

    private static int requireValidNeighbor(EdgeNode neighbor, int nodeCount) {
        int neighborIndex = neighbor.destinationIndex;
        if (neighborIndex < 0 || neighborIndex >= nodeCount) {
            throw new IllegalStateException("Graph contains an invalid edge destination.");
        }
        return neighborIndex;
    }

    private static boolean contains(DynamicArray<String> locations, String targetNode) {
        for (int index = 0; index < locations.size(); index++) {
            if (locations.get(index).equals(targetNode)) {
                return true;
            }
        }
        return false;
    }
}
