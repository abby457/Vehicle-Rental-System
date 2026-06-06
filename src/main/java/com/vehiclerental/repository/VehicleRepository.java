package com.vehiclerental.repository;

import com.vehiclerental.Database;
import com.vehiclerental.model.Vehicle;
import com.vehiclerental.model.VehicleStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VehicleRepository {
    private static final String SELECT_ALL = "SELECT id, name, price_per_day, status FROM vehicles ORDER BY id";
    private static final String SELECT_BY_ID = "SELECT id, name, price_per_day, status FROM vehicles WHERE id = ?";
    private static final String INSERT_VEHICLE = "INSERT INTO vehicles(name, price_per_day, status) VALUES (?, ?, ?)";
    private static final String DELETE_BY_ID = "DELETE FROM vehicles WHERE id = ?";
    private static final String UPDATE_STATUS = "UPDATE vehicles SET status = ? WHERE id = ?";

    public List<Vehicle> findAll() {
        var vehicles = new ArrayList<Vehicle>();
        try (Connection connection = Database.connect(); PreparedStatement statement = connection.prepareStatement(SELECT_ALL); ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                vehicles.add(mapVehicle(resultSet));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load vehicles.", e);
        }
        return vehicles;
    }

    public Optional<Vehicle> findById(int id) {
        try (Connection connection = Database.connect(); PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapVehicle(resultSet)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load vehicle.", e);
        }
    }

    public int save(String name, double pricePerDay) {
        try (Connection connection = Database.connect(); PreparedStatement statement = connection.prepareStatement(INSERT_VEHICLE, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            statement.setDouble(2, pricePerDay);
            statement.setString(3, VehicleStatus.AVAILABLE.name());
            int affected = statement.executeUpdate();
            if (affected == 0) {
                throw new IllegalStateException("Unable to insert vehicle.");
            }
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new IllegalStateException("Vehicle created but no ID was generated.");
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to save vehicle.", e);
        }
    }

    public boolean deleteById(int id) {
        try (Connection connection = Database.connect(); PreparedStatement statement = connection.prepareStatement(DELETE_BY_ID)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to delete vehicle.", e);
        }
    }

    public boolean updateStatus(int id, VehicleStatus status) {
        try (Connection connection = Database.connect(); PreparedStatement statement = connection.prepareStatement(UPDATE_STATUS)) {
            statement.setString(1, status.name());
            statement.setInt(2, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to update vehicle status.", e);
        }
    }

    public boolean updateStatus(Connection connection, int id, VehicleStatus status) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_STATUS)) {
            statement.setString(1, status.name());
            statement.setInt(2, id);
            return statement.executeUpdate() > 0;
        }
    }

    public Optional<Vehicle> findById(Connection connection, int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapVehicle(resultSet)) : Optional.empty();
            }
        }
    }

    private Vehicle mapVehicle(ResultSet resultSet) throws SQLException {
        return new Vehicle(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getDouble("price_per_day"),
                VehicleStatus.fromDbValue(resultSet.getString("status"))
        );
    }
}
