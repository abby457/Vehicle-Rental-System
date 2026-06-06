# 🚗 Vehicle Rental System

A Java Swing desktop application for managing vehicle rentals with two role-based portals — **Admin** and **Customer** — backed by a local SQLite database.

---

## 📸 Screenshots

### Login Screen

(screenshots/login.png)

### Admin Dashboard

(screenshots/admin-dashboard.png)

### Customer Dashboard

(screenshots/customer-dashboard.png)

---

## ✨ Features

### 🛠 Admin

* Add, view, and delete vehicles from the fleet
* Approve or reject customer booking requests
* Mark vehicles as returned and free them back up
* View all orders (Pending / Approved / Rejected)
* Manage users
* Handle password reset requests

### 👤 Customer

* Browse available vehicles with live name & price filters
* Place rental orders directly from the vehicle list
* Track personal orders and their status
* Receive notifications when orders are approved or rejected
* Request password resets

---

## 🗃️ Database Schema

| Table              | Purpose                                  |
| ------------------ | ---------------------------------------- |
| users              | Admin and customer accounts              |
| vehicles           | Fleet inventory with availability status |
| orders             | Customer rental requests                 |
| bookings           | Confirmed rentals                        |
| notifications      | Customer notifications                   |
| pwd_reset_requests | Password reset requests                  |

---

## 🔐 Default Credentials

| Role     | Username | Password |
| -------- | -------- | -------- |
| Admin    | admin    | admin123 |
| Customer | customer | cust123  |

Additional seeded accounts:

* alice / alice123
* bob / bob123
* sara / sara123

---

## 🧰 Tech Stack

* Java 17+
* Java Swing
* SQLite
* JDBC
* Maven

---

## 🚀 Build & Run

```bash
mvn clean package
java -jar target/vehicle-rental-app-1.0.0.jar
```

---

## 🔄 Workflow

```text
Customer → Places Order
        ↓
Admin → Approves / Rejects
        ↓
Notification Sent
        ↓
Customer Views Status
```

---

## 🔮 Future Enhancements

* Online payment integration
* Vehicle image uploads
* Email notifications
* Analytics dashboard
* Advanced search and filtering
* Password hashing (bcrypt)

---

