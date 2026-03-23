# Asia Pacific Airport Simulation - Individual Assignment Report

## CT074-3-2 Concurrent Programming

---

## 1. Introduction and Background

This report documents the development of a concurrent airport simulation system built using Java. The simulation models the operations of Asia Pacific Airport, where six planes attempt to land, perform ground operations, and depart using a single shared runway, three gates, and one refueling truck.

The main goal of this assignment is to apply concurrent programming concepts to simulate real-world airport operations. In a real airport, many things happen at the same time — multiple planes are being serviced at different gates, passengers are boarding and disembarking, and ground crews are cleaning and refueling aircraft simultaneously. This makes it a suitable scenario for concurrent programming, where multiple threads run in parallel and need to coordinate with each other to avoid conflicts.

The system is implemented entirely in Java using basic threading facilities such as `Thread`, `synchronized`, `wait()`, `notify()`, and `Thread.join()`. No restricted libraries like `ExecutorService`, `ForkJoinPool`, or `CompletableFuture` were used. The simulation runs within 60 seconds and handles both basic and additional requirements including emergency landings and a shared refueling truck.

---

## 2. Explanation of Safety Aspects of the Multi-Threaded System

### 2.1 Race Conditions

A race condition happens when two or more threads try to access and modify the same shared data at the same time. In our simulation, several shared resources could be affected by race conditions:

- **Runway**: Multiple planes could try to land or take off at the same time, but only one can use the runway.
- **Gates**: Two planes could be assigned the same gate if the gate status is checked simultaneously.
- **Refueling Truck**: Only one truck exists, so two planes should not refuel at the same time.

To prevent race conditions, we use the `synchronized` keyword on methods and blocks that access shared resources. For example, the ATC's `handleLanding()` and `handleTakeoff()` methods are synchronized so that only one request is processed at a time.

### 2.2 Mutual Exclusion

Mutual exclusion ensures that only one thread can access a critical section at a time. In our simulation:

- The **runway** is protected by a boolean flag `runwayFree` inside the ATC class, which is only modified within `synchronized` blocks.
- The **refueling truck** uses a `synchronized` method with `wait()` and `notifyAll()` so that only one plane can refuel at a time. Other planes wait until the truck is free.
- The **print method** is synchronized to prevent garbled output when multiple threads print at the same time.

### 2.3 Deadlock Prevention

Deadlock occurs when two or more threads are waiting for each other to release resources, and none of them can proceed. In our design, we avoided deadlock by:

- Using a **single lock ordering** — the ATC thread is the only one that makes decisions about runway and gate assignments. Plane threads submit requests and wait for responses, rather than trying to acquire multiple locks themselves.
- Using **timeouts** on `wait()` calls (e.g., `requests.wait(300)`) so the ATC thread periodically checks for new requests even without being explicitly notified.
- Keeping synchronized blocks as **short as possible** to minimize the time locks are held.

### 2.4 Thread Starvation

Thread starvation happens when a thread never gets a chance to execute. In our simulation, we handle this by:

- Processing requests in **FIFO order** (first come, first served) from the request queue.
- Emergency planes get **priority** in the queue, but regular planes are still served eventually as other planes depart and free up resources.

---

## 3. Justification of Coding Techniques Implemented

### 3.1 Thread per Plane

Each plane is implemented as a separate thread by extending the `Thread` class. This is a straightforward approach that makes it easy to simulate each plane operating independently. Each plane thread handles its own lifecycle — from requesting landing to departing.

```java
public class Plane extends Thread {
    public void run() {
        // request landing, land, do gate ops, take off
    }
}
```

### 3.2 Producer-Consumer Pattern for ATC

The ATC (Air Traffic Controller) runs as its own thread and uses a request queue to receive requests from plane threads. This is the **producer-consumer pattern** — planes produce requests, and the ATC consumes and processes them. This design ensures that ATC messages are printed from the ATC thread, which is important for correct thread attribution in the output.

```java
// Plane sends request
PlaneRequest req = new PlaneRequest(plane, type);
synchronized (requests) {
    requests.add(req);
    requests.notifyAll();
}
// Plane waits for ATC to process
synchronized (req) {
    while (!req.processed) {
        req.wait();
    }
}
```

### 3.3 Synchronized Methods for Shared Resources

The `RefuelingTruck` class uses a `synchronized` method with `wait()`/`notifyAll()` to ensure only one plane refuels at a time. This is a classic example of **condition synchronization** — a plane waits if the truck is busy, and is notified when the truck becomes free.

```java
public synchronized void refuel(String planeName) throws InterruptedException {
    while (isBusy) {
        wait();
    }
    isBusy = true;
    // ... refueling happens ...
    isBusy = false;
    notifyAll();
}
```

### 3.4 Thread.join() for Barrier Synchronization

At each gate, three operations happen concurrently: passenger handling, cleaning, and refueling. We create three sub-threads for these operations and use `Thread.join()` to wait for all three to finish before the plane can undock. This acts as a **barrier** — the plane cannot proceed until all gate operations are complete.

```java
t1.start(); t2.start(); t3.start();
t1.join();  t2.join();  t3.join();
```

### 3.5 Emergency Priority Handling

For the additional requirement, the ATC checks the request queue for emergency planes first before processing regular requests. This is done using an iterator that scans the queue for emergency landing requests and processes them with higher priority.

---

## 4. Discussion of Concurrency Concepts

### 4.1 Threads and Concurrency

The simulation uses multiple threads running concurrently. Each plane is a thread, the ATC is a thread, and gate operations spawn sub-threads. This allows multiple planes to be serviced at different gates at the same time, which is the core concept of concurrency — multiple tasks making progress simultaneously.

### 4.2 Synchronization with synchronized and wait/notify

The `synchronized` keyword is used throughout the code to protect shared resources. When a thread enters a `synchronized` method or block, it acquires a lock on the object, preventing other threads from entering any other `synchronized` block on the same object. The `wait()` method makes a thread release the lock and wait until another thread calls `notify()` or `notifyAll()` on the same object. This is used for:

- ATC request processing (planes wait for ATC to process their request)
- Refueling truck access (planes wait for the truck to be free)
- ATC thread waking up when new requests arrive

### 4.3 Thread Safety

Thread safety means that shared data is accessed correctly by multiple threads. In our simulation, we ensure thread safety by:

- Using `synchronized` blocks when reading or writing shared variables like `runwayFree`, `planesOnGround`, and gate occupancy.
- Using a synchronized `print()` method so that output lines from different threads don't get mixed together.
- Using the `PlaneRequest` object as a communication channel between plane and ATC threads, with proper synchronization on the request object.

### 4.4 Critical Sections

A critical section is a part of the code that accesses shared resources and must not be executed by more than one thread at a time. In our simulation, the main critical sections are:

- The ATC's `handleLanding()` and `handleTakeoff()` methods, which modify runway status, gate assignments, and plane count.
- The refueling truck's `refuel()` method, which controls access to the single truck.
- The gate's `assignPlane()` and `freegate()` methods, which modify gate occupancy.

---

## 5. Requirements Fulfilled

### 5.1 Basic Requirements Met

- Single runway for landing and departure
- Maximum 3 planes on airport grounds
- 3 gates for plane operations
- Full plane lifecycle (land → coast → dock → operations → undock → coast → takeoff)
- Concurrent gate operations (passengers, cleaning, refueling)
- Statistics report with sanity checks, waiting times, and service counts
- Simulation completes within 60 seconds
- Correct thread attribution in output

### 5.2 Additional Requirements Met

- Single refueling truck shared across all gates (mutual exclusion)
- Emergency landing scenario with fuel shortage (priority handling)
- Congested scenario simulated (planes denied landing when airport is full)

### 5.3 Requirements NOT Met

ALL Requirements were Fulfilled.

---

## References

1. Goetz, B. et al. (2006). *Java Concurrency in Practice*. Addison-Wesley.
2. Oracle. (2024). *Java SE Documentation - Concurrency*. https://docs.oracle.com/javase/tutorial/essential/concurrency/
3. Silberschatz, A., Galvin, P. B., & Gagne, G. (2018). *Operating System Concepts* (10th ed.). Wiley.
