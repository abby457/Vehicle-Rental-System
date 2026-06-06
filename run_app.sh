#!/bin/bash
set -e
cd "$(dirname "$0")"
echo "Compiling Vehicle Rental App..."
javac -cp .:sqlite-jdbc-3.50.3.0.jar VehicleRentalAppFull.java

echo "Running Vehicle Rental App..."
java -cp .:sqlite-jdbc-3.50.3.0.jar VehicleRentalAppFull
