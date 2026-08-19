package ug.edu.ugmc.optimizer.algorithms.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import ug.edu.ugmc.optimizer.graph.CustomGraph;

class MstAlgorithmsTest {

    @Test
    void primAndKruskalAgreeOnConnectedGraph() {
        CustomGraph graph = connectedGraph();

        assertEquals(10, PathFinder.primMSTCost(graph));
        assertEquals(10, PathFinder.kruskalMSTCost(graph));
    }

    @Test
    void primAndKruskalExposeTheSelectedEdgesAsWellAsCost() {
        CustomGraph graph = connectedGraph();

        PathFinder.MstResult prim = PathFinder.primMST(graph);
        PathFinder.MstResult kruskal = PathFinder.kruskalMST(graph);

        assertMstResult(prim);
        assertMstResult(kruskal);
    }

    @Test
    void primAndKruskalRejectDisconnectedGraph() {
        CustomGraph graph = new CustomGraph(3);
        graph.addNode("ER");
        graph.addNode("ICU");
        graph.addNode("Remote");
        graph.addEdge("ER", "ICU", 4);

        assertEquals(-1, PathFinder.primMSTCost(graph));
        assertEquals(-1, PathFinder.kruskalMSTCost(graph));
    }

    @Test
    void emptyAndSingleNodeGraphsHaveZeroCost() {
        CustomGraph empty = new CustomGraph(1);
        CustomGraph single = new CustomGraph(1);
        single.addNode("ER");

        assertEquals(0, PathFinder.primMSTCost(empty));
        assertEquals(0, PathFinder.kruskalMSTCost(empty));
        assertEquals(0, PathFinder.primMSTCost(single));
        assertEquals(0, PathFinder.kruskalMSTCost(single));
    }

    @Test
    void nullGraphIsRejectedClearly() {
        assertThrows(IllegalArgumentException.class, () -> PathFinder.primMSTCost(null));
        assertThrows(IllegalArgumentException.class, () -> PathFinder.kruskalMSTCost(null));
    }

    private static CustomGraph connectedGraph() {
        CustomGraph graph = new CustomGraph(4);
        graph.addNode("ER");
        graph.addNode("ICU");
        graph.addNode("Lab");
        graph.addNode("Ward");
        graph.addEdge("ER", "ICU", 4);
        graph.addEdge("ER", "Lab", 3);
        graph.addEdge("ICU", "Lab", 2);
        graph.addEdge("ICU", "Ward", 5);
        graph.addEdge("Lab", "Ward", 8);
        return graph;
    }

    private static void assertMstResult(PathFinder.MstResult result) {
        assertTrue(result.isConnected());
        assertEquals(10, result.getTotalCost());
        assertEquals(3, result.getEdges().length);

        int edgeWeightSum = 0;
        for (PathFinder.MstEdge edge : result.getEdges()) {
            edgeWeightSum += edge.getWeight();
        }
        assertEquals(result.getTotalCost(), edgeWeightSum);
    }
}
