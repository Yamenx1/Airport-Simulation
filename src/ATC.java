import java.util.LinkedList;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collections;

/*
 * ATC (Air Traffic Controller)
 * This runs as a separate thread so that ATC messages come from the ATC thread
 * It manages the runway, gates, and how many planes can be on the ground
 */
public class ATC extends Thread {

    Gate[] gates;               // the 3 gates
    boolean runwayFree = true;  // is the runway available
    int planesOnGround = 0;     // how many planes currently on the ground
    int maxCapacity = 3;        // max planes allowed on ground at once
    boolean running = true;     // controls the main loop

    // queue to hold requests from planes
    LinkedList<PlaneRequest> requests = new LinkedList<>();

    // for statistics
    ArrayList<Long> waitTimes = new ArrayList<>();
    int totalPlanesServed = 0;
    int totalPassengers = 0;

    public ATC(Gate[] gates) {
        super("ATC"); // set thread name to "ATC"
        this.gates = gates;
    }

    // planes call this method to send a request to ATC
    // the plane thread will wait here until ATC processes the request
    public PlaneRequest sendRequest(Plane plane, PlaneRequest.Type type) throws InterruptedException {
        PlaneRequest req = new PlaneRequest(plane, type);

        // add request to queue
        synchronized (requests) {
            requests.add(req);
            requests.notifyAll(); // wake up ATC
        }

        // plane waits here until ATC processes the request
        synchronized (req) {
            while (!req.processed) {
                req.wait();
            }
        }
        return req;
    }

    // called by plane when it finishes landing and frees the runway
    public synchronized void freeRunway() {
        runwayFree = true;
        synchronized (requests) {
            requests.notifyAll();
        }
    }

    // called by plane when it leaves a gate
    public synchronized void freeGate(int gateIdx) {
        gates[gateIdx].freegate();
        synchronized (requests) {
            requests.notifyAll();
        }
    }

    // called when plane has taken off and left the airport
    public synchronized void planeLeft() {
        runwayFree = true;
        planesOnGround--;
        totalPlanesServed++;
        synchronized (requests) {
            requests.notifyAll();
        }
    }

    // add passenger count for statistics
    public synchronized void addPassengers(int count) {
        totalPassengers += count;
    }

    // record how long a plane waited
    public void addWaitTime(long time) {
        synchronized (waitTimes) {
            waitTimes.add(time);
        }
    }

    // stop the ATC thread
    public void stopRunning() {
        running = false;
        synchronized (requests) {
            requests.notifyAll();
        }
    }

    // main ATC thread loop
    @Override
    public void run() {
        print("ATC: Tower is now operational.");

        while (running || !requests.isEmpty()) {
            PlaneRequest req = null;

            synchronized (requests) {
                // check for emergency planes first (they get priority)
                Iterator<PlaneRequest> it = requests.iterator();
                while (it.hasNext()) {
                    PlaneRequest r = it.next();
                    if (r.type == PlaneRequest.Type.LANDING && r.plane.isEmergency) {
                        req = r;
                        it.remove();
                        break;
                    }
                }

                // if no emergency, take the first request
                if (req == null && !requests.isEmpty()) {
                    req = requests.poll();
                }

                // no requests? wait a bit
                if (req == null) {
                    try {
                        requests.wait(300);
                    } catch (InterruptedException e) {
                        break;
                    }
                    continue;
                }
            }

            // process the request
            handleRequest(req);
        }

        print("ATC: Tower shutting down. All planes have departed.");
    }

    // handle a single request
    private void handleRequest(PlaneRequest req) {
        synchronized (this) {
            if (req.type == PlaneRequest.Type.LANDING) {
                handleLanding(req);
            } else {
                handleTakeoff(req);
            }
        }
    }

    // handle landing request
    private void handleLanding(PlaneRequest req) {
        String name = req.plane.planeName;
        int gate = findFreeGate();

        // check if we can land: need free runway, free gate, and space on ground
        if (planesOnGround < maxCapacity && runwayFree && gate >= 0) {
            // landing is granted
            if (req.plane.isEmergency) {
                print("ATC: *** EMERGENCY *** " + name + " declaring fuel shortage!");
                print("ATC: Emergency landing permission GRANTED for " + name + ".");
            } else {
                print("ATC: Landing permission granted for " + name + ".");
            }
            print("ATC: " + gates[gate].getName() + " assigned for " + name + ".");

            runwayFree = false;
            planesOnGround++;
            gates[gate].assignPlane(name);
            req.gateNumber = gate;
            req.granted = true;
        } else {
            // landing denied - airport is full or runway busy
            String reason = "Airport Full";
            if (!runwayFree) reason = "Runway Busy";
            else if (gate < 0) reason = "No Gates Available";
            print("ATC: Landing Permission Denied for " + name + ", " + reason + ".");
            req.granted = false;
        }

        // notify the waiting plane thread
        req.processed = true;
        synchronized (req) {
            req.notifyAll();
        }
    }

    // handle takeoff request
    private void handleTakeoff(PlaneRequest req) {
        String name = req.plane.planeName;

        if (runwayFree) {
            print("ATC: Taking-off is granted for " + name + ". Runway is free.");
            runwayFree = false;
            req.granted = true;
        } else {
            print("ATC: Takeoff denied for " + name + ", Runway Busy. Please hold.");
            req.granted = false;
        }

        req.processed = true;
        synchronized (req) {
            req.notifyAll();
        }
    }

    // find an empty gate, returns -1 if none available
    private int findFreeGate() {
        for (int i = 0; i < gates.length; i++) {
            if (gates[i].isFree()) return i;
        }
        return -1;
    }

    // print with thread name
    private void print(String msg) {
        System.out.println("[" + Thread.currentThread().getName() + "] " + msg);
    }

    // print statistics at the end
    public void printStats() {
        System.out.println("\n========== AIRPORT STATISTICS REPORT ==========");

        // check that all gates are empty (sanity check)
        System.out.println("\n--- Sanity Checks ---");
        boolean allOk = true;
        for (Gate g : gates) {
            boolean empty = g.isFree();
            System.out.println(g.getName() + ": " + (empty ? "EMPTY (OK)" : "OCCUPIED (ERROR!)"));
            if (!empty) allOk = false;
        }
        System.out.println("All gates empty: " + (allOk ? "YES" : "NO"));
        System.out.println("Planes on ground: " + planesOnGround);

        // waiting time stats
        System.out.println("\n--- Waiting Time Statistics ---");
        if (!waitTimes.isEmpty()) {
            long max = Collections.max(waitTimes);
            long min = Collections.min(waitTimes);
            long sum = 0;
            for (long t : waitTimes) sum += t;
            double avg = (double) sum / waitTimes.size();

            System.out.println("Maximum waiting time: " + max + " ms");
            System.out.println("Minimum waiting time: " + min + " ms");
            System.out.println("Average waiting time: " + String.format("%.0f", avg) + " ms");
        }

        // planes and passengers
        System.out.println("\n--- Service Statistics ---");
        System.out.println("Total planes served: " + totalPlanesServed);
        System.out.println("Total passengers boarded: " + totalPassengers);
        System.out.println("================================================\n");
    }
}
