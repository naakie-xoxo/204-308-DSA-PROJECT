package ug.edu.ugmc.optimizer.algorithms.graph;

import org.junit.jupiter.api.Test;
import ug.edu.ugmc.optimizer.graph.CustomGraph;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Contract tests for Dijkstra's input and arithmetic safeguards. */
class DijkstraHardeningTest {

    @Test
    void returnsMinusOneForAnUnreachableTarget() {
        CustomGraph graph = new CustomGraph(2);
        graph.addNode("ER");
        graph.addNode("ICU");

        PathFinder.ShortestPathResult result = PathFinder.dijkstraWithRoute(graph, "ER", "ICU");

        assertEquals(-1, result.getDistance());
        assertArrayEquals(new String[0], result.getRoute());
    }

    @Test
    void rejectsNegativeWeights() {
        CustomGraph graph = new CustomGraph(2);
        graph.addNode("ER");
        graph.addNode("ICU");
        graph.addEdge("ER", "ICU", -1);

        assertThrows(IllegalArgumentException.class,
                () -> PathFinder.dijkstra(graph, "ER", "ICU"));
    }

    @Test
    void rejectsInvalidLocationArguments() {
        CustomGraph graph = new CustomGraph(1);
        graph.addNode("ER");

        assertThrows(IllegalArgumentException.class, () -> PathFinder.dijkstra(null, "ER", "ER"));
        assertThrows(IllegalArgumentException.class, () -> PathFinder.dijkstra(graph, null, "ER"));
        assertThrows(IllegalArgumentException.class, () -> PathFinder.dijkstra(graph, " ", "ER"));
        assertThrows(IllegalArgumentException.class, () -> PathFinder.dijkstra(graph, "ER", "ICU"));
    }

    @Test
    void reportsLongRouteTotalsInsteadOfWrapping() {
        CustomGraph graph = new CustomGraph(3);
        graph.addNode("A");
        graph.addNode("B");
        graph.addNode("C");
        graph.addEdge("A", "B", Integer.MAX_VALUE);
        graph.addEdge("B", "C", 1);

        assertThrows(ArithmeticException.class, () -> PathFinder.dijkstra(graph, "A", "C"));
    }

    @Test
    void reconstructsTheUnambiguousShortestHospitalRoute() {
        CustomGraph graph = new CustomGraph(3);
        graph.addNode("ER");
        graph.addNode("Ward-A");
        graph.addNode("ICU");
        graph.addEdge("ER", "Ward-A", 5);
        graph.addEdge("Ward-A", "ICU", 10);
        graph.addEdge("ER", "ICU", 20);

        PathFinder.ShortestPathResult result = PathFinder.dijkstraWithRoute(graph, "ER", "ICU");
        String[] route = result.getRoute();

        assertEquals(15, result.getDistance());
        assertArrayEquals(new String[] {"ER", "Ward-A", "ICU"}, route);
        assertEquals("ER", route[0]);
        assertEquals("ICU", route[route.length - 1]);
    }

    @Test
    void priorityQueueExpandsForA500NodeGraph() {
        int nodeCount = 500;
        CustomGraph graph = new CustomGraph(nodeCount);
        graph.addNode("ER");
        for (int i = 1; i < nodeCount; i++) {
            graph.addNode("Ward-" + i);
            graph.addEdge("ER", "Ward-" + i, 1);
        }

        assertEquals(1, PathFinder.dijkstra(graph, "ER", "Ward-499"));
    }
}
