package com.vehiclerental.model;

public record Booking(int id, int vehicleId, String customerName, int days, double total) {
}
