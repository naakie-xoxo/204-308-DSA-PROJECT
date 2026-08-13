package ug.edu.ugmc.optimizer.algorithms.graph;
import ug.edu.ugmc.optimizer.graph.CustomGraph;
import ug.edu.ugmc.optimizer.datastructures.disjointset.DisjointSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GraphAndOptimizationTest {
    private CustomGraph graph;

    @BeforeEach
    void setUp() {
        graph = new CustomGraph(100);
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

}
