import java.util.Random;

/**
 * AirportSimulation.java
 * Main class for the Asia Pacific Airport concurrent simulation.
 *
 * Concurrency concepts demonstrated:
 * - Thread creation and lifecycle management
 * - Mutual exclusion (synchronized, wait/notify) for shared resources
 * - Producer-Consumer pattern (Plane -> ATC request queue)
 * - Barrier synchronization (Thread.join for gate operations)
 * - Thread-safe logging with synchronized print method
 * - Priority scheduling (emergency landing)
 *
 * Restrictions: No ExecutorService, ForkJoinPool, CompletableFuture, or ParallelStream.
 */
public class AirportSimulation {

    private static final int TOTAL_PLANES = 6;
    private static final int NUM_GATES = 3;

    /**
     * Thread-safe logging method.
     * Prints the current thread name alongside the message to verify
     * that each thread only acts on behalf of itself.
     */
    public static synchronized void log(String message) {
        System.out.println("[" + Thread.currentThread().getName() + "] " + message);
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("============================================");
        System.out.println("   ASIA PACIFIC AIRPORT SIMULATION");
        System.out.println("   Concurrent Programming Assignment");
        System.out.println("============================================\n");

        long simStartTime = System.currentTimeMillis();

        // Create airport resources
        Gate[] gates = new Gate[NUM_GATES];
        for (int i = 0; i < NUM_GATES; i++) {
            gates[i] = new Gate(i + 1);
        }

        RefuelingTruck refuelingTruck = new RefuelingTruck();

        // Create and start the ATC thread
        ATC atc = new ATC(gates);
        atc.setDaemon(true); // ATC will stop when all planes finish
        atc.start();

        // Create plane threads
        Plane[] planes = new Plane[TOTAL_PLANES];
        Random rand = new Random();

        for (int i = 0; i < TOTAL_PLANES; i++) {
            // Plane 6 is the emergency plane (fuel shortage) - Additional Requirement
            boolean isEmergency = (i == 5);
            planes[i] = new Plane(i + 1, atc, gates, refuelingTruck, isEmergency);
        }

        // Start planes with random arrival intervals (0, 1, or 2 seconds)
        for (int i = 0; i < TOTAL_PLANES; i++) {
            if (i > 0) {
                int delay = rand.nextInt(3) * 1000; // 0, 1000, or 2000 ms
                Thread.sleep(delay);
            }
            log("Main: " + planes[i].getPlaneName() + " arriving at airport airspace."
                + (planes[i].isEmergency() ? " *** FUEL EMERGENCY ***" : ""));
            planes[i].start();
        }

        // Wait for all planes to finish (complete their lifecycle)
        for (Plane plane : planes) {
            plane.join();
        }

        // Small delay to let ATC finish processing
        Thread.sleep(500);

        // Shutdown ATC and print statistics
        atc.shutdown();
        Thread.sleep(300);

        long simEndTime = System.currentTimeMillis();
        double simDuration = (simEndTime - simStartTime) / 1000.0;

        System.out.println("\n--- Simulation completed in " + String.format("%.1f", simDuration) + " seconds ---");

        // Print the statistics report (sanity checks + stats)
        atc.printStatistics();
    }
}
