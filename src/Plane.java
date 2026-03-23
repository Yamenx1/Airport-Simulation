import java.util.Random;

/*
 * Plane class - each plane is a thread
 * It goes through the whole process: land, go to gate, passengers get on/off, then take off
 */
public class Plane extends Thread {
    int id;
    String planeName;
    ATC atc;
    Gate[] gates;
    RefuelingTruck truck;
    boolean isEmergency;
    int passengers;
    int gateIndex = -1;
    long arrivalTime; // when the plane first requested landing
    Random rand = new Random();

    public Plane(int id, ATC atc, Gate[] gates, RefuelingTruck truck, boolean emergency) {
        super("Plane-" + id);
        this.id = id;
        this.planeName = "Plane-" + id;
        this.atc = atc;
        this.gates = gates;
        this.truck = truck;
        this.isEmergency = emergency;
        this.passengers = rand.nextInt(41) + 10; // random between 10-50
    }

    // helper to print messages with thread name
    void print(String msg) {
        System.out.println("[" + Thread.currentThread().getName() + "] " + msg);
    }

    @Override
    public void run() {
        try {
            arrivalTime = System.currentTimeMillis();

            // request landing
            if (isEmergency) {
                print(planeName + ": *** EMERGENCY *** Fuel shortage! Requesting immediate landing!");
            } else {
                print(planeName + ": Requesting Landing.");
            }

            // keep trying to land until ATC says yes
            boolean gotPermission = false;
            while (!gotPermission) {
                PlaneRequest response = atc.sendRequest(this, PlaneRequest.Type.LANDING);
                if (response.granted) {
                    gateIndex = response.gateNumber;
                    gotPermission = true;
                } else {
                    // denied, so circle around and try again
                    print(planeName + ": Circling... waiting for clearance.");
                    Thread.sleep(1500 + rand.nextInt(1000));
                    print(planeName + ": Requesting Landing.");
                }
            }

            // record how long we waited
            long waitTime = System.currentTimeMillis() - arrivalTime;
            atc.addWaitTime(waitTime);

            // land on runway
            print(planeName + ": Landing.");
            Thread.sleep(700);
            print(planeName + ": Landed.");

            // free the runway so other planes can use it
            atc.freeRunway();

            // coast to gate
            String gateName = gates[gateIndex].getName();
            print(planeName + ": Coasting to " + gateName + ".");
            Thread.sleep(500);

            // dock at gate
            print(planeName + ": Docked at " + gateName + ".");

            // ---- do gate operations (these happen at the same time) ----
            doGateOperations();

            // ---- takeoff ----
            print(planeName + ": Requesting Taking off.");

            boolean canTakeoff = false;
            while (!canTakeoff) {
                PlaneRequest response = atc.sendRequest(this, PlaneRequest.Type.TAKEOFF);
                if (response.granted) {
                    canTakeoff = true;
                } else {
                    Thread.sleep(500);
                    print(planeName + ": Requesting Taking off.");
                }
            }

            // undock and leave
            print(planeName + ": Undocking from " + gateName + ".");
            Thread.sleep(300);
            atc.freeGate(gateIndex);
            print(planeName + ": " + gateName + " is now free.");

            // go to runway
            print(planeName + ": Coasting to runway.");
            Thread.sleep(400);

            // take off
            print(planeName + ": Taking off.");
            Thread.sleep(600);
            print(planeName + ": Has departed.");

            // tell ATC we left
            atc.planeLeft();

        } catch (InterruptedException e) {
            print(planeName + ": Thread interrupted!");
        }
    }

    /*
     * Gate operations - 3 things happen at the same time:
     * 1. passengers get off and new ones get on
     * 2. clean the plane and refill food/supplies
     * 3. refuel the plane (only 1 truck so planes have to wait for it)
     * We use Thread.join() to wait for all 3 to finish
     */
    void doGateOperations() throws InterruptedException {
        int oldPassengers = passengers;
        int newPassengers = rand.nextInt(41) + 10;

        // thread for passengers
        Thread t1 = new Thread(() -> {
            try {
                print(planeName + "'s Passengers: " + oldPassengers + " disembarking out of " + planeName + ".");
                Thread.sleep(600);
                print(planeName + "'s Passengers: All passengers disembarked.");
                print(planeName + "'s Passengers: " + newPassengers + " embarking into " + planeName + ".");
                Thread.sleep(600);
                print(planeName + "'s Passengers: All passengers boarded.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, planeName + "-Passengers");

        // thread for cleaning
        Thread t2 = new Thread(() -> {
            try {
                print(planeName + ": Cleaning and refilling supplies started.");
                Thread.sleep(700);
                print(planeName + ": Cleaning and refilling supplies complete.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, planeName + "-Cleaning");

        // thread for refueling (uses the shared truck)
        Thread t3 = new Thread(() -> {
            try {
                truck.refuel(planeName);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, planeName + "-Refueling");

        // start all 3 at the same time
        t1.start();
        t2.start();
        t3.start();

        // wait for all 3 to finish before continuing
        t1.join();
        t2.join();
        t3.join();

        // update stats
        atc.addPassengers(newPassengers);

        print(planeName + ": All gate operations complete. Ready for departure.");
    }
}
