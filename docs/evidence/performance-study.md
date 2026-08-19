# Empirical performance study: theory versus observation

This interpretation is tied to the measured evidence committed in `results/`
from the 19 August 2026 run. The machine metadata records Windows 11, Java
23.0.1, four available processors, and a maximum JVM heap of 2,116,026,368
bytes. Every reported value is the arithmetic mean of three measured trials;
the individual nanosecond measurements remain in `benchmark_raw.csv`.

The runner uses deterministic inputs and excludes input creation, database
loading, validation, and output generation from the timed regions. JVM
compilation, cache effects, garbage collection, operating-system scheduling,
and the small three-trial sample still cause normal local variation. For that
reason, the study evaluates broad growth trends rather than requiring every
adjacent point to increase.

## Search

Expected behavior: Linear Search is O(n), while Binary Search is O(log n) once
the input is sorted. The timed Binary Search operation does not include sorting.

Observed behavior: Linear Search rose from 13,167 ns for 100 records to
1,931,933 ns for 10,000 records. Its 50,000-record mean was lower at 1,564,400
ns, an example of run-to-run/JVM variance rather than an algorithmic speedup.
Binary Search stayed between 1,367 ns and 6,667 ns across all six scales.

Assessment: The broad trend agrees with theory. Linear Search becomes much
more expensive as the scan grows, whereas logarithmic lookup remains nearly
flat at this scale. The small absolute Binary Search timings make fixed JVM and
timer effects especially visible.

## Sorting

Expected behavior: Merge Sort and average-case Quick Sort are O(n log n).
Selection Sort is O(n^2), and Insertion Sort is O(n^2) on the deterministic
non-sorted workload used here.

Observed behavior: At 50,000 records, Merge Sort averaged 13,198,033 ns and
Quick Sort 11,933,133 ns. Selection Sort required 3,963,008,600 ns and Insertion
Sort 7,564,505,733 ns. The quadratic algorithms separate sharply from the
n-log-n algorithms as input grows. Some smaller adjacent means are not
monotonic, such as Merge Sort at 10,000 versus 5,000 records.

Assessment: The large-scale results strongly agree with the expected growth
classes. Non-monotonic small points are consistent with JVM warm-up, cache and
scheduling noise; they do not outweigh the orders-of-magnitude gap at 50,000.

## Custom hash-table load factor

The target factors 0.50, 0.75, 1.00, 1.50, and 2.00 are experimental choices;
the brief requires different table sizes but does not prescribe exact factors.

Expected behavior: With separate chaining, average insertion and successful
lookup are O(1 + alpha), where alpha is the load factor. Higher load factors
normally lengthen chains and increase collision work, although the exact count
also depends on the key hash distribution and chosen capacity.

Observed behavior: At 20,000 keys, target factor 0.50 used capacity 40,000 and
recorded 6,231 collisions, a 1,027,600 ns insertion mean, and a 510,033 ns
lookup mean. Target factor 1.50 used capacity 13,334 and recorded 12,539
collisions, with means of 1,806,433 ns and 596,867 ns. At target factor 2.00,
insertion rose to 5,497,733 ns and lookup to 608,867 ns. Collision totals are
not strictly monotonic at every factor (for example, 11,523 at factor 2.00),
because Java String hashes interact differently with each modulus/capacity.

Assessment: The high-factor insertion slowdown and larger collision totals
broadly support O(1 + alpha), but three-run nanosecond timing and capacity-specific
hash distribution prevent a perfectly monotonic curve. The experiment measures
the project `CustomHashTable`, not `HashMap`.

## Ordinary BST versus balanced tree

Expected behavior: Inserting ascending keys degenerates an ordinary BST to
height O(n), making insertion/search O(n) per operation and O(n^2) for a full
batch. The project's Red-Black Tree maintains O(log n) height.

Observed behavior: For 2,000 ascending request keys, ordinary BST batch insert
averaged 12,200,100 ns and batch search 6,450,133 ns. The Red-Black Tree means
were 312,067 ns and 271,933 ns respectively. At 100 keys the corresponding BST
means were 76,200 ns and 341,900 ns, so the structural penalty becomes much
clearer as the input grows.

Assessment: The observed divergence agrees with the expected degenerate-BST
versus balanced-tree behavior. A few Red-Black Tree points are non-monotonic
because the operations are short enough for JVM and system noise to dominate.

## Heap-backed priority dispatch

Expected behavior: Each custom-heap insert and extraction is O(log n), so
building and fully draining a queue is O(n log n). Extraction also performs the
repeated heap restoration expected by the application's priority-dispatch path.

Observed behavior: At 100 requests, batch insertion averaged 82,467 ns and
full extraction 104,433 ns. At 20,000 requests they averaged 1,352,967 ns and
5,723,167 ns. Extraction was consistently more expensive at the larger scales.

Assessment: The growth and extraction cost broadly agree with heap theory.
The 20,000-item insertion mean is slightly below the 10,000-item mean, which is
measurement noise rather than evidence of sublinear insertion. The benchmark
uses `CustomPriorityQueue`, not Java's `PriorityQueue`.

## Graph traversal and shortest path

The required synthetic scales are 50, 100, 200, and 500 vertices. Each graph
is connected and has E = 2V (100, 200, 400, and 1,000 undirected edges), so
density is controlled. Edge weights come from canonical road travel times.

Expected behavior: BFS and DFS are O(V + E). The current Dijkstra path uses the
project's custom priority queue and is expected to scale approximately with
O((V + E) log V) for this sparse graph representation.

Observed behavior: At V=50/E=100, the means were 90,667 ns for BFS, 100,233 ns
for DFS, and 117,833 ns for Dijkstra. At V=500/E=1,000 they were 811,000 ns,
1,070,900 ns, and 1,078,300 ns respectively.

Assessment: The roughly tenfold vertex/edge increase produces clear runtime
growth and broadly agrees with the expected sparse-graph trends. Intermediate
points contain normal JIT/cache noise, but the endpoints and common graph
inputs make the comparison defensible.

## Minimum spanning tree

Expected behavior: The repository's Prim implementation scans adjacency-matrix
state and linearly selects the next minimum vertex, so it is O(V^2); it does
not use a heap. Kruskal sorts edges and uses the custom `DisjointSet`, giving
approximately O(E log E) for these inputs.

Observed behavior: At V=50/E=100, Prim averaged 256,100 ns and Kruskal 171,200
ns. At V=500/E=1,000, Prim averaged 6,083,700 ns and Kruskal 3,710,500 ns. Both
algorithms returned the same MST cost at every scale: 206, 382, 778, and 1,966.

Assessment: Prim's stronger growth and Kruskal's advantage at V=500 broadly
match the expected O(V^2) versus edge-sort behavior for E=2V. The V=100 and
V=200 timings are not strictly ordered because of runtime variance, while the
matching costs provide a correctness sanity check independent of timing.

## Greedy and dynamic programming scope

The project's Greedy and Dynamic Programming implementations and their
solution-quality counterexample remain unchanged. They are not included as a
new timing category here because the performance-table requirement and the
reviewer's identified empirical gaps name search, sort, hash load factor,
BST/balanced tree, heap dispatch, graph traversal/shortest path, and MST. This
study does not infer or fabricate an additional requirement.

## Evidence map

- Complete raw trials: `results/benchmark_raw.csv`
- Complete arithmetic-mean summary: `results/benchmark_summary.csv`
- Search/sort/hash/tree/priority/graph/MST summaries: the corresponding CSVs
  under `results/`
- Visualizations generated from the averages: the SVG files under `results/`
- Reproduction metadata and deterministic-input description:
  `results/benchmark_metadata.csv` and `docs/empirical-lab.md`
