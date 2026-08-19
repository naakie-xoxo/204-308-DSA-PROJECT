# UGMC Smart Service Operations Optimizer

## Team-Specific Trace Tables, Proof Sketches, and Counterexamples

**Reviewable source:** `docs/evidence/trace-tables.md`

**Submission-ready Word version:** `docs/evidence/trace-tables.docx`
**Evidence basis:** current `main` implementations and canonical CSV records in `data/`

This evidence uses the University of Ghana Medical Centre (UGMC) dataset rather than a synthetic hospital graph. Request identifiers, urgencies, weights, values, locations, and road travel times below come directly from the repository CSV files. The project brief requires team-specific outputs and at least three index-derived algorithm parameters, but it does not prescribe a new formula for every trace. Consequently, this document uses the formulas and constants already defined by the repository and does not invent additional index rules.

## Evidence Parameters and Sources

| Parameter | Repository derivation | Value | Evidence impact |
| --- | --- | ---: | --- |
| Road random seed | `RoadGenerator.RANDOM_SEED` from index `22040372` | `22040372` | Makes the 100 canonical roads reproducible. |
| Road speed modifier | `RoadGenerator.SPEED_MODIFIER`, documented in source as derived from index `22121287` | `7` | Converts geometric road distance to canonical travel time. |
| Road congestion penalty | `RoadGenerator.CONGESTION_PENALTY`, documented in source as derived from index `22013390` | `2` | Bounds the generated road-condition increment. |
| Insertion Sort shift weight | `22302749 % 1000` | `749` | Each actual shift contributes 749 to the reported weighted shift cost. |
| Merge Sort subarray cutoff | `(22389307 % 10) + 5` | `12` | A 13-element trace forces one merge split; both halves then use the implementation's insertion fallback. |

The source comments retain the exact road constants and their member indexes but do not retain formulas for the speed modifier or congestion penalty. This document reports what the repository proves and does not fabricate missing derivation formulas.

## 1. Binary Search Trace

### Fixture and implementation contract

The searchable keys are the numeric suffixes of the canonical records `REQ-001` through `REQ-015`:

`[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15]`

Target: `REQ-012`, represented by integer key `12`. The records exist in `data/service_requests.csv`; `REQ-012` is the Lab Sample Transport request from `LOC002` to `LOC012`.

`CustomSearch.binarySearch` first performs its implemented ascending-order validation. The fixture passes that validation, after which the binary-search loop runs as follows.

| Step | low | high | mid | key at mid | Decision |
| ---: | ---: | ---: | ---: | ---: | --- |
| 1 | 0 | 14 | 7 | 8 (`REQ-008`) | `8 < 12`; set `low = 8`. |
| 2 | 8 | 14 | 11 | 12 (`REQ-012`) | Match; return index `11`. |

**Result:** `REQ-012` is found at zero-based index `11` after two search-loop comparisons. For this public method, total worst-case time is O(n) for validation plus O(log n) for lookup; `binarySearchPresorted` is the O(log n) loop used only after its sorted-input precondition has been established.

## 2. Insertion Sort Trace

### Fixture

The input is the urgency sequence from the first eight canonical service requests.

| Request | Urgency | Request | Urgency |
| --- | ---: | --- | ---: |
| REQ-001 | 2 | REQ-005 | 1 |
| REQ-002 | 1 | REQ-006 | 3 |
| REQ-003 | 2 | REQ-007 | 4 |
| REQ-004 | 3 | REQ-008 | 3 |

Input: `[2, 1, 2, 3, 1, 3, 4, 3]`

| Outer `i` | Key | Array before insertion | Values shifted right | Array after insertion |
| ---: | ---: | --- | --- | --- |
| 1 | 1 | `[2, 1, 2, 3, 1, 3, 4, 3]` | `2` | `[1, 2, 2, 3, 1, 3, 4, 3]` |
| 2 | 2 | `[1, 2, 2, 3, 1, 3, 4, 3]` | None | `[1, 2, 2, 3, 1, 3, 4, 3]` |
| 3 | 3 | `[1, 2, 2, 3, 1, 3, 4, 3]` | None | `[1, 2, 2, 3, 1, 3, 4, 3]` |
| 4 | 1 | `[1, 2, 2, 3, 1, 3, 4, 3]` | `3, 2, 2` | `[1, 1, 2, 2, 3, 3, 4, 3]` |
| 5 | 3 | `[1, 1, 2, 2, 3, 3, 4, 3]` | None | `[1, 1, 2, 2, 3, 3, 4, 3]` |
| 6 | 4 | `[1, 1, 2, 2, 3, 3, 4, 3]` | None | `[1, 1, 2, 2, 3, 3, 4, 3]` |
| 7 | 3 | `[1, 1, 2, 2, 3, 3, 4, 3]` | `4` | `[1, 1, 2, 2, 3, 3, 3, 4]` |

**Result:** sorted urgencies are `[1, 1, 2, 2, 3, 3, 3, 4]`. The implementation reports 11 comparisons and 5 shifts. With the index-derived shift weight `749`, the weighted shift cost is `5 x 749 = 3745`. Because the loop shifts only when `arr[j] > key`, equal urgency values retain their relative order.

## 3. Merge Sort Trace

### Fixture and index-derived cutoff

The input is the urgency sequence from canonical records `REQ-001` through `REQ-013`:

| Request | Urgency | Request | Urgency | Request | Urgency |
| --- | ---: | --- | ---: | --- | ---: |
| REQ-001 | 2 | REQ-006 | 3 | REQ-011 | 1 |
| REQ-002 | 1 | REQ-007 | 4 | REQ-012 | 2 |
| REQ-003 | 2 | REQ-008 | 3 | REQ-013 | 1 |
| REQ-004 | 3 | REQ-009 | 4 | - | - |
| REQ-005 | 1 | REQ-010 | 5 | - | - |

`MergeSort.SUBARRAY_CUTOFF = (22389307 % 10) + 5 = 12`. The 13-element input therefore follows the actual hybrid implementation: split once at `mid = 6`, insertion-sort each half because both sizes are at most 12, and merge the two sorted halves.

### Split and insertion-fallback states

| Side | Original half | Important fallback states | Sorted half |
| --- | --- | --- | --- |
| Left, indices 0-6 | `[2, 1, 2, 3, 1, 3, 4]` | Insert second `1` at pass 4: `[1, 1, 2, 2, 3, 3, 4]` | `[1, 1, 2, 2, 3, 3, 4]` |
| Right, indices 7-12 | `[3, 4, 5, 1, 2, 1]` | Passes 3-5: `[1, 3, 4, 5, 2, 1]` -> `[1, 2, 3, 4, 5, 1]` -> `[1, 1, 2, 3, 4, 5]` | `[1, 1, 2, 3, 4, 5]` |

### Merge state

The implementation chooses the left value on equality because its comparison is `leftArray[i] <= rightArray[j]`.

| Step | Left front | Right front | Chosen side/value | Merged prefix |
| ---: | ---: | ---: | --- | --- |
| 1 | 1 | 1 | Left 1 | `[1]` |
| 2 | 1 | 1 | Left 1 | `[1, 1]` |
| 3 | 2 | 1 | Right 1 | `[1, 1, 1]` |
| 4 | 2 | 1 | Right 1 | `[1, 1, 1, 1]` |
| 5 | 2 | 2 | Left 2 | `[1, 1, 1, 1, 2]` |
| 6 | 2 | 2 | Left 2 | `[1, 1, 1, 1, 2, 2]` |
| 7 | 3 | 2 | Right 2 | `[1, 1, 1, 1, 2, 2, 2]` |
| 8 | 3 | 3 | Left 3 | `[1, 1, 1, 1, 2, 2, 2, 3]` |
| 9 | 3 | 3 | Left 3 | `[1, 1, 1, 1, 2, 2, 2, 3, 3]` |
| 10 | 4 | 3 | Right 3 | `[1, 1, 1, 1, 2, 2, 2, 3, 3, 3]` |
| 11 | 4 | 4 | Left 4 | `[1, 1, 1, 1, 2, 2, 2, 3, 3, 3, 4]` |
| Remainder | - | 4, 5 | Append right remainder | `[1, 1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 5]` |

**Result:** `[1, 1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 5]`.

## 4. Dijkstra Trace

### Dataset-derived route request

The fixture is canonical record `REQ-012`: Lab Sample Transport from `LOC002` (UGMC Main Reception) to `LOC012` (CT Scan Room). The graph contains all 50 locations and all 100 undirected roads from `data/roads.csv`; edge weights are the `travelTime` field, exactly as loaded by `DatabaseLoader`.

`PathFinder.dijkstraWithRoute` uses the graph adjacency list and `CustomPriorityQueue`. The first extraction of a node finalizes its shortest distance; stale duplicate queue entries are skipped.

| Settle step | Node | Final distance | Successful relaxations (`old -> new via node`) |
| ---: | --- | ---: | --- |
| 1 | LOC002 | 0 | LOC044 `INF -> 2`; LOC043 `INF -> 10`; LOC039 `INF -> 13`; LOC010 `INF -> 9`; LOC042 `INF -> 21`; LOC048 `INF -> 12`; LOC020 `INF -> 15`; LOC003 `INF -> 2`; LOC001 `INF -> 7` |
| 2 | LOC044 | 2 | LOC031 `INF -> 18`; LOC035 `INF -> 8`; LOC045 `INF -> 3` |
| 3 | LOC003 | 2 | LOC013 `INF -> 9`; LOC004 `INF -> 3` |
| 4 | LOC045 | 3 | LOC046 `INF -> 4` |
| 5 | LOC004 | 3 | LOC022 `INF -> 20`; LOC012 `INF -> 11`; LOC005 `INF -> 6` |
| 6 | LOC046 | 4 | LOC021 `INF -> 22`; LOC030 `INF -> 18`; LOC047 `INF -> 5` |
| 7 | LOC047 | 5 | None |
| 8 | LOC005 | 6 | LOC006 `INF -> 7` |
| 9 | LOC001 | 7 | LOC026 `INF -> 25`; LOC050 `INF -> 45` |
| 10 | LOC006 | 7 | LOC050 `45 -> 34`; LOC023 `INF -> 22`; LOC007 `INF -> 8` |
| 11 | LOC035 | 8 | LOC036 `INF -> 9`; LOC034 `INF -> 9` |
| 12 | LOC007 | 8 | LOC008 `INF -> 19` |
| 13 | LOC010 | 9 | LOC022 `20 -> 19`; LOC011 `INF -> 10`; LOC009 `INF -> 25` |
| 14 | LOC036 | 9 | LOC033 `INF -> 16`; LOC037 `INF -> 16` |
| 15 | LOC034 | 9 | LOC018 `INF -> 30` |
| 16 | LOC013 | 9 | LOC021 `22 -> 19`; LOC033 `16 -> 14`; LOC014 `INF -> 11` |
| 17 | LOC043 | 10 | None |
| 18 | LOC011 | 10 | None |
| 19 | LOC012 | 11 | Target settled; terminate and reconstruct path. |

Predecessors on the final route are `pred[LOC003] = LOC002`, `pred[LOC004] = LOC003`, and `pred[LOC012] = LOC004`.

| Final route edge | Canonical travel time |
| --- | ---: |
| LOC002 - LOC003 | 2 |
| LOC003 - LOC004 | 1 |
| LOC004 - LOC012 | 8 |
| **Total** | **11** |

**Result:** `LOC002 -> LOC003 -> LOC004 -> LOC012`, total travel time `11`.

## 5. Prim Minimum-Spanning-Tree Trace

### Actual implementation and dataset-derived graph

This trace uses the induced graph on six canonical locations involved in the `REQ-012` corridor and its nearby diagnostic links:

`LOC002` (Main Reception), `LOC003` (OPD), `LOC004` (Main Pharmacy), `LOC010` (Imaging & Radiology), `LOC012` (CT Scan Room), and `LOC013` (X-Ray & Ultrasound).

Every edge below is an actual row from `data/roads.csv`, using canonical `travelTime` as its weight.

| Road | Weight |
| --- | ---: |
| LOC002 - LOC003 | 2 |
| LOC003 - LOC004 | 1 |
| LOC012 - LOC013 | 2 |
| LOC003 - LOC013 | 7 |
| LOC002 - LOC010 | 9 |
| LOC004 - LOC012 | 8 |

`PathFinder.primMST` does **not** use `MinHeap` or `CustomPriorityQueue`. It reads weights with `getMatrixWeight`, stores the cheapest known connection in `minEdge[]`, records membership in `inMST[]`, stores the selected predecessor in `parent[]`, and uses an O(V) linear scan during each of V iterations to choose the unvisited vertex with minimum `minEdge`.

Node order for this trace matches the insertion order shown above; `LOC002` is index 0 and therefore the start vertex.

| Step | Vertex added | Selected edge | Cumulative cost | Important `minEdge` / `parent` updates |
| ---: | --- | --- | ---: | --- |
| 1 | LOC002 | Start (`minEdge = 0`) | 0 | LOC003 `INF -> 2`, parent LOC002; LOC010 `INF -> 9`, parent LOC002 |
| 2 | LOC003 | LOC002 - LOC003 (2) | 2 | LOC004 `INF -> 1`, parent LOC003; LOC013 `INF -> 7`, parent LOC003 |
| 3 | LOC004 | LOC003 - LOC004 (1) | 3 | LOC012 `INF -> 8`, parent LOC004 |
| 4 | LOC013 | LOC003 - LOC013 (7) | 10 | LOC012 improves `8 -> 2`, parent changes LOC004 -> LOC013 |
| 5 | LOC012 | LOC013 - LOC012 (2) | 12 | No cheaper connection found. |
| 6 | LOC010 | LOC002 - LOC010 (9) | 21 | All six vertices are now in the tree. |

**Selected MST edges:** LOC002-LOC003 (2), LOC003-LOC004 (1), LOC003-LOC013 (7), LOC013-LOC012 (2), LOC002-LOC010 (9).
**Total MST cost:** `2 + 1 + 7 + 2 + 9 = 21`.

The implementation runs in O(V²) time: V iterations, each containing one O(V) minimum scan and one O(V) adjacency-matrix update scan. Its algorithm-state space is O(V) for `inMST[]`, `minEdge[]`, and `parent[]`, plus O(V) for the returned edge list.

## 6. Dynamic Programming - 0/1 Knapsack Trace

### Dataset-derived requests

The five indivisible items are canonical requests `REQ-001` through `REQ-005`. Weight and value are read directly from `data/service_requests.csv`.

| Request | Weight | Value |
| --- | ---: | ---: |
| REQ-001 | 42 | 89 |
| REQ-002 | 22 | 130 |
| REQ-003 | 37 | 140 |
| REQ-004 | 36 | 121 |
| REQ-005 | 46 | 66 |

Trace capacity is `100` resource units. The brief permits a caller-supplied budget/capacity and does not define an index formula for it; `DPOptimizer.solveKnapsack` likewise uses the exact capacity supplied by the caller and has no hidden student-index cap.

The implementation computes every capacity from 0 through 100. The table below shows the capacities where the important choices become visible while preserving the exact values from the full table.

| Items available | 0 | 22 | 37 | 59 | 73 | 95 | 100 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| None | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| + REQ-001 | 0 | 0 | 0 | 89 | 89 | 89 | 89 |
| + REQ-002 | 0 | 130 | 130 | 130 | 219 | 219 | 219 |
| + REQ-003 | 0 | 130 | 140 | 270 | 270 | 270 | 270 |
| + REQ-004 | 0 | 130 | 140 | 270 | 270 | 391 | 391 |
| + REQ-005 | 0 | 130 | 140 | 270 | 270 | 391 | 391 |

### Reconstruction from `dp[5][100] = 391`

| Backtrack step | Comparison | Decision | Remaining capacity |
| ---: | --- | --- | ---: |
| 1 | `dp[5][100] = dp[4][100] = 391` | Exclude REQ-005. | 100 |
| 2 | `dp[4][100] != dp[3][100]` | Include REQ-004 (weight 36). | 64 |
| 3 | `dp[3][64] != dp[2][64]` | Include REQ-003 (weight 37). | 27 |
| 4 | `dp[2][27] != dp[1][27]` | Include REQ-002 (weight 22). | 5 |
| 5 | `dp[1][5] = dp[0][5] = 0` | Exclude REQ-001. | 5 |

**Result:** select `REQ-002`, `REQ-003`, and `REQ-004`; total weight `22 + 37 + 36 = 95`; optimal total value `130 + 140 + 121 = 391`.

## Proof Sketches

### Proof 1 - Insertion Sort Loop Invariant

**Invariant:** At the start of outer-loop iteration `i`, the prefix `arr[0..i-1]` is sorted in nondecreasing order and is a permutation of the same original prefix.

**Initialization:** Before `i = 1`, the one-element prefix `arr[0..0]` is sorted and contains its original element.

**Maintenance:** The implementation stores `key = arr[i]`, shifts each strictly larger prefix element one position right, and writes `key` into the vacated position. The shifts neither add nor lose an element, and the stopping condition places `key` after every value less than or equal to it and before every larger value. Thus `arr[0..i]` is sorted and is a permutation of its original contents. The strict `>` comparison also preserves the order of equal urgencies.

**Termination:** When `i = n`, the invariant applies to `arr[0..n-1]`; therefore the entire array is sorted and contains exactly the original elements. The index-derived shift accounting records work but does not alter the ordering operations.

### Proof 2 - Merge Sort Induction/Recursion

**Claim:** The repository's hybrid Merge Sort correctly sorts any `DynamicArray<Integer>` of length `n`.

**Base case:** For `n <= 12`, the implementation invokes its local insertion fallback. By the loop-invariant argument above, that fallback sorts the subarray correctly.

**Induction hypothesis:** Assume every subarray of length less than `n` is sorted correctly by the hybrid procedure.

**Inductive step:** For `n > 12`, the implementation splits at `left + (right-left)/2`, producing two shorter subarrays. By the hypothesis, recursive calls correctly sort both halves (eventually using the insertion fallback). During merge, the smaller current front value is copied next; equality chooses the left value. This preserves nondecreasing order, stability, and includes each input element exactly once. Therefore the merged length-`n` result is sorted.

**Conclusion:** By induction, the implemented hybrid Merge Sort is correct for every input length.

### Proof 3 - Dynamic-Programming Knapsack Correctness

Define `dp[i][w]` as the maximum value obtainable using the first `i` requests with capacity `w`.

**Correctness idea:** An optimal 0/1 solution either excludes request `i`, giving `dp[i-1][w]`, or includes it (only if its weight fits), giving `value[i] + dp[i-1][w-weight[i]]`. These are the only possibilities because each request is indivisible and may be chosen at most once. If the remaining subproblem in the include case were not optimal, replacing it with a better subsolution would improve the original solution, contradicting optimality.

The implementation therefore assigns the maximum of the include and exclude values. Its backtracking checks whether `table[i][remainingCapacity]` differs from the row above; a difference proves that item `i` was included in the reconstructed optimum. In the team-data trace, this reconstructs `REQ-002`, `REQ-003`, and `REQ-004` with value 391.

## Counterexamples

### Counterexample 1 - Ratio Greedy Fails for 0/1 Knapsack

The existing counterexample is numerically correct and matches `GreedyOptimizer.greedyKnapsack`.

| Item | Weight | Value | Value/weight ratio |
| ---: | ---: | ---: | ---: |
| 1 | 10 | 60 | 6.0 |
| 2 | 20 | 100 | 5.0 |
| 3 | 30 | 120 | 4.0 |

Capacity is 50. Ratio Greedy chooses item 1, leaving 40; then item 2, leaving 20; item 3 no longer fits. Greedy obtains value `60 + 100 = 160` at weight 30. Dynamic Programming selects items 2 and 3, exactly fills weight `20 + 30 = 50`, and obtains value `100 + 120 = 220`.

Because `220 > 160`, the locally best ratio choice can prevent the globally best 0/1 combination. This proves that descending value/weight ratio does not guarantee an optimal 0/1 Knapsack solution.

### Counterexample 2 - Binary Search Sorted-Input Precondition

### Actual project behavior

For unsorted input `[5, 1, 9, 3, 7]`, `CustomSearch.binarySearch` detects the first inversion (`5 > 1`) during its implemented precondition scan and throws `IllegalStateException("Array must be sorted prior to Binary Search.")`. It does **not** silently report that target `3` is absent.

`binarySearchPresorted` is separately documented for callers that have already established sortedness; passing unsorted input to it violates that method's precondition.

### Conceptual no-check counterexample

If an ordinary binary-search loop were executed without the project's validation, the same unsorted input could discard the region containing the target:

| Step | low | high | mid | value | Hypothetical no-check action |
| ---: | ---: | ---: | ---: | ---: | --- |
| 1 | 0 | 4 | 2 | 9 | `9 > 3`; discard indices 2-4 and set `high = 1`. |
| 2 | 0 | 1 | 0 | 5 | `5 > 3`; set `high = -1`. |
| End | - | - | - | - | Loop would return not found, although `3` is at index 3. |

This conceptual failure explains why sortedness is a necessary Binary Search precondition and why the public project method validates it explicitly.

## Verification Matrix

| Required evidence | Repository implementation checked | Dataset/source checked | Verified result |
| --- | --- | --- | --- |
| Binary Search | `CustomSearch.binarySearch` | `REQ-001..REQ-015` | REQ-012 at index 11 |
| Insertion Sort | `InsertionSort.sort` | Urgencies from `REQ-001..REQ-008` | 11 comparisons, 5 shifts, cost 3745 |
| Merge Sort | `MergeSort.sort` | Urgencies from `REQ-001..REQ-013` | Sorted sequence and cutoff 12 |
| Dijkstra | `PathFinder.dijkstraWithRoute` | REQ-012 and full 50-location/100-road graph | Route LOC002-LOC003-LOC004-LOC012, cost 11 |
| Prim | `PathFinder.primMST` | Six actual LOC nodes and six actual road rows | Five edges, total cost 21, O(V²) matrix implementation |
| Dynamic Programming | `DPOptimizer.solveKnapsack` | Weights/values from `REQ-001..REQ-005` | REQ-002/003/004, weight 95, value 391 |
| Greedy failure | `GreedyOptimizer.greedyKnapsack` | Required counterexample fixture | Greedy 160 versus DP 220 |
| Binary precondition | `CustomSearch.binarySearch` | Required invalid-precondition fixture | Actual method throws `IllegalStateException` |

All six required trace areas, all three proof sketches, and both required counterexamples are present. This evidence documents the current code; no algorithm implementation was changed to fit a trace.
