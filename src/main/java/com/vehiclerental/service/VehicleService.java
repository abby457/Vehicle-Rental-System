package com.vehiclerental.service;

import com.vehiclerental.model.Vehicle;
import com.vehiclerental.model.VehicleStatus;
import com.vehiclerental.repository.VehicleRepository;

import java.util.List;
import java.util.Optional;

public class VehicleService {
    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    public List<Vehicle> listVehicles() {
        return vehicleRepository.findAll();
    }

    public Vehicle addVehicle(String name, double pricePerDay) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Vehicle name cannot be empty.");
        }
        if (pricePerDay <= 0) {
            throw new IllegalArgumentException("Price per day must be a positive number.");
        }
        int id = vehicleRepository.save(name.trim(), pricePerDay);
        return vehicleRepository.findById(id).orElseThrow(() -> new IllegalStateException("Vehicle was added but could not be loaded."));
    }

    public void deleteVehicle(int vehicleId) {
        var vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("No vehicle found with ID " + vehicleId));
        if (!vehicle.isAvailable()) {
            throw new IllegalStateException("Cannot delete a booked vehicle.");
        }
        if (!vehicleRepository.deleteById(vehicleId)) {
            throw new IllegalStateException("Vehicle deletion failed for ID " + vehicleId);
        }
    }

    public Optional<Vehicle> getVehicle(int vehicleId) {
        return vehicleRepository.findById(vehicleId);
    }
}
