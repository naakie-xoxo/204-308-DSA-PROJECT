# Balanced tree rotations and node splits

Visual evidence for the balanced hospital-record indices in
`ug.edu.ugmc.optimizer.datastructures.trees` (Somuah, Group A, Module 3 and Module 6).

Every tree drawn below was printed from the committed implementations, not drawn by hand. The
scenarios are reproduced by the tests in `BalancedTreeTest.java` (tests 41-60), so the shapes
here and the shapes the code produces cannot drift apart.

## Assigned parameter derivation

| Index number | Parameter | Derivation | Value |
| --- | --- | --- | --- |
| 22018389 (Somuah) | B-tree minimum degree `t` | `(22018389 % 4) + 3` | 4 |

The degree fixes every other structural constant of the B-tree:

| Constant | Formula | Value |
| --- | --- | --- |
| Maximum keys per node | `2t - 1` | 7 |
| Minimum keys per non-root node | `t - 1` | 3 |
| Maximum children per internal node | `2t` | 8 |

`BTree.MIN_DEGREE` is computed from the index number in source rather than written as a
literal `4`, and test 51 asserts the derivation holds.

## Why both structures exist

Precious's hash table already gives O(1) lookup of a single patient ID. It cannot answer
"list every record between two IDs" or "what is the earliest admission" without scanning
everything. Both trees here keep records in sorted order, so those queries cost O(log n)
instead of O(n). The red-black tree is the in-memory index; the B-tree is the shape you would
use once records outgrow memory, because its height is the number of disk pages a lookup
touches.

---

## Part 1 — Red-black tree rotations

A rotation changes the shape of the tree without changing the left-to-right order of the
keys. That is the whole trick: the in-order traversal is identical before and after, so the
tree is still a valid search tree, but it is shorter on one side.

```text
        RIGHT rotation about Q                LEFT rotation about P
        ---------------------->               <----------------------

              Q                                       P
             / \                                      / \
            P   C          becomes / undoes          A   Q
           / \                                          / \
          A   B                                        B   C

    in-order: A P B Q C                        in-order: A P B Q C
```

Subtree `B` is the one that changes parents. Everything else just swaps roles. Both directions
are implemented explicitly in `RedBlackTree.rotateLeft` and `RedBlackTree.rotateRight`, each
O(1) because only a fixed number of pointers move.

Colours are written `(R)` for red and `(B)` for black. Null leaves count as black and are not
drawn.

### Case 1 — red uncle, recolour only, no rotation

Inserting 5 into a tree where the new node's uncle is red. No pointers move at all; three
nodes just change colour, and the violation is pushed two levels up the tree.

```text
BEFORE  insert(20), insert(10), insert(30)          AFTER  insert(5)

          20 (B)                                        20 (B)
         /      \                                      /      \
     10 (R)      30 (R)                            10 (B)      30 (B)
                                                   /
     black height 1                            5 (R)
     rotations: 0                              black height 2
                                               rotations: 0
```

5 is attached red below the red node 10, which breaks "no red node has a red child". The
uncle 30 is also red, so recolouring both children black and the grandparent red repairs it.
Black height rises from 1 to 2 uniformly, so no path is favoured over another.

### Case 3 — black uncle, straight line, single rotation

The classic case. Inserting keys in ascending order is exactly what turns an unbalanced
binary search tree into a linked list, so this is the case that earns the balance guarantee.

```text
BEFORE  insert(10), insert(20)              AFTER  insert(30)

     10 (B)                                       20 (B)
          \                                      /      \
           20 (R)                            10 (R)      30 (R)

     height 1                                height 1
     rotations: 0                            rotations: 1 left
```

30, 20 and 10 form a straight line to the right. One **left rotation about the grandparent
10**, plus recolouring, lifts 20 into the root. Without this the tree would already be a
three-node chain of height 2.

### Case 2 into case 3 — black uncle, zig-zag, two rotations

When the new node is the *inner* grandchild, one rotation is not enough. The first rotation
straightens the zig-zag into a line, which reduces the problem to case 3 above.

```text
BEFORE  insert(30), insert(10)      STEP 1  left rotation        AFTER  insert(20)
                                            about 10                    right rotation
                                                                        about 30

     30 (B)                              30 (B)                      20 (B)
     /                                   /                          /      \
 10 (R)                              20 (R)                     10 (R)      30 (R)
                                     /
                                 10 (R)
     inserting 20 goes            still violating              height 1
     to the right of 10           but now a line               rotations: 1 left, 1 right
```

Both scenarios end in the same balanced shape from different arrival orders, which is the
property the index needs: ambulance and admission records arrive in unpredictable order, and
the index has to stay balanced regardless.

### Measured effect on sorted input

Ascending insertion is the worst case for an unbalanced BST, which would degenerate to height
n-1. Test 43 inserts 1023 ascending keys and asserts the red-black height stays inside the
theoretical `2*log2(n+1)` bound.

| Records inserted ascending | Unbalanced BST height | Red-black height | Bound `2*log2(n+1)` |
| --- | --- | --- | --- |
| 100 | 99 | 10 | 13 |
| 1,000 | 999 | 16 | 19 |
| 10,000 | 9,999 | 23 | 26 |
| 50,000 | 49,999 | 28 | 31 |

---

## Part 2 — B-tree node splitting

The B-tree never rotates. It rebalances by **splitting a full node and promoting its median
key into the parent**. Splitting happens on the way *down*: before descending into a child
that already holds 7 keys, that child is split first. A node is therefore never allowed to
overflow, insertion needs only one downward pass, and it never has to back up.

### A node fills to capacity without splitting

With `t = 4` a node legally holds 7 keys. Seven records fit in the root with no structure at
all, and the tree still has height 0.

```text
AFTER inserting 10, 20, 30, 40, 50, 60, 70

    [ 10 | 20 | 30 | 40 | 50 | 60 | 70 ]     (leaf, and also the root)

    height 0, splits 0, 7 of 7 slots used
```

### The eighth key forces the split

```text
BEFORE  root is full                     AFTER  insert(80)

 [10|20|30|40|50|60|70]                          [ 40 ]
                                                /       \
                                    [10|20|30]           [50|60|70|80]

 height 0, splits 0                  height 1, splits 1
```

Reading the split precisely:

- Key 40 sits at index `t-1 = 3`, the median. It is **promoted** into a brand-new root.
- Keys 10, 20, 30 stay in the left node — exactly `t-1 = 3`, the legal minimum.
- Keys 50, 60, 70 move to a new right sibling — also exactly 3.
- Only then is 80 inserted, landing in the right sibling.

Both halves come out at the minimum legal occupancy, which is why `t-1` is defined as it is.
Because the split happened at the root, **every leaf moved down by one level at the same
time**. That is the mechanism behind invariant 5: a B-tree grows upward from the root, never
downward at one leaf, so all leaves stay at equal depth by construction. Test 53 asserts the
split count is exactly 0 at 7 keys and exactly 1 at 8.

### After 30 sequential records

```text
                        [ 4 | 8 | 12 | 16 | 20 | 24 ]
                       /    |    |    |     |    |    \
            [1|2|3] [5|6|7] [9|10|11] [13|14|15] [17|18|19] [21|22|23] [25|26|27|28|29|30]

    height 1, splits 6, size 30, all seven leaves at depth 1
```

Every leaf sits at depth 1. Six of them hold the minimum 3 keys, which is the signature of
sequential insertion — random arrival order fills nodes more evenly.

---

## Part 3 — Measured comparison

Ascending insertion, both structures built from the identical key sequence, counters read
straight from the implementations:

| Records | Red-black height | Rotations | Recolourings | B-tree height | Splits |
| --- | --- | --- | --- | --- | --- |
| 100 | 10 | 89 | 445 | 2 | 29 |
| 1,000 | 16 | 983 | 4,909 | 4 | 326 |
| 10,000 | 23 | 9,976 | 49,877 | 6 | 3,324 |
| 50,000 | 28 | 49,971 | 249,855 | 7 | 16,656 |

At the 50,000-record scale used by the Module 10 empirical lab, a lookup touches **28 nodes in
the red-black tree but only 7 in the B-tree**. Both are O(log n); the base of the logarithm
differs, and that constant is exactly why real database indices are B-trees. Held in memory
the difference barely shows, because all 28 red-black nodes are cheap pointer hops. Backed by
disk the same difference is 28 page reads against 7.

The counters above are exposed as `getTotalRotations()`, `getRecolorings()` and
`getSplitCount()` so Naakie's `PerformanceRunner` can chart them alongside Precious's hash
table collision count.

## Complexity summary

| Operation | Red-black tree | B-tree (t = 4) | Note |
| --- | --- | --- | --- |
| Insert | O(log n) | O(t · log_t n) | at most 2 rotations vs at most 1 split per level |
| Search | O(log n) | O(t · log_t n) | |
| Delete | not implemented | not implemented | insertion only, per the Sprint 2 brief |
| Minimum / maximum | O(log n) | O(log_t n) | leftmost or rightmost spine walk |
| Range query `[low, high]` | O(log n + k) | — | k = keys reported; skips whole subtrees |
| In-order traversal | O(n) | O(n) | |
| Validate invariants | O(n) | O(n) | full re-check, used by the tests |
| Space | O(n) | O(n) | B-tree nodes are fixed-size arrays, never reallocated |

Neither class imports anything from `java.util`. Nodes, arrays and the traversal logic are
written from scratch, as the brief requires for assessed structures.

## Reproducing these diagrams

```text
mvn -Dtest=BalancedTreeTest test
```

`validate()` on either tree re-checks every structural invariant from scratch in O(n) and is
called by tests 43, 50, 53, 55 and 60. It is public rather than test-only so the console
demonstration can prove balance to the examiner after a live insertion.
