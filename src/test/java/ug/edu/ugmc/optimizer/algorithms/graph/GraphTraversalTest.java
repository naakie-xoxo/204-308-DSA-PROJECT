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
    void traversalsStopAfterSixHops() {
        CustomGraph graph = new CustomGraph(9);
        for (int index = 0; index < 9; index++) {
            graph.addNode("L" + index);
        }
        for (int index = 0; index < 8; index++) {
            graph.addEdge("L" + index, "L" + (index + 1), 1);
        }

        DynamicArray<String> bfsOrder = GraphTraversal.bfsTraversal(graph, "L0");
        DynamicArray<String> dfsOrder = GraphTraversal.dfsTraversal(graph, "L0");

        assertEquals(7, bfsOrder.size());
        assertEquals("L6", bfsOrder.get(6));
        assertEquals(7, dfsOrder.size());
        assertEquals("L6", dfsOrder.get(6));
        assertFalse(GraphTraversal.bfsReachability(graph, "L0", "L7"));
        assertFalse(GraphTraversal.dfsReachability(graph, "L0", "L7"));
        assertTrue(GraphTraversal.bfsReachability(graph, "L0", "L6"));
        assertTrue(GraphTraversal.dfsReachability(graph, "L0", "L6"));
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

    private static void assertOrder(DynamicArray<String> actual, String... expected) {
        assertEquals(expected.length, actual.size());
        for (int index = 0; index < expected.length; index++) {
            assertEquals(expected[index], actual.get(index), "Unexpected location at index " + index);
        }
    }
}
