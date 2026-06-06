package com.vehiclerental.repository;

import com.vehiclerental.Database;
import com.vehiclerental.model.Booking;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BookingRepository {
    private static final String SELECT_ALL = "SELECT id, vehicle_id, customer_name, days, total FROM bookings ORDER BY id";
    private static final String INSERT_BOOKING = "INSERT INTO bookings(vehicle_id, customer_name, days, total) VALUES (?, ?, ?, ?)";
    private static final String SELECT_BY_VEHICLE_AND_CUSTOMER = "SELECT id, vehicle_id, customer_name, days, total FROM bookings WHERE vehicle_id = ? AND customer_name = ? LIMIT 1";
    private static final String DELETE_BY_ID = "DELETE FROM bookings WHERE id = ?";

    public List<Booking> findAll() {
        var bookings = new ArrayList<Booking>();
        try (Connection connection = Database.connect(); PreparedStatement statement = connection.prepareStatement(SELECT_ALL); ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                bookings.add(mapBooking(resultSet));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load bookings.", e);
        }
        return bookings;
    }

    public Optional<Booking> findByVehicleAndCustomer(int vehicleId, String customerName) {
        try (Connection connection = Database.connect(); PreparedStatement statement = connection.prepareStatement(SELECT_BY_VEHICLE_AND_CUSTOMER)) {
            statement.setInt(1, vehicleId);
            statement.setString(2, customerName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapBooking(resultSet)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load booking.", e);
        }
    }

    public Optional<Booking> findByVehicleAndCustomer(Connection connection, int vehicleId, String customerName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_BY_VEHICLE_AND_CUSTOMER)) {
            statement.setInt(1, vehicleId);
            statement.setString(2, customerName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapBooking(resultSet)) : Optional.empty();
            }
        }
    }

    public int save(int vehicleId, String customerName, int days, double total) {
        try (Connection connection = Database.connect(); PreparedStatement statement = connection.prepareStatement(INSERT_BOOKING, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, vehicleId);
            statement.setString(2, customerName);
            statement.setInt(3, days);
            statement.setDouble(4, total);
            int affected = statement.executeUpdate();
            if (affected == 0) {
                throw new IllegalStateException("Unable to insert booking.");
            }
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new IllegalStateException("Booking created but no ID was generated.");
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to save booking.", e);
        }
    }

    public int save(Connection connection, int vehicleId, String customerName, int days, double total) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_BOOKING, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, vehicleId);
            statement.setString(2, customerName);
            statement.setInt(3, days);
            statement.setDouble(4, total);
            int affected = statement.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Unable to insert booking.");
            }
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new SQLException("Booking created but no ID was generated.");
        }
    }

    public boolean deleteById(int id) {
        try (Connection connection = Database.connect(); PreparedStatement statement = connection.prepareStatement(DELETE_BY_ID)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to delete booking.", e);
        }
    }

    public boolean deleteById(Connection connection, int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(DELETE_BY_ID)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    private Booking mapBooking(ResultSet resultSet) throws SQLException {
        return new Booking(
                resultSet.getInt("id"),
                resultSet.getInt("vehicle_id"),
                resultSet.getString("customer_name"),
                resultSet.getInt("days"),
                resultSet.getDouble("total")
        );
    }
}
