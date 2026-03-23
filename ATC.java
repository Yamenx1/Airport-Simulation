import java.util.*;

/**
 * ATC.java (Air Traffic Controller)
 * Runs as its own thread to ensure correct thread attribution in output.
 * Manages: runway access, gate assignments, airport capacity, landing/takeoff permissions.
 * Uses wait/notify for inter-thread communication with Plane threads.
 */
public class ATC extends Thread {

    // Shared airport resources
    private final Gate[] gates;
    private final int MAX_CAPACITY = 3; // Max planes on the ground (including runway)
    private volatile boolean runwayFree = true;
    private volatile int planesOnGround = 0;
    private volatile boolean running = true;

    // Request queue for inter-thread communication
    private final LinkedList<PlaneRequest> requestQueue = new LinkedList<>();

    // Statistics tracking
    private final List<Long> waitingTimes = Collections.synchronizedList(new ArrayList<>());
    private int planesServed = 0;
    private int totalPassengersBoarded = 0;

    public ATC(Gate[] gates) {
        super("ATC"); // Name this thread "ATC"
        this.gates = gates;
    }

    // ---------- Request Submission (called by Plane threads) ----------

    /**
     * Plane threads call this to submit a landing/takeoff request.
     * The request is queued and the calling thread blocks until ATC processes it.
     */
    public PlaneRequest submitRequest(Plane plane, PlaneRequest.Type type) throws InterruptedException {
        PlaneRequest request = new PlaneRequest(plane, type);
        synchronized (requestQueue) {
            requestQueue.add(request);
            requestQueue.notifyAll(); // Wake up ATC thread
        }
        // Block the calling plane thread until ATC processes the request
        synchronized (request) {
            while (!request.isProcessed()) {
                request.wait();
            }
        }
        return request;
    }

    // ---------- State Update Methods (called by Plane threads) ----------

    /** Called by a Plane thread after it finishes landing and starts coasting */
    public synchronized void releaseRunway() {
        runwayFree = true;
        // Wake up ATC to process any pending requests now that runway is free
        synchronized (requestQueue) {
            requestQueue.notifyAll();
        }
    }

    /** Called by a Plane thread after it undocks from a gate */
    public synchronized void releaseGate(int gateIndex) {
        gates[gateIndex].undock();
        // Wake up ATC to process any pending requests now that a gate is free
        synchronized (requestQueue) {
            requestQueue.notifyAll();
        }
    }

    /** Called by a Plane thread after it takes off and leaves the airport */
    public synchronized void planeDeparted() {
        runwayFree = true;
        planesOnGround--;
        planesServed++;
        // Wake up ATC to process pending requests
        synchronized (requestQueue) {
            requestQueue.notifyAll();
        }
    }

    /** Track passengers boarded for statistics */
    public synchronized void addPassengersBoarded(int count) {
        totalPassengersBoarded += count;
    }

    /** Track waiting time for statistics */
    public void recordWaitingTime(long waitTimeMs) {
        waitingTimes.add(waitTimeMs);
    }

    /** Signal ATC to stop running */
    public void shutdown() {
        running = false;
        synchronized (requestQueue) {
            requestQueue.notifyAll();
        }
    }

    // ---------- ATC Thread Main Loop ----------

    @Override
    public void run() {
        AirportSimulation.log("ATC: Tower is now operational.");
        while (running || !requestQueue.isEmpty()) {
            PlaneRequest request = null;
            synchronized (requestQueue) {
                // Priority: emergency landing requests first
                Iterator<PlaneRequest> it = requestQueue.iterator();
                while (it.hasNext()) {
                    PlaneRequest r = it.next();
                    if (r.getType() == PlaneRequest.Type.LANDING && r.getPlane().isEmergency()) {
                        request = r;
                        it.remove();
                        break;
                    }
                }
                // Then regular requests (FIFO)
                if (request == null && !requestQueue.isEmpty()) {
                    request = requestQueue.poll();
                }
                // If no requests, wait for notification
                if (request == null) {
                    try { requestQueue.wait(300); } catch (InterruptedException e) { break; }
                    continue;
                }
            }
            processRequest(request);
        }
        AirportSimulation.log("ATC: Tower shutting down. All planes have departed.");
    }

    // ---------- Request Processing ----------

    private void processRequest(PlaneRequest request) {
        synchronized (this) {
            if (request.getType() == PlaneRequest.Type.LANDING) {
                processLanding(request);
            } else if (request.getType() == PlaneRequest.Type.TAKEOFF) {
                processTakeoff(request);
            }
        }
    }

    private void processLanding(PlaneRequest request) {
        String planeName = request.getPlane().getPlaneName();
        int freeGate = findFreeGate();

        if (planesOnGround < MAX_CAPACITY && runwayFree && freeGate >= 0) {
            // Grant landing
            if (request.getPlane().isEmergency()) {
                AirportSimulation.log("ATC: *** EMERGENCY *** " + planeName + " declaring fuel shortage!");
                AirportSimulation.log("ATC: Emergency landing permission GRANTED for " + planeName + ".");
            } else {
                AirportSimulation.log("ATC: Landing permission granted for " + planeName + ".");
            }
            AirportSimulation.log("ATC: " + gates[freeGate].getGateName() + " assigned for " + planeName + ".");

            runwayFree = false;
            planesOnGround++;
            gates[freeGate].dock(planeName);
            request.setAssignedGate(freeGate);
            request.setGranted(true);
        } else {
            // Deny landing
            String reason = "Airport Full";
            if (!runwayFree) reason = "Runway Busy";
            else if (freeGate < 0) reason = "No Gates Available";
            AirportSimulation.log("ATC: Landing Permission Denied for " + planeName + ", " + reason + ".");
            request.setGranted(false);
        }
        request.setProcessed(true);
        synchronized (request) {
            request.notifyAll(); // Wake up the waiting Plane thread
        }
    }

    private void processTakeoff(PlaneRequest request) {
        String planeName = request.getPlane().getPlaneName();

        if (runwayFree) {
            AirportSimulation.log("ATC: Takeoff granted for " + planeName + ". Runway is free.");
            runwayFree = false;
            request.setGranted(true);
        } else {
            AirportSimulation.log("ATC: Takeoff denied for " + planeName + ", Runway Busy. Please hold.");
            request.setGranted(false);
        }
        request.setProcessed(true);
        synchronized (request) {
            request.notifyAll();
        }
    }

    private int findFreeGate() {
        for (int i = 0; i < gates.length; i++) {
            if (!gates[i].isOccupied()) return i;
        }
        return -1;
    }

    // ---------- Statistics Report ----------

    public void printStatistics() {
        System.out.println("\n========== AIRPORT STATISTICS REPORT ==========");

        // Sanity check: all gates should be empty
        System.out.println("\n--- Sanity Checks ---");
        boolean allEmpty = true;
        for (Gate gate : gates) {
            boolean empty = !gate.isOccupied();
            System.out.println(gate.getGateName() + ": " + (empty ? "EMPTY (OK)" : "OCCUPIED (ERROR!)"));
            if (!empty) allEmpty = false;
        }
        System.out.println("All gates empty: " + (allEmpty ? "YES - PASSED" : "NO - FAILED"));
        System.out.println("Planes on ground: " + planesOnGround + (planesOnGround == 0 ? " (OK)" : " (ERROR!)"));

        // Waiting time stats
        System.out.println("\n--- Waiting Time Statistics ---");
        if (!waitingTimes.isEmpty()) {
            long max = Collections.max(waitingTimes);
            long min = Collections.min(waitingTimes);
            double avg = waitingTimes.stream().mapToLong(Long::longValue).average().orElse(0);
            System.out.println("Maximum waiting time: " + max + " ms");
            System.out.println("Minimum waiting time: " + min + " ms");
            System.out.println("Average waiting time: " + String.format("%.0f", avg) + " ms");
        } else {
            System.out.println("No waiting time data recorded.");
        }

        // Service stats
        System.out.println("\n--- Service Statistics ---");
        System.out.println("Total planes served: " + planesServed);
        System.out.println("Total passengers boarded: " + totalPassengersBoarded);
        System.out.println("================================================\n");
    }
}
