# Empirical efficiency lab

`PerformanceRunner` is the single orchestrator for the report evidence. It
loads seed requests and road weights from SQLite, expands deterministic
in-memory workloads, warms the JVM with 100 unrecorded records, then measures
the algorithms at exactly 100, 500, 1,000, 5,000, 10,000, and 50,000 records.

## Measurement contract

- Every official runtime uses `System.nanoTime()`.
- SQLite reads, custom-structure population, result validation, CSV writes,
  graph plotting, and console output are outside timed regions.
- Three trials are recorded for each algorithm and scale. Summary CSVs and SVG
  plots use the median, while `benchmark_raw.csv` retains every observation.
- A volatile result sink prevents the JVM from treating algorithm results as
  unused.
- Each sorted output and each search/routing result is validated after its
  timer stops.

## Match-ups

- Triage sorting: Merge Sort and QuickSort versus Selection Sort and Insertion
  Sort. Raw evidence includes QuickSort's cutoff value and fallback count, plus
  Insertion Sort's weighted shift cost and penalty-application count.
- Patient lookup: Linear Search versus the pure presorted Binary Search lookup.
  Sorting and sortedness preparation occur before the search timer.
- Routing: Dijkstra versus BFS over the same `CustomGraph` instance at each
  scale.

## Meaning of the graph scale

`CustomGraph` deliberately maintains both an adjacency list and an adjacency
matrix. A matrix with 50,000 locations would require 2.5 billion integer cells
before any roads were stored, which is not a valid experiment on the submitted
data structure. Therefore the graph input size is the number of unique,
undirected road records, capped at 500 hospital locations. The 50,000 scale
really inserts and processes 50,000 unique roads; it does not claim to allocate
a 50,000-by-50,000 matrix.

## Reproduction

From the repository root:

```powershell
mvn -q test
mvn -q exec:java "-Dexec.mainClass=ug.edu.ugmc.optimizer.experiments.PerformanceRunner"
```

The second command creates `hospital_system.db` from the canonical CSV seeds if
needed, replaces `data/algorithm_runs.csv`, and writes raw data, medians,
metadata, and SVG plots to `results/`.
