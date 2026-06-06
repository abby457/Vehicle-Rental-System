package com.vehiclerental.service;

import com.vehiclerental.Database;
import com.vehiclerental.model.Booking;
import com.vehiclerental.model.Vehicle;
import com.vehiclerental.model.VehicleStatus;
import com.vehiclerental.repository.BookingRepository;
import com.vehiclerental.repository.VehicleRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class BookingService {
    private final VehicleRepository vehicleRepository;
    private final BookingRepository bookingRepository;

    public BookingService(VehicleRepository vehicleRepository, BookingRepository bookingRepository) {
        this.vehicleRepository = vehicleRepository;
        this.bookingRepository = bookingRepository;
    }

    public List<Booking> listBookings() {
        return bookingRepository.findAll();
    }

    public Booking bookVehicle(int vehicleId, String customerName, int days) {
        if (customerName == null || customerName.isBlank()) {
            throw new IllegalArgumentException("Customer name cannot be empty.");
        }
        if (days <= 0) {
            throw new IllegalArgumentException("Days must be a positive integer.");
        }

        try (Connection connection = Database.connect()) {
            connection.setAutoCommit(false);
            try {
                Vehicle vehicle = vehicleRepository.findById(connection, vehicleId)
                        .orElseThrow(() -> new IllegalArgumentException("Vehicle not found with ID " + vehicleId));
                if (!vehicle.isAvailable()) {
                    throw new IllegalStateException("Vehicle is already booked.");
                }
                double total = vehicle.pricePerDay() * days;
                int bookingId = bookingRepository.save(connection, vehicleId, customerName.trim(), days, total);
                vehicleRepository.updateStatus(connection, vehicleId, VehicleStatus.BOOKED);
                connection.commit();
                return new Booking(bookingId, vehicleId, customerName.trim(), days, total);
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to complete booking.", e);
        }
    }

    public void returnVehicle(int vehicleId, String customerName) {
        if (customerName == null || customerName.isBlank()) {
            throw new IllegalArgumentException("Customer name cannot be empty.");
        }

        try (Connection connection = Database.connect()) {
            connection.setAutoCommit(false);
            try {
                Optional<Booking> booking = bookingRepository.findByVehicleAndCustomer(connection, vehicleId, customerName.trim());
                if (booking.isEmpty()) {
                    throw new IllegalArgumentException("No booking found for vehicle ID " + vehicleId + " and customer '" + customerName + "'.");
                }
                bookingRepository.deleteById(connection, booking.get().id());
                vehicleRepository.updateStatus(connection, vehicleId, VehicleStatus.AVAILABLE);
                connection.commit();
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to process return.", e);
        }
    }
}
