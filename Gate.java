/**
 * Gate.java
 * Represents an airport gate where planes dock for passenger and service operations.
 * Shared resource protected by synchronization.
 */
public class Gate {
    private final int gateId;
    private volatile boolean occupied = false;
    private String currentPlane = "";

    public Gate(int gateId) {
        this.gateId = gateId;
    }

    public int getGateId() { return gateId; }
    public String getGateName() { return "Gate-" + gateId; }
    public synchronized boolean isOccupied() { return occupied; }

    // Dock a plane at this gate
    public synchronized void dock(String planeName) {
        occupied = true;
        currentPlane = planeName;
    }

    // Undock a plane from this gate
    public synchronized void undock() {
        occupied = false;
        currentPlane = "";
    }

    public synchronized String getCurrentPlane() { return currentPlane; }
}
