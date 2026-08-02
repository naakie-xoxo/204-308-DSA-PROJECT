# Seed data

These CSV files are the canonical inputs for local database generation.

- Do not edit `hospital_system.db` and commit the binary result.
- Preserve the existing headers and column order.
- Keep IDs unique and all location references valid.
- Treat each `roads.csv` row as one undirected edge; graph loaders should add both directions.
- `algorithm_runs.csv` currently contains mock setup data. Replace it with repeated measured runs before final submission.

After changing seed data, run `mvn test` and rebuild the local database with `DatabaseManager`.
