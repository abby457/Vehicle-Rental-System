// VehicleRentalAppFull.java
// Single-file Swing app for Vehicle Rental with SQLite
// Two roles: admin and customer, with order approval workflow.
//
// Compile:  javac -cp .:sqlite-jdbc-3.50.3.0.jar VehicleRentalAppFull.java
// Run:      java  -cp .:sqlite-jdbc-3.50.3.0.jar VehicleRentalAppFull
// (Use ';' instead of ':' on Windows classpath)

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class VehicleRentalAppFull {

    // ─────────────────────────────────────────────────────────────────
    //  SHARED DB HELPERS
    // ─────────────────────────────────────────────────────────────────
    static final String DB_URL = "jdbc:sqlite:vehiclerental.db";

    static Connection connect() throws SQLException, ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        return DriverManager.getConnection(DB_URL);
    }

    static void showError(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    static void showInfo(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Ensure all required tables exist and insert seed data once. */
    static void ensureTablesExist() {
        String[] ddl = {
            // vehicles
            "CREATE TABLE IF NOT EXISTS vehicles (" +
            "  id INTEGER PRIMARY KEY," +
            "  name TEXT NOT NULL," +
            "  price_per_day REAL NOT NULL," +
            "  status TEXT NOT NULL DEFAULT 'Available'" +
            ");",

            // users: role = 'admin' | 'customer'
            "CREATE TABLE IF NOT EXISTS users (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  username TEXT NOT NULL UNIQUE," +
            "  password TEXT NOT NULL," +
            "  role TEXT NOT NULL" +
            ");",

            // orders placed by customers (admin approves/rejects)
            "CREATE TABLE IF NOT EXISTS orders (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  vehicle_id INTEGER NOT NULL," +
            "  customer_id INTEGER NOT NULL," +
            "  customer_name TEXT NOT NULL," +
            "  days INTEGER NOT NULL," +
            "  total REAL NOT NULL," +
            "  status TEXT NOT NULL DEFAULT 'Pending'," +
            "  FOREIGN KEY(vehicle_id) REFERENCES vehicles(id)," +
            "  FOREIGN KEY(customer_id) REFERENCES users(id)" +
            ");",

            // confirmed bookings (created when admin approves an order)
            "CREATE TABLE IF NOT EXISTS bookings (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  vehicle_id INTEGER NOT NULL," +
            "  customer_name TEXT NOT NULL," +
            "  days INTEGER NOT NULL," +
            "  total REAL NOT NULL," +
            "  FOREIGN KEY(vehicle_id) REFERENCES vehicles(id)" +
            ");",

            // notifications pushed to customers
            "CREATE TABLE IF NOT EXISTS notifications (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  customer_id INTEGER NOT NULL," +
            "  message TEXT NOT NULL," +
            "  is_read INTEGER NOT NULL DEFAULT 0," +
            "  created_at TEXT NOT NULL," +
            "  FOREIGN KEY(customer_id) REFERENCES users(id)" +
            ");",

            // password reset requests from customers
            "CREATE TABLE IF NOT EXISTS pwd_reset_requests (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  username TEXT NOT NULL," +
            "  status TEXT NOT NULL DEFAULT 'Pending'," +
            "  created_at TEXT NOT NULL" +
            ");"
        };

        try (Connection conn = connect(); Statement st = conn.createStatement()) {
            for (String sql : ddl) st.execute(sql);

            // Seed default accounts (only if users table is empty)
            ResultSet rs = st.executeQuery("SELECT COUNT(*) AS cnt FROM users");
            if (rs.next() && rs.getInt("cnt") == 0) {
                st.execute("INSERT INTO users(username,password,role) VALUES ('admin','admin123','admin')");
                st.execute("INSERT INTO users(username,password,role) VALUES ('customer','cust123','customer')");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                "DB init error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  SHARED UI STYLE HELPERS
    // ─────────────────────────────────────────────────────────────────
    static final Color CLR_BG        = new Color(18, 22, 36);
    static final Color CLR_PANEL     = new Color(28, 34, 54);
    static final Color CLR_CARD      = new Color(36, 44, 68);
    static final Color CLR_ACCENT    = new Color(79, 140, 255);
    static final Color CLR_ACCENT2   = new Color(118, 75, 226);
    static final Color CLR_SUCCESS   = new Color(52, 199, 130);
    static final Color CLR_DANGER    = new Color(255, 80, 80);
    static final Color CLR_TEXT      = new Color(220, 228, 255);
    static final Color CLR_SUBTEXT   = new Color(140, 155, 200);
    static final Color CLR_TABLE_HDR = new Color(46, 56, 88);
    static final Font  FONT_TITLE    = new Font("SansSerif", Font.BOLD, 22);
    static final Font  FONT_SUB      = new Font("SansSerif", Font.PLAIN, 13);
    static final Font  FONT_BTN      = new Font("SansSerif", Font.BOLD, 13);
    static final Font  FONT_LBL      = new Font("SansSerif", Font.BOLD, 12);
    static final Font  FONT_FIELD    = new Font("SansSerif", Font.PLAIN, 13);

    static void styleFrame(JFrame f) {
        f.getContentPane().setBackground(CLR_BG);
    }

    static JButton primaryButton(String text, Color bg) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, bg.brighter(), getWidth(), getHeight(), bg.darker());
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(getForeground());
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        b.setFont(FONT_BTN);
        b.setForeground(Color.WHITE);
        b.setBackground(bg);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(200, 44));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setForeground(new Color(255,255,180)); }
            @Override public void mouseExited(MouseEvent e)  { b.setForeground(Color.WHITE); }
        });
        return b;
    }

    static JTextField styledField() {
        JTextField f = new JTextField();
        f.setFont(FONT_FIELD);
        f.setBackground(CLR_BG);
        f.setForeground(CLR_TEXT);
        f.setCaretColor(CLR_TEXT);
        f.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(8, CLR_ACCENT),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        return f;
    }

    static JPasswordField styledPwdField() {
        JPasswordField f = new JPasswordField();
        f.setFont(FONT_FIELD);
        f.setBackground(CLR_BG);
        f.setForeground(CLR_TEXT);
        f.setCaretColor(CLR_TEXT);
        f.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(8, CLR_ACCENT),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        return f;
    }

    static JLabel styledLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_LBL);
        l.setForeground(CLR_SUBTEXT);
        return l;
    }

    static void styleTable(JTable t) {
        t.setBackground(CLR_CARD);
        t.setForeground(CLR_TEXT);
        t.setFont(FONT_SUB);
        t.setGridColor(new Color(50, 60, 90));
        t.setRowHeight(28);
        t.setSelectionBackground(CLR_ACCENT);
        t.setSelectionForeground(Color.WHITE);
        t.getTableHeader().setBackground(CLR_TABLE_HDR);
        t.getTableHeader().setForeground(CLR_TEXT);
        t.getTableHeader().setFont(FONT_LBL);
        t.getTableHeader().setBorder(BorderFactory.createEmptyBorder());
    }

    static JScrollPane styledScroll(JTable t) {
        JScrollPane sp = new JScrollPane(t);
        sp.setBackground(CLR_CARD);
        sp.getViewport().setBackground(CLR_CARD);
        sp.setBorder(new RoundedBorder(10, CLR_ACCENT));
        return sp;
    }

    // ─────────────────────────────────────────────────────────────────
    //  ROUNDED BORDER UTILITY
    // ─────────────────────────────────────────────────────────────────
    static class RoundedBorder extends AbstractBorder {
        private final int radius;
        private final Color color;
        RoundedBorder(int radius, Color color) { this.radius = radius; this.color = color; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.drawRoundRect(x, y, w - 1, h - 1, radius, radius);
            g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) { return new Insets(4, 8, 4, 8); }
        @Override public Insets getBorderInsets(Component c, Insets i) {
            i.set(4, 8, 4, 8); return i;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  LOGIN FRAME  (entry point)
    // ─────────────────────────────────────────────────────────────────
    static class LoginFrame extends JFrame {

        LoginFrame() {
            super("Vehicle Rental System — Login");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(420, 460);
            setLocationRelativeTo(null);
            setResizable(false);
            styleFrame(this);
            buildUI();
        }

        private void buildUI() {
            JPanel root = new JPanel(new GridBagLayout());
            root.setBackground(CLR_BG);
            root.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

            GridBagConstraints gc = new GridBagConstraints();
            gc.insets = new Insets(8, 0, 8, 0);
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.gridx = 0; gc.weightx = 1;

            // ── Logo / Title ──
            JLabel icon = new JLabel("🚗", SwingConstants.CENTER);
            icon.setFont(new Font("SansSerif", Font.PLAIN, 52));
            gc.gridy = 0; root.add(icon, gc);

            JLabel title = new JLabel("Vehicle Rental System", SwingConstants.CENTER);
            title.setFont(FONT_TITLE);
            title.setForeground(CLR_TEXT);
            gc.gridy = 1; root.add(title, gc);

            JLabel sub = new JLabel("Sign in to continue", SwingConstants.CENTER);
            sub.setFont(FONT_SUB);
            sub.setForeground(CLR_SUBTEXT);
            gc.gridy = 2; root.add(sub, gc);

            // ── Divider ──
            JSeparator sep = new JSeparator();
            sep.setForeground(CLR_CARD);
            gc.gridy = 3; root.add(sep, gc);

            // ── Fields ──
            JPanel form = new JPanel(new GridLayout(4, 1, 0, 8));
            form.setBackground(CLR_BG);

            form.add(styledLabel("Username"));
            JTextField fldUser = styledField();
            form.add(fldUser);
            form.add(styledLabel("Password"));
            JPasswordField fldPass = styledPwdField();
            form.add(fldPass);
            gc.gridy = 4; gc.insets = new Insets(16, 0, 8, 0);
            root.add(form, gc);

            // ── Login Button ──
            JButton btnLogin = primaryButton("Login", CLR_ACCENT);
            btnLogin.setPreferredSize(new Dimension(300, 46));
            gc.gridy = 5; gc.insets = new Insets(12, 0, 0, 0);
            root.add(btnLogin, gc);

            // ── Forgot Password link ──
            JLabel forgotLbl = new JLabel("<html><center><a href='' style='color:#4f8cff;'>Forgot your password?</a></center></html>",
                SwingConstants.CENTER);
            forgotLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
            forgotLbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            gc.gridy = 6; gc.insets = new Insets(10, 0, 0, 0);
            root.add(forgotLbl, gc);

            forgotLbl.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    String uname = fldUser.getText().trim();
                    if (uname.isEmpty()) {
                        showError(LoginFrame.this, "Enter your username first, then click Forgot Password.");
                        return;
                    }
                    // Check username exists
                    try (Connection conn = connect();
                         PreparedStatement ps = conn.prepareStatement("SELECT id FROM users WHERE username=?")) {
                        ps.setString(1, uname);
                        ResultSet rs = ps.executeQuery();
                        if (!rs.next()) { showError(LoginFrame.this, "Username '" + uname + "' not found."); return; }
                    } catch (Exception ex) { showError(LoginFrame.this, "DB error: " + ex.getMessage()); return; }
                    // Insert reset request
                    String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    try (Connection conn = connect();
                         PreparedStatement ps = conn.prepareStatement(
                             "INSERT INTO pwd_reset_requests(username,status,created_at) VALUES(?,'Pending',?)")) {
                        ps.setString(1, uname); ps.setString(2, now); ps.executeUpdate();
                        showInfo(LoginFrame.this,
                            "Password reset request sent! ✅\nAn admin will update your password shortly.\nPlease try logging in again later.");
                    } catch (Exception ex) { showError(LoginFrame.this, "DB error: " + ex.getMessage()); }
                }
            });

            // ── Hint ──
            JLabel hint = new JLabel(
                "<html><center><font color='#8c9bc8'>Default: admin/admin123 &nbsp;·&nbsp; customer/cust123</font></center></html>",
                SwingConstants.CENTER);
            hint.setFont(new Font("SansSerif", Font.PLAIN, 11));
            gc.gridy = 7; gc.insets = new Insets(10, 0, 0, 0);
            root.add(hint, gc);

            add(root);

            // ── Action ──
            ActionListener doLogin = e -> attemptLogin(fldUser.getText().trim(), new String(fldPass.getPassword()));
            btnLogin.addActionListener(doLogin);
            fldPass.addActionListener(doLogin);
            fldUser.addActionListener(doLogin);
        }

        private void attemptLogin(String username, String password) {
            if (username.isEmpty() || password.isEmpty()) {
                showError(this, "Please enter username and password.");
                return;
            }
            try (Connection conn = connect();
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, role FROM users WHERE username=? AND password=?")) {
                ps.setString(1, username);
                ps.setString(2, password);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        showError(this, "Invalid username or password.");
                        return;
                    }
                    int userId   = rs.getInt("id");
                    String role  = rs.getString("role");
                    dispose();
                    if ("admin".equals(role)) {
                        new AdminDashboard(username).setVisible(true);
                    } else {
                        new CustomerDashboard(userId, username).setVisible(true);
                    }
                }
            } catch (Exception ex) {
                showError(this, "Login error: " + ex.getMessage());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  ADMIN DASHBOARD
    // ─────────────────────────────────────────────────────────────────
    static class AdminDashboard extends JFrame {

        private final String adminName;

        AdminDashboard(String adminName) {
            super("Vehicle Rental — Admin Dashboard");
            this.adminName = adminName;
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(620, 640);
            setLocationRelativeTo(null);
            styleFrame(this);
            buildUI();
        }

        private void buildUI() {
            JPanel root = new JPanel(new BorderLayout(16, 16));
            root.setBackground(CLR_BG);
            root.setBorder(BorderFactory.createEmptyBorder(24, 30, 24, 30));

            // ── Header ──
            JPanel header = new JPanel(new BorderLayout());
            header.setBackground(CLR_BG);

            JLabel welcome = new JLabel("Welcome, " + adminName + "  👋");
            welcome.setFont(new Font("SansSerif", Font.BOLD, 18));
            welcome.setForeground(CLR_TEXT);
            header.add(welcome, BorderLayout.WEST);

            JLabel roleTag = new JLabel("  ADMIN");
            roleTag.setFont(new Font("SansSerif", Font.BOLD, 11));
            roleTag.setForeground(Color.WHITE);
            roleTag.setOpaque(true);
            roleTag.setBackground(CLR_ACCENT2);
            roleTag.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
            header.add(roleTag, BorderLayout.EAST);

            root.add(header, BorderLayout.NORTH);

            // ── Button Grid (4×2) ──
            JPanel grid = new JPanel(new GridLayout(4, 2, 16, 16));
            grid.setBackground(CLR_BG);

            grid.add(dashCard("➕  Add Vehicle",        CLR_ACCENT,                    e -> showAddVehicleDialog()));
            grid.add(dashCard("📋  View Vehicles",      CLR_ACCENT,                    e -> showViewVehiclesDialog()));
            grid.add(dashCard("🗑  Delete Vehicle",     CLR_DANGER,                    e -> showDeleteVehicleDialog()));
            grid.add(dashCard("↩️  Mark Returned",     CLR_SUCCESS,                   e -> showReturnVehicleDialog()));
            grid.add(dashCard("📦  View Orders",        CLR_ACCENT2,                   e -> showViewOrdersDialog()));
            grid.add(dashCard("✅  Approve / Reject",   CLR_ACCENT2,                   e -> showApproveRejectDialog()));
            grid.add(dashCard("👤  Manage Users",       new Color(255, 160, 50),       e -> showManageUsersDialog()));
            grid.add(dashCard("🔑  Password Requests",  new Color(200, 100, 255),      e -> showPasswordRequestsDialog()));

            root.add(grid, BorderLayout.CENTER);

            // ── Footer ──
            JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            footer.setBackground(CLR_BG);
            JButton btnLogout = primaryButton("Logout", CLR_DANGER);
            btnLogout.setPreferredSize(new Dimension(110, 36));
            btnLogout.addActionListener(e -> { dispose(); new LoginFrame().setVisible(true); });
            footer.add(btnLogout);
            root.add(footer, BorderLayout.SOUTH);

            add(root);
        }

        private JPanel dashCard(String label, Color accent, ActionListener al) {
            JPanel card = new JPanel(new BorderLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(CLR_CARD);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                    g2.setColor(accent);
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 16, 16);
                    g2.dispose();
                }
            };
            card.setOpaque(false);
            card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

            JLabel lbl = new JLabel(label, SwingConstants.CENTER);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 14));
            lbl.setForeground(CLR_TEXT);
            card.add(lbl, BorderLayout.CENTER);

            card.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { al.actionPerformed(null); }
                @Override public void mouseEntered(MouseEvent e) { lbl.setForeground(accent); repaint(); }
                @Override public void mouseExited(MouseEvent e)  { lbl.setForeground(CLR_TEXT); repaint(); }
            });
            return card;
        }

        // ── Add Vehicle ──
        private void showAddVehicleDialog() {
            JDialog d = styledDialog(this, "Add Vehicle", 400, 280);
            JPanel p = formPanel();

            p.add(styledLabel("Vehicle ID (integer):"));
            JTextField fldId = styledField(); p.add(fldId);
            p.add(styledLabel("Vehicle Name:"));
            JTextField fldName = styledField(); p.add(fldName);
            p.add(styledLabel("Price per Day ($):"));
            JTextField fldPrice = styledField(); p.add(fldPrice);

            JPanel btns = btnRow(
                primaryButton("Add", CLR_SUCCESS),
                primaryButton("Cancel", CLR_DANGER)
            );
            ((JButton)btns.getComponent(0)).addActionListener(e -> {
                String sId = fldId.getText().trim(), name = fldName.getText().trim(), sPrice = fldPrice.getText().trim();
                if (sId.isEmpty() || name.isEmpty() || sPrice.isEmpty()) { showError(d,"All fields required."); return; }
                int id; double price;
                try { id = Integer.parseInt(sId); } catch (NumberFormatException ex) { showError(d,"ID must be integer."); return; }
                try { price = Double.parseDouble(sPrice); } catch (NumberFormatException ex) { showError(d,"Price must be a number."); return; }
                try (Connection conn = connect();
                     PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO vehicles(id,name,price_per_day,status) VALUES(?,?,?,'Available')")) {
                    ps.setInt(1,id); ps.setString(2,name); ps.setDouble(3,price);
                    ps.executeUpdate();
                    showInfo(d,"Vehicle added successfully!"); d.dispose();
                } catch (Exception ex) { showError(d,"DB error: "+ex.getMessage()); }
            });
            ((JButton)btns.getComponent(1)).addActionListener(e -> d.dispose());

            d.setLayout(new BorderLayout(12,12));
            d.add(p, BorderLayout.CENTER);
            d.add(btns, BorderLayout.SOUTH);
            d.setVisible(true);
        }

        // ── View Vehicles ──
        private void showViewVehiclesDialog() {
            JDialog d = styledDialog(this, "All Vehicles", 720, 440);
            DefaultTableModel model = new DefaultTableModel(new String[]{"ID","Name","Price/Day","Status"},0){
                @Override public boolean isCellEditable(int r,int c){return false;}
            };
            JTable table = new JTable(model); styleTable(table);
            colorStatusColumn(table, 3);
            JScrollPane sp = styledScroll(table);

            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
            top.setBackground(CLR_BG);
            JButton btnRefresh = primaryButton("↻ Refresh", CLR_ACCENT);
            btnRefresh.setPreferredSize(new Dimension(120, 36));
            top.add(btnRefresh);
            btnRefresh.addActionListener(e -> loadAllVehicles(model));
            loadAllVehicles(model);

            d.setLayout(new BorderLayout(8,8));
            d.add(top, BorderLayout.NORTH);
            d.add(sp, BorderLayout.CENTER);
            d.setVisible(true);
        }

        private void loadAllVehicles(DefaultTableModel m) {
            m.setRowCount(0);
            try (Connection conn = connect();
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT id,name,price_per_day,status FROM vehicles ORDER BY id");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    m.addRow(new Object[]{rs.getInt(1),rs.getString(2),
                        String.format("$%.2f",rs.getDouble(3)),rs.getString(4)});
            } catch (Exception ex) { showError(this,"DB error: "+ex.getMessage()); }
        }

        // ── Delete Vehicle ──
        private void showDeleteVehicleDialog() {
            JDialog d = styledDialog(this, "Delete Vehicle", 380, 200);
            JPanel p = formPanel();
            p.add(styledLabel("Vehicle ID to delete:"));
            JTextField fldId = styledField(); p.add(fldId);

            JPanel btns = btnRow(
                primaryButton("Delete", CLR_DANGER),
                primaryButton("Cancel", CLR_ACCENT)
            );
            ((JButton)btns.getComponent(0)).addActionListener(e -> {
                String sId = fldId.getText().trim();
                if (sId.isEmpty()) { showError(d,"Enter vehicle ID."); return; }
                int id;
                try { id = Integer.parseInt(sId); } catch (NumberFormatException ex) { showError(d,"ID must be integer."); return; }
                int ok = JOptionPane.showConfirmDialog(d,"Delete vehicle ID "+id+"? Existing bookings will remain.","Confirm",JOptionPane.YES_NO_OPTION);
                if (ok != JOptionPane.YES_OPTION) return;
                try (Connection conn = connect();
                     PreparedStatement ps = conn.prepareStatement("DELETE FROM vehicles WHERE id=?")) {
                    ps.setInt(1,id);
                    int rows = ps.executeUpdate();
                    if (rows > 0) { showInfo(d,"Vehicle deleted."); d.dispose(); }
                    else showError(d,"No vehicle with ID "+id);
                } catch (Exception ex) { showError(d,"DB error: "+ex.getMessage()); }
            });
            ((JButton)btns.getComponent(1)).addActionListener(e -> d.dispose());

            d.setLayout(new BorderLayout(12,12));
            d.add(p, BorderLayout.CENTER);
            d.add(btns, BorderLayout.SOUTH);
            d.setVisible(true);
        }

        // ── Mark Returned ──
        private void showReturnVehicleDialog() {
            JDialog d = styledDialog(this, "Mark Vehicle Returned", 400, 220);
            JPanel p = formPanel();
            p.add(styledLabel("Booked Vehicle ID:"));
            JTextField fldVid = styledField(); p.add(fldVid);
            p.add(styledLabel("Customer Name (as on booking):"));
            JTextField fldCust = styledField(); p.add(fldCust);

            JPanel btns = btnRow(
                primaryButton("Mark Returned", CLR_SUCCESS),
                primaryButton("Cancel", CLR_DANGER)
            );
            ((JButton)btns.getComponent(0)).addActionListener(e -> {
                String sVid = fldVid.getText().trim(), cust = fldCust.getText().trim();
                if (sVid.isEmpty()||cust.isEmpty()) { showError(d,"Both fields required."); return; }
                int vid;
                try { vid = Integer.parseInt(sVid); } catch (NumberFormatException ex) { showError(d,"Vehicle ID must be integer."); return; }
                try (Connection conn = connect()) {
                    PreparedStatement find = conn.prepareStatement(
                        "SELECT id FROM bookings WHERE vehicle_id=? AND customer_name=? LIMIT 1");
                    find.setInt(1,vid); find.setString(2,cust);
                    ResultSet rs = find.executeQuery();
                    if (!rs.next()) { showError(d,"No matching booking found."); return; }
                    int bid = rs.getInt("id");
                    conn.setAutoCommit(false);
                    try {
                        conn.prepareStatement("DELETE FROM bookings WHERE id="+bid).executeUpdate();
                        conn.prepareStatement("UPDATE vehicles SET status='Available' WHERE id="+vid).executeUpdate();
                        conn.commit();
                        showInfo(d,"Vehicle marked as returned. Booking removed."); d.dispose();
                    } catch (SQLException ex) { conn.rollback(); throw ex; }
                    finally { conn.setAutoCommit(true); }
                } catch (Exception ex) { showError(d,"DB error: "+ex.getMessage()); }
            });
            ((JButton)btns.getComponent(1)).addActionListener(e -> d.dispose());

            d.setLayout(new BorderLayout(12,12));
            d.add(p, BorderLayout.CENTER);
            d.add(btns, BorderLayout.SOUTH);
            d.setVisible(true);
        }

        // ── View Orders ──
        private void showViewOrdersDialog() {
            JDialog d = styledDialog(this, "All Orders", 820, 460);
            DefaultTableModel model = new DefaultTableModel(
                new String[]{"Order ID","Vehicle ID","Vehicle Name","Customer","Days","Total ($)","Status"},0){
                @Override public boolean isCellEditable(int r,int c){return false;}
            };
            JTable table = new JTable(model); styleTable(table);
            colorStatusColumn(table, 6);
            JScrollPane sp = styledScroll(table);

            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
            top.setBackground(CLR_BG);
            JButton btnRefresh = primaryButton("↻ Refresh", CLR_ACCENT);
            btnRefresh.setPreferredSize(new Dimension(120, 36));
            top.add(btnRefresh);
            btnRefresh.addActionListener(e -> loadAllOrders(model));
            loadAllOrders(model);

            d.setLayout(new BorderLayout(8,8));
            d.add(top, BorderLayout.NORTH);
            d.add(sp, BorderLayout.CENTER);
            d.setVisible(true);
        }

        private void loadAllOrders(DefaultTableModel m) {
            m.setRowCount(0);
            String sql = "SELECT o.id,o.vehicle_id,v.name,o.customer_name,o.days,o.total,o.status " +
                         "FROM orders o LEFT JOIN vehicles v ON o.vehicle_id=v.id ORDER BY o.id DESC";
            try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    m.addRow(new Object[]{rs.getInt(1),rs.getInt(2),rs.getString(3),
                        rs.getString(4),rs.getInt(5),String.format("$%.2f",rs.getDouble(6)),rs.getString(7)});
            } catch (Exception ex) { showError(this,"DB error: "+ex.getMessage()); }
        }

        // ── Approve / Reject ──
        private void showApproveRejectDialog() {
            JDialog d = styledDialog(this, "Pending Orders — Click a row to Approve / Reject", 880, 480);
            d.setLayout(new BorderLayout(8, 8));

            // ── Pending orders table ──
            DefaultTableModel model = new DefaultTableModel(
                new String[]{"Order ID", "Vehicle ID", "Vehicle Name", "Customer", "Days", "Total ($)"}, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            JTable table = new JTable(model); styleTable(table);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            Runnable loadPending = () -> {
                model.setRowCount(0);
                String sql = "SELECT o.id,o.vehicle_id,v.name,o.customer_name,o.days,o.total " +
                             "FROM orders o LEFT JOIN vehicles v ON o.vehicle_id=v.id " +
                             "WHERE o.status='Pending' ORDER BY o.id";
                try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next())
                        model.addRow(new Object[]{
                            rs.getInt(1), rs.getInt(2), rs.getString(3),
                            rs.getString(4), rs.getInt(5),
                            String.format("$%.2f", rs.getDouble(6))
                        });
                } catch (Exception ex) { showError(d, "DB error: " + ex.getMessage()); }
            };
            loadPending.run();

            // ── Top bar ──
            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
            top.setBackground(CLR_BG);
            JButton btnRefresh = primaryButton("↻ Refresh", CLR_ACCENT);
            btnRefresh.setPreferredSize(new Dimension(120, 34));
            btnRefresh.addActionListener(e -> loadPending.run());
            top.add(btnRefresh);
            JLabel hint = new JLabel("  Click any row to approve or reject that order");
            hint.setFont(FONT_SUB); hint.setForeground(CLR_SUBTEXT);
            top.add(hint);

            // ── Row click → popup ──
            table.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    int row = table.getSelectedRow();
                    if (row < 0) return;

                    int    orderId   = (int)    model.getValueAt(row, 0);
                    int    vehicleId = (int)    model.getValueAt(row, 1);
                    String vehName   = (String) model.getValueAt(row, 2);
                    String customer  = (String) model.getValueAt(row, 3);
                    int    days      = (int)    model.getValueAt(row, 4);
                    String total     = (String) model.getValueAt(row, 5);

                    // ── Popup dialog ──
                    JDialog popup = new JDialog(d, "Order #" + orderId, true);
                    popup.setSize(400, 260);
                    popup.setLocationRelativeTo(d);
                    popup.getContentPane().setBackground(CLR_BG);

                    JPanel info = new JPanel(new GridLayout(5, 1, 0, 8));
                    info.setBackground(CLR_BG);
                    info.setBorder(BorderFactory.createEmptyBorder(20, 28, 12, 28));

                    JLabel lTitle = new JLabel("Order #" + orderId + " \u2014 Action Required", SwingConstants.CENTER);
                    lTitle.setFont(new Font("SansSerif", Font.BOLD, 15));
                    lTitle.setForeground(CLR_TEXT);

                    JLabel lVeh  = new JLabel("\ud83d\ude97  Vehicle:   " + vehName + "  (ID " + vehicleId + ")");
                    lVeh.setFont(FONT_SUB); lVeh.setForeground(CLR_SUBTEXT);
                    JLabel lCust = new JLabel("\ud83d\udc64  Customer: " + customer);
                    lCust.setFont(FONT_SUB); lCust.setForeground(CLR_SUBTEXT);
                    JLabel lDays = new JLabel("\ud83d\udcc5  Days: " + days + "   |   \ud83d\udcb0 Total: " + total);
                    lDays.setFont(FONT_SUB); lDays.setForeground(CLR_SUBTEXT);

                    info.add(lTitle);
                    info.add(lVeh);
                    info.add(lCust);
                    info.add(lDays);
                    info.add(new JLabel(" "));

                    JButton btnApprove = primaryButton("\u2705  Approve", CLR_SUCCESS);
                    JButton btnReject  = primaryButton("\u274c  Reject",  CLR_DANGER);
                    JButton btnCancel  = primaryButton("Cancel",     new Color(70, 80, 110));
                    btnApprove.setPreferredSize(new Dimension(140, 40));
                    btnReject .setPreferredSize(new Dimension(140, 40));
                    btnCancel .setPreferredSize(new Dimension(100, 40));

                    btnCancel .addActionListener(ev -> popup.dispose());
                    btnApprove.addActionListener(ev -> {
                        processOrder(popup, String.valueOf(orderId), true);
                        loadPending.run();
                    });
                    btnReject .addActionListener(ev -> {
                        processOrder(popup, String.valueOf(orderId), false);
                        loadPending.run();
                    });

                    JPanel popupBtns = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
                    popupBtns.setBackground(CLR_BG);
                    popupBtns.add(btnApprove); popupBtns.add(btnReject); popupBtns.add(btnCancel);

                    popup.setLayout(new BorderLayout());
                    popup.add(info, BorderLayout.CENTER);
                    popup.add(popupBtns, BorderLayout.SOUTH);
                    popup.setVisible(true);
                }
            });

            d.add(top, BorderLayout.NORTH);
            d.add(styledScroll(table), BorderLayout.CENTER);
            d.setVisible(true);
        }

        private void processOrder(JDialog d, String sOrderId, boolean approve) {
            if (sOrderId.isEmpty()) { showError(d,"Enter order ID."); return; }
            int orderId;
            try { orderId = Integer.parseInt(sOrderId); } catch (NumberFormatException ex) { showError(d,"Order ID must be integer."); return; }

            try (Connection conn = connect()) {
                // Fetch order
                PreparedStatement fetch = conn.prepareStatement(
                    "SELECT o.vehicle_id,o.customer_id,o.customer_name,o.days,o.total,o.status,v.price_per_day " +
                    "FROM orders o LEFT JOIN vehicles v ON o.vehicle_id=v.id WHERE o.id=?");
                fetch.setInt(1, orderId);
                ResultSet rs = fetch.executeQuery();
                if (!rs.next()) { showError(d,"Order #"+orderId+" not found."); return; }

                String curStatus = rs.getString("status");
                if (!"Pending".equals(curStatus)) {
                    showError(d,"Order #"+orderId+" is already "+curStatus+"."); return;
                }

                int vehicleId   = rs.getInt("vehicle_id");
                int customerId  = rs.getInt("customer_id");
                String custName = rs.getString("customer_name");
                int days        = rs.getInt("days");
                double total    = rs.getDouble("total");

                conn.setAutoCommit(false);
                try {
                    String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    if (approve) {
                        // Check vehicle still available
                        PreparedStatement chk = conn.prepareStatement("SELECT status FROM vehicles WHERE id=?");
                        chk.setInt(1,vehicleId);
                        ResultSet vs = chk.executeQuery();
                        if (vs.next() && "Booked".equalsIgnoreCase(vs.getString(1))) {
                            conn.rollback(); showError(d,"Vehicle is already booked by someone else."); return;
                        }
                        // Create booking
                        PreparedStatement ins = conn.prepareStatement(
                            "INSERT INTO bookings(vehicle_id,customer_name,days,total) VALUES(?,?,?,?)");
                        ins.setInt(1,vehicleId); ins.setString(2,custName); ins.setInt(3,days); ins.setDouble(4,total);
                        ins.executeUpdate();
                        // Set vehicle booked
                        conn.prepareStatement("UPDATE vehicles SET status='Booked' WHERE id="+vehicleId).executeUpdate();
                        // Update order status
                        conn.prepareStatement("UPDATE orders SET status='Approved' WHERE id="+orderId).executeUpdate();
                        // Notify customer
                        PreparedStatement notif = conn.prepareStatement(
                            "INSERT INTO notifications(customer_id,message,is_read,created_at) VALUES(?,?,0,?)");
                        notif.setInt(1,customerId);
                        notif.setString(2,"✅ Your order #"+orderId+" for Vehicle ID "+vehicleId+" has been APPROVED! Total: $"+String.format("%.2f",total)+" ("+days+" days).");
                        notif.setString(3,now); notif.executeUpdate();
                        conn.commit();
                        showInfo(d,"Order #"+orderId+" APPROVED. Booking created and customer notified.");
                    } else {
                        conn.prepareStatement("UPDATE orders SET status='Rejected' WHERE id="+orderId).executeUpdate();
                        PreparedStatement notif = conn.prepareStatement(
                            "INSERT INTO notifications(customer_id,message,is_read,created_at) VALUES(?,?,0,?)");
                        notif.setInt(1,customerId);
                        notif.setString(2,"❌ Your order #"+orderId+" for Vehicle ID "+vehicleId+" has been REJECTED. Please choose another vehicle or contact support.");
                        notif.setString(3,now); notif.executeUpdate();
                        conn.commit();
                        showInfo(d,"Order #"+orderId+" REJECTED. Customer notified.");
                    }
                    d.dispose();
                } catch (SQLException ex) { conn.rollback(); throw ex; }
                finally { conn.setAutoCommit(true); }

            } catch (Exception ex) { showError(d,"DB error: "+ex.getMessage()); }
        }

        // ── Manage Users ──
        private void showManageUsersDialog() {
            JDialog d = styledDialog(this, "Manage Users", 750, 500);
            d.setLayout(new BorderLayout(8, 8));

            // Tab-style toggle
            JPanel tabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            tabBar.setBackground(CLR_BG);
            JButton tabList = primaryButton("All Users", CLR_ACCENT);
            JButton tabAdd  = primaryButton("+ Add New User", new Color(255, 160, 50));
            tabList.setPreferredSize(new Dimension(140, 34));
            tabAdd.setPreferredSize(new Dimension(160, 34));
            tabBar.add(tabList); tabBar.add(tabAdd);
            d.add(tabBar, BorderLayout.NORTH);

            // ─ Panel A: list users ─
            DefaultTableModel usersModel = new DefaultTableModel(
                new String[]{"ID", "Username", "Role"}, 0){
                @Override public boolean isCellEditable(int r, int c){ return false; }
            };
            JTable usersTable = new JTable(usersModel); styleTable(usersTable);
            JPanel listPanel = new JPanel(new BorderLayout(8, 8));
            listPanel.setBackground(CLR_BG);
            listPanel.add(styledScroll(usersTable), BorderLayout.CENTER);

            Runnable loadUsers = () -> {
                usersModel.setRowCount(0);
                try (Connection conn = connect();
                     PreparedStatement ps = conn.prepareStatement("SELECT id,username,role FROM users ORDER BY id");
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) usersModel.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3)});
                } catch (Exception ex) { showError(d, "DB error: " + ex.getMessage()); }
            };
            loadUsers.run();

            // ─ Panel B: add user form ─
            JPanel addPanel = new JPanel(new BorderLayout(12, 12));
            addPanel.setBackground(CLR_BG);
            addPanel.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
            JPanel form = formPanel();
            form.add(styledLabel("Username:"));
            JTextField fldUname = styledField(); form.add(fldUname);
            form.add(styledLabel("Password:"));
            JPasswordField fldPwd = styledPwdField(); form.add(fldPwd);
            form.add(styledLabel("Role:"));
            JComboBox<String> roleBox = new JComboBox<>(new String[]{"customer", "admin"});
            roleBox.setBackground(CLR_CARD); roleBox.setForeground(CLR_TEXT); roleBox.setFont(FONT_FIELD);
            form.add(roleBox);
            addPanel.add(form, BorderLayout.CENTER);
            JButton btnCreate = primaryButton("Create User", new Color(255, 160, 50));
            btnCreate.addActionListener(ev -> {
                String uname = fldUname.getText().trim();
                String pwd   = new String(fldPwd.getPassword()).trim();
                String role  = (String) roleBox.getSelectedItem();
                if (uname.isEmpty() || pwd.isEmpty()) { showError(d, "Username and password are required."); return; }
                try (Connection conn = connect();
                     PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO users(username,password,role) VALUES(?,?,?)")) {
                    ps.setString(1, uname); ps.setString(2, pwd); ps.setString(3, role);
                    ps.executeUpdate();
                    showInfo(d, "User '" + uname + "' created as " + role + " ✅");
                    fldUname.setText(""); fldPwd.setText("");
                    loadUsers.run();
                } catch (Exception ex) { showError(d, "DB error (username may already exist): " + ex.getMessage()); }
            });
            addPanel.add(btnRow(btnCreate), BorderLayout.SOUTH);

            JPanel center = new JPanel(new CardLayout());
            center.setBackground(CLR_BG);
            center.add(listPanel, "list");
            center.add(addPanel, "add");
            d.add(center, BorderLayout.CENTER);

            tabList.addActionListener(ev -> ((CardLayout)center.getLayout()).show(center, "list"));
            tabAdd .addActionListener(ev -> ((CardLayout)center.getLayout()).show(center, "add"));

            d.setVisible(true);
        }

        // ── Password Reset Requests ──
        private void showPasswordRequestsDialog() {
            JDialog d = styledDialog(this, "Password Reset Requests", 820, 500);
            d.setLayout(new BorderLayout(8, 8));

            DefaultTableModel reqModel = new DefaultTableModel(
                new String[]{"Req ID", "Username", "Requested At", "Status"}, 0){
                @Override public boolean isCellEditable(int r, int c){ return false; }
            };
            JTable reqTable = new JTable(reqModel); styleTable(reqTable);
            colorStatusColumn(reqTable, 3);

            Runnable loadReqs = () -> {
                reqModel.setRowCount(0);
                try (Connection conn = connect();
                     PreparedStatement ps = conn.prepareStatement(
                         "SELECT id,username,created_at,status FROM pwd_reset_requests ORDER BY id DESC");
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next())
                        reqModel.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4)});
                } catch (Exception ex) { showError(d, "DB error: " + ex.getMessage()); }
            };
            loadReqs.run();

            // Bottom bar: set new password for selected request
            JPanel bottomBar = new JPanel(new BorderLayout(12, 0));
            bottomBar.setBackground(CLR_BG);
            bottomBar.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

            JPanel pwdInput = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            pwdInput.setBackground(CLR_BG);
            pwdInput.add(styledLabel("New Password:"));
            JPasswordField fldNewPwd = styledPwdField(); fldNewPwd.setPreferredSize(new Dimension(160, 32)); pwdInput.add(fldNewPwd);
            bottomBar.add(pwdInput, BorderLayout.CENTER);

            JPanel actionBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            actionBtns.setBackground(CLR_BG);
            JButton btnRefresh  = primaryButton("↻ Refresh", CLR_ACCENT);
            JButton btnSetPwd   = primaryButton("🔒 Set Password", new Color(200, 100, 255));
            btnRefresh.setPreferredSize(new Dimension(120, 34));
            btnSetPwd.setPreferredSize(new Dimension(160, 34));
            actionBtns.add(btnRefresh); actionBtns.add(btnSetPwd);
            bottomBar.add(actionBtns, BorderLayout.EAST);

            btnRefresh.addActionListener(ev -> loadReqs.run());

            btnSetPwd.addActionListener(ev -> {
                int row = reqTable.getSelectedRow();
                if (row < 0) { showError(d, "Select a request row first."); return; }
                String reqStatus = reqModel.getValueAt(row, 3).toString();
                if ("Resolved".equals(reqStatus)) { showError(d, "This request is already resolved."); return; }
                int reqId       = (int)   reqModel.getValueAt(row, 0);
                String username = (String) reqModel.getValueAt(row, 1);
                String newPwd   = new String(fldNewPwd.getPassword()).trim();
                if (newPwd.isEmpty()) { showError(d, "Enter a new password first."); return; }
                try (Connection conn = connect()) {
                    conn.prepareStatement("UPDATE users SET password='" + newPwd.replace("'","''") + "' WHERE username='" + username.replace("'","''") + "'").executeUpdate();
                    conn.prepareStatement("UPDATE pwd_reset_requests SET status='Resolved' WHERE id=" + reqId).executeUpdate();
                    // Notify customer if they have an account
                    PreparedStatement findUser = conn.prepareStatement("SELECT id FROM users WHERE username=?");
                    findUser.setString(1, username); ResultSet rs2 = findUser.executeQuery();
                    if (rs2.next()) {
                        int custId = rs2.getInt(1);
                        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        PreparedStatement notif = conn.prepareStatement(
                            "INSERT INTO notifications(customer_id,message,is_read,created_at) VALUES(?,?,0,?)");
                        notif.setInt(1, custId);
                        notif.setString(2, "🔒 Your password has been reset by admin. Please login with your new password.");
                        notif.setString(3, now); notif.executeUpdate();
                    }
                    showInfo(d, "Password updated for '" + username + "' and request marked Resolved. ✅");
                    fldNewPwd.setText("");
                    loadReqs.run();
                } catch (Exception ex) { showError(d, "DB error: " + ex.getMessage()); }
            });

            d.add(styledScroll(reqTable), BorderLayout.CENTER);
            d.add(bottomBar, BorderLayout.SOUTH);
            d.setVisible(true);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  CUSTOMER DASHBOARD
    // ─────────────────────────────────────────────────────────────────
    static class CustomerDashboard extends JFrame {

        private final int customerId;
        private final String customerName;
        private JLabel notifBadge;

        CustomerDashboard(int customerId, String customerName) {
            super("Vehicle Rental — Customer Dashboard");
            this.customerId   = customerId;
            this.customerName = customerName;
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(560, 520);
            setLocationRelativeTo(null);
            styleFrame(this);
            buildUI();
            refreshNotifBadge();
        }

        private void buildUI() {
            JPanel root = new JPanel(new BorderLayout(16, 16));
            root.setBackground(CLR_BG);
            root.setBorder(BorderFactory.createEmptyBorder(24, 30, 24, 30));

            // ── Header ──
            JPanel header = new JPanel(new BorderLayout());
            header.setBackground(CLR_BG);

            JLabel welcome = new JLabel("Welcome, " + customerName + "  👋");
            welcome.setFont(new Font("SansSerif", Font.BOLD, 18));
            welcome.setForeground(CLR_TEXT);
            header.add(welcome, BorderLayout.WEST);

            JLabel roleTag = new JLabel("  CUSTOMER");
            roleTag.setFont(new Font("SansSerif", Font.BOLD, 11));
            roleTag.setForeground(Color.WHITE);
            roleTag.setOpaque(true);
            roleTag.setBackground(CLR_SUCCESS);
            roleTag.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
            header.add(roleTag, BorderLayout.EAST);

            root.add(header, BorderLayout.NORTH);

            // ── Button Grid (2×2) ──
            JPanel grid = new JPanel(new GridLayout(2, 2, 16, 16));
            grid.setBackground(CLR_BG);

            grid.add(customerCard("🚗  Browse & Order Vehicles", CLR_ACCENT,  e -> showAvailableVehiclesDialog()));
            grid.add(customerCard("📋  My Orders",               CLR_ACCENT2, e -> showMyOrdersDialog()));

            // Notifications card with badge
            JPanel notifCard = buildNotifCard();
            grid.add(notifCard);

            // Filler
            JPanel filler = new JPanel(); filler.setBackground(CLR_BG);
            grid.add(filler);

            root.add(grid, BorderLayout.CENTER);

            // ── Footer ──
            JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            footer.setBackground(CLR_BG);
            JButton btnLogout = primaryButton("Logout", CLR_DANGER);
            btnLogout.setPreferredSize(new Dimension(110, 36));
            btnLogout.addActionListener(e -> { dispose(); new LoginFrame().setVisible(true); });
            footer.add(btnLogout);
            root.add(footer, BorderLayout.SOUTH);

            add(root);
        }

        private JPanel buildNotifCard() {
            JPanel card = new JPanel(new BorderLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(CLR_CARD);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                    g2.setColor(CLR_ACCENT2);
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 16, 16);
                    g2.dispose();
                }
            };
            card.setOpaque(false);
            card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

            JPanel inner = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
            inner.setOpaque(false);

            JLabel lbl = new JLabel("🔔  My Notifications");
            lbl.setFont(new Font("SansSerif", Font.BOLD, 14));
            lbl.setForeground(CLR_TEXT);
            inner.add(lbl);

            notifBadge = new JLabel("0");
            notifBadge.setFont(new Font("SansSerif", Font.BOLD, 11));
            notifBadge.setForeground(Color.WHITE);
            notifBadge.setOpaque(true);
            notifBadge.setBackground(CLR_DANGER);
            notifBadge.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            notifBadge.setVisible(false);
            inner.add(notifBadge);

            card.add(inner, BorderLayout.CENTER);

            card.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { showNotificationsDialog(); }
                @Override public void mouseEntered(MouseEvent e) { lbl.setForeground(CLR_ACCENT2); repaint(); }
                @Override public void mouseExited(MouseEvent e)  { lbl.setForeground(CLR_TEXT); repaint(); }
            });
            return card;
        }

        private JPanel customerCard(String label, Color accent, ActionListener al) {
            JPanel card = new JPanel(new BorderLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(CLR_CARD);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                    g2.setColor(accent);
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 16, 16);
                    g2.dispose();
                }
            };
            card.setOpaque(false);
            card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

            JLabel lbl = new JLabel(label, SwingConstants.CENTER);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 14));
            lbl.setForeground(CLR_TEXT);
            card.add(lbl, BorderLayout.CENTER);

            card.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { al.actionPerformed(null); }
                @Override public void mouseEntered(MouseEvent e) { lbl.setForeground(accent); repaint(); }
                @Override public void mouseExited(MouseEvent e)  { lbl.setForeground(CLR_TEXT); repaint(); }
            });
            return card;
        }

        private void refreshNotifBadge() {
            try (Connection conn = connect();
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM notifications WHERE customer_id=? AND is_read=0")) {
                ps.setInt(1, customerId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    int cnt = rs.getInt(1);
                    notifBadge.setText(String.valueOf(cnt));
                    notifBadge.setVisible(cnt > 0);
                }
            } catch (Exception ignored) {}
        }

        // ── Browse Available Vehicles + Place Order ──
        private void showAvailableVehiclesDialog() {
            JDialog d = styledDialog(this, "Available Vehicles", 820, 560);
            DefaultTableModel model = new DefaultTableModel(
                new String[]{"ID","Name","Price/Day","Status"},0){
                @Override public boolean isCellEditable(int r,int c){return false;}
            };
            JTable table = new JTable(model); styleTable(table);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            // ── Filter bar (NORTH) ──
            JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
            filterBar.setBackground(CLR_BG);
            filterBar.add(styledLabel("Search name:"));
            JTextField fldSearch = styledField(); fldSearch.setPreferredSize(new Dimension(160,32)); filterBar.add(fldSearch);
            filterBar.add(styledLabel("Max price/day ($):"));
            JTextField fldMax = styledField(); fldMax.setPreferredSize(new Dimension(90,32)); filterBar.add(fldMax);
            JButton btnFilter = primaryButton("Filter", CLR_ACCENT); btnFilter.setPreferredSize(new Dimension(90,34)); filterBar.add(btnFilter);
            JButton btnClear  = primaryButton("Clear",  new Color(70,80,110)); btnClear.setPreferredSize(new Dimension(80,34)); filterBar.add(btnClear);

            btnFilter.addActionListener(e -> loadAvailableVehicles(model, fldSearch.getText().trim(), fldMax.getText().trim()));
            btnClear.addActionListener(e -> { fldSearch.setText(""); fldMax.setText(""); loadAvailableVehicles(model,"",""); });
            loadAvailableVehicles(model, "", "");

            // ── Bottom bar: selection info + Place Order button (SOUTH) ──
            JPanel bottomBar = new JPanel(new BorderLayout(12, 0));
            bottomBar.setBackground(CLR_BG);
            bottomBar.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

            JLabel selectionHint = new JLabel("← Select a row, then click Place Order");
            selectionHint.setFont(FONT_SUB);
            selectionHint.setForeground(CLR_SUBTEXT);
            bottomBar.add(selectionHint, BorderLayout.WEST);

            JButton btnPlaceOrder = primaryButton("📝  Place Order", CLR_ACCENT2);
            btnPlaceOrder.setPreferredSize(new Dimension(160, 40));
            btnPlaceOrder.setEnabled(false);  // enabled only when a row is selected
            bottomBar.add(btnPlaceOrder, BorderLayout.EAST);

            // Enable button only when a row is selected
            table.getSelectionModel().addListSelectionListener(e -> {
                boolean selected = table.getSelectedRow() >= 0;
                btnPlaceOrder.setEnabled(selected);
                if (selected) {
                    int row = table.getSelectedRow();
                    String name  = model.getValueAt(row, 1).toString();
                    String price = model.getValueAt(row, 2).toString();
                    selectionHint.setText("Selected: " + name + "  ·  " + price + "/day");
                    selectionHint.setForeground(CLR_TEXT);
                } else {
                    selectionHint.setText("← Select a row, then click Place Order");
                    selectionHint.setForeground(CLR_SUBTEXT);
                }
            });

            // Place Order action — uses the selected row's vehicle ID & price
            btnPlaceOrder.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row < 0) { showError(d, "Please select a vehicle first."); return; }
                int    vehicleId = (int)    model.getValueAt(row, 0);
                String vehName   = (String) model.getValueAt(row, 1);
                String priceStr  = model.getValueAt(row, 2).toString().replace("$", "");
                double pricePerDay;
                try { pricePerDay = Double.parseDouble(priceStr); }
                catch (NumberFormatException ex) { showError(d, "Could not read price."); return; }

                // Small sub-dialog: just ask for number of days
                JDialog od = new JDialog(d, "Place Order — " + vehName, true);
                od.setSize(380, 240);
                od.setLocationRelativeTo(d);
                od.getContentPane().setBackground(CLR_BG);

                JPanel op = new JPanel(new BorderLayout(12, 12));
                op.setBackground(CLR_BG);
                op.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));

                JPanel info = new JPanel(new GridLayout(3, 1, 0, 6));
                info.setBackground(CLR_BG);
                JLabel lVeh   = new JLabel("Vehicle:  " + vehName); lVeh.setFont(FONT_SUB); lVeh.setForeground(CLR_TEXT);
                JLabel lPrice = new JLabel("Price:    $" + String.format("%.2f", pricePerDay) + " / day"); lPrice.setFont(FONT_SUB); lPrice.setForeground(CLR_SUBTEXT);
                info.add(lVeh); info.add(lPrice);
                info.add(styledLabel("Number of Days:"));
                op.add(info, BorderLayout.CENTER);

                JTextField fldDays = styledField();
                JPanel daysRow = new JPanel(new BorderLayout(0, 4));
                daysRow.setBackground(CLR_BG);
                daysRow.add(fldDays, BorderLayout.CENTER);

                JLabel totalPreview = new JLabel(" ");
                totalPreview.setFont(FONT_SUB); totalPreview.setForeground(CLR_ACCENT);
                daysRow.add(totalPreview, BorderLayout.SOUTH);
                op.add(daysRow, BorderLayout.SOUTH);

                // Live total preview
                fldDays.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                    void update() {
                        try {
                            int days = Integer.parseInt(fldDays.getText().trim());
                            if (days > 0) totalPreview.setText("Estimated Total: $" + String.format("%.2f", pricePerDay * days));
                            else totalPreview.setText(" ");
                        } catch (NumberFormatException ex) { totalPreview.setText(" "); }
                    }
                    public void insertUpdate(javax.swing.event.DocumentEvent e)  { update(); }
                    public void removeUpdate(javax.swing.event.DocumentEvent e)  { update(); }
                    public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
                });

                JPanel obt = btnRow(
                    primaryButton("Confirm Order", CLR_ACCENT2),
                    primaryButton("Cancel", CLR_DANGER)
                );
                ((JButton)obt.getComponent(1)).addActionListener(ev -> od.dispose());
                ((JButton)obt.getComponent(0)).addActionListener(ev -> {
                    String sDays = fldDays.getText().trim();
                    int days;
                    try { days = Integer.parseInt(sDays); if (days <= 0) throw new NumberFormatException(); }
                    catch (NumberFormatException ex) { showError(od, "Please enter a valid number of days."); return; }
                    double total = pricePerDay * days;
                    try (Connection conn = connect()) {
                        // Re-check availability before committing
                        PreparedStatement chk = conn.prepareStatement("SELECT status FROM vehicles WHERE id=?");
                        chk.setInt(1, vehicleId);
                        ResultSet rs = chk.executeQuery();
                        if (rs.next() && "Booked".equalsIgnoreCase(rs.getString(1))) {
                            showError(od, "Vehicle was just booked by someone else."); od.dispose(); return;
                        }
                        PreparedStatement ins = conn.prepareStatement(
                            "INSERT INTO orders(vehicle_id,customer_id,customer_name,days,total,status) VALUES(?,?,?,?,?,'Pending')");
                        ins.setInt(1, vehicleId); ins.setInt(2, customerId);
                        ins.setString(3, customerName); ins.setInt(4, days); ins.setDouble(5, total);
                        ins.executeUpdate();
                        showInfo(od, "Order placed! 🎉\n" + vehName + "  ·  " + days + " days\nEstimated Total: $"
                            + String.format("%.2f", total) + "\n\nWaiting for admin approval.");
                        od.dispose();
                        // Refresh list so newly-pending vehicle shows correct state
                        loadAvailableVehicles(model, "", "");
                    } catch (Exception ex) { showError(od, "DB error: " + ex.getMessage()); }
                });

                od.setLayout(new BorderLayout(8, 8));
                od.add(op, BorderLayout.CENTER);
                od.add(obt, BorderLayout.SOUTH);
                od.setVisible(true);
            });

            d.setLayout(new BorderLayout(8,8));
            d.add(filterBar, BorderLayout.NORTH);
            d.add(styledScroll(table), BorderLayout.CENTER);
            d.add(bottomBar, BorderLayout.SOUTH);
            d.setVisible(true);
        }

        private void loadAvailableVehicles(DefaultTableModel m, String nameFilter, String maxPriceStr) {
            m.setRowCount(0);
            String sql = "SELECT id,name,price_per_day,status FROM vehicles WHERE status='Available'";
            if (!nameFilter.isEmpty()) sql += " AND LOWER(name) LIKE LOWER('%'+?+'%')";
            double maxPrice = -1;
            if (!maxPriceStr.isEmpty()) {
                try { maxPrice = Double.parseDouble(maxPriceStr); sql += " AND price_per_day <= "+maxPrice; }
                catch (NumberFormatException ignored) {}
            }
            sql += " ORDER BY id";
            try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
                if (!nameFilter.isEmpty()) ps.setString(1, nameFilter);
                ResultSet rs = ps.executeQuery();
                while (rs.next())
                    m.addRow(new Object[]{rs.getInt(1),rs.getString(2),
                        String.format("$%.2f",rs.getDouble(3)),rs.getString(4)});
            } catch (Exception ex) { showError(this,"DB error: "+ex.getMessage()); }
        }

        // showPlaceOrderDialog removed — ordering is now done inline from showAvailableVehiclesDialog()

        // ── My Notifications ──
        private void showNotificationsDialog() {
            // Mark all as read
            try (Connection conn = connect();
                 PreparedStatement ps = conn.prepareStatement(
                     "UPDATE notifications SET is_read=1 WHERE customer_id=?")) {
                ps.setInt(1, customerId);
                ps.executeUpdate();
            } catch (Exception ignored) {}

            JDialog d = styledDialog(this, "My Notifications", 720, 440);
            DefaultTableModel model = new DefaultTableModel(
                new String[]{"#","Message","Date / Time","Read"},0){
                @Override public boolean isCellEditable(int r,int c){return false;}
            };
            JTable table = new JTable(model); styleTable(table);
            table.getColumnModel().getColumn(3).setMaxWidth(60);

            JScrollPane sp = styledScroll(table);

            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
            top.setBackground(CLR_BG);
            JButton btnRefresh = primaryButton("↻ Refresh", CLR_ACCENT);
            btnRefresh.setPreferredSize(new Dimension(120,36));
            btnRefresh.addActionListener(e -> loadNotifications(model));
            top.add(btnRefresh);
            loadNotifications(model);

            d.setLayout(new BorderLayout(8,8));
            d.add(top, BorderLayout.NORTH);
            d.add(sp, BorderLayout.CENTER);
            d.setVisible(true);

            // refresh badge after dialog closes
            refreshNotifBadge();
        }

        private void loadNotifications(DefaultTableModel m) {
            m.setRowCount(0);
            try (Connection conn = connect();
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT id,message,created_at,is_read FROM notifications WHERE customer_id=? ORDER BY id DESC")) {
                ps.setInt(1, customerId);
                ResultSet rs = ps.executeQuery();
                while (rs.next())
                    m.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3),
                        rs.getInt(4)==1 ? "Yes" : "No"});
            } catch (Exception ex) { showError(null,"DB error: "+ex.getMessage()); }
        }

        // ── My Orders ──
        private void showMyOrdersDialog() {
            JDialog d = styledDialog(this, "My Orders", 840, 460);
            DefaultTableModel model = new DefaultTableModel(
                new String[]{"Order ID", "Vehicle ID", "Vehicle Name", "Days", "Total ($)", "Status"}, 0){
                @Override public boolean isCellEditable(int r, int c){ return false; }
            };
            JTable table = new JTable(model); styleTable(table);
            colorStatusColumn(table, 5);

            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
            top.setBackground(CLR_BG);
            JButton btnRefresh = primaryButton("↻ Refresh", CLR_ACCENT);
            btnRefresh.setPreferredSize(new Dimension(120, 36));
            top.add(btnRefresh);

            JLabel hint = new JLabel("  Orders are updated in real-time when admin approves or rejects them.");
            hint.setFont(FONT_SUB); hint.setForeground(CLR_SUBTEXT);
            top.add(hint);

            Runnable loadOrders = () -> {
                model.setRowCount(0);
                String sql = "SELECT o.id, o.vehicle_id, v.name, o.days, o.total, o.status " +
                             "FROM orders o LEFT JOIN vehicles v ON o.vehicle_id=v.id " +
                             "WHERE o.customer_id=? ORDER BY o.id DESC";
                try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, customerId);
                    ResultSet rs = ps.executeQuery();
                    while (rs.next())
                        model.addRow(new Object[]{
                            rs.getInt(1), rs.getInt(2), rs.getString(3),
                            rs.getInt(4), String.format("$%.2f", rs.getDouble(5)), rs.getString(6)
                        });
                } catch (Exception ex) { showError(d, "DB error: " + ex.getMessage()); }
            };
            loadOrders.run();
            btnRefresh.addActionListener(e -> loadOrders.run());

            d.setLayout(new BorderLayout(8, 8));
            d.add(top, BorderLayout.NORTH);
            d.add(styledScroll(table), BorderLayout.CENTER);
            d.setVisible(true);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  SHARED DIALOG / LAYOUT HELPERS
    // ─────────────────────────────────────────────────────────────────
    static JDialog styledDialog(JFrame parent, String title, int w, int h) {
        JDialog d = new JDialog(parent, title, true);
        d.setSize(w, h);
        d.setLocationRelativeTo(parent);
        d.getContentPane().setBackground(CLR_BG);
        ((JPanel)d.getContentPane()).setBorder(BorderFactory.createEmptyBorder(20,24,16,24));
        return d;
    }

    static JPanel formPanel() {
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 8));
        p.setBackground(CLR_BG);
        return p;
    }

    static JPanel btnRow(JButton... btns) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        p.setBackground(CLR_BG);
        for (JButton b : btns) p.add(b);
        return p;
    }

    /** Color the status column: green=Available/Approved, red=Booked/Rejected, amber=Pending */
    static void colorStatusColumn(JTable table, int col) {
        table.getColumnModel().getColumn(col).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t,val,sel,foc,r,c);
                String s = val == null ? "" : val.toString();
                setHorizontalAlignment(SwingConstants.CENTER);
                setForeground(switch (s) {
                    case "Available", "Approved" -> CLR_SUCCESS;
                    case "Booked",   "Rejected"  -> CLR_DANGER;
                    case "Pending"               -> new Color(255, 200, 60);
                    default                      -> CLR_TEXT;
                });
                setBackground(sel ? CLR_ACCENT : CLR_CARD);
                setFont(FONT_LBL);
                return this;
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────
    //  MAIN
    // ─────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        // Dark title bar on supported platforms
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            ensureTablesExist();
            new LoginFrame().setVisible(true);
        });
    }
}
