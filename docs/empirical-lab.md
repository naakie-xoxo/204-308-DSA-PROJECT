# Empirical efficiency lab

`PerformanceRunner` is the reproducible orchestrator for the performance-study
evidence. It loads request and road-weight seeds from SQLite, prepares
deterministic in-memory workloads, performs a 100-record unmeasured warm-up,
and then runs three measured trials for every algorithm/scale combination.

## Measurement contract

- Every measured runtime uses `System.nanoTime()`.
- Input generation, SQLite loading, result validation, CSV writes, SVG
  generation, and console output are outside timed regions.
- Mutating operations receive fresh state for every measured trial.
- Every summary is the arithmetic mean of its three raw measurements, rounded
  to the nearest nanosecond. `benchmark_raw.csv` retains every individual run.
- A volatile result sink consumes results so each timed operation performs real
  work.
- Result correctness is checked after timing: sorted arrays, successful
  searches, connected traversals, and matching Prim/Kruskal MST costs.

## Experiments and scales

- Search and sort: 100, 500, 1,000, 5,000, 10,000, and 50,000 records. Linear
  and Binary Search use the same presorted requests; Merge, Quick, Selection,
  and Insertion Sort receive equivalent copies of the same deterministic data.
- Hash-table load factor: 100, 500, 1,000, 5,000, 10,000, and 20,000 keys at
  target load factors 0.50, 0.75, 1.00, 1.50, and 2.00. The brief does not
  prescribe exact factors; these are documented experimental choices. The
  custom table is measured for batch insertion and successful batch lookup,
  and collision counts are retained.
- BST versus balanced tree: 100, 250, 500, 1,000, and 2,000 ascending request
  keys. The same order deliberately exposes the ordinary BST's degenerate
  height while the project's Red-Black Tree remains balanced. Batch insertion
  and successful batch lookup are measured.
- Priority dispatch: 100, 500, 1,000, 5,000, 10,000, and 20,000 requests. The
  application's custom heap-backed priority queue is measured for batch insert
  and full priority-order extraction.
- Graph algorithms: 50, 100, 200, and 500 vertices. BFS, DFS, Dijkstra, Prim,
  and Kruskal use the same deterministic connected graph at each scale.

## Graph generation

The canonical hospital dataset contains 50 locations, so larger scaling graphs
are clearly synthetic rather than claims about real UGMC locations. For each
required vertex count, the runner creates `BENCH-LOC-0` through
`BENCH-LOC-(V-1)`, first adds a connected path backbone, then adds deterministic
chord edges until `E = 2V`. Edge weights cycle through the canonical SQLite
`roads.travel_time` values. Density therefore remains controlled and source
and target are always `BENCH-LOC-0` and `BENCH-LOC-(V-1)`.

## Evidence outputs

- `results/benchmark_raw.csv`: all individual measured trials.
- `results/benchmark_summary.csv`: one arithmetic-mean row per experiment,
  algorithm, scale, and secondary parameter.
- Experiment-specific summary CSVs and average-runtime SVGs under `results/`.
- `data/algorithm_runs.csv`: canonical raw algorithm-run export used by the
  application.
- `docs/evidence/performance-study.md`: theory-versus-observed interpretation
  tied to the committed measured values.

## Reproduction

From the repository root:

```powershell
mvn --batch-mode --no-transfer-progress test
mvn --batch-mode --no-transfer-progress exec:java "-Dexec.mainClass=ug.edu.ugmc.optimizer.experiments.PerformanceRunner"
```

The runner initializes `hospital_system.db` from canonical CSV seeds when
needed and replaces the generated performance evidence with measurements from
the current machine. Nanosecond values will vary across machines and runs; the
schema, scales, deterministic inputs, trial count, and aggregation method do
not.
