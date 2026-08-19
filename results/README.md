# Experiment results

The approved empirical lab writes actual measured evidence here:

- `benchmark_raw.csv`: every measured trial in nanoseconds.
- `benchmark_metadata.csv`: JVM, OS, hardware, trial count, scales, and seed
  provenance needed to reproduce the run.
- `sort_benchmarks.csv`, `search_benchmarks.csv`, and `graph_benchmarks.csv`:
  median summaries for the exact required scales.
- `*_runtimes.svg`: dependency-free report-ready plots generated directly from
  the median measurements.

Run from the repository root after compiling:

```powershell
mvn -q exec:java -Dexec.mainClass=ug.edu.ugmc.optimizer.experiments.PerformanceRunner
```

The runner creates and seeds `hospital_system.db` when it is absent. SQLite
loading, custom-structure construction, CSV writes, and console output are all
outside the `System.nanoTime()` measurement regions.
