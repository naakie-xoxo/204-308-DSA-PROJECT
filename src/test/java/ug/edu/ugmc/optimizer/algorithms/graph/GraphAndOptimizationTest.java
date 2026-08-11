package ug.edu.ugmc.optimizer.algorithms.graph;
import ug.edu.ugmc.optimizer.graph.CustomGraph;
import ug.edu.ugmc.optimizer.datastructures.disjointset.DisjointSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ug.edu.ugmc.optimizer.algorithms.optimization.GreedyOptimizer;
import ug.edu.ugmc.optimizer.algorithms.optimization.DPOptimizer;
import static org.junit.jupiter.api.Assertions.*;

class GraphAndOptimizationTest {
    private CustomGraph graph;

    @BeforeEach
    void setUp() {
        graph = new CustomGraph();
        graph.addNode("ER"); graph.addNode("Ward A"); graph.addNode("ICU");
        graph.addEdge("ER", "Ward A", 5);
        graph.addEdge("Ward A", "ICU", 10);
    }

    // Test 31: Graph - BFS Reachability
    @Test
    void testBFSReachability() {
        assertTrue(GraphTraversal.bfsReachability(graph, "ER", "ICU"));
    }

    // Test 32: Graph - DFS Disconnected Boundary
    @Test
    void testDFSDisconnected() {
        graph.addNode("Remote Clinic");
        assertFalse(GraphTraversal.dfsReachability(graph, "ER", "Remote Clinic"));
    }

    // Test 33: Dijkstra Shortest Path
    @Test
    void testDijkstraShortestPath() {
        graph.addEdge("ER", "ICU", 20); // Longer direct route
        // Shortest path should go ER -> Ward A -> ICU (cost 15)
        assertEquals(15, PathFinder.dijkstra(graph, "ER", "ICU"));
    }

    // Test 34: Prim's MST Cost
    @Test
    void testPrimMST() {
        assertEquals(15, PathFinder.primMSTCost(graph));
    }

    // Test 35: Kruskal's MST (Disjoint Set Check)
    @Test
    void testKruskalMST() {
        assertEquals(15, PathFinder.kruskalMSTCost(graph));
    }

    // Test 36: Disjoint Set Union/Find
    @Test
    void testDisjointSetConnectivity() {
        DisjointSet ds = new DisjointSet(5);
        ds.union(0, 1);
        ds.union(1, 2);
        assertTrue(ds.connected(0, 2));
        assertFalse(ds.connected(0, 4));
    }

    // Test 37: Greedy - Normal Operation
    @Test
    void testGreedyResourceAssignment() {
        int[] jobTimes = {3, 1, 2}; // Shortest job first
        assertArrayEquals(new int[]{1, 2, 3}, GreedyOptimizer.scheduleJobs(jobTimes));
    }

    // Test 38: Greedy - Documented Failure Case
    // Project Brief Section 10 requires this specific counterexample test
    @Test
    void testGreedyFailureCounterExample() {
        // e.g., Coin change where greedy fails (coins: 1, 3, 4 | target: 6)
        // Greedy takes 4, 1, 1 (3 coins). Optimal is 3, 3 (2 coins).
        int[] coins = {1, 3, 4};
        int greedyResult = GreedyOptimizer.coinChange(coins, 6);
        assertNotEquals(2, greedyResult, "Greedy algorithm fails to find optimal global maximum here.");
    }

    // Test 39: DP - Optimal Solution (Knapsack)
    @Test
    void testDPKnapsack() {
        int[] weights = {10, 20, 30};
        int[] values = {60, 100, 120};
        int capacity = 50;
        // Optimal is taking 20 and 30 (Value = 220)
        assertEquals(220, DPOptimizer.knapsack(weights, values, capacity));
    }
    
    // Test 40: DP - Zero Capacity Boundary
    @Test
    void testDPZeroCapacity() {
        int[] weights = {10, 20};
        int[] values = {60, 100};
        assertEquals(0, DPOptimizer.knapsack(weights, values, 0));
    }
}