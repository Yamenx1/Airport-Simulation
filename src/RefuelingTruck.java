// RefuelingTruck - only 1 truck shared by all planes
// uses synchronized so only one plane refuels at a time
public class RefuelingTruck {
    boolean isBusy = false;

    // refuel method - only one plane can use at a time
    public synchronized void refuel(String planeName) throws InterruptedException {
        // wait if truck is being used by another plane
        while (isBusy) {
            System.out.println("[" + Thread.currentThread().getName() + "] " + planeName + ": Waiting for refueling truck...");
            wait(); // wait until truck is free
        }
        isBusy = true;
        System.out.println("[" + Thread.currentThread().getName() + "] " + planeName + ": Refueling started.");
        Thread.sleep(800);
        System.out.println("[" + Thread.currentThread().getName() + "] " + planeName + ": Refueling complete.");
        isBusy = false;
        notifyAll(); // tell other planes the truck is free now
    }
}
