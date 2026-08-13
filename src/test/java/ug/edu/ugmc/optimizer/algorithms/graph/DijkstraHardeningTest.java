package ug.edu.ugmc.optimizer.algorithms.graph;

import org.junit.jupiter.api.Test;
import ug.edu.ugmc.optimizer.graph.CustomGraph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Contract tests for Dijkstra's input and arithmetic safeguards. */
class DijkstraHardeningTest {

    @Test
    void returnsMinusOneForAnUnreachableTarget() {
        CustomGraph graph = new CustomGraph(2);
        graph.addNode("ER");
        graph.addNode("ICU");

        assertEquals(-1, PathFinder.dijkstra(graph, "ER", "ICU"));
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
    void priorityQueueExpandsForManyRelaxations() {
        int targetCount = 200;
        CustomGraph graph = new CustomGraph(targetCount + 1);
        graph.addNode("ER");
        for (int i = 0; i < targetCount; i++) {
            graph.addNode("Ward-" + i);
            graph.addEdge("ER", "Ward-" + i, 1);
        }

        assertEquals(1, PathFinder.dijkstra(graph, "ER", "Ward-199"));
    }
}
