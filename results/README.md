# Experiment results

`PerformanceRunner` writes actual measured evidence here:

- `benchmark_raw.csv`: every measured trial in nanoseconds.
- `benchmark_summary.csv`: arithmetic means, trial counts, and the associated
  scale/secondary parameter for every benchmark series.
- `benchmark_metadata.csv`: JVM, OS, hardware, trial count, scales, and seed
  provenance needed to reproduce the run.
- `sort_benchmarks.csv`, `search_benchmarks.csv`,
  `hash_load_factor_benchmarks.csv`, `tree_benchmarks.csv`,
  `priority_dispatch_benchmarks.csv`, `graph_benchmarks.csv`, and
  `mst_benchmarks.csv`: review-friendly arithmetic-mean summaries.
- `*_runtimes.svg` and `hash_collisions.svg`: dependency-free, report-ready
  plots generated directly from those summaries.

Every committed average comes from three individual rows in
`benchmark_raw.csv`. The raw and summary CSVs are checked for internal
consistency by `PerformanceRunnerTest`; tests do not assert machine-specific
nanosecond values.

Run from the repository root after compiling:

```powershell
mvn --batch-mode --no-transfer-progress exec:java "-Dexec.mainClass=ug.edu.ugmc.optimizer.experiments.PerformanceRunner"
```

The runner creates and seeds `hospital_system.db` when it is absent. SQLite
loading, deterministic workload construction, CSV/SVG writes, and console
output are outside the `System.nanoTime()` measurement regions.
