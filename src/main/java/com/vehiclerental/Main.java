package com.vehiclerental;

import com.vehiclerental.repository.BookingRepository;
import com.vehiclerental.repository.VehicleRepository;
import com.vehiclerental.service.BookingService;
import com.vehiclerental.service.VehicleService;
import com.vehiclerental.ui.VehicleRentalApp;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        Database.ensureSchema();

        var vehicleRepository = new VehicleRepository();
        var bookingRepository = new BookingRepository();
        var vehicleService = new VehicleService(vehicleRepository);
        var bookingService = new BookingService(vehicleRepository, bookingRepository);

        SwingUtilities.invokeLater(() -> {
            var app = new VehicleRentalApp(vehicleService, bookingService);
            app.setVisible(true);
        });
    }
}
