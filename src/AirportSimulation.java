import java.util.Random;

/*
 * AirportSimulation - Main class
 * This simulates the Asia Pacific Airport with 6 planes, 3 gates, 1 runway
 * and 1 refueling truck. Uses threads for concurrency.
 */
public class AirportSimulation {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("============================================");
        System.out.println("   ASIA PACIFIC AIRPORT SIMULATION");
        System.out.println("============================================\n");

        long startTime = System.currentTimeMillis();

        // create 3 gates
        Gate[] gates = new Gate[3];
        gates[0] = new Gate(1);
        gates[1] = new Gate(2);
        gates[2] = new Gate(3);

        // create the single refueling truck
        RefuelingTruck truck = new RefuelingTruck();

        // create and start ATC thread
        ATC atc = new ATC(gates);
        atc.setDaemon(true);
        atc.start();

        // create 6 planes (plane 6 is emergency with fuel shortage)
        Plane[] planes = new Plane[6];
        for (int i = 0; i < 6; i++) {
            boolean emergency = (i == 5); // last plane has fuel emergency
            planes[i] = new Plane(i + 1, atc, gates, truck, emergency);
        }

        // start planes with random delays (0, 1 or 2 seconds between each)
        Random rand = new Random();
        for (int i = 0; i < 6; i++) {
            if (i > 0) {
                int delay = rand.nextInt(3) * 1000; // 0, 1000, or 2000 ms
                Thread.sleep(delay);
            }
            String msg = planes[i].planeName + " arriving at airport airspace.";
            if (planes[i].isEmergency) msg += " *** FUEL EMERGENCY ***";
            System.out.println("[main] Main: " + msg);
            planes[i].start();
        }

        // wait for all planes to finish
        for (int i = 0; i < 6; i++) {
            planes[i].join();
        }

        Thread.sleep(500);
        atc.stopRunning();
        Thread.sleep(300);

        // print how long the simulation took
        long endTime = System.currentTimeMillis();
        double duration = (endTime - startTime) / 1000.0;
        System.out.println("\n--- Simulation completed in " + String.format("%.1f", duration) + " seconds ---");

        // print statistics
        atc.printStats();
    }
}
