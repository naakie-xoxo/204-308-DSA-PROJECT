package ug.edu.ugmc.optimizer.experiments;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ug.edu.ugmc.optimizer.algorithms.search.CustomSearch;
import ug.edu.ugmc.optimizer.algorithms.sort.InsertionSort;
import ug.edu.ugmc.optimizer.algorithms.sort.QuickSort;
import ug.edu.ugmc.optimizer.datastructures.linear.DynamicArray;

/** Contract tests for the empirical lab's assessed scales and parameters. */
class PerformanceRunnerTest {

    @Test
    void exposesEveryRequiredScaleAndTrialContract() {
        assertArrayEquals(
                new int[] {100, 500, 1_000, 5_000, 10_000, 50_000},
                PerformanceRunner.getDataScales());
        assertArrayEquals(
                new int[] {50, 100, 200, 500}, PerformanceRunner.getGraphScales());
        assertArrayEquals(
                new int[] {100, 500, 1_000, 5_000, 10_000, 20_000},
                PerformanceRunner.getHashScales());
        assertArrayEquals(
                new double[] {0.50, 0.75, 1.00, 1.50, 2.00},
                PerformanceRunner.getHashLoadFactors());
        assertArrayEquals(
                new int[] {100, 250, 500, 1_000, 2_000},
                PerformanceRunner.getTreeScales());
        assertArrayEquals(
                new int[] {100, 500, 1_000, 5_000, 10_000, 20_000},
                PerformanceRunner.getPriorityScales());
        assertEquals(3, PerformanceRunner.getMinimumMeasuredTrials());
        assertArrayEquals(
                new String[] {
                    "Sort", "Search", "HashLoadFactor", "TreeComparison",
                    "PriorityDispatch", "GraphTraversal", "MST"
                },
                PerformanceRunner.getExperimentCategories());
    }

    @Test
    void arithmeticMeanUsesEveryRunRatherThanTheMedian() {
        assertEquals(34, PerformanceRunner.arithmeticMean(new long[] {1, 2, 100}));
        assertEquals(20, PerformanceRunner.arithmeticMean(new long[] {10, 20, 30}));
        assertThrows(IllegalArgumentException.class,
                () -> PerformanceRunner.arithmeticMean(new long[0]));
    }

    @Test
    void presortedBinarySearchKeepsThePureLogarithmicLookupApi() {
        DynamicArray<Integer> ids = new DynamicArray<>(5);
        for (int id = 0; id < 5; id++) {
            ids.insert(id);
        }

        assertEquals(4, CustomSearch.binarySearchPresorted(ids, 4));
        assertEquals(-1, CustomSearch.binarySearchPresorted(ids, 9));
        assertThrows(IllegalArgumentException.class,
                () -> CustomSearch.binarySearchPresorted(null, 1));
    }

    @Test
    void insertionPenaltyAndQuickSortCutoffAreObservable() {
        DynamicArray<Integer> descending = new DynamicArray<>(4);
        descending.insert(4);
        descending.insert(3);
        descending.insert(2);
        descending.insert(1);
        InsertionSort.sort(descending);

        assertEquals(6, InsertionSort.getShiftPenaltyApplications());
        assertEquals(6L * 749L, InsertionSort.getWeightedShiftCost());

        DynamicArray<Integer> quickInput = new DynamicArray<>(10);
        for (int value = 10; value > 0; value--) {
            quickInput.insert(value);
        }
        QuickSort.sort(quickInput);

        assertEquals(12, QuickSort.getSubarrayCutoff());
        assertTrue(QuickSort.getInsertionFallbackCount() > 0);
    }

    @Test
    void committedRawRunsAndSummariesAreInternallyConsistent() throws IOException {
        Path rawPath = Path.of("results", "benchmark_raw.csv");
        Path summaryPath = Path.of("results", "benchmark_summary.csv");
        assertTrue(Files.isRegularFile(rawPath));
        assertTrue(Files.isRegularFile(summaryPath));

        List<String> rawLines = Files.readAllLines(rawPath);
        List<String> summaryLines = Files.readAllLines(summaryPath);
        assertEquals(
                "experiment,algorithm,input_size,secondary_parameter,trial,runtime_ns,"
                        + "memory_kb,metric_name,metric_value,result_value",
                rawLines.get(0));
        assertEquals(
                "experiment,algorithm,input_size,secondary_parameter,trials,"
                        + "average_runtime_ns,metric_name,metric_value",
                summaryLines.get(0));

        Map<String, List<Long>> rawTimes = new HashMap<>();
        for (int line = 1; line < rawLines.size(); line++) {
            String[] columns = csvColumns(rawLines.get(line), 10);
            int trial = Integer.parseInt(columns[4]);
            long runtime = Long.parseLong(columns[5]);
            assertTrue(trial >= 1);
            assertTrue(runtime >= 0);
            rawTimes.computeIfAbsent(groupKey(columns), ignored -> new ArrayList<>())
                    .add(runtime);
        }

        Set<String> categories = new HashSet<>();
        Set<Integer> graphScales = new HashSet<>();
        for (int line = 1; line < summaryLines.size(); line++) {
            String[] columns = csvColumns(summaryLines.get(line), 8);
            categories.add(columns[0]);
            int inputSize = Integer.parseInt(columns[2]);
            int trials = Integer.parseInt(columns[4]);
            long average = Long.parseLong(columns[5]);
            List<Long> measurements = rawTimes.get(groupKey(columns));
            assertTrue(measurements != null, "Summary group has no raw measurements");
            assertEquals(trials, measurements.size());
            assertTrue(trials >= PerformanceRunner.getMinimumMeasuredTrials());
            long[] values = new long[measurements.size()];
            for (int index = 0; index < values.length; index++) {
                values[index] = measurements.get(index);
            }
            assertEquals(PerformanceRunner.arithmeticMean(values), average);
            if (columns[0].equals("GraphTraversal") || columns[0].equals("MST")) {
                graphScales.add(inputSize);
            }
        }

        assertEquals(Set.of(PerformanceRunner.getExperimentCategories()), categories);
        assertEquals(Set.of(50, 100, 200, 500), graphScales);
    }

    private static String groupKey(String[] columns) {
        return columns[0] + "|" + columns[1] + "|" + columns[2] + "|" + columns[3];
    }

    private static String[] csvColumns(String line, int expectedColumns) {
        String[] columns = line.split(",", -1);
        assertEquals(expectedColumns, columns.length, "Unexpected CSV column count");
        for (int index = 0; index < columns.length; index++) {
            String value = columns[index];
            if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                columns[index] = value.substring(1, value.length() - 1).replace("\"\"", "\"");
            }
        }
        return columns;
    }
}
