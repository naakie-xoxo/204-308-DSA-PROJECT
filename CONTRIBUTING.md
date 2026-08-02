# Contributing

## Branch workflow

1. Pull the latest `main`.
2. Create a focused branch using `group-a/<topic>`, `group-b/<topic>`, or `group-c/<topic>`.
3. Implement one assignment or integration change.
4. Run `mvn test`.
5. Push the branch and open a pull request into `main`.
6. Request review from a member whose work integrates with yours.
7. Merge only after CI passes and review feedback is resolved.

Do not commit directly to `main`, share one long-lived branch between several people, or combine unrelated structures and algorithms in one pull request.

## Architecture boundaries

- UI code calls `application.services`; it does not import JDBC or concrete data-structure implementations.
- Application services depend on interfaces in `application.ports`.
- SQLite implementations live under `database.repository`.
- Assessed logic under `datastructures`, `graph`, and `algorithms` remains independent of the UI and database.
- Tests mirror the package being tested. Maron owns the central 40+ test suite; implementation owners provide expected behavior, edge cases, and fixes for failures in their code.
- Performance benchmarks live under `experiments`, not inside unit tests.

## Pull-request checklist

- [ ] The change stays inside the assigned package or clearly documents a shared-interface change.
- [ ] Public classes and non-obvious invariants have concise Javadoc.
- [ ] Normal, boundary, and invalid-input tests are included.
- [ ] Changes respect the dependency rules in `docs/architecture.md`.
- [ ] Assessed structures are implemented without prohibited built-in collection substitutes.
- [ ] Database changes update `schema.sql`, seed validation, and documentation together.
- [ ] Algorithm output is deterministic where a fixed seed is expected.
- [ ] Trace-table or performance-evidence hooks are included where applicable.
- [ ] `mvn test` passes locally.

## Shared-interface rule

Changes to models, repository ports, application-service contracts, graph contracts, database record shapes, or package-level APIs affect multiple groups. Open a short design issue or draft pull request first, then obtain review from each affected group before merging.

## Definition of done

An assigned structure or algorithm is complete only when its implementation, tests, examiner-facing demonstration path, complexity notes, and required trace/performance evidence are all present.
