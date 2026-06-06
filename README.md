# Vehicle Rental App

This project is a refactored and upgraded version of the original single-file Swing vehicle rental application.

## Build and run

Requirements:
- Java 17 or newer
- Maven

Build:
```bash
mvn clean package
```

Run:
```bash
java -jar target/vehicle-rental-app-1.0.0.jar
```

## What changed

- Maven-based project structure
- Modular persistence layer with SQLite repository classes
- Service layer with business rules for booking and returning vehicles
- Strongly typed domain model using Java records and enum status
- Improved UI structure and input validation
- Executable shaded JAR for easy distribution
