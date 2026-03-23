// Gate class - represents one gate at the airport
public class Gate {
    int gateId;
    boolean isOccupied;
    String planeName;

    public Gate(int id) {
        this.gateId = id;
        this.isOccupied = false;
        this.planeName = "";
    }

    public String getName() {
        return "Gate-" + gateId;
    }

    // check if gate is free
    public synchronized boolean isFree() {
        return !isOccupied;
    }

    // assign plane to gate
    public synchronized void assignPlane(String name) {
        isOccupied = true;
        planeName = name;
    }

    // free the gate when plane leaves
    public synchronized void freegate() {
        isOccupied = false;
        planeName = "";
    }
}
