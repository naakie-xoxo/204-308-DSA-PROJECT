# Development Log

This is a repository-grounded development chronology created to preserve
verifiable evidence of how the project progressed. It was reconstructed from
Git commits, merged and closed GitHub pull requests, changed-file records, PR
descriptions and GitHub Actions results. For PR milestones, the date is the
GitHub merge date; standalone events use the Git commit date. The chronology
does not attempt to recreate undocumented meetings, conversations or decisions.

| Date | Development milestone | Repository evidence | Result/status |
| ---- | --------------------- | ------------------- | ------------- |
| 2026-07-29 | The repository was initialised with the first UGMC-oriented seed datasets, SQLite schema/database work and early generator/application files. | Commit `77dd3a6` (`init commit`). | Initial repository state recorded. |
| 2026-08-02 | The codebase was reorganised into the collaborative Maven package structure and gained CI, contribution rules, architecture documentation, workstreams and seed-data documentation. | Commit `7a67c49` (`Set up collaborative DSA project foundation`). | Shared build and collaboration foundation established. |
| 2026-08-04 | A broad mandatory QA baseline was added across graph/optimisation, sort/search, hashing/trees, heap, linear and queue/stack packages. | Merged PR #1, `test(group-c): implement 40 mandatory QA tests across all modules`; six test files added. | Test-first coverage was established, although the recorded early CI runs were failing while implementation dependencies were incomplete. |
| 2026-08-05 | Initial sorting and linear-structure implementations were integrated: selection, insertion, merge and quick sort, plus `DynamicArray`, `SinglyLinkedList` and a custom iterator. | Merged PRs #2 and #3. | Initial implementations merged; later PRs corrected integration and resize defects. |
| 2026-08-10 | The custom-structure and core-algorithm library expanded with `MinHeap`, `CustomPriorityQueue`, red-black tree, B-tree, stack, FIFO queue, deque, circular queue, `CustomHashTable`, DP knapsack, `DisjointSet` and Dijkstra integration. BST test imports and a `DynamicArray` resize data-loss defect were corrected during the same integration period. | Merged PRs #6-#14; corrective PRs #12 and #13; PRs #9 and #10 contain the same DP/Disjoint Set change and share one merge commit. | Major assessed structures and initial optimisation/routing code were present; follow-up fixes show issues were not treated as correct on first implementation. |
| 2026-08-11 | Greedy optimisation, DynamicArray-based ascending sort alignment, SQLite/request-schema integration and the missing `CustomGraph` source were integrated. The request migration utility and strict database loader were added in this period. | Merged PRs #15-#18, including database PR #17. | Greedy, graph and database components became available for later application integration; historical CI records for these PRs were not all green. |
| 2026-08-13 | Search and graph coverage expanded with `BinarySearchTree`, DynamicArray-based custom search, and Prim/Kruskal MST implementations using the existing graph and disjoint-set work. | Merged PRs #20 and #21. | Search indexing and minimum-spanning-tree operations were added with tests. |
| 2026-08-13 | A concentrated integration/correctness-hardening sequence aligned sort pivot/performance contracts, added explicit circular-queue capacity, aligned SQLite schema/import/loading, fixed empty linked-list removal, removed the DP capacity cap and added reconstruction, strengthened Greedy validation/non-mutation, separated optimisation counterexample tests, and hardened Dijkstra validation and reconstructed paths. | Merged PRs #22-#30; database alignment in PR #24; DP/Greedy evidence in PRs #26-#29; Dijkstra hardening in PR #30. | Contracts and test isolation became more defensible; several defects and hidden assumptions in earlier work were explicitly corrected. |
| 2026-08-13 | BFS and iterative DFS were first integrated with validation and traversal-order tests, but the implementation deliberately enforced a six-hop limit. | Merged PR #31, `feat(graph): implement bounded BFS and DFS traversals`; green GitHub Actions check. | Initial traversal implementation completed with bounded semantics that was later found inconsistent with full reachability. |
| 2026-08-14 | The initial empirical efficiency lab added `PerformanceRunner`, raw benchmark results, search/sort/graph summaries and SVG plots. It used three trials but reported medians and did not yet contain every reviewer-required experiment. | Merged PR #32, `feat(experiments): add empirical efficiency lab`; green GitHub Actions check. | Initial empirical evidence established; later reviewer-aligned completion was still required. |
| 2026-08-18 | The Console UI was routed through application services and repository/experiment ports. Database loading, scheduling, search/sort, graph, optimisation, tests and experiment actions were exposed, with scripted UI tests and full 300-request queue sizing. | Merged PR #19, `feat(group-c): add service-backed Console UI`; green GitHub Actions check. | The documented application boundary became concrete, while `App.java` startup remained a separate integration task. |
| 2026-08-19 | The placeholder application entry point was replaced by a composition root that constructs the SQLite repository, experiment gateway, application service and Console UI, initialises data and launches the real menu. | Merged PR #34, `fix(app): wire integrated Console UI startup`; `App.java` and `AppTest.java`; green GitHub Actions check. | The integrated application became runnable through `App.main()`. |
| 2026-08-19 | Trace/proof evidence was corrected into reviewable Markdown and submission-ready Word files using canonical `REQ-*`, `LOC*` and road data. The work aligned Prim and Binary Search descriptions with the real implementations and retained six traces, three proof sketches and counterexamples. | Merged PR #33, `Correct team-specific trace tables, proofs, and counterexamples`; `trace-tables.md` and `trace-tables.docx`; green GitHub Actions check. | Team- and dataset-specific correctness evidence was merged. |
| 2026-08-19 | The empirical study replaced median reporting with arithmetic means over three retained trials; added hash-load-factor, BST/red-black-tree, custom priority dispatch, Dijkstra and MST studies; used graph scales 50/100/200/500; and added raw/summary CSVs, eight SVGs and theory-versus-observed interpretation. A final commit corrected Kruskal's analysis to reflect its insertion-sort-based quadratic edge ordering. | Merged PR #35, `Complete empirical performance study`; commits `410b07a` and `30be173`; green GitHub Actions check. | The earlier empirical lab was corrected and completed against the identified performance-study gaps. |
| 2026-08-20 | The missing project-owned `CustomSet<T>` was added on top of `CustomHashTable`, with collision/null/duplicate tests and dataset-grounded service-category membership/lookup evidence. | Merged PR #39, `Add project-owned custom set`; green GitHub Actions check. PR #37 was closed without merge and superseded. | Reviewer finding #5's implementation/evidence change was merged through the final correctly named PR. |
| 2026-08-20 | The six-hop BFS/DFS cutoff was removed, DFS storage was made safe beyond the audit stack's 121-entry retention limit, and regression tests covered deep and fully reachable paths. | Merged PR #38, `Fix full graph reachability traversal`; green GitHub Actions checks. PR #36 was closed without merge and superseded. | BFS and DFS now traverse the full reachable component rather than stopping after six hops. |
| 2026-08-21 | Submission-evidence preparation began with a concise, repository-verified dataset-construction/localisation note documenting UGMC grounding, simulated data, reproducibility, privacy and provenance limits. | Commit `388e06e`; open draft PR #40; `docs/evidence/dataset-construction.md`; green GitHub Actions check. | Submission packaging is in progress. This event does not establish that reviewer finding #6 or the final submission is complete. |

## Development approach

The repository shows work developed on focused branches and integrated through
pull requests. Maven tests and GitHub Actions were used throughout, but the
history includes both failing early checks and later green verification; this
log therefore does not claim perfect process compliance at every point.
Follow-up PRs corrected defects, contract mismatches and incomplete evidence
instead of rewriting the earlier events as if every feature were correct on
first implementation. Production/integration work and evidence documents were
also added in separately scoped PRs. Branch naming was not perfectly uniform,
and the superseded #36/#37 branches were replaced before the final #38/#39
merges.

## Evidence and limitations

- Dates represent verifiable Git commit dates or GitHub PR merge dates; no
  times of day are inferred or reproduced in the chronology.
- Closed, unmerged PRs are not treated as completed milestones. PRs #36 and
  #37 are mentioned only to explain their verified replacement by #38 and #39;
  other abandoned PRs are omitted from the milestone table.
- The log does not reconstruct undocumented verbal discussions, reasons,
  meetings, hours worked or attendance.
- Git history does not provide meeting sign-in/sign-out records. This log is
  not a substitute for the attendance evidence separately required by the
  project brief.
- This is not an individual contribution statement or oral-defence allocation.
  Personal ownership is not assigned from Git authorship, branch names or
  group prefixes alone.
- Test totals are included only where directly stated by authoritative PR
  evidence; increasing counts are not presented as proof of quality by
  themselves.
- This chronology records repository-visible development only and does not
  establish completion of the final report or other submission artifacts.
