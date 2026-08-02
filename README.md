# UGMC Smart Service Operations Optimizer

Java and SQLite foundation for the University of Ghana DCIT 204/308 joint Data Structures and Algorithms project. The team will implement the assessed data structures and algorithms; this repository supplies the shared build, seed data, database bootstrap, package boundaries, tests, and collaboration workflow.

## Prerequisites

- JDK 17 or newer
- Apache Maven 3.9 or newer
- Git

Check the tools with `java -version`, `javac -version`, `mvn -version`, and `git --version`.

## First-time setup

1. Clone the repository and create a feature branch.
2. Run `mvn test` to compile the project and verify the shared foundation.
3. Create a local SQLite database:

   ```text
   mvn -q compile exec:java -Dexec.mainClass=ug.edu.ugmc.optimizer.database.DatabaseManager
   ```

4. Run the application shell:

   ```text
   mvn -q compile exec:java -Dexec.mainClass=ug.edu.ugmc.optimizer.App
   ```

`hospital_system.db` is generated locally and intentionally ignored by Git. The authoritative database inputs are `schema.sql` and the CSV files in `data/`.

If you used the older repository version, delete its existing `hospital_system.db` once before running setup; that file used an incompatible schema and contained duplicated roads.

## Shared data contract

- `locations.csv`: 50 hospital locations.
- `roads.csv`: 100 unique undirected weighted roads. A graph loader should add both adjacency directions for every row.
- `service_requests.csv`: 300 pending service requests.
- `resources.csv`: 30 hospital resources.
- `algorithm_runs.csv`: 30 clearly labelled mock rows used only to verify the experiment schema. Replace these with measured results for the final report.

Running the database loader more than once is safe: primary keys and upserts prevent duplicate seed rows. Foreign-key checks are enabled during setup.

## Team work areas

- **Group A — 6 members, Core Data Structures and Indexing:** `datastructures/` and `algorithms/search/`.
- **Group B — 6 assignment areas held by 5 members, Graph Routing, Optimization and Sorting:** `graph/`, `algorithms/graph/`, `algorithms/sort/`, and `algorithms/optimization/`.
- **Group C — 5 members, Architecture, Database, Console UI, QA and Evidence:** `application/`, `database/`, `ui/console/`, `experiments/`, and cross-project tests.

There are 15 unique members because Naakie owns multiple assignments across Groups B and C. The named allocation is in `docs/team-workstreams.md`, and the dependency rules and folder structure are in `docs/architecture.md`. Package documentation defines boundaries without implementing the assessed work.

## Architecture summary

The program follows this dependency flow:

```text
Console UI -> Application services -> Custom structures and algorithms
                                 -> Repository ports <- SQLite adapters
```

`App` is only the composition root. The UI must not query SQLite or instantiate individual algorithms directly. Tests mirror the production packages, while benchmarks remain separate from unit tests so CI results do not depend on machine speed.

## Collaboration rules

- Never commit directly to `main`.
- Create a small branch such as `group-a/hash-table`, `group-b/dijkstra`, or `group-c/database-loader`.
- Keep one primary assignment per pull request.
- Coordinate new features with Maron so the central 40+ test suite covers normal, boundary, and invalid-input behavior.
- Do not use Java's built-in `HashMap`, `TreeMap`, `PriorityQueue`, `Stack`, `ArrayDeque`, or equivalent structures for assessed core logic.
- Rebase or merge the latest `main` before requesting review.
- Require at least one teammate review and a passing CI build before merging.

See `CONTRIBUTING.md` for the complete workflow and definition of done.

## Optional seed regeneration

The committed roads are deterministic. To regenerate the same 100-road dataset:

```text
mvn -q compile exec:java -Dexec.mainClass=ug.edu.ugmc.optimizer.tools.RoadGenerator
```

Seed generators may use Java collections because they are tooling, not assessed data-structure implementations.
