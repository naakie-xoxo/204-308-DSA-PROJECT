# Custom Set Membership and Lookup Evidence

The project brief requires a custom set/map built on a hash table or BST, with
evidence of a membership and lookup use case. The hospital-domain example uses
`CustomSet<String>` to track unique service-request categories.

Operations demonstrated: `add`, `contains`, duplicate `add`, and `size`.

```text
add Ward Maintenance -> true
add Lab Sample Transport -> true
add Ward Maintenance again -> false
contains Ward Maintenance -> true
contains Medical Supply Dispatch -> false
size -> 2
```

The executable evidence is
`CustomSetTest.serviceRequestCategoriesDemonstrateMembershipAndLookup()`.

Run the focused hashing/set tests with:

```bash
mvn --batch-mode --no-transfer-progress "-Dtest=CustomSetTest,HashTableTest" test
```
