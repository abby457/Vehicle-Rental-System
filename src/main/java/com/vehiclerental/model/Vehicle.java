package com.vehiclerental.model;

public record Vehicle(int id, String name, double pricePerDay, VehicleStatus status) {
    public boolean isAvailable() {
        return status == VehicleStatus.AVAILABLE;
    }
}
