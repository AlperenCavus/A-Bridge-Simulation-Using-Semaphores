package Main;

import java.util.*;
//MAHMUT ALPEREN ÇAVUŞ 210408044

class Semaphore {
    private int permits;
   

    public Semaphore(int initialPermits) {
        this.permits = initialPermits;
        
    }

    public synchronized void acquire() throws InterruptedException {
        while (permits == 0) {
            wait();
        }
        permits--;
    }

    public synchronized void release() {
        permits++;
        notify();
    }

    public int availablePermits() {
        return permits;
    }

    
}
class Bridge {
    private final Semaphore semaphore;
    private final Semaphore mutex;
    private int northboundCount;
    private int southboundCount;
    private final int capacity=5;

    public Bridge() {
        semaphore = new Semaphore(capacity); // Capacity of the bridge is 5
        mutex = new Semaphore(1); // Mutex to ensure mutual exclusion
        northboundCount = 0;
        southboundCount = 0;
    }


    public void crossBridge(int vehicleId, String direction) {
        try {
            mutex.acquire();

            if ((direction.equals("Northbound") && southboundCount > 0) ||
                (direction.equals("Southbound") && northboundCount > 0)) {
                System.out.println("Vehicle " + vehicleId + " (" + direction + ") is waiting to cross the bridge.");
                mutex.release();
                return; // Dont acquire the semaphore if there are vehicles from the opposite direction
            }

            mutex.release();

            semaphore.acquire();

            // Update count is called and simulate crossing the bridge
            updateCount(direction, 1);
            System.out.println("Vehicle " + vehicleId + " (" + direction + ") is crossing the bridge.");
            Thread.sleep(1000); 
            System.out.println("Vehicle " + vehicleId + " (" + direction + ") has crossed the bridge.");

            semaphore.release();

            // Update count is called
           updateCount(direction, 0);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Thread interrupted while crossing the bridge.");
        }
    }
    public void updateCount(String direction, int count) {
        try {
			mutex.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        if (direction.equals("Northbound")) {
            northboundCount = count;
        } else {
            southboundCount = count;
        }
        mutex.release();
    }
}

class Vehicle implements Runnable {
    private static int nextId = 1;
    private final int vehicleId;
    private final Bridge bridge;
    private final String direction;

    public Vehicle(Bridge bridge, String direction) {
        this.vehicleId = nextId++;
        this.bridge = bridge;
        this.direction = direction;
    }

    @Override
    public void run() {
        bridge.updateCount(direction, 1);
        bridge.crossBridge(vehicleId, direction);
        bridge.updateCount(direction, 0);
    }
}

public class BridgeSimulation {
    public static void main(String[] args) {
    	Bridge bridge = new Bridge();

        List<Thread> northboundThreads = Arrays.asList(
                new Thread(new Vehicle(bridge, "Northbound")),
                new Thread(new Vehicle(bridge, "Northbound")),
                new Thread(new Vehicle(bridge, "Northbound"))
        );

        List<Thread> southboundThreads = Arrays.asList(
                new Thread(new Vehicle(bridge, "Southbound")),
                new Thread(new Vehicle(bridge, "Southbound")),
                new Thread(new Vehicle(bridge, "Southbound"))
        );

        List<Thread> allThreads = new ArrayList<>();
        allThreads.addAll(northboundThreads);
        allThreads.addAll(southboundThreads);

        // I do shuffling in here because otherwise the output always starts with the vehicle 1.
        Collections.shuffle(allThreads);

        // Start the threads in the shuffled order
        for (Thread thread : allThreads) {
            thread.start();
            try {
                thread.join();  // Ensure that the main thread waits for each thread to complete
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
