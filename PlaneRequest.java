/**
 * PlaneRequest.java
 * Communication object between Plane and ATC threads.
 * Uses volatile fields to ensure visibility across threads.
 */
public class PlaneRequest {

    public enum Type { LANDING, TAKEOFF }

    private final Plane plane;
    private final Type type;
    private volatile boolean processed = false;
    private volatile boolean granted = false;
    private volatile int assignedGate = -1;

    public PlaneRequest(Plane plane, Type type) {
        this.plane = plane;
        this.type = type;
    }

    public Plane getPlane() { return plane; }
    public Type getType() { return type; }
    public boolean isProcessed() { return processed; }
    public boolean isGranted() { return granted; }
    public int getAssignedGate() { return assignedGate; }

    public void setProcessed(boolean processed) { this.processed = processed; }
    public void setGranted(boolean granted) { this.granted = granted; }
    public void setAssignedGate(int gate) { this.assignedGate = gate; }
}
