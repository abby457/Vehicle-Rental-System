package com.vehiclerental.model;

public enum VehicleStatus {
    AVAILABLE,
    BOOKED;

    public static VehicleStatus fromDbValue(String value) {
        if (value == null) {
            return AVAILABLE;
        }
        return switch (value.trim().toUpperCase()) {
            case "BOOKED" -> BOOKED;
            default -> AVAILABLE;
        };
    }
}
