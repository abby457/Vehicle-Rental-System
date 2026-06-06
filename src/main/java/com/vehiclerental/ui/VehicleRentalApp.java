package com.vehiclerental.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.table.DefaultTableModel;

import com.vehiclerental.model.Booking;
import com.vehiclerental.model.Vehicle;
import com.vehiclerental.service.BookingService;
import com.vehiclerental.service.VehicleService;

public class VehicleRentalApp extends JFrame {
    private final VehicleService vehicleService;
    private final BookingService bookingService;

    public VehicleRentalApp(VehicleService vehicleService, BookingService bookingService) {
        super("Vehicle Rental System");
        this.vehicleService = vehicleService;
        this.bookingService = bookingService;

        initLookAndFeel();
        initUserInterface();
    }

    private void initUserInterface() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(680, 420);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(14, 14));

        var contentPanel = new JPanel(new BorderLayout(12, 12));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        contentPanel.setBackground(new Color(245, 248, 252));
        setContentPane(contentPanel);

        var headerPanel = new JPanel(new BorderLayout(8, 8));
        headerPanel.setBackground(new Color(35, 100, 180));
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(24, 78, 150), 2),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)));

        var titleLabel = new JLabel("Vehicle Rental System");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);

        var subtitleLabel = new JLabel("Manage vehicles, bookings, and returns with ease.");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(220, 230, 250));

        var titleWrapper = new JPanel(new BorderLayout());
        titleWrapper.setOpaque(false);
        titleWrapper.add(titleLabel, BorderLayout.NORTH);
        titleWrapper.add(subtitleLabel, BorderLayout.SOUTH);
        headerPanel.add(titleWrapper, BorderLayout.CENTER);

        var exitButton = createActionButton("Exit", new Color(200, 51, 51), Color.WHITE);
        exitButton.addActionListener(e -> System.exit(0));
        exitButton.setPreferredSize(new Dimension(90, 36));
        headerPanel.add(exitButton, BorderLayout.EAST);

        contentPanel.add(headerPanel, BorderLayout.NORTH);

        var buttonsPanel = new JPanel(new GridLayout(2, 3, 16, 16));
        buttonsPanel.setOpaque(false);
        addButton(buttonsPanel, "Add Vehicle", e -> showAddVehicleDialog());
        addButton(buttonsPanel, "View Vehicles", e -> showViewVehiclesDialog());
        addButton(buttonsPanel, "View Bookings", e -> showViewBookingsDialog());
        addButton(buttonsPanel, "Book Vehicle", e -> showBookVehicleDialog());
        addButton(buttonsPanel, "Delete Vehicle", e -> showDeleteVehicleDialog());
        addButton(buttonsPanel, "Return Vehicle", e -> showReturnVehicleDialog());

        var buttonCard = new JPanel(new BorderLayout());
        buttonCard.setBackground(Color.WHITE);
        buttonCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 226, 235), 1),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)));
        buttonCard.add(buttonsPanel, BorderLayout.CENTER);
        contentPanel.add(buttonCard, BorderLayout.CENTER);

        var statsPanel = new JPanel(new GridLayout(1, 3, 14, 14));
        statsPanel.setOpaque(false);
        statsPanel.add(createStatCard("Available Vehicles", "0", new Color(50, 168, 82)));
        statsPanel.add(createStatCard("Rented Vehicles", "0", new Color(243, 182, 14)));
        statsPanel.add(createStatCard("Total Bookings", "0", new Color(59, 130, 246)));

        var statsCard = new JPanel(new BorderLayout());
        statsCard.setBackground(Color.WHITE);
        statsCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 226, 235), 1),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        statsCard.add(statsPanel, BorderLayout.CENTER);
        contentPanel.add(statsCard, BorderLayout.SOUTH);

        refreshStats(statsPanel);
    }

    private void addButton(JPanel panel, String text, ActionListener action) {
        var button = createActionButton(text, new Color(45, 115, 220), Color.WHITE);
        button.addActionListener(action);
        panel.add(button);
    }

    private JButton createActionButton(String text, Color background, Color foreground) {
        var button = new JButton(text);
        button.setBackground(background);
        button.setForeground(foreground);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(background.darker(), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        return button;
    }

    private JPanel createStatCard(String label, String value, Color accent) {
        var card = new JPanel(new BorderLayout(6, 6));
        card.setBackground(new Color(250, 252, 255));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 226, 235), 1),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));

        var labelComponent = new JLabel(label);
        labelComponent.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        labelComponent.setForeground(new Color(80, 89, 102));

        var valueComponent = new JLabel(value, SwingConstants.CENTER);
        valueComponent.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valueComponent.setForeground(accent);

        card.add(labelComponent, BorderLayout.NORTH);
        card.add(valueComponent, BorderLayout.CENTER);
        return card;
    }

    private void refreshStats(JPanel statsPanel) {
        int available = (int) vehicleService.listVehicles().stream().filter(v -> v.status().name().equalsIgnoreCase("AVAILABLE")).count();
        int rented = (int) vehicleService.listVehicles().stream().filter(v -> v.status().name().equalsIgnoreCase("RENTED")).count();
        int bookings = bookingService.listBookings().size();

        var count = 0;
        for (Component comp : statsPanel.getComponents()) {
            if (comp instanceof JPanel card) {
                var valueLabel = findValueLabel(card);
                if (valueLabel != null) {
                    switch (count) {
                        case 0 -> valueLabel.setText(String.valueOf(available));
                        case 1 -> valueLabel.setText(String.valueOf(rented));
                        case 2 -> valueLabel.setText(String.valueOf(bookings));
                    }
                }
                count++;
            }
        }
    }

    private JLabel findValueLabel(JPanel card) {
        for (Component inner : card.getComponents()) {
            if (inner instanceof JLabel label && label.getFont().getSize() == 28) {
                return label;
            }
        }
        return null;
    }

    private void showAddVehicleDialog() {
        var dialog = new JDialog(this, "Add Vehicle", true);
        dialog.setSize(380, 220);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridLayout(4, 2, 8, 8));

        dialog.add(new JLabel("Vehicle name:"));
        var nameField = new JTextField();
        dialog.add(nameField);

        dialog.add(new JLabel("Price per day:"));
        var priceField = new JTextField();
        dialog.add(priceField);

        var addButton = new JButton("Add");
        var cancelButton = new JButton("Cancel");
        dialog.add(addButton);
        dialog.add(cancelButton);

        cancelButton.addActionListener(e -> dialog.dispose());
        addButton.addActionListener(e -> {
            try {
                String name = nameField.getText().trim();
                double price = Double.parseDouble(priceField.getText().trim());
                vehicleService.addVehicle(name, price);
                showMessage(dialog, "Vehicle added successfully.");
                dialog.dispose();
            } catch (NumberFormatException ex) {
                showError(dialog, "Price must be a valid number.");
            } catch (IllegalArgumentException ex) {
                showError(dialog, ex.getMessage());
            } catch (RuntimeException ex) {
                showError(dialog, "Unable to add vehicle: " + ex.getMessage());
            }
        });

        dialog.setVisible(true);
    }

    private void showViewVehiclesDialog() {
        var dialog = new JDialog(this, "View Vehicles", true);
        dialog.setSize(700, 420);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(8, 8));

        var model = new DefaultTableModel(new String[]{"ID", "Name", "Price/Day", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        var table = new JTable(model);
        dialog.add(new JScrollPane(table), BorderLayout.CENTER);

        var topPanel = new JPanel();
        var refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loadVehicles(model));
        topPanel.add(refreshButton);
        dialog.add(topPanel, BorderLayout.NORTH);

        loadVehicles(model);
        dialog.setVisible(true);
    }

    private void loadVehicles(DefaultTableModel model) {
        model.setRowCount(0);
        for (Vehicle vehicle : vehicleService.listVehicles()) {
            model.addRow(new Object[]{vehicle.id(), vehicle.name(), vehicle.pricePerDay(), vehicle.status()});
        }
    }

    private void showViewBookingsDialog() {
        var dialog = new JDialog(this, "View Bookings", true);
        dialog.setSize(700, 420);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(8, 8));

        var model = new DefaultTableModel(new String[]{"Booking ID", "Vehicle ID", "Customer", "Days", "Total"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        var table = new JTable(model);
        dialog.add(new JScrollPane(table), BorderLayout.CENTER);

        var topPanel = new JPanel();
        var refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loadBookings(model));
        topPanel.add(refreshButton);
        dialog.add(topPanel, BorderLayout.NORTH);

        loadBookings(model);
        dialog.setVisible(true);
    }

    private void loadBookings(DefaultTableModel model) {
        model.setRowCount(0);
        for (Booking booking : bookingService.listBookings()) {
            model.addRow(new Object[]{booking.id(), booking.vehicleId(), booking.customerName(), booking.days(), booking.total()});
        }
    }

    private void showBookVehicleDialog() {
        var dialog = new JDialog(this, "Book Vehicle", true);
        dialog.setSize(420, 250);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridLayout(5, 2, 8, 8));

        dialog.add(new JLabel("Vehicle ID:"));
        var vehicleIdField = new JTextField();
        dialog.add(vehicleIdField);

        dialog.add(new JLabel("Customer name:"));
        var customerField = new JTextField();
        dialog.add(customerField);

        dialog.add(new JLabel("Number of days:"));
        var daysField = new JTextField();
        dialog.add(daysField);

        var bookButton = new JButton("Book");
        var cancelButton = new JButton("Cancel");
        dialog.add(bookButton);
        dialog.add(cancelButton);

        cancelButton.addActionListener(e -> dialog.dispose());
        bookButton.addActionListener(e -> {
            try {
                int vehicleId = Integer.parseInt(vehicleIdField.getText().trim());
                String customerName = customerField.getText().trim();
                int days = Integer.parseInt(daysField.getText().trim());
                bookingService.bookVehicle(vehicleId, customerName, days);
                showMessage(dialog, "Booking completed successfully.");
                dialog.dispose();
            } catch (NumberFormatException ex) {
                showError(dialog, "Vehicle ID and days must be integers.");
            } catch (IllegalArgumentException | IllegalStateException ex) {
                showError(dialog, ex.getMessage());
            } catch (RuntimeException ex) {
                showError(dialog, "Unable to book vehicle: " + ex.getMessage());
            }
        });

        dialog.setVisible(true);
    }

    private void showDeleteVehicleDialog() {
        var dialog = new JDialog(this, "Delete Vehicle", true);
        dialog.setSize(380, 180);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridLayout(3, 2, 8, 8));

        dialog.add(new JLabel("Vehicle ID to delete:"));
        var vehicleIdField = new JTextField();
        dialog.add(vehicleIdField);

        var deleteButton = new JButton("Delete");
        var cancelButton = new JButton("Cancel");
        dialog.add(deleteButton);
        dialog.add(cancelButton);

        cancelButton.addActionListener(e -> dialog.dispose());
        deleteButton.addActionListener(e -> {
            try {
                int vehicleId = Integer.parseInt(vehicleIdField.getText().trim());
                int confirm = JOptionPane.showConfirmDialog(dialog, "Delete vehicle ID " + vehicleId + "?", "Confirm deletion", JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }
                vehicleService.deleteVehicle(vehicleId);
                showMessage(dialog, "Vehicle deleted successfully.");
                dialog.dispose();
            } catch (NumberFormatException ex) {
                showError(dialog, "Vehicle ID must be an integer.");
            } catch (IllegalArgumentException | IllegalStateException ex) {
                showError(dialog, ex.getMessage());
            } catch (RuntimeException ex) {
                showError(dialog, "Unable to delete vehicle: " + ex.getMessage());
            }
        });

        dialog.setVisible(true);
    }

    private void showReturnVehicleDialog() {
        var dialog = new JDialog(this, "Return Vehicle", true);
        dialog.setSize(420, 220);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridLayout(4, 2, 8, 8));

        dialog.add(new JLabel("Vehicle ID:"));
        var vehicleIdField = new JTextField();
        dialog.add(vehicleIdField);

        dialog.add(new JLabel("Customer name:"));
        var customerField = new JTextField();
        dialog.add(customerField);

        var returnButton = new JButton("Return");
        var cancelButton = new JButton("Cancel");
        dialog.add(returnButton);
        dialog.add(cancelButton);

        cancelButton.addActionListener(e -> dialog.dispose());
        returnButton.addActionListener(e -> {
            try {
                int vehicleId = Integer.parseInt(vehicleIdField.getText().trim());
                String customerName = customerField.getText().trim();
                bookingService.returnVehicle(vehicleId, customerName);
                showMessage(dialog, "Vehicle returned successfully.");
                dialog.dispose();
            } catch (NumberFormatException ex) {
                showError(dialog, "Vehicle ID must be an integer.");
            } catch (IllegalArgumentException | IllegalStateException ex) {
                showError(dialog, ex.getMessage());
            } catch (RuntimeException ex) {
                showError(dialog, "Unable to return vehicle: " + ex.getMessage());
            }
        });

        dialog.setVisible(true);
    }

    private void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showMessage(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    private void initLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ignored) {
            // fallback to default
        }
    }
}
