# Asia Pacific Airport Simulation

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Concurrency](https://img.shields.io/badge/Concurrency-Multi--Threading-blue?style=for-the-badge)

A multi-threaded Java simulation modeling the operations of the Asia Pacific Airport. This project was developed as part of a Concurrent Programming assignment to demonstrate the practical application of thread synchronization, mutual exclusion, and safety in concurrent environments.

## ✈️ Project Overview
The system simulates multiple planes attempting to land, perform ground operations, and depart using limited shared resources. The core challenge involves managing these resources concurrently without causing race conditions, deadlocks, or thread starvation.

**Key Infrastructure Setup:**
- **1 Runway:** Shared for both landings and takeoffs.
- **3 Gates:** For docking and ground operations.
- **1 Refueling Truck:** Shared across all gates.
- **6 Planes:** Attempting to use the airport simultaneously (including an emergency scenario).

## 🚀 Features
- **Concurrent Execution:** Each plane runs as an independent thread. The Air Traffic Controller (ATC) operates as a separate producer-consumer thread.
- **Mutual Exclusion & Thread Safety:** Utilizes Java's `synchronized` keyword, `wait()`, and `notifyAll()` to safely manage shared resources (runway, gates, and the refueling truck).
- **Complex Gate Operations:** At each gate, sub-threads are spawned for Passenger Handling, Cleaning, and Refueling. A barrier synchronization (`Thread.join()`) ensures all three operations complete before the plane can undock.
- **Emergency Priority Handling:** The ATC identifies and prioritizes planes declared with a "Fuel Emergency" over standard arrivals.
- **Congestion Handling:** Planes are forced to wait in the airspace if the airport is at maximum capacity or gates are full.
- **Detailed Statistics:** Generates a comprehensive summary report of the simulation upon completion.

## 🛠️ Technologies Used
- **Java SE:** The entire system is built using pure Java.
- **Core Threading Facilities:** Implemented exclusively with `Thread`, `Runnable`, `synchronized`, `wait()`, `notify()`, and `Thread.join()`. *(Note: No high-level concurrency libraries such as `ExecutorService` or `CompletableFuture` were used, adhering strictly to assignment guidelines).*

## 📂 Project Structure
```
├── src/
│   ├── AirportSimulation.java  # Main driver class
│   ├── ATC.java                # Air Traffic Controller managing resources
│   ├── Plane.java              # Plane thread logic
│   ├── Gate.java               # Gate structure and state
│   ├── RefuelingTruck.java     # Shared refueling truck synchronized monitor
│   └── PlaneRequest.java       # Communication object between planes and ATC
├── Report.md                   # Detailed technical report on concurrency concepts
└── ... (NetBeans project files)
```

## ⚙️ How to Run

### Using an IDE (NetBeans/IntelliJ/Eclipse)
1. Clone or download the repository.
2. Open the project folder in your preferred Java IDE (originally a NetBeans project).
3. Run the `AirportSimulation.java` file.

### Using the Command Line
1. Navigate to the `src` directory:
   ```bash
   cd src
   ```
2. Compile the Java files:
   ```bash
   javac *.java
   ```
3. Run the simulation:
   ```bash
   java AirportSimulation
   ```

## 📝 Concurrency Concepts Demonstrated
- **Race Condition Prevention:** Synchronized methods ensure only one thread modifies the runway and gate status simultaneously.
- **Deadlock Prevention:** Managed through safe lock-ordering, where planes submit requests to a centralized ATC rather than acquiring multiple locks individually.
- **Condition Synchronization:** Planes actively wait (`wait()`) for the single refueling truck and are notified (`notifyAll()`) when it becomes available.

---
*This project was developed for the CT074-3-2 Concurrent Programming module.*
