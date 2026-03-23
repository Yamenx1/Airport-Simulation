import java.util.Random;

/**
 * Plane.java
 * Each Plane runs as its own thread, simulating the full lifecycle:
 * Request landing -> Land -> Coast to gate -> Dock -> Gate operations -> Undock -> Takeoff
 * 
 * Gate operations run concurrently using sub-threads:
 *   - Passenger handling (disembark then embark)
 *   - Cleaning and supply refill
 *   - Refueling (shared truck - mutual exclusion)
 */
public class Plane extends Thread {
    private final int planeId;
    private final ATC atc;
    private final Gate[] gates;
    private final RefuelingTruck refuelingTruck;
    private final boolean emergency; // true if plane has fuel shortage
    private final int passengerCount;
    private int assignedGateIndex = -1;
    private long requestTime; // For waiting time statistics

    public Plane(int planeId, ATC atc, Gate[] gates, RefuelingTruck truck, boolean emergency) {
        super("Plane-" + planeId); // Thread name matches plane name
        this.planeId = planeId;
        this.atc = atc;
        this.gates = gates;
        this.refuelingTruck = truck;
        this.emergency = emergency;
        // Random passenger count, max 50
        this.passengerCount = new Random().nextInt(41) + 10; // 10-50 passengers
    }

    public String getPlaneName() { return "Plane-" + planeId; }
    public boolean isEmergency() { return emergency; }

    @Override
    public void run() {
        try {
            // ========== LANDING PHASE ==========
            requestTime = System.currentTimeMillis();

            if (emergency) {
                AirportSimulation.log(getPlaneName() + ": *** EMERGENCY *** Fuel shortage! Requesting immediate landing!");
            } else {
                AirportSimulation.log(getPlaneName() + ": Requesting landing.");
            }

            // Keep requesting until landing is granted
            boolean landed = false;
            while (!landed) {
                PlaneRequest response = atc.submitRequest(this, PlaneRequest.Type.LANDING);
                if (response.isGranted()) {
                    assignedGateIndex = response.getAssignedGate();
                    landed = true;
                } else {
                    // Denied - circle and retry
                    AirportSimulation.log(getPlaneName() + ": Circling... waiting for clearance.");
                    Thread.sleep(1500 + new Random().nextInt(1000));
                    AirportSimulation.log(getPlaneName() + ": Re-requesting landing.");
                }
            }

            // Record waiting time
            long waitTime = System.currentTimeMillis() - requestTime;
            atc.recordWaitingTime(waitTime);

            // Land on the runway
            AirportSimulation.log(getPlaneName() + ": Landing on runway.");
            Thread.sleep(700); // Simulate landing
            AirportSimulation.log(getPlaneName() + ": Landed on runway.");

            // Release the runway (other planes can now land/takeoff)
            atc.releaseRunway();
            AirportSimulation.log(getPlaneName() + ": Runway is now free.");

            // Coast to the assigned gate
            String gateName = gates[assignedGateIndex].getGateName();
            AirportSimulation.log(getPlaneName() + ": Coasting to " + gateName + ".");
            Thread.sleep(500); // Simulate coasting

            // Dock at the gate
            AirportSimulation.log(getPlaneName() + ": Docked at " + gateName + ".");

            // ========== GATE OPERATIONS (CONCURRENT) ==========
            performGateOperations();

            // ========== TAKEOFF PHASE ==========
            AirportSimulation.log(getPlaneName() + ": Requesting takeoff.");

            // Keep requesting until takeoff is granted
            boolean tookOff = false;
            while (!tookOff) {
                PlaneRequest response = atc.submitRequest(this, PlaneRequest.Type.TAKEOFF);
                if (response.isGranted()) {
                    tookOff = true;
                } else {
                    Thread.sleep(500);
                    AirportSimulation.log(getPlaneName() + ": Re-requesting takeoff.");
                }
            }

            // Undock from gate
            AirportSimulation.log(getPlaneName() + ": Undocking from " + gateName + ".");
            Thread.sleep(300);
            atc.releaseGate(assignedGateIndex);
            AirportSimulation.log(getPlaneName() + ": " + gateName + " is now free.");

            // Coast to runway
            AirportSimulation.log(getPlaneName() + ": Coasting to runway.");
            Thread.sleep(400);

            // Take off
            AirportSimulation.log(getPlaneName() + ": Taking off!");
            Thread.sleep(600);
            AirportSimulation.log(getPlaneName() + ": Has departed. Goodbye!");

            // Notify ATC that plane has left the airport
            atc.planeDeparted();

        } catch (InterruptedException e) {
            AirportSimulation.log(getPlaneName() + ": Interrupted!");
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Performs concurrent gate operations using sub-threads:
     * 1. Passenger handling (disembark old, embark new)
     * 2. Cleaning & supply refill
     * 3. Refueling (uses shared RefuelingTruck)
     * All three run concurrently; plane waits for all to finish using Thread.join().
     */
    private void performGateOperations() throws InterruptedException {
        int disembarkCount = passengerCount;
        int embarkCount = new Random().nextInt(41) + 10; // 10-50 new passengers

        // Sub-thread 1: Passenger handling
        Thread passengerThread = new Thread(() -> {
            try {
                Thread.currentThread().setName(getPlaneName() + "-Passengers");
                // Disembark
                AirportSimulation.log(getPlaneName() + " Passengers: " + disembarkCount + " passengers disembarking.");
                Thread.sleep(600);
                AirportSimulation.log(getPlaneName() + " Passengers: All passengers disembarked.");
                // Embark new passengers
                AirportSimulation.log(getPlaneName() + " Passengers: " + embarkCount + " new passengers embarking.");
                Thread.sleep(600);
                AirportSimulation.log(getPlaneName() + " Passengers: All passengers boarded.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, getPlaneName() + "-Passengers");

        // Sub-thread 2: Cleaning and supply refill
        Thread cleaningThread = new Thread(() -> {
            try {
                Thread.currentThread().setName(getPlaneName() + "-Cleaning");
                AirportSimulation.log(getPlaneName() + ": Cleaning and refilling supplies started.");
                Thread.sleep(700);
                AirportSimulation.log(getPlaneName() + ": Cleaning and refilling supplies complete.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, getPlaneName() + "-Cleaning");

        // Sub-thread 3: Refueling (uses the shared RefuelingTruck)
        Thread refuelThread = new Thread(() -> {
            try {
                Thread.currentThread().setName(getPlaneName() + "-Refueling");
                refuelingTruck.refuel(getPlaneName());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, getPlaneName() + "-Refueling");

        // Start all three operations concurrently
        passengerThread.start();
        cleaningThread.start();
        refuelThread.start();

        // Wait for all operations to complete (join = barrier synchronization)
        passengerThread.join();
        cleaningThread.join();
        refuelThread.join();

        // Track passengers boarded for statistics
        atc.addPassengersBoarded(embarkCount);

        AirportSimulation.log(getPlaneName() + ": All gate operations complete. Ready for departure.");
    }
}
