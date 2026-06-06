package com.vehiclerental;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {
    private static final String CREATE_VEHICLES = "CREATE TABLE IF NOT EXISTS vehicles (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "name TEXT NOT NULL, " +
            "price_per_day REAL NOT NULL, " +
            "status TEXT NOT NULL DEFAULT 'AVAILABLE' CHECK(status IN ('AVAILABLE','BOOKED'))" +
            ");";

    private static final String CREATE_BOOKINGS = "CREATE TABLE IF NOT EXISTS bookings (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "vehicle_id INTEGER NOT NULL, " +
            "customer_name TEXT NOT NULL, " +
            "days INTEGER NOT NULL, " +
            "total REAL NOT NULL, " +
            "FOREIGN KEY(vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE" +
            ");";

    private Database() {
        // utility class
    }

    public static Connection connect() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver is not available.", e);
        }
        Connection connection = DriverManager.getConnection(AppConfig.DATABASE_URL);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    public static void ensureSchema() {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.execute(CREATE_VEHICLES);
            statement.execute(CREATE_BOOKINGS);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to initialize database schema.", e);
        }
    }
}
