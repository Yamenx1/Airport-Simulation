/**
 * RefuelingTruck.java
 * Represents the single refueling truck shared across all gates.
 * Uses synchronized keyword for mutual exclusion - only one plane can refuel at a time.
 */
public class RefuelingTruck {
    private boolean available = true;

    /**
     * Refuel a plane. Only one plane can use the truck at a time.
     * Uses wait/notify for thread coordination.
     */
    public synchronized void refuel(String planeName) throws InterruptedException {
        // Wait until the truck is available (condition synchronization)
        while (!available) {
            AirportSimulation.log(planeName + ": Waiting for refueling truck...");
            wait();
        }
        available = false;
        AirportSimulation.log(planeName + ": Refueling started.");
        Thread.sleep(800); // Simulate refueling time
        AirportSimulation.log(planeName + ": Refueling complete.");
        available = true;
        notifyAll(); // Wake up any planes waiting for the truck
    }
}
