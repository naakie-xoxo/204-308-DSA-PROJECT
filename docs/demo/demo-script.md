# Demonstration Video Recording Script

## Purpose

This is a recording guide for the required 5–8 minute project demonstration. It plans a concise walkthrough of the integrated application, database load, core algorithms, automated tests, and committed performance graphs; it is not the demonstration video itself.

## Before Recording

- Work from a clean, current `main` branch and confirm `git status --short` produces no output.
- Run `mvn --batch-mode --no-transfer-progress test` and confirm it succeeds.
- Ensure the committed CSV seed data and `schema.sql` are available; `hospital_system.db` is generated locally when the application initializes.
- Ensure the committed performance CSV and SVG evidence is present under `results/`.
- Close unnecessary terminals, notifications, and windows.
- Do not regenerate performance experiments during the normal recording unless regeneration is intentional.
- Start the application with:

  ```powershell
  mvn --batch-mode --no-transfer-progress compile exec:java "-Dexec.mainClass=ug.edu.ugmc.optimizer.App"
  ```

## Recording Sequence

Target approximately 6–7 minutes, leaving margin below the eight-minute limit.

| Time | Action | What to explain |
| --- | --- | --- |
| 0:00–0:30 | Show the project title and integrated Console UI. | Introduce the UGMC-grounded smart service operations simulation. CSV files provide seed data, SQLite provides persistence, and the application uses project-owned data structures and algorithms. Keep the architecture explanation brief. |
| 0:30–1:05 | Select **5. Initialize/reload database**, show the program's actual loaded request and location counts, then press Enter to return. | Explain the flow: CSV seed data → SQLite → application services and custom structures. Read the counts displayed by the live application rather than stating assumed values. |
| 1:05–1:35 | Select **1. Request ID lookup**, enter `REQ-001`, show the result, then press `B`. | Explain that the request comes from the modelled hospital dataset and is accessed through the application service/custom-structure path. The patient label is synthetic and does not represent a real patient. |
| 1:35–2:10 | Select **3. Run graph traversal (BFS/DFS)**, start from `LOC002`, and run BFS. If time permits, press Enter and run DFS before pressing `B`. | BFS and DFS explore locations in the reachable hospital-network component. Enter repeats the feature and `B` returns to the main menu. |
| 2:10–2:50 | Select **9. Find shortest path (Dijkstra)** with start `LOC002` and target `LOC012`. Show the route and total distance, then press `B`. | Dijkstra finds the lowest-cost route through the weighted hospital graph. If the live output remains `LOC002 -> LOC003 -> LOC004 -> LOC012` with distance `11`, point it out; otherwise narrate the values actually displayed. |
| 2:50–3:30 | Select **4. Show MST edges and cost (Prim/Kruskal)** and choose either Prim or Kruskal. Show the edge list and total cost without reading every edge, then press `B`. | A minimum spanning tree connects all model locations with minimum total edge cost and no cycles. |
| 3:30–4:05 | Select **7. Search request numbers**, enter `12`, and choose `B` for Binary Search. If time permits, select **8. Sort request urgency values** and choose `M` for Merge Sort. | Binary Search operates on sorted request-number data. The project's Merge Sort implementation orders the request urgency values. Avoid a long complexity lecture. |
| 4:05–4:40 | Select **10. Compare greedy and DP allocation**, enter capacity `100`, show the greedy value, DP optimal value, selected weight, and selected request IDs, then press `B`. | A locally greedy selection can differ from the Dynamic Programming optimum. Narrate only the values shown by the live run. |
| 4:40–5:15 | Briefly show the successful Maven test result prepared before recording, or use **11. Display latest Maven test summary**. | Tests cover normal, boundary, and invalid-input behaviour across the project's structures and algorithms. Do not state a fixed test count because the suite may change. |
| 5:15–6:15 | Show representative committed graphs: `results/search_runtimes.svg`, `sort_runtimes.svg`, `hash_lookup_runtimes.svg`, `tree_runtimes.svg`, `priority_dispatch_runtimes.svg`, and `mst_runtimes.svg`. Briefly show `results/benchmark_summary.csv`. | Measurements were repeated and averaged; the graphs compare observed runtime behaviour across input sizes or configurations. Raw and summary evidence is preserved in the repository. These files were prepared before the video, not generated during it. |
| 6:15–6:40 | Return to the Console UI if needed, enter `0`, and show the clean exit. | Summarise the local hospital data model, custom DSA layer, database integration, algorithm demonstrations, automated tests, and empirical performance evidence. |

## Recording Safety Notes

- Enter repeats interactive features where supported; `B` returns from repeatable features.
- One-shot features wait for Enter before returning to the main menu.
- Blank Enter at the main menu should simply reprompt after PR #42.
- Do not select option 12 during the normal recording. The full experiment suite may take several minutes and rewrites tracked benchmark CSV/SVG outputs.
- If option 12 is intentionally run, check `git status --short` afterward for changed benchmark/result files.

## What This Document Does Not Prove

This script is preparation evidence only. It does not replace the required 5–8 minute demonstration video. The actual video must still be recorded and submitted, and a video link or repository location should be added only after that real artifact exists.
