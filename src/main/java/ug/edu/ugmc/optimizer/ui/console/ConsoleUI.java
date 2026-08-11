package ug.edu.ugmc.optimizer.ui.console;

import java.util.InputMismatchException;
import java.util.Scanner;

import ug.edu.ugmc.optimizer.datastructures.hashing.CustomHashTable;
import ug.edu.ugmc.optimizer.datastructures.queues.CircularQueue;
import ug.edu.ugmc.optimizer.graph.CustomGraph;

/**
 * Examiner-facing console menu, input validation, and result formatting.
 *
 * Sprint 3 — Papa Kwame (L200), Universal Parameter 22271087.
 * Invalid-input retry limit formula: (22271087 % 5) + 3 = 5 attempts.
 *
 * NOTE on architecture: the README's dependency flow is
 *   Console UI -> Application services -> Custom structures and algorithms
 * This class currently references Julyn's structures directly (CustomHashTable,
 * CircularQueue, CustomGraph) ONLY as a placeholder so the menu compiles and is
 * demoable right now. Once the application/services/ layer exists, swap these
 * direct references for calls into that service layer instead — the UI is not
 * supposed to touch the structures or the database directly per the architecture
 * rule in the README.
 */
public class ConsoleUI {

    // (22271087 % 5) + 3
    private static final int MAX_INVALID_ATTEMPTS = (22271087 % 5) + 3;

    private final Scanner scanner;

    // Placeholder references to Julyn's loaded structures.
    // These should eventually be passed in (constructor injection) from the
    // application/service layer once it exists, rather than instantiated here.
    private final CustomHashTable<String, Object> patientLookup;
    private final CircularQueue<Object> triageQueue;
    private final CustomGraph hospitalGraph;

    public ConsoleUI(CustomHashTable<String, Object> patientLookup,
                      CircularQueue<Object> triageQueue,
                      CustomGraph hospitalGraph) {
        this.scanner = new Scanner(System.in);
        this.patientLookup = patientLookup;
        this.triageQueue = triageQueue;
        this.hospitalGraph = hospitalGraph;
    }

    public void run() {
        int invalidAttempts = 0;
        boolean running = true;

        while (running) {
            printMenu();

            try {
                int choice = scanner.nextInt();
                invalidAttempts = 0; // reset on any successfully parsed input

                switch (choice) {
                    case 1:
                        handlePatientLookup();
                        break;
                    case 2:
                        handleTriageQueueView();
                        break;
                    case 3:
                        handleGraphTraversal(); // Amankwah's BFS/DFS — not yet available
                        break;
                    case 4:
                        handleMstCalculation(); // Denzel's Prim/Kruskal — not yet available
                        break;
                    case 0:
                        running = false;
                        System.out.println("Exiting system.");
                        break;
                    default:
                        System.out.println("Invalid option. Please choose a number from the menu.");
                }
            } catch (InputMismatchException e) {
                scanner.next(); // discard the bad token so we don't loop forever
                invalidAttempts++;
                System.out.println("Invalid input. Attempt " + invalidAttempts
                        + " of " + MAX_INVALID_ATTEMPTS + ".");

                if (invalidAttempts >= MAX_INVALID_ATTEMPTS) {
                    System.out.println("Too many invalid attempts. Terminating system.");
                    running = false;
                }
            }
        }

        scanner.close();
    }

    private void printMenu() {
        System.out.println();
        System.out.println("=== UGMC Smart Service Operations Optimizer ===");
        System.out.println("1. Patient ID Lookup (CustomHashTable)");
        System.out.println("2. View Pending Triage Queue (CircularQueue)");
        System.out.println("3. Run Graph Traversal (BFS/DFS) [pending Amankwah's branch]");
        System.out.println("4. Run MST Calculation (Prim/Kruskal) [pending Denzel's branch]");
        System.out.println("0. Exit");
        System.out.print("Choose an option: ");
    }

    /**
     * Uses Julyn's CustomHashTable for O(1) patient ID lookups.
     * Signature confirmed from group-c/db-integration:
     *   V get(K key)
     */
    private void handlePatientLookup() {
        System.out.print("Enter patient ID: ");
        String patientId = scanner.next();

        Object result = patientLookup.get(patientId);
        if (result == null) {
            System.out.println("No record found for patient ID: " + patientId);
        } else {
            System.out.println("Record found: " + result);
        }
    }

    /**
     * Uses Julyn's CircularQueue for pending triage requests.
     * Signature confirmed from group-c/db-integration:
     *   T peek(), boolean isEmpty(), int size(), int getCapacity()
     *
     * NOTE: peek() only, not dequeue() — the console menu should not be
     * silently consuming pending triage requests just to display them.
     */
    private void handleTriageQueueView() {
        if (triageQueue.isEmpty()) {
            System.out.println("Triage queue is empty.");
            return;
        }

        System.out.println("Queue size: " + triageQueue.size()
                + " / " + triageQueue.getCapacity());
        System.out.println("Next in line: " + triageQueue.peek());
    }

    /**
     * TODO: Wire this up once Amankwah's BFS/DFS lands (group-b, per docs/team-workstreams.md).
     * His MAX_TRAVERSAL_DEPTH formula: (22394896 % 15) + 5.
     * Expected usage once available, something like:
     *   GraphTraversal.bfs(hospitalGraph, startIndex, maxDepth)
     *   GraphTraversal.dfs(hospitalGraph, startIndex, maxDepth)
     */
    private void handleGraphTraversal() {
        System.out.println("Graph traversal (BFS/DFS) is not available yet.");
        System.out.println("This depends on Amankwah's branch, which hasn't been merged.");
    }

    /**
     * TODO: Wire this up once Denzel's Prim/Kruskal MST lands (group-b).
     * His CONGESTION_PENALTY base weight modifier: 22013390.
     * Expected usage once available, something like:
     *   MstAlgorithms.kruskal(hospitalGraph)
     *   MstAlgorithms.prim(hospitalGraph)
     */
    private void handleMstCalculation() {
        System.out.println("MST calculation (Prim/Kruskal) is not available yet.");
        System.out.println("This depends on Denzel's branch, which hasn't been merged.");
    }
}
