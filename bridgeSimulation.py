import threading
import time
import random

class Semaphore:
    def __init__(self, initial):
        self.value = initial
        self.mutex = threading.Semaphore(1)
        self.queue = []

    def wait(self):
        self.mutex.acquire()
        if self.value <= 0:
            current_thread = threading.current_thread()
            current_thread.semaphore = threading.Semaphore(0)
            self.queue.append(current_thread)
            self.mutex.release()
            current_thread.semaphore.acquire()
        else:
            self.value -= 1
            self.mutex.release()

    def signal(self):
        self.mutex.acquire()
        if len(self.queue) > 0:
            thread = self.queue.pop(0)
            thread.semaphore.release()
        else:
            self.value += 1
        self.mutex.release()

class Vehicle(threading.Thread):
    total_vehicles = 10
    vehicles_crossed = 0
    vehicles_lock = threading.Lock()

    def __init__(self, vehicle_id, direction, bridge, exit_signal):
        super(Vehicle, self).__init__()
        self.vehicle_id = vehicle_id
        self.direction = direction
        self.bridge = bridge
        self.exit_signal = exit_signal

    def cross_bridge(self):
        print(f"Vehicle {self.vehicle_id} is crossing the bridge in the {self.direction} direction.")
        time.sleep(random.uniform(0.1, 1.0))  # Simulating time to cross the bridge
        print(f"Vehicle {self.vehicle_id} has crossed the bridge.")

        with Vehicle.vehicles_lock:
            Vehicle.vehicles_crossed += 1
            if Vehicle.vehicles_crossed == Vehicle.total_vehicles:
                self.exit_signal.set()
                Vehicle.vehicles_crossed = 0  # Reset the counter for the next round

    def run(self):
        while not self.exit_signal.is_set():
            self.bridge.wait()  # Wait for permission to cross the bridge
            self.cross_bridge()  # Cross the bridge
            self.bridge.signal()  # Release the bridge

if __name__ == "__main__":
    bridge_semaphore = Semaphore(1)  # Semaphore to control access to the bridge
    exit_signal = threading.Event()  # Event to signal when all vehicles have crossed

    # Creating vehicles
    vehicles = []
    for i in range(1, 11):
        direction = "left" if i % 2 == 1 else "right"
        vehicle = Vehicle(vehicle_id=i, direction=direction, bridge=bridge_semaphore, exit_signal=exit_signal)
        vehicles.append(vehicle)

    # Starting vehicles
    for vehicle in vehicles:
        vehicle.start()

    # Waiting for all vehicles to finish
    for vehicle in vehicles:
        vehicle.join()

    # Set the exit signal to notify threads to exit
    exit_signal.set()
