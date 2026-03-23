// PlaneRequest - used to send requests between Plane and ATC threads
public class PlaneRequest {
    // type of request
    enum Type { LANDING, TAKEOFF }

    Plane plane;
    Type type;
    boolean processed = false;
    boolean granted = false;
    int gateNumber = -1; // which gate assigned, -1 means none

    public PlaneRequest(Plane p, Type t) {
        this.plane = p;
        this.type = t;
    }
}
