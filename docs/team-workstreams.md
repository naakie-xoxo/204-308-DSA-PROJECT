# Team workstreams — current 15-member assignment

This file reflects the updated team-assignment document and is the working allocation until the team approves another revision.

## Headcount clarification

- Group A has 6 assignment rows and 6 unique members.
- Group B has 6 assignment rows and 5 unique members because Naakie owns two rows.
- Group C has 5 assignment rows and 5 unique members, including Naakie as a cross-group member.
- Across all groups there are 15 unique members.

The asterisks beside Amankwah Yaw Adu, Denzel, and Papa Kwame are preserved from the source document; no meaning is assumed for them here.

## Group A — Core Data Structures and Indexing

Focus: foundational collections and search indices for patients, staff, requests, and hospital resources.

| Member | Level | Assigned structures and algorithms | UGMC application | Primary packages |
| --- | --- | --- | --- | --- |
| Precious | L300 | Hash table and custom set/map | Patient-ID and pharmacy-request lookup | `datastructures.hashing` |
| Somuah | L300 | Red-black tree and B-tree | Balanced indices for hospital records | `datastructures.trees` |
| Afia | L200 | Dynamic array and linked list with iterator | Active wards and request history | `datastructures.linear` |
| Kingsley | L200 | Stack, queue, circular queue, and deque | Audit/undo and urgent ambulance arrivals | `datastructures.queues` |
| Tawiah Kwaku | L200 | Priority queue and heap | Emergency triage dispatch | `datastructures.heap` |
| Kafui | L200 | BST, linear search, and binary search | Staff lookup and searchable indices | `datastructures.trees`, `algorithms.search` |

## Group B — Graph Routing, Optimization and Sorting

Focus: ambulance pathfinding, ward connectivity, sorting, and constrained resource allocation.

| Member | Level | Assigned structures and algorithms | UGMC application | Primary packages |
| --- | --- | --- | --- | --- |
| Naakie | L300 | Graph using adjacency list/matrix and Dijkstra | Hospital network and shortest ambulance routes | `graph`, `algorithms.graph` |
| Naakie | L300 | Dynamic programming and disjoint set | Knapsack-style resource allocation and connectivity | `algorithms.optimization`, `datastructures.disjointset` |
| Amankwah Yaw Adu* | L200 | BFS and DFS | Reachable wards from the emergency room | `algorithms.graph` |
| Denzel* | L200 | Prim and Kruskal | Minimum hospital-network connection cost | `algorithms.graph` |
| Aham | L200 | Selection sort and insertion sort | Baseline sorting for small daily datasets | `algorithms.sort` |
| Zakari | L200 | Merge sort, quicksort, and greedy algorithm | Fast triage sorting and priority assignment | `algorithms.sort`, `algorithms.optimization` |

Naakie's graph contract should be agreed before Amankwah and Denzel begin their traversal and MST branches. Denzel's Kruskal implementation should reuse Naakie's disjoint-set API, and Dijkstra/Prim should reuse Tawiah Kwaku's heap API instead of duplicating those structures.

## Group C — Architecture, QA, UI and Empirical Labs

Focus: database integration, application wiring, examiner-facing UI, automated testing, and report evidence.

| Member | Level | Assigned responsibility | Required output | Primary packages/files |
| --- | --- | --- | --- | --- |
| Julyn Anim | L300 | Database integration and system wiring | Connect SQLite repositories to Groups A and B through stable application services | `application`, `database`, `model` |
| Naakie | L300 | Empirical lab lead | Run repeated benchmarks and plot search/sort/graph results | `experiments`, `results` |
| Papa Kwame* | L200 | Console UI menu | Continuous menu with validated input and safe demonstrations | `ui.console`, `App.java` integration |
| Maron | L200 | QA and unit testing | 40+ tests covering normal, boundary, invalid, duplicate, empty, and disconnected cases | `src/test` and CI review |
| Ganyo | L200 | Trace tables and proof sketches | Six trace tables, three proof sketches, counterexamples, and report-ready evidence | `docs/evidence` |

## Integration sequence

1. Julyn defines shared models, repository ports, and application-service contracts.
2. Group A implements the independent structures and search APIs.
3. Naakie defines the graph interfaces and disjoint-set contract needed by Group B.
4. Group B implements algorithms against those stable APIs.
5. Papa Kwame builds the console menu against application services, not directly against SQLite or individual algorithms.
6. Maron grows the test suite alongside merged features and reports failures to the relevant implementation owner.
7. Naakie runs final experiments only after implementations and correctness tests stabilize.
8. Ganyo converts approved algorithm traces and correctness reasoning into final evidence.

## Branch suggestions

- `group-a/hash-table`, `group-a/linked-list`, `group-a/heap`, or similar.
- `group-b/graph-dijkstra`, `group-b/dp-disjoint-set`, `group-b/mst`, or similar.
- `group-c/database-wiring`, `group-c/console-ui`, `group-c/test-suite`, or similar.

GitHub usernames are intentionally not encoded. Add `CODEOWNERS` only after all 15 members confirm their account handles.
