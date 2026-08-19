package ug.edu.ugmc.optimizer.algorithms.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import ug.edu.ugmc.optimizer.datastructures.linear.DynamicArray;
import ug.edu.ugmc.optimizer.graph.CustomGraph;

/** Independent QA coverage for the UGMC BFS and DFS traversal contracts. */
class GraphTraversalTest {

    @Test
    void bfsVisitsHospitalLocationsLevelByLevel() {
        CustomGraph graph = branchingHospitalGraph();

        DynamicArray<String> order = GraphTraversal.bfsTraversal(graph, "ER");

        assertOrder(order, "ER", "Ward-A", "Ward-B", "ICU", "Pharmacy");
    }

    @Test
    void dfsFollowsOnePatrolRouteBeforeBacktracking() {
        CustomGraph graph = branchingHospitalGraph();

        DynamicArray<String> order = GraphTraversal.dfsTraversal(graph, "ER");

        assertOrder(order, "ER", "Ward-A", "ICU", "Ward-B", "Pharmacy");
    }

    @Test
    void disconnectedWardReturnsOnlyTheStartingLocation() {
        CustomGraph graph = new CustomGraph(1);
        graph.addNode("New-Wing");

        assertOrder(GraphTraversal.bfsTraversal(graph, "New-Wing"), "New-Wing");
        assertOrder(GraphTraversal.dfsTraversal(graph, "New-Wing"), "New-Wing");
    }

    @Test
    void invalidStartLocationIsRejectedClearly() {
        CustomGraph graph = new CustomGraph(1);
        graph.addNode("ER");

        assertThrows(IllegalArgumentException.class,
                () -> GraphTraversal.bfsTraversal(graph, "Unknown-Ward"));
        assertThrows(IllegalArgumentException.class,
                () -> GraphTraversal.dfsTraversal(graph, null));
        assertThrows(IllegalArgumentException.class,
                () -> GraphTraversal.bfsTraversal(null, "ER"));
    }

    @Test
    void traversalsReachEveryLocationBeyondSixHops() {
        CustomGraph graph = pathGraph(9);

        DynamicArray<String> bfsOrder = GraphTraversal.bfsTraversal(graph, "L0");
        DynamicArray<String> dfsOrder = GraphTraversal.dfsTraversal(graph, "L0");

        assertEquals(9, bfsOrder.size());
        assertEquals(9, dfsOrder.size());
        for (int index = 0; index < 9; index++) {
            assertEquals("L" + index, bfsOrder.get(index));
            assertEquals("L" + index, dfsOrder.get(index));
        }
        assertEquals(9, GraphTraversal.reachableNodesBFS(graph, "L0").size());
        assertTrue(GraphTraversal.bfsReachability(graph, "L0", "L7"));
        assertTrue(GraphTraversal.dfsReachability(graph, "L0", "L7"));
        assertTrue(GraphTraversal.bfsReachability(graph, "L0", "L8"));
        assertTrue(GraphTraversal.dfsReachability(graph, "L0", "L8"));
    }

    @Test
    void dfsTraversesBeyondTheAuditStacksRetentionLimit() {
        CustomGraph graph = pathGraph(150);

        DynamicArray<String> order = GraphTraversal.dfsTraversal(graph, "L0");

        assertEquals(150, order.size());
        assertEquals("L149", order.get(149));
        assertTrue(GraphTraversal.dfsReachability(graph, "L0", "L149"));
    }

    @Test
    void traversalsExcludeDisconnectedComponents() {
        CustomGraph graph = new CustomGraph(6);
        for (int index = 0; index < 3; index++) {
            graph.addNode("A" + index);
            graph.addNode("B" + index);
        }
        graph.addEdge("A0", "A1", 1);
        graph.addEdge("A1", "A2", 1);
        graph.addEdge("B0", "B1", 1);
        graph.addEdge("B1", "B2", 1);

        assertEquals(3, GraphTraversal.bfsTraversal(graph, "A0").size());
        assertEquals(3, GraphTraversal.dfsTraversal(graph, "A0").size());
        assertFalse(GraphTraversal.bfsReachability(graph, "A0", "B2"));
        assertFalse(GraphTraversal.dfsReachability(graph, "A0", "B2"));
    }

    @Test
    void cyclesDoNotRepeatVisitedLocations() {
        CustomGraph graph = new CustomGraph(4);
        for (int index = 0; index < 4; index++) {
            graph.addNode("C" + index);
        }
        graph.addEdge("C0", "C1", 1);
        graph.addEdge("C1", "C2", 1);
        graph.addEdge("C2", "C0", 1);
        graph.addEdge("C2", "C3", 1);

        assertEquals(4, GraphTraversal.bfsTraversal(graph, "C0").size());
        assertEquals(4, GraphTraversal.dfsTraversal(graph, "C0").size());
    }

    @Test
    void bfsHandlesAFrontierWiderThanTheMergedQueueCapacity() {
        CustomGraph graph = new CustomGraph(101);
        graph.addNode("ER");
        for (int index = 1; index <= 100; index++) {
            graph.addNode("Ward-" + index);
            graph.addEdge("ER", "Ward-" + index, 1);
        }

        assertEquals(101, GraphTraversal.bfsTraversal(graph, "ER").size());
    }

    private static CustomGraph branchingHospitalGraph() {
        CustomGraph graph = new CustomGraph(5);
        graph.addNode("ER");
        graph.addNode("Ward-A");
        graph.addNode("Ward-B");
        graph.addNode("ICU");
        graph.addNode("Pharmacy");

        // CustomGraph inserts adjacency-list edges at the head. Add Ward-B first
        // so Ward-A is the first ER neighbor examined by both traversals.
        graph.addEdge("ER", "Ward-B", 1);
        graph.addEdge("ER", "Ward-A", 1);
        graph.addEdge("Ward-A", "ICU", 1);
        graph.addEdge("Ward-B", "Pharmacy", 1);
        return graph;
    }

    private static CustomGraph pathGraph(int vertexCount) {
        CustomGraph graph = new CustomGraph(vertexCount);
        for (int index = 0; index < vertexCount; index++) {
            graph.addNode("L" + index);
        }
        for (int index = 0; index < vertexCount - 1; index++) {
            graph.addEdge("L" + index, "L" + (index + 1), 1);
        }
        return graph;
    }

    private static void assertOrder(DynamicArray<String> actual, String... expected) {
        assertEquals(expected.length, actual.size());
        for (int index = 0; index < expected.length; index++) {
            assertEquals(expected[index], actual.get(index), "Unexpected location at index " + index);
        }
    }
}
